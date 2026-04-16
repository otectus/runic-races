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
    /** Camera shake proxy (renderer offsets GUI slightly). Used for Giant-Blooded landings. */
    SHAKE,
    /** Pulsing red heart-outline flash. Used for low-HP triggers. */
    HEARTBEAT_FLASH,
    /** Single "life rune" glyph flash at HUD center. Used for Nine Lives save. */
    LIFE_RUNE_FLASH
}
