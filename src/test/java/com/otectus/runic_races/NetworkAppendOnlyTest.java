package com.otectus.runic_races;

import org.junit.jupiter.api.Test;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotEquals;

/**
 * Locks the network contract: packet ids are assigned by registration order, so
 * the order below is wire format. New packets go at the END of the list (never
 * mid-list — that silently shifts every later id), and any change to the list
 * must bump PROTOCOL_VERSION so mismatched jars fail the handshake cleanly
 * instead of misrouting packets. If this test fails, you changed the list:
 * append, bump the protocol, then update the expected order here in the same
 * commit — the diff is the review trail.
 */
class NetworkAppendOnlyTest {

    private static final Path HANDLER =
            Path.of("src/main/java/com/otectus/runic_races/network/NetworkHandler.java");

    private static final List<String> EXPECTED_ORDER = List.of(
            "FlightFlapPacket",
            "FlightCancelPacket",
            "S2CScreenCuePacket",
            "S2CRaceStatePacket",
            "S2CAdaptationStacksPacket",
            "C2SBackToFamilyPacket"
    );

    private static final String EXPECTED_PROTOCOL = "2";

    @Test
    void registrationOrderIsLocked() throws IOException {
        String source = Files.readString(HANDLER);
        List<String> actual = new ArrayList<>();
        Matcher m = Pattern.compile("messageBuilder\\((\\w+)\\.class").matcher(source);
        while (m.find()) {
            actual.add(m.group(1));
        }
        assertEquals(EXPECTED_ORDER, actual,
                "Packet registration order changed — append only, bump PROTOCOL_VERSION, update this fixture");
    }

    @Test
    void protocolVersionMatchesFixture() throws IOException {
        String source = Files.readString(HANDLER);
        Matcher m = Pattern.compile("PROTOCOL_VERSION = \"(\\d+)\"").matcher(source);
        assertEquals(true, m.find(), "PROTOCOL_VERSION constant not found");
        assertEquals(EXPECTED_PROTOCOL, m.group(1),
                "PROTOCOL_VERSION changed — update this fixture in the same commit as the packet-list change");
        assertNotEquals("1", m.group(1), "protocol 1 predates the append-only rule");
    }
}
