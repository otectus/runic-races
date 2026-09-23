package com.otectus.runic_races.ability;

/** Arithmetic shared by completed-hit accounting and focused tests. */
public final class DamageAccounting {
    private DamageAccounting() {}
    public static float healthLost(float before, float after, float acceptedAmount) {
        if (!Float.isFinite(before) || !Float.isFinite(after) || !Float.isFinite(acceptedAmount) || acceptedAmount <= 0) return 0;
        return Math.min(Math.max(0, before), Math.min(acceptedAmount, Math.max(0, before - after)));
    }
    public static float feedOffer(float lost, float fraction, float perHitCap, float remainingBudget) {
        if (!Float.isFinite(lost) || !Float.isFinite(fraction) || !Float.isFinite(perHitCap) || !Float.isFinite(remainingBudget) || lost <= 0) return 0;
        return Math.max(0, Math.min(remainingBudget, Math.min(perHitCap, lost * fraction)));
    }
}
