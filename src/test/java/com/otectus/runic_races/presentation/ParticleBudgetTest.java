package com.otectus.runic_races.presentation;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;

class ParticleBudgetTest {
    @Test
    void densityZeroAndInvalidNumbersNeverEmit() {
        for (double density : new double[]{0, -1, Double.NaN, Double.POSITIVE_INFINITY}) {
            assertEquals(0, ParticleBudget.scale(20, density, 6));
        }
        assertEquals(0, ParticleBudget.scale(0, 1, 6));
    }

    @Test
    void settingsScaleBothDownAndUpWithBoundedGeometryFloor() {
        assertEquals(6, ParticleBudget.scale(20, 0.1, 6));
        assertEquals(3, ParticleBudget.scale(3, 0.1, 6));
        assertEquals(10, ParticleBudget.scale(20, 0.5, 6));
        assertEquals(20, ParticleBudget.scale(20, 1, 6));
        assertEquals(40, ParticleBudget.scale(20, 2, 6));
        assertEquals(40, ParticleBudget.scale(20, 99, 6));
    }

    @Test
    void crowdedDoubleDensityCastsAllRemainVisibleWithinTickBudget() {
        int budget = 192;
        for (int left = 12; left > 0; left--) {
            int count = ParticleBudget.fairShare(32, budget, left);
            assertEquals(16, count);
            budget -= count;
        }
        assertEquals(0, budget);
        assertEquals(0, ParticleBudget.fairShare(16, 0, 1));
    }
}
