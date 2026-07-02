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
import java.util.TreeSet;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.stream.Stream;

import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * Guards the custom particle/sound registries against asset drift: every
 * {@code ModParticles} id needs a particle definition JSON and sprite frames on
 * disk (missing ones crash the client at texture-stitch time), and every
 * {@code ModSounds} id needs a {@code sounds.json} entry (missing ones are
 * silent at runtime). Both directions are checked so stale assets surface too.
 */
class ParticleAssetTest {

    private static final Path ASSETS = Path.of("src/main/resources/assets/runic_races");
    private static final Path PARTICLES_SRC =
            Path.of("src/main/java/com/otectus/runic_races/registry/ModParticles.java");
    private static final Path SOUNDS_SRC =
            Path.of("src/main/java/com/otectus/runic_races/registry/ModSounds.java");

    private static final Pattern REGISTER_ID = Pattern.compile("register\\(\"([a-z._]+)\"\\)");

    @Test
    void everyParticleTypeHasDefinitionJsonAndSpriteFrames() throws IOException {
        Set<String> ids = registeredIds(PARTICLES_SRC);
        assertTrue(!ids.isEmpty(), "Expected particle registrations in ModParticles.java");

        List<String> problems = new ArrayList<>();
        for (String id : ids) {
            Path definition = ASSETS.resolve("particles/" + id + ".json");
            if (!Files.isRegularFile(definition)) {
                problems.add("Missing particle definition: " + definition);
                continue;
            }
            JsonObject json = JsonParser.parseString(Files.readString(definition)).getAsJsonObject();
            for (JsonElement texture : json.getAsJsonArray("textures")) {
                String name = texture.getAsString().replace("runic_races:", "");
                Path sprite = ASSETS.resolve("textures/particle/" + name + ".png");
                if (!Files.isRegularFile(sprite)) {
                    problems.add("Missing particle sprite: " + sprite + " (run tools/generate_particles.py?)");
                }
            }
        }
        assertTrue(problems.isEmpty(), String.join("\n", problems));
    }

    @Test
    void everyParticleDefinitionBelongsToARegisteredType() throws IOException {
        Set<String> ids = registeredIds(PARTICLES_SRC);
        List<String> problems = new ArrayList<>();
        try (Stream<Path> stream = Files.list(ASSETS.resolve("particles"))) {
            for (Path definition : stream.sorted().toList()) {
                String stem = definition.getFileName().toString().replace(".json", "");
                if (!ids.contains(stem)) {
                    problems.add("Stale particle definition (no ModParticles registration): " + definition);
                }
            }
        }
        assertTrue(problems.isEmpty(), String.join("\n", problems));
    }

    @Test
    void soundRegistrationsAndSoundsJsonMatchExactly() throws IOException {
        Set<String> ids = registeredIds(SOUNDS_SRC);
        assertTrue(!ids.isEmpty(), "Expected sound registrations in ModSounds.java");

        JsonObject soundsJson = JsonParser.parseString(
                Files.readString(ASSETS.resolve("sounds.json"))).getAsJsonObject();

        List<String> problems = new ArrayList<>();
        for (String id : ids) {
            if (!soundsJson.has(id)) {
                problems.add("ModSounds." + id + " has no sounds.json entry — silent at runtime");
            }
        }
        for (String key : soundsJson.keySet()) {
            if (!ids.contains(key)) {
                problems.add("Stale sounds.json entry (no ModSounds registration): " + key);
            }
        }
        assertTrue(problems.isEmpty(), String.join("\n", problems));
    }

    @Test
    void everySoundSubtitleKeyExistsInLang() throws IOException {
        JsonObject soundsJson = JsonParser.parseString(
                Files.readString(ASSETS.resolve("sounds.json"))).getAsJsonObject();
        JsonObject lang = JsonParser.parseString(
                Files.readString(ASSETS.resolve("lang/en_us.json"))).getAsJsonObject();

        List<String> problems = new ArrayList<>();
        for (String key : soundsJson.keySet()) {
            JsonObject entry = soundsJson.getAsJsonObject(key);
            if (entry.has("subtitle") && !lang.has(entry.get("subtitle").getAsString())) {
                problems.add("sounds.json subtitle key missing from en_us.json: " + entry.get("subtitle").getAsString());
            }
        }
        assertTrue(problems.isEmpty(), String.join("\n", problems));
    }

    private static Set<String> registeredIds(Path source) throws IOException {
        Set<String> ids = new TreeSet<>();
        Matcher m = REGISTER_ID.matcher(Files.readString(source));
        while (m.find()) {
            ids.add(m.group(1));
        }
        return ids;
    }
}
