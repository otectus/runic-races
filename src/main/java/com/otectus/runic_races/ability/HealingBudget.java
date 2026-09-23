package com.otectus.runic_races.ability;

import net.minecraft.world.entity.LivingEntity;
import java.util.ArrayDeque;

/** Caps the final vanilla heal after every normal healing modifier, without applying them twice. */
public final class HealingBudget {
    private record Limit(LivingEntity entity, float health, float allowance) {}
    private static final ThreadLocal<ArrayDeque<Limit>> LIMITS = ThreadLocal.withInitial(ArrayDeque::new);
    private HealingBudget() {}
    public static float heal(LivingEntity entity, float offered, float finalAllowance) {
        if (!entity.isAlive() || !Float.isFinite(offered) || !Float.isFinite(finalAllowance) || offered <= 0 || finalAllowance <= 0) return 0;
        float before = entity.getHealth();
        LIMITS.get().push(new Limit(entity, before, finalAllowance));
        try { entity.heal(offered); }
        finally { LIMITS.get().pop(); if (LIMITS.get().isEmpty()) LIMITS.remove(); }
        return Math.max(0, entity.getHealth() - before);
    }
    public static float cap(LivingEntity entity, float requestedHealth) {
        var limit = LIMITS.get().peek();
        if (limit == null || limit.entity != entity) return requestedHealth;
        return Float.isNaN(requestedHealth) ? limit.health : Math.min(requestedHealth, limit.health + limit.allowance);
    }
}
