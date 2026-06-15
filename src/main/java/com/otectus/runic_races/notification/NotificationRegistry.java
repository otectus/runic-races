package com.otectus.runic_races.notification;

import com.otectus.runic_races.common.state.RaceStateFlags;

import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;

/**
 * Single source of truth mapping a {@code (race, flag)} pair to the player-facing
 * notification copy for that harmful state's start/stop transitions.
 *
 * Pure data + lookup logic — imports only the Minecraft-free {@link RaceStateFlags}
 * enum so it can be exercised directly by JUnit (Minecraft/Origins are {@code compileOnly}).
 *
 * A {@code null} result means "no banner for this pair" — e.g. the churny informational
 * flags ({@code BIOME_HOME}, {@code BIOME_HOSTILE}, {@code NIGHT_EMPOWERED},
 * {@code ADAPTATION_ACTIVE}) are intentionally absent so they stay rune-only; learning
 * mode for those is handled generically in {@link RaceNotificationService}.
 */
public final class NotificationRegistry {

    private static final Map<String, NotificationSpec> SPECS = new HashMap<>();

    private NotificationRegistry() {}

    private static String key(String race, RaceStateFlags flag) {
        return race + "|" + flag.name();
    }

    private static void register(String race, RaceStateFlags flag, String topic, String color) {
        SPECS.put(key(race, flag), new NotificationSpec(
                "message.runic_races." + topic + ".start",
                "message.runic_races." + topic + ".stop",
                color));
    }

    static {
        // ----- Sunlight burn/decay (shared SUNLIGHT_BURNING bit, per-race copy) -----
        register("zombie",   RaceStateFlags.SUNLIGHT_BURNING, "zombie.sunlight",   "dark_green");
        register("skeleton", RaceStateFlags.SUNLIGHT_BURNING, "skeleton.sunlight", "white");
        register("wraith",   RaceStateFlags.SUNLIGHT_BURNING, "wraith.sunlight",   "dark_aqua");
        register("reaper",   RaceStateFlags.SUNLIGHT_BURNING, "reaper.sunlight",   "dark_purple");
        register("deep_one", RaceStateFlags.SUNLIGHT_BURNING, "deep_one.sunlight", "gold");
        register("dark_elf", RaceStateFlags.SUNLIGHT_BURNING, "dark_elf.sunlight", "dark_purple");

        // ----- Tight space / claustrophobia (shared TIGHT_SPACE bit) -----
        register("sky_one",   RaceStateFlags.TIGHT_SPACE, "sky_one.tight_space",   "gray");
        register("wind_wyrm", RaceStateFlags.TIGHT_SPACE, "wind_wyrm.tight_space", "white");

        // ----- Fire vulnerability (shared FIRE_VULNERABLE bit) -----
        register("dryad",      RaceStateFlags.FIRE_VULNERABLE, "dryad.fire",      "red");
        register("arachnid",   RaceStateFlags.FIRE_VULNERABLE, "arachnid.fire",   "red");
        register("nymph",      RaceStateFlags.FIRE_VULNERABLE, "nymph.fire",      "aqua");
        register("ice_drake",  RaceStateFlags.FIRE_VULNERABLE, "ice_drake.fire",  "aqua");
        register("frost_one",  RaceStateFlags.FIRE_VULNERABLE, "frost_one.fire",  "aqua");
        register("sea_serpen", RaceStateFlags.FIRE_VULNERABLE, "sea_serpen.fire", "blue");

        // ----- Open-sky weakness (OPEN_SKY bit) -----
        register("volt_drake", RaceStateFlags.OPEN_SKY, "volt_drake.open_sky", "yellow");
    }

    /**
     * @return the notification copy for this race entering/leaving the given flag,
     *         or {@code null} if this pair should not banner.
     */
    public static NotificationSpec resolve(String race, RaceStateFlags flag) {
        if (race == null || flag == null) return null;
        return SPECS.get(key(race, flag));
    }

    /** Every non-empty translation key referenced by the registry (for i18n coverage tests). */
    public static Set<String> allReferencedKeys() {
        Set<String> keys = new HashSet<>();
        for (NotificationSpec spec : SPECS.values()) {
            if (spec.startKey() != null && !spec.startKey().isEmpty()) keys.add(spec.startKey());
            if (spec.stopKey() != null && !spec.stopKey().isEmpty()) keys.add(spec.stopKey());
        }
        return keys;
    }
}
