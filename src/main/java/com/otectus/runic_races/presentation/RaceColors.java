package com.otectus.runic_races.presentation;

import net.minecraft.core.particles.DustParticleOptions;
import org.joml.Vector3f;

/**
 * Precise per-identity particle colors expressed as redstone-style {@link DustParticleOptions}.
 * Vanilla {@code SimpleParticleType}s only <em>approximate</em> a race's color; coloured dust lets
 * a signature hit the exact accent so the VFX matches the HUD frame tint.
 *
 * RGB values are aligned with {@link FamilyAccent} (the same palette the cooldown/state-rune HUD
 * uses) so a race's particle accent and its icon frame read as the same colour. These are layered
 * as an extra accent on top of a readable vanilla base particle — never as the sole particle of an
 * ability (see {@link SignatureRegistry}).
 */
public final class RaceColors {

    /** Build a 1.0-scale dust option from a packed 0xRRGGBB colour. */
    private static DustParticleOptions dust(int rgb) {
        float r = ((rgb >> 16) & 0xFF) / 255.0f;
        float g = ((rgb >> 8) & 0xFF) / 255.0f;
        float b = (rgb & 0xFF) / 255.0f;
        return new DustParticleOptions(new Vector3f(r, g, b), 1.0f);
    }

    // ----- Race / family accents -----
    /** Moon Elf — silvered moonlight. */
    public static final DustParticleOptions SILVER_MOON = dust(0xCFE0FF);
    /** Blood Elf — deep crimson. */
    public static final DustParticleOptions CRIMSON_BLOOD = dust(0xB01530);
    /** Frost One / Ice Drake — pale glacial cyan. */
    public static final DustParticleOptions GLACIAL_CYAN = dust(0xA9E8FF);
    /** Forge One / Fire Drake / Demon — molten ember orange. */
    public static final DustParticleOptions FORGE_EMBER = dust(0xFF7A2A);
    /** Volt Drake — electric gold. */
    public static final DustParticleOptions VOLT_GOLD = dust(0xFFE34A);
    /** Faeborne / Dryad — verdant green (matches BESTIAL/nature accent). */
    public static final DustParticleOptions VERDANT_GREEN = dust(0x66C266);
    /** Undead souls — violet (matches UNDEAD accent). */
    public static final DustParticleOptions SOUL_VIOLET = dust(0x9B59D9);

    private RaceColors() {}
}
