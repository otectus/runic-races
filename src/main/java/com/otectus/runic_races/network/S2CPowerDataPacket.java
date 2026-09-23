package com.otectus.runic_races.network;

import io.github.edwinmindcraft.apoli.api.component.IPowerContainer;
import io.github.edwinmindcraft.apoli.api.power.configuration.ConfiguredPower;
import io.netty.handler.codec.DecoderException;
import net.minecraft.core.Holder;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.NbtAccounter;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.resources.ResourceLocation;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.fml.DistExecutor;
import net.minecraftforge.network.NetworkEvent;

import java.util.ArrayList;
import java.util.List;
import java.util.function.Supplier;

/**
 * Updates the stored data of individual Apoli powers on one entity — the per-power
 * slice of Apoli's full container sync. The client applies each entry through
 * {@link ConfiguredPower#deserialize}, exactly as the full sync does for that power, so
 * HUD reads and client-side power conditions see the same value either way.
 */
public record S2CPowerDataPacket(int entityId, List<Entry> entries) {

    /** Far above any real batch (a race holds at most three cooldown timers). */
    public static final int MAX_ENTRIES = 64;
    /** Per-entry NBT budget; a resource value serializes to a few bytes. */
    private static final long MAX_ENTRY_NBT_BYTES = 1024;

    public record Entry(ResourceLocation power, CompoundTag data) {}

    public S2CPowerDataPacket {
        entries = List.copyOf(entries);
    }

    public static void encode(S2CPowerDataPacket msg, FriendlyByteBuf buf) {
        buf.writeVarInt(msg.entityId);
        buf.writeVarInt(msg.entries.size());
        for (Entry entry : msg.entries) {
            buf.writeResourceLocation(entry.power());
            buf.writeNbt(entry.data());
        }
    }

    public static S2CPowerDataPacket decode(FriendlyByteBuf buf) {
        int entityId = buf.readVarInt();
        int count = buf.readVarInt();
        if (count < 0 || count > MAX_ENTRIES) {
            throw new DecoderException("Runic Races power data packet with " + count + " entries");
        }
        List<Entry> entries = new ArrayList<>(count);
        for (int i = 0; i < count; i++) {
            ResourceLocation power = buf.readResourceLocation();
            CompoundTag data = buf.readNbt(new NbtAccounter(MAX_ENTRY_NBT_BYTES));
            entries.add(new Entry(power, data == null ? new CompoundTag() : data));
        }
        return new S2CPowerDataPacket(entityId, entries);
    }

    public static void handle(S2CPowerDataPacket msg, Supplier<NetworkEvent.Context> ctx) {
        DistExecutor.unsafeRunWhenOn(Dist.CLIENT, () -> () -> ClientHandler.receive(msg));
        ctx.get().setPacketHandled(true);
    }

    private static final class ClientHandler {
        @SuppressWarnings({"rawtypes", "unchecked"})
        private static void receive(S2CPowerDataPacket msg) {
            var level = net.minecraft.client.Minecraft.getInstance().level;
            if (level == null) return;
            var entity = level.getEntity(msg.entityId);
            if (entity == null) return;
            IPowerContainer container = IPowerContainer.get(entity).orElse(null);
            if (container == null) return;
            for (Entry entry : msg.entries) {
                // A power the client does not hold yet arrives with Apoli's own full sync.
                if (!container.hasPower(entry.power())) continue;
                Holder holder = container.getPower(entry.power());
                if (holder == null || !holder.isBound()) continue;
                ((ConfiguredPower) holder.value()).deserialize(container, entry.data());
            }
        }
    }
}
