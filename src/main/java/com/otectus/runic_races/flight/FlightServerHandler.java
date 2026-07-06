package com.otectus.runic_races.flight;

import com.otectus.runic_races.RunicRacesMod;
import com.otectus.runic_races.presentation.RunicPresentation;
import com.otectus.runic_races.presentation.SignatureKey;
import com.otectus.runic_races.util.OriginsPowerHelper;
import com.otectus.runic_races.util.RaceHelper;
import net.minecraft.server.level.ServerPlayer;

import java.util.Optional;
import java.util.WeakHashMap;

/**
 * Server-side handler for flight flap and glide cancel packets.
 */
public final class FlightServerHandler {

    private static final WeakHashMap<ServerPlayer, Long> lastFlapTick = new WeakHashMap<>();
    private static final int MIN_FLAP_INTERVAL_TICKS = 2;

    private FlightServerHandler() {}

    public static void handleFlap(ServerPlayer player) {
        String race = RaceHelper.getRaceName(player).orElse(null);
        if (race == null) return;

        FlightConfig config = FlightConfig.forRace(race).orElse(null);
        if (config == null) return;

        // The legit client only sends flap packets mid-glide (FlightInputHandler);
        // anything else is a forged packet angling for free vertical thrust.
        if (!player.isFallFlying()) return;

        // Rate-limit: reject packets that arrive too quickly
        long now = player.level().getGameTime();
        Long last = lastFlapTick.get(player);
        if (last != null && now - last < MIN_FLAP_INTERVAL_TICKS) return;

        // Check Origins cooldown resource
        if (!OriginsPowerHelper.isResourceReady(player, config.getCooldownResource())) return;

        // Apply flap
        player.setDeltaMovement(player.getDeltaMovement().add(0, config.getFlapVelocityY(), 0));
        player.hurtMarked = true; // Force velocity sync to client

        // Set cooldown
        OriginsPowerHelper.setResourceValue(player, config.getCooldownResource(), config.getCooldownTicks());
        lastFlapTick.put(player, now);

        // Fire unified presentation (sfx + vfx + actionbar banner)
        signatureKeyFor(config).ifPresent(key -> RunicPresentation.fire(player, key));

        RunicRacesMod.debug("[RunicRaces] {} flapped (race: {}, vel: +{})",
                player.getName().getString(), race, config.getFlapVelocityY());
    }

    public static void handleCancel(ServerPlayer player) {
        if (!player.isFallFlying()) return;

        String race = RaceHelper.getRaceName(player).orElse(null);
        if (race == null) return;

        FlightConfig config = FlightConfig.forRace(race).orElse(null);
        if (config == null) return;

        // Stop gliding — fall damage immunity is handled by Origins powers
        player.stopFallFlying();

        RunicPresentation.fire(player, SignatureKey.FLIGHT_CANCEL);

        RunicRacesMod.debug("[RunicRaces] {} canceled glide (race: {})",
                player.getName().getString(), race);
    }

    private static Optional<SignatureKey> signatureKeyFor(FlightConfig config) {
        return switch (config) {
            case SPRITE -> Optional.of(SignatureKey.SPRITE_WING_FLAP);
            case FAERIE -> Optional.of(SignatureKey.FAERIE_WING_FLAP);
            case AVIAN -> Optional.of(SignatureKey.AVIAN_WING_FLAP);
            case WIND_WYRM -> Optional.of(SignatureKey.WIND_WYRM_WING_FLAP);
        };
    }
}
