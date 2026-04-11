package com.otectus.runic_races.flight;

import com.otectus.runic_races.RunicRacesMod;
import com.otectus.runic_races.util.OriginsPowerHelper;
import com.otectus.runic_races.util.RaceHelper;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.core.particles.ParticleTypes;

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

        if (!player.isFallFlying() && player.onGround()) return;

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

        // Effects
        player.level().playSound(null, player.getX(), player.getY(), player.getZ(),
                config.getFlapSound(), SoundSource.PLAYERS, config.getFlapVolume(), config.getFlapPitch());

        player.displayClientMessage(Component.literal(config.getFlapMessage()), true);

        // Particles
        if (player.level() instanceof ServerLevel serverLevel) {
            spawnFlapParticles(serverLevel, player, config);
        }

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

        // Feedback
        player.displayClientMessage(
                Component.literal("\u00A77You fold your wings."), true);

        player.level().playSound(null, player.getX(), player.getY(), player.getZ(),
                SoundEvents.PHANTOM_FLAP, SoundSource.PLAYERS, 0.3f, 0.8f);

        RunicRacesMod.debug("[RunicRaces] {} canceled glide (race: {})",
                player.getName().getString(), race);
    }

    private static void spawnFlapParticles(ServerLevel level, ServerPlayer player, FlightConfig config) {
        double x = player.getX();
        double y = player.getY() + 0.3;
        double z = player.getZ();

        switch (config) {
            case SPRITE -> {
                level.sendParticles(ParticleTypes.END_ROD, x, y, z, 8, 0.3, 0.3, 0.3, 0.3);
                level.sendParticles(ParticleTypes.CLOUD, x, y, z, 8, 0.3, 0.1, 0.3, 0.02);
            }
            case WYVERN -> {
                level.sendParticles(ParticleTypes.CLOUD, x, y, z, 12, 0.5, 0.2, 0.5, 0.3);
                level.sendParticles(ParticleTypes.CLOUD, x, y, z, 10, 0.5, 0.1, 0.5, 0.02);
            }
            case ELDER_DRAKE -> {
                level.sendParticles(ParticleTypes.DRAGON_BREATH, x, y, z, 15, 0.8, 0.3, 0.8, 0.3);
                level.sendParticles(ParticleTypes.SMOKE, x, y, z, 8, 0.5, 0.3, 0.5, 0.2);
                level.sendParticles(ParticleTypes.CAMPFIRE_SIGNAL_SMOKE, x, y + 0.2, z, 5, 1.0, 0.1, 1.0, 0.01);
            }
        }
    }
}
