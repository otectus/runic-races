package com.otectus.runic_races.network;

import com.otectus.runic_races.flight.FlightServerHandler;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.server.level.ServerPlayer;
import net.minecraftforge.network.NetworkEvent;

import java.util.function.Supplier;

public class FlightCancelPacket {

    public FlightCancelPacket() {}

    public static void encode(FlightCancelPacket msg, FriendlyByteBuf buf) {
        // empty payload
    }

    public static FlightCancelPacket decode(FriendlyByteBuf buf) {
        return new FlightCancelPacket();
    }

    public static void handle(FlightCancelPacket msg, Supplier<NetworkEvent.Context> ctx) {
        ctx.get().enqueueWork(() -> {
            ServerPlayer player = ctx.get().getSender();
            if (player != null) {
                FlightServerHandler.handleCancel(player);
            }
        });
        ctx.get().setPacketHandled(true);
    }
}
