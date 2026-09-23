package com.otectus.runic_races;

import org.junit.jupiter.api.Test;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * Locks the two invariants of the race-screen "Back" button. Origins Forge sets its
 * {@code OPEN_NEXT_LAYER} latch on every confirm and never clears it; if the handler stops
 * clearing it, the second heritage pick skips the (family-conditioned) race layer and the
 * selection closes with no race. And the back-out must be a screen-to-screen swap: any
 * {@code setScreen(null)} grabs the mouse and pulls the cursor to the window centre, and the
 * server must not arm Origins' {@code S2COpenOriginScreen} reopen, which only fires while no
 * screen is open.
 */
class OriginBackOutTest {

    private static final Path HANDLER =
            Path.of("src/main/java/com/otectus/runic_races/client/OriginBackButtonHandler.java");
    private static final Path PACKET =
            Path.of("src/main/java/com/otectus/runic_races/network/C2SBackToFamilyPacket.java");

    @Test
    void backButtonClearsOriginsStaleNextLayerLatch() throws IOException {
        String src = Files.readString(HANDLER);
        assertTrue(src.contains("OPEN_NEXT_LAYER.set(false)"),
                "OriginBackButtonHandler must clear OriginsClient.OPEN_NEXT_LAYER, or the second "
                        + "heritage pick advances past the race layer before the confirm lands");
    }

    @Test
    void backOutNeverPassesThroughANullScreen() throws IOException {
        String handler = Files.readString(HANDLER);
        assertFalse(handler.contains("setScreen(null)"),
                "setScreen(null) grabs the mouse and recentres the cursor; swap screen -> screen instead");
        String packet = Files.readString(PACKET);
        assertFalse(packet.contains("import io.github.edwinmindcraft.origins.common.network.S2COpenOriginScreen"),
                "the server must not arm Origins' reopen flag; the client owns the family-screen swap");
    }
}
