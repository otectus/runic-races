package com.otectus.runic_races.race;

import java.util.UUID;

/**
 * Authoritative definition of a race's metadata.
 * All race-dependent systems should query {@link RaceRegistry} rather than
 * maintaining their own hardcoded maps.
 */
public record RaceDefinition(
        String name,
        String family,
        String displayName,
        float scale,
        int maxFeathers,
        double luckBonus,
        SlotGrant[] curiosSlotGrants
) {
    /** No extra curios slots. */
    public static final SlotGrant[] NO_SLOTS = new SlotGrant[0];

    public record SlotGrant(String slotId, UUID uuid, int amount) {}
}
