package com.otectus.runic_races;

import com.otectus.runic_races.notification.NotificationRegistry;
import org.junit.jupiter.api.Test;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * Highest-value regression guard: asserts that every translation key referenced by code
 * actually exists in en_us.json. Catches typos and missing entries at CI time without any
 * Minecraft runtime — the lang file is read as plain text and each key checked as a JSON key.
 */
class I18nKeyPresenceTest {

    private static final Path LANG = Path.of("src/main/resources/assets/runic_races/lang/en_us.json");

    /** Keys referenced from Java/data outside the notification registry (hand-maintained contract). */
    private static final List<String> STATIC_KEYS = List.of(
            // SignatureRegistry banner keys
            "message.runic_races.signature.reaper.revival",
            "message.runic_races.signature.reaper.revival_rejected",
            "message.runic_races.signature.wraith.phase",
            "message.runic_races.signature.demon.wrath",
            "message.runic_races.signature.feline.nine_lives",
            "message.runic_races.signature.forge_one.forge_blessing",
            "message.runic_races.signature.runic_one.runic_ward",
            "message.runic_races.signature.faerie.glamour",
            "message.runic_races.signature.fire_drake.breath",
            "message.runic_races.signature.ice_drake.breath",
            "message.runic_races.signature.sea_serpen.breath",
            "message.runic_races.signature.terra_drake.breath",
            "message.runic_races.signature.volt_drake.breath",
            "message.runic_races.signature.wind_wyrm.breath",
            "message.runic_races.signature.sprite.wing_flap",
            "message.runic_races.signature.faerie.wing_flap",
            "message.runic_races.signature.avian.wing_flap",
            "message.runic_races.signature.wind_wyrm.wing_flap",
            "message.runic_races.signature.flight.cancel",
            // Learning-mode generic copy (RaceNotificationService)
            "message.runic_races.learning.biome_home.start",
            "message.runic_races.learning.biome_home.stop",
            "message.runic_races.learning.biome_hostile.start",
            "message.runic_races.learning.biome_hostile.stop",
            "message.runic_races.learning.night_empowered.start",
            "message.runic_races.learning.night_empowered.stop",
            "message.runic_races.learning.adaptation_active.start",
            "message.runic_races.learning.adaptation_active.stop",
            // State-rune tooltip keys (StateRuneOverlay)
            "tooltip.runic_races.state.home_biome",
            "tooltip.runic_races.state.hostile_biome",
            "tooltip.runic_races.state.night_empowered",
            "tooltip.runic_races.state.tight_space",
            "tooltip.runic_races.state.sunlight_burn",
            "tooltip.runic_races.state.fire_vulnerable",
            "tooltip.runic_races.state.regrowth_suppressed",
            "tooltip.runic_races.state.beast_surge",
            "tooltip.runic_races.state.adapting",
            "tooltip.runic_races.state.open_sky"
    );

    @Test
    void everyReferencedKeyExistsInEnUs() throws IOException {
        String content = Files.readString(LANG);

        List<String> keys = new ArrayList<>(NotificationRegistry.allReferencedKeys());
        keys.addAll(STATIC_KEYS);

        List<String> missing = new ArrayList<>();
        for (String key : keys) {
            // Match the key as a JSON property name: "key":
            if (!content.contains("\"" + key + "\":")) {
                missing.add(key);
            }
        }
        assertTrue(missing.isEmpty(), "Missing en_us.json keys: " + missing);
    }
}
