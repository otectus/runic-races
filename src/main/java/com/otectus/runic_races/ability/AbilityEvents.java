package com.otectus.runic_races.ability;

import com.otectus.runic_races.RunicRacesMod;
import com.otectus.runic_races.util.RaceHelper;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.tags.DamageTypeTags;
import net.minecraft.world.damagesource.DamageTypes;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.projectile.AbstractArrow;
import net.minecraft.world.item.Items;
import net.minecraft.world.item.PotionItem;
import net.minecraft.world.item.enchantment.EnchantmentHelper;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.nbt.CompoundTag;
import net.minecraftforge.event.*;
import net.minecraftforge.event.entity.*;
import net.minecraftforge.event.entity.living.*;
import net.minecraftforge.event.entity.player.*;
import net.minecraftforge.event.level.BlockEvent;
import net.minecraftforge.event.server.*;
import net.minecraftforge.eventbus.api.*;
import net.minecraftforge.fml.common.Mod;
import java.util.*;

@Mod.EventBusSubscriber(modid = RunicRacesMod.MOD_ID)
public final class AbilityEvents {
    private record Reveal(UUID target, UUID generation, long expires) {}
    private static final Map<UUID, Reveal> REVEALS = new HashMap<>();
    private static final int MAX_ARROW_TRAILS_PER_OWNER = 4;
    private static final class ArrowTrail {
        final UUID arrow, generation;
        final long expires;
        net.minecraft.world.phys.Vec3 previous;
        ArrowTrail(AbstractArrow arrow, UUID generation, long expires) {
            this.arrow = arrow.getUUID(); this.generation = generation; this.expires = expires;
            previous = arrow.position();
        }
    }
    private static final Map<UUID, ArrayDeque<ArrowTrail>> ARROW_TRAILS = new HashMap<>();
    private AbilityEvents() {}
    public static void reveal(ServerPlayer p, LivingEntity e, int ticks) {
        var session = AbilityService.session(p);
        if (session != null) REVEALS.put(p.getUUID(), new Reveal(e.getUUID(), session.generation, AbilityService.now(p) + ticks));
    }
    @SubscribeEvent public static void tick(TickEvent.PlayerTickEvent e) {
        if (e.phase != TickEvent.Phase.END || !(e.player instanceof ServerPlayer p)) return;
        AbilityService.tick(p);
        if (p.tickCount % 2 == 0) arrowTrails(p);
        Reveal reveal = REVEALS.get(p.getUUID());
        if (reveal != null) {
            LivingEntity target = AbilityService.entity(p, reveal.target);
            var session = AbilityService.session(p);
            if (reveal.expires <= AbilityService.now(p) || target == null || session == null
                    || !reveal.generation.equals(session.generation) || !RaceHelper.isRace(p, "grove_elf")) REVEALS.remove(p.getUUID());
            else if (p.tickCount % 10 == 0 && p.distanceToSqr(target) <= 4096 && TargetPolicy.visible(p, target))
                AbilityService.ownerCue(p, target.getBoundingBox().getCenter(), "reveal");
        }
    }
    @SubscribeEvent public static void serverTick(TickEvent.ServerTickEvent e) {
        if (e.phase == TickEvent.Phase.END) AbilityService.serverTick(Integer.toUnsignedLong(e.getServer().getTickCount()));
    }
    @SubscribeEvent public static void stopped(ServerStoppedEvent e) { AbilityService.stop(); REVEALS.clear(); ARROW_TRAILS.clear(); pendingBreaks.clear(); }
    @SubscribeEvent public static void stopping(ServerStoppingEvent e) { for (ServerPlayer p : e.getServer().getPlayerList().getPlayers()) AbilityService.clear(p, true); }
    @SubscribeEvent public static void logout(PlayerEvent.PlayerLoggedOutEvent e) { if (e.getEntity() instanceof ServerPlayer p) { AbilityService.clear(p, true); REVEALS.remove(p.getUUID()); ARROW_TRAILS.remove(p.getUUID()); } }
    @SubscribeEvent public static void dimension(PlayerEvent.PlayerChangedDimensionEvent e) { if (e.getEntity() instanceof ServerPlayer p) { AbilityService.clear(p, false); REVEALS.remove(p.getUUID()); ARROW_TRAILS.remove(p.getUUID()); } }
    @SubscribeEvent(priority = EventPriority.LOWEST) public static void death(LivingDeathEvent e) { if (!e.isCanceled() && e.getEntity() instanceof ServerPlayer p) { AbilityService.clear(p, false); REVEALS.remove(p.getUUID()); ARROW_TRAILS.remove(p.getUUID()); } }
    @SubscribeEvent public static void clone(PlayerEvent.Clone e) {
        if (!(e.getOriginal() instanceof ServerPlayer old) || !(e.getEntity() instanceof ServerPlayer next)) return;
        AbilityService.save(old);
        if (old.getPersistentData().contains(AbilityService.SAVE_KEY)) next.getPersistentData().put(AbilityService.SAVE_KEY, old.getPersistentData().getCompound(AbilityService.SAVE_KEY).copy());
        AbilityService.clear(old, true); REVEALS.remove(old.getUUID()); ARROW_TRAILS.remove(old.getUUID());
    }
    @SubscribeEvent public static void reload(OnDatapackSyncEvent e) {
        if (e.getPlayer() == null) {
            REVEALS.clear(); ARROW_TRAILS.clear(); pendingBreaks.clear(); AbilityService.reload(e.getPlayerList().getServer());
        }
    }
    @SubscribeEvent(priority = EventPriority.HIGH) public static void damage(LivingHurtEvent e) {
        if (e.getEntity().level().isClientSide || e.getAmount() <= 0) return;
        if (e.getSource().getEntity() instanceof ServerPlayer attacker && AbilityService.withdrawn(attacker)) { e.setCanceled(true); return; }
        float damage = DamageHooks.outgoing(e.getEntity(), e.getSource(), e.getAmount());
        if (e.getEntity() instanceof ServerPlayer p && !DamagePolicy.bypass(e.getSource())) {
            var s = AbilityService.session(p);
            if (s != null && RaceHelper.isRace(p, s.race)) {
                if (DamagePolicy.magic(e.getSource())) damage *= 1 - Math.min(1, s.trait("magic_reduction"));
                else if (DamagePolicy.physical(e.getSource())) damage *= 1 + s.trait("physical_extra");
                if (DamagePolicy.projectile(e.getSource())) damage *= 1 + s.trait("projectile_extra");
                if (e.getSource().is(DamageTypeTags.IS_EXPLOSION)) damage *= 1 + s.trait("explosion_extra");
                if (e.getSource().is(DamageTypeTags.IS_FIRE)) damage *= 1 + s.trait("fire_extra");
                if (e.getSource().is(DamageTypeTags.IS_FREEZING) && s.cold) damage *= 1 + s.trait("cold_freeze_extra");
                if (s.trait("sun_extra") > 0 && RacialEnvironment.sunlight(p)) damage *= 1 + s.trait("sun_extra");
            }
        }
        e.setAmount(damage);
    }
    @SubscribeEvent public static void fall(LivingFallEvent e) {
        if (e.getEntity() instanceof ServerPlayer p) { var s = AbilityService.session(p); if (s != null) e.setDamageMultiplier(e.getDamageMultiplier() * (float)(1 - Math.min(1, s.trait("fall_reduction")))); }
    }
    @SubscribeEvent public static void underbrush(LivingAttackEvent e) {
        if (e.getEntity() instanceof ServerPlayer p && RaceHelper.isRace(p, "grove_elf") && e.getSource().is(DamageTypes.SWEET_BERRY_BUSH)) e.setCanceled(true);
    }
    @SubscribeEvent(priority = EventPriority.LOWEST) public static void arrow(EntityJoinLevelEvent e) {
        if (e.isCanceled() || e.getLevel().isClientSide || e.loadedFromDisk() || !(e.getEntity() instanceof AbstractArrow arrow) || !(arrow.getOwner() instanceof ServerPlayer p)) return;
        var s = AbilityService.session(p);
        if (s == null || s.tuning == null || s.tuning.kind() != AbilityKind.GROVE_ELF || !s.prepared || !s.live(AbilityService.now(p))
                || !(arrow.isCritArrow() || arrow.shotFromCrossbow())
                || java.util.stream.Stream.of(p.getMainHandItem(), p.getOffhandItem()).noneMatch(stack ->
                    stack.getItem() instanceof net.minecraft.world.item.BowItem || stack.getItem() instanceof net.minecraft.world.item.CrossbowItem)) return;
        CompoundTag shot = new CompoundTag(); shot.putUUID("owner", p.getUUID()); shot.putUUID("generation", s.generation);
        shot.putFloat("fraction", s.tuning.amount("fraction")); shot.putFloat("cap", s.tuning.amount("bonus"));
        shot.putInt("reveal_ticks", s.tuning.ticks("effect_ticks")); shot.putLong("expires", AbilityService.now(p) + 1200);
        arrow.getPersistentData().put(DamageHooks.SHOT_KEY, shot);
        ArrayDeque<ArrowTrail> trails = ARROW_TRAILS.computeIfAbsent(p.getUUID(), ignored -> new ArrayDeque<>());
        while (trails.size() >= MAX_ARROW_TRAILS_PER_OWNER) trails.removeFirst();
        trails.addLast(new ArrowTrail(arrow, s.generation, shot.getLong("expires")));
        s.prepared = false; s.until = 0; AbilityService.sync(p, s);
    }
    private static void arrowTrails(ServerPlayer p) {
        ArrayDeque<ArrowTrail> trails = ARROW_TRAILS.get(p.getUUID());
        if (trails == null) return;
        var s = AbilityService.session(p);
        if (!p.isAlive() || s == null || !RaceHelper.isRace(p, "grove_elf")) {
            ARROW_TRAILS.remove(p.getUUID()); return;
        }
        var iterator = trails.iterator();
        while (iterator.hasNext()) {
            ArrowTrail trail = iterator.next();
            var entity = p.serverLevel().getEntity(trail.arrow);
            if (trail.expires <= AbilityService.now(p) || !trail.generation.equals(s.generation)
                    || !(entity instanceof AbstractArrow arrow) || !arrow.isAlive() || arrow.getOwner() != p
                    || arrow.getPersistentData().getCompound(DamageHooks.SHOT_KEY).getBoolean("spent")) {
                iterator.remove(); continue;
            }
            var position = arrow.position();
            if (position.distanceToSqr(trail.previous) < 1.0e-6 && arrow.getDeltaMovement().lengthSqr() < 1.0e-6) {
                iterator.remove(); continue;
            }
            AbilityFeedback.arrow(p, trail.previous, position);
            trail.previous = position;
        }
        if (trails.isEmpty()) ARROW_TRAILS.remove(p.getUUID());
    }
    @SubscribeEvent public static void breakSpeed(PlayerEvent.BreakSpeed e) {
        if (!(e.getEntity() instanceof ServerPlayer p) || e.getPosition().isEmpty()) return;
        if (AbilityService.withdrawn(p)) { e.setCanceled(true); return; }
        var s = AbilityService.session(p); if (s == null || s.tuning == null) return;
        if (s.tuning.kind() == AbilityKind.TIDE_ELF && p.isEyeInFluid(net.minecraft.tags.FluidTags.WATER)
                && !EnchantmentHelper.hasAquaAffinity(p) && e.getState().canHarvestBlock(p.level(), e.getPosition().get(), p)) e.setNewSpeed(e.getNewSpeed() * 5);
        if (s.tuning.kind() == AbilityKind.MOUNTAIN_ONE && s.live(AbilityService.now(p)) && s.charges > 0 && e.getState().is(RacialTags.QUARRY)
                && e.getState().getDestroySpeed(p.level(), e.getPosition().get()) >= 0 && e.getState().canHarvestBlock(p.level(), e.getPosition().get(), p))
            e.setNewSpeed(e.getNewSpeed() * (1 + s.tuning.amount("fraction")));
    }
    @SubscribeEvent(priority = EventPriority.LOWEST) public static void broken(BlockEvent.BreakEvent e) {
        if (e.isCanceled() || !(e.getPlayer() instanceof ServerPlayer p)) return;
        var s = AbilityService.session(p);
        if (s != null && s.tuning != null && s.tuning.kind() == AbilityKind.MOUNTAIN_ONE && s.live(AbilityService.now(p))
                && e.getState().is(RacialTags.QUARRY) && e.getState().getDestroySpeed(p.level(), e.getPos()) >= 0 && e.getState().canHarvestBlock(p.level(), e.getPos(), p)) {
            // Confirm the block was actually removed at the next server tick; canceled/failed breaks do not count.
            if (s.charges > 0 && pendingBreaks.keySet().stream().filter(key -> key.player.equals(p.getUUID())).limit(10).count() < 10)
                pendingBreaks.put(new BreakKey(p.getUUID(), e.getPos().immutable()), new PendingBreak(p.serverLevel().dimension(), e.getState(), AbilityService.now(p), s.generation));
        }
    }
    private record BreakKey(UUID player, net.minecraft.core.BlockPos pos) {}
    private record PendingBreak(net.minecraft.resources.ResourceKey<net.minecraft.world.level.Level> dimension, net.minecraft.world.level.block.state.BlockState state, long tick, UUID generation) {}
    private static final Map<BreakKey, PendingBreak> pendingBreaks = new HashMap<>();
    @SubscribeEvent public static void verifyBreaks(TickEvent.ServerTickEvent e) {
        if (e.phase != TickEvent.Phase.END) return;
        var it = pendingBreaks.entrySet().iterator();
        while (it.hasNext()) {
            var entry = it.next(); var data = entry.getValue();
            if (data.tick >= Integer.toUnsignedLong(e.getServer().getTickCount())) continue;
            it.remove(); ServerPlayer p = e.getServer().getPlayerList().getPlayer(entry.getKey().player);
            if (p == null || p.level().dimension() != data.dimension || !p.level().hasChunkAt(entry.getKey().pos)) continue;
            var s = AbilityService.session(p);
            if (s != null && s.tuning != null && s.tuning.kind() == AbilityKind.MOUNTAIN_ONE && s.live(AbilityService.now(p))
                    && s.generation.equals(data.generation) && s.charges > 0 && !p.level().getBlockState(entry.getKey().pos).equals(data.state)) {
                s.charges--; if (s.charges == 0) s.until = 0; AbilityFeedback.impact(p, entry.getKey().pos.getCenter(), AbilityKind.MOUNTAIN_ONE);
            }
        }
    }
    @SubscribeEvent public static void consumed(LivingEntityUseItemEvent.Finish e) {
        if (!(e.getEntity() instanceof ServerPlayer p)) return;
        var s = AbilityService.session(p); if (s == null || s.tuning == null) return;
        if (s.tuning.kind() == AbilityKind.TIDE_ELF && e.getItem().getItem() instanceof PotionItem) { s.dryTicks = 0; s.dry = false; AbilityService.sync(p, s); }
        if (s.tuning.kind() == AbilityKind.MOSS_ONE && e.getItem().is(RacialTags.MUSHROOM_FOOD) && e.getItem().isEdible()) {
            var food = p.getFoodData(); food.setFoodLevel(Math.min(20, food.getFoodLevel() + (int)s.trait("food_bonus")));
            food.setSaturation(Math.min(food.getFoodLevel(), food.getSaturationLevel() + (float)s.trait("saturation_bonus")));
        }
    }
    @SubscribeEvent public static void interaction(PlayerInteractEvent e) {
        if (e.getEntity() instanceof ServerPlayer p && AbilityService.withdrawn(p) && e.isCancelable()) e.setCanceled(true);
    }
    @SubscribeEvent public static void use(LivingEntityUseItemEvent.Start e) { if (e.getEntity() instanceof ServerPlayer p && AbilityService.withdrawn(p)) e.setCanceled(true); }
    @SubscribeEvent public static void attack(AttackEntityEvent e) { if (e.getEntity() instanceof ServerPlayer p && AbilityService.withdrawn(p)) e.setCanceled(true); }
}
