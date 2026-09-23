package com.otectus.runic_races.gametest;

import com.otectus.runic_races.ability.*;
import io.redspace.ironsspellbooks.api.events.*;
import io.redspace.ironsspellbooks.api.registry.SpellRegistry;
import io.redspace.ironsspellbooks.api.magic.MagicData;
import io.redspace.ironsspellbooks.api.spells.CastSource;
import io.redspace.ironsspellbooks.damage.SpellDamageSource;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.server.level.ServerPlayer;
import net.minecraftforge.common.MinecraftForge;

public final class IronsProbe {
    public static void affinity(GameTestHelper h, ServerPlayer p) {
        var spell = SpellRegistry.FIREBOLT_SPELL.get();
        var source = SpellDamageSource.source(p, spell);
        var event = new SpellDamageEvent(p, 20, source); MinecraftForge.EVENT_BUS.post(event);
        h.assertTrue(Math.abs(event.getAmount() - 21) < .001, "Auroran's explicit spell-damage affinity failed");
        h.assertTrue(DamagePolicy.magic(source), "Verified Iron's damage type was not classified as magic");
    }
    public static void shell(GameTestHelper h, ServerPlayer p) {
        var spell = SpellRegistry.FIRE_BREATH_SPELL.get();
        var data = MagicData.getPlayerMagicData(p);
        // Normal login initializes synchronized spell state before the first cast.
        data.setServerPlayer(p); data.getSyncedData();
        data.initiateCast(spell, 1, 100, CastSource.SPELLBOOK, "mainhand");
        h.assertTrue(data.isCasting(), "Channel fixture did not begin casting");
        AbilityService.input(p, 1, true);
        h.assertTrue(AbilityService.withdrawn(p) && !data.isCasting(), "Shellfast failed to interrupt the existing channel");
        var cast = new SpellPreCastEvent(p, spell.getSpellId(), 1, spell.getSchoolType(), CastSource.SPELLBOOK);
        MinecraftForge.EVENT_BUS.post(cast);
        h.assertTrue(cast.isCanceled(), "Shellfast allowed a new Iron's cast");
    }
}
