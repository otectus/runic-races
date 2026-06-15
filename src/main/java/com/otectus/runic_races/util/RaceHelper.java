package com.otectus.runic_races.util;

import com.otectus.runic_races.RunicRacesMod;
import com.otectus.runic_races.race.RaceRegistry;
import io.github.edwinmindcraft.origins.api.capabilities.IOriginContainer;
import io.github.edwinmindcraft.origins.api.origin.Origin;
import io.github.edwinmindcraft.origins.api.origin.OriginLayer;
import net.minecraft.resources.ResourceKey;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.entity.player.Player;

import java.util.Map;
import java.util.Optional;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Utility for checking a player's race/origin across all integration systems.
 * Scale, family, and other metadata are delegated to {@link RaceRegistry}.
 */
public class RaceHelper {

    /**
     * Within-tick memo for {@link #getRaceId(Player)}. The race lookup is hit by several
     * unrelated subscribers in the same tick (the per-player state watcher, integration
     * events, client ambience). This dedupes those to a single Origins capability resolve
     * per player per game-tick. It deliberately never caches across ticks, so an origin
     * change is reflected on the very next tick — there is no stale-race window.
     *
     * Keyed by UUID; the {@code client} flag disambiguates the integrated client/server
     * sharing one JVM (a mismatch simply recomputes). Concurrent because both logical
     * sides may query on their own threads. Cleared on logout via {@link #invalidate(UUID)}.
     */
    private record RaceMemo(long tick, boolean client, ResourceLocation raceId) {}

    private static final Map<UUID, RaceMemo> RACE_MEMO = new ConcurrentHashMap<>();

    /**
     * Get the configured Pehkui scale for a race. Returns 1.0 for unknown/null races.
     */
    public static float getRaceScale(String raceName) {
        if (raceName == null) return 1.0f;
        return RaceRegistry.getScale(raceName);
    }

    /**
     * Get the player's current race ID (e.g. "runic_races:vampire").
     */
    public static Optional<ResourceLocation> getRaceId(Player player) {
        long tick = player.level().getGameTime();
        boolean client = player.level().isClientSide();
        UUID id = player.getUUID();

        RaceMemo memo = RACE_MEMO.get(id);
        if (memo != null && memo.tick() == tick && memo.client() == client) {
            return Optional.ofNullable(memo.raceId());
        }

        ResourceLocation resolved = resolveRaceId(player);
        RACE_MEMO.put(id, new RaceMemo(tick, client, resolved));
        return Optional.ofNullable(resolved);
    }

    /** Uncached Origins capability resolve — returns null if the player has no Runic race. */
    private static ResourceLocation resolveRaceId(Player player) {
        try {
            IOriginContainer container = IOriginContainer.get(player).orElse(null);
            if (container == null) return null;

            Map<ResourceKey<OriginLayer>, ResourceKey<Origin>> origins = container.getOrigins();
            for (ResourceKey<Origin> origin : origins.values()) {
                if (origin.location().getNamespace().equals(RunicRacesMod.MOD_ID)) {
                    return origin.location();
                }
            }
        } catch (Exception e) {
            RunicRacesMod.debug("[RunicRaces] Could not resolve race for {}: {}", player.getName().getString(), e.getMessage());
        }
        return null;
    }

    /** Drop any memoized race for a player (call on logout to avoid UUID accumulation). */
    public static void invalidate(UUID playerId) {
        RACE_MEMO.remove(playerId);
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
        return RaceRegistry.getFamily(raceName);
    }
}
