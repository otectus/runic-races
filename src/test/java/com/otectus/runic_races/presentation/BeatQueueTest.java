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
    void burstDeliveryKeepsOrderAndSurvivors() {
        BeatQueue<String> queue = new BeatQueue<>();
        // Interleave due and pending beats so compaction must slide survivors past gaps.
        for (int i = 0; i < 10_000; i++) {
            queue.schedule(i % 2 == 0 ? ALICE : BOB, i % 3 == 0 ? 2 : 1, "beat" + i);
        }
        List<String> first = queue.tick();
        assertEquals(10_000 - 3_334, first.size());
        assertEquals("beat1", first.get(0));
        assertEquals("beat9998", first.get(first.size() - 1));
        assertEquals(3_334, queue.size());
        List<String> second = queue.tick();
        assertEquals("beat0", second.get(0));
        assertEquals("beat9999", second.get(second.size() - 1));
        for (int i = 1; i < second.size(); i++) {
            int previous = Integer.parseInt(second.get(i - 1).substring(4));
            assertTrue(Integer.parseInt(second.get(i).substring(4)) > previous, "same-tick order preserved");
        }
        assertEquals(0, queue.size());
    }

    @Test
    void cancelAfterPartialDeliveryReleasesPendingBeats() {
        BeatQueue<String> queue = new BeatQueue<>();
        queue.schedule(ALICE, 1, "a-now");
        queue.schedule(ALICE, 5, "a-later");
        queue.schedule(BOB, 5, "b-later");
        assertEquals(List.of("a-now"), queue.tick());
        queue.cancel(ALICE);
        assertEquals(1, queue.size());
        for (int i = 0; i < 3; i++) assertTrue(queue.tick().isEmpty());
        assertEquals(List.of("b-later"), queue.tick());
        assertEquals(0, queue.size());
    }

    @Test
    void schedulingWhileDeliveringDoesNotJoinTheCurrentBatch() {
        BeatQueue<String> queue = new BeatQueue<>();
        queue.schedule(ALICE, 1, "first");
        for (String beat : queue.tick()) {
            queue.schedule(ALICE, 1, beat + "-echo");
        }
        assertEquals(List.of("first-echo"), queue.tick());
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
