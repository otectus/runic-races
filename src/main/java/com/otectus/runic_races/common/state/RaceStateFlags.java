package com.otectus.runic_races.common.state;

/**
 * Bit flags for transient race-driven state. Each enum constant owns a single
 * bit in an {@code int} bitfield so the whole state packs into 4 bytes on the wire.
 *
 * This is not persistent — flags are (re)computed on power tick / event hooks
 * and mirror client-side via {@code S2CRaceStatePacket} for HUD rendering.
 */
public enum RaceStateFlags {
    /** Player is currently in their home biome (from BiomeAffinityPower). */
    BIOME_HOME(1 << 0),
    /** Player is currently in a hostile biome (from BiomeAffinityPower). */
    BIOME_HOSTILE(1 << 1),
    /** Night-empowered scaling attribute is active (from ScalingAttributePower). */
    NIGHT_EMPOWERED(1 << 2),
    /** Player is in a tight/enclosed space (Wyvern claustrophobia). */
    TIGHT_SPACE(1 << 3),
    /** Player is currently taking sunlight damage (Vampire). */
    SUNLIGHT_BURNING(1 << 4),
    /** Player is on fire and has fire-vulnerability (Dryad, Troll). */
    FIRE_VULNERABLE(1 << 5),
    /** Regrowth suppressed by fire contact (Troll). */
    REGROWTH_SUPPRESSED(1 << 6),
    /** Lycanthrope is currently in Beast Surge (Strength III buff active). */
    BEAST_SURGE(1 << 7),
    /** Human Adaptation stack count > 0 (details tracked server-side). */
    ADAPTATION_ACTIVE(1 << 8),
    /** Player is exposed to the open sky — storm empowerment cue (Volt Drake's grounded penalty lifts). */
    OPEN_SKY(1 << 9),
    /** Player is submerged and taking amplified damage (Feline hydrophobia, Volt Drake wet scales). */
    SUBMERGED_WEAK(1 << 10),
    /** Player is out of water and sluggish for it (Sea Serpen landbound coils). */
    DRY_SLUGGISH(1 << 11);

    private final int mask;

    RaceStateFlags(int mask) {
        this.mask = mask;
    }

    public int mask() {
        return mask;
    }

    public boolean isSet(int flags) {
        return (flags & mask) != 0;
    }

    public int set(int flags, boolean on) {
        return on ? (flags | mask) : (flags & ~mask);
    }

    /** Convenience: OR-together any number of flags into a starting value. */
    public static int of(RaceStateFlags... flags) {
        int result = 0;
        for (RaceStateFlags f : flags) result |= f.mask;
        return result;
    }
}
