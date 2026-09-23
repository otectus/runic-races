package com.otectus.runic_races.presentation;

import net.minecraft.world.phys.Vec3;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

class EffectGeometryTest {
    @Test
    void aimedPlanesStayOrthogonalAtVerticalAndDiagonalAim() {
        for (Vec3 aim : new Vec3[]{Vec3.ZERO, new Vec3(0, 1, 0), new Vec3(0, -1, 0),
                new Vec3(1, 2, -3), new Vec3(0, 0, 1)}) {
            EffectGeometry frame = EffectGeometry.along(aim);
            assertEquals(1, frame.forward().length(), 1.0e-9);
            assertEquals(1, frame.right().length(), 1.0e-9);
            assertEquals(1, frame.up().length(), 1.0e-9);
            assertEquals(0, frame.forward().dot(frame.right()), 1.0e-9);
            assertEquals(0, frame.forward().dot(frame.up()), 1.0e-9);
            for (int i = 0; i < 32; i++) {
                Vec3 radial = frame.radial(i * Math.PI / 16);
                assertEquals(0, radial.dot(frame.forward()), 1.0e-9);
                assertEquals(1, radial.length(), 1.0e-9);
                assertTrue(Double.isFinite(radial.y));
                Vec3 cone = frame.coneOffset(7, 2, i * Math.PI / 16);
                assertEquals(7, cone.length(), 1.0e-9);
                assertTrue(cone.normalize().dot(frame.forward()) > Math.cos(Math.toRadians(22)));
            }
        }
    }

    @Test
    void pullBackLiftsAHitOffTheSurfaceWithoutNaN() {
        Vec3 eyes = new Vec3(0, 2, 0);
        Vec3 hit = new Vec3(0, 2, 4);
        Vec3 lifted = EffectGeometry.pullBack(eyes, hit, 0.15);
        assertEquals(3.85, lifted.z, 1.0e-9);
        assertEquals(eyes.distanceTo(hit) - 0.15, eyes.distanceTo(lifted), 1.0e-9);

        // Segment shorter than the pull-back distance clamps to the origin.
        assertEquals(eyes, EffectGeometry.pullBack(eyes, new Vec3(0, 2, 0.1), 0.15));

        // Coincident points: no zero-vector normalize, no NaN.
        Vec3 same = EffectGeometry.pullBack(eyes, eyes, 0.15);
        assertEquals(eyes, same);
        assertTrue(Double.isFinite(same.x) && Double.isFinite(same.y) && Double.isFinite(same.z));
    }
}
