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

    private void applyRacialStamina(ServerPlayer player) {
        String race = RaceHelper.getRaceName(player).orElse(null);
        CompoundTag persisted = player.getPersistentData().getCompound(Player.PERSISTED_NBT_TAG);
        if (race == null) {
            // Only reset players we previously modified — a pack that globally raised
            // max feathers keeps its value for players this mod never touched.
            if (persisted.getBoolean(APPLIED_TAG)) {
                StaminaHelper.setPlayerMaxStamina(player, DEFAULT_MAX_FEATHERS);
                persisted.remove(APPLIED_TAG);
                player.getPersistentData().put(Player.PERSISTED_NBT_TAG, persisted);
            }
            return;
        }
        StaminaHelper.setPlayerMaxStamina(player, RaceRegistry.getMaxFeathers(race));
        persisted.putBoolean(APPLIED_TAG, true);
        player.getPersistentData().put(Player.PERSISTED_NBT_TAG, persisted);
    }
}
