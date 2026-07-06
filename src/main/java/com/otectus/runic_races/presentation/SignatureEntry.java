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
 * lang values may contain {@code %s} placeholders for runtime substitution args. It may be
 * {@code null} for bannerless entries (e.g. weakness onset cues whose copy is owned by the
 * notification system).
 *
 * Specs carry an optional {@code delayTicks}: zero-delay specs fire the instant the signature
 * does; delayed specs are run by {@link PresentationScheduler} against the caster's live
 * position. Author multi-beat recipes with {@code .delayed(n)} withers.
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
    public record SfxSpec(java.util.function.Supplier<SoundEvent> sound, float volume, float pitch, int delayTicks) {
        public SfxSpec(java.util.function.Supplier<SoundEvent> sound, float volume, float pitch) {
            this(sound, volume, pitch, 0);
        }

        /** Convenience for already-constructed vanilla sounds ({@code SoundEvents.*}). */
        public SfxSpec(SoundEvent sound, float volume, float pitch) {
            this(() -> sound, volume, pitch, 0);
        }

        /** Copy of this spec that fires {@code ticks} after the signature moment. */
        public SfxSpec delayed(int ticks) {
            return new SfxSpec(sound, volume, pitch, ticks);
        }
    }

    /**
     * Geometry the particles are emitted in. {@code POINT} is the classic random
     * cloud (vanilla {@code sendParticles} semantics — spreads are gaussian offsets).
     * All other shapes place each particle individually and reinterpret the spec:
     * {@code spreadX} = primary radius/length (blocks), {@code spreadY} = height
     * (HELIX/DOME rise), {@code speed} = directed velocity magnitude.
     */
    public enum Shape {
        /** Random cloud (vanilla semantics). */
        POINT,
        /** Circle around the origin, drifting outward. */
        RING,
        /** Circle around the origin, rushing inward (implode). */
        RING_IN,
        /** Circle around the origin with tangential velocity (orbit illusion). */
        RING_ORBIT,
        /** Rising spiral around the origin; spreadY = total climb. */
        HELIX,
        /** Hemisphere shell snapping up around the origin. */
        DOME,
        /** Evenly spaced along origin→target; needs the target-position fire overload. */
        LINE,
        /** Radial ground lines (8 spokes) out to spreadX blocks. */
        SPOKES,
        /**
         * Forward cone from eye height along the caster's look vector;
         * spreadX = range (blocks), spreadY = end radius, velocity = look * speed.
         * Degrades to POINT when fired through a position-only entry point.
         */
        CONE,
        /** Feet-anchored fountain column: spreadX = disc radius, spreadY = column height, velocity straight up. */
        BURST_UP
    }

    public record VfxSpec(
            java.util.function.Supplier<? extends ParticleOptions> particle,
            int count,
            double spreadX,
            double spreadY,
            double spreadZ,
            double speed,
            Shape shape,
            int delayTicks
    ) {
        /** Shaped emission with a supplier-based particle, firing immediately. */
        public VfxSpec(java.util.function.Supplier<? extends ParticleOptions> particle, int count,
                       double spreadX, double spreadY, double spreadZ, double speed, Shape shape) {
            this(particle, count, spreadX, spreadY, spreadZ, speed, shape, 0);
        }

        /** Convenience for already-constructed particles ({@code ParticleTypes.*}, {@code RaceColors.*}). */
        public VfxSpec(ParticleOptions particle, int count,
                       double spreadX, double spreadY, double spreadZ, double speed) {
            this(() -> particle, count, spreadX, spreadY, spreadZ, speed, Shape.POINT, 0);
        }

        /** Convenience for supplier-based particles keeping the classic point-cloud shape. */
        public VfxSpec(java.util.function.Supplier<? extends ParticleOptions> particle, int count,
                       double spreadX, double spreadY, double spreadZ, double speed) {
            this(particle, count, spreadX, spreadY, spreadZ, speed, Shape.POINT, 0);
        }

        /** Shaped emission with an already-constructed particle. */
        public VfxSpec(ParticleOptions particle, int count,
                       double spreadX, double spreadY, double spreadZ, double speed, Shape shape) {
            this(() -> particle, count, spreadX, spreadY, spreadZ, speed, shape, 0);
        }

        /** Copy of this spec that fires {@code ticks} after the signature moment. */
        public VfxSpec delayed(int ticks) {
            return new VfxSpec(particle, count, spreadX, spreadY, spreadZ, speed, shape, ticks);
        }
    }
}
