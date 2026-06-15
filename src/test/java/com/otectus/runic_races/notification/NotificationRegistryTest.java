package com.otectus.runic_races.notification;

import com.otectus.runic_races.common.state.RaceStateFlags;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * Verifies the {@code (race, flag)} -> copy mapping. Pure: {@link NotificationRegistry}
 * imports only {@link RaceStateFlags}, so no Minecraft runtime is needed.
 */
class NotificationRegistryTest {

    @Test
    void harmfulPairResolvesToExpectedKeysAndColor() {
        NotificationSpec spec = NotificationRegistry.resolve("zombie", RaceStateFlags.SUNLIGHT_BURNING);
        assertNotNull(spec);
        assertEquals("message.runic_races.zombie.sunlight.start", spec.startKey());
        assertEquals("message.runic_races.zombie.sunlight.stop", spec.stopKey());
        assertEquals("dark_green", spec.colorName());
    }

    @Test
    void sharedFlagDisambiguatesByRace() {
        NotificationSpec sky = NotificationRegistry.resolve("sky_one", RaceStateFlags.TIGHT_SPACE);
        NotificationSpec wyrm = NotificationRegistry.resolve("wind_wyrm", RaceStateFlags.TIGHT_SPACE);
        assertNotNull(sky);
        assertNotNull(wyrm);
        assertNotEquals(sky.startKey(), wyrm.startKey());

        NotificationSpec deepOne = NotificationRegistry.resolve("deep_one", RaceStateFlags.SUNLIGHT_BURNING);
        NotificationSpec skeleton = NotificationRegistry.resolve("skeleton", RaceStateFlags.SUNLIGHT_BURNING);
        assertNotEquals(deepOne.startKey(), skeleton.startKey());
    }

    @Test
    void openSkyPairsArePresent() {
        assertNotNull(NotificationRegistry.resolve("volt_drake", RaceStateFlags.OPEN_SKY));
    }

    @Test
    void informationalFlagsAreSilent() {
        RaceStateFlags[] informational = {
                RaceStateFlags.BIOME_HOME,
                RaceStateFlags.BIOME_HOSTILE,
                RaceStateFlags.NIGHT_EMPOWERED,
                RaceStateFlags.ADAPTATION_ACTIVE
        };
        for (RaceStateFlags f : informational) {
            assertNull(NotificationRegistry.resolve("zombie", f), f + " must not banner");
            assertNull(NotificationRegistry.resolve("volt_drake", f), f + " must not banner");
        }
    }

    @Test
    void unknownRaceOrNullArgsResolveToNull() {
        assertNull(NotificationRegistry.resolve("not_a_race", RaceStateFlags.SUNLIGHT_BURNING));
        assertNull(NotificationRegistry.resolve(null, RaceStateFlags.SUNLIGHT_BURNING));
        assertNull(NotificationRegistry.resolve("zombie", null));
    }

    @Test
    void everyReferencedKeyIsWellFormed() {
        assertTrue(NotificationRegistry.allReferencedKeys().size() >= 20);
        for (String key : NotificationRegistry.allReferencedKeys()) {
            assertTrue(key.startsWith("message.runic_races."), key);
            assertTrue(key.endsWith(".start") || key.endsWith(".stop"), key);
        }
    }
}
