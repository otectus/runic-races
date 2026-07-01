# Runic Races — Forge 1.20.1 Mod

## Quick Reference
- **Mod ID**: `runic_races`
- **Package**: `com.otectus.runic_races`
- **Version**: 1.2.0
- **MC**: 1.20.1 | **Forge**: 47.2.0 | **Java**: 17
- **Mappings**: Official

## Build
- `./gradlew build` — full build
- `./gradlew compileJava` — compile-only
- `./gradlew runData` — run data generators
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
- `src/generated/resources/` — datagen output
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
- `tools/generate_races.py` — origins/powers/layers + `tools/race_lang.json`
- `tools/generate_icons.py` — downscales per-race art from `~/Notes/Runic Races/` to `textures/item/`
- `tools/build_lang.py` — merges race_lang + notification copy into `en_us.json`
Resource-id rule: cooldown resources are `runic_races:<race>/<powerFile>_cooldown_timer` and
must match exactly in JSON and any Java that reads them (`FlightConfig`, `AbilityIconRegistry`,
`RacialEventHandler`).

## VFX Density Guideline
Tier every ability's particle count so VFX grammar stays consistent:
- **Minor** (passive procs, ambient states): 10–20 particles
- **Major** (signature actives, cooldown abilities): 30–60 particles
- **Mythic** (rare life-saving moments, Nine Lives, Death Revival): 80+ particles

Pick one VFX path per ability — never both `origins:spawn_particles` AND `execute_command particle ... force`. Use `execute_command ... force` only for mythic tier.
