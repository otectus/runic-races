package com.otectus.runic_races.presentation;

import net.minecraft.ChatFormatting;
import net.minecraft.core.particles.ParticleOptions;
import net.minecraft.sounds.SoundEvent;

import java.util.List;

/**
 * Declarative bundle describing how a signature ability should *feel*:
 * banner text + color, sounds, particles, optional client screen cue.
 *
 * Stored in {@link SignatureRegistry} and fired through {@link RunicPresentation#fire}.
 */
public record SignatureEntry(
        String bannerText,
        ChatFormatting bannerColor,
        boolean bannerBold,
        List<SfxSpec> sounds,
        List<VfxSpec> particles,
        CueType screenCue,
        int screenCueDurationTicks,
        Intensity intensity
) {
    public record SfxSpec(SoundEvent sound, float volume, float pitch) {}

    public record VfxSpec(
            ParticleOptions particle,
            int count,
            double spreadX,
            double spreadY,
            double spreadZ,
            double speed
    ) {}
}
