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
import java.util.stream.Stream;

import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * Guards the cooldown-HUD contract: every race in {@code RaceRegistry} must have
 * an {@code AbilityIconRegistry} registration (or its HUD is empty), and every
 * key-activated power ({@code origins:active_self}) must have its cooldown
 * resource represented as an icon literal (or that ability has no cooldown readout).
 *
 * AbilityIconRegistry is source-scanned — its static init constructs ItemStacks,
 * which need the Minecraft registry bootstrap unavailable under plain JUnit.
 */
class AbilityIconCoverageTest {

    private static final Path POWERS = Path.of("src/main/resources/data/runic_races/powers");
    private static final Path REGISTRY_SRC =
            Path.of("src/main/java/com/otectus/runic_races/client/AbilityIconRegistry.java");

    private static final Pattern REGISTER_RACE = Pattern.compile("register\\(\"([a-z_]+)\"");

    @Test
    void everyRaceHasAnAbilityIconRegistration() throws IOException {
        String source = Files.readString(REGISTRY_SRC);
        Set<String> registered = new TreeSet<>();
        Matcher m = REGISTER_RACE.matcher(source);
        while (m.find()) {
            registered.add(m.group(1));
        }

        List<String> problems = new ArrayList<>();
        for (String race : RaceRegistry.allRaceNames()) {
            if (!registered.contains(race)) {
                problems.add(race + " has no AbilityIconRegistry entry — cooldown HUD renders nothing");
            }
        }
        for (String race : registered) {
            if (!RaceRegistry.allRaceNames().contains(race)) {
                problems.add("AbilityIconRegistry registers unknown race \"" + race + "\"");
            }
        }
        assertTrue(problems.isEmpty(), String.join("\n", problems));
    }

    @Test
    void everyKeyActivatedPowerHasAnIconForItsCooldownResource() throws IOException {
        String source = Files.readString(REGISTRY_SRC);
        List<String> problems = new ArrayList<>();
        for (Path json : listFiles(POWERS, ".json")) {
            String text = Files.readString(json);
            if (!text.contains("\"origins:active_self\"")) continue;
            String race = json.getParent().getFileName().toString();
            String file = json.getFileName().toString().replace(".json", "");
            String literal = race + "/" + file + "_cooldown_timer";
            if (!source.contains("\"" + literal + "\"")) {
                problems.add(literal + " (active_self power) has no AbilityIconRegistry icon");
            }
        }
        assertTrue(problems.isEmpty(), String.join("\n", problems));
    }

    private static List<Path> listFiles(Path root, String suffix) throws IOException {
        try (Stream<Path> stream = Files.walk(root)) {
            return stream.filter(p -> p.toString().endsWith(suffix)).sorted().toList();
        }
    }
}
