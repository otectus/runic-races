package com.otectus.runic_races;

import org.junit.jupiter.api.Test;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;
import java.util.Set;
import java.util.stream.Stream;

import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * Guards the single-source-of-truth rule for offensive targeting: the mob-aggro
 * hostility heuristic ({@code getTarget()}) lives only in {@code util/Hostility.java}.
 * Every server-side AoE (afflict/glow/tremor, breath cones, Valen's shoulder check)
 * must route through it so "who counts as an enemy" can never drift per-ability —
 * that drift is exactly how the defending-pet and friendly-fire bugs happened.
 *
 * The client ambience handler keeps a cosmetic copy by design: mob targets are
 * server-only data, so the client heuristic is a different (visual-only) contract.
 */
class HostilityConsolidationTest {

    private static final Path MAIN_SRC = Path.of("src/main/java/com/otectus/runic_races");

    private static final Set<String> ALLOWED = Set.of(
            "util/Hostility.java",
            "client/ClientRacialAmbienceHandler.java"
    );

    @Test
    void aggroHeuristicOnlyInHostilityHelper() throws IOException {
        List<String> offenders = new ArrayList<>();
        try (Stream<Path> files = Files.walk(MAIN_SRC)) {
            for (Path file : files.filter(f -> f.toString().endsWith(".java")).toList()) {
                String rel = MAIN_SRC.relativize(file).toString().replace('\\', '/');
                if (ALLOWED.contains(rel)) continue;
                String source = Files.readString(file);
                if (source.contains(".getTarget()")) {
                    offenders.add(rel + " re-implements the aggro heuristic — use util/Hostility instead");
                }
            }
        }
        assertTrue(offenders.isEmpty(), String.join("\n", offenders));
    }
}
