package com.otectus.runic_races.network;

import com.otectus.runic_races.action.ConeBreathAction.Element;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.phys.Vec3;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.fml.DistExecutor;
import net.minecraftforge.network.NetworkEvent;

import java.util.function.Supplier;

/** One cast snapshot creates a complete, bounded client torrent for every nearby viewer. */
public record S2CBreathVfxPacket(int casterId, ResourceLocation dimension, Element element,
                                Vec3 origin, Vec3 direction, double range, double halfAngle,
                                double density) {
    public S2CBreathVfxPacket {
        range = bounded(range, 0.5, 16);
        halfAngle = bounded(halfAngle, 1, 60);
        density = bounded(density, 0, 2);
        if (!finite(origin)) origin = Vec3.ZERO;
        direction = finite(direction) && direction.lengthSqr() > 1.0e-6
                ? direction.normalize() : new Vec3(0, 0, 1);
    }

    private static boolean finite(Vec3 v) {
        return Double.isFinite(v.x) && Double.isFinite(v.y) && Double.isFinite(v.z);
    }

    private static double bounded(double value, double min, double max) {
        return Double.isFinite(value) ? Math.max(min, Math.min(max, value)) : min;
    }

    public static void encode(S2CBreathVfxPacket msg, FriendlyByteBuf buf) {
        buf.writeVarInt(msg.casterId);
        buf.writeResourceLocation(msg.dimension);
        buf.writeEnum(msg.element);
        buf.writeDouble(msg.origin.x).writeDouble(msg.origin.y).writeDouble(msg.origin.z);
        buf.writeDouble(msg.direction.x).writeDouble(msg.direction.y).writeDouble(msg.direction.z);
        buf.writeDouble(msg.range).writeDouble(msg.halfAngle).writeDouble(msg.density);
    }

    public static S2CBreathVfxPacket decode(FriendlyByteBuf buf) {
        int caster = buf.readVarInt();
        ResourceLocation dimension = buf.readResourceLocation();
        int ordinal = buf.readVarInt();
        Element element = ordinal >= 0 && ordinal < Element.values().length ? Element.values()[ordinal] : Element.FIRE;
        return new S2CBreathVfxPacket(caster, dimension, element,
                new Vec3(buf.readDouble(), buf.readDouble(), buf.readDouble()),
                new Vec3(buf.readDouble(), buf.readDouble(), buf.readDouble()),
                buf.readDouble(), buf.readDouble(), buf.readDouble());
    }

    public static void handle(S2CBreathVfxPacket msg, Supplier<NetworkEvent.Context> ctx) {
        DistExecutor.unsafeRunWhenOn(Dist.CLIENT, () -> () -> ClientHandler.receive(msg));
        ctx.get().setPacketHandled(true);
    }

    private static final class ClientHandler {
        private static void receive(S2CBreathVfxPacket msg) {
            com.otectus.runic_races.client.presentation.BreathVfxRenderer.enqueue(msg);
        }
    }
}
