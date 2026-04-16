package com.otectus.runic_races.presentation;

/**
 * Type-safe identifier for a signature moment. Each key maps to exactly one
 * {@link SignatureEntry} in {@link SignatureRegistry}.
 *
 * Keep the set minimal: a key only earns a slot here if it has a bespoke
 * sfx/vfx/banner recipe that needs to stay consistent across invocations.
 */
public enum SignatureKey {
    CATFOLK_NINE_LIVES,
    REVENANT_REVIVAL,
    REVENANT_REVIVAL_REJECTED,
    DWARF_FORGE_BLESSING,
    SPRITE_WING_FLAP,
    WYVERN_WING_FLAP,
    ELDER_DRAKE_WING_FLAP,
    FLIGHT_CANCEL
}
