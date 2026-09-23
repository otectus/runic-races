package com.otectus.runic_races.network;

import com.otectus.runic_races.RunicRacesMod;
import com.otectus.runic_races.util.RaceHelper;
import io.github.edwinmindcraft.origins.api.capabilities.IOriginContainer;
import io.github.edwinmindcraft.origins.common.OriginsCommon;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.server.level.ServerPlayer;
import net.minecraftforge.network.NetworkEvent;
import net.minecraftforge.network.PacketDistributor;

import java.util.function.Supplier;

/**
 * Sent by the "&lt; Back" button on the race selection screen. Un-chooses the player's
 * family-layer origin on the server and re-syncs the container. The client has already
 * cleared the same layer locally and swapped to the family screen itself
 * (see {@code client/OriginBackButtonHandler}); this packet only makes the server agree.
 * It deliberately does <em>not</em> send Origins' {@code S2COpenOriginScreen}: that arms a
 * reopen flag which only fires while no screen is open, so it would either do nothing or stay
 * armed for the rest of the session.
 */
public class C2SBackToFamilyPacket {

    /** Kept on the wire for format stability; the client now owns the reopen. */
    private final boolean showDirtBackground;

    public C2SBackToFamilyPacket(boolean showDirtBackground) {
        this.showDirtBackground = showDirtBackground;
    }

    public static void encode(C2SBackToFamilyPacket msg, FriendlyByteBuf buf) {
        buf.writeBoolean(msg.showDirtBackground);
    }

    public static C2SBackToFamilyPacket decode(FriendlyByteBuf buf) {
        return new C2SBackToFamilyPacket(buf.readBoolean());
    }

    public static void handle(C2SBackToFamilyPacket msg, Supplier<NetworkEvent.Context> ctx) {
        ctx.get().enqueueWork(() -> {
            ServerPlayer player = ctx.get().getSender();
            if (player == null) return;
            IOriginContainer container = IOriginContainer.get(player).orElse(null);
            if (container == null) return;

            // Only mid-selection: family chosen, race not yet. A finished character
            // replaying this packet must not be able to reset their heritage.
            if (!container.hasOrigin(RaceHelper.FAMILY_LAYER) || container.hasOrigin(RaceHelper.RACE_LAYER)) {
                return;
            }

            container.setOrigin(RaceHelper.FAMILY_LAYER, RaceHelper.EMPTY_ORIGIN);
            container.synchronize();
            // Explicit immediate sync so the client's optimistic clear is confirmed before
            // the player's next choice can possibly be answered.
            OriginsCommon.CHANNEL.send(PacketDistributor.PLAYER.with(() -> player),
                    container.getSynchronizationPacket());
            RunicRacesMod.debug("[RunicRaces] {} backed out of family selection", player.getName().getString());
        });
        ctx.get().setPacketHandled(true);
    }
}
