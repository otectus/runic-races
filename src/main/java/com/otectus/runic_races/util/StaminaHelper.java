package com.otectus.runic_races.util;

import com.otectus.runic_races.RunicRacesMod;
import com.otectus.runic_races.config.RRServerConfig;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.Entity;
import net.minecraftforge.fml.ModList;

import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;

/**
 * Reflection-based access to Feather's Mod stamina system.
 * Uses reflection to avoid hard class references so the mod loads without Feather's installed.
 *
 * The stamina gate (read + spend) is all-or-nothing: it only reports available once both
 * members resolved. A call that proves the API incompatible latches onto the configured
 * fail-open/fail-closed answer instead of throwing and catching on every check; an
 * exception raised inside Feather's itself is treated as transient and retried.
 */
public final class StaminaHelper {

    private static final boolean FEATHERS_LOADED = ModList.get().isLoaded("feathers");

    private static final Method GET_FEATHERS;
    private static final Method SPEND_FEATHERS;
    // Optional members: only the max-feather helpers use them, never the stamina gate.
    private static final Method GET_MAX_FEATHERS;
    private static final Method SET_MAX_FEATHERS;

    private static volatile boolean incompatible = false;
    // One-shot so a persistent API mismatch doesn't spam the log every tick.
    private static volatile boolean invokeFailureWarned = false;

    static {
        Method get = null;
        Method spend = null;
        Method getMax = null;
        Method setMax = null;
        if (FEATHERS_LOADED) {
            try {
                Class<?> helperClass = Class.forName("com.elenai.feathers.api.FeathersHelper");
                Method resolvedGet = helperClass.getMethod("getFeathers", ServerPlayer.class);
                Method resolvedSpend = helperClass.getMethod("spendFeathers", ServerPlayer.class, int.class);
                get = resolvedGet;
                spend = resolvedSpend;
                getMax = optional(helperClass, "getMaxFeathers", ServerPlayer.class);
                setMax = optional(helperClass, "setMaxFeathers", ServerPlayer.class, int.class);
            } catch (Exception e) {
                RunicRacesMod.LOGGER.error("[RunicRaces] Failed to load Feather's Mod stamina API", e);
            }
        }
        GET_FEATHERS = get;
        SPEND_FEATHERS = spend;
        GET_MAX_FEATHERS = getMax;
        SET_MAX_FEATHERS = setMax;
    }

    private static Method optional(Class<?> owner, String name, Class<?>... parameters) {
        try {
            return owner.getMethod(name, parameters);
        } catch (NoSuchMethodException e) {
            return null;
        }
    }

    private StaminaHelper() {}

    public static boolean isAvailable() {
        return FEATHERS_LOADED && GET_FEATHERS != null;
    }

    private static boolean failClosed() {
        return RRServerConfig.FAIL_CLOSED_WHEN_RESOURCE_MOD_MISSING.get();
    }

    private static int fallbackStamina() {
        return failClosed() ? 0 : Integer.MAX_VALUE;
    }

    /**
     * Returns the player's current feathers (stamina), or the fail-open/fail-closed
     * fallback when Feather's is not installed or cannot answer.
     */
    public static int getPlayerStamina(Entity entity) {
        if (!isAvailable() || !(entity instanceof ServerPlayer player) || incompatible) {
            return fallbackStamina();
        }
        try {
            return (int) GET_FEATHERS.invoke(null, player);
        } catch (InvocationTargetException e) {
            // Raised inside Feather's for this player — possibly transient; keep asking.
            warnFailure(e.getCause() != null ? e.getCause() : e);
            return fallbackStamina();
        } catch (ReflectiveOperationException | RuntimeException e) {
            // Signature or return type no longer matches: every later call would fail the same way.
            incompatible = true;
            warnFailure(e);
            return fallbackStamina();
        }
    }

    private static void warnFailure(Throwable cause) {
        // Honor the configured policy instead of always failing open, which would hand
        // out free stamina-gated abilities.
        if (!invokeFailureWarned) {
            invokeFailureWarned = true;
            RunicRacesMod.LOGGER.warn("[RunicRaces] Feather's Mod stamina API call failed (version mismatch?); "
                    + "stamina checks will use the fail-{} default", failClosed() ? "closed" : "open", cause);
        }
    }

    /**
     * Spends feathers from the player. Returns true on success.
     * Returns true (no-op) if Feather's is not installed.
     */
    public static boolean consumePlayerStamina(Entity entity, int amount) {
        if (!isAvailable() || !(entity instanceof ServerPlayer player)) return true;
        if (incompatible) return false;
        try {
            return (boolean) SPEND_FEATHERS.invoke(null, player, amount);
        } catch (InvocationTargetException e) {
            RunicRacesMod.debug("[RunicRaces] Failed to spend feathers for {}", entity.getName().getString());
            return false;
        } catch (ReflectiveOperationException | RuntimeException e) {
            incompatible = true;
            warnFailure(e);
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
     * Returns the player's maximum feathers, or {@code fallback} if Feather's is not
     * installed or the call fails.
     */
    public static int getPlayerMaxStamina(Entity entity, int fallback) {
        if (!isAvailable() || GET_MAX_FEATHERS == null || !(entity instanceof ServerPlayer player)) return fallback;
        try {
            return (int) GET_MAX_FEATHERS.invoke(null, player);
        } catch (ReflectiveOperationException | RuntimeException e) {
            RunicRacesMod.debug("[RunicRaces] Failed to read max feathers for {}", entity.getName().getString());
            return fallback;
        }
    }

    /**
     * Sets the player's maximum feathers. Does nothing if Feather's is not installed.
     */
    public static void setPlayerMaxStamina(Entity entity, int max) {
        if (!isAvailable() || SET_MAX_FEATHERS == null || !(entity instanceof ServerPlayer player)) return;
        try {
            SET_MAX_FEATHERS.invoke(null, player, max);
        } catch (ReflectiveOperationException | RuntimeException e) {
            RunicRacesMod.debug("[RunicRaces] Failed to set max feathers for {}", entity.getName().getString());
        }
    }
}
