package com.otectus.runic_races.common.state;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * Aggregation contract behind the race-state HUD: several powers may report the same
 * flag (Canine's forest and taiga affinities both drive BIOME_HOME), and one reporting
 * "off" must never erase another's "on".
 */
class FlagAggregateTest {

    private static final int HOME = RaceStateFlags.BIOME_HOME.mask();
    private static final int HOSTILE = RaceStateFlags.BIOME_HOSTILE.mask();
    private static final int AFFINITY = HOME | HOSTILE;

    @Test
    void stationaryConflictingSourcesProduceNoTransitions() {
        FlagAggregate<String> flags = new FlagAggregate<>();
        // Standing in a forest that is not a taiga: forest says home, taiga says not.
        flags.setContribution("forest_home", AFFINITY, HOME);
        flags.setContribution("taiga_home", AFFINITY, 0);
        assertEquals(HOME, flags.takeTransitions(), "first evaluation turns the flag on once");
        for (int evaluation = 0; evaluation < 100; evaluation++) {
            // Either evaluation order, every check interval.
            if (evaluation % 2 == 0) {
                flags.setContribution("taiga_home", AFFINITY, 0);
                flags.setContribution("forest_home", AFFINITY, HOME);
            } else {
                flags.setContribution("forest_home", AFFINITY, HOME);
                flags.setContribution("taiga_home", AFFINITY, 0);
            }
            assertEquals(0, flags.takeTransitions(), "a stationary player must not flicker");
        }
        assertEquals(HOME, flags.effective());
    }

    @Test
    void removingOneSourceKeepsTheOthersContribution() {
        FlagAggregate<String> flags = new FlagAggregate<>();
        flags.setContribution("forest_home", AFFINITY, HOME);
        flags.setContribution("taiga_home", AFFINITY, HOME);
        flags.takeTransitions();
        assertTrue(flags.clearContribution("taiga_home"));
        assertEquals(HOME, flags.effective());
        assertEquals(0, flags.takeTransitions());
        assertTrue(flags.clearContribution("forest_home"));
        assertEquals(HOME, flags.takeTransitions(), "last source gone: one off-transition");
        assertFalse(flags.clearContribution("forest_home"), "clearing twice is a no-op");
    }

    @Test
    void contributionsOnlyTouchTheirOwnMask() {
        FlagAggregate<String> flags = new FlagAggregate<>();
        int night = RaceStateFlags.NIGHT_EMPOWERED.mask();
        flags.setContribution("scaling", night, night);
        flags.setContribution("scaling", AFFINITY, HOME);
        assertEquals(night | HOME, flags.effective(), "a second mask from the same source leaves the first alone");
        flags.setContribution("scaling", night, 0);
        assertEquals(HOME, flags.effective());
    }

    @Test
    void writesWithinOneTickCollapseToTheNetChange() {
        FlagAggregate<String> flags = new FlagAggregate<>();
        int sunlight = RaceStateFlags.SUNLIGHT_BURNING.mask();
        assertTrue(flags.setDirect(sunlight, true));
        assertTrue(flags.setDirect(sunlight, false));
        assertTrue(flags.setDirect(sunlight, true));
        assertFalse(flags.setDirect(sunlight, true), "unchanged direct write reports no change");
        assertEquals(sunlight, flags.takeTransitions());
        assertTrue(flags.setDirect(sunlight, false));
        assertTrue(flags.setDirect(sunlight, true));
        assertEquals(0, flags.takeTransitions(), "off-then-on inside one tick is no transition");
    }

    @Test
    void directAndContributedBitsCombine() {
        FlagAggregate<String> flags = new FlagAggregate<>();
        flags.setDirect(HOME, true);
        flags.setContribution("forest_home", AFFINITY, 0);
        assertEquals(HOME, flags.effective(), "a contribution of zero cannot clear a direct bit");
        flags.setDirect(HOME, false);
        flags.setContribution("forest_home", AFFINITY, HOSTILE);
        assertEquals(HOSTILE, flags.effective());
    }

    @Test
    void retainSourcesDropsOnlyInvalidOnes() {
        FlagAggregate<String> flags = new FlagAggregate<>();
        flags.setContribution("kept", AFFINITY, HOME);
        flags.setContribution("removed_by_reload", AFFINITY, HOSTILE);
        assertTrue(flags.retainSources("kept"::equals));
        assertEquals(HOME, flags.effective());
        assertFalse(flags.retainSources("kept"::equals));
    }

    @Test
    void zeroContributionFromUnknownSourceStoresNothing() {
        FlagAggregate<String> flags = new FlagAggregate<>();
        assertFalse(flags.setContribution("taiga_home", AFFINITY, 0));
        assertFalse(flags.hasContributions());
        assertEquals(0, flags.takeTransitions());
    }
}
