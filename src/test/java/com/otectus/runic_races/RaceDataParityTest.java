package com.otectus.runic_races;

import com.google.gson.JsonArray;
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
import java.util.Set;
import java.util.stream.Stream;

import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * Structural parity between {@link RaceRegistry} (pure Java, the single source of truth for
 * the 37 races) and the on-disk datapack/resourcepack: every registered race must have its
 * origin JSON, exactly 3 power JSONs, resolvable origin power references, an icon texture,
 * and an icon item model. Files are parsed as plain JSON via Gson — no Minecraft bootstrap.
 */
class RaceDataParityTest {

    private static final Path DATA = Path.of("src/main/resources/data/runic_races");
    private static final Path ASSETS = Path.of("src/main/resources/assets/runic_races");

    /** Only vetted summon entities may be referenced by runic_races:summon_minion. */
    private static final Set<String> SUMMON_ENTITY_ALLOWLIST = Set.of("runic_races:grave_servant");

    @Test
    void everyRaceHasOriginThreePowersIconAndModel() throws IOException {
        List<String> problems = new ArrayList<>();

        for (String race : RaceRegistry.allRaceNames()) {
            Path originFile = DATA.resolve("origins/" + race + ".json");
            if (!Files.isRegularFile(originFile)) {
                problems.add(race + ": missing origin file " + originFile);
                continue;
            }

            // Exactly 3 power JSONs on disk.
            Path powerDir = DATA.resolve("powers/" + race);
            List<Path> powerFiles = listJsonFiles(powerDir);
            if (powerFiles.size() != 3) {
                problems.add(race + ": expected 3 power files in " + powerDir + ", found " + powerFiles.size());
            }

            // The origin's "powers" array must resolve to existing power files.
            JsonObject origin = JsonParser.parseString(Files.readString(originFile)).getAsJsonObject();
            JsonArray powers = origin.getAsJsonArray("powers");
            if (powers == null) {
                problems.add(race + ": origin has no \"powers\" array");
            } else {
                for (JsonElement entry : powers) {
                    String id = entry.getAsString(); // e.g. "runic_races:arachnid/web_snare"
                    if (!id.startsWith("runic_races:")) {
                        problems.add(race + ": origin power " + id + " is not in the runic_races namespace");
                        continue;
                    }
                    Path powerFile = DATA.resolve("powers/" + id.substring("runic_races:".length()) + ".json");
                    if (!Files.isRegularFile(powerFile)) {
                        problems.add(race + ": origin references missing power file " + powerFile);
                    }
                }
            }

            if (!Files.isRegularFile(ASSETS.resolve("textures/item/" + race + ".png"))) {
                problems.add(race + ": missing icon texture textures/item/" + race + ".png");
            }
            if (!Files.isRegularFile(ASSETS.resolve("models/item/" + race + "_icon.json"))) {
                problems.add(race + ": missing icon model models/item/" + race + "_icon.json");
            }
        }

        assertTrue(problems.isEmpty(), "Race data parity failures:\n" + String.join("\n", problems));
    }

    @Test
    void summonMinionEntitiesAreAllowlisted() throws IOException {
        List<String> problems = new ArrayList<>();
        for (String race : RaceRegistry.allRaceNames()) {
            for (Path powerFile : listJsonFiles(DATA.resolve("powers/" + race))) {
                JsonElement root = JsonParser.parseString(Files.readString(powerFile));
                walk(root, obj -> {
                    if ("runic_races:summon_minion".equals(typeOf(obj))) {
                        String entity = obj.has("entity") ? obj.get("entity").getAsString() : null;
                        if (entity == null || !SUMMON_ENTITY_ALLOWLIST.contains(entity)) {
                            problems.add(powerFile + ": summon_minion entity " + entity
                                    + " not in allowlist " + SUMMON_ENTITY_ALLOWLIST);
                        }
                    }
                });
            }
        }
        assertTrue(problems.isEmpty(), "summon_minion allowlist failures:\n" + String.join("\n", problems));
    }

    // === helpers ===

    private static List<Path> listJsonFiles(Path dir) throws IOException {
        if (!Files.isDirectory(dir)) {
            return List.of();
        }
        try (Stream<Path> stream = Files.list(dir)) {
            return stream.filter(p -> p.getFileName().toString().endsWith(".json")).sorted().toList();
        }
    }

    private static String typeOf(JsonObject obj) {
        JsonElement type = obj.get("type");
        return type != null && type.isJsonPrimitive() ? type.getAsString() : null;
    }

    /** Depth-first visit of every JsonObject in the tree. */
    private static void walk(JsonElement element, java.util.function.Consumer<JsonObject> visitor) {
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
