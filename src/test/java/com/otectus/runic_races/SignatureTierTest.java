package com.otectus.runic_races;

import org.junit.jupiter.api.Test;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * Enforces the CLAUDE.md VFX density guideline on the Java signature recipes:
 * summed authored particle counts per entry must sit in the entry's Intensity band
 * (MINOR ≤ 20, MAJOR 30-60, MYTHIC ≥ 80), staged beats must stay within the
 * scheduler's clamp, and every entry must have at least one zero-delay spec so
 * activation feedback is instant.
 *
 * Source-scanned (SignatureRegistry's static init builds Minecraft particle
 * options, which are compileOnly). Relies on the registry's formatting
 * convention: one {@code new VfxSpec(<particle-constant>, <count>, ...)} per
 * spec, optionally suffixed {@code .delayed(n)} — documented in the
 * SignatureRegistry header.
 */
class SignatureTierTest {

    private static final Path REGISTRY_SRC =
            Path.of("src/main/java/com/otectus/runic_races/presentation/SignatureRegistry.java");

    private static final int MINOR_MAX = 20;
    private static final int MAJOR_MIN = 30;
    private static final int MAJOR_MAX = 60;
    private static final int MYTHIC_MIN = 80;

    private static final Pattern ENTRY_START = Pattern.compile("ENTRIES\\.put\\(SignatureKey\\.([A-Z_]+)");
    private static final Pattern VFX_COUNT = Pattern.compile("new VfxSpec\\([\\w.]+,\\s*(\\d+)");
    private static final Pattern INTENSITY = Pattern.compile("Intensity\\.(MINOR|MAJOR|MYTHIC)");
    private static final Pattern DELAYED = Pattern.compile("\\.delayed\\((\\d+)\\)");
    private static final Pattern ANY_SPEC = Pattern.compile("new [SV]fxSpec\\(");

    /**
     * Entries allowed outside their band, with the dated reason:
     * - 2026-07: the six draconic breath accents stay small on purpose — the Major-band
     *   bulk is the runtime cone from ConeBreathAction (see SignatureRegistry comment).
     * - 2026-07: SPRITE_WING_FLAP sits 2 over the Minor ceiling; wing flaps are the
     *   sprite's identity moment and 20 reads too thin at its flap cadence.
     */
    private static final Set<String> KNOWN_EXCEPTIONS = Set.of(
            "FIRE_DRAKE_BREATH",
            "ICE_DRAKE_BREATH",
            "SEA_SERPEN_BREATH",
            "TERRA_DRAKE_BREATH",
            "VOLT_DRAKE_BREATH",
            "WIND_WYRM_BREATH",
            "SPRITE_WING_FLAP"
    );

    @Test
    void everyEntrySitsInItsIntensityBand() throws IOException {
        List<String> problems = new ArrayList<>();
        for (Map.Entry<String, String> entry : entryBlocks().entrySet()) {
            String key = entry.getKey();
            String block = entry.getValue();

            int sum = 0;
            Matcher counts = VFX_COUNT.matcher(block);
            while (counts.find()) {
                sum += Integer.parseInt(counts.group(1));
            }

            Matcher intensity = INTENSITY.matcher(block);
            if (!intensity.find()) {
                problems.add(key + ": no Intensity token found");
                continue;
            }
            if (KNOWN_EXCEPTIONS.contains(key)) {
                continue;
            }
            String band = intensity.group(1);
            boolean ok = switch (band) {
                case "MINOR" -> sum <= MINOR_MAX;
                case "MAJOR" -> sum >= MAJOR_MIN && sum <= MAJOR_MAX;
                case "MYTHIC" -> sum >= MYTHIC_MIN;
                default -> false;
            };
            if (!ok) {
                problems.add(key + ": " + sum + " particles is outside the " + band + " band");
            }
        }
        assertTrue(problems.isEmpty(), "Tier band violations:\n" + String.join("\n", problems));
    }

    @Test
    void stagedBeatsAreClampSafeAndEveryEntryHasAnInstantBeat() throws IOException {
        List<String> problems = new ArrayList<>();
        for (Map.Entry<String, String> entry : entryBlocks().entrySet()) {
            String key = entry.getKey();
            String block = entry.getValue();

            Matcher delays = DELAYED.matcher(block);
            int delayedSpecs = 0;
            while (delays.find()) {
                delayedSpecs++;
                int delay = Integer.parseInt(delays.group(1));
                if (delay < 1 || delay > 100) {
                    problems.add(key + ": .delayed(" + delay + ") outside the scheduler clamp [1, 100]");
                }
            }

            Matcher specs = ANY_SPEC.matcher(block);
            int totalSpecs = 0;
            while (specs.find()) {
                totalSpecs++;
            }
            if (totalSpecs > 0 && delayedSpecs >= totalSpecs) {
                problems.add(key + ": every spec is delayed — activation gives no instant feedback");
            }
        }
        assertTrue(problems.isEmpty(), "Staging violations:\n" + String.join("\n", problems));
    }

    /** Splits the registry source into per-entry blocks keyed by SignatureKey name. */
    private static Map<String, String> entryBlocks() throws IOException {
        String source = Files.readString(REGISTRY_SRC);
        Map<String, String> blocks = new LinkedHashMap<>();
        Matcher starts = ENTRY_START.matcher(source);
        int prevEnd = -1;
        String prevKey = null;
        while (starts.find()) {
            if (prevKey != null) {
                blocks.put(prevKey, source.substring(prevEnd, starts.start()));
            }
            prevKey = starts.group(1);
            prevEnd = starts.end();
        }
        if (prevKey != null) {
            blocks.put(prevKey, source.substring(prevEnd));
        }
        assertTrue(!blocks.isEmpty(), "Expected ENTRIES.put blocks in SignatureRegistry.java");
        return blocks;
    }
}
