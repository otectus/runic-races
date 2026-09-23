package com.otectus.runic_races.ability;

import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.*;

class DamageAccountingTest {
    @Test void proposalAbsorptionAndCancellationAreNotHealthLoss() {
        assertEquals(0,DamageAccounting.healthLost(20,20,100));
        assertEquals(0,DamageAccounting.healthLost(20,18,0));
        assertEquals(0,DamageAccounting.healthLost(10,12,2));
        assertEquals(0,DamageAccounting.healthLost(20,0,Float.NaN));
    }
    @Test void overkillAndAmplificationCannotExceedFeedingCaps() {
        float lost=DamageAccounting.healthLost(3,0,1000);assertEquals(3,lost);
        assertEquals(.75f,DamageAccounting.feedOffer(lost,.25f,2,6));
        assertEquals(2,DamageAccounting.feedOffer(1000,.25f,2,6));
        assertEquals(.5f,DamageAccounting.feedOffer(1000,.25f,2,.5f));
        assertEquals(0,DamageAccounting.feedOffer(1000,.25f,2,0));
    }
}
