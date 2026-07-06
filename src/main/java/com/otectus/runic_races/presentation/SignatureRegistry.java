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
                // The soul slips loose; the wither-hush lingers after the body fades.
                List.of(
                        new SfxSpec(SoundEvents.SOUL_ESCAPE, 0.5f, 0.7f),
                        new SfxSpec(SoundEvents.WITHER_AMBIENT, 0.25f, 1.6f).delayed(8)
                ),
                List.of(
                        // Sink-drain: wisps collapse inward while shadows pool downward,
                        // then the last soul-flames thin out where the wraith stood.
                        new VfxSpec(ModParticles.SOUL_WISP, 16, 1.2, 0.0, 1.2, 0.2, SignatureEntry.Shape.RING_IN),
                        new VfxSpec(ModParticles.SHADOW_WISP, 14, 0.5, 0.9, 0.5, 0.04),
                        new VfxSpec(ParticleTypes.SOUL_FIRE_FLAME, 12, 0.5, 1.0, 0.5, 0.03).delayed(8)
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
                // The roar builds the wrath; the detonation releases it.
                List.of(
                        new SfxSpec(SoundEvents.RAVAGER_ROAR, 0.8f, 0.7f),
                        new SfxSpec(SoundEvents.GENERIC_EXPLODE, 0.5f, 1.0f).delayed(6)
                ),
                List.of(
                        // Soul-fire + embers: infernal palette, distinct from Fire Drake's flame cone.
                        // Embers ring out with the roar; the soul-fire dome erupts on the blast.
                        new VfxSpec(ModParticles.EMBER_SCALE, 16, 1.2, 0.0, 1.2, 0.1, SignatureEntry.Shape.RING),
                        new VfxSpec(ParticleTypes.SOUL_FIRE_FLAME, 22, 1.1, 0.0, 1.1, 0.1, SignatureEntry.Shape.DOME).delayed(6),
                        new VfxSpec(ParticleTypes.LAVA, 8, 0.5, 0.3, 0.5, 0.02).delayed(6)
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
                // The anvil rings, then the blessing takes (enchant shimmer).
                List.of(
                        new SfxSpec(SoundEvents.ANVIL_LAND, 0.35f, 1.3f),
                        new SfxSpec(SoundEvents.ENCHANTMENT_TABLE_USE, 0.5f, 1.1f).delayed(5)
                ),
                List.of(
                        // Rune circle on the workbench floor, then forge-fire fountains up through it.
                        new VfxSpec(ModParticles.RUNE_GLYPH, 12, 1.0, 0.0, 1.0, 0.02, SignatureEntry.Shape.RING),
                        new VfxSpec(ParticleTypes.FLAME, 18, 0.4, 1.6, 0.4, 0.12, SignatureEntry.Shape.BURST_UP).delayed(5),
                        new VfxSpec(RaceColors.FORGE_EMBER, 14, 1.0, 0.0, 1.0, 0.06, SignatureEntry.Shape.DOME).delayed(5),
                        new VfxSpec(ParticleTypes.CRIT, 8, 0.4, 0.6, 0.4, 0.2).delayed(5)
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
                // The ward is inscribed, then hums to life.
                List.of(
                        new SfxSpec(ModSounds.WARD_ACTIVATE, 0.6f, 1.0f),
                        new SfxSpec(SoundEvents.ENCHANTMENT_TABLE_USE, 0.5f, 0.8f).delayed(8)
                ),
                List.of(
                        // The full ward sigil is drawn on the ground — glyph circle plus radial
                        // strokes — then enchant-light settles down over the warded zone.
                        new VfxSpec(ModParticles.RUNE_GLYPH, 18, 2.0, 0.6, 2.0, 0.02, SignatureEntry.Shape.RING),
                        new VfxSpec(ModParticles.RUNE_GLYPH, 12, 2.0, 0.0, 2.0, 0.02, SignatureEntry.Shape.SPOKES),
                        new VfxSpec(ParticleTypes.ENCHANT, 24, 1.4, 0.0, 1.4, 0.05, SignatureEntry.Shape.DOME).delayed(8)
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
                // The glamour is woven (enchant), then sealed with a bell-chime pop.
                List.of(
                        new SfxSpec(SoundEvents.ENCHANTMENT_TABLE_USE, 0.6f, 1.5f),
                        new SfxSpec(SoundEvents.BELL_BLOCK, 0.5f, 1.8f).delayed(6)
                ),
                List.of(
                        // Sparkles swirl around the bargain being struck, then it pops in pastel.
                        new VfxSpec(ModParticles.FAE_SPARKLE, 20, 1.0, 0.0, 1.0, 0.15, SignatureEntry.Shape.RING_ORBIT),
                        new VfxSpec(ParticleTypes.FIREWORK, 20, 0.8, 1.0, 0.8, 0.2).delayed(6),
                        new VfxSpec(ParticleTypes.HAPPY_VILLAGER, 12, 0.8, 1.0, 0.8, 0.1).delayed(6)
                ),
                CueType.VIGNETTE_PULSE,
                25,
                Intensity.MAJOR
        ));

        // ===== Draconic breaths (MAJOR each) =====
        // Accent bursts only (~15-20 particles): the dominant VFX path for breaths is
        // the cone fill emitted by ConeBreathAction (~42 particles at 2/step, range 7),
        // so cone + accent lands inside the Major 30-60 band. The cone fires the same
        // tick as the signature (both sit in the JSON action list), so the "inhale" is
        // a sound layer under the growl, not a delayed exhale; settle accents trail at
        // t6. Every breath carries a screen cue matched to its element.
        ENTRIES.put(SignatureKey.FIRE_DRAKE_BREATH, new SignatureEntry(
                "message.runic_races.signature.fire_drake.breath",
                ChatFormatting.RED, true,
                List.of(new SfxSpec(ModSounds.BREATH_INHALE, 0.8f, 1.0f),
                        new SfxSpec(SoundEvents.ENDER_DRAGON_GROWL, 0.9f, 1.3f),
                        new SfxSpec(ModSounds.BREATH_FIRE, 0.6f, 1.0f)),
                List.of(
                        // Actual fire — flame and embers, no purple vanilla dragon-breath.
                        new VfxSpec(ParticleTypes.FLAME, 8, 0.8, 0.0, 0.8, 0.2, SignatureEntry.Shape.RING_IN),
                        new VfxSpec(ModParticles.EMBER_SCALE, 7, 0.7, 0.4, 0.7, 0.05).delayed(6),
                        new VfxSpec(RaceColors.FORGE_EMBER, 5, 0.7, 0.3, 0.7, 0.05).delayed(6)),
                CueType.HEAT_SHIMMER, 15, Intensity.MAJOR));

        ENTRIES.put(SignatureKey.ICE_DRAKE_BREATH, new SignatureEntry(
                "message.runic_races.signature.ice_drake.breath",
                ChatFormatting.AQUA, true,
                List.of(new SfxSpec(ModSounds.BREATH_INHALE, 0.8f, 1.2f),
                        new SfxSpec(SoundEvents.ENDER_DRAGON_GROWL, 0.9f, 1.5f),
                        new SfxSpec(ModSounds.BREATH_FROST, 0.5f, 1.0f)),
                List.of(new VfxSpec(ParticleTypes.SNOWFLAKE, 10, 0.8, 0.3, 0.8, 0.2),
                        new VfxSpec(RaceColors.GLACIAL_CYAN, 5, 0.7, 0.3, 0.7, 0.05).delayed(6)),
                CueType.FROST_RIME, 20, Intensity.MAJOR));

        ENTRIES.put(SignatureKey.SEA_SERPEN_BREATH, new SignatureEntry(
                "message.runic_races.signature.sea_serpen.breath",
                ChatFormatting.BLUE, true,
                List.of(new SfxSpec(ModSounds.BREATH_INHALE, 0.8f, 0.9f),
                        new SfxSpec(SoundEvents.ENDER_DRAGON_GROWL, 0.9f, 1.1f),
                        new SfxSpec(ModSounds.BREATH_WATER, 0.7f, 1.0f)),
                List.of(new VfxSpec(ParticleTypes.BUBBLE, 10, 0.8, 0.3, 0.8, 0.2),
                        new VfxSpec(ParticleTypes.SPLASH, 5, 0.6, 0.3, 0.6, 0.1),
                        // Tidal teal accent — sea_serpen's palette color, matching its HUD tint.
                        new VfxSpec(RaceColors.TIDAL_TEAL, 5, 0.7, 0.3, 0.7, 0.05).delayed(6)),
                CueType.WIND_STREAK, 12, Intensity.MAJOR));

        ENTRIES.put(SignatureKey.TERRA_DRAKE_BREATH, new SignatureEntry(
                "message.runic_races.signature.terra_drake.breath",
                ChatFormatting.DARK_GREEN, true,
                List.of(new SfxSpec(ModSounds.BREATH_INHALE, 0.8f, 0.7f),
                        new SfxSpec(SoundEvents.ENDER_DRAGON_GROWL, 0.9f, 0.7f),
                        new SfxSpec(ModSounds.BREATH_EARTH, 0.5f, 1.0f)),
                List.of(new VfxSpec(ParticleTypes.POOF, 10, 0.8, 0.4, 0.8, 0.1),
                        // Real stone shards instead of generic crit sparks.
                        new VfxSpec(ModParticles.ROCK_CHIP, 8, 0.6, 0.4, 0.6, 0.15).delayed(6)),
                CueType.SHAKE, 10, Intensity.MAJOR));

        ENTRIES.put(SignatureKey.VOLT_DRAKE_BREATH, new SignatureEntry(
                "message.runic_races.signature.volt_drake.breath",
                ChatFormatting.YELLOW, true,
                List.of(new SfxSpec(ModSounds.BREATH_INHALE, 0.8f, 1.4f),
                        new SfxSpec(SoundEvents.ENDER_DRAGON_GROWL, 0.9f, 1.6f),
                        new SfxSpec(ModSounds.BREATH_SHOCK, 0.5f, 1.0f)),
                List.of(new VfxSpec(ParticleTypes.ELECTRIC_SPARK, 10, 0.8, 0.3, 0.8, 0.3),
                        new VfxSpec(RaceColors.VOLT_GOLD, 5, 0.7, 0.3, 0.7, 0.1).delayed(6)),
                // A 4-tick white flash — the lightning strobe.
                CueType.FREEZE_FRAME, 4, Intensity.MAJOR));

        ENTRIES.put(SignatureKey.WIND_WYRM_BREATH, new SignatureEntry(
                "message.runic_races.signature.wind_wyrm.breath",
                ChatFormatting.WHITE, true,
                List.of(new SfxSpec(ModSounds.BREATH_INHALE, 0.9f, 1.1f),
                        new SfxSpec(SoundEvents.ENDER_DRAGON_GROWL, 0.9f, 1.2f),
                        new SfxSpec(ModSounds.BREATH_WIND, 0.6f, 1.0f)),
                List.of(new VfxSpec(ParticleTypes.CLOUD, 10, 0.9, 0.4, 0.9, 0.3),
                        // Wind streaks race down the gale instead of borrowed purple dragon-breath.
                        new VfxSpec(ModParticles.GALE_STREAK, 8, 5.0, 1.0, 0.0, 0.4, SignatureEntry.Shape.CONE)),
                // Streaks + FOV kick suit the self-launch better than a camera shake.
                CueType.WIND_STREAK, 12, Intensity.MAJOR));

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
                // Fae dust off the wings — distinct from the sprite's end-rod shimmer.
                List.of(new VfxSpec(ModParticles.FAE_SPARKLE, 12, 0.3, 0.3, 0.3, 0.3),
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

        // ----- Human: crisp, mundane excellence — sharp and over fast (beats ≤ 10 ticks) -----
        ENTRIES.put(SignatureKey.PRIMIAN_FORTUNE, new SignatureEntry(
                "message.runic_races.primian.stroke_of_fortune",
                ChatFormatting.GOLD, true,
                // The coin flips (bell), then fortune lands (levelup).
                List.of(new SfxSpec(SoundEvents.BELL_BLOCK, 0.4f, 1.6f),
                        new SfxSpec(SoundEvents.PLAYER_LEVELUP, 0.5f, 1.4f).delayed(4)),
                List.of(
                        // Gold snaps in as the flip resolves, then luck spirals up and bursts.
                        new VfxSpec(RaceColors.VOLT_GOLD, 12, 1.2, 0.0, 1.2, 0.2, SignatureEntry.Shape.RING_IN),
                        new VfxSpec(ParticleTypes.END_ROD, 18, 0.5, 2.2, 0.5, 0.05, SignatureEntry.Shape.HELIX).delayed(4),
                        new VfxSpec(ParticleTypes.TOTEM_OF_UNDYING, 15, 0.5, 0.8, 0.5, 0.15).delayed(4)),
                null, 0, Intensity.MAJOR));

        ENTRIES.put(SignatureKey.CELERON_DASH, new SignatureEntry(
                "message.runic_races.celeron.messengers_dash",
                ChatFormatting.YELLOW, true,
                List.of(new SfxSpec(ModSounds.DASH_WHOOSH, 0.7f, 1.0f)),
                List.of(
                        // Launch ring snaps at the take-off point; the delayed feathers track the
                        // sprinting caster, shedding a trail along the actual dash path.
                        new VfxSpec(ParticleTypes.CLOUD, 16, 0.9, 0.0, 0.9, 0.15, SignatureEntry.Shape.RING),
                        new VfxSpec(ModParticles.FEATHER_DOWN, 8, 0.4, 0.5, 0.4, 0.05).delayed(2),
                        new VfxSpec(ModParticles.FEATHER_DOWN, 8, 0.4, 0.5, 0.4, 0.05).delayed(5)),
                CueType.WIND_STREAK, 8, Intensity.MAJOR));

        ENTRIES.put(SignatureKey.MAGI_OVERFLOW, new SignatureEntry(
                "message.runic_races.magi.arcane_overflow",
                ChatFormatting.LIGHT_PURPLE, true,
                // The circle is drawn (chime), then the vessel overloads (cast).
                List.of(new SfxSpec(SoundEvents.AMETHYST_BLOCK_CHIME, 0.6f, 0.9f),
                        new SfxSpec(SoundEvents.EVOKER_CAST_SPELL, 0.5f, 1.2f).delayed(5)),
                List.of(
                        // Rune circle materializes first, then the stored mana bursts outward as the nova.
                        new VfxSpec(ModParticles.RUNE_GLYPH, 12, 1.1, 0.0, 1.1, 0.02, SignatureEntry.Shape.RING),
                        new VfxSpec(ModParticles.ARCANE_GLINT, 16, 1.1, 0.0, 1.1, 0.3, SignatureEntry.Shape.RING).delayed(5),
                        new VfxSpec(RaceColors.ARCANE_AZURE, 12, 0.6, 0.8, 0.6, 0.05).delayed(5)),
                CueType.VIGNETTE_PULSE, 15, Intensity.MAJOR));

        ENTRIES.put(SignatureKey.VALEN_STAND, new SignatureEntry(
                "message.runic_races.valen.unbreakable_stand",
                ChatFormatting.GOLD, true,
                // Brace first, then the anvil-clang of a stance that won't move.
                List.of(new SfxSpec(ModSounds.SHIELD_BRACE, 0.6f, 0.9f),
                        new SfxSpec(SoundEvents.ANVIL_LAND, 0.4f, 0.9f).delayed(4)),
                List.of(
                        // An iron dome snaps up; the ground cracks and kicks stone under the planted feet.
                        new VfxSpec(RaceColors.IRON_GRAY, 20, 1.3, 0.0, 1.3, 0.1, SignatureEntry.Shape.DOME),
                        new VfxSpec(RaceColors.IRON_GRAY, 12, 1.6, 0.0, 1.6, 0.05, SignatureEntry.Shape.SPOKES).delayed(4),
                        new VfxSpec(ModParticles.ROCK_CHIP, 10, 0.6, 0.4, 0.6, 0.15).delayed(4)),
                CueType.SHAKE, 6, Intensity.MAJOR));

        // ----- Elven: jewel-tone arcana — graceful rises and implosions -----
        ENTRIES.put(SignatureKey.HIGH_ELF_REFLEX, new SignatureEntry(
                "message.runic_races.high_elf.arcane_reflex",
                ChatFormatting.LIGHT_PURPLE, true,
                List.of(new SfxSpec(SoundEvents.AMETHYST_CLUSTER_BREAK, 0.6f, 1.2f),
                        new SfxSpec(SoundEvents.EXPERIENCE_ORB_PICKUP, 0.5f, 1.8f).delayed(6)),
                List.of(
                        // Glints rush INWARD — a reflex snapping into focus — then pop at the chest.
                        new VfxSpec(ModParticles.ARCANE_GLINT, 20, 1.8, 0.0, 1.8, 0.25, SignatureEntry.Shape.RING_IN),
                        new VfxSpec(RaceColors.ARCANE_AZURE, 12, 1.2, 0.0, 1.2, 0.2, SignatureEntry.Shape.RING_IN),
                        new VfxSpec(ParticleTypes.ENCHANT, 10, 0.4, 0.6, 0.4, 0.3).delayed(6)),
                null, 0, Intensity.MAJOR));

        ENTRIES.put(SignatureKey.DARK_ELF_SHADOWMELD, new SignatureEntry(
                "message.runic_races.dark_elf.shadowmeld",
                ChatFormatting.DARK_PURPLE, true,
                // Shadows gather first; the sculk-click is the soft "gone" beat.
                List.of(new SfxSpec(SoundEvents.SOUL_ESCAPE, 0.5f, 0.6f),
                        new SfxSpec(SoundEvents.SCULK_CLICKING, 0.5f, 0.8f).delayed(8)),
                List.of(
                        // Shadows pour in and swallow the caster; an ink puff marks where they stood.
                        new VfxSpec(ModParticles.SHADOW_WISP, 18, 1.4, 0.0, 1.4, 0.15, SignatureEntry.Shape.RING_IN),
                        new VfxSpec(RaceColors.DUSK_INDIGO, 12, 0.5, 0.8, 0.5, 0.04),
                        new VfxSpec(ParticleTypes.SQUID_INK, 10, 0.4, 0.6, 0.4, 0.04).delayed(8)),
                CueType.VIGNETTE_PULSE, 20, Intensity.MAJOR));

        ENTRIES.put(SignatureKey.MOON_ELF_VEIL, new SignatureEntry(
                "message.runic_races.moon_elf.moonlit_veil",
                ChatFormatting.AQUA, true,
                List.of(new SfxSpec(ModSounds.MOON_VEIL, 0.7f, 1.0f)),
                List.of(
                        // Moonlight climbs the body, then the veil settles as drifting slivers.
                        new VfxSpec(ParticleTypes.END_ROD, 14, 0.7, 2.4, 0.7, 0.04, SignatureEntry.Shape.HELIX),
                        new VfxSpec(RaceColors.SILVER_MOON, 8, 1.3, 0.0, 1.3, 0.04, SignatureEntry.Shape.RING),
                        new VfxSpec(ModParticles.MOON_SLIVER, 16, 1.0, 0.0, 1.0, 0.02, SignatureEntry.Shape.DOME).delayed(8)),
                CueType.MOON_GLOW, 30, Intensity.MAJOR));

        ENTRIES.put(SignatureKey.BLOOD_ELF_FRENZY, new SignatureEntry(
                "message.runic_races.blood_elf.blood_frenzy",
                ChatFormatting.DARK_RED, true,
                // Two heartbeats: the draw, then the eruption.
                List.of(new SfxSpec(SoundEvents.WARDEN_HEARTBEAT, 0.7f, 1.1f),
                        new SfxSpec(SoundEvents.WARDEN_HEARTBEAT, 0.8f, 1.3f).delayed(6),
                        new SfxSpec(SoundEvents.WITCH_DRINK, 0.5f, 0.8f).delayed(6)),
                List.of(
                        // Blood is drawn in on the first beat, then erupts up the body on the second.
                        new VfxSpec(RaceColors.CRIMSON_BLOOD, 14, 1.2, 0.0, 1.2, 0.25, SignatureEntry.Shape.RING_IN),
                        new VfxSpec(RaceColors.CRIMSON_BLOOD, 20, 0.7, 2.0, 0.7, 0.06, SignatureEntry.Shape.HELIX).delayed(6),
                        new VfxSpec(ParticleTypes.DAMAGE_INDICATOR, 8, 0.5, 0.8, 0.5, 0.1).delayed(6)),
                CueType.HEARTBEAT_FLASH, 20, Intensity.MAJOR));

        ENTRIES.put(SignatureKey.ICE_ELF_FROSTBIND, new SignatureEntry(
                "message.runic_races.ice_elf.frostbind",
                ChatFormatting.AQUA, true,
                // The cold gathers silently, then shatters outward.
                List.of(new SfxSpec(SoundEvents.POWDER_SNOW_BREAK, 0.5f, 0.7f),
                        new SfxSpec(SoundEvents.GLASS_BREAK, 0.5f, 1.4f).delayed(6)),
                List.of(
                        // Elven implode into the caster, then the nova the ability actually is:
                        // rime bursting outward along the ground — distinct from Ice Drake's cone.
                        new VfxSpec(ModParticles.FROST_MOTE, 14, 1.4, 0.0, 1.4, 0.2, SignatureEntry.Shape.RING_IN),
                        new VfxSpec(RaceColors.GLACIAL_CYAN, 16, 2.0, 0.0, 2.0, 0.15, SignatureEntry.Shape.RING).delayed(6),
                        new VfxSpec(ParticleTypes.SNOWFLAKE, 10, 0.6, 0.6, 0.6, 0.05).delayed(6)),
                CueType.FROST_RIME, 20, Intensity.MAJOR));

        // ----- Dwarven: ground-hugging bursts of stone and craft -----
        ENTRIES.put(SignatureKey.DEEP_ONE_TREMOR, new SignatureEntry(
                "message.runic_races.deep_one.tremorsense",
                ChatFormatting.GRAY, true,
                List.of(new SfxSpec(ModSounds.TREMOR_PULSE, 0.8f, 1.0f)),
                List.of(
                        // A seismic wavefront rolls outward along the floor: three expanding rings.
                        new VfxSpec(ParticleTypes.POOF, 12, 1.5, 0.0, 1.5, 0.1, SignatureEntry.Shape.RING),
                        new VfxSpec(RaceColors.VOLT_GOLD, 10, 1.0, 0.0, 1.0, 0.05, SignatureEntry.Shape.RING),
                        new VfxSpec(ParticleTypes.POOF, 12, 3.0, 0.0, 3.0, 0.1, SignatureEntry.Shape.RING).delayed(4),
                        new VfxSpec(ParticleTypes.POOF, 12, 4.5, 0.0, 4.5, 0.1, SignatureEntry.Shape.RING).delayed(8)),
                CueType.SHAKE, 5, Intensity.MAJOR));

        ENTRIES.put(SignatureKey.FROST_ONE_RESOLVE, new SignatureEntry(
                "message.runic_races.frost_one.glacial_resolve",
                ChatFormatting.AQUA, true,
                // Ice crystallizes, then the braced shield-thud of settled resolve.
                List.of(new SfxSpec(SoundEvents.GLASS_PLACE, 0.6f, 0.7f),
                        new SfxSpec(SoundEvents.SHIELD_BLOCK, 0.6f, 0.9f).delayed(8)),
                List.of(
                        // A glacial shell crystallizes around the dwarf, then loose rime settles.
                        new VfxSpec(RaceColors.GLACIAL_CYAN, 20, 1.4, 0.0, 1.4, 0.08, SignatureEntry.Shape.DOME),
                        new VfxSpec(ModParticles.FROST_MOTE, 16, 0.7, 1.0, 0.7, 0.04).delayed(8),
                        new VfxSpec(ParticleTypes.SNOWFLAKE, 10, 0.6, 0.8, 0.6, 0.05).delayed(8)),
                CueType.FROST_RIME, 15, Intensity.MAJOR));

        ENTRIES.put(SignatureKey.IRON_ONE_SHIELD_WALL, new SignatureEntry(
                "message.runic_races.iron_one.shield_wall",
                ChatFormatting.GRAY, true,
                List.of(new SfxSpec(ModSounds.SHIELD_BRACE, 0.8f, 1.0f)),
                List.of(
                        // Iron first — the wall goes up — then a glint pass runs across its face.
                        new VfxSpec(RaceColors.IRON_GRAY, 20, 1.2, 0.0, 1.2, 0.04, SignatureEntry.Shape.DOME),
                        new VfxSpec(ParticleTypes.ENCHANTED_HIT, 16, 1.4, 0.0, 1.4, 0.05, SignatureEntry.Shape.DOME).delayed(4)),
                CueType.SHAKE, 4, Intensity.MAJOR));

        ENTRIES.put(SignatureKey.SKY_ONE_LEAP, new SignatureEntry(
                "message.runic_races.sky_one.mountain_leap",
                ChatFormatting.WHITE, true,
                // The crouch-and-spring, then the whoosh of leaving the ground.
                List.of(new SfxSpec(SoundEvents.GOAT_LONG_JUMP, 0.7f, 1.0f),
                        new SfxSpec(ModSounds.DASH_WHOOSH, 0.5f, 0.8f).delayed(2)),
                List.of(
                        // Dust ring at the feet, then the launch column erupts under the ascent.
                        new VfxSpec(ParticleTypes.POOF, 14, 1.2, 0.0, 1.2, 0.1, SignatureEntry.Shape.RING),
                        new VfxSpec(ParticleTypes.CLOUD, 18, 0.4, 2.5, 0.4, 0.25, SignatureEntry.Shape.BURST_UP).delayed(2)),
                CueType.WIND_STREAK, 10, Intensity.MAJOR));

        // ----- Bestial: organic, directional, physical -----
        ENTRIES.put(SignatureKey.ARACHNID_WEB_SNARE, new SignatureEntry(
                "message.runic_races.arachnid.web_snare",
                ChatFormatting.WHITE, true,
                List.of(new SfxSpec(ModSounds.WEB_SNARE, 0.8f, 1.0f),
                        new SfxSpec(SoundEvents.SPIDER_AMBIENT, 0.5f, 1.2f).delayed(4)),
                List.of(
                        // Eight silk spokes shoot out first; the strand ring settles on the snare edge.
                        new VfxSpec(ModParticles.WEB_STRAND, 32, 5.0, 0.0, 5.0, 0.02, SignatureEntry.Shape.SPOKES),
                        new VfxSpec(ParticleTypes.CRIT, 8, 0.5, 0.4, 0.5, 0.15),
                        new VfxSpec(ModParticles.WEB_STRAND, 16, 5.0, 0.0, 5.0, 0.01, SignatureEntry.Shape.RING).delayed(4)),
                null, 0, Intensity.MAJOR));

        ENTRIES.put(SignatureKey.AVIAN_WIND_BURST, new SignatureEntry(
                "message.runic_races.avian.wind_burst",
                ChatFormatting.AQUA, true,
                // A double wing-beat drives the launch.
                List.of(new SfxSpec(SoundEvents.PHANTOM_FLAP, 0.7f, 1.2f),
                        new SfxSpec(SoundEvents.PHANTOM_FLAP, 0.7f, 1.4f).delayed(3),
                        new SfxSpec(ModSounds.DASH_WHOOSH, 0.5f, 1.3f).delayed(3)),
                List.of(
                        // First beat kicks the gust column, the second shakes feathers loose in a ring.
                        new VfxSpec(ParticleTypes.CLOUD, 18, 0.5, 2.0, 0.5, 0.25, SignatureEntry.Shape.BURST_UP),
                        new VfxSpec(ModParticles.FEATHER_DOWN, 16, 1.2, 0.0, 1.2, 0.1, SignatureEntry.Shape.RING).delayed(3)),
                CueType.WIND_STREAK, 8, Intensity.MAJOR));

        ENTRIES.put(SignatureKey.CANINE_HOWL, new SignatureEntry(
                "message.runic_races.canine.howl_of_the_pack",
                ChatFormatting.GREEN, true,
                List.of(new SfxSpec(ModSounds.HOWL_PACK, 0.9f, 1.0f)),
                List.of(
                        // The howl propagates: three sound-rings rolling out as the cry carries.
                        new VfxSpec(ParticleTypes.POOF, 12, 1.5, 0.0, 1.5, 0.2, SignatureEntry.Shape.RING),
                        new VfxSpec(ParticleTypes.NOTE, 8, 0.8, 1.0, 0.8, 0.1),
                        new VfxSpec(ParticleTypes.POOF, 12, 2.5, 0.0, 2.5, 0.2, SignatureEntry.Shape.RING).delayed(6),
                        new VfxSpec(ParticleTypes.POOF, 12, 3.5, 0.0, 3.5, 0.2, SignatureEntry.Shape.RING).delayed(12)),
                null, 0, Intensity.MAJOR));

        ENTRIES.put(SignatureKey.FELINE_POUNCE, new SignatureEntry(
                "message.runic_races.feline.pounce",
                ChatFormatting.GOLD, true,
                // The hiss of the crouch, then claws at the moment of the leap.
                List.of(new SfxSpec(SoundEvents.CAT_HISS, 0.5f, 1.1f),
                        new SfxSpec(ModSounds.POUNCE_STRIKE, 0.7f, 1.0f).delayed(3)),
                List.of(
                        // Coiling gather at the crouch, then claw streaks rake down the lunge line
                        // (the CONE tracks the caster's live aim at the moment of the strike beat).
                        new VfxSpec(ParticleTypes.CLOUD, 10, 0.8, 0.0, 0.8, 0.15, SignatureEntry.Shape.RING_IN),
                        new VfxSpec(ModParticles.GALE_STREAK, 14, 3.5, 0.6, 0.0, 0.3, SignatureEntry.Shape.CONE).delayed(3),
                        new VfxSpec(ParticleTypes.CLOUD, 10, 0.4, 0.3, 0.4, 0.1).delayed(3)),
                null, 0, Intensity.MAJOR));

        ENTRIES.put(SignatureKey.KITSUNE_FOXFIRE, new SignatureEntry(
                "message.runic_races.kitsune.foxfire_illusion",
                ChatFormatting.LIGHT_PURPLE, true,
                // Foxfire catches, then a chime as the illusion completes and the fox is gone.
                List.of(new SfxSpec(ModSounds.FOXFIRE_IGNITE, 0.8f, 1.0f),
                        new SfxSpec(SoundEvents.AMETHYST_BLOCK_CHIME, 0.5f, 1.5f).delayed(6)),
                List.of(
                        // Spirit flames orbit while the illusion takes shape, then flare as it completes.
                        // (Arcane read on a bestial race is intentional — the kitsune is a spirit fox.)
                        new VfxSpec(ModParticles.FOXFIRE, 15, 1.0, 0.0, 1.0, 0.18, SignatureEntry.Shape.RING_ORBIT),
                        new VfxSpec(ParticleTypes.SOUL_FIRE_FLAME, 16, 0.6, 0.8, 0.6, 0.04).delayed(6),
                        new VfxSpec(ParticleTypes.ENCHANT, 10, 0.7, 0.9, 0.7, 0.3).delayed(6)),
                null, 0, Intensity.MAJOR));

        ENTRIES.put(SignatureKey.SERPEN_SHED, new SignatureEntry(
                "message.runic_races.serpen.shed_skin",
                ChatFormatting.GREEN, true,
                // The skin loosens (squish), then the hiss as the serpen slips free of it.
                List.of(new SfxSpec(SoundEvents.SLIME_SQUISH, 0.6f, 0.8f),
                        new SfxSpec(SoundEvents.CAT_HISS, 0.5f, 0.5f).delayed(6)),
                List.of(
                        // The old skin ripples around the body, then sloughs off and falls away
                        // with a few venom drips — left behind as the serpen escapes.
                        new VfxSpec(RaceColors.VERDANT_GREEN, 14, 1.0, 0.0, 1.0, 0.03, SignatureEntry.Shape.RING),
                        new VfxSpec(ParticleTypes.ITEM_SLIME, 14, 0.4, 0.6, 0.4, 0.03).delayed(6),
                        new VfxSpec(ModParticles.VENOM_DRIP, 4, 0.3, 0.5, 0.3, 0.02).delayed(6)),
                CueType.WIND_STREAK, 6, Intensity.MAJOR));

        // ----- Faeborne: playful swirls, blink-outs, pastel pops -----
        ENTRIES.put(SignatureKey.CHANGELING_MIRROR, new SignatureEntry(
                "message.runic_races.changeling.mirror_shift",
                ChatFormatting.LIGHT_PURPLE, true,
                // The mirror forms, then shatters as the new face steps through.
                List.of(new SfxSpec(ModSounds.MIRROR_SHIFT, 0.8f, 1.0f),
                        new SfxSpec(ModSounds.MIRROR_SHATTER, 0.7f, 1.0f).delayed(4)),
                List.of(
                        // Glass panes orbit into a mirror around the changeling — the freeze-frame
                        // is the camera flash — then the mirror shatters and the shards rain down.
                        new VfxSpec(ModParticles.MIRROR_SHARD, 14, 1.0, 0.0, 1.0, 0.15, SignatureEntry.Shape.RING_ORBIT),
                        new VfxSpec(ModParticles.MIRROR_SHARD, 12, 0.5, 0.8, 0.5, 0.2).delayed(4),
                        new VfxSpec(ModParticles.FAE_SPARKLE, 10, 0.5, 0.9, 0.5, 0.08).delayed(4)),
                CueType.FREEZE_FRAME, 6, Intensity.MAJOR));

        ENTRIES.put(SignatureKey.DRYAD_BLOOM, new SignatureEntry(
                "message.runic_races.dryad.verdant_bloom",
                ChatFormatting.GREEN, true,
                List.of(new SfxSpec(ModSounds.BLOOM_VERDANT, 0.9f, 1.0f)),
                List.of(
                        // Petals roll outward through the grove, then the bloom releases its pollen.
                        new VfxSpec(ModParticles.LEAF_PETAL, 20, 1.8, 0.0, 1.8, 0.1, SignatureEntry.Shape.RING),
                        new VfxSpec(RaceColors.VERDANT_GREEN, 8, 0.6, 0.8, 0.6, 0.04),
                        new VfxSpec(ModParticles.POLLEN_MOTE, 16, 1.2, 0.0, 1.2, 0.04, SignatureEntry.Shape.DOME).delayed(6),
                        new VfxSpec(ParticleTypes.HAPPY_VILLAGER, 8, 1.4, 0.4, 1.4, 0.05).delayed(6)),
                null, 0, Intensity.MAJOR));

        ENTRIES.put(SignatureKey.SPRITE_PHASE, new SignatureEntry(
                "message.runic_races.sprite.phase_shift",
                ChatFormatting.LIGHT_PURPLE, true,
                // Blink-out, then the arrival chime rings where the sprite reappears.
                List.of(new SfxSpec(SoundEvents.ENDERMAN_TELEPORT, 0.4f, 1.6f),
                        new SfxSpec(SoundEvents.BELL_BLOCK, 0.4f, 2.0f).delayed(4)),
                List.of(
                        // Sparkles collapse inward at the origin; the delayed burst tracks the
                        // caster live, so the re-appear pop lands at the blink DESTINATION.
                        new VfxSpec(ModParticles.FAE_SPARKLE, 22, 1.3, 0.0, 1.3, 0.3, SignatureEntry.Shape.RING_IN),
                        new VfxSpec(ParticleTypes.END_ROD, 14, 0.4, 0.6, 0.4, 0.2).delayed(4)),
                null, 0, Intensity.MAJOR));

        ENTRIES.put(SignatureKey.NYMPH_CHARM, new SignatureEntry(
                "message.runic_races.nymph.sirens_charm",
                ChatFormatting.AQUA, true,
                List.of(new SfxSpec(ModSounds.CHARM_SONG, 0.9f, 1.0f)),
                List.of(
                        // The song spirals up in verses while tide ripples spread with each one;
                        // hearts drift up last, as the charm lands.
                        new VfxSpec(ParticleTypes.NOTE, 14, 0.8, 2.2, 0.8, 0.05, SignatureEntry.Shape.HELIX),
                        new VfxSpec(RaceColors.TIDAL_TEAL, 8, 1.2, 0.0, 1.2, 0.1, SignatureEntry.Shape.RING),
                        new VfxSpec(RaceColors.TIDAL_TEAL, 8, 2.2, 0.0, 2.2, 0.1, SignatureEntry.Shape.RING).delayed(6),
                        new VfxSpec(ParticleTypes.HEART, 8, 0.6, 0.9, 0.6, 0.08).delayed(10)),
                null, 0, Intensity.MAJOR));

        // ----- Undead: everything sinks, lingers, or drains INTO the caster -----
        ENTRIES.put(SignatureKey.ZOMBIE_HUNGER, new SignatureEntry(
                "message.runic_races.zombie.undying_hunger",
                ChatFormatting.DARK_GREEN, true,
                // Two feeding gulps, eight ticks apart.
                List.of(new SfxSpec(SoundEvents.ZOMBIE_AMBIENT, 0.6f, 0.8f),
                        new SfxSpec(SoundEvents.GENERIC_EAT, 0.6f, 0.7f),
                        new SfxSpec(SoundEvents.GENERIC_EAT, 0.6f, 0.6f).delayed(8)),
                List.of(
                        // Life is pulled INTO the zombie in two waves — feeding, not exploding.
                        new VfxSpec(ParticleTypes.SCULK_SOUL, 18, 1.6, 0.0, 1.6, 0.2, SignatureEntry.Shape.RING_IN),
                        new VfxSpec(ParticleTypes.CRIMSON_SPORE, 14, 1.2, 0.0, 1.2, 0.15, SignatureEntry.Shape.RING_IN).delayed(8),
                        new VfxSpec(ParticleTypes.ASH, 8, 0.5, 0.7, 0.5, 0.03).delayed(8)),
                null, 0, Intensity.MAJOR));

        ENTRIES.put(SignatureKey.SKELETON_CONSCRIPT, new SignatureEntry(
                "message.runic_races.skeleton.conscript_the_dead",
                ChatFormatting.WHITE, true,
                // The circle is called, dirt breaks, then bone knits together.
                List.of(new SfxSpec(SoundEvents.SKELETON_AMBIENT, 0.6f, 0.8f),
                        new SfxSpec(SoundEvents.BONE_BLOCK_BREAK, 0.7f, 0.7f).delayed(4)),
                List.of(
                        // Summoning circle flares, grave dirt erupts with flying bone chips,
                        // then the soul column rises as the conscripts take shape.
                        new VfxSpec(ModParticles.RUNE_GLYPH, 10, 1.5, 0.0, 1.5, 0.02, SignatureEntry.Shape.RING),
                        new VfxSpec(ParticleTypes.POOF, 14, 0.6, 0.2, 0.6, 0.1).delayed(4),
                        new VfxSpec(ModParticles.BONE_CHIP, 8, 0.6, 0.5, 0.6, 0.15).delayed(4),
                        new VfxSpec(RaceColors.SOUL_VIOLET, 14, 0.5, 2.2, 0.5, 0.05, SignatureEntry.Shape.HELIX).delayed(8)),
                null, 0, Intensity.MAJOR));

        ENTRIES.put(SignatureKey.REAPER_HARVEST, new SignatureEntry(
                "message.runic_races.reaper.soul_harvest",
                ChatFormatting.DARK_PURPLE, true,
                List.of(new SfxSpec(ModSounds.HARVEST_SOUL, 0.9f, 1.0f)),
                List.of(
                        // Souls stream inward from the harvest radius, then the taken essence
                        // climbs the reaper's body as the stolen life settles in.
                        new VfxSpec(ParticleTypes.SOUL, 24, 2.6, 0.0, 2.6, 0.25, SignatureEntry.Shape.RING_IN),
                        new VfxSpec(ParticleTypes.SCULK_SOUL, 14, 0.7, 0.9, 0.7, 0.06),
                        new VfxSpec(ModParticles.SOUL_WISP, 12, 0.7, 2.0, 0.7, 0.05, SignatureEntry.Shape.HELIX).delayed(8)),
                CueType.VIGNETTE_PULSE, 20, Intensity.MAJOR));

        // =====================================================================
        // Fragility procs — bannerless victim-side cues (MINOR). The words stay
        // with the notification system; these are the sound + spark of a weakness
        // actually biting. Gating (damage floor, fall, magic) lives in
        // RacialEventHandler; all seven share the "fragility" debounce channel.
        // =====================================================================

        ENTRIES.put(SignatureKey.HIGH_ELF_FRAGILITY, new SignatureEntry(
                null, ChatFormatting.WHITE, false,
                // Fragile Grace: a heavy hit rings like struck crystal.
                List.of(new SfxSpec(SoundEvents.AMETHYST_CLUSTER_BREAK, 0.5f, 1.5f)),
                List.of(),
                CueType.VIGNETTE_PULSE, 8, Intensity.MINOR));

        ENTRIES.put(SignatureKey.ARACHNID_FRAGILITY, new SignatureEntry(
                null, ChatFormatting.WHITE, false,
                // Fragile Carapace: chitin cracks under a heavy blow.
                List.of(new SfxSpec(SoundEvents.TURTLE_EGG_CRACK, 0.6f, 0.6f)),
                List.of(new VfxSpec(ParticleTypes.CRIT, 6, 0.3, 0.5, 0.3, 0.1)),
                null, 0, Intensity.MINOR));

        ENTRIES.put(SignatureKey.SKELETON_FRAGILITY, new SignatureEntry(
                null, ChatFormatting.WHITE, false,
                // Brittle Bones: chips fly on falls and heavy hits.
                List.of(new SfxSpec(SoundEvents.BONE_BLOCK_BREAK, 0.5f, 1.2f)),
                List.of(new VfxSpec(ModParticles.BONE_CHIP, 6, 0.3, 0.5, 0.3, 0.08)),
                null, 0, Intensity.MINOR));

        ENTRIES.put(SignatureKey.AVIAN_FRAGILITY, new SignatureEntry(
                null, ChatFormatting.WHITE, false,
                // Hollow Bones: feathers burst loose when the landing goes wrong.
                List.of(new SfxSpec(SoundEvents.BONE_BLOCK_BREAK, 0.35f, 1.5f)),
                List.of(new VfxSpec(ModParticles.FEATHER_DOWN, 8, 0.4, 0.6, 0.4, 0.05)),
                null, 0, Intensity.MINOR));

        ENTRIES.put(SignatureKey.SPRITE_FRAGILITY, new SignatureEntry(
                null, ChatFormatting.WHITE, false,
                // Fragile Essence: the gossamer form scatters sparks and cries out.
                List.of(new SfxSpec(SoundEvents.BAT_HURT, 0.4f, 1.6f)),
                List.of(new VfxSpec(ModParticles.FAE_SPARKLE, 6, 0.4, 0.6, 0.4, 0.1)),
                null, 0, Intensity.MINOR));

        ENTRIES.put(SignatureKey.CELERON_FRAGILITY, new SignatureEntry(
                null, ChatFormatting.WHITE, false,
                // Featherweight Frame: knocked around hard enough to shake feathers loose.
                List.of(),
                List.of(new VfxSpec(ModParticles.FEATHER_DOWN, 5, 0.4, 0.6, 0.4, 0.06)),
                null, 0, Intensity.MINOR));

        ENTRIES.put(SignatureKey.DEMON_FRAGILITY, new SignatureEntry(
                null, ChatFormatting.WHITE, false,
                // Holy Vulnerability: sanctified magic flares gold and tolls a bell.
                List.of(new SfxSpec(SoundEvents.BELL_BLOCK, 0.4f, 2.0f)),
                List.of(new VfxSpec(ParticleTypes.END_ROD, 6, 0.3, 0.5, 0.3, 0.05)),
                null, 0, Intensity.MINOR));

        // =====================================================================
        // Passive proc cues — bannerless (MINOR). A passive earns a cue only at
        // a real moment; stat-only passives stay expressed through ambience.
        // =====================================================================

        ENTRIES.put(SignatureKey.PRIMIAN_ADAPTATION, new SignatureEntry(
                null, ChatFormatting.WHITE, false,
                // A new place learned: a small gold glint (the rune counter shows the stacks).
                List.of(new SfxSpec(SoundEvents.EXPERIENCE_ORB_PICKUP, 0.25f, 1.6f)),
                List.of(new VfxSpec(RaceColors.VOLT_GOLD, 8, 0.4, 0.6, 0.4, 0.05)),
                null, 0, Intensity.MINOR));

        ENTRIES.put(SignatureKey.VALEN_SHOULDER_CHECK, new SignatureEntry(
                null, ChatFormatting.WHITE, false,
                // The shove lands like a golem's swing; iron dust flies along the impact line.
                List.of(new SfxSpec(SoundEvents.IRON_GOLEM_ATTACK, 0.4f, 0.7f)),
                List.of(new VfxSpec(RaceColors.IRON_GRAY, 10, 0.3, 0.2, 0.3, 0.15, SignatureEntry.Shape.LINE)),
                null, 0, Intensity.MINOR));

        ENTRIES.put(SignatureKey.ZOMBIE_DEATHLESS, new SignatureEntry(
                null, ChatFormatting.WHITE, false,
                // Poison/hunger washes off the deathless flesh as a puff of grave-ash.
                List.of(new SfxSpec(SoundEvents.ZOMBIE_AMBIENT, 0.25f, 1.3f)),
                List.of(new VfxSpec(ParticleTypes.ASH, 8, 0.3, 0.6, 0.3, 0.03)),
                null, 0, Intensity.MINOR));

        ENTRIES.put(SignatureKey.ARACHNID_WEB_SENSE, new SignatureEntry(
                null, ChatFormatting.WHITE, false,
                // The web trembles — silent silk shiver when the senses mark prey.
                List.of(),
                List.of(new VfxSpec(ModParticles.WEB_STRAND, 6, 0.5, 0.4, 0.5, 0.03)),
                null, 0, Intensity.MINOR));

        // =====================================================================
        // Weakness onset cues — bannerless (MINOR). Fired on RaceStateFlags 0→1
        // edges through WeaknessCueRegistry; the notification system owns the
        // words, these own the sensation of the weakness starting to bite.
        // =====================================================================

        ENTRIES.put(SignatureKey.WEAKNESS_SUNLIGHT_SEAR, new SignatureEntry(
                null, ChatFormatting.WHITE, false,
                // Grave-touched flesh hisses the moment the sun finds it.
                List.of(new SfxSpec(ModSounds.SIZZLE_SUNLIGHT, 0.35f, 1.2f)),
                List.of(new VfxSpec(ParticleTypes.SMOKE, 6, 0.2, 0.5, 0.2, 0.02)),
                null, 0, Intensity.MINOR));

        ENTRIES.put(SignatureKey.WEAKNESS_SUN_DAZZLE, new SignatureEntry(
                null, ChatFormatting.WHITE, false,
                // Daylight glare for the dark-adapted: a silent white-out squint, no sizzle.
                List.of(),
                List.of(),
                CueType.MOON_GLOW, 12, Intensity.MINOR));

        ENTRIES.put(SignatureKey.WEAKNESS_KINDLING, new SignatureEntry(
                null, ChatFormatting.WHITE, false,
                // Dryad: living wood catches — embers bite and leaves scatter in panic.
                List.of(new SfxSpec(ModSounds.WARN_KINDLING, 0.7f, 1.0f)),
                List.of(new VfxSpec(ModParticles.EMBER_SCALE, 8, 0.4, 0.6, 0.4, 0.06),
                        new VfxSpec(ModParticles.LEAF_PETAL, 10, 0.5, 0.7, 0.5, 0.15)),
                CueType.HEAT_SHIMMER, 20, Intensity.MINOR));

        ENTRIES.put(SignatureKey.WEAKNESS_THAW, new SignatureEntry(
                null, ChatFormatting.WHITE, false,
                // Frost-kin on fire: rime flashes to steam and drips away.
                List.of(new SfxSpec(SoundEvents.FIRE_EXTINGUISH, 0.4f, 1.5f)),
                List.of(new VfxSpec(ModParticles.FROST_MOTE, 8, 0.3, 0.6, 0.3, 0.04)),
                null, 0, Intensity.MINOR));

        ENTRIES.put(SignatureKey.WEAKNESS_HYDROPHOBIA, new SignatureEntry(
                null, ChatFormatting.WHITE, false,
                // Feline fully under: a muffled yowl of protest in a burst of bubbles.
                List.of(new SfxSpec(SoundEvents.CAT_HISS, 0.6f, 1.1f),
                        new SfxSpec(SoundEvents.PLAYER_SPLASH, 0.4f, 1.3f)),
                List.of(new VfxSpec(ParticleTypes.BUBBLE, 6, 0.3, 0.5, 0.3, 0.05)),
                null, 0, Intensity.MINOR));

        ENTRIES.put(SignatureKey.WEAKNESS_SHORT_CIRCUIT, new SignatureEntry(
                null, ChatFormatting.WHITE, false,
                // Storm scales short out the moment they go under.
                List.of(new SfxSpec(SoundEvents.FIRE_EXTINGUISH, 0.5f, 1.8f)),
                List.of(new VfxSpec(ParticleTypes.ELECTRIC_SPARK, 8, 0.4, 0.5, 0.4, 0.1)),
                null, 0, Intensity.MINOR));

        ENTRIES.put(SignatureKey.WEAKNESS_DRY, new SignatureEntry(
                null, ChatFormatting.WHITE, false,
                // Waterbound skin tightens and cracks on dry land.
                List.of(new SfxSpec(SoundEvents.SAND_BREAK, 0.35f, 0.8f)),
                List.of(new VfxSpec(ParticleTypes.POOF, 6, 0.3, 0.4, 0.3, 0.02)),
                null, 0, Intensity.MINOR));

        ENTRIES.put(SignatureKey.WEAKNESS_COLD_IRON, new SignatureEntry(
                null, ChatFormatting.WHITE, false,
                // Cold iron scalds fae hands with a quiet quench-hiss.
                List.of(new SfxSpec(SoundEvents.LAVA_EXTINGUISH, 0.25f, 1.7f)),
                List.of(new VfxSpec(ParticleTypes.SMOKE, 3, 0.2, 0.3, 0.2, 0.02)),
                null, 0, Intensity.MINOR));
    }

    public static SignatureEntry get(SignatureKey key) {
        return ENTRIES.get(key);
    }

    private SignatureRegistry() {}
}
