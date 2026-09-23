package com.otectus.runic_races.common.state;

import java.util.HashMap;
import java.util.Map;
import java.util.function.Predicate;

/**
 * One player's race-state bitfield, assembled from its writers. Minecraft-free so the
 * aggregation rules are unit-testable; {@link RaceStateTracker} owns the Forge glue.
 * <ul>
 *   <li>Direct bits have a single writer and are set or cleared outright.</li>
 *   <li>Contributed bits are the OR over sources. A source reports its whole view of a
 *       mask; clearing a bit in one source never clears another source's bit.</li>
 * </ul>
 * {@link #takeTransitions()} compares the current aggregate with the last delivered one, so
 * any number of writes within a tick collapse into one net change.
 */
public final class FlagAggregate<S> {

    private int direct;
    private Map<S, Integer> contributions;
    private int delivered;

    public int effective() {
        int value = direct;
        if (contributions != null) {
            for (int bits : contributions.values()) value |= bits;
        }
        return value;
    }

    public int delivered() {
        return delivered;
    }

    public boolean hasContributions() {
        return contributions != null && !contributions.isEmpty();
    }

    /** Sets or clears single-writer bits; true when the stored value changed. */
    public boolean setDirect(int mask, boolean on) {
        int updated = on ? direct | mask : direct & ~mask;
        if (updated == direct) return false;
        direct = updated;
        return true;
    }

    /**
     * Records {@code source}'s view of {@code mask}: its bits in {@code bits} are on, its
     * other bits of {@code mask} off. Returns true when the stored contribution changed.
     */
    public boolean setContribution(S source, int mask, int bits) {
        Integer previous = contributions == null ? null : contributions.get(source);
        int updated = (previous == null ? 0 : previous & ~mask) | (bits & mask);
        if (previous == null ? updated == 0 : previous == updated) return false;
        if (updated == 0) {
            contributions.remove(source);
        } else {
            if (contributions == null) contributions = new HashMap<>(4);
            contributions.put(source, updated);
        }
        return true;
    }

    /** Forgets everything {@code source} contributed; true when it had contributed anything. */
    public boolean clearContribution(S source) {
        return contributions != null && contributions.remove(source) != null;
    }

    /** Forgets sources that fail {@code stillValid}; true when any was dropped. */
    public boolean retainSources(Predicate<S> stillValid) {
        return contributions != null && contributions.keySet().removeIf(source -> !stillValid.test(source));
    }

    /** Bits that differ from the last delivered aggregate; marks the current one delivered. */
    public int takeTransitions() {
        int current = effective();
        int changed = current ^ delivered;
        delivered = current;
        return changed;
    }
}
