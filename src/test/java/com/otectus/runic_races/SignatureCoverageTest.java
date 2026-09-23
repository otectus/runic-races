package com.otectus.runic_races;

import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import com.otectus.runic_races.race.RaceRegistry;
import org.junit.jupiter.api.Test;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.Set;
import java.util.TreeSet;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.stream.Stream;

import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * Guards the signature-presentation contract: every {@code SignatureKey} enum
 * constant must have a {@code SignatureRegistry} recipe (a key without a recipe
 * fires silently at runtime), and every {@code runic_races:signature_presentation}
 * key used in a power JSON must be a valid enum constant (a typo is a silent no-op).
 *
 * Source-scanned — SignatureRegistry's static init constructs Minecraft particle
 * options, which are compileOnly and not loadable under plain JUnit.
 */
class SignatureCoverageTest {

    private static final Path POWERS = Path.of("src/main/resources/data/runic_races/powers");
    private static final Path KEY_SRC =
            Path.of("src/main/java/com/otectus/runic_races/presentation/SignatureKey.java");
    private static final Path REGISTRY_SRC =
            Path.of("src/main/java/com/otectus/runic_races/presentation/SignatureRegistry.java");

    // The final constant may have no trailing comma/semicolon before the closing brace.
    private static final Pattern ENUM_CONSTANT = Pattern.compile("^\\s{4}([A-Z][A-Z_]*)[,;]?\\s*$", Pattern.MULTILINE);
    private static final Pattern REGISTRY_ENTRY = Pattern.compile("ENTRIES\\.put\\(SignatureKey\\.([A-Z_]+)");
    private static final Pattern JSON_KEY = Pattern.compile("\"key\"\\s*:\\s*\"([A-Z][A-Z_]*)\"");

    @Test
    void everySignatureKeyHasARegistryRecipe() throws IOException {
        Set<String> constants = enumConstants();
        Set<String> entries = new TreeSet<>();
        Matcher m = REGISTRY_ENTRY.matcher(Files.readString(REGISTRY_SRC));
        while (m.find()) {
            entries.add(m.group(1));
        }

        List<String> problems = new ArrayList<>();
        for (String constant : constants) {
            if (!entries.contains(constant)) {
                problems.add("SignatureKey." + constant + " has no SignatureRegistry entry — fires silently");
            }
        }
        for (String entry : entries) {
            if (!constants.contains(entry)) {
                problems.add("SignatureRegistry references nonexistent SignatureKey." + entry);
            }
        }
        assertTrue(problems.isEmpty(), String.join("\n", problems));
    }

    @Test
    void everyJsonSignaturePresentationKeyIsAValidSignatureKey() throws IOException {
        Set<String> constants = enumConstants();
        List<String> problems = new ArrayList<>();
        for (Path json : listFiles(POWERS, ".json")) {
            String text = Files.readString(json);
            if (!text.contains("\"runic_races:signature_presentation\"")) continue;
            Matcher m = JSON_KEY.matcher(text);
            boolean found = false;
            while (m.find()) {
                found = true;
                if (!constants.contains(m.group(1))) {
                    problems.add(json + " uses unknown signature key \"" + m.group(1) + "\"");
                }
            }
            if (!found) {
                problems.add(json + " has a signature_presentation action but no uppercase \"key\" value");
            }
        }
        assertTrue(problems.isEmpty(), String.join("\n", problems));
    }

    @Test
    void everyRacesActivatedPowerHasImmediateShapedAndStagedWorldFeedback() throws IOException {
        String registry = Files.readString(REGISTRY_SRC);
        List<String> problems = new ArrayList<>();
        for (String race : RaceRegistry.allRaceNames()) {
            JsonObject origin = JsonParser.parseString(Files.readString(
                    POWERS.resolve("../origins/" + race + ".json").normalize())).getAsJsonObject();
            String activeId = origin.getAsJsonArray("powers").get(0).getAsString();
            JsonObject power = JsonParser.parseString(Files.readString(
                    POWERS.resolve(activeId.substring("runic_races:".length()) + ".json"))).getAsJsonObject();
            JsonElement active = power.get("active_ability");
            Set<String> keys = new TreeSet<>();
            collectActiveSignatures(active, keys);
            if (keys.isEmpty()) {
                problems.add(race + ": activated power has no signature presentation route");
            }
            for (String key : keys) {
                int start = registry.indexOf("ENTRIES.put(SignatureKey." + key + ",");
                if (start < 0) {
                    problems.add(race + ": activated power has no recipe for " + key);
                    continue;
                }
                int end = registry.indexOf("ENTRIES.put(SignatureKey.", start + 1);
                String block = registry.substring(start, end < 0 ? registry.length() : end);
                List<String> particleLines = block.lines().filter(line -> line.contains("new VfxSpec(")).toList();
                if (particleLines.size() < 3) {
                    problems.add(race + ": active needs anticipation, a visible silhouette, and a settling layer");
                }
                if (particleLines.stream().noneMatch(line -> !line.contains(".delayed("))) {
                    problems.add(race + ": activation lacks immediate world particles");
                }
                long timedLayers = particleLines.stream().filter(line -> line.contains(".delayed(")).count();
                if (timedLayers < 2) {
                    problems.add(race + ": active has no sustained sequence of world particle layers");
                }
                if (particleLines.stream().noneMatch(line -> line.contains("SignatureEntry.Shape.")
                        && !line.contains("SignatureEntry.Shape.POINT"))) {
                    problems.add(race + ": active is only a random particle cloud");
                }
            }
        }
        assertTrue(problems.isEmpty(), String.join("\n", problems));
    }

    /** Both the legacy JSON action route and the expansion service's kind-to-key contract. */
    private static void collectActiveSignatures(JsonElement element, Set<String> keys) {
        if (element == null || element.isJsonNull()) return;
        if (element.isJsonArray()) {
            element.getAsJsonArray().forEach(child -> collectActiveSignatures(child, keys));
        } else if (element.isJsonObject()) {
            JsonObject object = element.getAsJsonObject();
            String type = object.has("type") ? object.get("type").getAsString() : "";
            if ("runic_races:signature_presentation".equals(type)) {
                keys.add(object.get("key").getAsString());
            } else if ("runic_races:racial_ability".equals(type)) {
                keys.add(object.get("kind").getAsString().toUpperCase(Locale.ROOT) + "_ACTIVE");
            }
            object.entrySet().forEach(entry -> collectActiveSignatures(entry.getValue(), keys));
        }
    }

    private static Set<String> enumConstants() throws IOException {
        Set<String> constants = new TreeSet<>();
        Matcher m = ENUM_CONSTANT.matcher(Files.readString(KEY_SRC));
        while (m.find()) {
            constants.add(m.group(1));
        }
        assertTrue(!constants.isEmpty(), "Expected enum constants in SignatureKey.java");
        return constants;
    }

    private static List<Path> listFiles(Path root, String suffix) throws IOException {
        try (Stream<Path> stream = Files.walk(root)) {
            return stream.filter(p -> p.toString().endsWith(suffix)).sorted().toList();
        }
    }
}
