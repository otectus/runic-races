package com.otectus.runic_races.presentation;

import net.minecraft.world.phys.Vec3;

/** Stable orthogonal frame, including when the player aims straight up or down. */
public record EffectGeometry(Vec3 forward, Vec3 right, Vec3 up) {
    public static EffectGeometry along(Vec3 look) {
        Vec3 forward = look == null || look.lengthSqr() < 1.0e-6 ? new Vec3(0, 0, 1) : look.normalize();
        Vec3 reference = Math.abs(forward.y) > 0.95 ? new Vec3(1, 0, 0) : new Vec3(0, 1, 0);
        Vec3 right = forward.cross(reference).normalize();
        return new EffectGeometry(forward, right, right.cross(forward).normalize());
    }

    public Vec3 radial(double angle) {
        return right.scale(Math.cos(angle)).add(up.scale(Math.sin(angle)));
    }

    /**
     * Moves {@code hit} back along the hit→{@code from} segment by {@code distance},
     * lifting a surface-clipped particle off the block face it landed on. Clamps to
     * {@code from} when the segment is shorter than {@code distance}, and returns
     * {@code from} unchanged when the two points coincide (no zero-vector normalize).
     */
    public static Vec3 pullBack(Vec3 from, Vec3 hit, double distance) {
        Vec3 back = from.subtract(hit);
        double length = back.length();
        if (length < 1.0e-6) return from;
        if (length <= distance) return from;
        return hit.add(back.scale(distance / length));
    }

    /** Radial depth is measured from the eyes, like the server's spherical cone hit test. */
    public Vec3 coneOffset(double distance, double width, double angle) {
        return forward.scale(distance).add(radial(angle).scale(width)).normalize().scale(distance);
    }
}
