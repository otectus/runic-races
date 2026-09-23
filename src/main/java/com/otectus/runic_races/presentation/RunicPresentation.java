package com.otectus.runic_races.presentation;

import com.otectus.runic_races.config.RRServerConfig;
import com.otectus.runic_races.diagnostics.RRMetrics;
import com.otectus.runic_races.network.NetworkHandler;
import com.otectus.runic_races.network.S2CScreenCuePacket;
import com.otectus.runic_races.presentation.SignatureEntry.SfxSpec;
import com.otectus.runic_races.presentation.SignatureEntry.VfxSpec;
import net.minecraft.ChatFormatting;
import net.minecraft.core.particles.ParticleOptions;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.MutableComponent;
import net.minecraft.network.protocol.game.ClientboundLevelParticlesPacket;
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
                spawnOneVfx(level, origin, look, spec, lineTarget, player.getEyePosition());
            } else {
                boolean snapshot = switch (key) {
                    case FIRE_DRAKE_BREATH, ICE_DRAKE_BREATH, SEA_SERPEN_BREATH,
                            TERRA_DRAKE_BREATH, VOLT_DRAKE_BREATH, WIND_WYRM_BREATH -> true;
                    default -> false;
                };
                PresentationScheduler.scheduleVfx(player, spec, lineTarget, snapshot);
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
        spawnOneVfx(level, origin, look, spec, lineTarget, origin.add(0, 1.2, 0));
    }

    static void spawnOneVfx(ServerLevel level, Vec3 origin, Vec3 look, VfxSpec spec, Vec3 lineTarget, Vec3 eyes) {
        spawnShaped(level, origin, look, spec, lineTarget, eyes);
    }

    /**
     * Scales an authored count by {@code vfx.signatureParticleDensity}. Shaped
     * emissions keep a small floor so a turned-down ring still reads as a ring
     * rather than a bug; 0.0 disables signature particles outright.
     */
    private static int scaledCount(VfxSpec spec) {
        int floor = switch (spec.shape()) {
            case POINT, LINE -> 1;
            default -> Math.min(spec.count(), 6);
        };
        return ParticleBudget.scale(spec.count(), RRServerConfig.SIGNATURE_PARTICLE_DENSITY.get(), floor);
    }

    /** How far a surface-clipped particle is lifted off the block face it landed on. */
    private static final double SURFACE_PULLBACK = 0.15;

    /** Beyond this range a signature cue is not worth a forced packet. */
    private static final double FORCE_RADIUS = 32.0;

    /**
     * Sends one particle emission, forcing it past the client's particle limiter so
     * signature cues still read at {@code Particles: Minimal}. The public per-player
     * {@code sendParticles} overload ties the override to a 512-block radius, so the
     * packet is built and delivered by hand to nearby players.
     */
    private static void emit(ServerLevel level, ParticleOptions particle,
                             double x, double y, double z, int count,
                             double dx, double dy, double dz, double speed) {
        var packet = new ClientboundLevelParticlesPacket(particle, true, x, y, z,
                (float) dx, (float) dy, (float) dz, (float) speed, count);
        Vec3 at = new Vec3(x, y, z);
        RRMetrics.add(RRMetrics.Counter.PARTICLE_EMISSIONS);
        RRMetrics.add(RRMetrics.Counter.PARTICLE_POINTS, count);
        for (ServerPlayer player : level.players()) {
            if (player.blockPosition().closerToCenterThan(at, FORCE_RADIUS)) {
                player.connection.send(packet);
                RRMetrics.add(RRMetrics.Counter.PARTICLE_PACKETS);
            }
        }
    }

    /**
     * Places each particle of a shaped spec individually. Directed motion uses the
     * vanilla count-0 trick: {@code sendParticles(p, x, y, z, 0, dx, dy, dz, speed)}
     * gives the single particle velocity {@code (dx, dy, dz) * speed}. The points of one
     * emission are delivered together by {@link ParticleBatch}; POINT bursts and the
     * no-target fallbacks are already a single vanilla packet.
     */
    private static void spawnShaped(ServerLevel level, Vec3 origin, Vec3 look, VfxSpec spec, Vec3 lineTarget, Vec3 eyes) {
        var particle = spec.particle().get();
        int count = scaledCount(spec);
        if (count <= 0) return;
        double radius = spec.spreadX();
        double height = spec.spreadY();
        // Every non-POINT shape places particles one by one; they travel as one batch.
        ParticleBatch batch = ParticleBatch.directed(particle, true, count);
        switch (spec.shape()) {
            case POINT -> emit(level, particle, origin.x, origin.y, origin.z, count,
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
                    batch.add(origin.x + cx * radius, origin.y + 0.1, origin.z + cz * radius,
                            vx, 0.05, vz, spec.speed());
                }
            }
            case HELIX -> {
                double turns = 2.0;
                for (int i = 0; i < count; i++) {
                    double t = (double) i / Math.max(1, count - 1);
                    double angle = Math.PI * 2 * turns * t;
                    double r = Math.max(0.3, radius);
                    batch.add(origin.x + Math.cos(angle) * r,
                            origin.y + t * Math.max(0.5, height),
                            origin.z + Math.sin(angle) * r,
                            0, 0.6, 0, spec.speed());
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
                    batch.add(origin.x + sx * radius, origin.y + sy * (height > 0 ? height : radius), origin.z + sz * radius,
                            sx, sy, sz, spec.speed());
                }
            }
            case LINE -> {
                if (lineTarget == null) {
                    emit(level, particle, origin.x, origin.y, origin.z, count,
                            0.3, 0.3, 0.3, spec.speed());
                    return;
                }
                Vec3 step = lineTarget.subtract(origin).scale(1.0 / Math.max(1, count - 1));
                Vec3 dir = lineTarget.subtract(origin).normalize();
                for (int i = 0; i < count; i++) {
                    Vec3 p = origin.add(step.scale(i));
                    batch.add(p.x, p.y, p.z,
                            dir.x, dir.y, dir.z, spec.speed());
                }
            }
            case SPOKES -> {
                int spokes = Math.min(8, count);
                for (int s = 0; s < spokes; s++) {
                    int perSpoke = count / spokes + (s < count % spokes ? 1 : 0);
                    double angle = (Math.PI * 2 * s) / spokes;
                    double cx = Math.cos(angle);
                    double cz = Math.sin(angle);
                    for (int i = 1; i <= perSpoke; i++) {
                        double dist = radius * i / perSpoke;
                        batch.add(origin.x + cx * dist, origin.y + 0.05, origin.z + cz * dist,
                                0, 0.02, 0, spec.speed());
                    }
                }
            }
            case CONE -> {
                if (look == null || look.lengthSqr() < 1.0e-4) {
                    // No aim available (position-based entry point) — degrade to a small cloud.
                    emit(level, particle, origin.x, origin.y, origin.z, count,
                            0.4, 0.4, 0.4, spec.speed());
                    return;
                }
                EffectGeometry frame = EffectGeometry.along(look);
                Vec3 dir = frame.forward();
                double range = Math.max(1.0, radius);
                double endRadius = Math.max(0.15, height);
                // Start the visual cone out from the face so first person isn't blinded
                // (same trick as ConeBreathAction).
                double startDist = Math.min(1.5, range * 0.25);
                for (int i = 0; i < count; i++) {
                    double t = (i + 0.5) / count;
                    Vec3 axis = eyes.add(dir.scale(startDist + (range - startDist) * t));
                    double spread = endRadius * t * Math.sqrt(level.random.nextDouble());
                    Vec3 sample = axis.add(frame.radial(i * 2.399963).scale(spread));
                    var hit = level.clip(new net.minecraft.world.level.ClipContext(eyes, sample,
                            net.minecraft.world.level.ClipContext.Block.COLLIDER,
                            net.minecraft.world.level.ClipContext.Fluid.NONE, null));
                    Vec3 spot = hit.getType() == net.minecraft.world.phys.HitResult.Type.MISS
                            ? sample
                            : EffectGeometry.pullBack(eyes, hit.getLocation(), SURFACE_PULLBACK);
                    batch.add(spot.x, spot.y, spot.z,
                            dir.x, dir.y, dir.z, spec.speed());
                }
            }
            case BURST_UP -> {
                // Fountain column: golden-angle azimuths in a spreadX disc, staggered up
                // the spreadY height, all velocity straight up.
                for (int i = 0; i < count; i++) {
                    double azimuth = Math.PI * (1 + Math.sqrt(5)) * i;
                    double r = radius * Math.sqrt((i + 0.5) / count);
                    double y = Math.max(0.5, height) * i / Math.max(1, count - 1);
                    batch.add(origin.x + Math.cos(azimuth) * r,
                            origin.y + y,
                            origin.z + Math.sin(azimuth) * r,
                            0, 1, 0, spec.speed());
                }
            }
            case SHIELD, WAVE -> {
                EffectGeometry frame = EffectGeometry.along(look);
                double distance = spec.shape() == SignatureEntry.Shape.WAVE ? height
                        : (spec.spreadZ() > 0 ? spec.spreadZ() : 0.9);
                double halfHeight = Math.max(0.15, height / 2);
                Vec3 center = (spec.shape() == SignatureEntry.Shape.WAVE ? eyes : origin.add(0, halfHeight, 0))
                        .add(frame.forward().scale(distance));
                for (int i = 0; i < count; i++) {
                    double angle = 2 * Math.PI * i / count;
                    double vertical = spec.shape() == SignatureEntry.Shape.SHIELD ? halfHeight : radius;
                    // Shield perimeter with four inset corner points gives it a faceted, voxel-friendly face.
                    double inset = spec.shape() == SignatureEntry.Shape.SHIELD && i % 4 == 0 ? 0.6 : 1;
                    Vec3 p = center.add(frame.right().scale(Math.cos(angle) * radius * inset))
                            .add(frame.up().scale(Math.sin(angle) * vertical * inset));
                    Vec3 spot = p;
                    if (spec.shape() == SignatureEntry.Shape.WAVE) {
                        var hit = level.clip(new net.minecraft.world.level.ClipContext(eyes, p,
                                net.minecraft.world.level.ClipContext.Block.COLLIDER,
                                net.minecraft.world.level.ClipContext.Fluid.NONE, null));
                        if (hit.getType() != net.minecraft.world.phys.HitResult.Type.MISS) {
                            // Aiming at a wall or the floor: paint the cue on the surface
                            // instead of losing the beat entirely.
                            spot = EffectGeometry.pullBack(eyes, hit.getLocation(), SURFACE_PULLBACK);
                        }
                    }
                    batch.add(spot.x, spot.y, spot.z,
                            frame.forward().x, frame.forward().y, frame.forward().z, spec.speed());
                }
            }
            case ARC -> {
                Vec3 flat = new Vec3(look.x, 0, look.z);
                EffectGeometry frame = EffectGeometry.along(flat);
                for (int i = 0; i < count; i++) {
                    double angle = -Math.PI * 0.4 + Math.PI * 0.8 * i / Math.max(1, count - 1);
                    Vec3 out = frame.forward().scale(Math.cos(angle)).add(frame.right().scale(Math.sin(angle)));
                    Vec3 p = origin.add(0, height, 0).add(out.scale(radius));
                    batch.add(p.x, p.y, p.z, out.x, 0.04, out.z, spec.speed());
                }
            }
            case SIGIL -> {
                int rim = Math.max(1, count * 2 / 3);
                for (int i = 0; i < count; i++) {
                    double x;
                    double z;
                    if (i < rim) {
                        double angle = Math.PI * 2 * i / rim;
                        x = Math.cos(angle) * radius;
                        z = Math.sin(angle) * radius;
                    } else {
                        double edge = (i - rim) * 4.0 / Math.max(1, count - rim);
                        int side = (int) edge;
                        double t = edge - side;
                        double a = side * Math.PI / 2;
                        double b = a + Math.PI / 2;
                        x = radius * 0.8 * ((1 - t) * Math.cos(a) + t * Math.cos(b));
                        z = radius * 0.8 * ((1 - t) * Math.sin(a) + t * Math.sin(b));
                    }
                    batch.add(origin.x + x, origin.y + 0.02, origin.z + z,
                            0, 1, 0, spec.speed());
                }
            }
        }
        batch.send(level);
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
