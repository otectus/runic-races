package com.otectus.runic_races.util;

import com.otectus.runic_races.RunicRacesMod;
import com.otectus.runic_races.config.RRServerConfig;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.LivingEntity;
import net.minecraftforge.fml.ModList;

import java.lang.reflect.Method;

/**
 * Reflection-based access to Iron's Spells 'n Spellbooks mana system.
 * Uses reflection to avoid hard class references so the mod loads without Iron's installed.
 */
public final class ManaHelper {

    private static final boolean IRONS_LOADED = ModList.get().isLoaded("irons_spellbooks");

    private static Method getPlayerMagicData;
    private static Method getMana;
    private static Method addMana;

    static {
        if (IRONS_LOADED) {
            try {
                Class<?> magicDataClass = Class.forName("io.redspace.ironsspellbooks.api.magic.MagicData");
                getPlayerMagicData = magicDataClass.getMethod("getPlayerMagicData", LivingEntity.class);
                getMana = magicDataClass.getMethod("getMana");
                addMana = magicDataClass.getMethod("addMana", float.class);
            } catch (Exception e) {
                RunicRacesMod.LOGGER.error("[RunicRaces] Failed to load Iron's Spellbooks mana API", e);
            }
        }
    }

    private ManaHelper() {}

    public static boolean isAvailable() {
        return IRONS_LOADED && getPlayerMagicData != null;
    }

    /**
     * Returns the player's current mana, or {@link Float#MAX_VALUE} if Iron's is not installed.
     */
    public static float getPlayerMana(Entity entity) {
        if (!isAvailable() || !(entity instanceof LivingEntity living)) {
            return RRServerConfig.FAIL_CLOSED_WHEN_RESOURCE_MOD_MISSING.get() ? 0.0f : Float.MAX_VALUE;
        }
        try {
            Object magicData = getPlayerMagicData.invoke(null, living);
            return (float) getMana.invoke(magicData);
        } catch (Exception e) {
            return Float.MAX_VALUE;
        }
    }

    /**
     * Consumes mana from the player. Does nothing if Iron's is not installed.
     */
    public static void consumePlayerMana(Entity entity, float amount) {
        if (!isAvailable() || !(entity instanceof LivingEntity living)) return;
        try {
            Object magicData = getPlayerMagicData.invoke(null, living);
            addMana.invoke(magicData, -amount);
        } catch (Exception e) {
            RunicRacesMod.debug("[RunicRaces] Failed to consume mana for {}", entity.getName().getString());
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
