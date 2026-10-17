package com.otectus.runic_races.integration.pehkui;

/** Pure calculations for the movement contribution owned by Runic Races. */
final class RacialMovementScales {
    private RacialMovementScales() {}

    static float jumpMultiplier(float appliedBaseScale, boolean enabled) {
        if (!enabled || !isShrunk(appliedBaseScale)) return 1.0f;
        // Pehkui scales movement displacement, not gravity. Jump height is roughly
        // quadratic in launch speed: 1 / scale overcompensates. 1 / sqrt(scale)
        // clears a block at every native racial size (1.15–1.25 blocks of rise).
        return (float) (1.0 / Math.sqrt(appliedBaseScale));
    }

    static float fallMultiplier(float appliedBaseScale) {
        // Pehkui's FALLING defaults to 1 / MOTION. Cancel only the extra fall
        // distance from our shrinking; external motion/falling scales still compose.
        return isShrunk(appliedBaseScale) ? appliedBaseScale : 1.0f;
    }

    private static boolean isShrunk(float scale) {
        return Float.isFinite(scale) && scale > 0.0f && scale < 1.0f;
    }
}
