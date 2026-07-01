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
        float knockbackTaken,
        boolean venomous,
        SlotGrant[] curiosSlotGrants
) {
    /** No extra curios slots. */
    public static final SlotGrant[] NO_SLOTS = new SlotGrant[0];

    /**
     * Convenience constructor for the common case: vanilla knockback (1.0), no venom.
     * {@code knockbackTaken} is a multiplier on knockback received — lightweight races
     * take more than 1.0 (enforced via LivingKnockBackEvent; a negative
     * knockback_resistance attribute clamps to 0 and does nothing). {@code venomous}
     * races inject Poison on direct melee hits (see RacialEventHandler).
     */
    public RaceDefinition(String name, String family, String displayName,
                          float scale, int maxFeathers, double luckBonus,
                          SlotGrant[] curiosSlotGrants) {
        this(name, family, displayName, scale, maxFeathers, luckBonus, 1.0f, false, curiosSlotGrants);
    }

    public record SlotGrant(String slotId, UUID uuid, int amount) {}
}
