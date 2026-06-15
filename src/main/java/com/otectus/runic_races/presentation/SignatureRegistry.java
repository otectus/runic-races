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
 * Banner entries store a translation <em>key</em>; lang values use {@code %s} placeholders where
 * callers pass runtime substitutions
 * (see {@link RunicPresentation#fire(net.minecraft.server.level.ServerPlayer, SignatureKey, Object...)}).
 */
public final class SignatureRegistry {

    private static final Map<SignatureKey, SignatureEntry> ENTRIES = new EnumMap<>(SignatureKey.class);

    static {
        // ===== Reaper: Death's Door revival (MYTHIC — literal life-saving moment) =====
        ENTRIES.put(SignatureKey.REAPER_REVIVAL, new SignatureEntry(
                "message.runic_races.signature.reaper.revival",
                ChatFormatting.DARK_PURPLE,
                true,
                List.of(
                        new SfxSpec(SoundEvents.WITHER_AMBIENT, 0.4f, 1.5f),
                        new SfxSpec(SoundEvents.BELL_BLOCK, 0.6f, 0.5f)
                ),
                List.of(
                        new VfxSpec(ParticleTypes.SOUL, 40, 0.6, 1.2, 0.6, 0.08),
                        new VfxSpec(ParticleTypes.SCULK_SOUL, 25, 0.8, 1.5, 0.8, 0.1),
                        new VfxSpec(ParticleTypes.ENCHANT, 20, 0.8, 1.0, 0.8, 0.3)
                ),
                CueType.VIGNETTE_PULSE,
                40,
                Intensity.MYTHIC
        ));

        // ===== Reaper: revival on cooldown (MINOR — the grave refuses you) =====
        ENTRIES.put(SignatureKey.REAPER_REVIVAL_REJECTED, new SignatureEntry(
                "message.runic_races.signature.reaper.revival_rejected",
                ChatFormatting.GRAY,
                false,
                List.of(new SfxSpec(SoundEvents.SOUL_ESCAPE, 0.4f, 0.8f)),
                List.of(new VfxSpec(ParticleTypes.ASH, 15, 0.4, 0.8, 0.4, 0.05)),
                null,
                0,
                Intensity.MINOR
        ));

        // ===== Wraith: Spectral Phase (MAJOR) =====
        ENTRIES.put(SignatureKey.WRAITH_PHASE, new SignatureEntry(
                "message.runic_races.signature.wraith.phase",
                ChatFormatting.DARK_AQUA,
                true,
                List.of(
                        new SfxSpec(SoundEvents.SOUL_ESCAPE, 0.5f, 0.7f),
                        new SfxSpec(SoundEvents.WITHER_AMBIENT, 0.3f, 1.6f)
                ),
                List.of(
                        new VfxSpec(ParticleTypes.SOUL_FIRE_FLAME, 25, 0.5, 1.0, 0.5, 0.05),
                        new VfxSpec(ParticleTypes.SMOKE, 15, 0.5, 0.8, 0.5, 0.05),
                        new VfxSpec(ParticleTypes.SOUL, 15, 0.6, 1.0, 0.6, 0.08)
                ),
                null,
                0,
                Intensity.MAJOR
        ));

        // ===== Demon: Infernal Wrath (MYTHIC) =====
        ENTRIES.put(SignatureKey.DEMON_WRATH, new SignatureEntry(
                "message.runic_races.signature.demon.wrath",
                ChatFormatting.RED,
                true,
                List.of(
                        new SfxSpec(SoundEvents.RAVAGER_ROAR, 0.8f, 0.7f),
                        new SfxSpec(SoundEvents.GENERIC_EXPLODE, 0.5f, 1.0f)
                ),
                List.of(
                        new VfxSpec(ParticleTypes.FLAME, 40, 0.8, 0.6, 0.8, 0.1),
                        new VfxSpec(ParticleTypes.SOUL_FIRE_FLAME, 25, 0.6, 0.6, 0.6, 0.08),
                        new VfxSpec(ParticleTypes.LAVA, 10, 0.5, 0.3, 0.5, 0.02)
                ),
                CueType.HEAT_SHIMMER,
                25,
                Intensity.MYTHIC
        ));

        // ===== Feline: Nine Lives (MYTHIC — literal life-saving moment) =====
        ENTRIES.put(SignatureKey.FELINE_NINE_LIVES, new SignatureEntry(
                "message.runic_races.signature.feline.nine_lives",
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

        // ===== Forge One: Forge Blessing (MAJOR) =====
        ENTRIES.put(SignatureKey.FORGE_BLESSING, new SignatureEntry(
                "message.runic_races.signature.forge_one.forge_blessing",
                ChatFormatting.GOLD,
                true,
                List.of(
                        new SfxSpec(SoundEvents.ANVIL_LAND, 0.35f, 1.3f),
                        new SfxSpec(SoundEvents.ENCHANTMENT_TABLE_USE, 0.5f, 1.1f)
                ),
                List.of(
                        new VfxSpec(ParticleTypes.FLAME, 18, 0.4, 0.6, 0.4, 0.05),
                        new VfxSpec(ParticleTypes.CRIT, 20, 0.4, 0.6, 0.4, 0.2),
                        new VfxSpec(ParticleTypes.ENCHANT, 15, 0.6, 1.0, 0.6, 0.3),
                        new VfxSpec(RaceColors.FORGE_EMBER, 7, 0.4, 0.5, 0.4, 0.02)
                ),
                null,
                0,
                Intensity.MAJOR
        ));

        // ===== Runic One: Rune of Warding (MAJOR) =====
        ENTRIES.put(SignatureKey.RUNIC_WARD, new SignatureEntry(
                "message.runic_races.signature.runic_one.runic_ward",
                ChatFormatting.AQUA,
                true,
                List.of(
                        new SfxSpec(SoundEvents.ENCHANTMENT_TABLE_USE, 0.5f, 0.8f),
                        new SfxSpec(SoundEvents.BEACON_ACTIVATE, 0.5f, 1.2f)
                ),
                List.of(
                        new VfxSpec(ParticleTypes.ENCHANT, 30, 0.8, 1.0, 0.8, 0.3),
                        new VfxSpec(ParticleTypes.END_ROD, 20, 0.8, 0.6, 0.8, 0.1),
                        new VfxSpec(ParticleTypes.CRIT, 15, 0.6, 0.4, 0.6, 0.2)
                ),
                CueType.VIGNETTE_PULSE,
                20,
                Intensity.MAJOR
        ));

        // ===== Faerie: Faerie Bargain glamour (MYTHIC) =====
        ENTRIES.put(SignatureKey.FAERIE_GLAMOUR, new SignatureEntry(
                "message.runic_races.signature.faerie.glamour",
                ChatFormatting.LIGHT_PURPLE,
                true,
                List.of(
                        new SfxSpec(SoundEvents.ENCHANTMENT_TABLE_USE, 0.6f, 1.5f),
                        new SfxSpec(SoundEvents.BELL_BLOCK, 0.5f, 1.8f)
                ),
                List.of(
                        new VfxSpec(ParticleTypes.FIREWORK, 30, 0.8, 1.0, 0.8, 0.2),
                        new VfxSpec(ParticleTypes.END_ROD, 25, 0.8, 1.0, 0.8, 0.15),
                        new VfxSpec(ParticleTypes.HAPPY_VILLAGER, 20, 0.8, 1.0, 0.8, 0.1)
                ),
                CueType.VIGNETTE_PULSE,
                25,
                Intensity.MYTHIC
        ));

        // ===== Draconic breaths (MAJOR each; wind wyrm MYTHIC) =====
        ENTRIES.put(SignatureKey.FIRE_DRAKE_BREATH, new SignatureEntry(
                "message.runic_races.signature.fire_drake.breath",
                ChatFormatting.RED, true,
                List.of(new SfxSpec(SoundEvents.ENDER_DRAGON_GROWL, 0.9f, 1.3f),
                        new SfxSpec(SoundEvents.BLAZE_SHOOT, 0.6f, 0.6f)),
                List.of(new VfxSpec(ParticleTypes.DRAGON_BREATH, 34, 0.8, 0.3, 0.8, 0.3),
                        new VfxSpec(ParticleTypes.FLAME, 18, 0.6, 0.3, 0.6, 0.1),
                        new VfxSpec(RaceColors.FORGE_EMBER, 8, 0.7, 0.3, 0.7, 0.05)),
                null, 0, Intensity.MAJOR));

        ENTRIES.put(SignatureKey.ICE_DRAKE_BREATH, new SignatureEntry(
                "message.runic_races.signature.ice_drake.breath",
                ChatFormatting.AQUA, true,
                List.of(new SfxSpec(SoundEvents.ENDER_DRAGON_GROWL, 0.9f, 1.5f),
                        new SfxSpec(SoundEvents.GLASS_BREAK, 0.5f, 0.7f)),
                List.of(new VfxSpec(ParticleTypes.SNOWFLAKE, 34, 0.8, 0.3, 0.8, 0.2),
                        new VfxSpec(ParticleTypes.DRAGON_BREATH, 13, 0.6, 0.3, 0.6, 0.2),
                        new VfxSpec(RaceColors.GLACIAL_CYAN, 8, 0.7, 0.3, 0.7, 0.05)),
                CueType.FROST_RIME, 20, Intensity.MAJOR));

        ENTRIES.put(SignatureKey.SEA_SERPEN_BREATH, new SignatureEntry(
                "message.runic_races.signature.sea_serpen.breath",
                ChatFormatting.BLUE, true,
                List.of(new SfxSpec(SoundEvents.ENDER_DRAGON_GROWL, 0.9f, 1.1f),
                        new SfxSpec(SoundEvents.PLAYER_SPLASH, 0.7f, 0.8f)),
                List.of(new VfxSpec(ParticleTypes.BUBBLE, 40, 0.8, 0.3, 0.8, 0.2),
                        new VfxSpec(ParticleTypes.SPLASH, 15, 0.6, 0.3, 0.6, 0.1),
                        new VfxSpec(ParticleTypes.DRAGON_BREATH, 10, 0.6, 0.3, 0.6, 0.2)),
                null, 0, Intensity.MAJOR));

        ENTRIES.put(SignatureKey.TERRA_DRAKE_BREATH, new SignatureEntry(
                "message.runic_races.signature.terra_drake.breath",
                ChatFormatting.DARK_GREEN, true,
                List.of(new SfxSpec(SoundEvents.ENDER_DRAGON_GROWL, 0.9f, 0.7f),
                        new SfxSpec(SoundEvents.GENERIC_EXPLODE, 0.4f, 0.6f)),
                List.of(new VfxSpec(ParticleTypes.POOF, 35, 0.8, 0.4, 0.8, 0.1),
                        new VfxSpec(ParticleTypes.CRIT, 20, 0.6, 0.3, 0.6, 0.2)),
                CueType.SHAKE, 10, Intensity.MAJOR));

        ENTRIES.put(SignatureKey.VOLT_DRAKE_BREATH, new SignatureEntry(
                "message.runic_races.signature.volt_drake.breath",
                ChatFormatting.YELLOW, true,
                List.of(new SfxSpec(SoundEvents.ENDER_DRAGON_GROWL, 0.9f, 1.6f),
                        new SfxSpec(SoundEvents.LIGHTNING_BOLT_THUNDER, 0.4f, 1.5f)),
                List.of(new VfxSpec(ParticleTypes.ELECTRIC_SPARK, 34, 0.8, 0.3, 0.8, 0.3),
                        new VfxSpec(ParticleTypes.CRIT, 13, 0.6, 0.3, 0.6, 0.2),
                        new VfxSpec(RaceColors.VOLT_GOLD, 8, 0.7, 0.3, 0.7, 0.1)),
                null, 0, Intensity.MAJOR));

        ENTRIES.put(SignatureKey.WIND_WYRM_BREATH, new SignatureEntry(
                "message.runic_races.signature.wind_wyrm.breath",
                ChatFormatting.WHITE, true,
                List.of(new SfxSpec(SoundEvents.ENDER_DRAGON_GROWL, 0.9f, 1.2f),
                        new SfxSpec(SoundEvents.ENDER_DRAGON_FLAP, 0.6f, 0.7f)),
                List.of(new VfxSpec(ParticleTypes.CLOUD, 40, 0.9, 0.4, 0.9, 0.3),
                        new VfxSpec(ParticleTypes.POOF, 20, 0.6, 0.3, 0.6, 0.2),
                        new VfxSpec(ParticleTypes.DRAGON_BREATH, 15, 0.7, 0.3, 0.7, 0.2)),
                CueType.SHAKE, 15, Intensity.MYTHIC));

        // ===== Wing flaps =====
        ENTRIES.put(SignatureKey.SPRITE_WING_FLAP, new SignatureEntry(
                "message.runic_races.signature.sprite.wing_flap",
                ChatFormatting.LIGHT_PURPLE, true,
                List.of(new SfxSpec(SoundEvents.BEE_LOOP, 0.2f, 2.0f)),
                List.of(new VfxSpec(ParticleTypes.END_ROD, 12, 0.3, 0.3, 0.3, 0.3),
                        new VfxSpec(ParticleTypes.CLOUD, 10, 0.3, 0.1, 0.3, 0.02)),
                null, 0, Intensity.MINOR));

        ENTRIES.put(SignatureKey.FAERIE_WING_FLAP, new SignatureEntry(
                "message.runic_races.signature.faerie.wing_flap",
                ChatFormatting.LIGHT_PURPLE, true,
                List.of(new SfxSpec(SoundEvents.BEE_LOOP, 0.2f, 1.8f)),
                List.of(new VfxSpec(ParticleTypes.END_ROD, 12, 0.3, 0.3, 0.3, 0.3),
                        new VfxSpec(ParticleTypes.FIREWORK, 8, 0.3, 0.1, 0.3, 0.05)),
                null, 0, Intensity.MINOR));

        ENTRIES.put(SignatureKey.AVIAN_WING_FLAP, new SignatureEntry(
                "message.runic_races.signature.avian.wing_flap",
                ChatFormatting.AQUA, true,
                List.of(new SfxSpec(SoundEvents.PHANTOM_FLAP, 0.3f, 1.4f)),
                List.of(new VfxSpec(ParticleTypes.CLOUD, 12, 0.4, 0.2, 0.4, 0.1),
                        new VfxSpec(ParticleTypes.POOF, 8, 0.3, 0.1, 0.3, 0.02)),
                null, 0, Intensity.MINOR));

        ENTRIES.put(SignatureKey.WIND_WYRM_WING_FLAP, new SignatureEntry(
                "message.runic_races.signature.wind_wyrm.wing_flap",
                ChatFormatting.WHITE, true,
                List.of(new SfxSpec(SoundEvents.ENDER_DRAGON_FLAP, 0.5f, 1.2f)),
                List.of(new VfxSpec(ParticleTypes.CLOUD, 30, 0.5, 0.2, 0.5, 0.2)),
                null, 0, Intensity.MAJOR));

        // ===== Flight Cancel (MINOR) =====
        ENTRIES.put(SignatureKey.FLIGHT_CANCEL, new SignatureEntry(
                "message.runic_races.signature.flight.cancel",
                ChatFormatting.GRAY, false,
                List.of(new SfxSpec(SoundEvents.PHANTOM_FLAP, 0.3f, 0.8f)),
                List.of(),
                null, 0, Intensity.MINOR));
    }

    public static SignatureEntry get(SignatureKey key) {
        return ENTRIES.get(key);
    }

    private SignatureRegistry() {}
}
