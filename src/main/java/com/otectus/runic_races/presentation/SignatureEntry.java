package com.otectus.runic_races.presentation;

import net.minecraft.ChatFormatting;
import net.minecraft.core.particles.ParticleOptions;
import net.minecraft.sounds.SoundEvent;

import java.util.List;

/**
 * Declarative bundle describing how a signature ability should *feel*:
 * banner translation key + color, sounds, particles, optional client screen cue.
 *
 * {@code bannerKey} is a translation key (resolved client-side via {@code Component.translatable});
 * lang values may contain {@code %s} placeholders for runtime substitution args.
 *
 * Stored in {@link SignatureRegistry} and fired through {@link RunicPresentation#fire}.
 */
public record SignatureEntry(
        String bannerKey,
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
