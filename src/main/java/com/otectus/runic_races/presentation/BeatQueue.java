package com.otectus.runic_races.presentation;

import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

/**
 * Deterministic delay queue for presentation "beats" (delayed sfx/vfx specs).
 *
 * Deliberately Minecraft-free so it can be unit-tested under plain JUnit;
 * {@link PresentationScheduler} owns the Forge glue. Not thread-safe — call
 * only from the server thread, matching the rest of the presentation layer.
 */
public final class BeatQueue<T> {

    /** Delays are clamped to this range; anything longer is an authoring mistake. */
    public static final int MIN_DELAY_TICKS = 1;
    public static final int MAX_DELAY_TICKS = 100;

    private static final class Entry<T> {
        final UUID owner;
        final T payload;
        int ticksLeft;

        Entry(UUID owner, T payload, int ticksLeft) {
            this.owner = owner;
            this.payload = payload;
            this.ticksLeft = ticksLeft;
        }
    }

    // Insertion order doubles as delivery order for same-tick beats.
    private final List<Entry<T>> entries = new ArrayList<>();

    /** Queues a payload to be delivered after {@code delayTicks} (clamped to [1, 100]). */
    public void schedule(UUID owner, int delayTicks, T payload) {
        int delay = Math.max(MIN_DELAY_TICKS, Math.min(MAX_DELAY_TICKS, delayTicks));
        entries.add(new Entry<>(owner, payload, delay));
    }

    /**
     * Advances one tick; returns due payloads in the order they were scheduled.
     * One stable compaction pass: survivors slide down in place and the vacated
     * tail is cleared once, so a burst of simultaneous beats costs O(n) instead of
     * one array shift per removal. Nothing is allocated when no beat is due.
     */
    public List<T> tick() {
        List<T> due = null;
        int size = entries.size();
        int kept = 0;
        for (int i = 0; i < size; i++) {
            Entry<T> entry = entries.get(i);
            if (--entry.ticksLeft <= 0) {
                if (due == null) due = new ArrayList<>();
                due.add(entry.payload);
            } else {
                if (kept != i) entries.set(kept, entry);
                kept++;
            }
        }
        if (kept < size) entries.subList(kept, size).clear();
        return due == null ? List.of() : due;
    }

    /** Drops every pending beat belonging to {@code owner} (player died / logged out). */
    public void cancel(UUID owner) {
        entries.removeIf(entry -> entry.owner.equals(owner));
    }

    public void clear() {
        entries.clear();
    }

    public int size() {
        return entries.size();
    }
}
