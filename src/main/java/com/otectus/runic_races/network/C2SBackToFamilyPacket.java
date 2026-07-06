package com.otectus.runic_races.network;

import com.otectus.runic_races.RunicRacesMod;
import com.otectus.runic_races.util.RaceHelper;
import io.github.edwinmindcraft.origins.api.capabilities.IOriginContainer;
import io.github.edwinmindcraft.origins.api.origin.Origin;
import io.github.edwinmindcraft.origins.api.registry.OriginsDynamicRegistries;
import io.github.edwinmindcraft.origins.common.OriginsCommon;
import io.github.edwinmindcraft.origins.common.network.S2COpenOriginScreen;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.resources.ResourceKey;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerPlayer;
import net.minecraftforge.network.NetworkEvent;
import net.minecraftforge.network.PacketDistributor;

import java.util.function.Supplier;

/**
 * Sent by the "&lt; Back" button on the race selection screen. Un-chooses the player's
 * family-layer origin so Origins re-prompts the whole two-layer selection from the top.
 */
public class C2SBackToFamilyPacket {

    /** Origins' sentinel for "no origin chosen on this layer". */
    private static final ResourceKey<Origin> EMPTY_ORIGIN = ResourceKey.create(
            OriginsDynamicRegistries.ORIGINS_REGISTRY, new ResourceLocation("origins", "empty"));

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

            container.setOrigin(RaceHelper.FAMILY_LAYER, EMPTY_ORIGIN);
            container.synchronize();
            // Explicit ordered sends on Origins' own channel: the client must apply the
            // cleared family BEFORE it rebuilds the unchosen-layer list for the screen.
            OriginsCommon.CHANNEL.send(PacketDistributor.PLAYER.with(() -> player),
                    container.getSynchronizationPacket());
            OriginsCommon.CHANNEL.send(PacketDistributor.PLAYER.with(() -> player),
                    new S2COpenOriginScreen(msg.showDirtBackground));
            RunicRacesMod.debug("[RunicRaces] {} backed out of family selection", player.getName().getString());
        });
        ctx.get().setPacketHandled(true);
    }
}
