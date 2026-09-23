package com.otectus.runic_races.util;

import com.otectus.runic_races.RunicRacesMod;
import com.otectus.runic_races.config.RRServerConfig;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.LivingEntity;
import net.minecraftforge.fml.ModList;

import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;

/**
 * Reflection-based access to Iron's Spells 'n Spellbooks mana system.
 * Uses reflection to avoid hard class references so the mod loads without Iron's installed.
 *
 * The adapter is all-or-nothing: it only reports available once every member it calls
 * resolved. A call that proves the API incompatible (wrong signature or return type)
 * latches the adapter onto the configured fail-open/fail-closed answer instead of throwing
 * and catching on every check; an exception raised inside Iron's itself, or a player whose
 * magic data is not attached yet, is treated as transient and retried next call.
 */
public final class ManaHelper {

    private static final boolean IRONS_LOADED = ModList.get().isLoaded("irons_spellbooks");

    private static final Method GET_PLAYER_MAGIC_DATA;
    private static final Method GET_MANA;
    private static final Method ADD_MANA;

    private static volatile boolean incompatible = false;
    // One-shot so a persistent API mismatch doesn't spam the log every tick.
    private static volatile boolean invokeFailureWarned = false;

    static {
        Method magicData = null;
        Method mana = null;
        Method add = null;
        if (IRONS_LOADED) {
            try {
                Class<?> magicDataClass = Class.forName("io.redspace.ironsspellbooks.api.magic.MagicData");
                Method resolvedMagicData = magicDataClass.getMethod("getPlayerMagicData", LivingEntity.class);
                Method resolvedMana = magicDataClass.getMethod("getMana");
                Method resolvedAdd = magicDataClass.getMethod("addMana", float.class);
                magicData = resolvedMagicData;
                mana = resolvedMana;
                add = resolvedAdd;
            } catch (Exception e) {
                RunicRacesMod.LOGGER.error("[RunicRaces] Failed to load Iron's Spellbooks mana API", e);
            }
        }
        GET_PLAYER_MAGIC_DATA = magicData;
        GET_MANA = mana;
        ADD_MANA = add;
    }

    private ManaHelper() {}

    public static boolean isAvailable() {
        return IRONS_LOADED && GET_PLAYER_MAGIC_DATA != null;
    }

    private static boolean failClosed() {
        return RRServerConfig.FAIL_CLOSED_WHEN_RESOURCE_MOD_MISSING.get();
    }

    private static float fallbackMana() {
        return failClosed() ? 0.0f : Float.MAX_VALUE;
    }

    /**
     * Returns the player's current mana, or the fail-open/fail-closed fallback when Iron's
     * is not installed or cannot answer.
     */
    public static float getPlayerMana(Entity entity) {
        if (!isAvailable() || !(entity instanceof LivingEntity living) || incompatible) {
            return fallbackMana();
        }
        try {
            Object magicData = GET_PLAYER_MAGIC_DATA.invoke(null, living);
            if (magicData == null) return fallbackMana();
            return (float) GET_MANA.invoke(magicData);
        } catch (InvocationTargetException e) {
            // Raised inside Iron's for this player — possibly transient; keep asking.
            warnFailure(e.getCause() != null ? e.getCause() : e);
            return fallbackMana();
        } catch (ReflectiveOperationException | RuntimeException e) {
            // Signature or return type no longer matches: every later call would fail the same way.
            incompatible = true;
            warnFailure(e);
            return fallbackMana();
        }
    }

    private static void warnFailure(Throwable cause) {
        // Honor the configured policy instead of always failing open, which would hand
        // out free mana-gated abilities.
        if (!invokeFailureWarned) {
            invokeFailureWarned = true;
            RunicRacesMod.LOGGER.warn("[RunicRaces] Iron's Spellbooks mana API call failed (version mismatch?); "
                    + "mana checks will use the fail-{} default", failClosed() ? "closed" : "open", cause);
        }
    }

    /**
     * Consumes mana from the player. Does nothing if Iron's is not installed.
     */
    public static void consumePlayerMana(Entity entity, float amount) {
        if (!isAvailable() || !(entity instanceof LivingEntity living) || incompatible) return;
        try {
            Object magicData = GET_PLAYER_MAGIC_DATA.invoke(null, living);
            if (magicData != null) ADD_MANA.invoke(magicData, -amount);
        } catch (InvocationTargetException e) {
            RunicRacesMod.debug("[RunicRaces] Failed to consume mana for {}", entity.getName().getString());
        } catch (ReflectiveOperationException | RuntimeException e) {
            incompatible = true;
            warnFailure(e);
        }
    }

    /**
     * Checks if the player has at least {@code amount} mana.
     * Returns true if Iron's is not installed (graceful degradation).
     */
    public static boolean hasEnoughMana(Entity entity, float amount) {
        return getPlayerMana(entity) >= amount;
    }
}
