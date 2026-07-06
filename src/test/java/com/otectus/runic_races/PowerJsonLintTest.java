package com.otectus.runic_races;

import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import org.junit.jupiter.api.Test;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;
import java.util.Set;
import java.util.function.BiConsumer;
import java.util.stream.Stream;

import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * Static lint over every power JSON in the datapack. Rules encode audit decisions from the
 * v1.1.0 hardening pass:
 * <ul>
 *   <li>{@code check_interval} must be &gt;= 1 (0 would tick every frame or never, per parser).</li>
 *   <li>Biome affinity tags must come from the vetted allowlist (typos fail silently in-game).</li>
 *   <li>Offensive AoE actives use {@code runic_races:afflict_hostiles}, not
 *       {@code origins:area_of_effect} with a living-only filter (which also hits allies/pets).</li>
 *   <li>Timed glow effects applied from an {@code action_over_time} must outlast the interval,
 *       or the effect strobes off between ticks.</li>
 * </ul>
 */
class PowerJsonLintTest {

    private static final Path POWERS = Path.of("src/main/resources/data/runic_races/powers");

    /** The only biome tags racial biome_affinity powers may target. */
    private static final Set<String> BIOME_TAG_ALLOWLIST = Set.of(
            "minecraft:is_forest",
            "minecraft:is_taiga",
            "forge:is_hot",
            "forge:is_cold",
            "forge:is_mountain",
            "forge:is_water"
    );

    /** Known origins:area_of_effect violations — keep empty; add entries only with a dated reason. */
    private static final Set<String> AREA_OF_EFFECT_KNOWN_VIOLATIONS = Set.of();

    @Test
    void lintAllPowerJsons() throws IOException {
        List<String> problems = new ArrayList<>();

        for (Path powerFile : listPowerFiles()) {
            String relative = POWERS.relativize(powerFile).toString();
            JsonElement root = JsonParser.parseString(Files.readString(powerFile));

            walk(root, (obj, path) -> {
                // Rule: check_interval >= 1
                if (obj.has("check_interval") && obj.get("check_interval").getAsInt() < 1) {
                    problems.add(relative + ": check_interval " + obj.get("check_interval") + " < 1");
                }

                // Rule: no explicit zero-valued bonus/penalty fields — the codec treats
                // them as optional and skips 0.0, so they are dead weight that reads as
                // a real effect to anyone skimming the JSON.
                for (String key : List.of("speed_bonus", "damage_bonus", "speed_penalty", "damage_penalty")) {
                    if (obj.has(key) && obj.get(key).getAsDouble() == 0.0) {
                        problems.add(relative + ": " + key + " is an explicit 0.0 — delete the line (no-op)");
                    }
                }

                // Rule: biome tags in allowlist
                for (String key : List.of("home_biome_tag", "hostile_biome_tag")) {
                    if (obj.has(key) && !BIOME_TAG_ALLOWLIST.contains(obj.get(key).getAsString())) {
                        problems.add(relative + ": " + key + " " + obj.get(key).getAsString()
                                + " not in allowlist " + BIOME_TAG_ALLOWLIST);
                    }
                }

                // Rule: no origins:area_of_effect (use runic_races:afflict_hostiles instead)
                if ("origins:area_of_effect".equals(typeOf(obj))
                        && !AREA_OF_EFFECT_KNOWN_VIOLATIONS.contains(relative)) {
                    problems.add(relative + ": uses origins:area_of_effect — offensive AoE actives"
                            + " must use runic_races:afflict_hostiles");
                }

                // Rule: timed effects applied from action_over_time must outlast the interval.
                if ("origins:action_over_time".equals(typeOf(obj)) && obj.has("entity_action")) {
                    int interval = obj.has("interval") ? obj.get("interval").getAsInt() : 20;
                    walk(obj.get("entity_action"), (inner, innerPath) -> {
                        if ("runic_races:glow_hostiles".equals(typeOf(inner)) && inner.has("duration_ticks")) {
                            int duration = inner.get("duration_ticks").getAsInt();
                            if (duration < interval) {
                                problems.add(relative + ": glow_hostiles duration_ticks " + duration
                                        + " < enclosing action_over_time interval " + interval
                                        + " (glow strobes off between ticks)");
                            }
                        }
                    }, path + ".entity_action");
                }
            }, "$");
        }

        assertTrue(problems.isEmpty(), "Power JSON lint failures:\n" + String.join("\n", problems));
    }

    // === helpers ===

    private static List<Path> listPowerFiles() throws IOException {
        try (Stream<Path> stream = Files.walk(POWERS)) {
            return stream.filter(p -> p.getFileName().toString().endsWith(".json")).sorted().toList();
        }
    }

    private static String typeOf(JsonObject obj) {
        JsonElement type = obj.get("type");
        return type != null && type.isJsonPrimitive() ? type.getAsString() : null;
    }

    /** Depth-first visit of every JsonObject with its JSON-path (for diagnostics). */
    private static void walk(JsonElement element, BiConsumer<JsonObject, String> visitor, String path) {
        if (element.isJsonObject()) {
            JsonObject obj = element.getAsJsonObject();
            visitor.accept(obj, path);
            for (var entry : obj.entrySet()) {
                walk(entry.getValue(), visitor, path + "." + entry.getKey());
            }
        } else if (element.isJsonArray()) {
            int i = 0;
            for (JsonElement child : element.getAsJsonArray()) {
                walk(child, visitor, path + "[" + i++ + "]");
            }
        }
    }
}
