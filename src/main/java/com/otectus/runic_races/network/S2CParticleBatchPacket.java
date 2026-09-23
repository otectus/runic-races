package com.otectus.runic_races.network;

import io.netty.handler.codec.DecoderException;
import net.minecraft.core.particles.ParticleOptions;
import net.minecraft.core.particles.ParticleType;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.util.RandomSource;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.fml.DistExecutor;
import net.minecraftforge.network.NetworkEvent;

import java.util.function.Supplier;

/**
 * One shaped particle emission — a ring, line, dome, cone, trail — in a single packet.
 * <p>
 * The server still computes every position (and every random draw or block clip that
 * shapes them); only the transport changes. The client replays each point exactly as it
 * would a vanilla {@code ClientboundLevelParticlesPacket}:
 * <ul>
 *   <li>{@link #MODE_DIRECTED} — vanilla count 0: one particle at the point, motion
 *       {@code (float) speed * (float) delta} per axis.</li>
 *   <li>{@link #MODE_JITTER} — vanilla count 1: one particle offset by a gaussian of the
 *       shared spread, with a gaussian of the shared speed as motion.</li>
 * </ul>
 * Positions travel as float offsets from a double origin; motion as floats, the same
 * precision vanilla uses. A batch holds at most {@link #MAX_POINTS} points.
 */
public final class S2CParticleBatchPacket {

    public static final int MAX_POINTS = 512;
    public static final int MODE_DIRECTED = 0;
    public static final int MODE_JITTER = 1;
    private static final int FLAG_SHARED_MOTION = 2;

    final ParticleOptions particle;
    final boolean force;
    final int mode;
    final double originX;
    final double originY;
    final double originZ;
    final int count;
    /** x,y,z offsets from the origin, three floats per point. */
    final float[] offsets;
    /** DIRECTED: motion, three floats per point or three shared. JITTER: dx, dy, dz, speed. */
    final float[] motion;
    final boolean sharedMotion;

    public S2CParticleBatchPacket(ParticleOptions particle, boolean force, int mode,
                           double originX, double originY, double originZ,
                           int count, float[] offsets, float[] motion, boolean sharedMotion) {
        this.particle = particle;
        this.force = force;
        this.mode = mode;
        this.originX = originX;
        this.originY = originY;
        this.originZ = originZ;
        this.count = count;
        this.offsets = offsets;
        this.motion = motion;
        this.sharedMotion = sharedMotion;
    }

    public int count() {
        return count;
    }

    public int mode() {
        return mode;
    }

    public boolean sharedMotion() {
        return sharedMotion;
    }

    /** World position of point {@code i}, as the client reconstructs it. */
    public double x(int i) {
        return originX + offsets[i * 3];
    }

    public double y(int i) {
        return originY + offsets[i * 3 + 1];
    }

    public double z(int i) {
        return originZ + offsets[i * 3 + 2];
    }

    /** DIRECTED: motion component {@code axis} of point {@code i}. JITTER: spread/speed at {@code axis} 0..3. */
    public float motion(int i, int axis) {
        if (mode == MODE_JITTER) return motion[axis];
        return motion[(sharedMotion ? 0 : i * 3) + axis];
    }

    public static void encode(S2CParticleBatchPacket msg, FriendlyByteBuf buf) {
        buf.writeId(BuiltInRegistries.PARTICLE_TYPE, msg.particle.getType());
        msg.particle.writeToNetwork(buf);
        buf.writeBoolean(msg.force);
        buf.writeByte(msg.mode | (msg.sharedMotion ? FLAG_SHARED_MOTION : 0));
        buf.writeDouble(msg.originX).writeDouble(msg.originY).writeDouble(msg.originZ);
        buf.writeVarInt(msg.count);
        for (float value : msg.motion) buf.writeFloat(value);
        for (int i = 0; i < msg.count * 3; i++) buf.writeFloat(msg.offsets[i]);
    }

    public static S2CParticleBatchPacket decode(FriendlyByteBuf buf) {
        ParticleType<?> type = buf.readById(BuiltInRegistries.PARTICLE_TYPE);
        if (type == null) throw new DecoderException("Runic Races particle batch with unknown particle type");
        ParticleOptions particle = readParticle(buf, type);
        boolean force = buf.readBoolean();
        int flags = buf.readByte();
        int mode = flags & 1;
        boolean shared = (flags & FLAG_SHARED_MOTION) != 0;
        double x = buf.readDouble();
        double y = buf.readDouble();
        double z = buf.readDouble();
        int count = buf.readVarInt();
        if (count < 0 || count > MAX_POINTS) {
            throw new DecoderException("Runic Races particle batch with " + count + " points");
        }
        int motionFloats = mode == MODE_JITTER ? 4 : shared ? 3 : count * 3;
        float[] motion = new float[motionFloats];
        for (int i = 0; i < motionFloats; i++) motion[i] = buf.readFloat();
        float[] offsets = new float[count * 3];
        for (int i = 0; i < offsets.length; i++) offsets[i] = buf.readFloat();
        return new S2CParticleBatchPacket(particle, force, mode, x, y, z, count, offsets, motion, shared);
    }

    private static <T extends ParticleOptions> T readParticle(FriendlyByteBuf buf, ParticleType<T> type) {
        return type.getDeserializer().fromNetwork(type, buf);
    }

    public static void handle(S2CParticleBatchPacket msg, Supplier<NetworkEvent.Context> ctx) {
        DistExecutor.unsafeRunWhenOn(Dist.CLIENT, () -> () -> ClientHandler.receive(msg));
        ctx.get().setPacketHandled(true);
    }

    private static final class ClientHandler {
        private static final RandomSource RANDOM = RandomSource.createNewThreadLocalInstance();

        private static void receive(S2CParticleBatchPacket msg) {
            var level = net.minecraft.client.Minecraft.getInstance().level;
            if (level == null) return;
            try {
                for (int i = 0; i < msg.count; i++) {
                    double x = msg.originX + msg.offsets[i * 3];
                    double y = msg.originY + msg.offsets[i * 3 + 1];
                    double z = msg.originZ + msg.offsets[i * 3 + 2];
                    if (!Double.isFinite(x) || !Double.isFinite(y) || !Double.isFinite(z)) continue;
                    if (msg.mode == MODE_JITTER) {
                        double speed = msg.motion[3];
                        level.addParticle(msg.particle, msg.force,
                                x + RANDOM.nextGaussian() * msg.motion[0],
                                y + RANDOM.nextGaussian() * msg.motion[1],
                                z + RANDOM.nextGaussian() * msg.motion[2],
                                RANDOM.nextGaussian() * speed, RANDOM.nextGaussian() * speed, RANDOM.nextGaussian() * speed);
                    } else {
                        int m = msg.sharedMotion ? 0 : i * 3;
                        level.addParticle(msg.particle, msg.force, x, y, z,
                                msg.motion[m], msg.motion[m + 1], msg.motion[m + 2]);
                    }
                }
            } catch (Throwable throwable) {
                // Same containment as vanilla's particle packet handler.
                com.otectus.runic_races.RunicRacesMod.LOGGER.warn("Could not spawn particle effect {}", msg.particle);
            }
        }
    }
}
