package com.otectus.runic_races;

import org.junit.jupiter.api.Test;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;

import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * Server-authority regression guard: flap packets are only honored mid-glide.
 * The pre-fix guard ({@code !isFallFlying() && onGround()}) accepted flaps from
 * any airborne moment, letting a modified client chain free vertical boosts
 * without ever deploying wings. The legit client never sends a flap outside a
 * glide, so the strict guard costs nothing and closes the hole.
 */
class FlapGuardTest {

    private static final Path HANDLER =
            Path.of("src/main/java/com/otectus/runic_races/flight/FlightServerHandler.java");

    @Test
    void flapRequiresActiveGlide() throws IOException {
        String source = Files.readString(HANDLER);
        int guard = source.indexOf("if (!player.isFallFlying()) return;");
        int thrust = source.indexOf("setDeltaMovement");
        assertTrue(guard >= 0, "handleFlap must reject non-gliding players outright");
        assertTrue(thrust > guard, "the isFallFlying guard must run before any velocity is applied");
        assertTrue(!source.contains("!player.isFallFlying() && player.onGround()"),
                "the lenient airborne-flap guard must not come back");
    }
}
