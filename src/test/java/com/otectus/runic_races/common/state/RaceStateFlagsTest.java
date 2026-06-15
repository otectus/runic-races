package com.otectus.runic_races.common.state;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * Pure bitfield tests for {@link RaceStateFlags} — no Minecraft on the classpath.
 * Guards against bit collisions (catastrophic: two states would share a rune) and
 * verifies set/clear round-trips after the OPEN_SKY addition (now 10 flags).
 */
class RaceStateFlagsTest {

    @Test
    void eachFlagOwnsExactlyOneUniqueBit() {
        int seen = 0;
        for (RaceStateFlags f : RaceStateFlags.values()) {
            assertNotEquals(0, f.mask(), f + " has a zero mask");
            assertEquals(1, Integer.bitCount(f.mask()), f + " must own exactly one bit");
            assertEquals(0, seen & f.mask(), f + " collides with an already-used bit");
            seen |= f.mask();
        }
        assertEquals(RaceStateFlags.values().length, Integer.bitCount(seen));
    }

    @Test
    void setThenClearReturnsToZero() {
        for (RaceStateFlags f : RaceStateFlags.values()) {
            int withFlag = f.set(0, true);
            assertTrue(f.isSet(withFlag), f + " should be set");
            int cleared = f.set(withFlag, false);
            assertFalse(f.isSet(cleared), f + " should be cleared");
            assertEquals(0, cleared);
        }
    }

    @Test
    void clearingOneFlagLeavesOthersUntouched() {
        int all = 0;
        for (RaceStateFlags f : RaceStateFlags.values()) {
            all = f.set(all, true);
        }
        int minusSun = RaceStateFlags.SUNLIGHT_BURNING.set(all, false);
        assertFalse(RaceStateFlags.SUNLIGHT_BURNING.isSet(minusSun));
        for (RaceStateFlags f : RaceStateFlags.values()) {
            if (f != RaceStateFlags.SUNLIGHT_BURNING) {
                assertTrue(f.isSet(minusSun), f + " should still be set");
            }
        }
    }

    @Test
    void ofCombinesOnlyTheGivenFlags() {
        int combo = RaceStateFlags.of(RaceStateFlags.OPEN_SKY, RaceStateFlags.TIGHT_SPACE);
        assertTrue(RaceStateFlags.OPEN_SKY.isSet(combo));
        assertTrue(RaceStateFlags.TIGHT_SPACE.isSet(combo));
        assertFalse(RaceStateFlags.BIOME_HOME.isSet(combo));
        assertFalse(RaceStateFlags.SUNLIGHT_BURNING.isSet(combo));
    }
}
