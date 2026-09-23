package com.otectus.runic_races.integration.feathers;

import com.otectus.runic_races.RunicRacesMod;
import com.otectus.runic_races.integration.ModIntegration;
import com.otectus.runic_races.race.RaceRegistry;
import com.otectus.runic_races.util.RaceHelper;
import com.otectus.runic_races.util.StaminaHelper;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.player.Player;

/**
 * Feather's Mod integration: assigns racial max stamina (feathers) to each race.
 * Larger/hardier races get more feathers; smaller/frailer races get fewer.
 * Human baseline is 20 feathers (Feather's default).
 *
 * Race-specific feather values are defined in {@link RaceRegistry}.
 */
public class FeathersIntegration implements ModIntegration {

    @Override
    public void init() {
        RunicRacesMod.LOGGER.info("[RunicRaces] Feather's Mod integration loaded — racial stamina pools assigned");
    }

    @Override
    public String getName() {
        return "Feather's Mod";
    }

    @Override
    public void syncPlayer(ServerPlayer player) {
        applyRacialStamina(player);
    }

    /** Feather's own default max, restored when a player no longer has a race. */
    private static final int DEFAULT_MAX_FEATHERS = 20;

    /** Persisted-NBT marker: we changed this player's max, so we own resetting it. */
    private static final String APPLIED_TAG = "runic_races:feathers_racial_applied";
    private static final String OWNED_FORMAT = "runic_races:feathers_modifier_format";
    private static final java.util.UUID MODIFIER = java.util.UUID.fromString("77170000-4e15-4000-a000-000000000005");

    private void applyRacialStamina(ServerPlayer player) {
        String race = RaceHelper.getRaceName(player).orElse(null);
        CompoundTag persisted = player.getPersistentData().getCompound(Player.PERSISTED_NBT_TAG);
        var attribute = player.getAttribute(com.elenai.feathers.attributes.FeathersAttributes.MAX_FEATHERS.get());
        if (attribute == null) return;
        if (!persisted.getBoolean(OWNED_FORMAT) && persisted.getBoolean(APPLIED_TAG)) {
            String prior = player.getPersistentData().getString("runic_races:last_synced_race");
            String priorRace = prior.startsWith("runic_races:") ? prior.substring(12) : race;
            // Migrate only a recognizable old absolute racial base. An unrelated changed
            // base is retained; every subsequent update owns a single additive modifier.
            if (priorRace != null && attribute.getBaseValue() == RaceRegistry.getMaxFeathers(priorRace)) attribute.setBaseValue(DEFAULT_MAX_FEATHERS);
        }
        int bonus = race != null && com.otectus.runic_races.config.RRServerConfig.FEATHERS_INTEGRATION.get()
                ? RaceRegistry.getMaxFeathers(race) - DEFAULT_MAX_FEATHERS : 0;
        var old = attribute.getModifier(MODIFIER);
        // An absent modifier already means "no bonus"; only a real difference is written.
        if (old == null ? bonus != 0 : old.getAmount() != bonus) {
            attribute.removeModifier(MODIFIER);
            if (bonus != 0) attribute.addTransientModifier(new net.minecraft.world.entity.ai.attributes.AttributeModifier(MODIFIER,
                    "Runic Races feather capacity", bonus, net.minecraft.world.entity.ai.attributes.AttributeModifier.Operation.ADDITION));
            com.otectus.runic_races.diagnostics.RRMetrics.add(com.otectus.runic_races.diagnostics.RRMetrics.Counter.MODIFIER_WRITES);
        }
        player.getCapability(com.elenai.feathers.capability.PlayerFeathersProvider.PLAYER_FEATHERS).ifPresent(data -> {
            int maximum = Math.max(0, (int)attribute.getValue());
            if (data.getMaxFeathers() == maximum) return;
            data.setMaxFeathers(maximum);
            com.elenai.feathers.networking.FeathersMessages.sendToPlayer(new com.elenai.feathers.networking.packet.FeatherSyncSTCPacket(
                    data.getFeathers(), maximum, data.getRegen(), com.elenai.feathers.api.FeathersHelper.getPlayerWeight(player), data.getEnduranceFeathers()), player);
        });
        if (!persisted.getBoolean(OWNED_FORMAT) || persisted.contains(APPLIED_TAG)) {
            persisted.remove(APPLIED_TAG); persisted.putBoolean(OWNED_FORMAT, true);
            player.getPersistentData().put(Player.PERSISTED_NBT_TAG, persisted);
        }
    }
}
