package com.otectus.runic_races.util;

import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.Mob;
import net.minecraft.world.entity.TamableAnimal;
import net.minecraft.world.entity.monster.Enemy;
import net.minecraft.world.entity.player.Player;

/**
 * Shared server-side targeting rules for racial abilities. All offensive AoE
 * (afflict/glow/tremor, breath cones, Valen's shoulder check) routes through
 * here so "who counts as an enemy" stays consistent across every race.
 */
public final class Hostility {

    private Hostility() {}

    /**
     * Entities a racial ability must never harm or debuff: same-team entities,
     * players the caster cannot harm (PvP off / scoreboard teams), and tamed
     * animals owned by the caster or a caster ally — including a pet that is
     * mid-fight defending its owner.
     */
    public static boolean isProtectedAlly(LivingEntity caster, LivingEntity entity) {
        if (entity.isAlliedTo(caster)) return true;
        if (entity instanceof Player other && caster instanceof Player casterPlayer
                && !casterPlayer.canHarmPlayer(other)) {
            return true;
        }
        if (entity instanceof TamableAnimal pet && pet.isTame()) {
            LivingEntity owner = pet.getOwner();
            return owner == caster || (owner != null && owner.isAlliedTo(caster));
        }
        return false;
    }

    /**
     * Hostility heuristic for hostile-only effects: monsters ({@link Enemy}),
     * or a mob currently aggroed at the caster. A mob fighting someone else
     * (an iron golem on zombies, a wolf defending its own owner) is not the
     * caster's problem and is left alone.
     */
    public static boolean isThreatTo(LivingEntity caster, LivingEntity entity) {
        if (isProtectedAlly(caster, entity)) return false;
        if (entity instanceof Enemy) return true;
        return entity instanceof Mob mob && mob.getTarget() == caster;
    }
}
