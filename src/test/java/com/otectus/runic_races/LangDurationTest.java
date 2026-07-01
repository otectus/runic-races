package com.otectus.runic_races;

import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import org.junit.jupiter.api.Test;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.function.Consumer;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * Soft consistency between the numbers players read in en_us.json power descriptions and
 * the actual tick values in the power JSONs:
 * <ul>
 *   <li>"... for Ns" describes an effect duration and must match SOME
 *       {@code origins:apply_effect} duration of N*20 ticks in the power file.</li>
 *   <li>"N-second cooldown" describes the ability cooldown and must match SOME
 *       {@code origins:resource} "max" of N*20 ticks in the power file (in this repo's copy,
 *       "N-second" is only ever used for cooldowns, so comparing it against apply_effect
 *       durations would be semantically wrong).</li>
 * </ul>
 * Descriptions with no numeric claims, and claims with nothing on the JSON side to compare
 * against (e.g. Java-implemented abilities with no apply_effect), are collected as skips and
 * printed, not failed. The test only fails when a number exists on both sides and disagrees.
 */
class LangDurationTest {

    private static final Path LANG = Path.of("src/main/resources/assets/runic_races/lang/en_us.json");
    private static final Path POWERS = Path.of("src/main/resources/data/runic_races/powers");

    private static final Pattern DESCRIPTION_KEY =
            Pattern.compile("power\\.runic_races\\.([a-z_]+)\\.([a-z_]+)\\.description");
    /** Effect-duration claims: "for 10s". */
    private static final Pattern EFFECT_SECONDS = Pattern.compile("for (\\d+)s\\b");
    /** Cooldown claims: "35-second cooldown". */
    private static final Pattern COOLDOWN_SECONDS = Pattern.compile("(\\d+)-second cooldown");

    /**
     * Tolerance whitelist: description keys whose numeric claims are intentionally allowed to
     * disagree with the JSON (e.g. flavor rounding). Deliberately empty — add entries only
     * with a justifying comment.
     */
    private static final Set<String> TOLERANCE_WHITELIST = Set.of();

    @Test
    void descriptionsMatchPowerJsonTickValues() throws IOException {
        JsonObject lang = JsonParser.parseString(Files.readString(LANG)).getAsJsonObject();

        List<String> mismatches = new ArrayList<>();
        List<String> skips = new ArrayList<>();
        int checked = 0;

        for (var entry : lang.entrySet()) {
            Matcher keyMatcher = DESCRIPTION_KEY.matcher(entry.getKey());
            if (!keyMatcher.matches() || TOLERANCE_WHITELIST.contains(entry.getKey())) {
                continue;
            }
            String race = keyMatcher.group(1);
            String power = keyMatcher.group(2);
            String description = entry.getValue().getAsString();

            List<Integer> effectSeconds = findAll(EFFECT_SECONDS, description);
            List<Integer> cooldownSeconds = findAll(COOLDOWN_SECONDS, description);
            if (effectSeconds.isEmpty() && cooldownSeconds.isEmpty()) {
                continue; // no numeric claim to verify
            }

            Path powerFile = POWERS.resolve(race + "/" + power + ".json");
            if (!Files.isRegularFile(powerFile)) {
                skips.add(entry.getKey() + ": no power file " + powerFile);
                continue;
            }

            JsonElement root = JsonParser.parseString(Files.readString(powerFile));
            Set<Integer> effectDurations = new HashSet<>();
            Set<Integer> resourceMaxes = new HashSet<>();
            walk(root, obj -> {
                if ("origins:apply_effect".equals(typeOf(obj))) {
                    JsonElement effect = obj.get("effect");
                    if (effect != null && effect.isJsonObject() && effect.getAsJsonObject().has("duration")) {
                        effectDurations.add(effect.getAsJsonObject().get("duration").getAsInt());
                    } else if (effect != null && effect.isJsonArray()) {
                        for (JsonElement e : effect.getAsJsonArray()) {
                            if (e.isJsonObject() && e.getAsJsonObject().has("duration")) {
                                effectDurations.add(e.getAsJsonObject().get("duration").getAsInt());
                            }
                        }
                    }
                }
                if ("origins:resource".equals(typeOf(obj)) && obj.has("max")) {
                    resourceMaxes.add(obj.get("max").getAsInt());
                }
            });

            for (int seconds : effectSeconds) {
                if (effectDurations.isEmpty()) {
                    skips.add(entry.getKey() + ": claims \"for " + seconds + "s\" but power has no apply_effect");
                } else if (effectDurations.contains(seconds * 20)) {
                    checked++;
                } else {
                    mismatches.add(entry.getKey() + ": claims \"for " + seconds + "s\" ("
                            + seconds * 20 + "t) but apply_effect durations are " + effectDurations);
                }
            }
            for (int seconds : cooldownSeconds) {
                if (resourceMaxes.isEmpty()) {
                    skips.add(entry.getKey() + ": claims \"" + seconds + "-second cooldown\" but power has no resource");
                } else if (resourceMaxes.contains(seconds * 20)) {
                    checked++;
                } else {
                    mismatches.add(entry.getKey() + ": claims \"" + seconds + "-second cooldown\" ("
                            + seconds * 20 + "t) but resource maxes are " + resourceMaxes);
                }
            }
        }

        if (!skips.isEmpty()) {
            System.out.println("[LangDurationTest] skipped (nothing to compare against):");
            skips.forEach(s -> System.out.println("  " + s));
        }
        System.out.println("[LangDurationTest] verified " + checked + " numeric claims, "
                + skips.size() + " skipped.");

        assertTrue(checked > 0, "Expected at least one verifiable duration/cooldown claim in en_us.json");
        assertTrue(mismatches.isEmpty(), "Lang/JSON duration mismatches:\n" + String.join("\n", mismatches));
    }

    // === helpers ===

    private static List<Integer> findAll(Pattern pattern, String text) {
        List<Integer> values = new ArrayList<>();
        Matcher matcher = pattern.matcher(text);
        while (matcher.find()) {
            values.add(Integer.parseInt(matcher.group(1)));
        }
        return values;
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
