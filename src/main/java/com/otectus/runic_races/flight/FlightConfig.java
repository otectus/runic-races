package com.otectus.runic_races.flight;

import com.otectus.runic_races.RunicRacesMod;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.sounds.SoundEvent;
import net.minecraft.sounds.SoundEvents;

import java.util.Optional;

/**
 * Per-race flight parameters for winged races.
 */
public enum FlightConfig {
    SPRITE("sprite",
            0.3, 30,
            "sprite/gossamer_wings_flap_cooldown_timer",
            SoundEvents.BEE_LOOP, 0.2f, 2.0f,
            "\u00A7d\u00A7lYour gossamer wings flutter!"),
    FAERIE("faerie",
            0.32, 30,
            "faerie/pixie_flight_flap_cooldown_timer",
            SoundEvents.BEE_LOOP, 0.2f, 1.8f,
            "\u00A7d\u00A7lYour pixie wings shimmer and lift!"),
    AVIAN("avian",
            0.45, 35,
            "avian/skyborne_flap_cooldown_timer",
            SoundEvents.PHANTOM_FLAP, 0.3f, 1.4f,
            "\u00A7b\u00A7lYou beat your wings and rise!"),
    WIND_WYRM("wind_wyrm",
            0.7, 50,
            "wind_wyrm/skylord_flap_cooldown_timer",
            SoundEvents.ENDER_DRAGON_FLAP, 0.5f, 1.2f,
            "\u00A7f\u00A7lYour wings ride the gale!");

    private final String raceName;
    private final double flapVelocityY;
    private final int cooldownTicks;
    private final ResourceLocation cooldownResource;
    private final SoundEvent flapSound;
    private final float flapVolume;
    private final float flapPitch;
    private final String flapMessage;

    FlightConfig(String raceName, double flapVelocityY, int cooldownTicks,
                 String cooldownResourcePath, SoundEvent flapSound, float flapVolume, float flapPitch,
                 String flapMessage) {
        this.raceName = raceName;
        this.flapVelocityY = flapVelocityY;
        this.cooldownTicks = cooldownTicks;
        this.cooldownResource = new ResourceLocation(RunicRacesMod.MOD_ID, cooldownResourcePath);
        this.flapSound = flapSound;
        this.flapVolume = flapVolume;
        this.flapPitch = flapPitch;
        this.flapMessage = flapMessage;
    }

    public String getRaceName() { return raceName; }
    public double getFlapVelocityY() { return flapVelocityY; }
    public int getCooldownTicks() { return cooldownTicks; }
    public ResourceLocation getCooldownResource() { return cooldownResource; }
    public SoundEvent getFlapSound() { return flapSound; }
    public float getFlapVolume() { return flapVolume; }
    public float getFlapPitch() { return flapPitch; }
    public String getFlapMessage() { return flapMessage; }

    public static Optional<FlightConfig> forRace(String raceName) {
        for (FlightConfig config : values()) {
            if (config.raceName.equals(raceName)) {
                return Optional.of(config);
            }
        }
        return Optional.empty();
    }
}
