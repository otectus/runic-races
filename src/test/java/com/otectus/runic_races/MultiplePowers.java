package com.otectus.runic_races;

import com.google.gson.JsonElement;
import com.google.gson.JsonObject;

import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Set;

/**
 * The subpowers of an {@code origins:multiple} power, resolved the way Apoli itself resolves them.
 *
 * <p>Apoli's {@code MultiplePower} decodes <em>every</em> key of the power object except a fixed
 * reserved set into a nested {@code ConfiguredPower}. There is no index key, and adding one is not
 * harmless: the value is handed to the subpower codec, fails it, and the surrounding power then
 * loads only partially — logged once per file per world load as
 * {@code Power "..." will only be partially loaded: Failed to read fields: Not a JSON object: [...]}.
 * Runic Races shipped exactly such an index (a {@code "subpowers"} string array) in all 111 power
 * files up to 1.6.1; {@link PowerJsonLintTest} now fails the build if one reappears.
 *
 * <p>Reserved keys are mirrored from {@code MultiplePower.EXCLUDED}. Keeping the list here, rather
 * than reaching for a {@code subpowers} array that Apoli never reads, means the tests see the same
 * set of subpowers the game does.
 */
final class MultiplePowers {

    /**
     * Keys {@code origins:multiple} does not treat as a subpower.
     *
     * <p>The first nine mirror {@code MultiplePower.EXCLUDED}. {@code badges} is not in that set:
     * it is exempt through the predicate's second clause, {@code ApoliAPI.isAdditionalDataField},
     * because Origins registers it at init with {@code ApoliAPI.addAdditionalDataField("badges")}.
     * Any mod may register more the same way, which is why {@link #isReserved} also honours the
     * predicate's third clause — a leading {@code $} — rather than treating this list as complete.
     */
    static final Set<String> RESERVED_KEYS = Set.of(
            "type",
            "loading_priority",
            "name",
            "description",
            "hidden",
            "condition",
            "conditions",
            "fabric:load_conditions",
            "forge:conditions",
            "badges");

    /** The full {@code MultiplePower.ALLOWED} rule, negated: true when {@code key} is not a subpower. */
    static boolean isReserved(String key) {
        return RESERVED_KEYS.contains(key) || key.startsWith("$");
    }

    private MultiplePowers() {
    }

    /**
     * @return the subpowers of {@code power}, keyed by the name Apoli gives them, in declaration
     *         order. Empty for a power that is not {@code origins:multiple}, and for one whose
     *         non-reserved keys are all scalars.
     */
    static Map<String, JsonObject> of(JsonObject power) {
        Map<String, JsonObject> subpowers = new LinkedHashMap<>();
        for (Map.Entry<String, JsonElement> entry : power.entrySet()) {
            if (isReserved(entry.getKey())) {
                continue;
            }
            if (entry.getValue().isJsonObject()) {
                subpowers.put(entry.getKey(), entry.getValue().getAsJsonObject());
            }
        }
        return subpowers;
    }
}
