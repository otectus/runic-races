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
        // Family grammar with anticipation, a readable geometric silhouette, and a settling beat.
        // Expansion actions share the same bounded presentation path as the legacy races.
        ENTRIES.put(SignatureKey.COLOSSAN_ACTIVE, new SignatureEntry(
                "message.runic_races.colossan.colossal_heave", ChatFormatting.GOLD, true,
                List.of(new SfxSpec(SoundEvents.IRON_GOLEM_STEP, 0.45f, 0.9f)),
                List.of(
                        // Stone braces the feet; a heavy crescent wraps the prepared striking arm.
                        new VfxSpec(ModParticles.ROCK_CHIP, 16, 1.3, 0.0, 1.3, 0.035, SignatureEntry.Shape.SPOKES),
                        new VfxSpec(RaceColors.IRON_GRAY, 16, 1.1, 0.9, 0.3, 0.035, SignatureEntry.Shape.ARC).delayed(3),
                        new VfxSpec(ParticleTypes.CRIT, 16, 1.4, 1.0, 0.3, 0.08, SignatureEntry.Shape.ARC).delayed(6)
                ), null, 0, Intensity.MAJOR));
        ENTRIES.put(SignatureKey.AURORAN_ACTIVE, new SignatureEntry(
                "message.runic_races.auroran.dawnward", ChatFormatting.GOLD, true,
                List.of(new SfxSpec(SoundEvents.AMETHYST_BLOCK_CHIME, 0.45f, 0.9f)),
                List.of(
                        // A sunrise sigil opens into a golden ward, then its bright rim seals.
                        new VfxSpec(RaceColors.VOLT_GOLD, 16, 1.5, 0.0, 1.5, 0.015, SignatureEntry.Shape.SIGIL),
                        new VfxSpec(ModParticles.ARCANE_GLINT, 24, 1.7, 1.9, 1.7, 0.025, SignatureEntry.Shape.DOME).delayed(3),
                        new VfxSpec(ParticleTypes.END_ROD, 16, 1.1, 1.7, 0.8, 0.02, SignatureEntry.Shape.SHIELD).delayed(7)
                ), null, 0, Intensity.MAJOR));
        ENTRIES.put(SignatureKey.GROVE_ELF_ACTIVE, new SignatureEntry(
                "message.runic_races.grove_elf.stillleaf_aim", ChatFormatting.GREEN, true,
                List.of(new SfxSpec(SoundEvents.ARROW_SHOOT, 0.45f, 0.9f)),
                List.of(
                        // Leaves rise along the bow arm and gather into a clear aiming aperture.
                        new VfxSpec(ModParticles.LEAF_PETAL, 16, 0.7, 2.0, 0.7, 0.045, SignatureEntry.Shape.HELIX),
                        new VfxSpec(RaceColors.VERDANT_GREEN, 16, 0.45, 1.5, 0.0, 0.025, SignatureEntry.Shape.WAVE).delayed(4),
                        new VfxSpec(ParticleTypes.ENCHANT, 16, 1.3, 0.0, 1.3, 0.13, SignatureEntry.Shape.RING_IN).delayed(8)
                ), null, 0, Intensity.MAJOR));
        ENTRIES.put(SignatureKey.TIDE_ELF_ACTIVE, new SignatureEntry(
                "message.runic_races.tide_elf.currentstep", ChatFormatting.AQUA, true,
                List.of(new SfxSpec(SoundEvents.PLAYER_SPLASH, 0.45f, 0.9f)),
                List.of(
                        // A curling tide marks the step and sheds three visible wakes along movement.
                        new VfxSpec(RaceColors.TIDAL_TEAL, 16, 0.65, 1.5, 0.65, 0.04, SignatureEntry.Shape.HELIX),
                        new VfxSpec(ParticleTypes.SPLASH, 12, 0.65, 1.2, 0.0, 0.13, SignatureEntry.Shape.WAVE).delayed(2),
                        new VfxSpec(ModParticles.MOON_SLIVER, 12, 0.9, 1.4, 0.0, 0.1, SignatureEntry.Shape.WAVE).delayed(5),
                        new VfxSpec(ParticleTypes.SPLASH, 12, 1.2, 0.0, 1.2, 0.09, SignatureEntry.Shape.RING).delayed(9)
                ), null, 0, Intensity.MAJOR));
        ENTRIES.put(SignatureKey.ASTRAL_ELF_ACTIVE, new SignatureEntry(
                "message.runic_races.astral_elf.starbound_thread", ChatFormatting.LIGHT_PURPLE, true,
                List.of(new SfxSpec(SoundEvents.AMETHYST_BLOCK_RESONATE, 0.45f, 0.9f)),
                List.of(
                        // A star anchor is inscribed beneath the caster, then its thread winds upward.
                        new VfxSpec(ModParticles.MOON_SLIVER, 20, 1.5, 0.0, 1.5, 0.012, SignatureEntry.Shape.SIGIL),
                        new VfxSpec(ParticleTypes.END_ROD, 20, 0.8, 2.5, 0.8, 0.035, SignatureEntry.Shape.HELIX).delayed(4),
                        new VfxSpec(RaceColors.SILVER_MOON, 16, 1.8, 0.0, 1.8, 0.12, SignatureEntry.Shape.RING_IN).delayed(10)
                ), null, 0, Intensity.MAJOR));
        ENTRIES.put(SignatureKey.MOUNTAIN_ONE_ACTIVE, new SignatureEntry(
                "message.runic_races.mountain_one.quarry_rhythm", ChatFormatting.GRAY, true,
                List.of(new SfxSpec(SoundEvents.STONE_BREAK, 0.45f, 0.9f)),
                List.of(
                        // Measured quarry beats trace the stone and throw chips from the planted feet.
                        new VfxSpec(ModParticles.ROCK_CHIP, 16, 1.4, 0.0, 1.4, 0.018, SignatureEntry.Shape.SIGIL),
                        new VfxSpec(ParticleTypes.CRIT, 16, 1.9, 0.0, 1.9, 0.025, SignatureEntry.Shape.SPOKES).delayed(4),
                        new VfxSpec(ModParticles.ROCK_CHIP, 16, 0.65, 1.5, 0.65, 0.12, SignatureEntry.Shape.BURST_UP).delayed(8)
                ), null, 0, Intensity.MAJOR));
        ENTRIES.put(SignatureKey.MOSS_ONE_ACTIVE, new SignatureEntry(
                "message.runic_races.moss_one.mycelial_respite", ChatFormatting.DARK_GREEN, true,
                List.of(new SfxSpec(SoundEvents.MOSS_PLACE, 0.45f, 0.9f)),
                List.of(
                        // A mycelial circle outlines the recovery patch before spores rise through it.
                        new VfxSpec(RaceColors.VERDANT_GREEN, 20, 3.0, 0.0, 3.0, 0.008, SignatureEntry.Shape.SIGIL),
                        new VfxSpec(ModParticles.POLLEN_MOTE, 20, 2.5, 1.5, 2.5, 0.018, SignatureEntry.Shape.DOME).delayed(4),
                        new VfxSpec(ParticleTypes.SPORE_BLOSSOM_AIR, 16, 1.5, 2.0, 1.5, 0.025, SignatureEntry.Shape.HELIX).delayed(10)
                ), null, 0, Intensity.MAJOR));
        ENTRIES.put(SignatureKey.CRYSTAL_ONE_ACTIVE, new SignatureEntry(
                "message.runic_races.crystal_one.prism_reprisal", ChatFormatting.LIGHT_PURPLE, true,
                List.of(new SfxSpec(SoundEvents.AMETHYST_BLOCK_CHIME, 0.45f, 0.9f)),
                List.of(
                        // Facets rise from the ground and lock into a prismatic counterguard.
                        new VfxSpec(ModParticles.MIRROR_SHARD, 16, 0.8, 1.5, 0.8, 0.075, SignatureEntry.Shape.BURST_UP),
                        new VfxSpec(RaceColors.ARCANE_AZURE, 20, 1.25, 1.8, 0.9, 0.01, SignatureEntry.Shape.SHIELD).delayed(4),
                        new VfxSpec(ModParticles.MIRROR_SHARD, 16, 1.3, 0.0, 1.3, 0.075, SignatureEntry.Shape.RING_ORBIT).delayed(8)
                ), null, 0, Intensity.MAJOR));
        ENTRIES.put(SignatureKey.BOVINE_ACTIVE, new SignatureEntry(
                "message.runic_races.bovine.hornrush", ChatFormatting.GOLD, true,
                List.of(new SfxSpec(SoundEvents.RAVAGER_STEP, 0.45f, 0.9f)),
                List.of(
                        // Dust gathers during the windup; paired horn arcs strike forward as the rush starts.
                        new VfxSpec(ModParticles.ROCK_CHIP, 16, 1.5, 0.0, 1.5, 0.12, SignatureEntry.Shape.RING_IN),
                        new VfxSpec(ParticleTypes.CRIT, 16, 1.6, 0.9, 0.5, 0.18, SignatureEntry.Shape.ARC).delayed(6),
                        new VfxSpec(ParticleTypes.POOF, 12, 1.1, 0.0, 1.1, 0.12, SignatureEntry.Shape.RING).delayed(9),
                        new VfxSpec(ModParticles.ROCK_CHIP, 12, 1.5, 0.7, 0.6, 0.1, SignatureEntry.Shape.ARC).delayed(13)
                ), null, 0, Intensity.MAJOR));
        ENTRIES.put(SignatureKey.SAURIAN_ACTIVE, new SignatureEntry(
                "message.runic_races.saurian.patient_ambush", ChatFormatting.DARK_GREEN, true,
                List.of(new SfxSpec(SoundEvents.TURTLE_AMBIENT_LAND, 0.45f, 0.9f)),
                List.of(
                        // A close coil draws inward, then amber claw edges mark the prepared ambush.
                        new VfxSpec(RaceColors.VERDANT_GREEN, 16, 1.4, 0.0, 1.4, 0.12, SignatureEntry.Shape.RING_IN),
                        new VfxSpec(ModParticles.ROCK_CHIP, 12, 1.0, 0.65, 0.4, 0.025, SignatureEntry.Shape.ARC).delayed(4),
                        new VfxSpec(ParticleTypes.CRIT, 20, 1.35, 1.0, 0.5, 0.05, SignatureEntry.Shape.ARC).delayed(8)
                ), null, 0, Intensity.MAJOR));
        ENTRIES.put(SignatureKey.CHELON_ACTIVE, new SignatureEntry(
                "message.runic_races.chelon.shellfast", ChatFormatting.DARK_AQUA, true,
                List.of(new SfxSpec(SoundEvents.SHIELD_BLOCK, 0.45f, 0.9f)),
                List.of(
                        // Shell segments emerge from the feet and settle into two overlapping protective rims.
                        new VfxSpec(ParticleTypes.WAX_ON, 20, 1.25, 1.8, 1.25, 0.025, SignatureEntry.Shape.DOME),
                        new VfxSpec(RaceColors.TIDAL_TEAL, 16, 1.4, 0.0, 1.4, 0.012, SignatureEntry.Shape.SIGIL).delayed(4),
                        new VfxSpec(ParticleTypes.WAX_ON, 20, 1.45, 1.7, 1.45, 0.015, SignatureEntry.Shape.DOME).delayed(8)
                ), null, 0, Intensity.MAJOR));
        ENTRIES.put(SignatureKey.ZEPHYR_ACTIVE, new SignatureEntry(
                "message.runic_races.zephyr.crosswind", ChatFormatting.AQUA, true,
                List.of(new SfxSpec(SoundEvents.PHANTOM_FLAP, 0.45f, 0.9f)),
                List.of(
                        // Air coils around the body, then successive ribbon crescents trace the crosswind.
                        new VfxSpec(ModParticles.GALE_STREAK, 16, 1.0, 0.0, 1.0, 0.14, SignatureEntry.Shape.RING_ORBIT),
                        new VfxSpec(ParticleTypes.CLOUD, 12, 1.3, 0.7, 0.5, 0.12, SignatureEntry.Shape.ARC).delayed(2),
                        new VfxSpec(ModParticles.GALE_STREAK, 12, 1.5, 1.0, 0.6, 0.18, SignatureEntry.Shape.ARC).delayed(4),
                        new VfxSpec(ModParticles.GALE_STREAK, 12, 1.2, 0.0, 1.2, 0.1, SignatureEntry.Shape.RING_ORBIT).delayed(8)
                ), null, 0, Intensity.MAJOR));
        ENTRIES.put(SignatureKey.NIGHTBORN_ACTIVE, new SignatureEntry(
                "message.runic_races.nightborn.crimson_hunt", ChatFormatting.DARK_RED, true,
                List.of(new SfxSpec(SoundEvents.BAT_TAKEOFF, 0.45f, 0.9f)),
                List.of(
                        // Shadow drains inward; crimson helixes pulse twice as the hunt takes hold.
                        new VfxSpec(ModParticles.SHADOW_WISP, 16, 1.8, 0.0, 1.8, 0.13, SignatureEntry.Shape.RING_IN),
                        new VfxSpec(RaceColors.CRIMSON_BLOOD, 20, 0.7, 2.1, 0.7, 0.045, SignatureEntry.Shape.HELIX).delayed(4),
                        new VfxSpec(ParticleTypes.CRIMSON_SPORE, 16, 1.6, 0.0, 1.6, 0.11, SignatureEntry.Shape.RING_IN).delayed(10)
                ), null, 0, Intensity.MAJOR));
        ENTRIES.put(SignatureKey.RETURNED_ACTIVE, new SignatureEntry(
                "message.runic_races.returned.unfinished_purpose", ChatFormatting.DARK_PURPLE, true,
                List.of(new SfxSpec(SoundEvents.SOUL_ESCAPE, 0.45f, 0.9f)),
                List.of(
                        // A grave seal contracts into the remnant, then soul light points toward the pursuit.
                        new VfxSpec(RaceColors.SOUL_VIOLET, 16, 1.3, 0.0, 1.3, 0.015, SignatureEntry.Shape.SIGIL),
                        new VfxSpec(ModParticles.SOUL_WISP, 20, 1.7, 0.0, 1.7, 0.15, SignatureEntry.Shape.RING_IN).delayed(4),
                        new VfxSpec(ModParticles.SOUL_WISP, 16, 1.2, 1.0, 0.5, 0.06, SignatureEntry.Shape.ARC).delayed(9)
                ), null, 0, Intensity.MAJOR));
        ENTRIES.put(SignatureKey.WAILER_ACTIVE, new SignatureEntry(
                "message.runic_races.wailer.keening_cry", ChatFormatting.LIGHT_PURPLE, true,
                List.of(new SfxSpec(ModSounds.WAILER_CRY, 0.45f, 0.9f)),
                List.of(
                        // Soul wisps gather visibly through the warning; AbilityService emits the wave only when the cry releases.
                        new VfxSpec(ModParticles.SOUL_WISP, 20, 1.6, 0.0, 1.6, 0.12, SignatureEntry.Shape.RING_IN),
                        new VfxSpec(RaceColors.SOUL_VIOLET, 16, 0.4, 0.8, 0.0, 0.008, SignatureEntry.Shape.WAVE).delayed(4),
                        new VfxSpec(ModParticles.SOUL_WISP, 16, 0.65, 1.8, 0.65, 0.025, SignatureEntry.Shape.HELIX).delayed(8)
                ), null, 0, Intensity.MAJOR));
        ENTRIES.put(SignatureKey.SCALEHEIR_ACTIVE, new SignatureEntry(
                "message.runic_races.scaleheir.dominion_roar", ChatFormatting.GOLD, true,
                List.of(new SfxSpec(SoundEvents.ENDER_DRAGON_GROWL, 0.45f, 0.9f)),
                List.of(
                        // The roar rolls outward across the domain while a scale ward seals around its heir.
                        new VfxSpec(ModParticles.EMBER_SCALE, 16, 1.4, 1.8, 1.4, 0.04, SignatureEntry.Shape.DOME),
                        new VfxSpec(RaceColors.VOLT_GOLD, 20, 2.5, 0.0, 2.5, 0.13, SignatureEntry.Shape.RING).delayed(3),
                        new VfxSpec(ParticleTypes.WAX_ON, 20, 4.5, 0.0, 4.5, 0.1, SignatureEntry.Shape.RING).delayed(7)
                ), null, 0, Intensity.MAJOR));
        ENTRIES.put(SignatureKey.WYVERNKIN_ACTIVE, new SignatureEntry(
                "message.runic_races.wyvernkin.venom_swoop", ChatFormatting.DARK_GREEN, true,
                List.of(new SfxSpec(SoundEvents.PHANTOM_SWOOP, 0.45f, 0.9f)),
                List.of(
                        // Hooked wing arcs cut the air and venom streams behind the committed swoop.
                        new VfxSpec(ModParticles.GALE_STREAK, 16, 1.7, 1.0, 0.5, 0.17, SignatureEntry.Shape.ARC),
                        new VfxSpec(ModParticles.VENOM_DRIP, 12, 2.8, 0.5, 0.0, 0.2, SignatureEntry.Shape.CONE).delayed(2),
                        new VfxSpec(ModParticles.GALE_STREAK, 16, 2.0, 1.0, 0.5, 0.2, SignatureEntry.Shape.ARC).delayed(5),
                        new VfxSpec(ModParticles.VENOM_DRIP, 12, 1.1, 0.0, 1.1, 0.055, SignatureEntry.Shape.RING).delayed(9)
                ), null, 0, Intensity.MAJOR));
        ENTRIES.put(SignatureKey.ZEPHYR_WING_FLAP, new SignatureEntry(
                "message.runic_races.signature.zephyr.wing_flap", ChatFormatting.AQUA, false,
                List.of(new SfxSpec(ModSounds.WINGS_FLAP_SMALL, 0.2f, 1.6f)),
                List.of(
                        // Two short air ribbons trace the flap and its curling wake.
                        new VfxSpec(ModParticles.GALE_STREAK, 8, 1.1, 0.65, 0.35, 0.11, SignatureEntry.Shape.ARC),
                        new VfxSpec(ModParticles.GALE_STREAK, 8, 0.9, 0.0, 0.9, 0.09, SignatureEntry.Shape.RING_ORBIT).delayed(2)
                ), null, 0, Intensity.MINOR));

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
                        // Souls collapse into the fallen body, erupt as a revival column, and seal a luminous grave sigil.
                        new VfxSpec(ParticleTypes.SOUL, 30, 3.2, 0.0, 3.2, 0.2, SignatureEntry.Shape.RING_IN),
                        new VfxSpec(ModParticles.SOUL_WISP, 30, 0.9, 3.4, 0.9, 0.13, SignatureEntry.Shape.BURST_UP).delayed(4),
                        new VfxSpec(ModParticles.RUNE_GLYPH, 25, 2.6, 0.0, 2.6, 0.02, SignatureEntry.Shape.SIGIL).delayed(12)
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
                        // The spectral body unwinds into a pale spiral and drains through its lingering ground veil.
                        new VfxSpec(ModParticles.SOUL_WISP, 20, 0.8, 2.1, 0.8, 0.035, SignatureEntry.Shape.HELIX),
                        new VfxSpec(ModParticles.SHADOW_WISP, 16, 1.8, 0.0, 1.8, 0.16, SignatureEntry.Shape.RING_IN).delayed(4),
                        new VfxSpec(ParticleTypes.SOUL_FIRE_FLAME, 16, 1.4, 0.0, 1.4, 0.025, SignatureEntry.Shape.RING).delayed(9)
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
                        // An ember seal ignites, followed by a soul-fire eruption and a spreading infernal rim.
                        new VfxSpec(ModParticles.EMBER_SCALE, 20, 1.5, 0.0, 1.5, 0.025, SignatureEntry.Shape.SIGIL),
                        new VfxSpec(ParticleTypes.SOUL_FIRE_FLAME, 24, 2.0, 2.5, 2.0, 0.13, SignatureEntry.Shape.DOME).delayed(6),
                        new VfxSpec(ParticleTypes.LAVA, 12, 2.6, 0.0, 2.6, 0.055, SignatureEntry.Shape.RING).delayed(10)
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
                        // A golden life seal flashes; nine-lived essence spirals upward and opens into a heart-bright crown.
                        new VfxSpec(ParticleTypes.END_ROD, 30, 1.8, 0.0, 1.8, 0.025, SignatureEntry.Shape.SIGIL),
                        new VfxSpec(ParticleTypes.CRIT, 20, 0.8, 2.5, 0.8, 0.08, SignatureEntry.Shape.HELIX).delayed(3),
                        new VfxSpec(ParticleTypes.HEART, 40, 2.0, 2.2, 2.0, 0.09, SignatureEntry.Shape.DOME).delayed(8)
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
                        // The forge sigil is struck; flame rises through it and hot chips mark the blessing's seal.
                        new VfxSpec(ModParticles.RUNE_GLYPH, 20, 1.3, 0.0, 1.3, 0.015, SignatureEntry.Shape.SIGIL),
                        new VfxSpec(ParticleTypes.FLAME, 20, 0.55, 2.0, 0.55, 0.11, SignatureEntry.Shape.BURST_UP).delayed(4),
                        new VfxSpec(ModParticles.EMBER_SCALE, 16, 1.5, 1.6, 1.5, 0.05, SignatureEntry.Shape.DOME).delayed(8)
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
                        // Runic strokes draw the ward boundary; a shield dome grows above its glowing inscription.
                        new VfxSpec(ModParticles.RUNE_GLYPH, 24, 2.6, 0.0, 2.6, 0.01, SignatureEntry.Shape.SIGIL),
                        new VfxSpec(RaceColors.ARCANE_AZURE, 16, 2.4, 2.4, 2.4, 0.02, SignatureEntry.Shape.DOME).delayed(4),
                        new VfxSpec(ParticleTypes.ENCHANT, 20, 2.7, 2.7, 2.7, 0.025, SignatureEntry.Shape.DOME).delayed(10)
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
                        // A sparkling spiral weaves the bargain, then a rune ring seals with a pastel starburst.
                        new VfxSpec(ModParticles.FAE_SPARKLE, 20, 0.9, 2.2, 0.9, 0.055, SignatureEntry.Shape.HELIX),
                        new VfxSpec(ModParticles.RUNE_GLYPH, 16, 1.6, 0.0, 1.6, 0.015, SignatureEntry.Shape.SIGIL).delayed(3),
                        new VfxSpec(ParticleTypes.FIREWORK, 20, 1.7, 2.2, 1.7, 0.11, SignatureEntry.Shape.DOME).delayed(8)
                ),
                CueType.VIGNETTE_PULSE,
                25,
                Intensity.MAJOR
        ));

        // ===== Draconic breaths (MAJOR each) =====
        // These 18-20 particle accents trace mouth pressure rings; ConeBreathAction owns
        // the sustained, element-specific torrent and its independent density budget.
        // Breath beats retain cast-time eyes and aim so pressure rings stay inside the
        // original exhale when the caster moves; other recipes follow the caster live.
        // Every breath carries a screen cue matched to its element.
        ENTRIES.put(SignatureKey.FIRE_DRAKE_BREATH, new SignatureEntry(
                "message.runic_races.signature.fire_drake.breath",
                ChatFormatting.RED, true,
                List.of(new SfxSpec(ModSounds.BREATH_INHALE, 0.8f, 1.0f),
                        new SfxSpec(SoundEvents.ENDER_DRAGON_GROWL, 0.9f, 1.3f),
                        new SfxSpec(ModSounds.BREATH_FIRE, 0.6f, 1.0f)),
                List.of(
                        // A hot mouth ring flares and ember pressure rings travel into the flame torrent.
                        new VfxSpec(ParticleTypes.FLAME, 8, 0.32, 0.9, 0.0, 0.0, SignatureEntry.Shape.WAVE),
                        new VfxSpec(ModParticles.EMBER_SCALE, 6, 0.55, 1.8, 0.0, 0.0, SignatureEntry.Shape.WAVE).delayed(5),
                        new VfxSpec(RaceColors.FORGE_EMBER, 6, 0.8, 2.8, 0.0, 0.0, SignatureEntry.Shape.WAVE).delayed(10)
                ),
                CueType.HEAT_SHIMMER, 15, Intensity.MAJOR));

        ENTRIES.put(SignatureKey.ICE_DRAKE_BREATH, new SignatureEntry(
                "message.runic_races.signature.ice_drake.breath",
                ChatFormatting.AQUA, true,
                List.of(new SfxSpec(ModSounds.BREATH_INHALE, 0.8f, 1.2f),
                        new SfxSpec(SoundEvents.ENDER_DRAGON_GROWL, 0.9f, 1.5f),
                        new SfxSpec(ModSounds.BREATH_FROST, 0.5f, 1.0f)),
                List.of(
                        // Frost forms at the mouth, then icy pressure rings punctuate the frozen torrent.
                        new VfxSpec(ModParticles.FROST_MOTE, 6, 0.35, 0.9, 0.0, 0.0, SignatureEntry.Shape.WAVE),
                        new VfxSpec(ParticleTypes.SNOWFLAKE, 6, 0.6, 1.8, 0.0, 0.0, SignatureEntry.Shape.WAVE).delayed(5),
                        new VfxSpec(RaceColors.GLACIAL_CYAN, 6, 0.85, 2.8, 0.0, 0.0, SignatureEntry.Shape.WAVE).delayed(10)
                ),
                CueType.FROST_RIME, 20, Intensity.MAJOR));

        ENTRIES.put(SignatureKey.SEA_SERPEN_BREATH, new SignatureEntry(
                "message.runic_races.signature.sea_serpen.breath",
                ChatFormatting.BLUE, true,
                List.of(new SfxSpec(ModSounds.BREATH_INHALE, 0.8f, 0.9f),
                        new SfxSpec(SoundEvents.ENDER_DRAGON_GROWL, 0.9f, 1.1f),
                        new SfxSpec(ModSounds.BREATH_WATER, 0.7f, 1.0f)),
                List.of(
                        // Sea spray leaves the mouth in three expanding pressure rings.
                        new VfxSpec(ParticleTypes.SPLASH, 8, 0.4, 0.9, 0.0, 0.0, SignatureEntry.Shape.WAVE),
                        new VfxSpec(RaceColors.TIDAL_TEAL, 6, 0.7, 1.8, 0.0, 0.0, SignatureEntry.Shape.WAVE).delayed(5),
                        new VfxSpec(ParticleTypes.SPLASH, 6, 1.0, 2.8, 0.0, 0.0, SignatureEntry.Shape.WAVE).delayed(10)
                ),
                CueType.WIND_STREAK, 12, Intensity.MAJOR));

        ENTRIES.put(SignatureKey.TERRA_DRAKE_BREATH, new SignatureEntry(
                "message.runic_races.signature.terra_drake.breath",
                ChatFormatting.DARK_GREEN, true,
                List.of(new SfxSpec(ModSounds.BREATH_INHALE, 0.8f, 0.7f),
                        new SfxSpec(SoundEvents.ENDER_DRAGON_GROWL, 0.9f, 0.7f),
                        new SfxSpec(ModSounds.BREATH_EARTH, 0.5f, 1.0f)),
                List.of(
                        // Stone dust curls from the jaw before two rock-edged pressure rings join the torrent.
                        new VfxSpec(ParticleTypes.POOF, 6, 0.4, 0.9, 0.0, 0.0, SignatureEntry.Shape.WAVE),
                        new VfxSpec(ModParticles.ROCK_CHIP, 6, 0.65, 1.8, 0.0, 0.0, SignatureEntry.Shape.WAVE).delayed(5),
                        new VfxSpec(ModParticles.ROCK_CHIP, 6, 0.9, 2.8, 0.0, 0.0, SignatureEntry.Shape.WAVE).delayed(10)
                ),
                CueType.SHAKE, 10, Intensity.MAJOR));

        ENTRIES.put(SignatureKey.VOLT_DRAKE_BREATH, new SignatureEntry(
                "message.runic_races.signature.volt_drake.breath",
                ChatFormatting.YELLOW, true,
                List.of(new SfxSpec(ModSounds.BREATH_INHALE, 0.8f, 1.4f),
                        new SfxSpec(SoundEvents.ENDER_DRAGON_GROWL, 0.9f, 1.6f),
                        new SfxSpec(ModSounds.BREATH_SHOCK, 0.5f, 1.0f)),
                List.of(
                        // Electric arcs flash around the mouth and form two gold-edged shock fronts.
                        new VfxSpec(ParticleTypes.ELECTRIC_SPARK, 6, 0.35, 0.9, 0.0, 0.0, SignatureEntry.Shape.WAVE),
                        new VfxSpec(RaceColors.VOLT_GOLD, 6, 0.6, 1.8, 0.0, 0.0, SignatureEntry.Shape.WAVE).delayed(4),
                        new VfxSpec(ParticleTypes.ELECTRIC_SPARK, 6, 0.9, 2.8, 0.0, 0.0, SignatureEntry.Shape.WAVE).delayed(8)
                ),
                // A 4-tick white flash — the lightning strobe.
                CueType.FREEZE_FRAME, 4, Intensity.MAJOR));

        ENTRIES.put(SignatureKey.WIND_WYRM_BREATH, new SignatureEntry(
                "message.runic_races.signature.wind_wyrm.breath",
                ChatFormatting.WHITE, true,
                List.of(new SfxSpec(ModSounds.BREATH_INHALE, 0.9f, 1.1f),
                        new SfxSpec(SoundEvents.ENDER_DRAGON_GROWL, 0.9f, 1.2f),
                        new SfxSpec(ModSounds.BREATH_WIND, 0.6f, 1.0f)),
                List.of(
                        // Three pale compression rings spiral outward into the driving gale.
                        new VfxSpec(ModParticles.GALE_STREAK, 6, 0.4, 0.9, 0.0, 0.0, SignatureEntry.Shape.WAVE),
                        new VfxSpec(ParticleTypes.CLOUD, 6, 0.8, 2.0, 0.0, 0.0, SignatureEntry.Shape.WAVE).delayed(5),
                        new VfxSpec(ModParticles.GALE_STREAK, 6, 1.2, 3.2, 0.0, 0.0, SignatureEntry.Shape.WAVE).delayed(10)
                ),
                // Streaks + FOV kick suit the self-launch better than a camera shake.
                CueType.WIND_STREAK, 12, Intensity.MAJOR));

        // ===== Wing flaps =====
        ENTRIES.put(SignatureKey.SPRITE_WING_FLAP, new SignatureEntry(
                "message.runic_races.signature.sprite.wing_flap",
                ChatFormatting.LIGHT_PURPLE, true,
                List.of(new SfxSpec(ModSounds.WINGS_FLAP_SMALL, 0.2f, 2.0f)),
                List.of(
                        // A tiny shimmer crest sheds a ring of sparks as the gossamer wings lift.
                        new VfxSpec(ParticleTypes.END_ROD, 12, 0.7, 0.4, 0.25, 0.06, SignatureEntry.Shape.ARC),
                        new VfxSpec(ModParticles.FAE_SPARKLE, 10, 0.65, 0.0, 0.65, 0.07, SignatureEntry.Shape.RING).delayed(2)
                ),
                null, 0, Intensity.MINOR));

        ENTRIES.put(SignatureKey.FAERIE_WING_FLAP, new SignatureEntry(
                "message.runic_races.signature.faerie.wing_flap",
                ChatFormatting.LIGHT_PURPLE, true,
                List.of(new SfxSpec(ModSounds.WINGS_FLAP_SMALL, 0.2f, 1.8f)),
                // Fae dust off the wings — distinct from the sprite's end-rod shimmer.
                List.of(
                        // Fae dust outlines the wing stroke before a small starry wake curls away.
                        new VfxSpec(ModParticles.FAE_SPARKLE, 12, 1.15, 0.8, 0.35, 0.09, SignatureEntry.Shape.ARC),
                        new VfxSpec(ParticleTypes.FIREWORK, 8, 0.95, 0.0, 0.95, 0.05, SignatureEntry.Shape.RING_ORBIT).delayed(3)
                ),
                null, 0, Intensity.MINOR));

        ENTRIES.put(SignatureKey.AVIAN_WING_FLAP, new SignatureEntry(
                "message.runic_races.signature.avian.wing_flap",
                ChatFormatting.AQUA, true,
                List.of(new SfxSpec(ModSounds.WINGS_FLAP_FEATHER, 0.3f, 1.4f)),
                List.of(
                        // Feathers sweep along the wing stroke; a separate gust follows the downbeat.
                        new VfxSpec(ModParticles.FEATHER_DOWN, 12, 1.7, 1.0, 0.35, 0.08, SignatureEntry.Shape.ARC),
                        new VfxSpec(ModParticles.GALE_STREAK, 8, 1.55, 0.75, 0.3, 0.11, SignatureEntry.Shape.ARC).delayed(2)
                ),
                null, 0, Intensity.MINOR));

        ENTRIES.put(SignatureKey.WIND_WYRM_WING_FLAP, new SignatureEntry(
                "message.runic_races.signature.wind_wyrm.wing_flap",
                ChatFormatting.WHITE, true,
                List.of(new SfxSpec(ModSounds.WINGS_FLAP_LARGE, 0.5f, 1.2f)),
                List.of(
                        // A broad wind crest and rolling pressure ring emphasize the wyrm's powered downbeat.
                        new VfxSpec(ModParticles.GALE_STREAK, 12, 2.1, 1.0, 0.45, 0.16, SignatureEntry.Shape.ARC),
                        new VfxSpec(ParticleTypes.CLOUD, 10, 1.7, 0.0, 1.7, 0.11, SignatureEntry.Shape.RING).delayed(2),
                        new VfxSpec(ModParticles.GALE_STREAK, 8, 2.3, 0.85, 0.45, 0.13, SignatureEntry.Shape.ARC).delayed(4)
                ),
                null, 0, Intensity.MAJOR));

        // ===== Flight Cancel (MINOR) =====
        ENTRIES.put(SignatureKey.FLIGHT_CANCEL, new SignatureEntry(
                "message.runic_races.signature.flight.cancel",
                ChatFormatting.GRAY, false,
                List.of(new SfxSpec(SoundEvents.PHANTOM_FLAP, 0.3f, 0.8f)),
                // Wings fold: down drifts inward, then settles around the landing.
                List.of(
                        new VfxSpec(ModParticles.FEATHER_DOWN, 10, 1.0, 0.0, 1.0, 0.06, SignatureEntry.Shape.RING_IN),
                        new VfxSpec(ModParticles.FEATHER_DOWN, 6, 0.4, 0.6, 0.4, 0.015).delayed(3)
                ),
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
                        // A golden coin-like aperture snaps shut; fortune rises and scatters in a final crest.
                        new VfxSpec(RaceColors.VOLT_GOLD, 16, 0.6, 0.9, 0.0, 0.025, SignatureEntry.Shape.WAVE),
                        new VfxSpec(ParticleTypes.END_ROD, 20, 0.6, 2.4, 0.6, 0.045, SignatureEntry.Shape.HELIX).delayed(3),
                        new VfxSpec(ParticleTypes.TOTEM_OF_UNDYING, 16, 1.25, 1.8, 1.25, 0.09, SignatureEntry.Shape.DOME).delayed(7)
                ),
                null, 0, Intensity.MAJOR));

        ENTRIES.put(SignatureKey.CELERON_DASH, new SignatureEntry(
                "message.runic_races.celeron.messengers_dash",
                ChatFormatting.YELLOW, true,
                List.of(new SfxSpec(ModSounds.DASH_WHOOSH, 0.7f, 1.0f)),
                List.of(
                        // A launch ring and three short, aim-aligned speed crescents follow the dash.
                        new VfxSpec(ParticleTypes.CLOUD, 16, 1.1, 0.0, 1.1, 0.14, SignatureEntry.Shape.RING),
                        new VfxSpec(ModParticles.GALE_STREAK, 12, 1.1, 0.8, 0.4, 0.18, SignatureEntry.Shape.ARC).delayed(2),
                        new VfxSpec(ModParticles.FEATHER_DOWN, 12, 1.3, 0.9, 0.4, 0.1, SignatureEntry.Shape.ARC).delayed(5),
                        new VfxSpec(ModParticles.GALE_STREAK, 12, 1.4, 0.7, 0.4, 0.15, SignatureEntry.Shape.ARC).delayed(8)
                ),
                CueType.WIND_STREAK, 8, Intensity.MAJOR));

        ENTRIES.put(SignatureKey.MAGI_OVERFLOW, new SignatureEntry(
                "message.runic_races.magi.arcane_overflow",
                ChatFormatting.LIGHT_PURPLE, true,
                // The circle is drawn (chime), then the vessel overloads (cast).
                List.of(new SfxSpec(SoundEvents.AMETHYST_BLOCK_CHIME, 0.6f, 0.9f),
                        new SfxSpec(SoundEvents.EVOKER_CAST_SPELL, 0.5f, 1.2f).delayed(5)),
                List.of(
                        // A drawn mana sigil overloads into an arcane shell and a spreading outer pulse.
                        new VfxSpec(ModParticles.RUNE_GLYPH, 20, 1.4, 0.0, 1.4, 0.015, SignatureEntry.Shape.SIGIL),
                        new VfxSpec(ModParticles.ARCANE_GLINT, 20, 1.7, 2.2, 1.7, 0.13, SignatureEntry.Shape.DOME).delayed(4),
                        new VfxSpec(RaceColors.ARCANE_AZURE, 16, 2.8, 0.0, 2.8, 0.16, SignatureEntry.Shape.RING).delayed(8)
                ),
                CueType.VIGNETTE_PULSE, 15, Intensity.MAJOR));

        ENTRIES.put(SignatureKey.VALEN_STAND, new SignatureEntry(
                "message.runic_races.valen.unbreakable_stand",
                ChatFormatting.GOLD, true,
                // Brace first, then the anvil-clang of a stance that won't move.
                List.of(new SfxSpec(ModSounds.SHIELD_BRACE, 0.6f, 0.9f),
                        new SfxSpec(SoundEvents.ANVIL_LAND, 0.4f, 0.9f).delayed(4)),
                List.of(
                        // An iron guard locks ahead of the planted stance as ground fractures spread beneath it.
                        new VfxSpec(RaceColors.IRON_GRAY, 24, 1.4, 2.0, 0.9, 0.015, SignatureEntry.Shape.SHIELD),
                        new VfxSpec(ModParticles.ROCK_CHIP, 16, 2.1, 0.0, 2.1, 0.05, SignatureEntry.Shape.SPOKES).delayed(3),
                        new VfxSpec(ParticleTypes.CRIT, 16, 1.5, 2.1, 0.9, 0.025, SignatureEntry.Shape.SHIELD).delayed(7)
                ),
                CueType.SHAKE, 6, Intensity.MAJOR));

        // ----- Elven: jewel-tone arcana — graceful rises and implosions -----
        ENTRIES.put(SignatureKey.HIGH_ELF_REFLEX, new SignatureEntry(
                "message.runic_races.high_elf.arcane_reflex",
                ChatFormatting.LIGHT_PURPLE, true,
                List.of(new SfxSpec(SoundEvents.AMETHYST_CLUSTER_BREAK, 0.6f, 1.2f),
                        new SfxSpec(SoundEvents.EXPERIENCE_ORB_PICKUP, 0.5f, 1.8f).delayed(6)),
                List.of(
                        // Jewel light climbs the body, converges, and flashes into a clean reflex guard.
                        new VfxSpec(ModParticles.ARCANE_GLINT, 20, 0.8, 2.3, 0.8, 0.055, SignatureEntry.Shape.HELIX),
                        new VfxSpec(RaceColors.ARCANE_AZURE, 16, 1.8, 0.0, 1.8, 0.2, SignatureEntry.Shape.RING_IN).delayed(4),
                        new VfxSpec(ParticleTypes.ENCHANT, 16, 1.1, 1.8, 0.8, 0.04, SignatureEntry.Shape.SHIELD).delayed(8)
                ),
                null, 0, Intensity.MAJOR));

        ENTRIES.put(SignatureKey.DARK_ELF_SHADOWMELD, new SignatureEntry(
                "message.runic_races.dark_elf.shadowmeld",
                ChatFormatting.DARK_PURPLE, true,
                // Shadows gather first; the sculk-click is the soft "gone" beat.
                List.of(new SfxSpec(SoundEvents.SOUL_ESCAPE, 0.5f, 0.6f),
                        new SfxSpec(SoundEvents.SCULK_CLICKING, 0.5f, 0.8f).delayed(8)),
                List.of(
                        // An indigo spiral gathers before shadows collapse and leave a low ink veil.
                        new VfxSpec(RaceColors.DUSK_INDIGO, 16, 0.8, 2.0, 0.8, 0.03, SignatureEntry.Shape.HELIX),
                        new VfxSpec(ModParticles.SHADOW_WISP, 20, 1.6, 0.0, 1.6, 0.16, SignatureEntry.Shape.RING_IN).delayed(4),
                        new VfxSpec(ParticleTypes.SQUID_INK, 16, 0.85, 1.4, 0.85, 0.025, SignatureEntry.Shape.DOME).delayed(9)
                ),
                CueType.VIGNETTE_PULSE, 20, Intensity.MAJOR));

        ENTRIES.put(SignatureKey.MOON_ELF_VEIL, new SignatureEntry(
                "message.runic_races.moon_elf.moonlit_veil",
                ChatFormatting.AQUA, true,
                List.of(new SfxSpec(ModSounds.MOON_VEIL, 0.7f, 1.0f)),
                List.of(
                        // A silver lunar seal sends a spiral upward, then settles into a slivered moonlit veil.
                        new VfxSpec(RaceColors.SILVER_MOON, 16, 1.4, 0.0, 1.4, 0.012, SignatureEntry.Shape.SIGIL),
                        new VfxSpec(ParticleTypes.END_ROD, 20, 0.75, 2.5, 0.75, 0.04, SignatureEntry.Shape.HELIX).delayed(3),
                        new VfxSpec(ModParticles.MOON_SLIVER, 20, 1.5, 2.0, 1.5, 0.025, SignatureEntry.Shape.DOME).delayed(9)
                ),
                CueType.MOON_GLOW, 30, Intensity.MAJOR));

        ENTRIES.put(SignatureKey.BLOOD_ELF_FRENZY, new SignatureEntry(
                "message.runic_races.blood_elf.blood_frenzy",
                ChatFormatting.DARK_RED, true,
                // Two heartbeats: the draw, then the eruption.
                List.of(new SfxSpec(SoundEvents.WARDEN_HEARTBEAT, 0.7f, 1.1f),
                        new SfxSpec(SoundEvents.WARDEN_HEARTBEAT, 0.8f, 1.3f).delayed(6),
                        new SfxSpec(SoundEvents.WITCH_DRINK, 0.5f, 0.8f).delayed(6)),
                List.of(
                        // Two heartbeats draw blood inward, raise it in a helix, then flare around the claws.
                        new VfxSpec(RaceColors.CRIMSON_BLOOD, 16, 1.5, 0.0, 1.5, 0.2, SignatureEntry.Shape.RING_IN),
                        new VfxSpec(RaceColors.CRIMSON_BLOOD, 20, 0.7, 2.2, 0.7, 0.06, SignatureEntry.Shape.HELIX).delayed(6),
                        new VfxSpec(ParticleTypes.DAMAGE_INDICATOR, 16, 1.2, 1.0, 0.4, 0.075, SignatureEntry.Shape.ARC).delayed(10)
                ),
                CueType.HEARTBEAT_FLASH, 20, Intensity.MAJOR));

        ENTRIES.put(SignatureKey.ICE_ELF_FROSTBIND, new SignatureEntry(
                "message.runic_races.ice_elf.frostbind",
                ChatFormatting.AQUA, true,
                // The cold gathers silently, then shatters outward.
                List.of(new SfxSpec(SoundEvents.POWDER_SNOW_BREAK, 0.5f, 0.7f),
                        new SfxSpec(SoundEvents.GLASS_BREAK, 0.5f, 1.4f).delayed(6)),
                List.of(
                        // Cold gathers inward before two increasingly broad frost rings reveal the binding nova.
                        new VfxSpec(ModParticles.FROST_MOTE, 16, 1.5, 0.0, 1.5, 0.17, SignatureEntry.Shape.RING_IN),
                        new VfxSpec(RaceColors.GLACIAL_CYAN, 20, 2.5, 0.0, 2.5, 0.15, SignatureEntry.Shape.RING).delayed(5),
                        new VfxSpec(ParticleTypes.SNOWFLAKE, 20, 4.0, 0.0, 4.0, 0.08, SignatureEntry.Shape.RING).delayed(10)
                ),
                CueType.FROST_RIME, 20, Intensity.MAJOR));

        // ----- Dwarven: ground-hugging bursts of stone and craft -----
        ENTRIES.put(SignatureKey.DEEP_ONE_TREMOR, new SignatureEntry(
                "message.runic_races.deep_one.tremorsense",
                ChatFormatting.GRAY, true,
                List.of(new SfxSpec(ModSounds.TREMOR_PULSE, 0.8f, 1.0f)),
                List.of(
                        // A ground sigil sends three seismic rings outward through the detected area.
                        new VfxSpec(RaceColors.VOLT_GOLD, 16, 1.5, 0.0, 1.5, 0.015, SignatureEntry.Shape.SIGIL),
                        new VfxSpec(ModParticles.ROCK_CHIP, 12, 2.3, 0.0, 2.3, 0.08, SignatureEntry.Shape.RING).delayed(3),
                        new VfxSpec(ParticleTypes.POOF, 12, 3.8, 0.0, 3.8, 0.1, SignatureEntry.Shape.RING).delayed(6),
                        new VfxSpec(RaceColors.VOLT_GOLD, 16, 5.5, 0.0, 5.5, 0.075, SignatureEntry.Shape.RING).delayed(10)
                ),
                CueType.SHAKE, 5, Intensity.MAJOR));

        ENTRIES.put(SignatureKey.FROST_ONE_RESOLVE, new SignatureEntry(
                "message.runic_races.frost_one.glacial_resolve",
                ChatFormatting.AQUA, true,
                // Ice crystallizes, then the braced shield-thud of settled resolve.
                List.of(new SfxSpec(SoundEvents.GLASS_PLACE, 0.6f, 0.7f),
                        new SfxSpec(SoundEvents.SHIELD_BLOCK, 0.6f, 0.9f).delayed(8)),
                List.of(
                        // Ice erupts from the boots and crystallizes in a shell before its outer rime settles.
                        new VfxSpec(ModParticles.FROST_MOTE, 16, 0.8, 1.7, 0.8, 0.06, SignatureEntry.Shape.BURST_UP),
                        new VfxSpec(RaceColors.GLACIAL_CYAN, 24, 1.45, 1.8, 1.45, 0.025, SignatureEntry.Shape.DOME).delayed(4),
                        new VfxSpec(ParticleTypes.SNOWFLAKE, 16, 1.8, 0.0, 1.8, 0.035, SignatureEntry.Shape.RING).delayed(9)
                ),
                CueType.FROST_RIME, 15, Intensity.MAJOR));

        ENTRIES.put(SignatureKey.IRON_ONE_SHIELD_WALL, new SignatureEntry(
                "message.runic_races.iron_one.shield_wall",
                ChatFormatting.GRAY, true,
                List.of(new SfxSpec(ModSounds.SHIELD_BRACE, 0.8f, 1.0f)),
                List.of(
                        // A broad iron plane braces ahead; two glint passes show the wall taking its full weight.
                        new VfxSpec(RaceColors.IRON_GRAY, 24, 1.65, 2.1, 0.95, 0.008, SignatureEntry.Shape.SHIELD),
                        new VfxSpec(ParticleTypes.CRIT, 16, 1.6, 0.0, 1.6, 0.025, SignatureEntry.Shape.SIGIL).delayed(3),
                        new VfxSpec(ParticleTypes.ENCHANTED_HIT, 16, 1.75, 2.2, 0.95, 0.02, SignatureEntry.Shape.SHIELD).delayed(7)
                ),
                CueType.SHAKE, 4, Intensity.MAJOR));

        ENTRIES.put(SignatureKey.SKY_ONE_LEAP, new SignatureEntry(
                "message.runic_races.sky_one.mountain_leap",
                ChatFormatting.WHITE, true,
                // The crouch-and-spring, then the whoosh of leaving the ground.
                List.of(new SfxSpec(SoundEvents.GOAT_LONG_JUMP, 0.7f, 1.0f),
                        new SfxSpec(ModSounds.DASH_WHOOSH, 0.5f, 0.8f).delayed(2)),
                List.of(
                        // Stone bursts from the launch point, then an ascending gust and feather crown trace the leap.
                        new VfxSpec(ModParticles.ROCK_CHIP, 16, 1.4, 0.0, 1.4, 0.1, SignatureEntry.Shape.RING),
                        new VfxSpec(ParticleTypes.CLOUD, 20, 0.6, 2.8, 0.6, 0.19, SignatureEntry.Shape.BURST_UP).delayed(2),
                        new VfxSpec(ModParticles.GALE_STREAK, 16, 1.35, 0.0, 1.35, 0.12, SignatureEntry.Shape.RING).delayed(6)
                ),
                CueType.WIND_STREAK, 10, Intensity.MAJOR));

        // ----- Bestial: organic, directional, physical -----
        ENTRIES.put(SignatureKey.ARACHNID_WEB_SNARE, new SignatureEntry(
                "message.runic_races.arachnid.web_snare",
                ChatFormatting.WHITE, true,
                List.of(new SfxSpec(ModSounds.WEB_SNARE, 0.8f, 1.0f),
                        new SfxSpec(SoundEvents.SPIDER_AMBIENT, 0.5f, 1.2f).delayed(4)),
                List.of(
                        // Eight radial strands weave the snare, then two silk rings close its outer edge.
                        new VfxSpec(ModParticles.WEB_STRAND, 24, 5.0, 0.0, 5.0, 0.01, SignatureEntry.Shape.SPOKES),
                        new VfxSpec(ModParticles.WEB_STRAND, 16, 3.5, 0.0, 3.5, 0.012, SignatureEntry.Shape.RING).delayed(3),
                        new VfxSpec(ModParticles.WEB_STRAND, 20, 5.0, 0.0, 5.0, 0.008, SignatureEntry.Shape.RING).delayed(7)
                ),
                null, 0, Intensity.MAJOR));

        ENTRIES.put(SignatureKey.AVIAN_WIND_BURST, new SignatureEntry(
                "message.runic_races.avian.wind_burst",
                ChatFormatting.AQUA, true,
                // A double wing-beat drives the launch.
                List.of(new SfxSpec(SoundEvents.PHANTOM_FLAP, 0.7f, 1.2f),
                        new SfxSpec(SoundEvents.PHANTOM_FLAP, 0.7f, 1.4f).delayed(3),
                        new SfxSpec(ModSounds.DASH_WHOOSH, 0.5f, 1.3f).delayed(3)),
                List.of(
                        // Two wing-shaped gust arcs drive the launch; a feather ring follows the updraft.
                        new VfxSpec(ModParticles.GALE_STREAK, 16, 1.9, 1.1, 0.5, 0.16, SignatureEntry.Shape.ARC),
                        new VfxSpec(ParticleTypes.CLOUD, 20, 0.6, 2.5, 0.6, 0.21, SignatureEntry.Shape.BURST_UP).delayed(3),
                        new VfxSpec(ModParticles.FEATHER_DOWN, 16, 1.8, 0.0, 1.8, 0.08, SignatureEntry.Shape.RING).delayed(7)
                ),
                CueType.WIND_STREAK, 8, Intensity.MAJOR));

        ENTRIES.put(SignatureKey.CANINE_HOWL, new SignatureEntry(
                "message.runic_races.canine.howl_of_the_pack",
                ChatFormatting.GREEN, true,
                List.of(new SfxSpec(ModSounds.HOWL_PACK, 0.9f, 1.0f)),
                List.of(
                        // Three widening sound fronts make the pack call visible above a grounding ripple.
                        new VfxSpec(ParticleTypes.NOTE, 12, 0.5, 0.9, 0.0, 0.1, SignatureEntry.Shape.WAVE),
                        new VfxSpec(ParticleTypes.POOF, 16, 2.0, 0.0, 2.0, 0.15, SignatureEntry.Shape.RING).delayed(3),
                        new VfxSpec(ModParticles.GALE_STREAK, 16, 1.3, 2.6, 0.0, 0.17, SignatureEntry.Shape.WAVE).delayed(6),
                        new VfxSpec(ParticleTypes.POOF, 16, 2.0, 4.5, 0.0, 0.19, SignatureEntry.Shape.WAVE).delayed(12)
                ),
                null, 0, Intensity.MAJOR));

        ENTRIES.put(SignatureKey.FELINE_POUNCE, new SignatureEntry(
                "message.runic_races.feline.pounce",
                ChatFormatting.GOLD, true,
                // The hiss of the crouch, then claws at the moment of the leap.
                List.of(new SfxSpec(SoundEvents.CAT_HISS, 0.5f, 1.1f),
                        new SfxSpec(ModSounds.POUNCE_STRIKE, 0.7f, 1.0f).delayed(3)),
                List.of(
                        // The crouch coils inward before two claw crescents rake along the moving lunge.
                        new VfxSpec(ParticleTypes.CLOUD, 16, 1.1, 0.0, 1.1, 0.14, SignatureEntry.Shape.RING_IN),
                        new VfxSpec(ModParticles.GALE_STREAK, 20, 1.5, 1.0, 0.6, 0.24, SignatureEntry.Shape.ARC).delayed(3),
                        new VfxSpec(ParticleTypes.CRIT, 16, 1.7, 0.8, 0.6, 0.18, SignatureEntry.Shape.ARC).delayed(6)
                ),
                null, 0, Intensity.MAJOR));

        ENTRIES.put(SignatureKey.KITSUNE_FOXFIRE, new SignatureEntry(
                "message.runic_races.kitsune.foxfire_illusion",
                ChatFormatting.LIGHT_PURPLE, true,
                // Foxfire catches, then a chime as the illusion completes and the fox is gone.
                List.of(new SfxSpec(ModSounds.FOXFIRE_IGNITE, 0.8f, 1.0f),
                        new SfxSpec(SoundEvents.AMETHYST_BLOCK_CHIME, 0.5f, 1.5f).delayed(6)),
                List.of(
                        // Spirit foxfire circles, spirals up the illusion, and flares into a bright vanishing shell.
                        new VfxSpec(ModParticles.FOXFIRE, 16, 1.1, 0.0, 1.1, 0.13, SignatureEntry.Shape.RING_ORBIT),
                        new VfxSpec(ParticleTypes.SOUL_FIRE_FLAME, 20, 0.8, 2.1, 0.8, 0.05, SignatureEntry.Shape.HELIX).delayed(3),
                        new VfxSpec(ModParticles.FOXFIRE, 16, 1.5, 1.9, 1.5, 0.07, SignatureEntry.Shape.DOME).delayed(8)
                ),
                null, 0, Intensity.MAJOR));

        ENTRIES.put(SignatureKey.SERPEN_SHED, new SignatureEntry(
                "message.runic_races.serpen.shed_skin",
                ChatFormatting.GREEN, true,
                // The skin loosens (squish), then the hiss as the serpen slips free of it.
                List.of(new SfxSpec(SoundEvents.SLIME_SQUISH, 0.6f, 0.8f),
                        new SfxSpec(SoundEvents.CAT_HISS, 0.5f, 0.5f).delayed(6)),
                List.of(
                        // A scaled coil rises before the old skin peels outward and venom falls from its edge.
                        new VfxSpec(RaceColors.VERDANT_GREEN, 20, 0.7, 1.8, 0.7, 0.025, SignatureEntry.Shape.HELIX),
                        new VfxSpec(ParticleTypes.ITEM_SLIME, 16, 1.1, 1.7, 1.1, 0.04, SignatureEntry.Shape.DOME).delayed(5),
                        new VfxSpec(ModParticles.VENOM_DRIP, 16, 1.35, 0.0, 1.35, 0.025, SignatureEntry.Shape.RING).delayed(10)
                ),
                CueType.WIND_STREAK, 6, Intensity.MAJOR));

        // ----- Faeborne: playful swirls, blink-outs, pastel pops -----
        ENTRIES.put(SignatureKey.CHANGELING_MIRROR, new SignatureEntry(
                "message.runic_races.changeling.mirror_shift",
                ChatFormatting.LIGHT_PURPLE, true,
                // The mirror forms, then shatters as the new face steps through.
                List.of(new SfxSpec(ModSounds.MIRROR_SHIFT, 0.8f, 1.0f),
                        new SfxSpec(ModSounds.MIRROR_SHATTER, 0.7f, 1.0f).delayed(4)),
                List.of(
                        // Shards orbit into a upright mirror, which breaks into a descending shell of facets.
                        new VfxSpec(ModParticles.MIRROR_SHARD, 16, 1.2, 0.0, 1.2, 0.12, SignatureEntry.Shape.RING_ORBIT),
                        new VfxSpec(ModParticles.FAE_SPARKLE, 20, 1.1, 2.2, 0.75, 0.015, SignatureEntry.Shape.SHIELD).delayed(3),
                        new VfxSpec(ModParticles.MIRROR_SHARD, 20, 1.5, 2.0, 1.5, 0.12, SignatureEntry.Shape.DOME).delayed(7)
                ),
                CueType.FREEZE_FRAME, 6, Intensity.MAJOR));

        ENTRIES.put(SignatureKey.DRYAD_BLOOM, new SignatureEntry(
                "message.runic_races.dryad.verdant_bloom",
                ChatFormatting.GREEN, true,
                List.of(new SfxSpec(ModSounds.BLOOM_VERDANT, 0.9f, 1.0f)),
                List.of(
                        // Petals trace a living garden seal; rising pollen blooms outward into its full canopy.
                        new VfxSpec(ModParticles.LEAF_PETAL, 20, 2.5, 0.0, 2.5, 0.02, SignatureEntry.Shape.SIGIL),
                        new VfxSpec(RaceColors.VERDANT_GREEN, 16, 1.0, 2.2, 1.0, 0.04, SignatureEntry.Shape.HELIX).delayed(4),
                        new VfxSpec(ModParticles.POLLEN_MOTE, 20, 2.8, 2.1, 2.8, 0.035, SignatureEntry.Shape.DOME).delayed(9)
                ),
                null, 0, Intensity.MAJOR));

        ENTRIES.put(SignatureKey.SPRITE_PHASE, new SignatureEntry(
                "message.runic_races.sprite.phase_shift",
                ChatFormatting.LIGHT_PURPLE, true,
                // Blink-out, then the arrival chime rings where the sprite reappears.
                List.of(new SfxSpec(SoundEvents.ENDERMAN_TELEPORT, 0.4f, 1.6f),
                        new SfxSpec(SoundEvents.BELL_BLOCK, 0.4f, 2.0f).delayed(4)),
                List.of(
                        // A sparkle coil collapses at departure; a bright spiral blooms at the live arrival position.
                        new VfxSpec(ModParticles.FAE_SPARKLE, 20, 1.4, 0.0, 1.4, 0.22, SignatureEntry.Shape.RING_IN),
                        new VfxSpec(ParticleTypes.END_ROD, 16, 0.6, 1.6, 0.6, 0.06, SignatureEntry.Shape.HELIX).delayed(3),
                        new VfxSpec(ModParticles.FAE_SPARKLE, 16, 1.2, 1.5, 1.2, 0.1, SignatureEntry.Shape.DOME).delayed(7)
                ),
                null, 0, Intensity.MAJOR));

        ENTRIES.put(SignatureKey.NYMPH_CHARM, new SignatureEntry(
                "message.runic_races.nymph.sirens_charm",
                ChatFormatting.AQUA, true,
                List.of(new SfxSpec(ModSounds.CHARM_SONG, 0.9f, 1.0f)),
                List.of(
                        // Notes rise in a verse while two sea-green wavefronts carry the charm outward.
                        new VfxSpec(ParticleTypes.NOTE, 16, 0.75, 2.2, 0.75, 0.04, SignatureEntry.Shape.HELIX),
                        new VfxSpec(RaceColors.TIDAL_TEAL, 16, 0.9, 1.5, 0.0, 0.11, SignatureEntry.Shape.WAVE).delayed(4),
                        new VfxSpec(RaceColors.TIDAL_TEAL, 16, 1.8, 3.8, 0.0, 0.13, SignatureEntry.Shape.WAVE).delayed(8),
                        new VfxSpec(ParticleTypes.HEART, 8, 1.25, 1.8, 1.25, 0.04, SignatureEntry.Shape.DOME).delayed(12)
                ),
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
                        // Two hungry circles drain inward, leaving a climbing thread of stolen life.
                        new VfxSpec(ParticleTypes.SCULK_SOUL, 20, 2.0, 0.0, 2.0, 0.18, SignatureEntry.Shape.RING_IN),
                        new VfxSpec(ParticleTypes.CRIMSON_SPORE, 16, 1.6, 0.0, 1.6, 0.13, SignatureEntry.Shape.RING_IN).delayed(6),
                        new VfxSpec(ModParticles.SOUL_WISP, 16, 0.65, 1.9, 0.65, 0.035, SignatureEntry.Shape.HELIX).delayed(11)
                ),
                null, 0, Intensity.MAJOR));

        ENTRIES.put(SignatureKey.SKELETON_CONSCRIPT, new SignatureEntry(
                "message.runic_races.skeleton.conscript_the_dead",
                ChatFormatting.WHITE, true,
                // The circle is called, dirt breaks, then bone knits together.
                List.of(new SfxSpec(SoundEvents.SKELETON_AMBIENT, 0.6f, 0.8f),
                        new SfxSpec(SoundEvents.BONE_BLOCK_BREAK, 0.7f, 0.7f).delayed(4)),
                List.of(
                        // A grave sigil opens, bone erupts, and soul threads knit the summoned servants.
                        new VfxSpec(ModParticles.RUNE_GLYPH, 20, 1.8, 0.0, 1.8, 0.012, SignatureEntry.Shape.SIGIL),
                        new VfxSpec(ModParticles.BONE_CHIP, 20, 1.0, 1.5, 1.0, 0.12, SignatureEntry.Shape.BURST_UP).delayed(4),
                        new VfxSpec(RaceColors.SOUL_VIOLET, 16, 0.7, 2.3, 0.7, 0.045, SignatureEntry.Shape.HELIX).delayed(9)
                ),
                null, 0, Intensity.MAJOR));

        ENTRIES.put(SignatureKey.REAPER_HARVEST, new SignatureEntry(
                "message.runic_races.reaper.soul_harvest",
                ChatFormatting.DARK_PURPLE, true,
                List.of(new SfxSpec(ModSounds.HARVEST_SOUL, 0.9f, 1.0f)),
                List.of(
                        // A scythe arc sweeps the harvest before souls drain inward and wind up the reaper.
                        new VfxSpec(ModParticles.SOUL_WISP, 20, 2.7, 1.0, 0.5, 0.16, SignatureEntry.Shape.ARC),
                        new VfxSpec(ParticleTypes.SOUL, 20, 3.2, 0.0, 3.2, 0.22, SignatureEntry.Shape.RING_IN).delayed(4),
                        new VfxSpec(ModParticles.SOUL_WISP, 20, 0.8, 2.2, 0.8, 0.05, SignatureEntry.Shape.HELIX).delayed(10)
                ),
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
                List.of(new VfxSpec(ModParticles.ARCANE_GLINT, 10, 0.7, 0.0, 0.7, 0.12, SignatureEntry.Shape.RING_IN)),
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
                // A glare halo across the view — WAVE is the only shape anchored at eye height.
                List.of(new VfxSpec(ParticleTypes.END_ROD, 12, 0.55, 0.8, 0.0, 0.02, SignatureEntry.Shape.WAVE)),
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
