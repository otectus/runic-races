package com.otectus.runic_races.network;

import io.netty.buffer.Unpooled;
import io.netty.handler.codec.DecoderException;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.resources.ResourceLocation;
import org.junit.jupiter.api.Test;

import java.util.ArrayList;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

class PowerDataPacketTest {

    private static CompoundTag value(int v) {
        CompoundTag tag = new CompoundTag();
        tag.putInt("Value", v);
        return tag;
    }

    @Test
    void entriesSurviveTheWire() {
        S2CPowerDataPacket packet = new S2CPowerDataPacket(1234, List.of(
                new S2CPowerDataPacket.Entry(new ResourceLocation("runic_races", "fire_drake/dragonfire_breath_cooldown_timer"), value(790)),
                new S2CPowerDataPacket.Entry(new ResourceLocation("runic_races", "avian/skyborne_flap_cooldown_timer"), value(0))));
        FriendlyByteBuf buffer = new FriendlyByteBuf(Unpooled.buffer());
        try {
            S2CPowerDataPacket.encode(packet, buffer);
            // One cooldown step is tiny next to a full power-container snapshot.
            assertTrue(buffer.readableBytes() < 160, "encoded " + buffer.readableBytes() + " bytes");
            S2CPowerDataPacket decoded = S2CPowerDataPacket.decode(buffer);
            assertEquals(packet, decoded);
            assertEquals(0, buffer.readableBytes());
        } finally {
            buffer.release();
        }
    }

    @Test
    void oversizedBatchesAreRejectedBeforeAllocation() {
        FriendlyByteBuf buffer = new FriendlyByteBuf(Unpooled.buffer());
        try {
            buffer.writeVarInt(1);
            buffer.writeVarInt(S2CPowerDataPacket.MAX_ENTRIES + 1);
            assertThrows(DecoderException.class, () -> S2CPowerDataPacket.decode(buffer));
        } finally {
            buffer.release();
        }
    }

    @Test
    void oversizedEntryNbtIsRejected() {
        CompoundTag huge = new CompoundTag();
        huge.putString("padding", "x".repeat(4096));
        List<S2CPowerDataPacket.Entry> entries = new ArrayList<>();
        entries.add(new S2CPowerDataPacket.Entry(new ResourceLocation("runic_races", "x"), huge));
        FriendlyByteBuf buffer = new FriendlyByteBuf(Unpooled.buffer());
        try {
            S2CPowerDataPacket.encode(new S2CPowerDataPacket(1, entries), buffer);
            assertThrows(RuntimeException.class, () -> S2CPowerDataPacket.decode(buffer));
        } finally {
            buffer.release();
        }
    }
}
