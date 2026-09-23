package com.otectus.runic_races.presentation;

import com.otectus.runic_races.network.S2CParticleBatchPacket;
import net.minecraft.core.particles.ParticleOptions;
import net.minecraft.core.particles.ParticleType;
import net.minecraft.network.FriendlyByteBuf;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * The batch transport must hand the client the same particles vanilla packets would:
 * same positions (to float precision), bit-identical motion, and only the points a
 * fringe player would have received.
 */
class ParticleBatchTest {

    private static final ParticleOptions DUMMY = new ParticleOptions() {
        @Override public ParticleType<?> getType() { return null; }
        @Override public void writeToNetwork(FriendlyByteBuf buffer) { }
        @Override public String writeToString() { return "dummy"; }
    };

    @Test
    void directedMotionMatchesVanillaClientArithmetic() {
        ParticleBatch batch = ParticleBatch.directed(DUMMY, true, 24);
        for (int i = 0; i < 24; i++) {
            double angle = Math.PI * 2 * i / 24;
            batch.add(100.3 + Math.cos(angle) * 2.5, 64.1, -2000.7 + Math.sin(angle) * 2.5,
                    Math.cos(angle), 0.05, Math.sin(angle), 0.08);
        }
        S2CParticleBatchPacket packet = batch.build(null, batch.size());
        assertEquals(24, packet.count());
        assertFalse(packet.sharedMotion(), "a ring's motion differs per point");
        for (int i = 0; i < 24; i++) {
            double angle = Math.PI * 2 * i / 24;
            // Vanilla client: (double) (packet.maxSpeed * packet.xDist), both floats.
            assertEquals(Float.floatToIntBits((float) 0.08 * (float) Math.cos(angle)),
                    Float.floatToIntBits(packet.motion(i, 0)));
            assertEquals(Float.floatToIntBits((float) 0.08 * (float) 0.05),
                    Float.floatToIntBits(packet.motion(i, 1)));
            assertEquals(100.3 + Math.cos(angle) * 2.5, packet.x(i), 1.0e-4);
            assertEquals(64.1, packet.y(i), 1.0e-4);
            assertEquals(-2000.7 + Math.sin(angle) * 2.5, packet.z(i), 1.0e-4);
        }
    }

    @Test
    void identicalMotionIsSentOnce() {
        ParticleBatch batch = ParticleBatch.directed(DUMMY, true, 8);
        for (int i = 0; i < 8; i++) {
            batch.add(i * 0.5, 70, 0, 0.6, 0.0, 0.8, 0.12);
        }
        S2CParticleBatchPacket packet = batch.build(null, 8);
        assertTrue(packet.sharedMotion(), "a line's shared direction travels once");
        for (int i = 0; i < 8; i++) {
            assertEquals(Float.floatToIntBits((float) 0.12 * (float) 0.8), Float.floatToIntBits(packet.motion(i, 2)));
            assertEquals(i * 0.5, packet.x(i), 1.0e-6);
        }
    }

    @Test
    void fringeSubsetKeepsOnlyVisiblePointsInOrder() {
        ParticleBatch batch = ParticleBatch.directed(DUMMY, false, 6);
        for (int i = 0; i < 6; i++) {
            batch.add(i, 0, 0, 1, 0, 0, 0.1 * (i + 1));
        }
        boolean[] include = {false, true, false, true, true, false};
        S2CParticleBatchPacket packet = batch.build(include, 3);
        assertEquals(3, packet.count());
        assertEquals(1.0, packet.x(0), 1.0e-6);
        assertEquals(3.0, packet.x(1), 1.0e-6);
        assertEquals(4.0, packet.x(2), 1.0e-6);
        assertEquals(Float.floatToIntBits((float) 0.4), Float.floatToIntBits(packet.motion(1, 0)));
    }

    @Test
    void jitterCarriesVanillaSpreadOnce() {
        ParticleBatch batch = ParticleBatch.jitter(DUMMY, false, 24, 0.05, 0.02, 0.05, 0.0);
        batch.add(1, 2, 3);
        batch.add(4, 5, 6);
        S2CParticleBatchPacket packet = batch.build(null, 2);
        assertEquals(S2CParticleBatchPacket.MODE_JITTER, packet.mode());
        assertEquals((float) 0.05, packet.motion(1, 0));
        assertEquals((float) 0.02, packet.motion(1, 1));
        assertEquals(0.0f, packet.motion(0, 3));
        assertEquals(6.0, packet.z(1), 1.0e-6);
    }

    @Test
    void batchIsBoundedToTheDecoderLimit() {
        ParticleBatch batch = ParticleBatch.directed(DUMMY, true, 4);
        for (int i = 0; i < S2CParticleBatchPacket.MAX_POINTS + 100; i++) {
            batch.add(i, 0, 0, 0, 1, 0, 0.1);
        }
        assertEquals(S2CParticleBatchPacket.MAX_POINTS, batch.size());
    }
}
