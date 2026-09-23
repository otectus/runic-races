package com.otectus.runic_races.presentation;

/** Shared density arithmetic; zero really disables emission, and double means double. */
public final class ParticleBudget {
    private ParticleBudget() {}

    public static int scale(int authored, double density, int floor) {
        if (authored <= 0 || !Double.isFinite(density) || density <= 0) return 0;
        return Math.max(Math.min(authored, floor), (int) Math.round(authored * Math.min(2, density)));
    }

    /** Every simultaneous effect gets a share instead of late arrivals aging out unseen. */
    public static int fairShare(int wanted, int remaining, int effectsLeft) {
        return Math.max(0, Math.min(wanted, remaining / Math.max(1, effectsLeft)));
    }
}
