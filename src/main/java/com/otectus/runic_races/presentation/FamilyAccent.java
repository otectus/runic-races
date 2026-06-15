package com.otectus.runic_races.presentation;

import java.util.Locale;

/**
 * Family-specific HUD accent colors. Used by the cooldown overlay to tint
 * icon frames and by the state-rune overlay to color active state glyphs.
 *
 * Palette chosen to match the existing command-layer conventions:
 * Human gold, Elven magenta, Dwarven slate, Bestial green, Faeborne teal,
 * Undead purple, Draconic red.
 *
 * NOTE: each constant name must equal the family id upper-cased — {@link #forFamily}
 * resolves via {@code valueOf(family.toUpperCase())}.
 */
public enum FamilyAccent {
    HUMAN(0xFFD4A235, 0xFF7A5E1A),
    ELVEN(0xFFD966D9, 0xFF6A2D6A),
    DWARVEN(0xFF8899AA, 0xFF364450),
    BESTIAL(0xFF66C266, 0xFF26612A),
    FAEBORNE(0xFF55E0C0, 0xFF1E5A4C),
    UNDEAD(0xFF9B59D9, 0xFF3F1E6A),
    DRACONIC(0xFFD94A2B, 0xFF6A1F10),
    UNKNOWN(0xFFCCCCCC, 0xFF555555);

    private final int accentColor;
    private final int frameShadow;

    FamilyAccent(int accentColor, int frameShadow) {
        this.accentColor = accentColor;
        this.frameShadow = frameShadow;
    }

    /** Fully-opaque ARGB accent color. */
    public int accent() {
        return accentColor;
    }

    /** Darker variant for frame shadows / pressed states. */
    public int shadow() {
        return frameShadow;
    }

    /** Accent with the given alpha channel (0-255) applied. */
    public int withAlpha(int alpha) {
        int clamped = Math.max(0, Math.min(255, alpha));
        return (clamped << 24) | (accentColor & 0x00FFFFFF);
    }

    public static FamilyAccent forFamily(String family) {
        if (family == null) return UNKNOWN;
        try {
            return valueOf(family.toUpperCase(Locale.ROOT));
        } catch (IllegalArgumentException e) {
            return UNKNOWN;
        }
    }
}
