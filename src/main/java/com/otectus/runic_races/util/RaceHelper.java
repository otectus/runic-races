package com.otectus.runic_races.util;

import com.otectus.runic_races.RunicRacesMod;
import io.github.edwinmindcraft.origins.api.capabilities.IOriginContainer;
import io.github.edwinmindcraft.origins.api.origin.Origin;
import io.github.edwinmindcraft.origins.api.origin.OriginLayer;
import net.minecraft.resources.ResourceKey;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.entity.player.Player;

import java.util.Map;
import java.util.Optional;
import java.util.Set;

/**
 * Utility for checking a player's race/origin across all integration systems.
 */
public class RaceHelper {

    /** Pehkui scale values for all 24 races. Shared between PehkuiIntegration and jump compensation. */
    public static final Map<String, Float> RACE_SCALES = Map.ofEntries(
            // Mortal
            Map.entry("human", 1.00f),
            Map.entry("halfling", 0.60f),
            Map.entry("nomad", 0.95f),
            Map.entry("giant_blooded", 1.40f),
            // Fae
            Map.entry("high_elf", 1.05f),
            Map.entry("wood_elf", 0.95f),
            Map.entry("sprite", 0.425f),
            Map.entry("changeling", 0.90f),
            Map.entry("dryad", 0.90f),
            // Beast
            Map.entry("wolfkin", 1.00f),
            Map.entry("dragonborn", 1.15f),
            Map.entry("catfolk", 0.85f),
            Map.entry("minotaur", 1.25f),
            Map.entry("serpentfolk", 0.90f),
            // Underfolk
            Map.entry("mountain_dwarf", 0.70f),
            Map.entry("deep_dwarf", 0.65f),
            Map.entry("goblin", 0.55f),
            Map.entry("troll", 1.25f),
            Map.entry("kobold", 0.50f),
            // Dragon
            Map.entry("wyvern_blooded", 1.10f),
            Map.entry("elder_drake", 1.30f),
            // Cursed
            Map.entry("vampire", 1.00f),
            Map.entry("lycanthrope", 1.00f),
            Map.entry("revenant", 1.00f)
    );

    /**
     * Get the configured Pehkui scale for a race. Returns 1.0 for unknown races.
     */
    public static float getRaceScale(String raceName) {
        return RACE_SCALES.getOrDefault(raceName, 1.0f);
    }

    /**
     * Get the player's current race ID (e.g. "runic_races:vampire").
     */
    public static Optional<ResourceLocation> getRaceId(Player player) {
        try {
            IOriginContainer container = IOriginContainer.get(player).orElse(null);
            if (container == null) return Optional.empty();

            Map<ResourceKey<OriginLayer>, ResourceKey<Origin>> origins = container.getOrigins();
            for (ResourceKey<Origin> origin : origins.values()) {
                if (origin.location().getNamespace().equals(RunicRacesMod.MOD_ID)) {
                    return Optional.of(origin.location());
                }
            }
        } catch (Exception e) {
            // Origins not loaded or player has no origin
        }
        return Optional.empty();
    }

    /**
     * Get just the race name (e.g. "vampire" from "runic_races:vampire").
     */
    public static Optional<String> getRaceName(Player player) {
        return getRaceId(player).map(ResourceLocation::getPath);
    }

    /**
     * Check if the player has a specific race.
     */
    public static boolean isRace(Player player, String raceName) {
        return getRaceName(player).map(name -> name.equals(raceName)).orElse(false);
    }

    /**
     * Check if the player belongs to a race family.
     */
    public static boolean isFamily(Player player, String family) {
        return getRaceName(player).map(name -> getRaceFamily(name).equals(family)).orElse(false);
    }

    /**
     * Get the family for a race name.
     */
    public static String getRaceFamily(String raceName) {
        return switch (raceName) {
            case "human", "halfling", "nomad", "giant_blooded" -> "mortal";
            case "high_elf", "wood_elf", "sprite", "changeling", "dryad" -> "fae";
            case "wolfkin", "dragonborn", "catfolk", "minotaur", "serpentfolk" -> "beast";
            case "mountain_dwarf", "deep_dwarf", "goblin", "troll", "kobold" -> "underfolk";
            case "wyvern_blooded", "elder_drake" -> "dragon";
            case "vampire", "lycanthrope", "revenant" -> "cursed";
            default -> "unknown";
        };
    }
}
