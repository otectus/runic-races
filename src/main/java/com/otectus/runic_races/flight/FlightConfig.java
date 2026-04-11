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
            "sprite/fae_wings_flap_cooldown_timer",
            SoundEvents.BEE_LOOP, 0.2f, 2.0f,
            "\u00A7d\u00A7lYour wings flutter!"),
    WYVERN("wyvern_blooded",
            0.7, 60,
            "wyvern_blooded/wyvern_wings_flap_cooldown_timer",
            SoundEvents.ENDER_DRAGON_FLAP, 0.4f, 2.0f,
            "\u00A76\u00A7lYour wings beat powerfully!"),
    ELDER_DRAKE("elder_drake",
            1.0, 100,
            "elder_drake/ancient_wings_flap_cooldown_timer",
            SoundEvents.ENDER_DRAGON_FLAP, 0.5f, 1.0f,
            "\u00A74\u00A7lYour ancient wings thunder!");

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
