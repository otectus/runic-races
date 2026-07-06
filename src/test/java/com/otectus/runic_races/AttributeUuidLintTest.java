package com.otectus.runic_races;

import org.junit.jupiter.api.Test;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;
import java.util.stream.Stream;

import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * Datapack-facing power factories must derive their attribute-modifier UUIDs from
 * the power's configuration ({@code UUID.nameUUIDFromBytes}) rather than fixed
 * {@code UUID.fromString} constants: two powers of the same type stacked on one
 * entity by a datapack would otherwise silently last-writer-win a shared modifier.
 * (Fixed UUIDs elsewhere — integrations, event handlers — are fine: those are
 * one-instance-per-player systems, not datapack-stackable.)
 */
class AttributeUuidLintTest {

    private static final Path POWER_SRC = Path.of("src/main/java/com/otectus/runic_races/power");

    @Test
    void powerFactoriesDeriveModifierUuidsFromConfig() throws IOException {
        List<String> offenders = new ArrayList<>();
        try (Stream<Path> files = Files.walk(POWER_SRC)) {
            for (Path file : files.filter(f -> f.toString().endsWith(".java")).toList()) {
                String source = Files.readString(file);
                if (source.contains("UUID.fromString(")) {
                    offenders.add(file.getFileName()
                            + " uses a fixed modifier UUID — derive it from the power config"
                            + " (UUID.nameUUIDFromBytes) so stacked powers don't collide");
                }
            }
        }
        assertTrue(offenders.isEmpty(), String.join("\n", offenders));
    }
}
