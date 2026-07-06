package com.otectus.runic_races.presentation;

/**
 * Type-safe identifier for a signature moment. Each key maps to exactly one
 * {@link SignatureEntry} in {@link SignatureRegistry}.
 *
 * Keep the set minimal: a key only earns a slot here if it has a bespoke
 * sfx/vfx/banner recipe that needs to stay consistent across invocations.
 */
public enum SignatureKey {
    // Human
    PRIMIAN_FORTUNE,
    CELERON_DASH,
    MAGI_OVERFLOW,
    VALEN_STAND,
    // Elven
    HIGH_ELF_REFLEX,
    DARK_ELF_SHADOWMELD,
    MOON_ELF_VEIL,
    BLOOD_ELF_FRENZY,
    ICE_ELF_FROSTBIND,
    // Undead
    REAPER_REVIVAL,
    REAPER_REVIVAL_REJECTED,
    REAPER_HARVEST,
    WRAITH_PHASE,
    DEMON_WRATH,
    ZOMBIE_HUNGER,
    SKELETON_CONSCRIPT,
    // Bestial
    FELINE_NINE_LIVES,
    FELINE_POUNCE,
    ARACHNID_WEB_SNARE,
    AVIAN_WIND_BURST,
    CANINE_HOWL,
    KITSUNE_FOXFIRE,
    SERPEN_SHED,
    // Dwarven
    FORGE_BLESSING,
    RUNIC_WARD,
    DEEP_ONE_TREMOR,
    FROST_ONE_RESOLVE,
    IRON_ONE_SHIELD_WALL,
    SKY_ONE_LEAP,
    // Faeborne
    FAERIE_GLAMOUR,
    CHANGELING_MIRROR,
    DRYAD_BLOOM,
    SPRITE_PHASE,
    NYMPH_CHARM,
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
    FLIGHT_CANCEL,
    // Fragility procs (bannerless, victim-side "your weakness just bit" cues;
    // gating conditions live in RacialEventHandler, shared "fragility" debounce channel)
    HIGH_ELF_FRAGILITY,
    ARACHNID_FRAGILITY,
    SKELETON_FRAGILITY,
    AVIAN_FRAGILITY,
    SPRITE_FRAGILITY,
    CELERON_FRAGILITY,
    DEMON_FRAGILITY,
    // Passive proc cues (bannerless; the passive's moment made visible)
    PRIMIAN_ADAPTATION,
    VALEN_SHOULDER_CHECK,
    ZOMBIE_DEATHLESS,
    ARACHNID_WEB_SENSE,
    // Weakness onset cues (bannerless; fired on RaceStateFlags 0→1 edges via WeaknessCueRegistry)
    WEAKNESS_SUNLIGHT_SEAR,
    WEAKNESS_SUN_DAZZLE,
    WEAKNESS_KINDLING,
    WEAKNESS_THAW,
    WEAKNESS_HYDROPHOBIA,
    WEAKNESS_SHORT_CIRCUIT,
    WEAKNESS_DRY,
    WEAKNESS_COLD_IRON
}
