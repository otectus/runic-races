package com.otectus.runic_races.network;

import com.otectus.runic_races.RunicRacesMod;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerPlayer;
import net.minecraftforge.network.NetworkDirection;
import net.minecraftforge.network.NetworkRegistry;
import net.minecraftforge.network.PacketDistributor;
import net.minecraftforge.network.simple.SimpleChannel;

public final class NetworkHandler {

    // Bump whenever the message list changes — mismatched jars must fail the
    // handshake cleanly instead of silently misrouting packet ids.
    private static final String PROTOCOL_VERSION = "5";
    private static SimpleChannel channel;

    private NetworkHandler() {}

    public static void init() {
        channel = NetworkRegistry.newSimpleChannel(
                new ResourceLocation(RunicRacesMod.MOD_ID, "main"),
                () -> PROTOCOL_VERSION,
                PROTOCOL_VERSION::equals,
                PROTOCOL_VERSION::equals
        );

        int id = 0;
        channel.messageBuilder(FlightFlapPacket.class, id++, NetworkDirection.PLAY_TO_SERVER)
                .encoder(FlightFlapPacket::encode)
                .decoder(FlightFlapPacket::decode)
                .consumerMainThread(FlightFlapPacket::handle)
                .add();
        channel.messageBuilder(FlightCancelPacket.class, id++, NetworkDirection.PLAY_TO_SERVER)
                .encoder(FlightCancelPacket::encode)
                .decoder(FlightCancelPacket::decode)
                .consumerMainThread(FlightCancelPacket::handle)
                .add();
        channel.messageBuilder(S2CScreenCuePacket.class, id++, NetworkDirection.PLAY_TO_CLIENT)
                .encoder(S2CScreenCuePacket::encode)
                .decoder(S2CScreenCuePacket::decode)
                .consumerMainThread(S2CScreenCuePacket::handle)
                .add();
        channel.messageBuilder(S2CRaceStatePacket.class, id++, NetworkDirection.PLAY_TO_CLIENT)
                .encoder(S2CRaceStatePacket::encode)
                .decoder(S2CRaceStatePacket::decode)
                .consumerMainThread(S2CRaceStatePacket::handle)
                .add();
        channel.messageBuilder(S2CAdaptationStacksPacket.class, id++, NetworkDirection.PLAY_TO_CLIENT)
                .encoder(S2CAdaptationStacksPacket::encode)
                .decoder(S2CAdaptationStacksPacket::decode)
                .consumerMainThread(S2CAdaptationStacksPacket::handle)
                .add();
        // APPEND ONLY: register new packets below existing ones so ids stay stable,
        // and bump PROTOCOL_VERSION with every addition.
        channel.messageBuilder(C2SBackToFamilyPacket.class, id++, NetworkDirection.PLAY_TO_SERVER)
                .encoder(C2SBackToFamilyPacket::encode)
                .decoder(C2SBackToFamilyPacket::decode)
                .consumerMainThread(C2SBackToFamilyPacket::handle)
                .add();

        channel.messageBuilder(AbilityInputPacket.class, id++, NetworkDirection.PLAY_TO_SERVER)
                .encoder(AbilityInputPacket::encode).decoder(AbilityInputPacket::decode)
                .consumerMainThread(AbilityInputPacket::handle).add();
        channel.messageBuilder(AbilitySnapshot.class, id++, NetworkDirection.PLAY_TO_CLIENT)
                .encoder(AbilitySnapshot::encode).decoder(AbilitySnapshot::decode)
                .consumerMainThread(AbilitySnapshot::handle).add();
        channel.messageBuilder(S2CBreathVfxPacket.class, id++, NetworkDirection.PLAY_TO_CLIENT)
                .encoder(S2CBreathVfxPacket::encode).decoder(S2CBreathVfxPacket::decode)
                .consumerMainThread(S2CBreathVfxPacket::handle).add();
        // Protocol 5: cooldown decay values and batched shaped particles.
        channel.messageBuilder(S2CPowerDataPacket.class, id++, NetworkDirection.PLAY_TO_CLIENT)
                .encoder(S2CPowerDataPacket::encode).decoder(S2CPowerDataPacket::decode)
                .consumerMainThread(S2CPowerDataPacket::handle).add();
        channel.messageBuilder(S2CParticleBatchPacket.class, id++, NetworkDirection.PLAY_TO_CLIENT)
                .encoder(S2CParticleBatchPacket::encode).decoder(S2CParticleBatchPacket::decode)
                .consumerMainThread(S2CParticleBatchPacket::handle).add();
        RunicRacesMod.LOGGER.info("[RunicRaces] Network channel registered ({} packets)", id);
    }

    public static void sendToServer(Object msg) {
        channel.send(PacketDistributor.SERVER.noArg(), msg);
    }

    public static void sendToPlayer(ServerPlayer player, Object msg) {
        channel.send(PacketDistributor.PLAYER.with(() -> player), msg);
    }

    /** The owner (if a player) and every player tracking {@code entity} — Apoli's sync audience. */
    public static void sendToTrackingAndSelf(net.minecraft.world.entity.Entity entity, Object msg) {
        channel.send(PacketDistributor.TRACKING_ENTITY_AND_SELF.with(() -> entity), msg);
    }

    /** Encodes {@code msg} once so the same packet can be handed to several connections. */
    public static net.minecraft.network.protocol.Packet<?> toClientPacket(Object msg) {
        return channel.toVanillaPacket(msg, NetworkDirection.PLAY_TO_CLIENT);
    }

    public static void sendNear(net.minecraft.server.level.ServerLevel level,
                                net.minecraft.world.phys.Vec3 origin, double radius, Object msg) {
        channel.send(PacketDistributor.NEAR.with(() -> new PacketDistributor.TargetPoint(
                origin.x, origin.y, origin.z, radius, level.dimension())), msg);
    }
}
