# Runic Races 1.7.0 — pack-author guide

Target: Minecraft 1.20.1, Forge 47.2.0+, Java 17. Install the same 1.7.0 build on both sides; protocol 3 intentionally rejects the older protocol. Origins Forge remains required. No optional mod is required to activate any of the 17 new primary abilities.

## Authoring and diagnostics

`tools/generate_races.py` is the authoring entry point; `tools/expansion_content.py` contains the new race specifications. `python tools/generate_races.py --check` compares isolated generated JSON semantically with the working tree. `python tools/build_lang.py --check` validates the canonical `tools/ui_lang.json` plus generated `tools/race_lang.json` merge. Do not independently edit generated language or power values and expect a subsequent generator run to preserve them.

For a pack override, use an ordinary datapack at the same power path. Each new active is an `origins:multiple` bundle containing `cooldown_timer` (an Apoli resource) and `active_ability` (`runic_races:racial_ability`). Preserve the stable top-level IDs and the two child names. Change `cooldown_ticks` and the resource's `max` together. The active configuration holds `kind`, `duration_ticks` and a bounded `parameters` map. Passive/weakness `runic_races:racial_traits` children hold a bounded `values` map. Unknown keys, non-finite values and unsafe movement step ratios are rejected. The Java service reads those parsed values; there is no separate TOML table of numerical kits.

Use `/runicraces list`, `/runicraces info`, `/runicraces validate`, and `/runicraces state <player>`. Info reports the selected race, effective ability parameters, cooldown ticks, preparation/field/stance state, remaining charges/guard and environment flags. Looking up another player's info and the validation/state commands require operator permission. Inspect the server log for **partially loaded** powers as well as missing top-level powers: a top-level Origins entry alone does not prove that all of its children parsed. The GameTest suite checks every authored native subpower.

## Extension tags

These tags use `replace: false`. Optional mod entries should use `required: false`. Identifiers are explicit; the implementation never treats a substring in an entity, biome or damage name as a classification rule.

| Tag | Registry / data path | Effect |
|---|---|---|
| `runic_races:grove_homes` | `tags/worldgen/biome/grove_homes.json` | Grove Elf's grounded forest speed |
| `runic_races:cold_environments` | `tags/worldgen/biome/cold_environments.json` | Saurian cold state, subject to warmth/insulation |
| `runic_races:stone_support` | `tags/blocks/stone_support.json` | Mountain One's grounded footing |
| `runic_races:quarry_eligible` | `tags/blocks/quarry_eligible.json` | Quarry Rhythm's normal mining-speed bonus and successful-break charge |
| `runic_races:grove_underbrush` | `tags/blocks/grove_underbrush.json` | Removes the tagged block's stuck-in-block movement multiplier for Grove Elves; does not remove solid collision |
| `runic_races:mushroom_nourishment` | `tags/items/mushroom_nourishment.json` | Extra food/saturation after finishing edible mushroom preparations |
| `runic_races:cannot_bleed` | `tags/entity_types/cannot_bleed.json` | Additional entities excluded from Nightborn feeding |
| `runic_races:racial_control_resistant` | `tags/entity_types/racial_control_resistant.json` | Additional bosses/entities immune to racial control, without granting damage immunity |
| `runic_races:racial_magic` | `tags/damage_type/racial_magic.json` | Magical affinity and Prism classification |
| `runic_races:racial_physical` | `tags/damage_type/racial_physical.json` | Ordinary physical affinity and Shell classification |

For example, put this in `data/runic_races/tags/entity_types/racial_control_resistant.json`:

```json
{"replace": false, "values": [{"id": "your_boss_mod:ancient_guardian", "required": false}]}
```

The same format extends `grove_homes` with `your_biome_mod:elder_forest`, `mushroom_nourishment` with `your_food_mod:mushroom_pie`, or `cannot_bleed` with `your_people_mod:citizen`. Food still must be edible. Villagers, ownable entities, undead mobs, undead-family players and protected pets are excluded independently of tags; a datapack cannot make those targets into safe feeding exploits by removing a tag. The shipped exclusions include optional explicit MCA villager IDs. Other mods may need their own explicit entries.

Magical classification takes precedence over ordinary physical/projectile affinity for Auroran. Wyvernkin's projectile weakness remains independent, including magical projectiles. Fire and explosion vulnerabilities are independent categories. Bypass/administrative damage is never absorbed by racial wards. The magic tag includes optional references to `forge:is_magic` and `neoforge:is_magic`, as published by the inspected Ars and Iron's jars, along with explicit vanilla magic types. Unclassified custom damage is not guessed to be magic: tag it deliberately. Optional spell-damage bonuses and incoming damage classification are separate concerns.

## Multiplayer and claims

`combat.racialPvpControl = false` is the default in Forge's per-world server configuration, `serverconfig/runic_races/runic_races-server.toml`. Enabling it permits control only against otherwise legal PvP opponents. Player effect durations are halved and reapplication is guarded for at least 60 ticks. Boss control immunity still applies. The server's PvP switch, team permissions, protected pets and direct ownership are respected independently. Aimed damage and hostile area control have distinct filters; ambient farm animals are not automatically threats.

New damage uses ordinary attributed `hurt` calls and Forge's normal attack/hurt/damage events. Non-damage control and support additionally post the cancellable `RacialTargetEvent` on the Forge bus, exposing caster, target and action. A claims integration can cancel that event. Astral recall posts `EntityTeleportEvent.EnderEntity`; a cancellation or a changed destination rejects the recall. Claims plugins that only intercept particular vanilla packets may need an adapter for custom ability movement/control. Universal compatibility with every claims system is not claimed; test the actual pack.

## Budgets and lifecycle

Only one racial ward contributes to a hit: the eligible ward offering the greatest prevention at that moment. Same-family wards replace only with a greater remaining budget, or a later expiry on an equal budget. Different families remain independent offers rather than adding their prevention. Budgets are debited after armor, normal mitigation, absorption and the final Forge damage event. Canceling or fully shielding a hit grants no successful-hit reward. Absorption loss is not health loss for feeding. Prism replies are attributed internal damage and cannot trigger a racial reply/feed loop.

Moss One fields admit three recipients over their entire lifetime, including the owner. Healing intervals are shared by recipient across overlapping fields. Nightborn offers at most 2 HP per eligible proc / 6 HP per cast; Moss One offers at most 4 HP per recipient. Final restoration is capped after healing modifiers at 6 HP per relevant cast budget. A canceled heal is not retried through a bypassing health write.

`runic_races:expansion_cooldowns` is the only new persisted ability compound. It contains known per-ability cooldown debts and Tide Elf's drying progress. Online time reduces all stored debts, including races currently unselected; offline time pauses them. Death/clone, logout, dimension/race changes, reload and server stop clear transient preparations, fields, marks, shell, anchors and owned ward offers. An accepted cast's cooldown survives those transitions. The old Reaper `runic_races:revenant_revival_cd` is untouched and unrelated to Returned.

## Optional mods and presentation

The [race reference](EXPANSION_1.7.0_REFERENCE.md) lists every explicit affinity and slot allocation. New race metadata is centralized in `RaceRegistry`. Optional integrations own their attribute/slot/scale modifiers and remove their own contribution when the race changes. New Pehkui body size uses width/height modifiers, so it grants no racial reach increase and composes with external scale. If growth cannot fit safely, it is deferred until the player reaches an open position. Existing races retain their established base-scale behavior.

Curios grants use its inventory API with stable owned UUIDs. The shipped player entity assignment and zero-sized necklace/belt/ring/charm definitions make those types available with Curios alone; they do not grant every player a baseline slot. Curios merges other packs' positive base sizes normally. Race changes remove only the owned modifiers and let Curios return or drop invalidated occupied stacks. Feathers similarly uses one owned additive `MAX_FEATHERS` modifier relative to the normal 20-feather baseline, preserving external base values and modifiers. Only a recognizable old Runic absolute-value marker is migrated. Ars mana/cost calculations retain integer truncation while correcting a one-ULP floating-point multiplication error.

Ars and Iron's Shellfast casting restrictions load whenever the corresponding mod is present, independently of whether racial-affinity bonuses are enabled. Iron's ongoing channels are canceled through its own cancellation path with the normal cooldown policy. Shellfast also stops vanilla item use, attacks, mining and interactions. It changes owned speed modifiers and caps horizontal movement without removing another mod's effects.

Zephyr reuses the server-authoritative flap pipeline: 0.30 lift, 40-tick cooldown, one configured feather cost. Wyvernkin has a glide power and distinct membrane wings, with no flap profile. Optional Feathers absence follows the existing flight resource policy.

Portraits, active icons, signatures, ambience and the two wing sheets are original authored assets. Primary signatures emit 32 particles; auxiliary owner-only cues are sparse and bounded. Existing particle-density, HUD placement/scale/opacity and effects settings apply. Grove's reveal is an owner-only visible-target cue, not a globally visible Glowing effect. New transient state uses a small owner packet, avoiding full power-container synchronization on every cooldown tick.

## Local validation

Run `./gradlew test build runGameTestServer`. The pinned required development stack resolves Origins Forge 1.10.0.9, Apoli 2.9.0.8, Calio 1.11.0.5, Additional Entity Attributes 1.4.0.5 and Caelus 3.2.0. The generated test floor and `src/gameTest` classes are isolated from the release jar and use separate `run/gametest/<forge-version>-<sorted-profile>` worlds, never an existing player world. A successful Gradle run requires a fresh explicit passing GameTest summary; a zero process exit after a world-load failure is rejected.

Inspected optional compile APIs are Ars 4.12.7, Iron's **3.15.5.1**, Curios 5.14.1, Pehkui 3.8.2, Apotheosis 7.4.8 and Feathers 1.1 (CurseForge file 4814309). The existing local file named `irons_spellbooks-1.20.1-3.15.4.jar` actually declares **1.20.1-3.15.5.1** in its `mods.toml`; its filename is not evidence of testing 3.15.4. CI now preserves the real version in its filename. That Iron's runtime requires Forge 47.4.0+, GeckoLib 4.8.2+, Player Animator, Curios and Iron's Patreon library. Compilation alone is not an optional runtime compatibility result.

Run the additional cached runtime matrix with `./gradlew -PrrRuntime=pehkui,curios,feathers runGameTestServer`. The spell profile uses `./gradlew -PrrRuntime=curios,feathers,ars,irons -Pforge_version=47.4.10 runGameTestServer` and pins the required **Iron's Patreon Library 1.0.1**, whose mod ID differs from the later Iron's Lib 1.0.2. These are separate profiles: Pehkui 3.8.2 has an observed startup mixin failure on Forge 47.4.10. Passing each profile does not prove a compatible complete combined stack.

Disabling an adapter that loaded at startup takes effect on server-config reload and removes its owned contributions where applicable. Enabling one that was disabled at startup requires a restart. Shellfast spell restrictions remain active when optional affinity bonuses are disabled. See [validation status and remaining gates](VALIDATION_1.7.0.md) before publishing a pack update.
