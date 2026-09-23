package com.otectus.runic_races.ability;

import org.junit.jupiter.api.Test;
import java.util.UUID;
import static org.junit.jupiter.api.Assertions.*;
import static com.otectus.runic_races.ability.WardLedger.Kind.*;

class WardLedgerTest {
    private final UUID owner = new UUID(0,1);
    private WardLedger.Ward ward(WardLedger.Kind kind, float budget, float fraction, long expiry) {
        return new WardLedger.Ward(kind,owner,expiry,budget,fraction);
    }
    @Test void choosesOneStrongestCandidateAndDoesNotAddBudgets() {
        WardLedger ledger = new WardLedger();
        ledger.offer(ward(DAWN,4,1,120),0); ledger.offer(ward(SHELL,12,.5f,100),0);
        var prevention=ledger.select(10,1,k->true).orElseThrow();
        assertEquals(SHELL,prevention.ward().kind()); assertEquals(5,prevention.amount());
        ledger.consume(prevention);
        assertEquals(7,ledger.remaining(SHELL)); assertEquals(4,ledger.remaining(DAWN));
        var next=ledger.select(2,2,k->true).orElseThrow();
        assertEquals(DAWN,next.ward().kind()); assertEquals(2,next.amount());
    }
    @Test void tiedCandidatesAreDeterministicAndCannotDoubleConsume() {
        WardLedger ledger=new WardLedger();ledger.offer(ward(DAWN,4,1,120),0);ledger.offer(ward(DOMINION,4,1,100),0);
        var hit=ledger.select(2,0,k->true).orElseThrow();assertEquals(DAWN,hit.ward().kind());
        ledger.consume(hit);ledger.consume(hit);assertEquals(2,ledger.remaining(DAWN));
        assertEquals(4,ledger.remaining(DOMINION));
    }
    @Test void replacementDoesNotAddCapacityOrRemoveAnUnrelatedOwner() {
        WardLedger ledger=new WardLedger();ledger.offer(ward(DAWN,3,1,100),0);
        ledger.offer(new WardLedger.Ward(DAWN,new UUID(0,2),200,2,1),0);
        assertEquals(3,ledger.remaining(DAWN));
        ledger.offer(new WardLedger.Ward(DAWN,new UUID(0,2),120,4,1),0);
        ledger.removeOwner(owner);assertEquals(4,ledger.remaining(DAWN));
        ledger.expire(120);assertTrue(ledger.isEmpty());
    }
    @Test void rejectedDamageAndIneligibleCategoriesLeaveBudgetsIntact() {
        WardLedger ledger=new WardLedger();ledger.offer(ward(PRISM,4,.5f,120),0);
        assertTrue(ledger.select(0,1,k->true).isEmpty());
        assertTrue(ledger.select(Float.NaN,1,k->true).isEmpty());
        assertTrue(ledger.select(10,1,k->false).isEmpty());
        assertEquals(4,ledger.remaining(PRISM));
        var hit=ledger.select(1,1,k->true).orElseThrow();assertEquals(.5f,hit.amount());
        ledger.consume(hit);assertTrue(ledger.isEmpty(),"A prism consumes its opportunity even below the reply threshold");
    }
}
