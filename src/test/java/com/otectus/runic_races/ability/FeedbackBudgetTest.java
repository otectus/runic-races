package com.otectus.runic_races.ability;

import org.junit.jupiter.api.Test;

import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertEquals;

class FeedbackBudgetTest {
    private static final UUID ALICE = new UUID(0, 1);
    private static final UUID BOB = new UUID(0, 2);

    @Test
    void crowdedTargetAndNestedHitFeedbackShareTheOwnersTickBudget() {
        FeedbackBudget budget = new FeedbackBudget(160);
        int emitted = 0;
        for (int target = 0; target < 200; target++) emitted += budget.reserve(ALICE, 42, 24);
        assertEquals(160, emitted);
        assertEquals(0, budget.reserve(ALICE, 42, 36));
        assertEquals(36, budget.reserve(BOB, 42, 36));
    }

    @Test
    void nextTickAndServerStopReleaseBudget() {
        FeedbackBudget budget = new FeedbackBudget(160);
        assertEquals(160, budget.reserve(ALICE, 42, 200));
        assertEquals(24, budget.reserve(ALICE, 43, 24));
        budget.clear();
        assertEquals(160, budget.reserve(ALICE, 43, 200));
    }

    @Test
    void disabledOrInvalidRequestsCannotConsumeBudget() {
        FeedbackBudget budget = new FeedbackBudget(160);
        assertEquals(0, budget.reserve(ALICE, 42, 0));
        assertEquals(0, budget.reserve(ALICE, 42, -5));
        assertEquals(160, budget.reserve(ALICE, 42, Integer.MAX_VALUE));
    }
}
