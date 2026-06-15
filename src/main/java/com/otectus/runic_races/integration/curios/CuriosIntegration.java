package com.otectus.runic_races.integration.curios;

import com.otectus.runic_races.RunicRacesMod;
import com.otectus.runic_races.integration.ModIntegration;
import com.otectus.runic_races.race.RaceDefinition;
import com.otectus.runic_races.race.RaceRegistry;
import com.otectus.runic_races.util.RaceHelper;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.ai.attributes.AttributeInstance;
import net.minecraft.world.entity.ai.attributes.AttributeModifier;
import top.theillusivec4.curios.api.SlotAttribute;

/**
 * Curios integration: Grants extra accessory slots based on race.
 *
 * Goblin: +1 ring slot, +1 charm slot (double accessories fantasy)
 * Dwarves: +1 belt slot
 * Elves: +1 necklace slot
 *
 * Uses SlotAttribute (Curios API) which allows dynamic slot counts
 * via standard Minecraft attribute modifiers.
 *
 * Race-specific slot grants are defined in {@link RaceRegistry}.
 */
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

        // Remove any existing racial slot grants first
        removeAllSlotGrants(player);

        // Apply grants for current race
        if (race == null) return;
        RaceDefinition.SlotGrant[] grants = RaceRegistry.getSlotGrants(race);
        if (grants.length == 0) return;

        for (RaceDefinition.SlotGrant grant : grants) {
            try {
                AttributeInstance instance = player.getAttribute(SlotAttribute.getOrCreate(grant.slotId()));
                if (instance != null && instance.getModifier(grant.uuid()) == null) {
                    instance.addTransientModifier(new AttributeModifier(
                            grant.uuid(),
                            "Runic Races " + grant.slotId() + " slot",
                            grant.amount(),
                            AttributeModifier.Operation.ADDITION
                    ));
                }
            } catch (Exception e) {
                RunicRacesMod.debug("[RunicRaces] Could not apply slot grant for {}: {}", grant.slotId(), e.getMessage());
            }
        }
    }

    private void removeAllSlotGrants(ServerPlayer player) {
        for (RaceDefinition.SlotGrant grant : RaceRegistry.allSlotGrants()) {
            try {
                AttributeInstance instance = player.getAttribute(SlotAttribute.getOrCreate(grant.slotId()));
                if (instance != null) {
                    instance.removeModifier(grant.uuid());
                }
            } catch (Exception ignored) {
            }
        }
    }
}
