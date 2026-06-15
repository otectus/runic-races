package com.otectus.runic_races.presentation;

/**
 * Type-safe identifier for a signature moment. Each key maps to exactly one
 * {@link SignatureEntry} in {@link SignatureRegistry}.
 *
 * Keep the set minimal: a key only earns a slot here if it has a bespoke
 * sfx/vfx/banner recipe that needs to stay consistent across invocations.
 */
public enum SignatureKey {
    // Undead
    REAPER_REVIVAL,
    REAPER_REVIVAL_REJECTED,
    WRAITH_PHASE,
    DEMON_WRATH,
    // Bestial
    FELINE_NINE_LIVES,
    // Dwarven
    FORGE_BLESSING,
    RUNIC_WARD,
    // Faeborne
    FAERIE_GLAMOUR,
    // Draconic elemental breaths
    FIRE_DRAKE_BREATH,
    ICE_DRAKE_BREATH,
    SEA_SERPEN_BREATH,
    TERRA_DRAKE_BREATH,
    VOLT_DRAKE_BREATH,
    WIND_WYRM_BREATH,
    // Wing flaps (fired by the flight handler)
    SPRITE_WING_FLAP,
    FAERIE_WING_FLAP,
    AVIAN_WING_FLAP,
    WIND_WYRM_WING_FLAP,
    FLIGHT_CANCEL
}
