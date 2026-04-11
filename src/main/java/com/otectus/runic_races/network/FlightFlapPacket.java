package com.otectus.runic_races.network;

import com.otectus.runic_races.flight.FlightServerHandler;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.server.level.ServerPlayer;
import net.minecraftforge.network.NetworkEvent;

import java.util.function.Supplier;

public class FlightFlapPacket {

    public FlightFlapPacket() {}

    public static void encode(FlightFlapPacket msg, FriendlyByteBuf buf) {
        // empty payload
    }

    public static FlightFlapPacket decode(FriendlyByteBuf buf) {
        return new FlightFlapPacket();
    }

    public static void handle(FlightFlapPacket msg, Supplier<NetworkEvent.Context> ctx) {
        ctx.get().enqueueWork(() -> {
            ServerPlayer player = ctx.get().getSender();
            if (player != null) {
                FlightServerHandler.handleFlap(player);
            }
        });
        ctx.get().setPacketHandled(true);
    }
}
