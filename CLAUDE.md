# Runic Races — Forge 1.20.1 Mod

## Quick Reference
- **Mod ID**: `runic_races`
- **Package**: `com.otectus.runic_races`
- **Version**: 1.5.0
- **MC**: 1.20.1 | **Forge**: 47.2.0 | **Java**: 17
- **Mappings**: Official

## Build
- `./gradlew build` — full build
- `./gradlew compileJava` — compile-only
- `./gradlew test` — run JUnit tests

## Project Structure
- `action/` — Origins entity/bientity actions
- `client/` — client-side rendering/UI
- `command/` — in-game commands
- `condition/` — Origins conditions
- `config/` — mod configuration
- `event/` — Forge event handlers
- `integration/` — optional mod integrations (reflection + ModList guards)
  - `ars/`, `irons/`, `apotheosis/`, `curios/`, `pehkui/`, `feathers/`
- `network/` — packet handling
- `power/` — Origins power definitions
- `registry/` — DeferredRegister registrations
- `util/` — utility classes
- `Dependencies/` — local dependency JARs

## Key Dependencies
- **Origins Forge** 1.10.0+ (required, local JAR)
- **Apoli/Calio** (required, bundled via Origins)
- **Ars Nouveau** (optional), **Iron's Spellbooks** (optional)
- **Curios** (optional), **Apotheosis** (optional), **Pehkui** (optional)
- **Feathers** (optional)

## Conventions
- Registration: DeferredRegister on MOD bus
- Origins add-on: **37 races across 7 families** (`human, elven, dwarven, bestial, faeborne, undead, draconic`)
- Each race has exactly **3 powers**: one active (cooldown-gated), one passive positive, one weakness
- Per-race metadata (`scale`/Pehkui height, `maxFeathers`, `luckBonus`, Curios slots) lives in `race/RaceRegistry.java`; integrations read it via `RaceHelper`/`RaceRegistry` (no per-race code)
- Two-layer selection: pick a `family_*` origin (layer `family`), then a race (layer `race`, gated by family)
- All optional mod compat uses `ModList.isLoaded()` guards with reflection-based loading
- Dependencies via local JARs in `Dependencies/` (compileOnly, gitignored — download yourself)
- License: All Rights Reserved

## Race data authoring (`tools/`)
The 44 origins, 111 power JSONs, 2 origin layers, the 37 icon textures, and `en_us.json`
are emitted by static generator scripts (NOT wired into Gradle — the committed JSON is
hand-written-equivalent and authoritative):
- `tools/generate_races.py` — origins/powers/layers + `tools/race_lang.json`; its `PRESENT_SIG`
  map swaps banner/sound/particle actions for `signature_presentation` on converted actives
- `tools/generate_icons.py` — downscales per-race art (path via `RR_ART_DIR`, default `~/Notes/Runic Races/`) to `textures/item/`
- `tools/generate_ability_icons.py` — hand-typed 16×16 grids, Scale2x+shaded to 32×32 HUD icons
- `tools/build_lang.py` — merges race_lang + notification copy into `en_us.json`
- `tools/generate_wings.py` — paints the 64×64 articulated wing sheets from the base art
  (`pixie_wings_base.png`, `wyvern_wings_base.png`, `drake_wings.png` are the hand-made sources)
- `tools/generate_particles.py` — emits the custom particle sprite frames (`textures/particle/`)
- `tools/generate_overlays.py` — screen-cue overlay textures (`textures/gui/overlay/`)
- `tools/generate_state_runes.py` — one 16×16 rune glyph per `RaceStateFlags` (`textures/gui/rune/`)
Resource-id rule: cooldown resources are `runic_races:<race>/<powerFile>_cooldown_timer` and
must match exactly in JSON and any Java that reads them (`FlightConfig`, `AbilityIconRegistry`,
`RacialEventHandler`).

## Custom particles & sounds
- `registry/ModParticles.java` — 13 identity particles (`rune_glyph`, `soul_wisp`, `fae_sparkle`,
  `ember_scale`, `frost_mote`, `venom_drip`, `web_strand`, `leaf_petal`, `feather_down`,
  `shadow_wisp`, `foxfire`, `arcane_glint`, `bone_chip`); behaviors in
  `client/particle/RunicParticle` (registration common-side; providers client-only; physical
  matter — DRIP/LEAF/SHADE/CHIP — is world-lit, magic is emissive)
- `registry/ModSounds.java` + `assets/runic_races/sounds.json` — 23 sound events that
  curate vanilla `.ogg`s via `"type": "event"`; bespoke audio can drop in without code changes
- `SignatureEntry` Sfx/Vfx specs hold `Supplier`s so DeferredRegister objects can be referenced
  before registration resolves

## Signature presentation (every active)
- ALL 37 actives route through `SignatureRegistry` (JSON has one
  `runic_races:signature_presentation` action; the recipe — layered sounds, shaped particles,
  banner, screen cue — lives in Java). Keep the family grammar: Human snap / Elven rise-implode /
  Dwarven ground-burst / Bestial lunge / Faeborne swirl-pop / Undead sink-drain / Draconic exhale.
- `SignatureEntry.VfxSpec` shapes: `POINT` (default), `RING`, `RING_IN`, `RING_ORBIT`, `HELIX`,
  `DOME`, `LINE` (needs `RunicPresentation.fire(player, key, lineTarget)`), `SPOKES` —
  spreadX = radius/length, spreadY = height, speed = directed velocity.
- `client/ClientRacialAmbienceHandler` — one `AmbienceRoutine` per race (registry map);
  `AmbienceCoverageTest` enforces every race is registered or explicitly quiet.

## Wings & presentation
- `client/render/WingModel` — one 64×64 bake, three silhouettes (`MEMBRANE` wyvern/drakes,
  `FEATHERED` avian, `GOSSAMER` sprite/faerie) toggled via `setSilhouette`; outer tips lag the
  arm (cascaded child parts, per-race `tipLagFactor`/`tipOvershoot` in `WingType`)
- `WingRenderLayer` handles state (flap/hover/glide/swim/ground, riding/sleep/death hidden),
  glide wingtip trails + landing puffs live in the ambience handler — keep new races registry-driven
- Client config (`RRClientConfig`): `wings.*` (enabled/showOnOtherPlayers/reducedMotion/glideTrails),
  `effects.*` (cameraShake/screenCueIntensity/heavyEffects/simpleCues/fovEffects),
  `ambient.particleDensity`
- Server config: `vfx.breathParticleDensity` scales `ConeBreathAction` particles
- Screen cues draw tinted overlay textures (`textures/gui/overlay/`); `effects.simpleCues`
  falls back to flat rects. `FovKickHandler` reads `ScreenCueRenderer.fovKickDelta()` (±6% max).
- State runes: 16×16 glyphs in `textures/gui/rune/` (one per `RaceStateFlags`, enforced by
  `StaleAssetTest`), family-accent frames, fade-in labels on first light
- Ability deny cue: `client/AbilityDenyHandler` watches the Origins primary-active key and
  pulses the HUD red via `RacialCooldownOverlay.triggerDenyPulse` (cooldown-only; mana/stamina
  denials get server-side banners: arcane_overflow's else-branch, FlightServerHandler)
- Badge pilot: `fire_drake/dragonfire_breath.json` carries an `origins:badge` keybind hint —
  verify in a launcher before batching badges to all actives via `generate_races.py`

## VFX Density Guideline
Tier every ability's particle count so VFX grammar stays consistent:
- **Minor** (passive procs, ambient states): 10–20 particles
- **Major** (signature actives, cooldown abilities): 30–60 particles
- **Mythic** (rare life-saving moments, Nine Lives, Death Revival): 80+ particles

Pick one VFX path per ability — never both `origins:spawn_particles` AND `execute_command particle ... force`. Use `execute_command ... force` only for mythic tier.
