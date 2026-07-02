package com.otectus.runic_races.presentation;

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
 * actionbar banner and optional screen cue in one shot.
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

        playSignatureSfx(level, player.position(), entry);
        spawnSignatureVfx(level, player.position(), entry, lineTarget);
        showRunicBanner(player, entry, bannerArgs);
        if (entry.screenCue() != null) {
            showScreenCue(player, entry.screenCue(), entry.screenCueDurationTicks());
        }
    }

    public static void playSignatureSfx(ServerLevel level, Vec3 pos, SignatureEntry entry) {
        for (SfxSpec spec : entry.sounds()) {
            level.playSound(null, pos.x, pos.y, pos.z, spec.sound().get(), SoundSource.PLAYERS, spec.volume(), spec.pitch());
        }
    }

    public static void spawnSignatureVfx(ServerLevel level, Vec3 pos, SignatureEntry entry) {
        spawnSignatureVfx(level, pos, entry, null);
    }

    public static void spawnSignatureVfx(ServerLevel level, Vec3 pos, SignatureEntry entry, Vec3 lineTarget) {
        Vec3 origin = pos.add(0, 0.3, 0);
        for (VfxSpec spec : entry.particles()) {
            spawnShaped(level, origin, spec, lineTarget);
        }
    }

    /**
     * Places each particle of a shaped spec individually. Directed motion uses the
     * vanilla count-0 trick: {@code sendParticles(p, x, y, z, 0, dx, dy, dz, speed)}
     * gives the single particle velocity {@code (dx, dy, dz) * speed}.
     */
    private static void spawnShaped(ServerLevel level, Vec3 origin, VfxSpec spec, Vec3 lineTarget) {
        var particle = spec.particle().get();
        int count = spec.count();
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
        }
    }

    public static void showRunicBanner(ServerPlayer player, SignatureEntry entry, Object... args) {
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
