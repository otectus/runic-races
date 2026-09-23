package com.otectus.runic_races.gametest;

import com.hollingsworth.arsnouveau.api.event.*;
import com.hollingsworth.arsnouveau.api.spell.*;
import com.otectus.runic_races.ability.AbilityService;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.item.ItemStack;
import net.minecraftforge.common.MinecraftForge;

public final class ArsProbe {
    public static void affinity(GameTestHelper h, ServerPlayer p) {
        var mana = new MaxManaCalcEvent(p, 100); MinecraftForge.EVENT_BUS.post(mana);
        h.assertTrue(mana.getMax() == 115, "Astral Elf's explicit Ars mana affinity failed: " + mana.getMax());
        var cost = new SpellCostCalcEvent(SpellContext.fromEntity(new Spell(), p, ItemStack.EMPTY), 100);
        MinecraftForge.EVENT_BUS.post(cost);
        h.assertTrue(cost.currentCost == 90, "Astral Elf's explicit Ars cost affinity failed: " + cost.currentCost);
    }
    public static void shell(GameTestHelper h, ServerPlayer p) {
        AbilityService.input(p, 1, true);
        var spell = new Spell(); var context = SpellContext.fromEntity(spell, p, ItemStack.EMPTY);
        var cast = new SpellCastEvent(spell, context); MinecraftForge.EVENT_BUS.post(cast);
        h.assertTrue(cast.isCanceled(), "Shellfast allowed an Ars cast");
    }
}
