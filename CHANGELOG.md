# Changelog

All notable changes to Runic Races are documented here.
The format is based on [Keep a Changelog](https://keepachangelog.com/).

## [1.2.0] — 2026-07-01 — Full Audit Remediation

Every finding from the full bug/stability/balance audit (`AUDIT_REPORT.md`), fixed.

### Fixed — critical
- **Skeleton's Conscript the Dead now summons friendly Grave Servants** instead of hostile
  vanilla skeletons (`conscript_the_dead.json` pointed at `minecraft:skeleton`; the
  purpose-built ally entity was never referenced).
- **Arachnid's Web Snare trap actually triggers.** The trap block used `stepOn`, which never
  fires for a collision-less block; it now uses `entityInside` (pressure-plate pattern).
  Trap lifetime raised from 10s to 60s.
- **Crash hardening:** `check_interval: 0` in a third-party datapack power no longer causes a
  divide-by-zero server crash (`biome_affinity` / `scaling_attribute` clamp to ≥1).

### Fixed — broken weaknesses & passives
- **Canine and Dryad forest affinity works** — `forge:is_forest` does not exist in Forge
  1.20.1; both now use `minecraft:is_forest`, and `biome_affinity` warns once when a
  referenced biome tag is defined by nothing.
- **Lightweight knockback weaknesses are real.** Negative `knockback_resistance` modifiers
  clamp to 0 and did nothing; Celeron (1.3×), Avian (1.4×), Faerie (1.4×), and Sprite (1.5×)
  now take amplified knockback via a `LivingKnockBackEvent` handler driven by
  `RaceRegistry` metadata.
- **Moon Elf night regeneration exists** (heals half a heart every 5s at night) — the old
  subpower was a configured 0.0/0.0 no-op that the tooltip advertised anyway.

### Fixed — misfiring mechanics
- **Valen's shoulder-check is gated:** direct melee only (no arrows/tridents), hostile
  targets only (no players/pets/villagers), once per sprint. Previously it fired on every
  sprint attack against anything, at any range, enabling juggle-locks.
- **Reaper revival no longer burns its 30-minute cooldown on failure.** The cooldown is
  consumed only on a successful revival; the lifetime attempt counter (which silently ate
  legitimate revivals) is gone; the missing-dimension failure path now shows the rejection
  cue; the tooltip's "10 minutes" now says 30.
- **Tremor abilities no longer eat their cooldown while doing nothing** — the hidden
  sneaking/stone/underground gate inside `tremor_ping` is removed (Deep One Tremorsense,
  Terra Drake Seismic Breath).
- **Magi's Arcane Overflow requires 30 mana to activate** (new `has_mana` gate), instead of
  firing at 0 mana and draining into the negative.
- **Nine Lives no longer wastes its charge on void damage or `/kill`** (invulnerability-
  bypassing sources are excluded — Resistance V couldn't save you anyway).

### Changed — targeting & new mechanics
- **New `runic_races:afflict_hostiles` action**: all 11 offensive AoE actives (Rune of
  Warding, Arcane Overflow, Arcane Reflex, Frostbind, Foxfire Illusion, Siren's Charm,
  Infernal Wrath, Spectral Phase, Soul Harvest, Verdant Bloom, Web Snare) now hit only
  hostile mobs — never party members, pets, or villagers. Rune of Warding is finally the
  party ward its description promised.
- **Doc-promised mechanics implemented:** Arachnid/Serpen venomous melee (Poison I on
  direct hits), Blood Elf 20% melee lifesteal (taxed by their own −30% healing), Wraith's
  Spectral Phase drains 1 heart back, Wind Wyrm gains the advertised slow-fall drift when
  airborne and not gliding.

### Balance
- Demon: night attack bonus removed (fire/lava immunity stays the marquee).
- Sprite: impact rating corrected to 2 (its knockback weakness is now real).
- Serpen: Shed Skin cooldown 30s → 45s. Canine: hunger drain ×1.25 → ×1.4.
- Changeling: self-cancelling −0.5 luck weakness replaced with −10% attack damage.
- Primian: adaptation kill-stacks now require *different* mob types (no spawner farming),
  matching the documented behavior.

### Polish
- Dark Elf's daytime attack malus only applies under open sky (new
  `require_sky_exposure` option on `scaling_attribute`), matching its cave-dweller fantasy.
- Flap cooldown HUD icons now sweep the full dial (flap resource max = actual cooldown).
- Invisibility actives (Wraith/Kitsune/Serpen) no longer leak potion swirls from their
  companion buffs.
- VFX grammar normalized: Demon Wrath / Faerie Glamour / Galeforce Breath re-tiered from
  mis-labeled "mythic" to Major (≤60 particles); dragon-breath cone density trimmed;
  under-budget actives (Pounce, Mirror Shift, Wind Burst, Shed Skin, Shadowmeld, Mountain
  Leap) raised into the 30–60 band; sun/claustrophobia HUD runes now match the actual
  penalty conditions.
- Weakness damage-type coverage widened (tridents/thrown/stings for High Elf & Kitsune,
  fireballs for Ice Elf & Arachnid); Forge Blessing never duplicates an existing Unbreaking
  enchantment; Serpen's shed-skin sound is no longer a spider hiss.
- Removed 24 stale pre-overhaul origin models and an orphaned wing texture; fixed the
  example datapack's family id (`family_faeborne`); `origins` dependency now requires 1.10+;
  added a LICENSE file; removed the machine-specific `org.gradle.java.home` pin.

### Added — diagnostics
- `/runicraces validate` (op): cross-checks the race registry against loaded origins and
  powers — run it after editing race datapacks.
- `/runicraces state <player>` (op): dumps a player's effective race state (flags, cooldown
  resources, adaptation stacks, revival cooldown, integration sync).
- Five JUnit regression suites: registry↔data parity, cooldown resource IDs, power-JSON
  lint (tags, intervals, targeting), VFX budgets, and lang-duration consistency.

## [1.1.0] — 2026-06-15 — Audit Hardening & Build Fixes

A focused follow-up to the 1.0.0 overhaul: a full project audit pass plus the fixes it
surfaced. No gameplay or roster changes — every race, power, height, and balance value is
unchanged.

### Fixed
- **Resource gating no longer fails open on API drift.** When Iron's Spellbooks (mana) or
  Feather's Mod (stamina) is installed but its API call throws at runtime, `ManaHelper` /
  `StaminaHelper` now honor `failClosedWhenResourceModMissing` instead of always reporting
  "unlimited" (and log a single warning). Previously a version mismatch could silently hand
  out free mana-/stamina-gated abilities.
- **Trap auto-cleanup gate.** `TrapMarkerBlockEntity` used a broken bitmask (`& 19`) where a
  once-per-second check was intended; replaced with `% 20`.
- **Startup log accuracy.** Common setup now logs the real race/family counts (from
  `RaceRegistry`) instead of a hardcoded "24 races".

### Changed
- **Build:** Apotheosis now resolves via CurseMaven (`curse.maven:apotheosis-313970:6461960`),
  so a clean checkout compiles without a local Apotheosis jar.
- **Repo hygiene:** removed committed decompiled dependency `.class` files (`/io/`,
  `Dependencies/io/`) from version control and added them to `.gitignore`.

### Docs
- Version bumped to 1.1.0 across `gradle.properties`, `README.md`, `CLAUDE.md`, and the
  CurseForge description; corrected the README build note (Apotheosis/Pehkui/Feathers all
  resolve from Maven automatically).

## [1.0.0] — 2026-06-15 — The Great Overhaul

A complete, from-scratch replacement of the entire race roster. Every race is new, and
each is built on a single consistent contract: **one active ability, one passive strength,
and one weakness**, balanced to net ≈ 0.

### Added
- **7 families, 37 races** (replacing the old 6 families / 24 races):
  - **Human** — Primian, Celeron, Magi, Valen
  - **Elven** — High Elf, Dark Elf, Moon Elf, Blood Elf, Ice Elf
  - **Dwarven** — Deep One, Forge One, Frost One, Iron One, Sky One, Runic One
  - **Bestial** — Arachnid, Avian, Canine, Feline, Kitsune, Serpen
  - **Faeborne** — Changeling, Dryad, Sprite, Nymph, Faerie
  - **Undead** — Zombie, Skeleton, Wraith, Demon, Reaper
  - **Draconic** — Fire Drake, Ice Drake, Sea Serpen, Terra Drake, Volt Drake, Wind Wyrm
- **Elemental breath weapons**: `ConeBreathAction` gained a 6-element parameter
  (fire / frost / water / earth / shock / wind), powering all six drakes with distinct
  rider effects (ignite, freeze, drown-slow, mining-fatigue, stun, levitation-knockback).
- **New signature moments** (SFX + VFX + banner + screen cue): Reaper revival, Wraith
  Phase, Demon Wrath, Feline Nine Lives, Forge Blessing, Rune of Warding, Faerie Glamour,
  the six drake breaths, and wing-flap cues for Sprite / Faerie / Avian / Wind Wyrm.
- **Flight**: new `FAERIE_WINGS` and `AVIAN_WINGS`; flap-flight for Sprite, Faerie, Avian,
  and Wind Wyrm; gliding wings for the four elemental drakes.
- **Faeborne family accent** (teal) added as the 7th HUD color.
- 37 race icons sourced from the design art, plus per-race origins, powers, and a full
  localization pass; family icons reuse their first race's icon.
- **Per-race Pehkui heights** for all 37 races, clustered by family so a party reads at a
  glance: tiny fae (Sprite/Faerie 0.45–0.50), short stocky dwarves (0.68–0.74), tall lithe
  elves (1.04–1.08), looming undead (Demon 1.20, Reaper 1.10), and large drakes
  (up to Terra Drake 1.30). Range 0.45–1.30, applied via the registry-driven `race_scale` type.
- `tools/` static generators (`generate_races.py`, `generate_icons.py`, `build_lang.py`)
  that emit the committed data/icons/lang.

### Changed
- Family taxonomy: `mortal/fae/beast/underfolk/dragon/cursed` →
  `human/elven/dwarven/bestial/faeborne/undead/draconic`.
- Curios slot grants by family: Elven +necklace, Dwarven +belt, Faeborne +ring, Undead +charm.
- Integrations re-pointed to the new roster: Ars Nouveau mana/cost, Iron's Spellbooks
  spell damage, Curios slots, Apotheosis luck, Pehkui scaling, Feathers stamina.
- Banners standardized on the localized `runic_races:show_banner` / `signature_presentation`
  path; particle counts follow the Minor/Major/Mythic VFX tiers.
- `BALANCE.md`, `README.md`, and `CLAUDE.md` rewritten for the new roster.

### Removed
- All 24 previous races and their powers (Human, Halfling, Nomad, Giant-Blooded, Wood Elf,
  Wolfkin, Dragonborn, Catfolk, Minotaur, Serpentfolk, Mountain/Deep Dwarf, Goblin, Troll,
  Kobold, Wyvern-Blooded, Elder Drake, Vampire, Lycanthrope, Revenant, …) and the 6 old families.
- **Runic Skills** and **Spells 'n Gods (Runic Gods)** integrations dropped entirely — both
  removed as dependencies, along with their config toggles and the features they provided.

### Migration
- Players on a removed origin will be prompted to re-select a family and race on next load.
  Orphaned per-race NBT is harmless. No automatic old→new remap is provided.

## Earlier development

Pre-1.0 iterations (numbering was non-linear during development):
- **0.10.0** — Family/race two-layer selection, HUD fixes, and Origins Forge compatibility.
- **0.9.0** — Null-crash fixes, fail-open exploit fixes, modpack compat, centralized race registry.
- **1.1.0 (beta)** — Integration overhaul, icon HUD polish, and a race balance pass.
- **1.0.0 (beta)** — Icon-based cooldown HUD replacing the Origins bar HUD.
- **1.0.0 (initial)** — First release of Runic Races.
