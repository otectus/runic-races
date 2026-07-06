package com.otectus.runic_races.presentation;

import com.otectus.runic_races.common.state.RaceStateFlags;

import java.util.EnumMap;
import java.util.HashMap;
import java.util.Map;

/**
 * Maps a race-state flag's 0→1 onset to the sensory cue that plays when the
 * weakness starts biting (fired from {@code RaceStateTracker.setFlag}). The
 * matching {@link SignatureEntry} recipes are bannerless — the notification
 * system owns the words; this registry owns the sound and the spark.
 *
 * Race-specific overrides beat the flag default; an override may map to
 * {@code null} to explicitly silence a race (e.g. the zombie keeps its bespoke
 * escalating sunlight-burn sequence). Minecraft-free so the coverage test can
 * cross-check every reference under plain JUnit.
 */
public final class WeaknessCueRegistry {

    private static final Map<RaceStateFlags, SignatureKey> DEFAULTS = new EnumMap<>(RaceStateFlags.class);
    /** race → flag → key (may hold an explicit null to silence the default). */
    private static final Map<String, Map<RaceStateFlags, SignatureKey>> RACE_OVERRIDES = new HashMap<>();

    static {
        // Grave-touched flesh sears the moment the sun finds it.
        DEFAULTS.put(RaceStateFlags.SUNLIGHT_BURNING, SignatureKey.WEAKNESS_SUNLIGHT_SEAR);
        // Deep One and Skeleton are dazzled by daylight, not burned — glare, no sizzle.
        override("deep_one", RaceStateFlags.SUNLIGHT_BURNING, SignatureKey.WEAKNESS_SUN_DAZZLE);
        override("skeleton", RaceStateFlags.SUNLIGHT_BURNING, SignatureKey.WEAKNESS_SUN_DAZZLE);
        // Zombie keeps its bespoke escalating burn sequence in RacialEventHandler.
        override("zombie", RaceStateFlags.SUNLIGHT_BURNING, null);

        // Catching fire: only races whose fire weakness is amplified get a warning
        // beyond vanilla's own burning feedback. Dryad's triple damage earns the
        // loudest one; the frost-kin get a melting-rime hiss.
        override("dryad", RaceStateFlags.FIRE_VULNERABLE, SignatureKey.WEAKNESS_KINDLING);
        override("ice_drake", RaceStateFlags.FIRE_VULNERABLE, SignatureKey.WEAKNESS_THAW);
        override("frost_one", RaceStateFlags.FIRE_VULNERABLE, SignatureKey.WEAKNESS_THAW);

        // Going under: the cat protests, the storm scales short out. Iron One and
        // Sky One merely sink — the rune and notification carry that.
        override("feline", RaceStateFlags.SUBMERGED_WEAK, SignatureKey.WEAKNESS_HYDROPHOBIA);
        override("volt_drake", RaceStateFlags.SUBMERGED_WEAK, SignatureKey.WEAKNESS_SHORT_CIRCUIT);

        // Drying out (Sea Serpen landbound coils, Nymph bound-to-water).
        DEFAULTS.put(RaceStateFlags.DRY_SLUGGISH, SignatureKey.WEAKNESS_DRY);

        // Cold iron burns fae hands.
        override("faerie", RaceStateFlags.COLD_IRON_GRIP, SignatureKey.WEAKNESS_COLD_IRON);
    }

    private static void override(String race, RaceStateFlags flag, SignatureKey key) {
        RACE_OVERRIDES.computeIfAbsent(race, r -> new EnumMap<>(RaceStateFlags.class)).put(flag, key);
    }

    /** The cue for this race + flag onset, or null for silent (rune/notification only). */
    public static SignatureKey onsetCue(String race, RaceStateFlags flag) {
        Map<RaceStateFlags, SignatureKey> byRace = RACE_OVERRIDES.get(race);
        if (byRace != null && byRace.containsKey(flag)) {
            return byRace.get(flag);
        }
        return DEFAULTS.get(flag);
    }

    private WeaknessCueRegistry() {}
}
