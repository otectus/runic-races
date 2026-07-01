package com.otectus.runic_races;

import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import org.junit.jupiter.api.Test;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.TreeSet;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.stream.Stream;

import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * Guards the resource-id contract from CLAUDE.md: cooldown resources are
 * {@code runic_races:<race>/<powerFile>_cooldown_timer} and must match exactly between the
 * power JSONs that define them and any Java that reads them ({@code FlightConfig},
 * {@code AbilityIconRegistry}, {@code RacialEventHandler}). A typo on either side silently
 * breaks an ability at runtime, so this is enforced at CI time by cross-referencing string
 * literals in Java source against origins:resource subpower definitions in the datapack.
 */
class CooldownResourceIdTest {

    private static final Path JAVA_ROOT = Path.of("src/main/java");
    private static final Path POWERS = Path.of("src/main/resources/data/runic_races/powers");

    /** Matches literals like "sprite/gossamer_wings_flap_cooldown_timer" in Java source. */
    private static final Pattern JAVA_LITERAL = Pattern.compile("\"([a-z_]+/[a-z_]+_cooldown_timer)\"");

    /** Matches fully-qualified resource ids inside power JSONs (change_resource/resource conditions). */
    private static final Pattern JSON_REFERENCE =
            Pattern.compile("\"runic_races:([a-z_]+/[a-z_]+_cooldown_timer)\"");

    /**
     * Flap cooldowns duplicated between JSON (resource "max") and Java (FlightConfig enum
     * constants SPRITE/FAERIE/AVIAN/WIND_WYRM cooldownTicks). Update both places together.
     */
    private static final Map<String, Integer> FLAP_COOLDOWNS = Map.of(
            "sprite/gossamer_wings_flap_cooldown_timer", 30,
            "faerie/pixie_flight_flap_cooldown_timer", 30,
            "avian/skyborne_flap_cooldown_timer", 35,
            "wind_wyrm/skylord_flap_cooldown_timer", 50
    );

    @Test
    void everyJavaCooldownLiteralIsDefinedInExactlyOnePowerJson() throws IOException {
        Map<String, List<Path>> definitions = collectResourceDefinitions();

        Set<String> javaLiterals = new TreeSet<>();
        for (Path javaFile : listFiles(JAVA_ROOT, ".java")) {
            Matcher matcher = JAVA_LITERAL.matcher(Files.readString(javaFile));
            while (matcher.find()) {
                javaLiterals.add(matcher.group(1));
            }
        }
        assertTrue(!javaLiterals.isEmpty(), "Expected cooldown literals in Java source (FlightConfig etc.)");

        List<String> problems = new ArrayList<>();
        for (String literal : javaLiterals) {
            List<Path> defs = definitions.getOrDefault(literal, List.of());
            if (defs.size() != 1) {
                problems.add("Java literal \"" + literal + "\" defined in " + defs.size()
                        + " power JSONs (expected exactly 1): " + defs);
            }
        }
        assertTrue(problems.isEmpty(), "Java -> JSON cooldown id failures:\n" + String.join("\n", problems));
    }

    @Test
    void jsonResourceReferencesAreSelfConsistentWithinTheirFile() throws IOException {
        // Every runic_races:*_cooldown_timer referenced by a change_resource action or a
        // resource condition must be defined by an origins:resource subpower in the SAME
        // file (the repo never references another power's cooldown across files).
        List<String> problems = new ArrayList<>();
        for (Path powerFile : listFiles(POWERS, ".json")) {
            Set<String> definedHere = resourcesDefinedIn(powerFile);
            Matcher matcher = JSON_REFERENCE.matcher(Files.readString(powerFile));
            while (matcher.find()) {
                String ref = matcher.group(1);
                if (!definedHere.contains(ref)) {
                    problems.add(powerFile + " references runic_races:" + ref
                            + " but defines only " + definedHere);
                }
            }
        }
        assertTrue(problems.isEmpty(), "JSON resource self-consistency failures:\n" + String.join("\n", problems));
    }

    @Test
    void flapResourceMaxMatchesFlightConfigCooldown() throws IOException {
        List<String> problems = new ArrayList<>();
        for (Map.Entry<String, Integer> expected : FLAP_COOLDOWNS.entrySet()) {
            Integer max = resourceMax(expected.getKey());
            if (max == null) {
                problems.add(expected.getKey() + ": resource not found in any power JSON");
            } else if (!max.equals(expected.getValue())) {
                problems.add(expected.getKey() + ": JSON max=" + max
                        + " but FlightConfig cooldownTicks=" + expected.getValue());
            }
        }
        assertTrue(problems.isEmpty(), "Flap cooldown JSON/Java drift:\n" + String.join("\n", problems));
    }

    // === helpers ===

    /**
     * Maps each resource path ("&lt;race&gt;/&lt;powerFile&gt;_&lt;subpowerKey&gt;") to the power JSONs
     * defining it. Apoli derives subpower ids as {@code <powerId>_<subpowerKey>}, so a subpower
     * "cooldown_timer" of power {@code <race>/<file>} becomes {@code <race>/<file>_cooldown_timer}.
     */
    private static Map<String, List<Path>> collectResourceDefinitions() throws IOException {
        Map<String, List<Path>> definitions = new HashMap<>();
        for (Path powerFile : listFiles(POWERS, ".json")) {
            for (String resource : resourcesDefinedIn(powerFile)) {
                definitions.computeIfAbsent(resource, k -> new ArrayList<>()).add(powerFile);
            }
        }
        return definitions;
    }

    /** Resource paths defined by origins:resource subpowers of the given power file. */
    private static Set<String> resourcesDefinedIn(Path powerFile) throws IOException {
        String race = powerFile.getParent().getFileName().toString();
        String stem = powerFile.getFileName().toString().replace(".json", "");
        JsonObject root = JsonParser.parseString(Files.readString(powerFile)).getAsJsonObject();

        Set<String> defined = new HashSet<>();
        if (root.has("subpowers")) {
            for (JsonElement key : root.getAsJsonArray("subpowers")) {
                JsonElement sub = root.get(key.getAsString());
                if (sub != null && sub.isJsonObject()
                        && "origins:resource".equals(typeOf(sub.getAsJsonObject()))) {
                    defined.add(race + "/" + stem + "_" + key.getAsString());
                }
            }
        }
        return defined;
    }

    private static Integer resourceMax(String resourcePath) throws IOException {
        // resourcePath = "<race>/<file>_<subkey>"; locate the defining file and read its "max".
        for (Path powerFile : listFiles(POWERS, ".json")) {
            if (!resourcesDefinedIn(powerFile).contains(resourcePath)) {
                continue;
            }
            String stem = powerFile.getFileName().toString().replace(".json", "");
            String subKey = resourcePath.substring(resourcePath.indexOf('/') + 1 + stem.length() + 1);
            JsonObject root = JsonParser.parseString(Files.readString(powerFile)).getAsJsonObject();
            JsonObject sub = root.getAsJsonObject(subKey);
            return sub != null && sub.has("max") ? sub.get("max").getAsInt() : null;
        }
        return null;
    }

    private static String typeOf(JsonObject obj) {
        JsonElement type = obj.get("type");
        return type != null && type.isJsonPrimitive() ? type.getAsString() : null;
    }

    private static List<Path> listFiles(Path root, String suffix) throws IOException {
        try (Stream<Path> stream = Files.walk(root)) {
            return stream.filter(p -> p.getFileName().toString().endsWith(suffix)).sorted().toList();
        }
    }
}
