package com.otectus.runic_races.network;

import net.minecraft.network.FriendlyByteBuf;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.fml.DistExecutor;
import net.minecraftforge.network.NetworkEvent;

import java.util.function.Supplier;

/**
 * Server -> client: the local player's current Human "Adaptation" stack count.
 *
 * The {@code ADAPTATION_ACTIVE} flag only carries a boolean; this packet surfaces the
 * actual stack number so {@code StateRuneOverlay} can render it on the "A" rune. Sent
 * only when the count changes (diffed server-side in {@code RacialEventHandler}).
 */
public class S2CAdaptationStacksPacket {

    private final int stacks;

    public S2CAdaptationStacksPacket(int stacks) {
        this.stacks = stacks;
    }

    public int stacks() { return stacks; }

    public static void encode(S2CAdaptationStacksPacket msg, FriendlyByteBuf buf) {
        buf.writeVarInt(msg.stacks);
    }

    public static S2CAdaptationStacksPacket decode(FriendlyByteBuf buf) {
        return new S2CAdaptationStacksPacket(buf.readVarInt());
    }

    public static void handle(S2CAdaptationStacksPacket msg, Supplier<NetworkEvent.Context> ctx) {
        // Registered via consumerMainThread, so we're already on the client main thread here.
        DistExecutor.unsafeRunWhenOn(Dist.CLIENT, () -> () -> ClientHandler.receive(msg));
        ctx.get().setPacketHandled(true);
    }

    private static final class ClientHandler {
        private static void receive(S2CAdaptationStacksPacket msg) {
            com.otectus.runic_races.client.state.ClientRaceState.setAdaptationStacks(msg.stacks);
        }
    }
}
