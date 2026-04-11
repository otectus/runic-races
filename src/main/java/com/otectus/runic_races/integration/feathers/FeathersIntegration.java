package com.otectus.runic_races.integration.feathers;

import com.otectus.runic_races.RunicRacesMod;
import com.otectus.runic_races.integration.ModIntegration;
import com.otectus.runic_races.race.RaceRegistry;
import com.otectus.runic_races.util.RaceHelper;
import com.otectus.runic_races.util.StaminaHelper;
import net.minecraft.server.level.ServerPlayer;

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

    private void applyRacialStamina(ServerPlayer player) {
        String race = RaceHelper.getRaceName(player).orElse(null);
        if (race == null) return;
        int maxFeathers = RaceRegistry.getMaxFeathers(race);
        StaminaHelper.setPlayerMaxStamina(player, maxFeathers);
    }
}
