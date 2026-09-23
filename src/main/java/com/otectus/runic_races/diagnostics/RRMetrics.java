package com.otectus.runic_races.diagnostics;

/**
 * Fixed-category counters for attributing Runic Races' server cost, read with
 * {@code /runicraces diagnostics}. Always on: an increment is one array write, with no
 * per-player labels, per-event logging or timing around individual targets.
 *
 * Only the logical server thread increments these. A racing increment from another
 * thread would lose a count, never corrupt state, so the array is deliberately plain.
 */
public final class RRMetrics {

    public enum Counter {
        RACE_STATE_PACKETS("race-state packets"),
        RACE_STATE_TRANSITIONS("race-state flag transitions"),
        COOLDOWN_DECAY_STEPS("cooldown decay steps"),
        COOLDOWN_DELTA_PACKETS("cooldown delta packets"),
        COOLDOWN_DELTA_ENTRIES("cooldown delta entries"),
        COOLDOWN_FULL_SYNCS("full power-container syncs from RR decay"),
        PARTICLE_EMISSIONS("shaped particle emissions"),
        PARTICLE_POINTS("shaped particle points"),
        PARTICLE_PACKETS("particle packets to recipients"),
        BEATS_SCHEDULED("delayed presentation beats scheduled"),
        BEATS_DELIVERED("delayed presentation beats delivered"),
        INTEGRATION_SYNCS("integration player syncs"),
        MODIFIER_WRITES("RR modifier add/remove operations");

        private final String label;

        Counter(String label) {
            this.label = label;
        }

        public String label() {
            return label;
        }
    }

    private static final long[] COUNTS = new long[Counter.values().length];
    private static long sinceNanos = System.nanoTime();

    private RRMetrics() {}

    public static void add(Counter counter) {
        COUNTS[counter.ordinal()]++;
    }

    public static void add(Counter counter, long amount) {
        COUNTS[counter.ordinal()] += amount;
    }

    public static long get(Counter counter) {
        return COUNTS[counter.ordinal()];
    }

    /** Seconds covered by the current counts. */
    public static double windowSeconds() {
        return Math.max(1.0e-3, (System.nanoTime() - sinceNanos) / 1.0e9);
    }

    public static void reset() {
        java.util.Arrays.fill(COUNTS, 0L);
        sinceNanos = System.nanoTime();
    }
}
