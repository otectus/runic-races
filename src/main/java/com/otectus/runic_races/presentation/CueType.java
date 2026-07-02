package com.otectus.runic_races.presentation;

/**
 * Screen overlay cue rendered client-side by {@code ScreenCueRenderer} in response to
 * {@code S2CScreenCuePacket}. Each cue is a short-lived visual flourish layered on top of the HUD.
 */
public enum CueType {
    /** Brief full-screen flash (frozen-time feel). Used for Nine Lives, Death Revival. */
    FREEZE_FRAME,
    /** Radial vignette pulse. Used for Revenant revival, ancient presence. */
    VIGNETTE_PULSE,
    /** Heat-shimmer distortion frame overlay. Used for Elder Drake roar. */
    HEAT_SHIMMER,
    /** Real camera shake (perturbs view roll/pitch). Used for Terra/Wind drake breaths and heavy landings. */
    SHAKE,
    /** Pulsing red heart-outline flash. Used for low-HP triggers. */
    HEARTBEAT_FLASH,
    /** Single "life rune" glyph flash at HUD center. Used for Nine Lives save. */
    LIFE_RUNE_FLASH,
    /** Soft silver radial bloom from the screen edges inward. Used for Moon Elf moonlight. */
    MOON_GLOW,
    /** Pale-cyan vignette frosting (ice creeping in from the edges). Used for frost abilities. */
    FROST_RIME,
    /** Brief horizontal edge-streaks suggesting sudden speed. Used for dashes, leaps, wind bursts. */
    WIND_STREAK
}
