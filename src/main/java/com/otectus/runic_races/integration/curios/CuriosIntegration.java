package com.otectus.runic_races.integration.curios;

import com.otectus.runic_races.RunicRacesMod;
import com.otectus.runic_races.integration.ModIntegration;
import com.otectus.runic_races.race.RaceDefinition;
import com.otectus.runic_races.race.RaceRegistry;
import com.otectus.runic_races.util.RaceHelper;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.ai.attributes.AttributeModifier;

/** Optional Curios slot grants use its inventory API and retain unrelated modifiers and stacks. */
public class CuriosIntegration implements ModIntegration {

    @Override
    public void init() {
        RunicRacesMod.LOGGER.info("[RunicRaces] Curios integration initialized — slot grants active");
    }

    @Override
    public String getName() {
        return "Curios";
    }

    @Override
    public void syncPlayer(ServerPlayer player) {
        applySlotGrants(player);
    }

    private void applySlotGrants(ServerPlayer player) {
        String race = RaceHelper.getRaceName(player).orElse(null);

        var desired = new java.util.HashMap<java.util.UUID, RaceDefinition.SlotGrant>();
        if (race != null && com.otectus.runic_races.config.RRServerConfig.CURIOS_INTEGRATION.get())
            for (var grant : RaceRegistry.getSlotGrants(race)) desired.put(grant.uuid(), grant);
        top.theillusivec4.curios.api.CuriosApi.getCuriosInventory(player).ifPresent(inventory -> {
            boolean changed = false;
            var updated = new java.util.HashSet<top.theillusivec4.curios.api.type.inventory.ICurioStacksHandler>();
            for (var grant : RaceRegistry.allSlotGrants()) {
                var stacks = inventory.getStacksHandler(grant.slotId()).orElse(null);
                if (stacks == null) continue;
                var old = stacks.getModifiers().get(grant.uuid());
                var wanted = desired.get(grant.uuid());
                if (wanted != null && old != null && old.getAmount() == wanted.amount()
                        && old.getOperation() == AttributeModifier.Operation.ADDITION) continue;
                if (old != null) { inventory.removeSlotModifier(grant.slotId(), grant.uuid()); changed = true; }
                if (wanted != null) {
                    inventory.addTransientSlotModifier(wanted.slotId(), wanted.uuid(), "Runic Races " + wanted.slotId(), wanted.amount(), AttributeModifier.Operation.ADDITION);
                    changed = true;
                }
                if (old != null || wanted != null) updated.add(stacks);
            }
            // Let Curios process shrinking occupied inventories once, after the final desired
            // modifiers exist. Never clear slot stacks or another mod's modifier collection.
            if (changed) {
                // Curios resizes lazily. Materialize the final sizes before draining its
                // invalid-stack queue, otherwise a removed occupied slot waits until a
                // later inventory read and can miss this synchronization's return pass.
                updated.forEach(top.theillusivec4.curios.api.type.inventory.ICurioStacksHandler::update);
                inventory.handleInvalidStacks();
            }
        });
    }
}
