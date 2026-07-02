package com.otectus.runic_races;

import com.otectus.runic_races.race.RaceRegistry;
import org.junit.jupiter.api.Test;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;
import java.util.Set;
import java.util.TreeSet;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * Guards the per-race ambience contract: every race either has a routine in
 * {@code ClientRacialAmbienceHandler.ROUTINES} or is on the explicit
 * intentionally-quiet list (races whose identity lives in combat procs or
 * signature moments instead of idle ambience). A race missing from both is a
 * gap someone forgot, not a decision.
 *
 * Source-scanned — the handler's static init references Minecraft classes that
 * are compileOnly and not loadable under plain JUnit.
 */
class AmbienceCoverageTest {

    private static final Path HANDLER_SRC =
            Path.of("src/main/java/com/otectus/runic_races/client/ClientRacialAmbienceHandler.java");

    private static final Pattern ROUTINE_ENTRY = Pattern.compile("ROUTINES\\.put\\(\"([a-z_]+)\"");

    /** Races that are deliberately ambience-quiet (documented in the handler's comments). */
    private static final Set<String> INTENTIONALLY_QUIET = Set.of(
            "valen",     // identity is procs: shoulder-check, fall shake
            "blood_elf", // identity is the lifesteal siphon + heartbeat cue
            "arachnid",  // web-sense is threat-driven, not idle state
            "runic_one", // ward glyph ring marks its zone via the signature
            "reaper"     // mythic revival/harvest moments; idle quiet is the point
    );

    @Test
    void everyRaceHasAnAmbienceRoutineOrIsExplicitlyQuiet() throws IOException {
        Set<String> routines = new TreeSet<>();
        Matcher m = ROUTINE_ENTRY.matcher(Files.readString(HANDLER_SRC));
        while (m.find()) {
            routines.add(m.group(1));
        }
        assertTrue(!routines.isEmpty(), "Expected ROUTINES.put entries in ClientRacialAmbienceHandler");

        List<String> problems = new ArrayList<>();
        for (String race : RaceRegistry.allRaceNames()) {
            if (!routines.contains(race) && !INTENTIONALLY_QUIET.contains(race)) {
                problems.add(race + " has no ambience routine and is not on the intentionally-quiet list");
            }
        }
        for (String race : routines) {
            if (!RaceRegistry.allRaceNames().contains(race)) {
                problems.add("Ambience routine for unknown race: " + race);
            }
            if (INTENTIONALLY_QUIET.contains(race)) {
                problems.add(race + " is both routine-registered and on the quiet list — remove one");
            }
        }
        assertTrue(problems.isEmpty(), String.join("\n", problems));
    }
}
