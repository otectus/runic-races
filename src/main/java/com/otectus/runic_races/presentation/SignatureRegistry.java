package com.otectus.runic_races.presentation;

import com.otectus.runic_races.presentation.SignatureEntry.SfxSpec;
import com.otectus.runic_races.presentation.SignatureEntry.VfxSpec;
import com.otectus.runic_races.registry.ModParticles;
import com.otectus.runic_races.registry.ModSounds;
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
                        new SfxSpec(ModSounds.MYTHIC_REVIVAL, 0.8f, 1.0f),
                        new SfxSpec(SoundEvents.BELL_BLOCK, 0.6f, 0.5f)
                ),
                List.of(
                        new VfxSpec(ParticleTypes.SOUL, 40, 0.6, 1.2, 0.6, 0.08),
                        new VfxSpec(ModParticles.SOUL_WISP, 25, 0.8, 1.5, 0.8, 0.1),
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
                        new VfxSpec(ModParticles.SOUL_WISP, 15, 0.6, 1.0, 0.6, 0.08)
                ),
                null,
                0,
                Intensity.MAJOR
        ));

        // ===== Demon: Infernal Wrath (MAJOR — routine 50s active, not a life-saving moment) =====
        ENTRIES.put(SignatureKey.DEMON_WRATH, new SignatureEntry(
                "message.runic_races.signature.demon.wrath",
                ChatFormatting.RED,
                true,
                List.of(
                        new SfxSpec(SoundEvents.RAVAGER_ROAR, 0.8f, 0.7f),
                        new SfxSpec(SoundEvents.GENERIC_EXPLODE, 0.5f, 1.0f)
                ),
                List.of(
                        // Soul-fire + embers: infernal palette, distinct from Fire Drake's dragon-breath cone.
                        new VfxSpec(ParticleTypes.SOUL_FIRE_FLAME, 24, 0.7, 0.6, 0.7, 0.08),
                        new VfxSpec(ModParticles.EMBER_SCALE, 18, 0.6, 0.6, 0.6, 0.06),
                        new VfxSpec(ParticleTypes.LAVA, 8, 0.5, 0.3, 0.5, 0.02)
                ),
                CueType.HEAT_SHIMMER,
                25,
                Intensity.MAJOR
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
                        new VfxSpec(ModParticles.RUNE_GLYPH, 10, 0.6, 1.0, 0.6, 0.05),
                        new VfxSpec(RaceColors.FORGE_EMBER, 7, 0.4, 0.5, 0.4, 0.02)
                ),
                null,
                0,
                Intensity.MAJOR
        ));

        // ===== Runic One: Rune of Warding (MAJOR, 60 particles — top of band) =====
        ENTRIES.put(SignatureKey.RUNIC_WARD, new SignatureEntry(
                "message.runic_races.signature.runic_one.runic_ward",
                ChatFormatting.AQUA,
                true,
                List.of(
                        new SfxSpec(SoundEvents.ENCHANTMENT_TABLE_USE, 0.5f, 0.8f),
                        new SfxSpec(ModSounds.WARD_ACTIVATE, 0.6f, 1.0f)
                ),
                List.of(
                        new VfxSpec(ParticleTypes.ENCHANT, 30, 0.8, 1.0, 0.8, 0.3),
                        // Rune glyphs hang in a perfect circle, marking the warded zone for allies.
                        new VfxSpec(ModParticles.RUNE_GLYPH, 18, 2.0, 0.6, 2.0, 0.02, SignatureEntry.Shape.RING),
                        new VfxSpec(ParticleTypes.CRIT, 12, 0.6, 0.4, 0.6, 0.2)
                ),
                CueType.VIGNETTE_PULSE,
                20,
                Intensity.MAJOR
        ));

        // ===== Faerie: Faerie Bargain glamour (MAJOR — routine 50s active) =====
        ENTRIES.put(SignatureKey.FAERIE_GLAMOUR, new SignatureEntry(
                "message.runic_races.signature.faerie.glamour",
                ChatFormatting.LIGHT_PURPLE,
                true,
                List.of(
                        new SfxSpec(SoundEvents.ENCHANTMENT_TABLE_USE, 0.6f, 1.5f),
                        new SfxSpec(SoundEvents.BELL_BLOCK, 0.5f, 1.8f)
                ),
                List.of(
                        new VfxSpec(ParticleTypes.FIREWORK, 25, 0.8, 1.0, 0.8, 0.2),
                        new VfxSpec(ModParticles.FAE_SPARKLE, 20, 0.8, 1.0, 0.8, 0.15),
                        new VfxSpec(ParticleTypes.HAPPY_VILLAGER, 15, 0.8, 1.0, 0.8, 0.1)
                ),
                CueType.VIGNETTE_PULSE,
                25,
                Intensity.MAJOR
        ));

        // ===== Draconic breaths (MAJOR each) =====
        // Accent bursts only (~15 particles): the dominant VFX path for breaths is the
        // cone fill emitted by ConeBreathAction (~42 particles at 2/step, range 7), so
        // cone + accent lands inside the Major 30-60 band.
        ENTRIES.put(SignatureKey.FIRE_DRAKE_BREATH, new SignatureEntry(
                "message.runic_races.signature.fire_drake.breath",
                ChatFormatting.RED, true,
                List.of(new SfxSpec(SoundEvents.ENDER_DRAGON_GROWL, 0.9f, 1.3f),
                        new SfxSpec(ModSounds.BREATH_FIRE, 0.6f, 1.0f)),
                List.of(new VfxSpec(ParticleTypes.DRAGON_BREATH, 10, 0.8, 0.3, 0.8, 0.3),
                        new VfxSpec(RaceColors.FORGE_EMBER, 5, 0.7, 0.3, 0.7, 0.05)),
                null, 0, Intensity.MAJOR));

        ENTRIES.put(SignatureKey.ICE_DRAKE_BREATH, new SignatureEntry(
                "message.runic_races.signature.ice_drake.breath",
                ChatFormatting.AQUA, true,
                List.of(new SfxSpec(SoundEvents.ENDER_DRAGON_GROWL, 0.9f, 1.5f),
                        new SfxSpec(ModSounds.BREATH_FROST, 0.5f, 1.0f)),
                List.of(new VfxSpec(ParticleTypes.SNOWFLAKE, 10, 0.8, 0.3, 0.8, 0.2),
                        new VfxSpec(RaceColors.GLACIAL_CYAN, 5, 0.7, 0.3, 0.7, 0.05)),
                CueType.FROST_RIME, 20, Intensity.MAJOR));

        ENTRIES.put(SignatureKey.SEA_SERPEN_BREATH, new SignatureEntry(
                "message.runic_races.signature.sea_serpen.breath",
                ChatFormatting.BLUE, true,
                List.of(new SfxSpec(SoundEvents.ENDER_DRAGON_GROWL, 0.9f, 1.1f),
                        new SfxSpec(ModSounds.BREATH_WATER, 0.7f, 1.0f)),
                List.of(new VfxSpec(ParticleTypes.BUBBLE, 10, 0.8, 0.3, 0.8, 0.2),
                        new VfxSpec(ParticleTypes.SPLASH, 5, 0.6, 0.3, 0.6, 0.1)),
                null, 0, Intensity.MAJOR));

        ENTRIES.put(SignatureKey.TERRA_DRAKE_BREATH, new SignatureEntry(
                "message.runic_races.signature.terra_drake.breath",
                ChatFormatting.DARK_GREEN, true,
                List.of(new SfxSpec(SoundEvents.ENDER_DRAGON_GROWL, 0.9f, 0.7f),
                        new SfxSpec(ModSounds.BREATH_EARTH, 0.5f, 1.0f)),
                List.of(new VfxSpec(ParticleTypes.POOF, 10, 0.8, 0.4, 0.8, 0.1),
                        new VfxSpec(ParticleTypes.CRIT, 5, 0.6, 0.3, 0.6, 0.2)),
                CueType.SHAKE, 10, Intensity.MAJOR));

        ENTRIES.put(SignatureKey.VOLT_DRAKE_BREATH, new SignatureEntry(
                "message.runic_races.signature.volt_drake.breath",
                ChatFormatting.YELLOW, true,
                List.of(new SfxSpec(SoundEvents.ENDER_DRAGON_GROWL, 0.9f, 1.6f),
                        new SfxSpec(ModSounds.BREATH_SHOCK, 0.5f, 1.0f)),
                List.of(new VfxSpec(ParticleTypes.ELECTRIC_SPARK, 10, 0.8, 0.3, 0.8, 0.3),
                        new VfxSpec(RaceColors.VOLT_GOLD, 5, 0.7, 0.3, 0.7, 0.1)),
                null, 0, Intensity.MAJOR));

        ENTRIES.put(SignatureKey.WIND_WYRM_BREATH, new SignatureEntry(
                "message.runic_races.signature.wind_wyrm.breath",
                ChatFormatting.WHITE, true,
                List.of(new SfxSpec(SoundEvents.ENDER_DRAGON_GROWL, 0.9f, 1.2f),
                        new SfxSpec(ModSounds.BREATH_WIND, 0.6f, 1.0f)),
                List.of(new VfxSpec(ParticleTypes.CLOUD, 10, 0.9, 0.4, 0.9, 0.3),
                        new VfxSpec(ParticleTypes.DRAGON_BREATH, 5, 0.7, 0.3, 0.7, 0.2)),
                CueType.SHAKE, 15, Intensity.MAJOR));

        // ===== Wing flaps =====
        ENTRIES.put(SignatureKey.SPRITE_WING_FLAP, new SignatureEntry(
                "message.runic_races.signature.sprite.wing_flap",
                ChatFormatting.LIGHT_PURPLE, true,
                List.of(new SfxSpec(ModSounds.WINGS_FLAP_SMALL, 0.2f, 2.0f)),
                List.of(new VfxSpec(ParticleTypes.END_ROD, 12, 0.3, 0.3, 0.3, 0.3),
                        new VfxSpec(ParticleTypes.CLOUD, 10, 0.3, 0.1, 0.3, 0.02)),
                null, 0, Intensity.MINOR));

        ENTRIES.put(SignatureKey.FAERIE_WING_FLAP, new SignatureEntry(
                "message.runic_races.signature.faerie.wing_flap",
                ChatFormatting.LIGHT_PURPLE, true,
                List.of(new SfxSpec(ModSounds.WINGS_FLAP_SMALL, 0.2f, 1.8f)),
                List.of(new VfxSpec(ParticleTypes.END_ROD, 12, 0.3, 0.3, 0.3, 0.3),
                        new VfxSpec(ParticleTypes.FIREWORK, 8, 0.3, 0.1, 0.3, 0.05)),
                null, 0, Intensity.MINOR));

        ENTRIES.put(SignatureKey.AVIAN_WING_FLAP, new SignatureEntry(
                "message.runic_races.signature.avian.wing_flap",
                ChatFormatting.AQUA, true,
                List.of(new SfxSpec(ModSounds.WINGS_FLAP_FEATHER, 0.3f, 1.4f)),
                List.of(new VfxSpec(ParticleTypes.CLOUD, 12, 0.4, 0.2, 0.4, 0.1),
                        new VfxSpec(ParticleTypes.POOF, 8, 0.3, 0.1, 0.3, 0.02)),
                null, 0, Intensity.MINOR));

        ENTRIES.put(SignatureKey.WIND_WYRM_WING_FLAP, new SignatureEntry(
                "message.runic_races.signature.wind_wyrm.wing_flap",
                ChatFormatting.WHITE, true,
                List.of(new SfxSpec(ModSounds.WINGS_FLAP_LARGE, 0.5f, 1.2f)),
                List.of(new VfxSpec(ParticleTypes.CLOUD, 30, 0.5, 0.2, 0.5, 0.2)),
                null, 0, Intensity.MAJOR));

        // ===== Flight Cancel (MINOR) =====
        ENTRIES.put(SignatureKey.FLIGHT_CANCEL, new SignatureEntry(
                "message.runic_races.signature.flight.cancel",
                ChatFormatting.GRAY, false,
                List.of(new SfxSpec(SoundEvents.PHANTOM_FLAP, 0.3f, 0.8f)),
                List.of(),
                null, 0, Intensity.MINOR));

        // =====================================================================
        // v1.4.0 identity pass — every race's active routes through a signature.
        // Family grammar: Human snap / Elven rise-implode / Dwarven ground-burst /
        // Bestial lunge / Faeborne swirl-pop / Undead sink-drain / Draconic exhale.
        // Banner keys reuse the message.runic_races.<race>.<ability> strings the
        // JSON show_banner actions used before the swap.
        // =====================================================================

        // ----- Human: crisp, mundane excellence — sharp and over fast -----
        ENTRIES.put(SignatureKey.PRIMIAN_FORTUNE, new SignatureEntry(
                "message.runic_races.primian.stroke_of_fortune",
                ChatFormatting.GOLD, true,
                List.of(new SfxSpec(SoundEvents.PLAYER_LEVELUP, 0.5f, 1.4f),
                        new SfxSpec(SoundEvents.BELL_BLOCK, 0.4f, 1.6f)),
                List.of(
                        // Golden luck spirals up and a fortune ring snaps outward.
                        new VfxSpec(ParticleTypes.END_ROD, 18, 0.5, 2.2, 0.5, 0.05, SignatureEntry.Shape.HELIX),
                        new VfxSpec(RaceColors.VOLT_GOLD, 12, 1.2, 0.0, 1.2, 0.05, SignatureEntry.Shape.RING),
                        new VfxSpec(ParticleTypes.TOTEM_OF_UNDYING, 15, 0.5, 0.8, 0.5, 0.15)),
                null, 0, Intensity.MAJOR));

        ENTRIES.put(SignatureKey.CELERON_DASH, new SignatureEntry(
                "message.runic_races.celeron.messengers_dash",
                ChatFormatting.YELLOW, true,
                List.of(new SfxSpec(ModSounds.DASH_WHOOSH, 0.7f, 1.0f)),
                List.of(
                        // Burst left AT the launch point — the dash leaves it behind as a trail.
                        new VfxSpec(ParticleTypes.CLOUD, 16, 0.4, 0.3, 0.4, 0.1),
                        new VfxSpec(ModParticles.FEATHER_DOWN, 14, 0.5, 0.6, 0.5, 0.06)),
                CueType.WIND_STREAK, 8, Intensity.MAJOR));

        ENTRIES.put(SignatureKey.MAGI_OVERFLOW, new SignatureEntry(
                "message.runic_races.magi.arcane_overflow",
                ChatFormatting.LIGHT_PURPLE, true,
                List.of(new SfxSpec(SoundEvents.AMETHYST_BLOCK_CHIME, 0.6f, 0.9f),
                        new SfxSpec(SoundEvents.EVOKER_CAST_SPELL, 0.5f, 1.2f)),
                List.of(
                        new VfxSpec(ModParticles.ARCANE_GLINT, 16, 1.1, 0.0, 1.1, 0.06, SignatureEntry.Shape.RING),
                        new VfxSpec(ModParticles.RUNE_GLYPH, 12, 0.6, 2.0, 0.6, 0.04, SignatureEntry.Shape.HELIX),
                        new VfxSpec(RaceColors.ARCANE_AZURE, 12, 0.6, 0.8, 0.6, 0.05)),
                CueType.VIGNETTE_PULSE, 15, Intensity.MAJOR));

        ENTRIES.put(SignatureKey.VALEN_STAND, new SignatureEntry(
                "message.runic_races.valen.unbreakable_stand",
                ChatFormatting.GOLD, true,
                List.of(new SfxSpec(SoundEvents.ANVIL_LAND, 0.4f, 0.9f),
                        new SfxSpec(SoundEvents.SHIELD_BLOCK, 0.7f, 0.8f)),
                List.of(
                        // An iron dome snaps up around the defender.
                        new VfxSpec(RaceColors.IRON_GRAY, 20, 1.3, 0.0, 1.3, 0.1, SignatureEntry.Shape.DOME),
                        new VfxSpec(ParticleTypes.CRIT, 20, 1.4, 0.0, 1.4, 0.08, SignatureEntry.Shape.RING)),
                CueType.SHAKE, 6, Intensity.MAJOR));

        // ----- Elven: jewel-tone arcana — graceful rises and implosions -----
        ENTRIES.put(SignatureKey.HIGH_ELF_REFLEX, new SignatureEntry(
                "message.runic_races.high_elf.arcane_reflex",
                ChatFormatting.LIGHT_PURPLE, true,
                List.of(new SfxSpec(SoundEvents.AMETHYST_CLUSTER_BREAK, 0.6f, 1.2f),
                        new SfxSpec(SoundEvents.EXPERIENCE_ORB_PICKUP, 0.5f, 1.8f)),
                List.of(
                        // Glints rush INWARD — a reflex snapping into focus.
                        new VfxSpec(ModParticles.ARCANE_GLINT, 20, 1.8, 0.0, 1.8, 0.25, SignatureEntry.Shape.RING_IN),
                        new VfxSpec(RaceColors.ARCANE_AZURE, 12, 1.2, 0.0, 1.2, 0.2, SignatureEntry.Shape.RING_IN),
                        new VfxSpec(ParticleTypes.ENCHANT, 10, 0.6, 0.8, 0.6, 0.3)),
                null, 0, Intensity.MAJOR));

        ENTRIES.put(SignatureKey.DARK_ELF_SHADOWMELD, new SignatureEntry(
                "message.runic_races.dark_elf.shadowmeld",
                ChatFormatting.DARK_PURPLE, true,
                List.of(new SfxSpec(SoundEvents.SOUL_ESCAPE, 0.5f, 0.6f),
                        new SfxSpec(SoundEvents.SCULK_CLICKING, 0.5f, 0.8f)),
                List.of(
                        // Shadows pour in and swallow the caster.
                        new VfxSpec(ModParticles.SHADOW_WISP, 18, 1.4, 0.0, 1.4, 0.15, SignatureEntry.Shape.RING_IN),
                        new VfxSpec(ParticleTypes.SQUID_INK, 10, 0.4, 0.6, 0.4, 0.04),
                        new VfxSpec(RaceColors.DUSK_INDIGO, 12, 0.5, 0.8, 0.5, 0.04)),
                CueType.VIGNETTE_PULSE, 20, Intensity.MAJOR));

        ENTRIES.put(SignatureKey.MOON_ELF_VEIL, new SignatureEntry(
                "message.runic_races.moon_elf.moonlit_veil",
                ChatFormatting.AQUA, true,
                List.of(new SfxSpec(SoundEvents.AMETHYST_BLOCK_CHIME, 0.6f, 1.3f),
                        new SfxSpec(SoundEvents.BELL_BLOCK, 0.4f, 1.5f)),
                List.of(
                        new VfxSpec(RaceColors.SILVER_MOON, 16, 1.3, 0.0, 1.3, 0.04, SignatureEntry.Shape.RING),
                        new VfxSpec(ParticleTypes.END_ROD, 18, 0.7, 2.4, 0.7, 0.04, SignatureEntry.Shape.HELIX)),
                CueType.MOON_GLOW, 30, Intensity.MAJOR));

        ENTRIES.put(SignatureKey.BLOOD_ELF_FRENZY, new SignatureEntry(
                "message.runic_races.blood_elf.blood_frenzy",
                ChatFormatting.DARK_RED, true,
                List.of(new SfxSpec(SoundEvents.WARDEN_HEARTBEAT, 0.7f, 1.1f),
                        new SfxSpec(SoundEvents.WITCH_DRINK, 0.5f, 0.8f)),
                List.of(
                        // Crimson spirals up the body as the frenzy takes hold.
                        new VfxSpec(RaceColors.CRIMSON_BLOOD, 20, 0.7, 2.0, 0.7, 0.06, SignatureEntry.Shape.HELIX),
                        new VfxSpec(ParticleTypes.DAMAGE_INDICATOR, 15, 0.5, 0.8, 0.5, 0.1)),
                CueType.HEARTBEAT_FLASH, 20, Intensity.MAJOR));

        ENTRIES.put(SignatureKey.ICE_ELF_FROSTBIND, new SignatureEntry(
                "message.runic_races.ice_elf.frostbind",
                ChatFormatting.AQUA, true,
                List.of(new SfxSpec(SoundEvents.GLASS_BREAK, 0.5f, 1.4f),
                        new SfxSpec(SoundEvents.POWDER_SNOW_BREAK, 0.6f, 0.8f)),
                List.of(
                        // Rime creeps outward along the ground — distinct from Ice Drake's cone.
                        new VfxSpec(ModParticles.FROST_MOTE, 22, 2.0, 0.0, 2.0, 0.12, SignatureEntry.Shape.RING),
                        new VfxSpec(RaceColors.GLACIAL_CYAN, 12, 1.2, 0.0, 1.2, 0.1, SignatureEntry.Shape.RING),
                        new VfxSpec(ParticleTypes.SNOWFLAKE, 10, 0.6, 0.6, 0.6, 0.05)),
                CueType.FROST_RIME, 20, Intensity.MAJOR));

        // ----- Dwarven: ground-hugging bursts of stone and craft -----
        ENTRIES.put(SignatureKey.DEEP_ONE_TREMOR, new SignatureEntry(
                "message.runic_races.deep_one.tremorsense",
                ChatFormatting.GRAY, true,
                List.of(new SfxSpec(ModSounds.TREMOR_PULSE, 0.8f, 1.0f)),
                List.of(
                        // A seismic pulse rolls outward along the floor.
                        new VfxSpec(ParticleTypes.POOF, 24, 2.2, 0.0, 2.2, 0.15, SignatureEntry.Shape.RING),
                        new VfxSpec(RaceColors.VOLT_GOLD, 10, 1.0, 0.0, 1.0, 0.05, SignatureEntry.Shape.RING)),
                CueType.SHAKE, 5, Intensity.MAJOR));

        ENTRIES.put(SignatureKey.FROST_ONE_RESOLVE, new SignatureEntry(
                "message.runic_races.frost_one.glacial_resolve",
                ChatFormatting.AQUA, true,
                List.of(new SfxSpec(SoundEvents.GLASS_PLACE, 0.6f, 0.7f),
                        new SfxSpec(SoundEvents.SHIELD_BLOCK, 0.6f, 0.9f)),
                List.of(
                        // A glacial shell forms around the dwarf.
                        new VfxSpec(RaceColors.GLACIAL_CYAN, 20, 1.4, 0.0, 1.4, 0.08, SignatureEntry.Shape.DOME),
                        new VfxSpec(ModParticles.FROST_MOTE, 16, 0.7, 1.0, 0.7, 0.04),
                        new VfxSpec(ParticleTypes.SNOWFLAKE, 10, 0.6, 0.8, 0.6, 0.05)),
                CueType.FROST_RIME, 15, Intensity.MAJOR));

        ENTRIES.put(SignatureKey.IRON_ONE_SHIELD_WALL, new SignatureEntry(
                "message.runic_races.iron_one.shield_wall",
                ChatFormatting.GRAY, true,
                List.of(new SfxSpec(ModSounds.SHIELD_BRACE, 0.8f, 1.0f)),
                List.of(
                        new VfxSpec(ParticleTypes.ENCHANTED_HIT, 20, 1.4, 0.0, 1.4, 0.05, SignatureEntry.Shape.DOME),
                        new VfxSpec(RaceColors.IRON_GRAY, 16, 1.2, 0.0, 1.2, 0.04, SignatureEntry.Shape.DOME)),
                CueType.SHAKE, 4, Intensity.MAJOR));

        ENTRIES.put(SignatureKey.SKY_ONE_LEAP, new SignatureEntry(
                "message.runic_races.sky_one.mountain_leap",
                ChatFormatting.WHITE, true,
                List.of(new SfxSpec(SoundEvents.GOAT_LONG_JUMP, 0.7f, 1.0f),
                        new SfxSpec(ModSounds.DASH_WHOOSH, 0.5f, 0.8f)),
                List.of(
                        // Launch ring at the feet plus a rising dust column along the ascent.
                        new VfxSpec(ParticleTypes.CLOUD, 20, 1.2, 0.0, 1.2, 0.15, SignatureEntry.Shape.RING),
                        new VfxSpec(ParticleTypes.POOF, 12, 0.3, 1.5, 0.3, 0.08)),
                CueType.WIND_STREAK, 10, Intensity.MAJOR));

        // ----- Bestial: organic, directional, physical -----
        ENTRIES.put(SignatureKey.ARACHNID_WEB_SNARE, new SignatureEntry(
                "message.runic_races.arachnid.web_snare",
                ChatFormatting.WHITE, true,
                List.of(new SfxSpec(ModSounds.WEB_SNARE, 0.8f, 1.0f),
                        new SfxSpec(SoundEvents.SPIDER_AMBIENT, 0.5f, 1.2f)),
                List.of(
                        // Eight silk spokes shoot along the ground and a strand ring marks the snare edge.
                        new VfxSpec(ModParticles.WEB_STRAND, 32, 5.0, 0.0, 5.0, 0.02, SignatureEntry.Shape.SPOKES),
                        new VfxSpec(ModParticles.WEB_STRAND, 16, 5.0, 0.0, 5.0, 0.01, SignatureEntry.Shape.RING),
                        new VfxSpec(ParticleTypes.CRIT, 8, 0.5, 0.4, 0.5, 0.15)),
                null, 0, Intensity.MAJOR));

        ENTRIES.put(SignatureKey.AVIAN_WIND_BURST, new SignatureEntry(
                "message.runic_races.avian.wind_burst",
                ChatFormatting.AQUA, true,
                List.of(new SfxSpec(SoundEvents.PHANTOM_FLAP, 0.7f, 1.2f),
                        new SfxSpec(ModSounds.DASH_WHOOSH, 0.5f, 1.3f)),
                List.of(
                        new VfxSpec(ParticleTypes.CLOUD, 20, 1.5, 0.0, 1.5, 0.2, SignatureEntry.Shape.RING),
                        new VfxSpec(ModParticles.FEATHER_DOWN, 16, 0.6, 1.5, 0.6, 0.15)),
                CueType.WIND_STREAK, 8, Intensity.MAJOR));

        ENTRIES.put(SignatureKey.CANINE_HOWL, new SignatureEntry(
                "message.runic_races.canine.howl_of_the_pack",
                ChatFormatting.GREEN, true,
                List.of(new SfxSpec(ModSounds.HOWL_PACK, 0.9f, 1.0f)),
                List.of(
                        // Two concentric sound-rings roll out from the howl.
                        new VfxSpec(ParticleTypes.POOF, 16, 1.5, 0.0, 1.5, 0.25, SignatureEntry.Shape.RING),
                        new VfxSpec(ParticleTypes.POOF, 16, 2.5, 0.0, 2.5, 0.25, SignatureEntry.Shape.RING),
                        new VfxSpec(ParticleTypes.NOTE, 8, 0.8, 1.0, 0.8, 0.1)),
                null, 0, Intensity.MAJOR));

        ENTRIES.put(SignatureKey.FELINE_POUNCE, new SignatureEntry(
                "message.runic_races.feline.pounce",
                ChatFormatting.GOLD, true,
                List.of(new SfxSpec(SoundEvents.CAT_HISS, 0.5f, 1.1f),
                        new SfxSpec(SoundEvents.PLAYER_ATTACK_SWEEP, 0.6f, 1.3f)),
                List.of(
                        new VfxSpec(ParticleTypes.SWEEP_ATTACK, 6, 0.4, 0.3, 0.4, 0.1),
                        new VfxSpec(ParticleTypes.CRIT, 10, 0.4, 0.4, 0.4, 0.2)),
                null, 0, Intensity.MINOR));

        ENTRIES.put(SignatureKey.KITSUNE_FOXFIRE, new SignatureEntry(
                "message.runic_races.kitsune.foxfire_illusion",
                ChatFormatting.LIGHT_PURPLE, true,
                List.of(new SfxSpec(ModSounds.FOXFIRE_IGNITE, 0.8f, 1.0f)),
                List.of(
                        // Spirit flames orbit the kitsune while the illusion takes shape.
                        new VfxSpec(ModParticles.FOXFIRE, 15, 1.0, 0.0, 1.0, 0.18, SignatureEntry.Shape.RING_ORBIT),
                        new VfxSpec(ParticleTypes.SOUL_FIRE_FLAME, 20, 0.6, 0.8, 0.6, 0.04),
                        new VfxSpec(ParticleTypes.ENCHANT, 10, 0.7, 0.9, 0.7, 0.3)),
                null, 0, Intensity.MAJOR));

        ENTRIES.put(SignatureKey.SERPEN_SHED, new SignatureEntry(
                "message.runic_races.serpen.shed_skin",
                ChatFormatting.GREEN, true,
                List.of(new SfxSpec(SoundEvents.SLIME_SQUISH, 0.6f, 0.8f),
                        new SfxSpec(SoundEvents.CAT_HISS, 0.5f, 0.5f)),
                List.of(
                        // The molt stays AT the cast point while the serpen slips free.
                        new VfxSpec(ParticleTypes.ITEM_SLIME, 16, 0.4, 0.6, 0.4, 0.03),
                        new VfxSpec(RaceColors.VERDANT_GREEN, 14, 1.0, 0.0, 1.0, 0.03, SignatureEntry.Shape.RING)),
                CueType.WIND_STREAK, 6, Intensity.MAJOR));

        // ----- Faeborne: playful swirls, blink-outs, pastel pops -----
        ENTRIES.put(SignatureKey.CHANGELING_MIRROR, new SignatureEntry(
                "message.runic_races.changeling.mirror_shift",
                ChatFormatting.LIGHT_PURPLE, true,
                List.of(new SfxSpec(ModSounds.MIRROR_SHIFT, 0.8f, 1.0f)),
                List.of(
                        // Portal shards implode into the new face; the freeze-frame is the camera flash.
                        new VfxSpec(ParticleTypes.PORTAL, 16, 1.3, 0.0, 1.3, 0.25, SignatureEntry.Shape.RING_IN),
                        new VfxSpec(ParticleTypes.PORTAL, 10, 0.4, 0.8, 0.4, 0.3),
                        new VfxSpec(ModParticles.FAE_SPARKLE, 10, 0.5, 0.9, 0.5, 0.08)),
                CueType.FREEZE_FRAME, 6, Intensity.MAJOR));

        ENTRIES.put(SignatureKey.DRYAD_BLOOM, new SignatureEntry(
                "message.runic_races.dryad.verdant_bloom",
                ChatFormatting.GREEN, true,
                List.of(new SfxSpec(ModSounds.BLOOM_VERDANT, 0.9f, 1.0f)),
                List.of(
                        // Petals and life roll outward through the grove.
                        new VfxSpec(ModParticles.LEAF_PETAL, 20, 1.8, 0.0, 1.8, 0.1, SignatureEntry.Shape.RING),
                        new VfxSpec(ParticleTypes.HAPPY_VILLAGER, 15, 2.2, 0.0, 2.2, 0.08, SignatureEntry.Shape.RING),
                        new VfxSpec(RaceColors.VERDANT_GREEN, 10, 0.6, 0.8, 0.6, 0.04)),
                null, 0, Intensity.MAJOR));

        ENTRIES.put(SignatureKey.SPRITE_PHASE, new SignatureEntry(
                "message.runic_races.sprite.phase_shift",
                ChatFormatting.LIGHT_PURPLE, true,
                List.of(new SfxSpec(SoundEvents.ENDERMAN_TELEPORT, 0.4f, 1.6f),
                        new SfxSpec(SoundEvents.BELL_BLOCK, 0.4f, 2.0f)),
                List.of(
                        // Blink-out: sparkles collapse inward, then a bright re-appear burst.
                        new VfxSpec(ModParticles.FAE_SPARKLE, 22, 1.3, 0.0, 1.3, 0.3, SignatureEntry.Shape.RING_IN),
                        new VfxSpec(ParticleTypes.END_ROD, 14, 0.4, 0.6, 0.4, 0.2)),
                null, 0, Intensity.MAJOR));

        ENTRIES.put(SignatureKey.NYMPH_CHARM, new SignatureEntry(
                "message.runic_races.nymph.sirens_charm",
                ChatFormatting.AQUA, true,
                List.of(new SfxSpec(ModSounds.CHARM_SONG, 0.9f, 1.0f)),
                List.of(
                        // The song spirals up while a tide ripple spreads at the feet.
                        new VfxSpec(ParticleTypes.NOTE, 14, 0.8, 2.2, 0.8, 0.05, SignatureEntry.Shape.HELIX),
                        new VfxSpec(RaceColors.TIDAL_TEAL, 14, 1.5, 0.0, 1.5, 0.1, SignatureEntry.Shape.RING),
                        new VfxSpec(ParticleTypes.HEART, 10, 0.6, 0.9, 0.6, 0.08)),
                null, 0, Intensity.MAJOR));

        // ----- Undead: everything sinks, lingers, or drains INTO the caster -----
        ENTRIES.put(SignatureKey.ZOMBIE_HUNGER, new SignatureEntry(
                "message.runic_races.zombie.undying_hunger",
                ChatFormatting.DARK_GREEN, true,
                List.of(new SfxSpec(SoundEvents.ZOMBIE_AMBIENT, 0.6f, 0.8f),
                        new SfxSpec(SoundEvents.GENERIC_EAT, 0.6f, 0.7f)),
                List.of(
                        // Life is pulled INTO the zombie — feeding, not exploding.
                        new VfxSpec(ParticleTypes.SCULK_SOUL, 18, 1.6, 0.0, 1.6, 0.2, SignatureEntry.Shape.RING_IN),
                        new VfxSpec(ParticleTypes.CRIMSON_SPORE, 14, 1.2, 0.0, 1.2, 0.15, SignatureEntry.Shape.RING_IN),
                        new VfxSpec(ParticleTypes.ASH, 8, 0.5, 0.7, 0.5, 0.03)),
                null, 0, Intensity.MAJOR));

        ENTRIES.put(SignatureKey.SKELETON_CONSCRIPT, new SignatureEntry(
                "message.runic_races.skeleton.conscript_the_dead",
                ChatFormatting.WHITE, true,
                List.of(new SfxSpec(SoundEvents.SKELETON_AMBIENT, 0.6f, 0.8f),
                        new SfxSpec(SoundEvents.BONE_BLOCK_BREAK, 0.7f, 0.7f)),
                List.of(
                        // Grave dirt erupts, a soul column rises, and a summoning circle flares.
                        new VfxSpec(ParticleTypes.POOF, 16, 0.6, 0.2, 0.6, 0.1),
                        new VfxSpec(RaceColors.SOUL_VIOLET, 14, 0.5, 2.2, 0.5, 0.05, SignatureEntry.Shape.HELIX),
                        new VfxSpec(ModParticles.RUNE_GLYPH, 10, 1.5, 0.0, 1.5, 0.02, SignatureEntry.Shape.RING)),
                null, 0, Intensity.MAJOR));

        ENTRIES.put(SignatureKey.REAPER_HARVEST, new SignatureEntry(
                "message.runic_races.reaper.soul_harvest",
                ChatFormatting.DARK_PURPLE, true,
                List.of(new SfxSpec(ModSounds.HARVEST_SOUL, 0.9f, 1.0f)),
                List.of(
                        // Souls stream inward from the harvest radius to the reaper.
                        new VfxSpec(ParticleTypes.SOUL, 24, 2.2, 0.0, 2.2, 0.25, SignatureEntry.Shape.RING_IN),
                        new VfxSpec(ParticleTypes.SCULK_SOUL, 14, 0.7, 0.9, 0.7, 0.06),
                        new VfxSpec(ModParticles.SOUL_WISP, 12, 0.7, 2.0, 0.7, 0.05, SignatureEntry.Shape.HELIX)),
                CueType.VIGNETTE_PULSE, 20, Intensity.MAJOR));
    }

    public static SignatureEntry get(SignatureKey key) {
        return ENTRIES.get(key);
    }

    private SignatureRegistry() {}
}
