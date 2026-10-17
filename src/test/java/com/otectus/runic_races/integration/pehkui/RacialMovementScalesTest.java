package com.otectus.runic_races.integration.pehkui;

import com.otectus.runic_races.race.RaceRegistry;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

class RacialMovementScalesTest {
    @Test
    void everyShortRaceClearsOneBlockWithoutTheOldHighJump() {
        for (var race : RaceRegistry.allRaces()) {
            if (race.scale() >= 1) continue;
            double rise = jumpRise(.42 * RacialMovementScales.jumpMultiplier(race.scale(), true), race.scale());
            assertTrue(rise > 1.05 && rise <= 1.26, race.name() + " rises " + rise + " blocks");
        }
        // Reproduce the report: the old inverse-size launch sent a Sprite over
        // two blocks, then Pehkui counted the return fall in scaled units.
        double oldRise = jumpRise(.42 / .45, .45);
        assertTrue(oldRise > 2);
        assertTrue(oldRise / .45 > 3);
    }

    @Test
    void toggleOffUsesNativeScaledJumpAndBodyOnlyRacesNeedNoCorrection() {
        for (var race : RaceRegistry.allRaces()) {
            assertEquals(1f, RacialMovementScales.jumpMultiplier(race.scale(), false));
        }
        assertEquals(1f, RacialMovementScales.jumpMultiplier(1f, true));
        assertEquals(1f, RacialMovementScales.jumpMultiplier(1.3f, true));
        assertEquals(1.2522, jumpRise(.42, 1), .0001);
    }

    @Test
    void normalJumpsAndSmallStepDownsAreSafeButRealFallsStillHurt() {
        for (var race : RaceRegistry.allRaces()) {
            if (race.scale() >= 1) continue;
            float scale = race.scale();
            double fallScale = RacialMovementScales.fallMultiplier(scale) / scale;
            double rise = jumpRise(.42 * RacialMovementScales.jumpMultiplier(scale, true), scale);
            assertEquals(0, vanillaFallDamage(rise * fallScale), race.name());
            assertEquals(0, vanillaFallDamage((rise + 1) * fallScale), race.name());
            assertEquals(1, vanillaFallDamage(4 * fallScale), race.name());
            assertEquals(7, vanillaFallDamage(10 * fallScale), race.name());
        }
    }

    @Test
    void externalMotionJumpAndFallContributionsRemainMultiplicative() {
        float race = .5f;
        double externalMotion = .8, externalFall = 1.2, externalJump = 1.4;
        double falling = externalFall / (externalMotion * race) * RacialMovementScales.fallMultiplier(race);
        assertEquals(externalFall / externalMotion, falling, 1e-6);
        double jump = externalJump * RacialMovementScales.jumpMultiplier(race, true);
        assertEquals(externalJump / Math.sqrt(race), jump, 1e-6);
        // Compensation scales the base jump, with no cap on legitimate Jump Boost.
        assertTrue(jumpRise(.42 * jump + .2, race) > jumpRise(.42 * jump, race));
    }

    @Test
    void fallCorrectionIsIndependentOfJumpToggleAndLeavesTallRacesAlone() {
        assertEquals(.45f, RacialMovementScales.fallMultiplier(.45f));
        assertEquals(1f, RacialMovementScales.fallMultiplier(1f));
        assertEquals(1f, RacialMovementScales.fallMultiplier(1.3f));
    }

    @Test
    void invalidScalesCannotProduceAnInvalidModifier() {
        for (float scale : new float[]{0, -1, Float.NaN, Float.POSITIVE_INFINITY, Float.NEGATIVE_INFINITY}) {
            assertEquals(1f, RacialMovementScales.jumpMultiplier(scale, true));
            assertEquals(1f, RacialMovementScales.fallMultiplier(scale));
        }
    }

    /** Vanilla 1.20.1 airborne movement: move, then gravity and vertical drag. */
    private static double jumpRise(double velocity, double motionScale) {
        double rise = 0;
        for (int tick = 0; velocity > 0 && tick < 200; tick++) {
            rise += velocity * motionScale;
            velocity = (velocity - .08) * .98;
        }
        return rise;
    }

    private static int vanillaFallDamage(double distance) {
        return Math.max(0, (int) Math.ceil(distance - 3));
    }
}
