package com.otectus.runic_races.gametest;

import com.otectus.runic_races.integration.IntegrationManager;
import com.otectus.runic_races.race.RaceRegistry;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.ai.attributes.AttributeModifier;
import top.theillusivec4.curios.api.CuriosApi;

public final class CuriosProbe {
    public static void verify(GameTestHelper h, ServerPlayer p) {
        var inventory = CuriosApi.getCuriosInventory(p).orElseThrow(() -> new AssertionError("No Curios inventory"));
        var grant = RaceRegistry.getSlotGrants("grove_elf")[0];
        var stacks = inventory.getStacksHandler(grant.slotId()).orElseThrow(() -> new AssertionError("No necklace slot"));
        int baseSlots = stacks.getSlots();
        if (!net.minecraftforge.fml.ModList.get().isLoaded("ars_nouveau") && !net.minecraftforge.fml.ModList.get().isLoaded("irons_spellbooks"))
            h.assertTrue(baseSlots == 0, "Runic slot definitions granted an unowned baseline slot");
        var external = java.util.UUID.randomUUID();
        inventory.addTransientSlotModifier(grant.slotId(), external, "Test external slots", 2, AttributeModifier.Operation.ADDITION);
        IntegrationManager.syncPlayer(p); IntegrationManager.syncPlayer(p);
        h.assertTrue(stacks.getModifiers().containsKey(grant.uuid()), "Racial slot was never applied");
        h.assertTrue(stacks.getModifiers().get(grant.uuid()).getAmount() == 1, "Racial slots stacked on repeated synchronization");
        h.assertTrue(stacks.getModifiers().containsKey(external), "Synchronization removed external slots");
        h.assertTrue(stacks.getSlots() == baseSlots + 3, "Curios did not apply the final combined slot count");
        stacks.getStacks().setStackInSlot(stacks.getSlots() - 1, new net.minecraft.world.item.ItemStack(net.minecraft.world.item.Items.DIAMOND));
        var origins = io.github.edwinmindcraft.origins.api.capabilities.IOriginContainer.get(p).orElseThrow(() -> new AssertionError("No Origins container"));
        origins.setOrigin(com.otectus.runic_races.util.RaceHelper.RACE_LAYER, net.minecraft.resources.ResourceKey.create(
                io.github.edwinmindcraft.origins.api.registry.OriginsDynamicRegistries.ORIGINS_REGISTRY,
                new net.minecraft.resources.ResourceLocation("runic_races", "colossan")));
        origins.tick(); com.otectus.runic_races.util.RaceHelper.invalidate(p.getUUID());
        IntegrationManager.syncPlayer(p);
        h.assertTrue(!stacks.getModifiers().containsKey(grant.uuid()) && stacks.getModifiers().containsKey(external), "Race change did not remove only the owned slot modifier");
        h.assertTrue(stacks.getSlots() == baseSlots + 2, "Race change left a stale racial slot");
        int returned = p.getInventory().countItem(net.minecraft.world.item.Items.DIAMOND);
        int dropped = p.level().getEntitiesOfClass(net.minecraft.world.entity.item.ItemEntity.class, p.getBoundingBox().inflate(3),
                e -> e.getItem().is(net.minecraft.world.item.Items.DIAMOND)).stream().mapToInt(e -> e.getItem().getCount()).sum();
        h.assertTrue(returned + dropped == 1, "Shrinking the occupied racial slot lost or duplicated its item: inventory=" + returned + " dropped=" + dropped);
    }
}
