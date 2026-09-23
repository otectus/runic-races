# Runic Races 1.7.0 implementation and validation

Target: Minecraft **1.20.1**, Forge, Java **17**. Source baseline: `1877c025265920f2800ac09ba7f194244e9a7e3c` (1.6.3). The implementation adds all 17 requested races: **54 races, seven families, 61 origins including selectors, 162 top-level powers**. Every race retains exactly three visible power bundles. No expansion ability is a placeholder or disabled behind a development flag.

**Status:** implemented and build-verified, with server runtime verification for the scenarios and separate dependency profiles below. **Release readiness is withheld** pending the interactive multiplayer, existing-world upgrade, measured balance and performance gates in this document. Server GameTests use real Forge events, Origins capabilities and transformed vanilla methods, but synthetic connected-player fixtures; they do not prove remote-client behavior.

## Per-race completion matrix

“Complete” means code, three generated bundles, effective tuning, explicit optional metadata, localized portrait/active icon, signature/ambience, owner feedback and shared lifecycle handling are present. Every row participates in native power parsing, valid activation and maximum-health runtime checks. It does not mean every kit has completed multiplayer playtesting. Final values and individual best/weak situations, nearest comparisons and counterplay are in [the race reference](EXPANSION_1.7.0_REFERENCE.md).

| Race | Content and gameplay | Weakness implementation | Additional automated evidence | Interactive gate |
|---|---|---|---|---|
| Colossan | Complete: prepared heavy strike | Attack speed and exhaustion | Lethal preparation consumed; attack speed; body size/reach; feather stacking; clone and race cycling | Charged/sweeping melee, armor and reach comparison |
| Auroran | Complete: finite ally wards | Physical affinity | Damage classification; ward cancellation/absorption and ledger ownership | Two remote allies, shields and mixed support composition |
| Grove Elf | Complete: one marked projectile, forest/underbrush | Reduced maximum health | Projectile launch attribution and piercing single consumption; Curios ownership and occupied shrink | Multishot, crossbows, forest route and owner-only reveal |
| Tide Elf | Complete: swept current step, amphibious traits | Drying state and reduced health | Native powers and activation; persistent dry progress uses owned save schema | Water/land transitions, rain/drink refresh, mining and underwater vision |
| Astral Elf | Complete: bounded safe anchor/recall | Reduced maximum health | Held-key suppression; wall veto; dimension/clone cleanup; Ars affinity | Remote correction, changed player size, borders and moving obstacles |
| Mountain One | Complete: confirmed-break quarry charges | Movement and attack speed | Parsed harvest/movement traits and active state | Canceled breaks, tool tiers, Mining Fatigue and ore/stone route |
| Moss One | Complete: stationary recovery field and mushroom food | Fire affinity and movement | Fire classification; amplified-healing final cap; field lifecycle | Three-recipient lifetime limit, poison, overlapping fields and containers |
| Crystal One | Complete: one finite prism counter | Explosion affinity and health | Magic classification; single-use/non-stacking ledger arithmetic | Player/projectile retaliation, mixed magical explosions and shield cases |
| Bovine | Complete: anticipated swept collision charge | Exhaustion and attack speed | Walls and protected pets; actual activation | Teammate body stops, stairs/ledges and high movement modifiers |
| Saurian | Complete: stationary preparation and finite air | Cold attack/freezing affinity and movement | 600-tick total air; native power load and arming | Cold biome, leather/fire/campfire remedies, moving ambush and shields |
| Chelon | Complete: finite shell, movement cap and action restrictions | Movement and attack speed | 500-tick total air; stance cleanup; optional spell veto/channel probes | Remote movement, toggle input, all item/container actions and armor |
| Zephyr | Complete: horizontal crosswind, glide/flap, original wings | Health and received knockback | Native glide/knockback powers; activation; bounded movement codec | Powered flap cost, collision/fall routes and wing readability |
| Nightborn | Complete: accepted-health feeding and night traits | Sunlight damage and healing | Canceled hits cannot feed; amplified feed cap; native healing drawback | Actual sunlight remedies, targets at varied attack speeds and food levels |
| Returned | Complete: recent-aggressor mark and one hit | Healing | Native healing drawback; real aggressor activation; cooldown lifecycle | Aggressor range/visibility, directional speed and Reaper regression |
| Wailer | Complete: anticipated cone and visible wounded-threat sense | Health and physical affinity | Physical classification; control veto and boss immunity | Interruption, six-target cone, teams and reduced PvP durations |
| Scaleheir | Complete: finite self ward and bounded threat weakness | Exhaustion and attack speed | Post-absorption finite ward; canceled event; control permission | Three-threat selection and PvP-disabled/enabled play |
| Wyvernkin | Complete: swept sting, glide and original membrane wings | Health and projectile affinity | Projectile classification; native glide and activation | Ground/glide geometry, one poison contact, no powered ascent |

## Reproduction

Fetch the repository's documented compile dependencies before a clean build. Run from the repository root. The following commands use the normal Gradle user home; this workspace used `--gradle-user-home .gradle`, with `--offline` only after dependencies were cached.

```text
python tools/generate_races.py --check
python tools/build_lang.py --check
./gradlew test build runGameTestServer
python tools/audit_release.py build/libs/runic_races-1.7.0.jar
./gradlew -PrrRuntime=pehkui,curios,feathers runGameTestServer
./gradlew -PrrRuntime=curios,feathers,ars,irons -Pforge_version=47.4.10 runGameTestServer
```

GameTests run in `run/gametest/<forge-version>-<sorted-profile>`, using an isolated generated stone-floor structure. They never open a user's survival world. Separate worlds prevent an optional mod's saved dimension from breaking a later required-only run. Gradle also requires a fresh explicit successful GameTest summary: Forge can otherwise exit zero after a world-load failure. Optional API probe classes load reflectively only when their mod is present; an absent-mod probe is a conditional no-op, not evidence that the optional integration passed. Compile warnings about deprecated APIs on newer Forge are not runtime test failures.

### Recorded results

Build logs and XML reports are local generated evidence, not dependencies of the source distribution:

- JUnit: `build/test-results/test/TEST-*.xml`, report `build/reports/tests/test/index.html`.
- Required server stack: `build/required-runtime-final.log`.
- Pehkui/Curios/Feathers server stack: `build/optional-runtime-final.log`.
- Ars/Iron's/Curios/Feathers server stack: `build/spell-matrix.log`.
- Distribution structure, size and SHA-256: `build/release-audit.json` from `tools/audit_release.py`.
- Original pixel-art contact sheet inspected during development: `build/art/expansion-contact-sheet.png`.

The suite has **19 unconditional server GameTests plus four optional probe tests**. An optional test returns without exercising its API when that mod is absent. The log's total remains 23, so read the profile column when interpreting compatibility evidence.

| Executed check | Result | Scope |
|---|---|---|
| JUnit | **67 passed, zero failed/errors/skipped** | Existing regression suite plus input, cooldown, accepted-damage and ward arithmetic |
| Generator check | **Passed** | 54 races; semantic JSON parity with authoring source |
| Language check | **Passed** | 704 merged localization keys |
| Required-only Forge 47.2.0 server | **23 passed** | 19 common tests; optional probes inactive |
| Pehkui/Curios/Feathers, Forge 47.2.0 | **23 passed** | Common tests, real body/reach/feather/slot APIs, zero unintended baseline slots, occupied-slot return; spell probes inactive |
| Ars/Iron's/Curios/Feathers, Forge 47.4.10 | **23 passed** | Common tests, real spell events and active channel cancellation, slot/feather APIs; Pehkui probe inactive |
| JAR audit | **Passed** | 61 origins, 162 powers, all 17 new portrait/model/active assets, two wing sheets, six Mixin classes and six populated reference-map groups; no test fixture in the distribution |
| Patch whitespace | **Passed** | `git diff --check` |

The final distribution is built against **Forge 47.2.0**, not the temporary newer spell-test override. Validation host: Windows 11 amd64, Eclipse Adoptium Java 17.0.20.8, Gradle 8.6. These successful automated checks do not close the interactive or measured release gates below.

Artifact: `build/libs/runic_races-1.7.0.jar`, **1,014,786 bytes**. SHA-256:

```text
6e6ba645ce039092e53bf7e12327a32d0a3db8c06af5078268c23fa75c498c5a
```

The audit report is `build/release-audit.json`. The final required-only build and runtime completed successfully with server-log timestamp **2026-09-09 01:55**; the final Pehkui profile and spell profile each explicitly reported all 23 tests passed in their preserved logs. This identifies the inspected artifact; rebuilding may change the archive checksum.

Pinned required runtime: Origins Forge 1.10.0.9, Apoli 2.9.0.8, Calio 1.11.0.5, Additional Entity Attributes 1.4.0.5, Caelus 3.2.0. Baseline Forge is 47.2.0. Optional runtime profiles use Pehkui 3.8.2, Curios 5.14.1, Feathers 1.1, Ars Nouveau 4.12.7, Iron's Spellbooks **1.20.1-3.15.5.1**, GeckoLib 4.8.2, Player Animator 1.0.2-rc1 and Iron's Patreon Library 1.20.1-1.0.1. The preexisting local Iron's filename says 3.15.4; its actual manifest is 3.15.5.1. No compatibility claim is made for the mislabeled version. The newer Iron's Lib 1.0.2 uses a different mod ID and does not satisfy that Spellbooks dependency.

### Observed external compatibility failures

Pehkui 3.8.2 fails before world startup on Forge 47.4.10: its `compat1204minus.ScreenHandlerMixin` callback `canUse$xOffset` expects a different `AbstractContainerMenu` argument order (`Block` versus `Player`). The same Pehkui version passed the body-size/reach probe on Forge 47.2.0. Iron's 3.15.5.1 requires Forge 47.4.0 or later. The [publisher's release metadata](https://api.modrinth.com/v2/project/pehkui/version) was checked during validation: 3.8.2 was the latest published Forge build for Minecraft 1.20.1. Therefore the successful separate profiles must not be advertised as proof that the complete combined optional stack works. Resolve a mutually compatible Forge/Pehkui/Iron's version set, then rerun the complete pack. No third-party mixin or dependency jar was patched or silently disabled.

The inspected Iron's jar also logs an invalid `irons_spellbooks:chests/citadel/citadel_tomes` loot function (`minecraft:set_written_book_pages`) on Minecraft 1.20.1. This belongs to that optional jar and is separate from Runic Races' power parsing. Keep the warning visible when qualifying the pack; passing racial probes does not certify all third-party content.

## Source findings and intentional corrections

The [baseline inventory](BASELINE_1.6.3_BALANCE_INVENTORY.md) records all 37 old kits. Generator reconciliation first reproduced all 111 committed baseline power files semantically, resolving the 21 documented drift differences. Expansion generation then added 51 new power files without renaming old race, power, item or cooldown IDs.

Native runtime inspection found five baseline bundles that only partially loaded because the installed Origins/Apoli implementation does not accept `origins:not` / `origins:invert` wrappers. They now use the same condition with its supported `inverted: true` field: Magi Arcane Overflow, Sky One Thin-Air Lungs, Sea Serpen Landbound Coils, Volt Drake Grounded and Wind Wyrm Untethered. Values are unchanged; formerly ineffective conditions now execute. The stronger native-subpower GameTest guards this distinction.

Curios grants now use the actual Curios inventory API. Looking for slot attributes on the vanilla player attribute map could silently do nothing. Zero-sized slot definitions and an entity assignment make racial grants available with Curios alone, while other mods' positive base sizes and modifiers remain independent. Feathers uses one owned additive attribute modifier instead of overwriting the external maximum. A narrowly recognized old absolute-value marker is migrated; unrelated values are retained. New Pehkui sizes affect width/height only, preserving external base scale and reach. Ars integer calculations correct a one-ULP floating-point truncation error (100 × 1.15 previously became 114).

New accepted-damage rewards run after real health loss; internal racial damage cannot recursively produce feeding, venom or retaliation. Wards debit only final eligible prevention after absorption and Forge cancellation. Final healing caps apply after external amplification. New cooldown debts persist independently of transient casts and tick only while online. Reaper's existing revival cooldown is unchanged. Network protocol is **3**, with previous packet IDs retained and new IDs appended; client and server must update together.

All proposed kit numbers were retained. Detailed implementation choices, tag extension points, interruption rules, rounding, ownership and fixed budgets are documented in [the pack-author guide](PACK_AUTHOR_1.7.0.md). These values are design starting points supported by bounded-behavior tests, not measured proof of cross-roster balance.

## Unresolved release gates and exact next steps

These gates require interactive clients, representative saves/modpacks or measured play workloads that were not available to the automated server harness. Source inspection and synthetic fixtures are not substituted for them.

1. **Dedicated server with remote clients, plus singleplayer.** Install the final JAR and pinned required stack on a dedicated server and two separate 1.7.0 clients. Repeat in singleplayer. Select all 17 races; check the family back flow, three power entries, portraits, HUD at GUI scales 2/3/4, long names, opacity/minimal mode and reduced effects. Test primary press/hold/release, secondary flap, mount/sleep/item-use denial, death, reconnect, dimension travel and `/reload` during each cast shape. Check that the owner sees state promptly and observers see readable bounded cues. Verify no clipped controls, stale wing state or trapped shell. A startup-only client run cannot close this gate.

2. **Existing 1.6.3 world upgrade and actual restart.** Work on a backup copy of a representative 1.6.3 world. Record every old race selection, racial item, active cooldown, Reaper revival timer and external attribute/slot/scale value before upgrading. Restart with 1.7.0; verify the 37 selections and items remain valid. Cast preparation, field, shell, mark and anchor, then stop/restart and repeat through death/clone, disconnect and dimension transfer. Cooldown debt must remain, transient casts must vanish, and no items may disappear when slots shrink. Save the before/after NBT and server logs. Hook and serialization tests are useful evidence but are not a historical-world upgrade test.

3. **Targeting, terrain and environment matrix.** With two clients, test PvP off/on, friendly-fire-disabled teams, owned pets (including an offline owner), villagers, bosses and the pack's claims mod. Check canceled damage/control/support. Traverse fixed ground, forest, mountain, water, cave and glide routes; include stairs, slabs, closed/open doors, low ceilings, a world border, unloaded destination chunks, boats and mounts. Verify cold remedies, Tide drying/drink/rain, Nightborn sun/helmet/roof, Overworld day/night, Nether, End and one actual modded dimension. Test matching and undersized tools, shields, armor, bows/crossbows and real spell projectiles.

4. **Complete optional-mod pack and disable/reload behavior.** Resolve the combined Pehkui/newer Forge conflict above. Include Apotheosis and the pack's actual Runic Skills/Runic Gods versions, which are not present in the executed runtime profiles. Record exact jar manifests. Verify external reach, mana, healing, attack speed, movement, armor, feathers and occupied Curios stacks before/after race changes and server-config reload. Disable already loaded adapters and check removal of owned contributions. Enabling an adapter that was disabled at startup requires a restart. Confirm Shellfast vetoes real casts and interrupts ongoing channels, not just synthetic API events.

5. **Comparative balance.** On identical difficulty, equipment and effects, record setup, sample count and results for early survival, iron/late-game melee/ranged/magic, six fixed traversal routes, support with self plus two allies, and the focused pairs in the prompt. Include Colossan/Bovine, Saurian/pounce users, Chelon/armor users, Nightborn/Blood Elf, Astral/escape users and Wyvernkin/breath users. Compare Auroran/Moss One/Runic One/Dryad under identical incoming damage and mixed compositions. At least ten repetitions per combat/support setup and five per fixed route make variability visible; record damage, healing, duration, food/feathers, failures and uptime. Tune only when those observations warrant it and regenerate descriptions afterward. The reference's per-race notes are design analysis, not invented samples.

6. **Measured performance against 1.6.3.** Use isolated baseline and updated profiles on the same hardware, settings and copied world. Record CPU/RAM, Java flags, mod versions, player/entity counts and view/simulation distances. Warm up each run, then profile five minutes each of idle, travel and combat with mixed races, repeated support/movement and approximately 200 nearby eligible mobs. Record MSPT distribution, allocations, packet rates/bytes and full power-container sync frequency. Inspect cleanup after all players leave. The implementation bounds target counts and transient maps, avoids inactive per-tick entity scans and forced chunk loads, and sends small owner snapshots; these source properties do not constitute measured absence of regressions.

Publish only after the applicable gates pass and this record contains their evidence. The implemented source and build artifacts remain reviewable independently of that release decision.
