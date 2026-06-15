package com.otectus.runic_races.network;

import net.minecraft.network.FriendlyByteBuf;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.fml.DistExecutor;
import net.minecraftforge.network.NetworkEvent;

import java.util.function.Supplier;

/**
 * Server -> client: push the full {@code RaceStateFlags} bitfield for the owning player.
 * Sent by {@code RaceStateTracker} whenever a flag actually changes.
 */
public class S2CRaceStatePacket {

    private final int flags;

    public S2CRaceStatePacket(int flags) {
        this.flags = flags;
    }

    public int flags() { return flags; }

    public static void encode(S2CRaceStatePacket msg, FriendlyByteBuf buf) {
        buf.writeInt(msg.flags);
    }

    public static S2CRaceStatePacket decode(FriendlyByteBuf buf) {
        return new S2CRaceStatePacket(buf.readInt());
    }

    public static void handle(S2CRaceStatePacket msg, Supplier<NetworkEvent.Context> ctx) {
        ctx.get().enqueueWork(() ->
                DistExecutor.unsafeRunWhenOn(Dist.CLIENT, () -> () -> ClientHandler.receive(msg))
        );
        ctx.get().setPacketHandled(true);
    }

    private static final class ClientHandler {
        private static void receive(S2CRaceStatePacket msg) {
            com.otectus.runic_races.client.state.ClientRaceState.set(msg.flags);
        }
    }
}
