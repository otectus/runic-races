package com.otectus.runic_races.ability;

import com.otectus.runic_races.config.RRServerConfig;
import com.otectus.runic_races.util.Hostility;
import com.otectus.runic_races.util.RaceHelper;
import com.otectus.runic_races.race.RaceRegistry;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.*;
import net.minecraft.world.entity.decoration.ArmorStand;
import net.minecraft.world.entity.npc.AbstractVillager;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.ClipContext;
import net.minecraft.world.phys.HitResult;
import net.minecraft.world.phys.Vec3;
import java.util.*;

public final class TargetPolicy {
    private TargetPolicy() {}
    public static boolean aimed(ServerPlayer caster, LivingEntity target) {
        return target.isAlive() && permitted(caster, target);
    }
    /** Permission checks remain meaningful after an accepted lethal health write. */
    public static boolean permitted(ServerPlayer caster, LivingEntity target) {
        if (caster == target || target.isInvulnerable() || target.isSpectator()
                || target instanceof ArmorStand || Hostility.isProtectedAlly(caster, target)) return false;
        if (target instanceof Player other && (!caster.server.isPvpAllowed() || !caster.canHarmPlayer(other))) return false;
        return target.level() == caster.level();
    }
    public static boolean threat(ServerPlayer caster, LivingEntity target) {
        return aimed(caster, target) && (Hostility.isThreatTo(caster, target)
                || target instanceof Player && RRServerConfig.RACIAL_PVP_CONTROL.get());
    }
    public static boolean support(ServerPlayer caster, LivingEntity target) {
        if (!target.isAlive() || target.isSpectator() || target.level() != caster.level()) return false;
        if (target == caster || target.isAlliedTo(caster)) return true;
        if (target instanceof OwnableEntity owned && owned.getOwnerUUID() != null) {
            if (owned.getOwnerUUID().equals(caster.getUUID())) return true;
            var owner = caster.server.getPlayerList().getPlayer(owned.getOwnerUUID());
            return owner != null && owner.isAlliedTo(caster);
        }
        return false;
    }
    public static boolean canBleed(ServerPlayer caster, LivingEntity target) {
        return permitted(caster, target) && !(target instanceof AbstractVillager)
                && !(target instanceof OwnableEntity) && !target.getType().is(RacialTags.CANNOT_BLEED)
                && target.getMobType() != MobType.UNDEAD
                && !(target instanceof Player p && RaceHelper.getRaceName(p)
                .map(r -> "undead".equals(RaceRegistry.getFamily(r))).orElse(false));
    }
    public static boolean control(ServerPlayer caster, LivingEntity target) {
        return aimed(caster, target) && !target.getType().is(RacialTags.CONTROL_RESISTANT)
                && (!(target instanceof Player) || RRServerConfig.RACIAL_PVP_CONTROL.get());
    }
    public static boolean visible(LivingEntity viewer, LivingEntity target) {
        return visible(viewer, viewer.getEyePosition(), target.getBoundingBox().getCenter());
    }
    public static boolean visible(LivingEntity viewer, Vec3 from, Vec3 to) {
        return viewer.level().clip(new ClipContext(from, to, ClipContext.Block.COLLIDER, ClipContext.Fluid.NONE, viewer)).getType() == HitResult.Type.MISS;
    }
    public static List<LivingEntity> nearby(ServerPlayer caster, Vec3 center, double radius,
                                           java.util.function.Predicate<LivingEntity> predicate, int limit) {
        return caster.level().getEntitiesOfClass(LivingEntity.class, new net.minecraft.world.phys.AABB(center, center).inflate(radius),
                        e -> e.distanceToSqr(center) <= radius * radius && predicate.test(e))
                .stream().sorted(Comparator.<LivingEntity>comparingDouble(e -> e.distanceToSqr(center))
                        .thenComparing(Entity::getUUID)).limit(Math.min(6, Math.max(0, limit))).toList();
    }
}
