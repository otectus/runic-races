package com.otectus.runic_races.presentation;

import java.util.Locale;

/**
 * Family-specific HUD accent colors. Used by the cooldown overlay to tint
 * icon frames and by the state-rune overlay to color active state glyphs.
 *
 * Palette chosen to match the existing command-layer conventions:
 * Mortal gold, Fae magenta, Beast green, Underfolk slate, Dragon red, Cursed purple.
 */
public enum FamilyAccent {
    MORTAL(0xFFD4A235, 0xFF7A5E1A),
    FAE(0xFFD966D9, 0xFF6A2D6A),
    BEAST(0xFF66C266, 0xFF26612A),
    UNDERFOLK(0xFF8899AA, 0xFF364450),
    DRAGON(0xFFD94A2B, 0xFF6A1F10),
    CURSED(0xFF9B59D9, 0xFF3F1E6A),
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
