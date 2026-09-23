package com.otectus.runic_races.ability;

import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.*;

class InputLatchTest {
    @Test void holdingOrReplayingCannotRecallOrToggle() {
        InputLatch latch = new InputLatch();
        assertTrue(latch.accept(1, true, 100));
        assertFalse(latch.accept(2, true, 200));
        assertFalse(latch.accept(2, false, 201));
        assertFalse(latch.accept(1, false, 202));
        assertFalse(latch.accept(3, true, 203));
        assertFalse(latch.accept(4, false, 204));
        assertTrue(latch.accept(5, true, 205));
    }
    @Test void deniedRequestsAreRateLimitedAndNeedANewEdge() {
        InputLatch latch = new InputLatch();
        assertTrue(latch.accept(10, true, 0));
        assertFalse(latch.accept(11, false, 0));
        assertFalse(latch.accept(12, true, 1));
        assertFalse(latch.accept(13, true, 10));
        assertFalse(latch.accept(14, false, 10));
        assertTrue(latch.accept(15, true, 11));
    }
}
