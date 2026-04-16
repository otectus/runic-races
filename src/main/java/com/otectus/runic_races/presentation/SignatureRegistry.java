package com.otectus.runic_races.presentation;

import com.otectus.runic_races.presentation.SignatureEntry.SfxSpec;
import com.otectus.runic_races.presentation.SignatureEntry.VfxSpec;
import net.minecraft.ChatFormatting;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.sounds.SoundEvents;

import java.util.EnumMap;
import java.util.List;
import java.util.Map;

/**
 * Single source of truth for signature-ability presentation recipes. Adding a new
 * signature moment means adding a {@link SignatureKey} and a row here — no event-handler
 * edits required.
 *
 * Particle counts follow the CLAUDE.md VFX guideline (minor 10-20 / major 30-60 / mythic 80+).
 * Banner text uses {@code %s} placeholders where callers need to pass runtime substitutions
 * (see {@link RunicPresentation#fire(net.minecraft.server.level.ServerPlayer, SignatureKey, Object...)}).
 */
public final class SignatureRegistry {

    private static final Map<SignatureKey, SignatureEntry> ENTRIES = new EnumMap<>(SignatureKey.class);

    static {
        // ===== Catfolk: Nine Lives (MYTHIC — literal life-saving moment) =====
        ENTRIES.put(SignatureKey.CATFOLK_NINE_LIVES, new SignatureEntry(
                "A life is spent... but you persist!",
                ChatFormatting.GOLD,
                true,
                List.of(
                        new SfxSpec(SoundEvents.CAT_PURR, 0.7f, 0.8f),
                        new SfxSpec(SoundEvents.TOTEM_USE, 0.5f, 1.4f)
                ),
                List.of(
                        new VfxSpec(ParticleTypes.HEART, 40, 0.6, 1.0, 0.6, 0.1),
                        new VfxSpec(ParticleTypes.END_ROD, 30, 0.8, 1.0, 0.8, 0.15),
                        new VfxSpec(ParticleTypes.CRIT, 20, 0.5, 0.8, 0.5, 0.2)
                ),
                CueType.LIFE_RUNE_FLASH,
                20,
                Intensity.MYTHIC
        ));

        // ===== Revenant: Death-Site Revival (MYTHIC) =====
        ENTRIES.put(SignatureKey.REVENANT_REVIVAL, new SignatureEntry(
                "Death cannot hold you. You rise where you fell.",
                ChatFormatting.DARK_PURPLE,
                true,
                List.of(
                        new SfxSpec(SoundEvents.WITHER_AMBIENT, 0.4f, 1.5f),
                        new SfxSpec(SoundEvents.BELL_BLOCK, 0.6f, 0.5f)
                ),
                List.of(
                        new VfxSpec(ParticleTypes.SOUL, 40, 0.6, 1.2, 0.6, 0.08),
                        new VfxSpec(ParticleTypes.WITCH, 25, 0.8, 1.5, 0.8, 0.1),
                        new VfxSpec(ParticleTypes.ENCHANT, 20, 0.8, 1.0, 0.8, 0.3)
                ),
                CueType.VIGNETTE_PULSE,
                40,
                Intensity.MYTHIC
        ));

        // ===== Revenant: Revival failure (MINOR — grave rejects you) =====
        ENTRIES.put(SignatureKey.REVENANT_REVIVAL_REJECTED, new SignatureEntry(
                "The grave rejects your return... you awaken elsewhere.",
                ChatFormatting.GRAY,
                false,
                List.of(new SfxSpec(SoundEvents.SOUL_ESCAPE, 0.4f, 0.8f)),
                List.of(new VfxSpec(ParticleTypes.ASH, 15, 0.4, 0.8, 0.4, 0.05)),
                null,
                0,
                Intensity.MINOR
        ));

        // ===== Mountain/Deep Dwarf: Forge Blessing (MAJOR) =====
        // Banner has %s for "Unbreaking II" style suffix.
        ENTRIES.put(SignatureKey.DWARF_FORGE_BLESSING, new SignatureEntry(
                "The forge blesses your work! %s applied.",
                ChatFormatting.GOLD,
                true,
                List.of(
                        new SfxSpec(SoundEvents.ANVIL_LAND, 0.35f, 1.3f),
                        new SfxSpec(SoundEvents.ENCHANTMENT_TABLE_USE, 0.5f, 1.1f)
                ),
                List.of(
                        new VfxSpec(ParticleTypes.CRIT, 30, 0.4, 0.6, 0.4, 0.2),
                        new VfxSpec(ParticleTypes.ENCHANT, 25, 0.6, 1.0, 0.6, 0.3),
                        new VfxSpec(ParticleTypes.FLAME, 15, 0.3, 0.3, 0.3, 0.05)
                ),
                null,
                0,
                Intensity.MAJOR
        ));

        // ===== Sprite Wing Flap (MINOR — frequent passive-active) =====
        ENTRIES.put(SignatureKey.SPRITE_WING_FLAP, new SignatureEntry(
                "Your wings flutter!",
                ChatFormatting.LIGHT_PURPLE,
                true,
                List.of(new SfxSpec(SoundEvents.BEE_LOOP, 0.2f, 2.0f)),
                List.of(
                        new VfxSpec(ParticleTypes.END_ROD, 12, 0.3, 0.3, 0.3, 0.3),
                        new VfxSpec(ParticleTypes.CLOUD, 10, 0.3, 0.1, 0.3, 0.02)
                ),
                null,
                0,
                Intensity.MINOR
        ));

        // ===== Wyvern Wing Flap (MAJOR) =====
        ENTRIES.put(SignatureKey.WYVERN_WING_FLAP, new SignatureEntry(
                "Your wings beat powerfully!",
                ChatFormatting.GOLD,
                true,
                List.of(new SfxSpec(SoundEvents.ENDER_DRAGON_FLAP, 0.4f, 2.0f)),
                List.of(new VfxSpec(ParticleTypes.CLOUD, 30, 0.5, 0.2, 0.5, 0.2)),
                null,
                0,
                Intensity.MAJOR
        ));

        // ===== Elder Drake Wing Flap (MAJOR — heavier, darker) =====
        ENTRIES.put(SignatureKey.ELDER_DRAKE_WING_FLAP, new SignatureEntry(
                "Your ancient wings thunder!",
                ChatFormatting.DARK_RED,
                true,
                List.of(new SfxSpec(SoundEvents.ENDER_DRAGON_FLAP, 0.6f, 1.0f)),
                List.of(
                        new VfxSpec(ParticleTypes.DRAGON_BREATH, 35, 0.8, 0.3, 0.8, 0.3),
                        new VfxSpec(ParticleTypes.SMOKE, 15, 0.5, 0.3, 0.5, 0.15),
                        new VfxSpec(ParticleTypes.CAMPFIRE_SIGNAL_SMOKE, 8, 1.0, 0.1, 1.0, 0.01)
                ),
                null,
                0,
                Intensity.MAJOR
        ));

        // ===== Flight Cancel (MINOR) =====
        ENTRIES.put(SignatureKey.FLIGHT_CANCEL, new SignatureEntry(
                "You fold your wings.",
                ChatFormatting.GRAY,
                false,
                List.of(new SfxSpec(SoundEvents.PHANTOM_FLAP, 0.3f, 0.8f)),
                List.of(),
                null,
                0,
                Intensity.MINOR
        ));
    }

    public static SignatureEntry get(SignatureKey key) {
        return ENTRIES.get(key);
    }

    private SignatureRegistry() {}
}
