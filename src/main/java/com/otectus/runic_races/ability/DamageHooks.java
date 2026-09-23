package com.otectus.runic_races.ability;

import com.otectus.runic_races.util.RaceHelper;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.entity.*;
import net.minecraft.world.entity.projectile.AbstractArrow;
import net.minecraft.world.effect.*;
import java.util.*;

/** Transaction-scoped accounting. Rewards run after actuallyHurt has performed its health write. */
public final class DamageHooks {
    public static final String SHOT_KEY = "runic_races:stillleaf_shot";
    private record Attack(ServerPlayer player, Entity primary, float charge) {}
    private static final class Frame {
        final LivingEntity target;
        final DamageSource source;
        float healthBefore, acceptedAmount;
        boolean finalized, prepared;
        AbilityService.Session attackerSession;
        AbstractArrow shot;
        WardLedger.Prevention prevention;
        Frame(LivingEntity target, DamageSource source) { this.target = target; this.source = source; }
    }
    private static final ThreadLocal<ArrayDeque<Frame>> FRAMES = ThreadLocal.withInitial(ArrayDeque::new);
    private static final ThreadLocal<ArrayDeque<Attack>> ATTACKS = ThreadLocal.withInitial(ArrayDeque::new);
    private DamageHooks() {}
    public static void beginAttack(ServerPlayer p, Entity target, float charge) { ATTACKS.get().push(new Attack(p, target, charge)); }
    public static void endAttack(ServerPlayer p) {
        var stack = ATTACKS.get(); if (!stack.isEmpty() && stack.peek().player == p) stack.pop();
        if (stack.isEmpty()) ATTACKS.remove();
    }
    public static void begin(LivingEntity target, DamageSource source) { FRAMES.get().push(new Frame(target, source)); }
    public static boolean chargedPrimary(ServerPlayer p, LivingEntity target, DamageSource source, float threshold) {
        Attack attack = ATTACKS.get().peek();
        return !DamagePolicy.internal(source) && source.getDirectEntity() == p && source.getEntity() == p
                && attack != null && attack.player == p && attack.primary == target && attack.charge >= threshold;
    }
    public static float outgoing(LivingEntity target, DamageSource source, float amount) {
        Frame frame = FRAMES.get().peek();
        if (frame == null || frame.target != target || frame.source != source || amount <= 0 || DamagePolicy.internal(source)
                || !(source.getEntity() instanceof ServerPlayer p) || !TargetPolicy.aimed(p, target)) return amount;
        AbilityService.Session s = AbilityService.session(p);
        if (s == null || s.tuning == null || !RaceHelper.isRace(p, s.race) || !AbilityService.hasAbility(p, s.tuning.kind())) return amount;
        if (s.live(AbilityService.now(p)) && s.prepared && chargedPrimary(p, target, source, s.tuning.amount("min_charge"))) {
            AbilityKind kind = s.tuning.kind();
            if (kind == AbilityKind.COLOSSAN || kind == AbilityKind.SAURIAN || kind == AbilityKind.RETURNED && target.getUUID().equals(s.target)) {
                // Nested secondary damage cannot borrow the same prepared opportunity.
                if (FRAMES.get().stream().anyMatch(f -> f != frame && f.prepared && f.attackerSession == s)) return amount;
                frame.prepared = true; frame.attackerSession = s;
                return amount + s.tuning.amount("bonus");
            }
        }
        if (source.getDirectEntity() instanceof AbstractArrow arrow && s.tuning.kind() == AbilityKind.GROVE_ELF) {
            CompoundTag shot = arrow.getPersistentData().getCompound(SHOT_KEY);
            if (shot.hasUUID("generation") && shot.getUUID("generation").equals(s.generation)
                    && shot.hasUUID("owner") && shot.getUUID("owner").equals(p.getUUID())
                    && arrow.getOwner() == p && !shot.getBoolean("spent") && AbilityService.now(p) <= shot.getLong("expires")) {
                frame.shot = arrow; frame.attackerSession = s;
                return amount + Math.min(shot.getFloat("cap"), amount * shot.getFloat("fraction"));
            }
        }
        return amount;
    }
    public static float finalAmount(LivingEntity target, DamageSource source, float amount) {
        Frame frame = FRAMES.get().peek();
        if (frame == null || frame.target != target) return amount;
        frame.healthBefore = target.getHealth(); frame.finalized = true;
        frame.acceptedAmount = Float.isFinite(amount) ? Math.max(0, amount) : 0;
        if (frame.acceptedAmount <= 0 || target.level().isClientSide || !DamagePolicy.wardEligible(source)) return frame.acceptedAmount;
        WardLedger ledger = AbilityService.existingWards(target);
        if (ledger == null) return frame.acceptedAmount;
        long now = Integer.toUnsignedLong(target.getServer().getTickCount());
        frame.prevention = ledger.select(frame.acceptedAmount, now, kind -> switch (kind) {
            case DAWN, DOMINION -> true;
            case SHELL -> DamagePolicy.physical(source);
            case PRISM -> !DamagePolicy.internal(source) && (DamagePolicy.magic(source) || DamagePolicy.projectile(source));
        }).orElse(null);
        return frame.acceptedAmount - (frame.prevention == null ? 0 : frame.prevention.amount());
    }
    public static void finish(LivingEntity target) {
        var stack = FRAMES.get();
        if (stack.isEmpty() || stack.peek().target != target) return;
        Frame frame = stack.pop(); if (stack.isEmpty()) FRAMES.remove();
        if (target.level().isClientSide || !frame.finalized) return;
        float lost = DamageAccounting.healthLost(frame.healthBefore, target.getHealth(), frame.acceptedAmount);
        if (frame.prevention != null && frame.acceptedAmount > 0) {
            WardLedger ledger = AbilityService.existingWards(target);
            if (ledger != null) ledger.consume(frame.prevention);
            ServerPlayer wardOwner = target.getServer().getPlayerList().getPlayer(frame.prevention.ward().owner());
            if (wardOwner != null && wardOwner.level() == target.level())
                AbilityFeedback.ward(wardOwner, target, frame.prevention.ward().kind());
            if (frame.prevention.ward().kind() == WardLedger.Kind.PRISM && target instanceof ServerPlayer defender) {
                AbilityService.Session s = AbilityService.session(defender);
                if (s != null && s.tuning != null && s.tuning.kind() == AbilityKind.CRYSTAL_ONE) {
                    s.until = 0;
                    if (frame.prevention.amount() >= 1 && frame.source.getEntity() instanceof LivingEntity attacker
                            && TargetPolicy.aimed(defender, attacker) && defender.distanceToSqr(attacker) <= Math.pow(s.tuning.number("reply_range"), 2)
                            && TargetPolicy.visible(defender, attacker)) {
                        if (deal(defender, attacker, s.tuning.amount("reply_damage"), true) > 0)
                            AbilityFeedback.impact(defender, attacker.getBoundingBox().getCenter(), AbilityKind.CRYSTAL_ONE);
                    }
                    AbilityService.sync(defender, s);
                }
            }
        }
        if (lost > 0 && target instanceof ServerPlayer interrupted) {
            var cast = AbilityService.session(interrupted);
            if (cast != null && cast.tuning != null && cast.tuning.kind() == AbilityKind.WAILER && cast.windup > 0) {
                AbilityService.clearCast(interrupted, cast); AbilityService.sync(interrupted, cast);
                AbilityService.deny(interrupted, cast, "interrupted");
            }
        }
        if (lost <= 0 || DamagePolicy.internal(frame.source)) return;
        if (target instanceof ServerPlayer victim && frame.source.getEntity() instanceof LivingEntity aggressor
                && TargetPolicy.aimed(victim, aggressor)) {
            AbilityService.Session s = AbilityService.session(victim);
            if (s != null && s.tuning != null && s.tuning.kind() == AbilityKind.RETURNED) {
                s.aggressor = aggressor.getUUID(); s.aggressorAt = AbilityService.now(victim);
            }
        }
        if (!(frame.source.getEntity() instanceof ServerPlayer p) || !TargetPolicy.permitted(p, target)) return;
        AbilityService.Session s = frame.attackerSession;
        if (frame.prepared && s != null && s.prepared && s.live(AbilityService.now(p))) {
            s.prepared = false;
            if (s.tuning.kind() == AbilityKind.SAURIAN)
                AbilityService.control(p, target, MobEffects.MOVEMENT_SLOWDOWN, s.tuning.ticks("effect_ticks"), 0, false);
            else if (s.tuning.kind() == AbilityKind.COLOSSAN) AbilityService.control(p, target, null, 0, 0, true);
            AbilityFeedback.impact(p, target.getBoundingBox().getCenter(), s.tuning.kind()); AbilityService.sync(p, s);
        }
        if (frame.shot != null && s != null) {
            CompoundTag shot = frame.shot.getPersistentData().getCompound(SHOT_KEY);
            shot.putBoolean("spent", true);
            AbilityFeedback.impact(p, target.getBoundingBox().getCenter(), AbilityKind.GROVE_ELF);
            // Owner-only reveal: three seconds of small target cues, no globally visible Glowing potion.
            AbilityEvents.reveal(p, target, Math.min(60, shot.getInt("reveal_ticks")));
        }
        s = AbilityService.session(p);
        if (s != null && s.tuning != null && s.tuning.kind() == AbilityKind.NIGHTBORN && s.live(AbilityService.now(p))
                && s.charges > 0 && AbilityService.now(p) - s.lastFeed >= s.tuning.ticks("interval")
                && chargedPrimary(p, target, frame.source, s.tuning.amount("min_charge")) && TargetPolicy.canBleed(p, target)) {
            s.charges--; s.lastFeed = AbilityService.now(p);
            float offer = DamageAccounting.feedOffer(lost, s.tuning.amount("fraction"), s.tuning.amount("healing"), s.tuning.amount("budget") - s.offered);
            s.offered += offer;
            float restored = HealingBudget.heal(p, offer, s.tuning.amount("final_cap") - s.restored);
            s.restored += restored;
            if (restored > 0) {
                AbilityFeedback.drain(p, target);
                AbilityFeedback.healing(p, p, AbilityKind.NIGHTBORN);
            }
            AbilityService.sync(p, s);
        }
        // Existing venom and Blood Elf rewards use the same accepted-health accounting.
        com.otectus.runic_races.event.RacialEventHandler.onAcceptedMeleeDamage(p, target, frame.source, lost);
    }
    public static float deal(ServerPlayer p, LivingEntity target, float amount, boolean resonance) {
        if (!TargetPolicy.aimed(p, target) || amount <= 0 || !Float.isFinite(amount)) return 0;
        float before = target.getHealth();
        boolean accepted = target.hurt(new RacialDamageSource(p, resonance), amount);
        return accepted ? Math.max(0, before - target.getHealth()) : 0;
    }
    public static void clear() { FRAMES.remove(); ATTACKS.remove(); }
}
