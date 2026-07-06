package com.otectus.runic_races;

import org.junit.jupiter.api.Test;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.stream.Stream;

import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * Guards the graceful-degradation contract for integration-backed resource costs:
 * under the default fail-closed policy, a {@code has_mana}/{@code has_stamina}
 * check reads 0 when the backing mod is absent — so a power that gates its
 * *activation condition* on one is permanently dead on standalone installs
 * (exactly how Magi's Arcane Overflow shipped broken). Resource gates must live
 * in the action tree, wrapped so {@code resource_available} can bypass them when
 * the backing mod is missing.
 */
class ResourceGateLintTest {

    private static final Path POWERS = Path.of("src/main/resources/data/runic_races/powers");

    /**
     * Grabs each "condition" block up to its "entity_action"/"if_action" sibling.
     * A gate inside such a span is only legal when the same span carries the
     * resource_available bypass (the shipped or/not pattern).
     */
    private static final Pattern GATE_IN_CONDITION =
            Pattern.compile("\"condition\"\\s*:((?:(?!\"entity_action\"|\"if_action\").)*?runic_races:(has_mana|has_stamina))",
                    Pattern.DOTALL);

    @Test
    void resourceGatesNeverBlockActivationConditions() throws IOException {
        List<String> problems = new ArrayList<>();
        try (Stream<Path> files = Files.walk(POWERS)) {
            for (Path file : files.filter(f -> f.toString().endsWith(".json")).sorted().toList()) {
                String json = Files.readString(file);
                Matcher m = GATE_IN_CONDITION.matcher(json);
                while (m.find()) {
                    if (m.group(1).contains("resource_available")) continue; // guarded — the sanctioned pattern
                    problems.add(POWERS.relativize(file) + ": " + m.group(2)
                            + " inside a condition block without a resource_available bypass — the"
                            + " ability goes dead standalone under fail-closed gating. Wrap it:"
                            + " origins:or [ origins:not(resource_available), has_mana ].");
                }
                if (json.contains("runic_races:has_mana") && !json.contains("runic_races:resource_available")) {
                    problems.add(POWERS.relativize(file)
                            + ": uses has_mana without a resource_available guard anywhere in the file.");
                }
                if (json.contains("runic_races:has_stamina") && !json.contains("runic_races:resource_available")) {
                    problems.add(POWERS.relativize(file)
                            + ": uses has_stamina without a resource_available guard anywhere in the file.");
                }
            }
        }
        assertTrue(problems.isEmpty(), String.join("\n", problems));
    }
}
