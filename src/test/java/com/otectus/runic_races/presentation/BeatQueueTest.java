package com.otectus.runic_races.presentation;

import org.junit.jupiter.api.Test;

import java.util.List;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * Locks in the delivery contract of the presentation beat queue: delays clamp
 * to [1, 100], beats fire on their exact tick in scheduling order, and
 * cancellation drops only the cancelled owner's beats.
 */
class BeatQueueTest {

    private static final UUID ALICE = UUID.fromString("00000000-0000-0000-0000-000000000001");
    private static final UUID BOB = UUID.fromString("00000000-0000-0000-0000-000000000002");

    @Test
    void zeroAndNegativeDelaysClampToOneTick() {
        BeatQueue<String> queue = new BeatQueue<>();
        queue.schedule(ALICE, 0, "zero");
        queue.schedule(ALICE, -5, "negative");
        assertEquals(List.of("zero", "negative"), queue.tick(),
                "clamped beats fire on the first tick after scheduling");
        assertEquals(0, queue.size());
    }

    @Test
    void oversizedDelaysClampToMaximum() {
        BeatQueue<String> queue = new BeatQueue<>();
        queue.schedule(ALICE, 10_000, "late");
        for (int i = 0; i < BeatQueue.MAX_DELAY_TICKS - 1; i++) {
            assertTrue(queue.tick().isEmpty(), "not due at tick " + (i + 1));
        }
        assertEquals(List.of("late"), queue.tick(), "due exactly at MAX_DELAY_TICKS");
    }

    @Test
    void beatsFireOnTheirExactTick() {
        BeatQueue<String> queue = new BeatQueue<>();
        queue.schedule(ALICE, 3, "third");
        queue.schedule(ALICE, 1, "first");
        assertEquals(List.of("first"), queue.tick());
        assertTrue(queue.tick().isEmpty());
        assertEquals(List.of("third"), queue.tick());
    }

    @Test
    void sameTickBeatsDeliverInSchedulingOrder() {
        BeatQueue<String> queue = new BeatQueue<>();
        queue.schedule(BOB, 2, "b1");
        queue.schedule(ALICE, 2, "a1");
        queue.schedule(BOB, 2, "b2");
        queue.tick();
        assertEquals(List.of("b1", "a1", "b2"), queue.tick());
    }

    @Test
    void cancelDropsOnlyThatOwner() {
        BeatQueue<String> queue = new BeatQueue<>();
        queue.schedule(ALICE, 2, "alice");
        queue.schedule(BOB, 2, "bob");
        queue.cancel(ALICE);
        assertEquals(1, queue.size());
        queue.tick();
        assertEquals(List.of("bob"), queue.tick());
    }

    @Test
    void clearDropsEverything() {
        BeatQueue<String> queue = new BeatQueue<>();
        queue.schedule(ALICE, 1, "a");
        queue.schedule(BOB, 50, "b");
        queue.clear();
        assertEquals(0, queue.size());
        assertTrue(queue.tick().isEmpty());
    }
}
