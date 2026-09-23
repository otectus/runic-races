package com.otectus.runic_races.ability;

import org.junit.jupiter.api.Test;
import java.util.Map;
import static org.junit.jupiter.api.Assertions.*;

class CooldownLedgerTest {
    @Test void cloneRestartAndRelogRestoreRemainingTicksWithoutWorldTime() {
        CooldownLedger live=new CooldownLedger();live.put(AbilityKind.ASTRAL_ELF,800);
        for(int i=0;i<173;i++)live.tick();
        var saved=live.save();CooldownLedger clone=new CooldownLedger();clone.restore(saved);
        assertEquals(627,clone.get(AbilityKind.ASTRAL_ELF));
        CooldownLedger restart=new CooldownLedger();restart.restore(clone.save());
        assertEquals(saved,restart.save(),"Offline wall time cannot advance or reset the bank");
    }
    @Test void raceChangesKeepIndependentDebtsAndNeverTouchReaperKeys() {
        CooldownLedger live=new CooldownLedger();live.restore(Map.of(AbilityKind.RETURNED.cooldownId(),800,"runic_races:revenant_revival_cd",36000));
        live.put(AbilityKind.CHELON,900);live.tick();
        assertEquals(799,live.get(AbilityKind.RETURNED));assertEquals(899,live.get(AbilityKind.CHELON));
        assertFalse(live.save().containsKey("runic_races:revenant_revival_cd"));
    }
    @Test void negativeAndUnknownSavedValuesNeverGrantOrExtendState() {
        CooldownLedger live=new CooldownLedger();live.restore(Map.of(AbilityKind.BOVINE.cooldownId(),-1,"garbage",99999));
        assertTrue(live.isEmpty());live.put(AbilityKind.BOVINE,1);live.tick();assertTrue(live.isEmpty());
    }
}
