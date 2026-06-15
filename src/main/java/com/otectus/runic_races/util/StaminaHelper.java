package com.otectus.runic_races.util;

import com.otectus.runic_races.RunicRacesMod;
import com.otectus.runic_races.config.RRServerConfig;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.Entity;
import net.minecraftforge.fml.ModList;

import java.lang.reflect.Method;

/**
 * Reflection-based access to Feather's Mod stamina system.
 * Uses reflection to avoid hard class references so the mod loads without Feather's installed.
 */
public final class StaminaHelper {

    private static final boolean FEATHERS_LOADED = ModList.get().isLoaded("feathers");

    private static Method getFeathers;
    private static Method spendFeathers;
    private static Method getMaxFeathers;
    private static Method setMaxFeathers;

    // One-shot so a persistent API mismatch doesn't spam the log every tick.
    private static boolean invokeFailureWarned = false;

    static {
        if (FEATHERS_LOADED) {
            try {
                Class<?> helperClass = Class.forName("com.elenai.feathers.api.FeathersHelper");
                getFeathers = helperClass.getMethod("getFeathers", ServerPlayer.class);
                spendFeathers = helperClass.getMethod("spendFeathers", ServerPlayer.class, int.class);
                getMaxFeathers = helperClass.getMethod("getMaxFeathers", ServerPlayer.class);
                setMaxFeathers = helperClass.getMethod("setMaxFeathers", ServerPlayer.class, int.class);
            } catch (Exception e) {
                RunicRacesMod.LOGGER.error("[RunicRaces] Failed to load Feather's Mod stamina API", e);
            }
        }
    }

    private StaminaHelper() {}

    public static boolean isAvailable() {
        return FEATHERS_LOADED && getFeathers != null;
    }

    private static boolean failClosed() {
        return RRServerConfig.FAIL_CLOSED_WHEN_RESOURCE_MOD_MISSING.get();
    }

    /**
     * Returns the player's current feathers (stamina), or {@link Integer#MAX_VALUE} if Feather's is not installed.
     */
    public static int getPlayerStamina(Entity entity) {
        if (!isAvailable() || !(entity instanceof ServerPlayer player)) {
            return failClosed() ? 0 : Integer.MAX_VALUE;
        }
        try {
            return (int) getFeathers.invoke(null, player);
        } catch (Exception e) {
            // Reflection failed at runtime (likely a Feathers API change). Honor the configured
            // policy instead of always failing open, which would hand out free stamina-gated abilities.
            if (!invokeFailureWarned) {
                invokeFailureWarned = true;
                RunicRacesMod.LOGGER.warn("[RunicRaces] Feather's Mod stamina API call failed (version mismatch?); "
                        + "stamina checks will use the fail-{} default", failClosed() ? "closed" : "open", e);
            }
            return failClosed() ? 0 : Integer.MAX_VALUE;
        }
    }

    /**
     * Spends feathers from the player. Returns true on success.
     * Returns true (no-op) if Feather's is not installed.
     */
    public static boolean consumePlayerStamina(Entity entity, int amount) {
        if (!isAvailable() || !(entity instanceof ServerPlayer player)) return true;
        try {
            return (boolean) spendFeathers.invoke(null, player, amount);
        } catch (Exception e) {
            RunicRacesMod.debug("[RunicRaces] Failed to spend feathers for {}", entity.getName().getString());
            return false;
        }
    }

    /**
     * Checks if the player has at least {@code amount} feathers.
     * Returns true if Feather's is not installed (graceful degradation).
     */
    public static boolean hasEnoughStamina(Entity entity, int amount) {
        return getPlayerStamina(entity) >= amount;
    }

    /**
     * Sets the player's maximum feathers. Does nothing if Feather's is not installed.
     */
    public static void setPlayerMaxStamina(Entity entity, int max) {
        if (!isAvailable() || !(entity instanceof ServerPlayer player)) return;
        try {
            setMaxFeathers.invoke(null, player, max);
        } catch (Exception e) {
            RunicRacesMod.debug("[RunicRaces] Failed to set max feathers for {}", entity.getName().getString());
        }
    }
}
