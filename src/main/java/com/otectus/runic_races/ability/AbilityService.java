package com.otectus.runic_races.ability;

import com.otectus.runic_races.util.RaceHelper;
import com.otectus.runic_races.presentation.*;
import com.otectus.runic_races.network.*;
import io.github.edwinmindcraft.apoli.api.component.IPowerContainer;
import net.minecraft.core.BlockPos;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.entity.*;
import net.minecraft.world.entity.ai.attributes.*;
import net.minecraft.world.effect.*;
import net.minecraft.world.phys.Vec3;
import net.minecraftforge.common.MinecraftForge;
import net.minecraftforge.event.entity.EntityTeleportEvent;
import java.util.*;

/** One bounded session per participating online player. Durable cooldowns are separate from casts. */
public final class AbilityService {
    public static final String SAVE_KEY = "runic_races:expansion_cooldowns";
    private static final Map<UUID, Session> SESSIONS = new HashMap<>();
    private static final Map<UUID, WardLedger> WARDS = new HashMap<>();
    private static final Map<UUID, Long> RECOVERY_ALLOWANCE = new HashMap<>();
    private static final Map<UUID, Long> CONTROL_ALLOWANCE = new HashMap<>();
    private static final UUID SPEED = UUID.fromString("77170000-4e15-4000-a000-000000000001");
    private static final UUID ATTACK = UUID.fromString("77170000-4e15-4000-a000-000000000002");
    private static final UUID FOOTING = UUID.fromString("77170000-4e15-4000-a000-000000000003");
    private static final UUID SWIM = UUID.fromString("77170000-4e15-4000-a000-000000000004");

    public static final class Session {
        public UUID generation = UUID.randomUUID();
        public final InputLatch input = new InputLatch();
        public final CooldownLedger cooldowns = new CooldownLedger();
        public AbilityTuning tuning;
        public String race = "";
        public long until, lastFeed = -1000, lastDeny = -1000, aggressorAt;
        public boolean prepared, cold, dry, inSun, groundedMovement, waterMovement;
        public int charges, stationary, dryTicks, wetTicks, moveTicks, windup;
        public float restored, offered;
        public UUID target, aggressor;
        public Vec3 anchor, direction, previous;
        public RecoveryField field;
        public Map<String, Double> traits = Map.of();
        public AbilitySnapshot lastSnapshot;
        public boolean live(long now) { return tuning != null && until > now; }
        public double trait(String name) { return traits.getOrDefault(name, 0.0); }
    }

    private AbilityService() {}
    public static long now(ServerPlayer p) { return Integer.toUnsignedLong(p.server.getTickCount()); }
    public static Session session(ServerPlayer p) { return SESSIONS.get(p.getUUID()); }
    public static WardLedger wards(LivingEntity e) { return WARDS.computeIfAbsent(e.getUUID(), k -> new WardLedger()); }
    public static WardLedger existingWards(LivingEntity e) { return WARDS.get(e.getUUID()); }
    public static boolean withdrawn(ServerPlayer p) {
        Session s = session(p);
        return s != null && s.tuning != null && s.tuning.kind() == AbilityKind.CHELON && s.live(now(p))
                && RaceHelper.isRace(p, "chelon") && hasAbility(p, AbilityKind.CHELON);
    }
    public static boolean hasAbility(ServerPlayer p, AbilityKind kind) { return resolve(p, kind) != null; }
    public static AbilityTuning resolve(net.minecraft.world.entity.player.Player p, AbilityKind kind) {
        var container = IPowerContainer.get(p).orElse(null);
        if (container == null) return null;
        var holder = container.getPower(new ResourceLocation("runic_races", kind.path() + "_active_ability"));
        if (holder == null || !holder.isBound() || !holder.value().isActive(p)) return null;
        return holder.value().getConfiguration() instanceof AbilityTuning tuning && tuning.kind() == kind ? tuning : null;
    }
    private static Map<String, Double> traits(ServerPlayer p) {
        Map<String, Double> out = new HashMap<>();
        for (var holder : IPowerContainer.getPowers(p, com.otectus.runic_races.registry.ModPowerFactories.RACIAL_TRAITS.get()))
            if (holder.isBound() && holder.value().isActive(p)) out.putAll(holder.value().getConfiguration().values());
        return Map.copyOf(out);
    }
    private static Session create(ServerPlayer p) {
        Session s = new Session();
        CompoundTag saved = p.getPersistentData().getCompound(SAVE_KEY);
        Map<String, Integer> cooldowns = new HashMap<>();
        for (AbilityKind kind : AbilityKind.values()) cooldowns.put(kind.cooldownId(), saved.getInt(kind.cooldownId()));
        s.cooldowns.restore(cooldowns);
        s.dryTicks = Math.min(1200, Math.max(0, saved.getInt("tide_dry_ticks")));
        return s;
    }
    public static void save(ServerPlayer p) {
        Session s = session(p); if (s == null) return;
        CompoundTag tag = new CompoundTag();
        s.cooldowns.forEach((kind, remaining) -> { if (remaining > 0) tag.putInt(kind.cooldownId(), remaining); });
        tag.putInt("tide_dry_ticks", s.dryTicks);
        p.getPersistentData().put(SAVE_KEY, tag);
    }
    public static void input(ServerPlayer p, long sequence, boolean down) {
        Session s = SESSIONS.computeIfAbsent(p.getUUID(), k -> create(p));
        if (!s.input.accept(sequence, down, now(p))) return;
        String race = RaceHelper.getRaceName(p).orElse("");
        AbilityKind kind = AbilityKind.forRace(race).orElse(null);
        AbilityTuning tuning = kind == null ? null : resolve(p, kind);
        if (tuning == null || !p.isAlive() || p.isSpectator() || p.isSleeping()) return;
        if (!race.equals(s.race)) { clearCast(p, s); s.race = race; }
        s.tuning = tuning;
        long now = now(p);
        if (kind == AbilityKind.ASTRAL_ELF && s.anchor != null && s.live(now)) { recall(p, s); sync(p, s); return; }
        if (kind == AbilityKind.CHELON && s.live(now)) { AbilityFeedback.wardEnd(p, p, WardLedger.Kind.SHELL); clearCast(p, s); sync(p, s); return; }
        if (s.cooldowns.getOrDefault(kind, 0) > 0) { deny(p, s, "cooldown"); return; }
        if (p.isPassenger() || p.isUsingItem() || s.moveTicks > 0) { deny(p, s, "busy"); return; }
        if ((kind == AbilityKind.BOVINE || kind == AbilityKind.MOSS_ONE || kind == AbilityKind.ASTRAL_ELF)
                && (!p.onGround() || RacialEnvironment.water(p))) { deny(p, s, "grounded"); return; }
        if (kind == AbilityKind.SAURIAN && s.stationary < tuning.ticks("stationary_ticks")) { deny(p, s, "stationary"); return; }
        if (kind == AbilityKind.WYVERNKIN && (RacialEnvironment.water(p) || !(p.onGround() || p.isFallFlying()))) { deny(p, s, "glide_or_ground"); return; }
        if (kind == AbilityKind.TIDE_ELF && !RacialEnvironment.water(p) && !p.onGround()) { deny(p, s, "water_or_ground"); return; }
        if (kind == AbilityKind.ASTRAL_ELF && !SafeMovement.safe(p, p.position(), true)) { deny(p, s, "unsafe_anchor"); return; }
        if (kind == AbilityKind.MOSS_ONE && !SafeMovement.safe(p, p.position(), true)) { deny(p, s, "grounded"); return; }
        LivingEntity aggressor = null;
        if (kind == AbilityKind.RETURNED) {
            aggressor = entity(p, s.aggressor);
            if (aggressor == null || now - s.aggressorAt > tuning.ticks("recency_ticks")
                    || !TargetPolicy.aimed(p, aggressor) || p.distanceToSqr(aggressor) > Math.pow(tuning.number("range"), 2)
                    || !TargetPolicy.visible(p, aggressor)) { deny(p, s, "aggressor"); return; }
        }
        s.until = now + tuning.durationTicks(); s.prepared = false; s.charges = tuning.ticks("charges");
        s.restored = 0; s.offered = 0; s.lastFeed = now - 1200;
        s.cooldowns.put(kind, tuning.cooldownTicks());
        switch (kind) {
            case COLOSSAN, GROVE_ELF, SAURIAN -> s.prepared = true;
            case RETURNED -> { s.prepared = true; s.target = aggressor.getUUID(); }
            case AURORAN -> {
                List<LivingEntity> recipients = new ArrayList<>(); recipients.add(p);
                p.level().getEntitiesOfClass(LivingEntity.class, p.getBoundingBox().inflate(tuning.number("range")),
                                e -> e != p && TargetPolicy.support(p, e) && p.distanceToSqr(e) <= Math.pow(tuning.number("range"), 2) && TargetPolicy.visible(p, e))
                        .stream().sorted(Comparator.<LivingEntity>comparingDouble(e -> e.getHealth() / e.getMaxHealth())
                                .thenComparingDouble(p::distanceToSqr).thenComparing(LivingEntity::getUUID))
                        .limit(Math.max(0, tuning.ticks("targets") - 1)).forEach(recipients::add);
                for (LivingEntity recipient : recipients) grant(p, recipient, WardLedger.Kind.DAWN, tuning, now);
            }
            case CRYSTAL_ONE -> grant(p, p, WardLedger.Kind.PRISM, tuning, now);
            case CHELON -> { p.stopUsingItem(); p.setSprinting(false); ShellCasting.interrupt(p); grant(p, p, WardLedger.Kind.SHELL, tuning, now); }
            case SCALEHEIR -> {
                grant(p, p, WardLedger.Kind.DOMINION, tuning, now);
                for (LivingEntity e : TargetPolicy.nearby(p, p.position(), tuning.number("range"),
                        e -> TargetPolicy.threat(p, e) && TargetPolicy.visible(p, e), tuning.ticks("targets")))
                    control(p, e, MobEffects.WEAKNESS, tuning.ticks("effect_ticks"), 0, false);
            }
            case ASTRAL_ELF -> s.anchor = p.position();
            case MOUNTAIN_ONE, NIGHTBORN -> { }
            case MOSS_ONE -> s.field = new RecoveryField(p, tuning, now);
            case WAILER -> s.windup = tuning.ticks("windup");
            case TIDE_ELF, BOVINE, ZEPHYR, WYVERNKIN -> {
                boolean water = kind == AbilityKind.TIDE_ELF && RacialEnvironment.water(p);
                boolean airborne = kind == AbilityKind.WYVERNKIN && p.isFallFlying();
                s.waterMovement = water;
                s.groundedMovement = kind == AbilityKind.BOVINE || kind == AbilityKind.TIDE_ELF && !water || kind == AbilityKind.WYVERNKIN && !airborne;
                Vec3 look = p.getLookAngle();
                if (!water) look = new Vec3(look.x, airborne ? Math.min(0, Math.max(-0.25, look.y)) : 0, look.z);
                if (look.lengthSqr() < 0.01) look = new Vec3(-Math.sin(Math.toRadians(p.getYRot())), 0, Math.cos(Math.toRadians(p.getYRot())));
                double range = (kind == AbilityKind.TIDE_ELF && !water || kind == AbilityKind.WYVERNKIN && !airborne)
                        ? tuning.number("land_range") : tuning.number("range");
                s.moveTicks = Math.max(1, tuning.ticks("move_ticks")); s.windup = tuning.ticks("windup");
                s.direction = look.normalize().scale(Math.min(0.75, range / s.moveTicks));
            }
        }
        RunicPresentation.fire(p, SignatureKey.valueOf(kind.name() + "_ACTIVE"));
        save(p); sync(p, s);
    }
    private static void grant(ServerPlayer owner, LivingEntity recipient, WardLedger.Kind kind, AbilityTuning tuning, long now) {
        if (!RacialTargetEvent.allowed(owner, recipient, RacialTargetEvent.Action.SUPPORT)) return;
        WardLedger ledger = wards(recipient);
        float previous = ledger.remaining(kind);
        ledger.offer(new WardLedger.Ward(kind, owner.getUUID(), now + tuning.durationTicks(), tuning.amount("budget"), tuning.amount("prevent_fraction")), now);
        if (tuning.amount("budget") >= previous) AbilityFeedback.ward(owner, recipient, kind);
        if (recipient instanceof ServerPlayer ally && ally != owner)
            ally.displayClientMessage(Component.translatable("message.runic_races.ward_received", tuning.amount("budget")), true);
    }
    private static void recall(ServerPlayer p, Session s) {
        Vec3 anchor = s.anchor;
        if (p.isPassenger() || p.isSleeping() || anchor.distanceToSqr(p.position()) > Math.pow(s.tuning.number("range"), 2)
                || !SafeMovement.safe(p, anchor, true) || !TargetPolicy.visible(p, p.getEyePosition(), anchor.add(0, p.getEyeHeight(), 0))) {
            deny(p, s, "blocked_recall"); return;
        }
        var teleport = new EntityTeleportEvent.EnderEntity(p, anchor.x, anchor.y, anchor.z);
        if (MinecraftForge.EVENT_BUS.post(teleport)) { deny(p, s, "blocked_recall"); return; }
        // A hook may veto, but cannot silently send this bounded ability to an unrelated destination.
        if (teleport.getTargetX() != anchor.x || teleport.getTargetY() != anchor.y || teleport.getTargetZ() != anchor.z) {
            deny(p, s, "blocked_recall"); return;
        }
        Vec3 departure = p.position();
        p.connection.teleport(anchor.x, anchor.y, anchor.z, p.getYRot(), p.getXRot());
        p.setDeltaMovement(Vec3.ZERO); p.fallDistance = 0;
        AbilityFeedback.recall(p, departure, anchor); s.anchor = null; s.until = 0;
    }
    public static void tick(ServerPlayer p) {
        String race = RaceHelper.getRaceName(p).orElse("");
        AbilityKind kind = AbilityKind.forRace(race).orElse(null);
        Session s = session(p);
        if (s == null && kind == null && !p.getPersistentData().contains(SAVE_KEY)) return;
        if (s == null) { s = create(p); SESSIONS.put(p.getUUID(), s); }
        s.cooldowns.tick();
        if (!race.equals(s.race)) { clearCast(p, s); s.generation = UUID.randomUUID(); s.race = race; s.tuning = null; s.traits = Map.of(); }
        if (p.tickCount % 10 == 0 || s.tuning == null) {
            AbilityTuning current = kind == null ? null : resolve(p, kind);
            if (!Objects.equals(current, s.tuning)) clearCast(p, s);
            s.tuning = current; s.traits = current == null ? Map.of() : traits(p);
        }
        if (!p.isAlive()) { clearCast(p, s); save(p); return; }
        long now = now(p);
        if (s.previous != null && p.position().distanceToSqr(s.previous) < 0.0001 && !p.isPassenger()
                && !p.isSprinting() && !p.horizontalCollision && p.getDeltaMovement().horizontalDistanceSqr() < 0.0001)
            s.stationary = Math.min(100, s.stationary + 1);
        else s.stationary = 0;
        Vec3 movement = s.previous == null ? Vec3.ZERO : p.position().subtract(s.previous);
        if (kind == AbilityKind.CHELON && s.live(now) && movement.horizontalDistance() > .043) {
            if (movement.lengthSqr() > 64) clearCast(p, s);
            else {
                double ratio = .043 / movement.horizontalDistance();
                Vec3 limited = new Vec3(s.previous.x + movement.x * ratio, p.getY(), s.previous.z + movement.z * ratio);
                if (SafeMovement.safe(p, limited, false))
                    p.connection.teleport(limited.x, limited.y, limited.z, p.getYRot(), p.getXRot());
                movement = p.position().subtract(s.previous);
            }
        }
        s.previous = p.position();
        if (s.until > 0 && !s.live(now) && s.field == null) clearCast(p, s);
        if (s.live(now) && (p.isPassenger() || p.isSleeping())) clearCast(p, s);
        if (s.live(now)) {
            if (s.target != null && (entity(p, s.target) == null || !TargetPolicy.aimed(p, entity(p, s.target)))) { s.target = null; s.prepared = false; s.until = 0; }
            if (s.windup > 0) {
                s.windup--;
                if (s.windup == 0 && kind == AbilityKind.WAILER) { cry(p, s); s.until = 0; }
            } else if (s.moveTicks > 0 && s.direction != null) {
                boolean attack = kind == AbilityKind.BOVINE || kind == AbilityKind.WYVERNKIN;
                Vec3 departure = p.position();
                SafeMovement.Step step = s.waterMovement && !RacialEnvironment.water(p)
                        ? new SafeMovement.Step(true, null) : SafeMovement.advance(p, s.direction, attack, s.groundedMovement);
                if (p.tickCount % 2 == 0) AbilityFeedback.movement(p, departure, p.position(), kind);
                s.moveTicks--;
                if (step.contact() != null && TargetPolicy.aimed(p, step.contact())) {
                    LivingEntity hit = step.contact();
                    if (DamageHooks.deal(p, hit, s.tuning.amount("damage"), false) > 0) {
                        if (kind == AbilityKind.WYVERNKIN) hit.addEffect(new MobEffectInstance(MobEffects.POISON, s.tuning.ticks("effect_ticks"), 0), p);
                        else control(p, hit, null, 0, 0, true);
                        AbilityFeedback.impact(p, hit.getBoundingBox().getCenter(), kind);
                    }
                }
                if (step.stopped() || s.moveTicks <= 0) { s.moveTicks = 0; s.direction = null; s.until = 0; }
            }
            if (kind == AbilityKind.CHELON) {
                p.stopUsingItem(); p.setSprinting(false);
                ShellCasting.interrupt(p);
                Vec3 velocity = p.getDeltaMovement(); double horizontal = velocity.horizontalDistance();
                if (horizontal > 0.02) p.setDeltaMovement(velocity.x * 0.02 / horizontal, velocity.y, velocity.z * 0.02 / horizontal);
                WardLedger ledger = existingWards(p);
                if (ledger == null || ledger.remaining(WardLedger.Kind.SHELL) <= 0) clearCast(p, s);
            }
            if (s.anchor != null && p.tickCount % 10 == 0) AbilityFeedback.anchor(p, s.anchor);
        }
        if (s.field != null && !s.field.tick(p, now, RECOVERY_ALLOWANCE)) { s.field = null; s.until = 0; }
        if (s.field != null && p.tickCount % 10 == 0)
            AbilityFeedback.field(p, s.field.center(), s.tuning.number("radius"));
        if (p.tickCount % 10 == 0) environment(p, s, movement);
        else modifiers(p, s, movement);
        if (p.tickCount % 20 == 0) {
            save(p);
            if (kind == AbilityKind.WAILER) for (LivingEntity e : TargetPolicy.nearby(p, p.position(), Math.min(8, s.trait("sense_range")),
                    e -> TargetPolicy.threat(p, e) && e.getHealth() < e.getMaxHealth() * 0.5 && TargetPolicy.visible(p, e), (int)Math.min(3, s.trait("sense_targets"))))
                ownerCue(p, e.getBoundingBox().getCenter(), "deathwatch");
        }
        if (p.tickCount % 10 == 0) sync(p, s);
    }
    private static void environment(ServerPlayer p, Session s, Vec3 movement) {
        AbilityKind kind = s.tuning == null ? null : s.tuning.kind();
        if (kind == AbilityKind.TIDE_ELF) {
            if (RacialEnvironment.rain(p)) { s.dryTicks = 0; s.wetTicks = 0; }
            else if (RacialEnvironment.water(p)) { s.wetTicks += 10; if (s.wetTicks >= s.trait("wet_refresh")) s.dryTicks = 0; }
            else { s.wetTicks = 0; s.dryTicks = Math.min(1200, s.dryTicks + 10); }
            s.dry = s.dryTicks >= s.trait("dry_after") && !RacialEnvironment.water(p);
        } else s.dry = false;
        s.cold = kind == AbilityKind.SAURIAN && RacialEnvironment.cold(p);
        s.inSun = kind == AbilityKind.NIGHTBORN && RacialEnvironment.sunlight(p);
        modifiers(p, s, movement);
    }
    private static void modifiers(ServerPlayer p, Session s, Vec3 movement) {
        AbilityKind kind = s.tuning == null ? null : s.tuning.kind();
        double speed = 0, attack = 0, kb = 0;
        if (kind == AbilityKind.GROVE_ELF && p.onGround() && p.level().getBiome(p.blockPosition()).is(RacialTags.GROVE)) speed += s.trait("forest_speed");
        if (s.dry) speed -= s.trait("dry_slow");
        if (s.cold) attack -= s.trait("cold_attack_slow");
        if (kind == AbilityKind.MOUNTAIN_ONE && RacialEnvironment.stoneFooting(p)) kb += s.trait("stone_knockback");
        if (kind == AbilityKind.BOVINE && p.onGround() && p.isSprinting()) speed += s.trait("sprint_speed");
        if (kind == AbilityKind.RETURNED && p.getHealth() < p.getMaxHealth() * 0.5) kb += s.trait("low_health_knockback");
        if (kind == AbilityKind.NIGHTBORN && RacialEnvironment.night(p) && p.getFoodData().getFoodLevel() >= s.trait("night_food")) speed += s.trait("night_speed");
        if (s.live(now(p))) {
            if (kind == AbilityKind.NIGHTBORN) speed += s.tuning.number("speed");
            if (kind == AbilityKind.WAILER && s.windup > 0) speed -= s.tuning.number("speed");
            if (kind == AbilityKind.RETURNED && s.target != null && p.onGround()) {
                LivingEntity target = entity(p, s.target);
                if (target != null && movement.horizontalDistanceSqr() > 0.0001 && movement.normalize().dot(target.position().subtract(p.position()).normalize()) > 0.5)
                    speed += s.tuning.number("speed");
            }
            if (kind == AbilityKind.CHELON) speed = -0.80;
        }
        modifier(p, Attributes.MOVEMENT_SPEED, SPEED, speed, AttributeModifier.Operation.MULTIPLY_TOTAL);
        modifier(p, Attributes.ATTACK_SPEED, ATTACK, attack, AttributeModifier.Operation.MULTIPLY_TOTAL);
        modifier(p, Attributes.KNOCKBACK_RESISTANCE, FOOTING, kb, AttributeModifier.Operation.ADDITION);
        modifier(p, net.minecraftforge.common.ForgeMod.SWIM_SPEED.get(), SWIM, s.trait("swim_speed"), AttributeModifier.Operation.MULTIPLY_TOTAL);
    }
    private static void modifier(ServerPlayer p, Attribute attribute, UUID id, double amount, AttributeModifier.Operation operation) {
        AttributeInstance instance = p.getAttribute(attribute); if (instance == null) return;
        AttributeModifier old = instance.getModifier(id);
        if (old != null && old.getAmount() == amount) return;
        if (old != null) instance.removeModifier(id);
        if (amount != 0) instance.addTransientModifier(new AttributeModifier(id, "Runic Races situational trait", amount, operation));
    }
    private static void cry(ServerPlayer p, Session s) {
        AbilityFeedback.cry(p, s.tuning.number("range"), s.tuning.number("angle"));
        Vec3 look = p.getLookAngle(); double cosine = Math.cos(Math.toRadians(s.tuning.number("angle")));
        for (LivingEntity e : TargetPolicy.nearby(p, p.position(), s.tuning.number("range"),
                e -> TargetPolicy.threat(p, e) && TargetPolicy.visible(p, e)
                        && e.getBoundingBox().getCenter().subtract(p.getEyePosition()).normalize().dot(look) >= cosine, s.tuning.ticks("targets"))) {
            if (DamageHooks.deal(p, e, s.tuning.amount("damage"), false) > 0) {
                control(p, e, MobEffects.WEAKNESS, s.tuning.ticks("effect_ticks"), 0, true);
                AbilityFeedback.impact(p, e.getBoundingBox().getCenter(), AbilityKind.WAILER);
            }
        }
    }
    public static void control(ServerPlayer p, LivingEntity e, MobEffect effect, int duration, int amplifier, boolean knockback) {
        if (!TargetPolicy.control(p, e) || !RacialTargetEvent.allowed(p, e, RacialTargetEvent.Action.CONTROL)) return;
        long now = now(p);
        if (e instanceof net.minecraft.world.entity.player.Player) {
            if (CONTROL_ALLOWANCE.getOrDefault(e.getUUID(), 0L) > now) return;
            duration = Math.max(1, duration / 2); CONTROL_ALLOWANCE.put(e.getUUID(), now + Math.max(60, duration + 20));
        }
        if (effect != null) e.addEffect(new MobEffectInstance(effect, duration, amplifier), p);
        if (knockback) { Vec3 d = e.position().subtract(p.position()); e.knockback(0.4, -d.x, -d.z); }
    }
    public static LivingEntity entity(ServerPlayer p, UUID id) {
        if (id == null) return null;
        Entity e = p.serverLevel().getEntity(id);
        return e instanceof LivingEntity living && living.isAlive() ? living : null;
    }
    public static void deny(ServerPlayer p, Session s, String reason) {
        if (now(p) - s.lastDeny < 20) return;
        s.lastDeny = now(p); p.displayClientMessage(Component.translatable("message.runic_races.denied." + reason), true);
    }
    public static void ownerCue(ServerPlayer p, Vec3 position, String kind) {
        if (com.otectus.runic_races.config.RRServerConfig.SIGNATURE_PARTICLE_DENSITY.get() <= 0) return;
        p.serverLevel().sendParticles(p, switch (kind) { case "recovery" -> ParticleTypes.SPORE_BLOSSOM_AIR; case "deathwatch" -> ParticleTypes.SOUL; default -> ParticleTypes.END_ROD; },
                false, position.x, position.y, position.z, 1, 0.06, 0.06, 0.06, 0);
    }
    public static void clearCast(ServerPlayer p, Session s) {
        s.until = 0; s.prepared = false; s.charges = 0; s.anchor = null; s.target = null;
        s.aggressor = null; s.aggressorAt = 0; s.direction = null; s.moveTicks = 0; s.windup = 0; s.field = null;
        s.groundedMovement = false; s.waterMovement = false;
        s.generation = UUID.randomUUID();
        WARDS.values().forEach(w -> w.removeOwner(p.getUUID()));
        modifier(p, Attributes.MOVEMENT_SPEED, SPEED, 0, AttributeModifier.Operation.MULTIPLY_TOTAL);
        modifier(p, Attributes.ATTACK_SPEED, ATTACK, 0, AttributeModifier.Operation.MULTIPLY_TOTAL);
        modifier(p, Attributes.KNOCKBACK_RESISTANCE, FOOTING, 0, AttributeModifier.Operation.ADDITION);
        modifier(p, net.minecraftforge.common.ForgeMod.SWIM_SPEED.get(), SWIM, 0, AttributeModifier.Operation.MULTIPLY_TOTAL);
    }
    public static void clear(ServerPlayer p, boolean logout) {
        Session s = session(p); if (s != null) { save(p); clearCast(p, s); s.lastSnapshot = null; if (!logout) sync(p, s); }
        WARDS.remove(p.getUUID());
        if (logout) SESSIONS.remove(p.getUUID());
    }
    public static void serverTick(long now) {
        WARDS.values().forEach(w -> w.expire(now)); WARDS.values().removeIf(WardLedger::isEmpty);
        RECOVERY_ALLOWANCE.values().removeIf(t -> t <= now); CONTROL_ALLOWANCE.values().removeIf(t -> t <= now);
    }
    public static void stop() { SESSIONS.clear(); WARDS.clear(); RECOVERY_ALLOWANCE.clear(); CONTROL_ALLOWANCE.clear(); DamageHooks.clear(); AbilityFeedback.clear(); }
    public static void reload(net.minecraft.server.MinecraftServer server) {
        for (ServerPlayer p : server.getPlayerList().getPlayers()) { clear(p, false); Session s = session(p); if (s != null) { s.tuning = null; s.traits = Map.of(); } }
    }
    public static void sync(ServerPlayer p, Session s) {
        if (s.tuning != null) com.otectus.runic_races.util.OriginsPowerHelper.assignResource(p,
                new ResourceLocation(s.tuning.kind().cooldownId()), s.cooldowns.getOrDefault(s.tuning.kind(), 0), false);
        AbilitySnapshot snapshot = AbilitySnapshot.of(p, s);
        if (!snapshot.equals(s.lastSnapshot)) { NetworkHandler.sendToPlayer(p, snapshot); s.lastSnapshot = snapshot; }
    }
}
