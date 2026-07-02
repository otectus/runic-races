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
 * Inverse of {@code RaceDataParityTest}: instead of "every race has its assets",
 * this asserts "every asset belongs to a race/registry" so renamed or removed
 * races can't leave orphaned files behind. Data is generator-authored
 * (tools/*.py) and drifts, so this runs as a test rather than a one-off audit.
 */
class StaleAssetTest {

    private static final Path ASSETS = Path.of("src/main/resources/assets/runic_races");
    private static final Path POWERS = Path.of("src/main/resources/data/runic_races/powers");
    private static final Path ICON_REGISTRY_SRC =
            Path.of("src/main/java/com/otectus/runic_races/client/AbilityIconRegistry.java");
    private static final Path WING_TYPE_SRC =
            Path.of("src/main/java/com/otectus/runic_races/client/render/WingType.java");

    private static final Pattern COOLDOWN_LITERAL = Pattern.compile("\"([a-z_]+/[a-z_]+)_cooldown_timer\"");

    /** Generator-source textures intentionally kept without a direct WingType reference. */
    private static final Set<String> ENTITY_TEXTURE_ALLOWLIST =
            Set.of("drake_wings.png", "pixie_wings_base.png", "wyvern_wings_base.png");

    @Test
    void everyItemTextureAndModelBelongsToARace() throws IOException {
        Set<String> races = RaceRegistry.allRaceNames();
        List<String> problems = new ArrayList<>();

        try (Stream<Path> stream = Files.list(ASSETS.resolve("textures/item"))) {
            for (Path png : stream.sorted().toList()) {
                String stem = png.getFileName().toString().replace(".png", "");
                if (!races.contains(stem)) {
                    problems.add("Stale race icon texture: " + png);
                }
            }
        }
        try (Stream<Path> stream = Files.list(ASSETS.resolve("models/item"))) {
            for (Path model : stream.sorted().toList()) {
                String stem = model.getFileName().toString().replace("_icon.json", "");
                if (!races.contains(stem)) {
                    problems.add("Stale race icon model: " + model);
                }
            }
        }
        assertTrue(problems.isEmpty(), String.join("\n", problems));
    }

    @Test
    void everyAbilityHudTextureIsReferencedByTheIconRegistry() throws IOException {
        Set<String> expected = new TreeSet<>();
        Matcher m = COOLDOWN_LITERAL.matcher(Files.readString(ICON_REGISTRY_SRC));
        while (m.find()) {
            expected.add("textures/gui/ability/" + m.group(1) + ".png");
        }

        List<String> problems = new ArrayList<>();
        try (Stream<Path> stream = Files.walk(ASSETS.resolve("textures/gui/ability"))) {
            for (Path png : stream.filter(p -> p.toString().endsWith(".png")).sorted().toList()) {
                String relative = ASSETS.relativize(png).toString().replace('\\', '/');
                if (!expected.contains(relative)) {
                    problems.add("Stale ability HUD texture (no AbilityIconRegistry reference): " + png);
                }
            }
        }
        assertTrue(problems.isEmpty(), String.join("\n", problems));
    }

    @Test
    void everyEntityTextureIsReferencedByAWingType() throws IOException {
        String wingTypeSource = Files.readString(WING_TYPE_SRC);
        List<String> problems = new ArrayList<>();
        try (Stream<Path> stream = Files.list(ASSETS.resolve("textures/entity"))) {
            for (Path png : stream.sorted().toList()) {
                String name = png.getFileName().toString();
                if (ENTITY_TEXTURE_ALLOWLIST.contains(name)) continue;
                if (!wingTypeSource.contains("textures/entity/" + name)) {
                    problems.add("Stale entity texture (no WingType reference): " + png);
                }
            }
        }
        assertTrue(problems.isEmpty(), String.join("\n", problems));
    }

    @Test
    void everyPowersDirectoryBelongsToARace() throws IOException {
        Set<String> races = RaceRegistry.allRaceNames();
        List<String> problems = new ArrayList<>();
        try (Stream<Path> stream = Files.list(POWERS)) {
            for (Path dir : stream.sorted().toList()) {
                if (!Files.isDirectory(dir)) continue;
                if (!races.contains(dir.getFileName().toString())) {
                    problems.add("Stale powers directory (unknown race): " + dir);
                }
            }
        }
        assertTrue(problems.isEmpty(), String.join("\n", problems));
    }
}
