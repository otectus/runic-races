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
import java.util.function.Consumer;
import java.util.stream.Stream;

import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * Enforces the VFX density guideline from CLAUDE.md for signature actives (Major tier:
 * 30-60 particles). For every power file with an {@code origins:active_self} subpower, the
 * summed {@code origins:spawn_particles} "count" inside that active must be either 0
 * (presentation fully delegated to Java, e.g. signature_presentation / breath actions) or
 * within the 30-60 Major band.
 */
class VfxBudgetTest {

    private static final Path POWERS = Path.of("src/main/resources/data/runic_races/powers");

    private static final int MAJOR_MIN = 30;
    private static final int MAJOR_MAX = 60;

    /** Known off-budget actives — keep empty; add entries only with a dated reason. */
    private static final Set<String> KNOWN_EXCEPTIONS = Set.of();

    @Test
    void activeAbilityParticleBudgetsAreOnGrammar() throws IOException {
        List<String> problems = new ArrayList<>();

        for (Path powerFile : listPowerFiles()) {
            String relative = POWERS.relativize(powerFile).toString();
            JsonObject root = JsonParser.parseString(Files.readString(powerFile)).getAsJsonObject();
            if (!root.has("subpowers")) {
                continue;
            }

            for (JsonElement key : root.getAsJsonArray("subpowers")) {
                JsonElement sub = root.get(key.getAsString());
                if (sub == null || !sub.isJsonObject()
                        || !"origins:active_self".equals(typeOf(sub.getAsJsonObject()))) {
                    continue;
                }

                int[] total = {0};
                walk(sub, obj -> {
                    if ("origins:spawn_particles".equals(typeOf(obj)) && obj.has("count")) {
                        total[0] += obj.get("count").getAsInt();
                    }
                });

                int sum = total[0];
                boolean ok = sum == 0 || (sum >= MAJOR_MIN && sum <= MAJOR_MAX);
                if (!ok && !KNOWN_EXCEPTIONS.contains(relative)) {
                    problems.add(relative + " (" + key.getAsString() + "): spawn_particles sum " + sum
                            + " — expected 0 (Java-delegated) or " + MAJOR_MIN + "-" + MAJOR_MAX);
                }
            }
        }

        assertTrue(problems.isEmpty(), "VFX budget violations:\n" + String.join("\n", problems));
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

    private static void walk(JsonElement element, Consumer<JsonObject> visitor) {
        if (element.isJsonObject()) {
            JsonObject obj = element.getAsJsonObject();
            visitor.accept(obj);
            for (var entry : obj.entrySet()) {
                walk(entry.getValue(), visitor);
            }
        } else if (element.isJsonArray()) {
            for (JsonElement child : element.getAsJsonArray()) {
                walk(child, visitor);
            }
        }
    }
}
