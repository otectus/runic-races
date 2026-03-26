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

/**
 * Utility for checking a player's race/origin across all integration systems.
 */
public class RaceHelper {

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
