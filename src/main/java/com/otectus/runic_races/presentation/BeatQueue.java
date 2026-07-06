package com.otectus.runic_races.presentation;

import java.util.ArrayList;
import java.util.Iterator;
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

    /** Advances one tick; returns due payloads in the order they were scheduled. */
    public List<T> tick() {
        List<T> due = new ArrayList<>();
        Iterator<Entry<T>> it = entries.iterator();
        while (it.hasNext()) {
            Entry<T> entry = it.next();
            if (--entry.ticksLeft <= 0) {
                due.add(entry.payload);
                it.remove();
            }
        }
        return due;
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
