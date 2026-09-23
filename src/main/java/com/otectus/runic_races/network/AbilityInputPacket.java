package com.otectus.runic_races.network;

import com.otectus.runic_races.ability.AbilityService;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraftforge.network.NetworkEvent;
import java.util.function.Supplier;

/** Only a sequenced input edge crosses the trust boundary. No target, position or tuning. */
public record AbilityInputPacket(long sequence, boolean down) {
    public static void encode(AbilityInputPacket p, FriendlyByteBuf b) { b.writeVarLong(p.sequence); b.writeBoolean(p.down); }
    public static AbilityInputPacket decode(FriendlyByteBuf b) { return new AbilityInputPacket(b.readVarLong(), b.readBoolean()); }
    public static void handle(AbilityInputPacket p, Supplier<NetworkEvent.Context> context) {
        var sender = context.get().getSender();
        if (sender != null && p.sequence >= 0) AbilityService.input(sender, p.sequence, p.down);
        context.get().setPacketHandled(true);
    }
}
