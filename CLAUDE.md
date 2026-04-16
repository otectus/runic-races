# Runic Races — Forge 1.20.1 Mod

## Quick Reference
- **Mod ID**: `runic_races`
- **Package**: `com.otectus.runic_races`
- **Version**: 0.9.0
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
  - `ars/`, `irons/`, `apotheosis/`, `curios/`, `passiveskill/`
  - `runicskills/`, `spellsngods/`, `pehkui/`, `feathers/`
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
- **Feathers** (optional), **Runic Skills** (optional), **Spells 'n Gods** (optional)

## Conventions
- Registration: DeferredRegister on MOD bus
- Origins add-on: 24 races across 6 families
- All optional mod compat uses `ModList.isLoaded()` guards with reflection-based loading
- Dependencies via local JARs in `Dependencies/` (compileOnly)
- License: All Rights Reserved

## VFX Density Guideline
Tier every ability's particle count so VFX grammar stays consistent:
- **Minor** (passive procs, ambient states): 10–20 particles
- **Major** (signature actives, cooldown abilities): 30–60 particles
- **Mythic** (rare life-saving moments, Nine Lives, Death Revival): 80+ particles

Pick one VFX path per ability — never both `origins:spawn_particles` AND `execute_command particle ... force`. Use `execute_command ... force` only for mythic tier.
