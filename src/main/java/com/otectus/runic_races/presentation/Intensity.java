package com.otectus.runic_races.presentation;

/**
 * VFX density tier. Mirrors the CLAUDE.md guideline (minor 10-20 / major 30-60 / mythic 80+).
 * Declarative tags on each {@link SignatureEntry} so future client config can globally scale particle density.
 */
public enum Intensity {
    MINOR,
    MAJOR,
    MYTHIC
}
