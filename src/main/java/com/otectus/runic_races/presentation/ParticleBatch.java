package com.otectus.runic_races.presentation;

import com.otectus.runic_races.config.RRServerConfig;
import com.otectus.runic_races.diagnostics.RRMetrics;
import com.otectus.runic_races.network.NetworkHandler;
import com.otectus.runic_races.network.S2CParticleBatchPacket;
import net.minecraft.core.BlockPos;
import net.minecraft.core.particles.ParticleOptions;
import net.minecraft.network.protocol.Packet;
import net.minecraft.network.protocol.game.ClientboundLevelParticlesPacket;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;

import java.util.Arrays;

/**
 * Collects the particles of one shaped emission and delivers them as one
 * {@link S2CParticleBatchPacket} per nearby player instead of one vanilla particle packet
 * per particle.
 * <p>
 * Each point keeps vanilla's single-particle semantics and visibility rule: a player
 * receives a point when their block centre is within 32 blocks of it
 * ({@code BlockPos.closerToCenterThan}), in the same dimension. A player who can see every
 * point shares one pre-encoded packet; a player on the fringe gets a packet with only the
 * points vanilla would have sent them. With {@code network.batchedParticles} off, every
 * point is sent as the same vanilla packet as before.
 * <p>
 * Server thread only. Callers add points in their authored order.
 */
public final class ParticleBatch {

    /** Vanilla's non-forced particle radius; signature emits apply the same limit to forced packets. */
    private static final double VISIBILITY_RADIUS_SQR = 32.0 * 32.0;

    /** Regression-test seam: forces one transport without writing the config file; null follows config. */
    private static volatile Boolean transportOverride;

    private final ParticleOptions particle;
    private final boolean force;
    private final int mode;
    // Vanilla count-1 spread, shared by a jitter batch.
    private final float spreadX;
    private final float spreadY;
    private final float spreadZ;
    private final float spreadSpeed;

    private double[] positions;
    /** Directed batches: the vanilla packet's (dx, dy, dz, speed) floats per point. */
    private float[] deltas;
    private int size;

    private ParticleBatch(ParticleOptions particle, boolean force, int mode, int expected,
                          double dx, double dy, double dz, double speed) {
        this.particle = particle;
        this.force = force;
        this.mode = mode;
        this.spreadX = (float) dx;
        this.spreadY = (float) dy;
        this.spreadZ = (float) dz;
        this.spreadSpeed = (float) speed;
        int capacity = Math.max(4, Math.min(expected, S2CParticleBatchPacket.MAX_POINTS));
        this.positions = new double[capacity * 3];
        this.deltas = mode == S2CParticleBatchPacket.MODE_DIRECTED ? new float[capacity * 4] : null;
    }

    /**
     * Points that each behave like {@code sendParticles(p, x, y, z, 0, dx, dy, dz, speed)}:
     * one particle with motion {@code (dx, dy, dz) * speed}.
     *
     * @param force whether the client's particle limiter is overridden (signature cues)
     */
    public static ParticleBatch directed(ParticleOptions particle, boolean force, int expected) {
        return new ParticleBatch(particle, force, S2CParticleBatchPacket.MODE_DIRECTED, expected, 0, 0, 0, 0);
    }

    /** Points that each behave like {@code sendParticles(p, x, y, z, 1, dx, dy, dz, speed)}. */
    public static ParticleBatch jitter(ParticleOptions particle, boolean force, int expected,
                                       double dx, double dy, double dz, double speed) {
        return new ParticleBatch(particle, force, S2CParticleBatchPacket.MODE_JITTER, expected, dx, dy, dz, speed);
    }

    /**
     * Forces the batched ({@code true}) or per-particle ({@code false}) transport for
     * equivalence tests; {@code null} returns to {@code network.batchedParticles}. Never
     * persisted — toggling the config value itself would rewrite the TOML file.
     */
    public static void overrideTransport(Boolean batched) {
        transportOverride = batched;
    }

    /** Adds a directed point. */
    public void add(double x, double y, double z, double dx, double dy, double dz, double speed) {
        int index = reserve();
        if (index < 0) return;
        deltas[index * 4] = (float) dx;
        deltas[index * 4 + 1] = (float) dy;
        deltas[index * 4 + 2] = (float) dz;
        deltas[index * 4 + 3] = (float) speed;
        store(index, x, y, z);
    }

    /** Adds a jitter point. */
    public void add(double x, double y, double z) {
        int index = reserve();
        if (index < 0) return;
        store(index, x, y, z);
    }

    public int size() {
        return size;
    }

    private int reserve() {
        if (size >= S2CParticleBatchPacket.MAX_POINTS) return -1;
        if (size * 3 == positions.length) {
            int capacity = Math.min(S2CParticleBatchPacket.MAX_POINTS, size * 2);
            positions = Arrays.copyOf(positions, capacity * 3);
            if (deltas != null) deltas = Arrays.copyOf(deltas, capacity * 4);
        }
        return size++;
    }

    private void store(int index, double x, double y, double z) {
        positions[index * 3] = x;
        positions[index * 3 + 1] = y;
        positions[index * 3 + 2] = z;
    }

    /** Delivers the emission to every player vanilla would have shown each point to. */
    public void send(ServerLevel level) {
        if (size == 0) return;
        RRMetrics.add(RRMetrics.Counter.PARTICLE_EMISSIONS);
        RRMetrics.add(RRMetrics.Counter.PARTICLE_POINTS, size);
        Boolean forced = transportOverride;
        if (!(forced != null ? forced : RRServerConfig.BATCHED_PARTICLES.get())) {
            sendLegacy(level);
            return;
        }
        Packet<?> shared = null;
        boolean[] visible = null;
        for (ServerPlayer player : level.players()) {
            BlockPos at = player.blockPosition();
            int seen = 0;
            for (int i = 0; i < size; i++) {
                if (visibleFrom(at, i)) seen++;
            }
            if (seen == 0) continue;
            if (seen == size) {
                if (shared == null) shared = NetworkHandler.toClientPacket(build(null, size));
                player.connection.send(shared);
            } else {
                if (visible == null) visible = new boolean[size];
                for (int i = 0; i < size; i++) visible[i] = visibleFrom(at, i);
                player.connection.send(NetworkHandler.toClientPacket(build(visible, seen)));
            }
            RRMetrics.add(RRMetrics.Counter.PARTICLE_PACKETS);
        }
    }

    /** Vanilla {@code Vec3i.closerToCenterThan}, arithmetic for arithmetic. */
    private boolean visibleFrom(BlockPos at, int point) {
        double dx = (double) at.getX() + 0.5 - positions[point * 3];
        double dy = (double) at.getY() + 0.5 - positions[point * 3 + 1];
        double dz = (double) at.getZ() + 0.5 - positions[point * 3 + 2];
        return dx * dx + dy * dy + dz * dz < VISIBILITY_RADIUS_SQR;
    }

    S2CParticleBatchPacket build(boolean[] include, int count) {
        // Anchor at the first included point so every offset stays small.
        int first = 0;
        if (include != null) {
            while (!include[first]) first++;
        }
        double ox = positions[first * 3];
        double oy = positions[first * 3 + 1];
        double oz = positions[first * 3 + 2];
        float[] offsets = new float[count * 3];
        float[] motion;
        boolean sharedMotion = false;
        if (mode == S2CParticleBatchPacket.MODE_JITTER) {
            motion = new float[]{spreadX, spreadY, spreadZ, spreadSpeed};
        } else {
            motion = new float[count * 3];
        }
        int written = 0;
        for (int i = 0; i < size; i++) {
            if (include != null && !include[i]) continue;
            offsets[written * 3] = (float) (positions[i * 3] - ox);
            offsets[written * 3 + 1] = (float) (positions[i * 3 + 1] - oy);
            offsets[written * 3 + 2] = (float) (positions[i * 3 + 2] - oz);
            if (mode == S2CParticleBatchPacket.MODE_DIRECTED) {
                // Exactly the client's vanilla computation: float speed * float delta.
                float speed = deltas[i * 4 + 3];
                motion[written * 3] = speed * deltas[i * 4];
                motion[written * 3 + 1] = speed * deltas[i * 4 + 1];
                motion[written * 3 + 2] = speed * deltas[i * 4 + 2];
            }
            written++;
        }
        if (mode == S2CParticleBatchPacket.MODE_DIRECTED && count > 1) {
            sharedMotion = true;
            for (int i = 3; i < motion.length && sharedMotion; i++) {
                sharedMotion = Float.floatToIntBits(motion[i]) == Float.floatToIntBits(motion[i % 3]);
            }
            if (sharedMotion) motion = Arrays.copyOf(motion, 3);
        }
        return new S2CParticleBatchPacket(particle, force, mode, ox, oy, oz, count, offsets, motion, sharedMotion);
    }

    /** The pre-batching transport: one vanilla particle packet per point, per nearby player. */
    private void sendLegacy(ServerLevel level) {
        for (int i = 0; i < size; i++) {
            double x = positions[i * 3];
            double y = positions[i * 3 + 1];
            double z = positions[i * 3 + 2];
            ClientboundLevelParticlesPacket packet = mode == S2CParticleBatchPacket.MODE_JITTER
                    ? new ClientboundLevelParticlesPacket(particle, force, x, y, z, spreadX, spreadY, spreadZ, spreadSpeed, 1)
                    : new ClientboundLevelParticlesPacket(particle, force, x, y, z,
                    deltas[i * 4], deltas[i * 4 + 1], deltas[i * 4 + 2], deltas[i * 4 + 3], 0);
            for (ServerPlayer player : level.players()) {
                if (visibleFrom(player.blockPosition(), i)) {
                    player.connection.send(packet);
                    RRMetrics.add(RRMetrics.Counter.PARTICLE_PACKETS);
                }
            }
        }
    }
}
