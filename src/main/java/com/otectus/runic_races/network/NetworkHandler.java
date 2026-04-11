package com.otectus.runic_races.network;

import com.otectus.runic_races.RunicRacesMod;
import net.minecraft.resources.ResourceLocation;
import net.minecraftforge.network.NetworkRegistry;
import net.minecraftforge.network.PacketDistributor;
import net.minecraftforge.network.simple.SimpleChannel;

public final class NetworkHandler {

    private static final String PROTOCOL_VERSION = "1";
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
        channel.registerMessage(id++, FlightFlapPacket.class,
                FlightFlapPacket::encode, FlightFlapPacket::decode, FlightFlapPacket::handle);
        channel.registerMessage(id++, FlightCancelPacket.class,
                FlightCancelPacket::encode, FlightCancelPacket::decode, FlightCancelPacket::handle);

        RunicRacesMod.LOGGER.info("[RunicRaces] Network channel registered ({} packets)", id);
    }

    public static void sendToServer(Object msg) {
        channel.send(PacketDistributor.SERVER.noArg(), msg);
    }
}
