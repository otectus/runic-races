package com.otectus.runic_races.presentation;

import com.otectus.runic_races.action.ConeBreathAction.Element;
import com.otectus.runic_races.network.S2CBreathVfxPacket;
import io.netty.buffer.Unpooled;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.phys.Vec3;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

class BreathVfxPacketTest {
    @Test
    void everyElementPreservesCastSnapshotAcrossWire() {
        for (Element element : Element.values()) {
            S2CBreathVfxPacket cast = new S2CBreathVfxPacket(42, new ResourceLocation("minecraft:overworld"),
                    element, new Vec3(12.5, 76.25, -16), new Vec3(0, -1, 0), 7, 22, 1.5);
            FriendlyByteBuf buffer = new FriendlyByteBuf(Unpooled.buffer());
            try {
                S2CBreathVfxPacket.encode(cast, buffer);
                assertEquals(cast, S2CBreathVfxPacket.decode(buffer));
                assertEquals(0, buffer.readableBytes());
            } finally {
                buffer.release();
            }
        }
    }

    @Test
    void malformedNumericParametersCannotCreateUnboundedEmitters() {
        S2CBreathVfxPacket cast = new S2CBreathVfxPacket(1, new ResourceLocation("minecraft:overworld"),
                Element.FIRE, new Vec3(Double.NaN, 1, 2), Vec3.ZERO,
                Double.POSITIVE_INFINITY, -100, Double.NaN);
        assertEquals(Vec3.ZERO, cast.origin());
        assertEquals(1, cast.direction().length(), 1.0e-9);
        assertEquals(0, cast.density());
        assertTrue(cast.range() >= 0.5 && cast.range() <= 16);
        assertTrue(cast.halfAngle() >= 1 && cast.halfAngle() <= 60);
    }
}
