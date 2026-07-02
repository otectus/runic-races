package com.otectus.runic_races;

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
 * Guards the wing-rendering contract: every race whose power JSON grants
 * {@code origins:elytra_flight} must have a {@code WingType} mapping (so wings
 * actually render), every wing texture referenced by {@code WingType} must exist
 * on disk, and every race with a flap cooldown resource must appear in
 * {@code FlightConfig} (so the flap keybind works).
 *
 * WingType/FlightConfig are scanned as source text — their static init touches
 * Minecraft classes that are compileOnly and not loadable under plain JUnit
 * (same approach as {@link CooldownResourceIdTest}).
 */
class WingCoverageTest {

    private static final Path POWERS = Path.of("src/main/resources/data/runic_races/powers");
    private static final Path ASSETS = Path.of("src/main/resources/assets/runic_races");
    private static final Path WING_TYPE_SRC =
            Path.of("src/main/java/com/otectus/runic_races/client/render/WingType.java");
    private static final Path FLIGHT_CONFIG_SRC =
            Path.of("src/main/java/com/otectus/runic_races/flight/FlightConfig.java");

    private static final Pattern CASE_LITERAL = Pattern.compile("case \"([a-z_]+)\"");
    private static final Pattern TEXTURE_LITERAL = Pattern.compile("\"(textures/entity/[a-z_]+\\.png)\"");
    private static final Pattern FLAP_RESOURCE = Pattern.compile("\"runic_races:([a-z_]+)/[a-z_]+_flap_cooldown_timer\"");

    @Test
    void everyElytraFlightRaceHasAWingTypeMapping() throws IOException {
        Set<String> flightRaces = racesWithPowerText("\"origins:elytra_flight\"");
        assertTrue(!flightRaces.isEmpty(), "Expected at least one race with origins:elytra_flight");

        Set<String> mappedRaces = new TreeSet<>();
        Matcher m = CASE_LITERAL.matcher(Files.readString(WING_TYPE_SRC));
        while (m.find()) {
            mappedRaces.add(m.group(1));
        }

        List<String> problems = new ArrayList<>();
        for (String race : flightRaces) {
            if (!mappedRaces.contains(race)) {
                problems.add(race + " grants elytra_flight but has no WingType.forRaceName case — wings won't render");
            }
        }
        assertTrue(problems.isEmpty(), String.join("\n", problems));
    }

    @Test
    void everyWingTypeMappingBelongsToAFlightRace() throws IOException {
        Set<String> flightRaces = racesWithPowerText("\"origins:elytra_flight\"");
        Matcher m = CASE_LITERAL.matcher(Files.readString(WING_TYPE_SRC));
        List<String> problems = new ArrayList<>();
        while (m.find()) {
            if (!flightRaces.contains(m.group(1))) {
                problems.add(m.group(1) + " has a WingType but no elytra_flight power — cosmetic wings on a flightless race?");
            }
        }
        assertTrue(problems.isEmpty(), String.join("\n", problems));
    }

    @Test
    void everyWingTextureReferencedByWingTypeExists() throws IOException {
        Matcher m = TEXTURE_LITERAL.matcher(Files.readString(WING_TYPE_SRC));
        Set<String> textures = new TreeSet<>();
        while (m.find()) {
            textures.add(m.group(1));
        }
        assertTrue(!textures.isEmpty(), "Expected texture literals in WingType.java");

        List<String> problems = new ArrayList<>();
        for (String texture : textures) {
            if (!Files.isRegularFile(ASSETS.resolve(texture))) {
                problems.add("Missing wing texture on disk: " + texture + " (run tools/generate_wings.py?)");
            }
        }
        assertTrue(problems.isEmpty(), String.join("\n", problems));
    }

    @Test
    void everyFlapResourceRaceIsInFlightConfig() throws IOException {
        Set<String> flapRaces = new TreeSet<>();
        for (Path json : listFiles(POWERS, ".json")) {
            Matcher m = FLAP_RESOURCE.matcher(Files.readString(json));
            while (m.find()) {
                flapRaces.add(m.group(1));
            }
        }
        assertTrue(!flapRaces.isEmpty(), "Expected flap cooldown resources in power JSONs");

        String flightConfigSource = Files.readString(FLIGHT_CONFIG_SRC);
        List<String> problems = new ArrayList<>();
        for (String race : flapRaces) {
            if (!flightConfigSource.contains("\"" + race + "\"")) {
                problems.add(race + " has a flap cooldown resource but no FlightConfig entry — flap key does nothing");
            }
        }
        assertTrue(problems.isEmpty(), String.join("\n", problems));
    }

    private static Set<String> racesWithPowerText(String needle) throws IOException {
        Set<String> races = new TreeSet<>();
        for (Path json : listFiles(POWERS, ".json")) {
            if (Files.readString(json).contains(needle)) {
                races.add(json.getParent().getFileName().toString());
            }
        }
        return races;
    }

    private static List<Path> listFiles(Path root, String suffix) throws IOException {
        try (Stream<Path> stream = Files.walk(root)) {
            return stream.filter(p -> p.toString().endsWith(suffix)).sorted().toList();
        }
    }
}
