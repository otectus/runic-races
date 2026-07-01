package com.otectus.runic_races.network;

import com.otectus.runic_races.presentation.CueType;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.fml.DistExecutor;
import net.minecraftforge.network.NetworkEvent;

import java.util.function.Supplier;

/**
 * Server -> client: trigger a short-lived screen overlay cue
 * (freeze-flash, vignette pulse, heartbeat flash, etc.).
 *
 * Delivered via {@link NetworkHandler#sendToPlayer}; rendered by
 * {@code ScreenCueRenderer} on the client side.
 */
public class S2CScreenCuePacket {

    private final CueType cue;
    private final int durationTicks;

    public S2CScreenCuePacket(CueType cue, int durationTicks) {
        this.cue = cue;
        this.durationTicks = durationTicks;
    }

    public CueType cue() { return cue; }
    public int durationTicks() { return durationTicks; }

    public static void encode(S2CScreenCuePacket msg, FriendlyByteBuf buf) {
        buf.writeEnum(msg.cue);
        buf.writeVarInt(msg.durationTicks);
    }

    public static S2CScreenCuePacket decode(FriendlyByteBuf buf) {
        return new S2CScreenCuePacket(buf.readEnum(CueType.class), buf.readVarInt());
    }

    public static void handle(S2CScreenCuePacket msg, Supplier<NetworkEvent.Context> ctx) {
        // Registered via consumerMainThread, so we're already on the client main thread here.
        DistExecutor.unsafeRunWhenOn(Dist.CLIENT, () -> () -> ClientHandler.receive(msg));
        ctx.get().setPacketHandled(true);
    }

    /** Isolated holder so the client class is only loaded on the physical client. */
    private static final class ClientHandler {
        private static void receive(S2CScreenCuePacket msg) {
            com.otectus.runic_races.client.presentation.ScreenCueRenderer.enqueue(msg.cue, msg.durationTicks);
        }
    }
}
