package com.otectus.runic_races;

import com.otectus.runic_races.race.RaceDefinition;
import com.otectus.runic_races.race.RaceRegistry;
import org.junit.jupiter.api.Test;

import java.util.ArrayList;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * Pehkui sanity: every race scale must sit inside the intended envelope
 * [0.4, 1.4]. Below that, hitboxes stop clearing slabs and cameras clip;
 * above it, players stop fitting through doors and 2-block tunnels.
 * (Intended extremes: Sprite 0.45, Terra Drake 1.30.)
 */
class RaceScaleRangeTest {

    private static final float MIN_SCALE = 0.4f;
    private static final float MAX_SCALE = 1.4f;

    @Test
    void everyRaceScaleIsWithinTheIntendedEnvelope() {
        List<String> problems = new ArrayList<>();
        for (RaceDefinition race : RaceRegistry.allRaces()) {
            if (race.scale() < MIN_SCALE || race.scale() > MAX_SCALE) {
                problems.add(race.name() + " scale " + race.scale()
                        + " outside [" + MIN_SCALE + ", " + MAX_SCALE + "]");
            }
        }
        assertTrue(problems.isEmpty(), String.join("\n", problems));
    }
}
