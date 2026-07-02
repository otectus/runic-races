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
