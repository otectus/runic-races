package com.otectus.runic_races.integration.curios;

import com.otectus.runic_races.RunicRacesMod;
import com.otectus.runic_races.integration.ModIntegration;
import com.otectus.runic_races.util.RaceHelper;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.ai.attributes.AttributeInstance;
import net.minecraft.world.entity.ai.attributes.AttributeModifier;
import net.minecraftforge.common.MinecraftForge;
import net.minecraftforge.event.entity.player.PlayerEvent;
import net.minecraftforge.event.TickEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import top.theillusivec4.curios.api.SlotAttribute;

import java.util.Map;
import java.util.UUID;

/**
 * Curios integration: Grants extra accessory slots based on race.
 *
 * Goblin: +1 ring slot, +1 charm slot (double accessories fantasy)
 * Dwarves: +1 belt slot
 * Elves: +1 necklace slot
 *
 * Uses SlotAttribute (Curios API) which allows dynamic slot counts
 * via standard Minecraft attribute modifiers.
 */
public class CuriosIntegration implements ModIntegration {

    private static final UUID GOBLIN_RING_UUID = UUID.fromString("c1d2e3f4-5678-9abc-def0-123456789001");
    private static final UUID GOBLIN_CHARM_UUID = UUID.fromString("c1d2e3f4-5678-9abc-def0-123456789002");
    private static final UUID DWARF_BELT_UUID = UUID.fromString("c1d2e3f4-5678-9abc-def0-123456789003");
    private static final UUID ELF_NECKLACE_UUID = UUID.fromString("c1d2e3f4-5678-9abc-def0-123456789004");

    // Slot grants by race: race_name -> (slot_id, uuid, amount)
    private static final Map<String, SlotGrant[]> RACE_SLOTS = Map.of(
            "goblin", new SlotGrant[]{
                    new SlotGrant("ring", GOBLIN_RING_UUID, 1),
                    new SlotGrant("charm", GOBLIN_CHARM_UUID, 1)
            },
            "mountain_dwarf", new SlotGrant[]{
                    new SlotGrant("belt", DWARF_BELT_UUID, 1)
            },
            "deep_dwarf", new SlotGrant[]{
                    new SlotGrant("belt", DWARF_BELT_UUID, 1)
            },
            "high_elf", new SlotGrant[]{
                    new SlotGrant("necklace", ELF_NECKLACE_UUID, 1)
            },
            "wood_elf", new SlotGrant[]{
                    new SlotGrant("necklace", ELF_NECKLACE_UUID, 1)
            }
    );

    private record SlotGrant(String slotId, UUID uuid, int amount) {}

    @Override
    public void init() {
        MinecraftForge.EVENT_BUS.register(this);
        RunicRacesMod.LOGGER.info("[RunicRaces] Curios integration initialized — slot grants active for 5 races");
    }

    @Override
    public String getName() {
        return "Curios";
    }

    /**
     * Apply slot modifiers when the player joins.
     */
    @SubscribeEvent
    public void onPlayerLogin(PlayerEvent.PlayerLoggedInEvent event) {
        if (!(event.getEntity() instanceof ServerPlayer player)) return;
        applySlotGrants(player);
    }

    /**
     * Reapply after respawn (modifiers can be lost on death).
     */
    @SubscribeEvent
    public void onPlayerRespawn(PlayerEvent.PlayerRespawnEvent event) {
        if (!(event.getEntity() instanceof ServerPlayer player)) return;
        applySlotGrants(player);
    }

    /**
     * Periodic check to ensure slot grants stay applied (handles edge cases
     * like origin changes). Runs every 5 seconds.
     */
    @SubscribeEvent
    public void onPlayerTick(TickEvent.PlayerTickEvent event) {
        if (event.phase != TickEvent.Phase.END) return;
        if (!(event.player instanceof ServerPlayer player)) return;
        if (player.tickCount % 100 != 0) return; // Every 5 seconds

        applySlotGrants(player);
    }

    private void applySlotGrants(ServerPlayer player) {
        String race = RaceHelper.getRaceName(player).orElse(null);

        // Remove any existing racial slot grants first
        removeAllSlotGrants(player);

        // Apply grants for current race
        if (race == null) return;
        SlotGrant[] grants = RACE_SLOTS.get(race);
        if (grants == null) return;

        for (SlotGrant grant : grants) {
            try {
                AttributeInstance instance = player.getAttribute(SlotAttribute.getOrCreate(grant.slotId()));
                if (instance != null && instance.getModifier(grant.uuid()) == null) {
                    instance.addPermanentModifier(new AttributeModifier(
                            grant.uuid(),
                            "Runic Races " + grant.slotId() + " slot",
                            grant.amount(),
                            AttributeModifier.Operation.ADDITION
                    ));
                }
            } catch (Exception e) {
                RunicRacesMod.LOGGER.debug("[RunicRaces] Could not apply slot grant for {}: {}", grant.slotId(), e.getMessage());
            }
        }
    }

    private void removeAllSlotGrants(ServerPlayer player) {
        UUID[] allUuids = {GOBLIN_RING_UUID, GOBLIN_CHARM_UUID, DWARF_BELT_UUID, ELF_NECKLACE_UUID};
        String[] allSlots = {"ring", "charm", "belt", "necklace"};

        for (int i = 0; i < allSlots.length; i++) {
            try {
                AttributeInstance instance = player.getAttribute(SlotAttribute.getOrCreate(allSlots[i]));
                if (instance != null) {
                    instance.removeModifier(allUuids[i]);
                }
            } catch (Exception ignored) {}
        }
    }
}
