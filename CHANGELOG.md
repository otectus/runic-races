# Changelog

All notable changes to Runic Races are documented here.
The format is based on [Keep a Changelog](https://keepachangelog.com/).

## [1.5.0] — 2026-07-05 — Full-Audit Remediation

A deep audit of the codebase, datapack, integrations, balance, docs, and tests — and the
fixes that came out of it. **Network protocol bumps to 2**: update client and server together.

### Fixed
- **Wings render again for all winged races** (Avian, Sprite, Faerie, Wind Wyrm, and the
  four drakes). Since the two-layer selection, `RaceHelper` could resolve the *family*
  origin (`family_bestial` etc.) instead of the race — origin-map iteration order is
  undefined — which silently disabled everything race-keyed: wings, Pehkui scale, ambience,
  HUD icons, and integrations. Race resolution now reads the `runic_races:race` layer
  explicitly.
- **Magi's Arcane Overflow works on standalone installs.** Under the default fail-closed
  resource gating, the 30-mana activation gate read 0 mana without Iron's Spellbooks —
  the signature active was permanently dead, silently. A new
  `runic_races:resource_available` condition lets the shipped powers apply resource gates
  only when the backing mod is actually present; a refused cast now shows a red banner and
  no longer consumes the cooldown.
- **Flap packets are rejected unless actually gliding.** The old guard accepted flaps from
  any airborne moment, letting a modified client chain free vertical boosts without wings
  deployed. The legit client only flaps mid-glide; the server now enforces it.
- **Breath weapons and offensive AoE spare allies and pets.** A drake's cone hit teammates
  and owned wolves; the shared hostility heuristic also debuffed *your own pet* while it
  defended you. New `util/Hostility` is the single source of truth: teams, PvP-protected
  players, and owned/ally-owned tamed animals are never hit; the aggro clause narrows to
  mobs targeting the caster. Breath still hits neutral and passive mobs — aim is the
  counterplay.
- **Feather's max-stamina resets when a player no longer has a race** (all other
  integrations already restored their baselines). Guarded by a persisted marker so packs
  that customize feather maxima are never stomped.
- **Datapack-stacked powers no longer overwrite each other's attribute modifiers** —
  `scaling_attribute` and `biome_affinity` derive modifier UUIDs from their config instead
  of sharing fixed constants.

### Added
- **"< Back" button on the race selection screen** (top-left). Players who confirmed a
  heritage can return to the family screen and pick a different one; the server un-chooses
  the family layer and Origins re-prompts from the top. Guarded server-side so a finished
  character can't replay it to reset.
- **Wing flaps cost feathers when Feather's Mod is present** (1 for small wings, 2 for the
  Wind Wyrm; `flight.flapStaminaCost` to disable). Exhausted wings refuse with a red banner
  and the deny sound — never a burned cooldown. Standalone flight is unchanged.
- **Foreign-origins warning**: when clean two-layer mode hides origins registered by other
  Origins add-ons or datapacks, the server log now names every hidden origin and the exact
  config remedy at startup.
- **Learning-mode hints**: `show_banner` gains an optional `learning_hint` — one
  explanatory chat line per player (e.g. the first time a Magi is mana-blocked), gated by
  `notifications.learningMode`.
- **CI**: GitHub Actions workflow compiles and runs the full test suite on every push;
  `ci/fetch-deps.sh` bootstraps the compile-only jars from Modrinth (Apoli/Calio extracted
  from the Origins all-jar).

### Balance
- **Sprite**: speed +30% → +20%, attack speed +15% → +10% (flight, tiny hitbox, and Phase
  Shift untouched).
- **Faerie**: ground speed +20% → +15%.
- **Feline**: Nine Lives cadence 10 → 15 minutes, and a spent life leaves 5s of Weakness II.
- **Fire Drake**: breath 7 → 6 damage; cold/wet vulnerability +30% → +35% (fire immunity
  stays — the sharpened opposite element is the counterplay).
- **Changeling**: Mirror Shift 40s → 25s; attack penalty −10% → −5%.
- **Canine**: hunger drain +40% → +25%; taigas now count as pack home ground.
- **Zombie**: healing penalty −25% → −15% (sun decay unchanged — it's the identity).
- **Valen**: the twin slow softens to −8% speed / −8% attack speed.
- **Iron One**: integration penalties only — Iron's spell power −15% → −10%, Ars cost
  +15% → +10%; the standalone kit is untouched.
- **Kitsune**: weakness re-shaped from a High Elf twin (flat physical vulnerability) to
  **+20% damage taken under the open daytime sky** — a night-predator pattern.
- **Volt Drake**: new storm-charged rider — an extra +10% attack speed while in the rain,
  so the lightning identity has a moment.
- Dead `damage_penalty: 0.0` no-op fields removed from seven biome-affinity powers.

### Diagnostics & tests
- Five new regression suites: resource-gate lint (no active may dead-gate on an
  integration-backed condition), network append-only + protocol fixture, flap-guard scan,
  hostility-heuristic consolidation, version-consistency (docs must name the built
  version), plus a zero-value no-op lint in the power JSON linter.

## [1.4.0] — 2026-07-02 — Immersion & Visual Identity Overhaul

A six-phase presentation overhaul: every race now looks, sounds, and moves like itself —
signature actives, minute-to-minute ambience, visible weaknesses, articulated wings, and a
rebuilt HUD/screen-cue layer.

### Added — signature presentation (every active)
- **All 37 actives now route through `SignatureRegistry`** (26 new keys), each with a shaped,
  family-grammar recipe: Human snap / Elven rise-implode / Dwarven ground-burst / Bestial
  lunge / Faeborne swirl-pop / Undead sink-drain / Draconic exhale. Power JSONs swap their
  banner/sound/particle actions for a single `signature_presentation`.
- **Shaped VFX**: `SignatureEntry.VfxSpec` gains `RING`, `RING_IN`, `RING_ORBIT`, `HELIX`,
  `DOME`, `LINE` (caster→target streams), and `SPOKES`; showcase pieces include Arachnid's
  web spokes, Deep One's seismic pulse ring, Kitsune's orbiting foxfire, Reaper's inward
  soul stream, Changeling's freeze-frame mirror shift, and Sprite's blink-out implode.
- **7 new identity particles** (`web_strand`, `leaf_petal`, `feather_down`, `shadow_wisp`,
  `foxfire`, `arcane_glint`, `bone_chip`) — physical matter is world-lit, magic stays
  emissive — plus 10 more curated sound events with subtitles, 4 new `RaceColors`, and a
  `WIND_STREAK` edge cue for dashes and leaps.
- **Dormant screen cues finally wired**: `MOON_GLOW` (Moon Elf veil), `HEARTBEAT_FLASH`
  (Blood Elf frenzy), `FREEZE_FRAME` (Changeling mirror).

### Added — ambience & weakness feedback
- **Per-race ambience registry**: `ClientRacialAmbienceHandler` becomes one
  `AmbienceRoutine` per race (shared rate limiter, density-scaled). 24 races gain
  minute-to-minute identity — Magi arcane glints off enchanted gear, Kitsune foxfire tail
  on night sprints, Wraith sneak shadow trail, Sea Serpen bubble wake, Terra Drake dust
  footfalls, Feline campfire purr, and more.
- **Glide wingtip trails** (config `wings.glideTrails`, per-race particle) and landing dust
  rings scaled to wing size.
- **Every fragile race tells you when it hurts**: victim-side fragility procs with a shared
  3s debounce — High Elf rings like struck crystal, Arachnid chitin-crack, Skeleton bone
  chips, Avian feather-burst on bad landings, Sprite sparkle-scatter, Demon gold flash +
  bell toll on holy damage. Blood Elf lifesteal is now a visible crimson siphon line.
- **New state flags + runes + banners**: `RAVENOUS` (Canine, low food) and `COLD_IRON_GRIP`
  (Faerie holding iron); `SUBMERGED_WEAK` extended to Iron One / Sky One; Nymph gets the
  `DRY_SLUGGISH` rune; Volt Drake short-circuits on submerge; Dark Elf / Reaper sizzle when
  the sun first finds them.

### Added — articulated wings
- **`WingModel` rebuilt** as one 64×64 bake with three silhouettes toggled per race:
  `MEMBRANE` (wyvern + drakes), `FEATHERED` (Avian), `GOSSAMER` (Sprite/Faerie).
- **Cascaded tip lag**: wingtips chase the arm with per-race lag and overshoot, so flap
  snaps whip through the downbeat and settle; gossamer hindwings run a lazier chase, and
  fast flutter renders a faint ghost pass as motion blur (`reducedMotion`-gated).
- `generate_wings.py` paints all wing islands from the hand-made base art.
  **Resource-pack note:** wing texture layout changed 64×32 → 64×64.

### Changed — HUD & screen cues
- **Screen cues upgrade from flat rects to tinted textures**: real radial vignettes, frost
  dendrites, and a three-strip wobbling heat shimmer; `effects.simpleCues` restores the
  flat-rect path.
- **FOV kicks** (`effects.fovEffects`): breath heat and wind rush push the FOV out, impacts
  and the freeze-frame punch in — eased, intensity-scaled, clamped to ±6%.
- **State rune HUD**: letter-boxes replaced by a 14-glyph thematic rune atlas with
  family-accent frames and fade-in labels when a rune first lights.
- Ability HUD icons upscaled to shaded 32×32 (Scale2x + gradient/rim pass); frost breath
  gains rime motes, water breath churns with bubbles.
- **Badge pilot**: Fire Drake's breath carries an `origins:badge` keybind hint to verify
  in-game before batching badges to all actives.

### Added — tests & docs
- `AmbienceCoverageTest` (every race has a routine or is explicitly quiet);
  `StaleAssetTest` gains overlay-texture and rune↔`RaceStateFlags` parity checks.
- `CLAUDE.md` documents the signature-shape system, 13 particles, 23 sounds, wing
  silhouettes, new config keys, the overlay/rune generators, and the badge pilot.

## [1.3.0] — 2026-07-02 — VFX & Race Presentation Pass

Foundation release for the presentation overhaul: custom particle/sound registries,
per-race wing textures, responsiveness fixes, and a test-coverage push.

### Added
- **`ModParticles` registry** with 6 custom identity particles (`rune_glyph`, `soul_wisp`,
  `fae_sparkle`, `ember_scale`, `frost_mote`, `venom_drip`) behind `RunicParticle`
  behaviors.
- **`ModSounds` registry** with 13 sound events curating vanilla audio via
  `"type": "event"` in `sounds.json` — bespoke audio can drop in without code changes.
- **Per-race wing textures** replacing shared family sheets.
- **Ability deny/ready cues**: the HUD pulses red when an active is used on cooldown
  (`AbilityDenyHandler`), and plays a ready ding when it comes back.
- Submerged/dry state flags for water-bound races.
- Six new test classes: `AbilityIconCoverageTest`, `ParticleAssetTest`,
  `RaceScaleRangeTest`, `SignatureCoverageTest`, `StaleAssetTest`, `WingCoverageTest`.

### Changed
- New config knobs: `ambient.particleDensity` and `effects.screenCueIntensity`.
- Pehkui height sync now reacts to config reloads.
- Breath cone polish pass.
- **Wings respond instantly to race switches** — `WingRenderLayer` resolves the wing type
  per-frame (`RaceHelper` memoizes per tick) instead of caching for up to 5 seconds.
- Per-family ability-ready ding pitch (`FamilyAccent.readyPitch()`); per-element breath and
  per-tier flap subtitles (previously one shared string each).
- `generate_icons.py` source-art path is no longer hardcoded to a specific home directory.

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
