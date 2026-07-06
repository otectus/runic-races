package com.otectus.runic_races.presentation;

import com.otectus.runic_races.config.RRServerConfig;
import com.otectus.runic_races.network.NetworkHandler;
import com.otectus.runic_races.network.S2CScreenCuePacket;
import com.otectus.runic_races.presentation.SignatureEntry.SfxSpec;
import com.otectus.runic_races.presentation.SignatureEntry.VfxSpec;
import net.minecraft.ChatFormatting;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.MutableComponent;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.phys.Vec3;

/**
 * Single entry point for "signature moment" presentation. Callers pick a
 * {@link SignatureKey} and this helper plays the matching sounds, particles,
 * actionbar banner and optional screen cue.
 *
 * Zero-delay specs fire immediately; {@code .delayed(n)} specs are handed to
 * {@link PresentationScheduler} and follow the caster's live position, letting
 * recipes play out in beats (anticipation → impact → settle). Banner and screen
 * cue always fire at t=0 — they acknowledge the input, not the animation.
 *
 * Server-side only. Banners/screen cues are delivered per-player; sfx/vfx are
 * broadcast at the player's position so nearby players see and hear them too.
 */
public final class RunicPresentation {

    private RunicPresentation() {}

    /** Fires the full presentation bundle at the given player. */
    public static void fire(ServerPlayer player, SignatureKey key, Object... bannerArgs) {
        fire(player, key, null, bannerArgs);
    }

    /**
     * Variant carrying a target position for {@link SignatureEntry.Shape#LINE} specs
     * (e.g. a soul stream from a victim into the caster). Non-LINE specs ignore it.
     */
    public static void fire(ServerPlayer player, SignatureKey key, Vec3 lineTarget, Object... bannerArgs) {
        SignatureEntry entry = SignatureRegistry.get(key);
        if (entry == null || !(player.level() instanceof ServerLevel level)) return;

        Vec3 pos = player.position();
        Vec3 look = player.getLookAngle();
        for (SfxSpec spec : entry.sounds()) {
            if (spec.delayTicks() <= 0) {
                playOneSfx(level, pos, spec);
            } else {
                PresentationScheduler.scheduleSfx(player, spec);
            }
        }
        Vec3 origin = pos.add(0, 0.3, 0);
        for (VfxSpec spec : entry.particles()) {
            if (spec.delayTicks() <= 0) {
                spawnOneVfx(level, origin, look, spec, lineTarget);
            } else {
                PresentationScheduler.scheduleVfx(player, spec, lineTarget);
            }
        }
        showRunicBanner(player, entry, bannerArgs);
        if (entry.screenCue() != null) {
            showScreenCue(player, entry.screenCue(), entry.screenCueDurationTicks());
        }
    }

    /** Debounced fire for repeatable proc cues; the channel defaults to the key itself. */
    public static boolean fireProc(ServerPlayer player, SignatureKey key, int debounceTicks) {
        return fireProc(player, key, key.name(), debounceTicks);
    }

    /**
     * Debounced fire on an explicit channel — related procs share a channel
     * (e.g. all fragility cues use {@code "fragility"}) so a burst of different
     * triggers still reads as one moment. Returns whether it fired.
     */
    public static boolean fireProc(ServerPlayer player, SignatureKey key, String channel, int debounceTicks) {
        if (!ProcDebounce.tryAcquire(player, channel, debounceTicks)) return false;
        fire(player, key);
        return true;
    }

    /**
     * Position-based variant with no player to schedule against: every spec fires
     * immediately, delays ignored. CONE degrades to POINT (no look vector).
     */
    public static void playSignatureSfx(ServerLevel level, Vec3 pos, SignatureEntry entry) {
        for (SfxSpec spec : entry.sounds()) {
            playOneSfx(level, pos, spec);
        }
    }

    public static void spawnSignatureVfx(ServerLevel level, Vec3 pos, SignatureEntry entry) {
        spawnSignatureVfx(level, pos, entry, null);
    }

    /** Position-based variant — see {@link #playSignatureSfx}: immediate, delays ignored. */
    public static void spawnSignatureVfx(ServerLevel level, Vec3 pos, SignatureEntry entry, Vec3 lineTarget) {
        Vec3 origin = pos.add(0, 0.3, 0);
        for (VfxSpec spec : entry.particles()) {
            spawnOneVfx(level, origin, Vec3.ZERO, spec, lineTarget);
        }
    }

    static void playOneSfx(ServerLevel level, Vec3 pos, SfxSpec spec) {
        level.playSound(null, pos.x, pos.y, pos.z, spec.sound().get(), SoundSource.PLAYERS, spec.volume(), spec.pitch());
    }

    static void spawnOneVfx(ServerLevel level, Vec3 origin, Vec3 look, VfxSpec spec, Vec3 lineTarget) {
        spawnShaped(level, origin, look, spec, lineTarget);
    }

    /**
     * Scales an authored count by {@code vfx.signatureParticleDensity}. Shaped
     * emissions keep a small floor so a turned-down ring still reads as a ring
     * rather than a bug; 0.0 disables signature particles outright.
     */
    private static int scaledCount(VfxSpec spec) {
        double density = RRServerConfig.SIGNATURE_PARTICLE_DENSITY.get();
        if (density <= 0.0) return 0;
        if (Math.abs(density - 1.0) < 1.0e-3) return spec.count();
        int floor = switch (spec.shape()) {
            case POINT, LINE -> 1;
            default -> Math.min(spec.count(), 6);
        };
        return Math.max(floor, (int) Math.round(spec.count() * density));
    }

    /**
     * Places each particle of a shaped spec individually. Directed motion uses the
     * vanilla count-0 trick: {@code sendParticles(p, x, y, z, 0, dx, dy, dz, speed)}
     * gives the single particle velocity {@code (dx, dy, dz) * speed}.
     */
    private static void spawnShaped(ServerLevel level, Vec3 origin, Vec3 look, VfxSpec spec, Vec3 lineTarget) {
        var particle = spec.particle().get();
        int count = scaledCount(spec);
        if (count <= 0) return;
        double radius = spec.spreadX();
        double height = spec.spreadY();
        switch (spec.shape()) {
            case POINT -> level.sendParticles(particle, origin.x, origin.y, origin.z, count,
                    spec.spreadX(), spec.spreadY(), spec.spreadZ(), spec.speed());
            case RING, RING_IN, RING_ORBIT -> {
                for (int i = 0; i < count; i++) {
                    double angle = (Math.PI * 2 * i) / count;
                    double cx = Math.cos(angle);
                    double cz = Math.sin(angle);
                    double vx;
                    double vz;
                    if (spec.shape() == SignatureEntry.Shape.RING_IN) {
                        vx = -cx; vz = -cz;
                    } else if (spec.shape() == SignatureEntry.Shape.RING_ORBIT) {
                        vx = -cz; vz = cx; // tangent — reads as orbiting
                    } else {
                        vx = cx; vz = cz;
                    }
                    level.sendParticles(particle,
                            origin.x + cx * radius, origin.y + 0.1, origin.z + cz * radius,
                            0, vx, 0.05, vz, spec.speed());
                }
            }
            case HELIX -> {
                double turns = 2.0;
                for (int i = 0; i < count; i++) {
                    double t = (double) i / Math.max(1, count - 1);
                    double angle = Math.PI * 2 * turns * t;
                    double r = Math.max(0.3, radius);
                    level.sendParticles(particle,
                            origin.x + Math.cos(angle) * r,
                            origin.y + t * Math.max(0.5, height),
                            origin.z + Math.sin(angle) * r,
                            0, 0, 0.6, 0, spec.speed());
                }
            }
            case DOME -> {
                // Fibonacci hemisphere for even coverage, velocity outward along the shell.
                for (int i = 0; i < count; i++) {
                    double t = (i + 0.5) / count;
                    double inclination = Math.acos(1 - t);        // 0..PI/2 (upper half)
                    double azimuth = Math.PI * (1 + Math.sqrt(5)) * i;
                    double sx = Math.sin(inclination) * Math.cos(azimuth);
                    double sy = Math.cos(inclination);
                    double sz = Math.sin(inclination) * Math.sin(azimuth);
                    level.sendParticles(particle,
                            origin.x + sx * radius, origin.y + sy * radius, origin.z + sz * radius,
                            0, sx, sy, sz, spec.speed());
                }
            }
            case LINE -> {
                if (lineTarget == null) {
                    level.sendParticles(particle, origin.x, origin.y, origin.z, count,
                            0.3, 0.3, 0.3, spec.speed());
                    return;
                }
                Vec3 step = lineTarget.subtract(origin).scale(1.0 / Math.max(1, count - 1));
                Vec3 dir = lineTarget.subtract(origin).normalize();
                for (int i = 0; i < count; i++) {
                    Vec3 p = origin.add(step.scale(i));
                    level.sendParticles(particle, p.x, p.y, p.z,
                            0, dir.x, dir.y, dir.z, spec.speed());
                }
            }
            case SPOKES -> {
                int spokes = 8;
                int perSpoke = Math.max(1, count / spokes);
                for (int s = 0; s < spokes; s++) {
                    double angle = (Math.PI * 2 * s) / spokes;
                    double cx = Math.cos(angle);
                    double cz = Math.sin(angle);
                    for (int i = 1; i <= perSpoke; i++) {
                        double dist = radius * i / perSpoke;
                        level.sendParticles(particle,
                                origin.x + cx * dist, origin.y + 0.05, origin.z + cz * dist,
                                0, 0, 0.02, 0, spec.speed());
                    }
                }
            }
            case CONE -> {
                if (look == null || look.lengthSqr() < 1.0e-4) {
                    // No aim available (position-based entry point) — degrade to a small cloud.
                    level.sendParticles(particle, origin.x, origin.y, origin.z, count,
                            0.4, 0.4, 0.4, spec.speed());
                    return;
                }
                Vec3 dir = look.normalize();
                // Raise from the feet-anchored origin toward the eyes so the jet leaves the face.
                Vec3 eye = origin.add(0, 1.2, 0);
                double range = Math.max(1.0, radius);
                double endRadius = Math.max(0.15, height);
                // Start the visual cone out from the face so first person isn't blinded
                // (same trick as ConeBreathAction).
                double startDist = Math.min(1.5, range * 0.25);
                for (int i = 0; i < count; i++) {
                    double t = (i + 0.5) / count;
                    Vec3 axis = eye.add(dir.scale(startDist + (range - startDist) * t));
                    double spread = endRadius * t;
                    double ox = (level.random.nextDouble() * 2 - 1) * spread;
                    double oy = (level.random.nextDouble() * 2 - 1) * spread;
                    double oz = (level.random.nextDouble() * 2 - 1) * spread;
                    level.sendParticles(particle,
                            axis.x + ox, axis.y + oy, axis.z + oz,
                            0, dir.x, dir.y, dir.z, spec.speed());
                }
            }
            case BURST_UP -> {
                // Fountain column: golden-angle azimuths in a spreadX disc, staggered up
                // the spreadY height, all velocity straight up.
                for (int i = 0; i < count; i++) {
                    double azimuth = Math.PI * (1 + Math.sqrt(5)) * i;
                    double r = radius * Math.sqrt((i + 0.5) / count);
                    double y = Math.max(0.5, height) * i / Math.max(1, count - 1);
                    level.sendParticles(particle,
                            origin.x + Math.cos(azimuth) * r,
                            origin.y + y,
                            origin.z + Math.sin(azimuth) * r,
                            0, 0, 1, 0, spec.speed());
                }
            }
        }
    }

    public static void showRunicBanner(ServerPlayer player, SignatureEntry entry, Object... args) {
        // Bannerless entries (weakness onset cues) leave the words to the notification system.
        if (entry.bannerKey() == null || entry.bannerKey().isEmpty()) return;
        // bannerKey is a translation key; runtime substitutions (e.g. an enchantment name) pass
        // straight through as Component.translatable args, mapping to %s in the localized value.
        MutableComponent component = (args.length == 0
                ? Component.translatable(entry.bannerKey())
                : Component.translatable(entry.bannerKey(), args))
                .withStyle(entry.bannerColor());
        if (entry.bannerBold()) {
            component = component.withStyle(ChatFormatting.BOLD);
        }
        player.displayClientMessage(component, true);
    }

    public static void showScreenCue(ServerPlayer player, CueType cue, int durationTicks) {
        NetworkHandler.sendToPlayer(player, new S2CScreenCuePacket(cue, durationTicks));
    }
}
