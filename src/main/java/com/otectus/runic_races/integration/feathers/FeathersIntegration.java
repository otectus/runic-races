package com.otectus.runic_races.integration.feathers;

import com.otectus.runic_races.RunicRacesMod;
import com.otectus.runic_races.integration.ModIntegration;
import com.otectus.runic_races.util.RaceHelper;
import com.otectus.runic_races.util.StaminaHelper;
import net.minecraft.server.level.ServerPlayer;

import java.util.Map;

/**
 * Feather's Mod integration: assigns racial max stamina (feathers) to each race.
 * Larger/hardier races get more feathers; smaller/frailer races get fewer.
 * Human baseline is 20 feathers (Feather's default).
 */
public class FeathersIntegration implements ModIntegration {

    private static final Map<String, Integer> RACE_MAX_FEATHERS = Map.ofEntries(
            // Mortal
            Map.entry("human", 20),
            Map.entry("halfling", 18),
            Map.entry("nomad", 22),
            Map.entry("giant_blooded", 28),
            // Fae
            Map.entry("high_elf", 16),
            Map.entry("wood_elf", 18),
            Map.entry("sprite", 14),
            Map.entry("changeling", 18),
            Map.entry("dryad", 16),
            // Beast
            Map.entry("wolfkin", 22),
            Map.entry("dragonborn", 22),
            Map.entry("catfolk", 20),
            Map.entry("minotaur", 26),
            Map.entry("serpentfolk", 18),
            // Underfolk
            Map.entry("mountain_dwarf", 24),
            Map.entry("deep_dwarf", 22),
            Map.entry("goblin", 16),
            Map.entry("troll", 28),
            Map.entry("kobold", 16),
            // Dragon
            Map.entry("wyvern_blooded", 22),
            Map.entry("elder_drake", 26),
            // Cursed
            Map.entry("vampire", 18),
            Map.entry("lycanthrope", 22),
            Map.entry("revenant", 20)
    );

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
        int maxFeathers = RACE_MAX_FEATHERS.getOrDefault(race, 20);
        StaminaHelper.setPlayerMaxStamina(player, maxFeathers);
    }
}
