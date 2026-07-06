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
    private static final String PROTOCOL_VERSION = "2";
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

        RunicRacesMod.LOGGER.info("[RunicRaces] Network channel registered ({} packets)", id);
    }

    public static void sendToServer(Object msg) {
        channel.send(PacketDistributor.SERVER.noArg(), msg);
    }

    public static void sendToPlayer(ServerPlayer player, Object msg) {
        channel.send(PacketDistributor.PLAYER.with(() -> player), msg);
    }
}
