# Runic Races 1.7.0 — Astra Full Development Prompt

Prepared September 8, 2026. Repository: [otectus/runic-races](https://github.com/otectus/runic-races).

This is a complete implementation prompt. Give Astra this document with access to the repository. The research baseline is **1.6.3**, `main` commit **1877c025265920f2800ac09ba7f194244e9a7e3c**, dated September 2, 2026. The repository has **37 races across seven families**, targets **Minecraft 1.20.1, Forge, Java 17**, and requires Origins Forge. The expansion below adds **17 races**, producing **54 built-in races**. [Verified release baseline](https://github.com/otectus/runic-races/commit/1877c025265920f2800ac09ba7f194244e9a7e3c), [build configuration](https://github.com/otectus/runic-races/blob/1877c025265920f2800ac09ba7f194244e9a7e3c/build.gradle), [race registry](https://github.com/otectus/runic-races/blob/1877c025265920f2800ac09ba7f194244e9a7e3c/src/main/java/com/otectus/runic_races/race/RaceRegistry.java).

The race mechanics and numerical values proposed here are **original design recommendations and starting values**, not existing features or measured balance results. Research included all 111 committed power JSON files and the relevant registry, event, integration, flight, presentation, and validation systems. Minecraft runtime testing was not performed for this document. Astra must verify behavior and tune the complete kits during implementation.

---

## 1. Your assignment

You are the lead developer, systems designer, balance designer, and release engineer for **Runic Races 1.7.0**. Fully implement, integrate, polish, test, and document the following races:

**Colossan, Auroran, Grove Elf, Tide Elf, Astral Elf, Mountain One, Moss One, Crystal One, Bovine, Saurian, Chelon, Zephyr, Nightborn, Returned, Wailer, Scaleheir, and Wyvernkin.**

Deliver working code and complete content, not merely a design document, scaffolding, placeholder powers, or a list of future tasks. Follow applicable repository instructions, inspect the current branch before editing, and preserve unrelated work. Reconcile changes made since the research baseline; implement any already-started race to completion rather than registering it twice.

The names, heritage assignments, overall identities, complete implementation, and compatibility obligations in this document are fixed scope. You may improve proposed names of individual abilities, numerical values, or implementation details when source review and testing support a better result. Record consequential deviations and their reasons. Do not silently drop a race, replace its identity with another race's kit, or introduce a progression system to solve balance problems.

### Preserve the mod's design contract

The existing project specifies **exactly three top-level powers per race: one active ability, one coherent passive strength, and one coherent weakness**. Hidden subpowers can implement those bundles. Its stated philosophy separates racial identity from progression, favors affinities over access restrictions, and expects meaningful changes in playstyle. [Project design philosophy](https://github.com/otectus/runic-races/blob/1877c025265920f2800ac09ba7f194244e9a7e3c/README.md#design-philosophy), [balance principles](https://github.com/otectus/runic-races/blob/1877c025265920f2800ac09ba7f194244e9a7e3c/BALANCE.md), [authoring conventions](https://github.com/otectus/runic-races/blob/1877c025265920f2800ac09ba7f194244e9a7e3c/CLAUDE.md).

Apply that contract rigorously:

- Race is a permanent identity, not a class, skill tree, advancement track, spell collection, or evolutionary ladder.
- Use the existing primary active input. A placement/recall or enter/exit interaction may be two stages of **one** ability, sharing one cooldown and state machine.
- Keep racial bonuses fixed. Temporary focus, wards, or charges may exist within a cast; they must not become permanent leveling or resource farming.
- Give every weakness a clear explanation, actual gameplay consequences, readable feedback, and practical mitigation.
- Preserve access to normal equipment, crafting, enchanting, exploration, villagers, and other mods. Do not impose arbitrary weapon, armor, spell-school, or profession bans.
- Each race must work with the required Origins stack alone. Optional integrations enhance it; their absence must not leave an active unusable or its weakness meaningless.
- Do not make every member of a family mechanically identical. Keep family identity through presentation, anatomy, and broad affinities while allowing distinct roles.
- Preserve all 37 established origin IDs and their existing save identities. Fix relevant defects and justified balance inconsistencies, but avoid an unrequested redesign of the old roster.

## 2. Fixed roster and identity map

Use the namespace `runic_races`. “Heritage” is the player-facing concept here; retain the existing internal `family` terminology and two-layer selection structure.

| Heritage | Existing count | New races and stable origin IDs | New total |
| --- | ---: | --- | ---: |
| Human | 4 | Colossan `colossan`; Auroran `auroran` | 6 |
| Elven | 5 | Grove Elf `grove_elf`; Tide Elf `tide_elf`; Astral Elf `astral_elf` | 8 |
| Dwarven | 6 | Mountain One `mountain_one`; Moss One `moss_one`; Crystal One `crystal_one` | 9 |
| Bestial | 6 | Bovine `bovine`; Saurian `saurian`; Chelon `chelon` | 9 |
| Faeborne | 5 | Zephyr `zephyr` | 6 |
| Undead | 5 | Nightborn `nightborn`; Returned `returned`; Wailer `wailer` | 8 |
| Draconic | 6 | Scaleheir `scaleheir`; Wyvernkin `wyvernkin` | 8 |
| **Total** | **37** | **17 additions** | **54** |

These are setting-neutral adaptations of broad fantasy archetypes: giant-descended mortals, celestial mortals, woodland/sea/star elves, mountain/nature/gem dwarves, horned and reptilian folk, an air spirit, three forms of undeath, and two forms of dragonkind. Do not import another franchise's geography, religion, factions, racial stereotypes, named characters, or exact lore. Write original Runic Races descriptions.

### The distinction each addition must preserve

| New race | Its own play pattern | Closest existing comparisons to test |
| --- | --- | --- |
| Colossan | Deliberate, forceful individual blows | Valen's armored defense and shoulder-check; Terra Drake's bulk |
| Auroran | Brief protection for self and allies | High Elf's self-defense; Magi's offense; Runic One's ward |
| Grove Elf | Prepared archery and forest movement | Canine's pursuit; Dryad's healing; Skeleton's summoning kit and stated ranged theme |
| Tide Elf | Amphibious movement and underwater work | Moon Elf's water breathing; Nymph's charm; Sea Serpen's armor and breath |
| Astral Elf | Planned placement and return | Sprite's escape; Moon Elf's night survival; Wraith's spectral theme |
| Mountain One | Deliberate excavation and stable footing | Deep One's detection/mining; Sky One's climbing; Iron One's defense |
| Moss One | A sheltered, stationary recovery area | Dryad's personal bloom; Runic One's ward; Forge One's crafting |
| Crystal One | A precise, finite magical/projectile counter | Runic One's broad ward; Iron One's armor; Auroran's shared protection |
| Bovine | A committed directional charge | Valen's melee shove; Feline's pounce; Colossan's heavy strike |
| Saurian | Armored patience before an ambush | Serpen's venom and cleanse; Arachnid's web/venom; Feline's mobility |
| Chelon | Withdraw, endure, then re-enter the fight | Iron One and Valen; Sea Serpen's aquatic specialization |
| Zephyr | Fine air movement and a horizontal gust | Avian's vertical launch; Sprite/Faerie; Wind Wyrm's superior flight |
| Nightborn | A finite feeding opportunity and nocturnal hunting | Blood Elf's continuous lifesteal; Zombie's endurance; Wraith's drain |
| Returned | Resolve directed at a recent aggressor | Reaper's revival; Feline's death save; Zombie's durability |
| Wailer | A telegraphed cry and short-range death-sense | Wraith's spectral identity; Nymph's debuffs; Deep One's detection |
| Scaleheir | Grounded draconic protection and presence | Existing elemental drakes; Valen; Iron One |
| Wyvernkin | A committed venomous aerial strike | Elemental breaths; Serpen's repeated venom; Wind Wyrm's sustained flight |

Treat overlap as a design question, not a ban on shared mechanics. A different color or damage type alone does not establish a different play pattern.

## 3. Research lessons to apply

Use the following projects as inspiration for tradeoffs and usability. They are **not new dependencies**, drop-in code, or evidence that their current JSON schemas work in the pinned Forge fork. Author original mechanics, descriptions, and assets; respect licenses if any external implementation is reused.

| Primary reference | Relevant evidence | Application to this update |
| --- | --- | --- |
| [Origins: Merling](https://origins.readthedocs.io/en/latest/misc/base_contents/origins/merling/) | Aquatic identity covers breathing, sight, mining, swimming, buoyancy, and land limitations. | Tide Elf needs usable underwater controls and work capability, with a gentler land drawback that fits Runic Races. |
| [Origins: Shulk](https://origins.readthedocs.io/en/latest/misc/base_contents/origins/shulk/) | Natural armor is paired with substantial costs; its listed armor can exceed the visible bar. | Evaluate equipped survivability. Chelon must pay for defense through its stance and existing equipment interactions. |
| [Origins: Elytrian](https://origins.readthedocs.io/en/latest/misc/base_contents/origins/elytrian/) | Flight and airborne combat benefits come with equipment, space, and collision drawbacks. | Mobility consumes real budget. Do not combine unrestricted flight, large airborne damage multipliers, and repeatable venom. |
| [Extra Origins](https://github.com/MoriyaShiine/extra-origins) | Floran connects growth to hunger and ecology; Truffle exchanges strengths between roles; Inchling couples small size with vulnerability. | Connect environment and identity, but keep Grove Elf a hunter and Moss One a dwarven caretaker. Avoid adding stance trees or severe diet dependencies. |
| [Extra Origins 1.20-7 changelog](https://modrinth.com/mod/extra-origins/version/wrFytf78) | This March 22, 2024 release reduced Inchling advantages and narrowed unnecessary healing/fertilization checks. The release is for Fabric/Quilt 1.20.2. | Small models, reach, attack speed, repeated healing, and growth eligibility require actual testing. This is historical design evidence, not Forge compatibility evidence. |
| [Medieval Origins Revival: Banshee power source](https://github.com/muon-rw/Medieval-Origins-Old/blob/1.20.1-multiloader/common/src/main/resources/data/medievalorigins/powers/banshee/sonic_shriek.json) and [race descriptions](https://github.com/muon-rw/Medieval-Origins-Old/blob/1.20.1-multiloader/common/src/main/resources/assets/medievalorigins/lang/en_us.json) | The inspected historical branch separates a shriek's area repulsion from its direct ray effects; its Revenant uses a different summon/resource identity. | Give Wailer consistent filtering in every branch. Give Returned a focused pursuit identity, preserving Reaper and Skeleton. |
| [Origins: Vampire](https://modrinth.com/datapack/origins-vampire) | Feeding, healing, sunlight, and fragile transformation form linked tradeoffs; Vampire Lord introduces progression. | Borrow bounded feeding tension for Nightborn. Omit evolution, compulsory blood-only survival, and a second transformation system. |
| [Stargazer Origin](https://github.com/0vergrown/Stargazer-Origin/blob/main/README.md) | Its description combines a return location, celestial defenses, night benefits, and severe active-use costs. | Extract one bounded star-anchor ability for Astral Elf. Do not import the entire kit or compulsory screen-disrupting penalties. |
| [DnD Monster Origins: Minataur charge](https://github.com/The-Architects727/DnDMonsterOrigins/blob/master/data/minataur/powers/charge.json) and [size power](https://github.com/The-Architects727/DnDMonsterOrigins/blob/master/data/minataur/powers/tall.json) | The inspected charge accumulates sprint resource; its inspected damage branches do not consume that resource. The size power uses global scale commands. | Bovine needs one-hit consumption tests. Use owned scaling modifiers for Colossan/Bovine. Do not infer a proven runtime exploit from those source fragments. |
| [Origins+: Wyvern](https://modrinth.com/mod/origins-plus) | Its published Wyvern uses fireballs, fire immunity, and environmental/diet costs; listed versions are older Fabric releases. | Reject that kit for Wyvernkin because existing Runic Races drakes already occupy it. |

The documentation for [`origins:active_self`](https://origins.readthedocs.io/en/latest/types/power_types/active_self/) exposes explicit cooldown, input, and HUD fields. Verify every borrowed type and field against the actual **Origins Forge/Apoli/Calio jars used by this project**, not only the latest upstream documentation.

## 4. Audit and stabilize the baseline before adding content

### 4.1 Establish the actual runtime contract

Read the current instructions and inspect at least:

- `README.md`, `BALANCE.md`, `CHANGELOG.md`, `CLAUDE.md`, `gradle.properties`, `build.gradle`, and `ci/fetch-deps.sh`.
- All existing origins, powers, and both origin layers.
- `RaceDefinition`, `RaceRegistry`, `RaceHelper`, `RacialEventHandler`, custom actions/powers, and integration classes.
- The cooldown HUD, state flags/tracker, signature registry, notification registry, ambience, wings, flight packets, and tests.
- `tools/generate_races.py`, `tools/race_lang.json`, `tools/build_lang.py`, and the icon/art authoring tools.

Create a working balance inventory of all 37 existing races from **committed JSON plus actual event/integration behavior**. Include cooldowns, damage, health, armor, movement, control, healing, environmental costs, flight, and optional modifiers. Distinguish a description from an executed effect. Do not infer phasing, charm AI, party healing, school-specific magic, or other behavior merely from an ability's name.

The inspected build pins Origins Forge `1.10.0.9`, Apoli Forge `2.9.0.8`, and Calio Forge `1.11.0.5` as compile references. Confirm the resolved runtime dependencies and do not upgrade loaders or port to another Minecraft version as part of this expansion. [Pinned dependencies](https://github.com/otectus/runic-races/blob/1877c025265920f2800ac09ba7f194244e9a7e3c/build.gradle).

### 4.2 Reconcile the generator with shipped data

**Required finding:** running the pinned generator in an isolated directory and comparing parsed power JSONs produced differences in **21 of the 111 power files**. Some differences are defaults or presentation/schema details, so this is not a claim of 21 runtime bugs. Several differences would alter shipped balance or resource behavior.

| Example | Shipped 1.6.3 power | Generator output at the same commit |
| --- | --- | --- |
| Feline, Nine Lives | 18,000 ticks, 15 minutes | 12,000 ticks, 10 minutes |
| Changeling, Mirror Shift | 500 ticks, 25 seconds | 800 ticks, 40 seconds |
| Canine exhaustion multiplier | +25% | +40% |
| Sprite movement / attack speed | +20% / +10% | +30% / +15% |
| Fire Drake direct breath damage | 6 HP | 7 HP |

Sources: [generator](https://github.com/otectus/runic-races/blob/1877c025265920f2800ac09ba7f194244e9a7e3c/tools/generate_races.py), [Feline](https://github.com/otectus/runic-races/blob/1877c025265920f2800ac09ba7f194244e9a7e3c/src/main/resources/data/runic_races/powers/feline/nine_lives.json), [Changeling](https://github.com/otectus/runic-races/blob/1877c025265920f2800ac09ba7f194244e9a7e3c/src/main/resources/data/runic_races/powers/changeling/mirror_shift.json), [Canine](https://github.com/otectus/runic-races/blob/1877c025265920f2800ac09ba7f194244e9a7e3c/src/main/resources/data/runic_races/powers/canine/ravenous.json), [Sprite](https://github.com/otectus/runic-races/blob/1877c025265920f2800ac09ba7f194244e9a7e3c/src/main/resources/data/runic_races/powers/sprite/gossamer_wings.json), [Fire Drake](https://github.com/otectus/runic-races/blob/1877c025265920f2800ac09ba7f194244e9a7e3c/src/main/resources/data/runic_races/powers/fire_drake/dragonfire_breath.json).

Reconcile authoring inputs to shipped behavior before generation. Add a temporary-output or check mode so generation can be compared without overwriting the working tree. Require semantic parity after deliberate changes. Preserve resource-availability fallbacks, corrected subpower shapes, cooldown decay intervals, and manually added presentation metadata. Do not “fix” parity by reverting the game to stale generator values.

### 4.3 Address shared defects relevant to the expansion

Source inspection of [`ConeBreathAction`](https://github.com/otectus/runic-races/blob/1877c025265920f2800ac09ba7f194244e9a7e3c/src/main/java/com/otectus/runic_races/action/ConeBreathAction.java) found a cone/distance test without a block-occlusion test, followed by unconditional elemental riders after `target.hurt(...)`. Reproduce and correct wall penetration and riders on rejected attacks where applicable. Add shared validated targeting and accepted-hit handling before using these patterns for new attacks. Preserve deliberately different geometry only when its design explicitly allows it.

[`Hostility`](https://github.com/otectus/runic-races/blob/1877c025265920f2800ac09ba7f194244e9a7e3c/src/main/java/com/otectus/runic_races/util/Hostility.java) protects the caster's or allied owners' tamed animals; the README's breath description promises protection for anyone's pets. Reconcile this mismatch deliberately. The new defaults should protect all tamed pets from racial area attacks and movement riders. Handle unloaded/offline owners using ownership data, not only a live owner entity.

Do not reuse legacy state based on similar names. In particular, **Reaper already owns `runic_races:revenant_revival_cd`** in [`RacialEventHandler`](https://github.com/otectus/runic-races/blob/1877c025265920f2800ac09ba7f194244e9a7e3c/src/main/java/com/otectus/runic_races/event/RacialEventHandler.java). Returned must use new `returned` state and must not consume, clear, reinterpret, or migrate that Reaper key.

## 5. Common balance and behavior rules

All amounts below use **HP**, where **2 HP = one heart**. Durations and cooldowns use game ticks, nominally 20 ticks per second; they are not wall-clock timers. Values are starting targets to test against the actual old roster.

- Prefer conditional bonuses around 5–15%, ordinary armor bonuses around 1–3 points, and finite defensive windows. Larger values require an explicit scope, cap, and tradeoff.
- Count movement, small size, natural armor, reach, immunity, night vision, healing, crafting utility, extra Curios slots, and optional magic modifiers in the same total kit.
- An existing overpowered or nonfunctional trait is not permission to inflate every new race. Record the defect and make a narrowly justified correction.
- Use normal damage mitigation and attribution. Do not use armor-bypassing damage, percent-maximum-health attacks, instant kills, or unconditional health writes merely to make a race feel strong.
- A timed strike is one consumed opportunity, not a buff that repeats on every pellet, sweep target, projectile, offhand callback, or damage rider.
- Healing and mitigation caps are specified before unrelated healing amplification unless stated otherwise. Apply normal healing modifiers once; also impose a documented final cap where needed to prevent multiplicative support loops.
- Define whether a cooldown starts at arming, casting, or completion. Default: **on accepted activation**, including a later miss, early cancellation, or expired opportunity. Invalid activation does not consume a cooldown or resource.
- Short control effects must respect bosses, teams, PvP rules, claims, and existing resistance. Repeated racial crowd control must not lock players or bosses indefinitely.
- Do not let normal beds, boats, mounts, helmets, gear, or potion use become broken side effects of anatomy or roleplay.

## 6. Race implementation sheets

Each sheet defines one active, one passive bundle, and one weakness bundle. Keep additional visual cues and internal helpers hidden from the three-entry origin presentation. The proposed power file IDs are fixed in the manifest after these sheets unless a documented naming correction is necessary before first release.

### 6.1 Colossan — Human

**Identity:** giant-descended mortals whose strength is deliberate and physical. A Colossan should feel powerful through commitment and impact, without becoming a second armored Valen or a miniature Terra Drake.

**Active — Colossal Heave:** arm one heavy strike for **5 seconds**, on a **35-second cooldown**. The next successful direct melee hit at at least **90% attack charge** adds **3 HP** of ordinary physical damage and a moderated outward knockback. Consume the opportunity once. It must not multiply the equipped weapon's damage, break blocks, damage a second sweep target, or add persistent reach. Display a clear “strike prepared” indicator until used or expired.

**Passive — Giant's Bearing:** **+4 maximum HP** and **+0.30 knockback resistance**. Use an initial Pehkui scale of **1.20** when available. Strength and stability remain functional without Pehkui.

**Weakness — Heavy Limbs:** **−12% attack speed** and **+15% exhaustion**. The deliberate wind-up and appetite should be noticeable without making tools unusable. Size is an additional real tradeoff when scaling is installed, not the sole weakness.

**Presentation:** a broad stone-and-gold fist or shoulder silhouette; a short Human-family impact beat, muted stone dust, and one heavy impact sound. Avoid sustained camera shake.

**Acceptance:** ordinary and modded melee weapons; canceled hits; shielded targets; sweep attacks; low attack charge; race change while primed; two-block passages in a valid crouched pose; beds, boats, mounts, eye height, and safe resizing. Confirm the larger scale does not silently increase reach beyond the intended design.

### 6.2 Auroran — Human

**Identity:** mortal bearers of celestial light who protect nearby companions. They are not angels with permanent flight or a second magical damage specialist.

**Active — Dawnward:** on a **55-second cooldown**, grant the caster and up to **two eligible allies** within **6 blocks** and line of sight a ward lasting **6 seconds**. Each recipient can prevent at most **4 HP**, applied to ordinary eligible damage. Use the shared finite-ward service, not uncontrolled accumulation of vanilla Absorption effects. Select recipients deterministically, prioritizing injured allies and then distance. The ability remains useful solo.

**Passive — Inner Radiance:** **+2 maximum HP** and **15% less classified magical damage**, excluding bypass/administrative damage. This is protective affinity, not universal elemental immunity.

**Weakness — Mortal Vessel:** **15% more direct physical and projectile damage**. Classify damage explicitly; a magical projectile must not arbitrarily receive both sides of the kit because two independent handlers disagree.

**Presentation:** sunrise shield, warm gold with pale rose. Brief outward light followed by a quiet ward rim; each affected ally receives modest confirmation. Avoid persistent bright screen overlays.

**Acceptance:** undead player races and allied undead summons receive protection normally; strangers are not assumed to be allies; multiple Aurorans cannot stack unlimited wards; no-target solo cast works; existing stronger external shields remain intact; damage cancellation and ward expiry behave correctly.

### 6.3 Grove Elf — Elven

**Identity:** a patient woodland archer and agile forest traveler. Preserve Dryad's healing and Canine's pursuit niches.

**Active — Stillleaf Aim:** prepare one shot for **6 seconds**, on a **35-second cooldown**. The next eligible arrow launched from a fully drawn bow or a loaded crossbow gains **20% direct-hit damage, capped at +2 HP**, and briefly reveals the successfully struck target for **3 seconds**. Mark the projectile at launch; never infer its owner or empowerment from the player's equipment at impact. If it misses, the cast is spent. Give one projectile the enhancement for Multishot or other multi-projectile releases.

**Passive — Canopy Strider:** **+10% grounded movement speed** in forest-tagged biomes and immunity to sweet-berry-bush movement penalties and bush damage. This does not confer cobweb immunity, phasing through leaves, automatic climbing, or unrestricted fall immunity. Use extensible terrain tags for legitimate modded equivalents.

**Weakness — Slender Frame:** **−3 maximum HP**. Keep the bow useful everywhere; do not punish every nonforest location with another hidden penalty.

**Presentation:** leaf-fletched arrow, restrained moss green and elven magenta. Show a clean focus cue and one leaf-like impact burst. Any target highlight should be scoped to the caster where technically practical and disclosed accurately.

**Acceptance:** bows, crossbows, Multishot, Piercing, projectile reflection, unloaded targets, player death before impact, ammunition conservation mods, and damage cancellation. The stored shot must neither gain repeated extra damage through piercing nor trigger unrelated on-hit systems twice.

### 6.4 Tide Elf — Elven

**Identity:** an amphibious coastal explorer who moves and works naturally underwater. Do not turn this into Moon Elf with a blue icon.

**Active — Currentstep:** a **25-second cooldown** movement burst. In water, move up to approximately **5 blocks over 10 ticks** in the aimed direction, with swept collision checks and capped velocity. On land, perform a modest **2-block grounded step** with the same cooldown. No water creation, damage, invulnerability, teleport through walls, or repeated upward launch. Make the water and land behavior clear before selection.

**Passive — Amphibious Grace:** water breathing, **+20% swimming speed**, improved underwater visibility, and removal of the ordinary underwater mining penalty with a suitable tool. Preserve tool tiers, block hardness, enchantments, and Mining Fatigue; do not implement the mining feature as a huge generic Haste bonus. Define buoyancy controls so jumping and descending remain predictable.

**Weakness — Drying Gills:** **−2 maximum HP**. After **60 seconds** continuously outside water or rain, apply **−8% movement speed** until refreshed. One second immersed in water, exposure to rain, or finishing a drinkable water bottle/potion refreshes this state. No land suffocation, automatic damage, or separate thirst bar.

**Presentation:** tidal arrow and small current ribbons. Reuse the dryness state where its semantics match; show a concise explanation and remedy.

**Acceptance:** Depth Strider, Dolphin's Grace, Aqua Affinity, Conduit Power, flowing water, bubble columns, boats, modded water-tagged fluids, Nether travel, drinking, and relogging. Aquatic bonuses must not accidentally classify every fluid, including lava, as water.

### 6.5 Astral Elf — Elven

**Identity:** a careful traveler who leaves a short-lived thread to a known position. Its strength is preparation and return, not general flight or repeated free blinks.

**Active — Starbound Thread:** on first press, place a personal anchor at the player's current safe grounded position. It lasts **8 seconds**; its **40-second cooldown starts immediately**. A second distinct press during that window recalls the player if the anchor is within **16 blocks**, in the same dimension, loaded, unobstructed at arrival, and connected by the agreed line-of-sight rule. This special recall branch may operate while the shared cooldown is running; it must not place another anchor. Only one anchor and one recall exist per cast.

Blocked recall gives a clear denial and leaves the anchor available until expiry. Successful recall consumes it. Death, disconnect, dimension change, race loss, or invalid world state removes it while preserving the cooldown. Do not load chunks or use fallback teleportation into unknown terrain. Validate the full scaled player bounding box and honor applicable teleport-cancellation hooks.

**Passive — Astral Poise:** **30% less fall damage** and night vision during actual nighttime in dimensions with a normal day/night cycle. No void immunity, block phasing, or extra celestial attack.

**Weakness — Thin Tether:** **−3 maximum HP**. The finite anchor range, setup requirement, and lost opportunity on expiry also constrain the active.

**Presentation:** two stars joined by a thread; an owner-visible anchor marker with time remaining. Use a brief departure and arrival shimmer without compulsory blindness or screen distortion.

**Acceptance:** held-key repeat, recast attempts, roofs, changed blocks, lava, portals, dimension removal, border edges, vehicle state, logout, server restart, and the anchor becoming unsafe after placement.

### 6.6 Mountain One — Dwarven

**Identity:** a patient excavator and stoneworker. Deep One detects underground threats; Sky One climbs; Mountain One makes deliberate excavation satisfying.

**Active — Quarry Rhythm:** for **6 seconds**, or the next **10 successfully broken eligible blocks**, gain **+40% mining speed**, on a **45-second cooldown**. Limit it to explicitly tagged stone/ore materials and a tool that can normally harvest the block. This accelerates the normal break process; it does not break adjacent blocks, increase drops, suppress durability, bypass fatigue, or grant higher harvest tiers.

**Passive — Bedrock Bearing:** **+2 armor**, plus **+0.25 knockback resistance** while grounded on stone-tagged support blocks. Check actual support beneath the feet with bounded work. Do not use a vague “underground” heuristic for both this race and Deep One.

**Weakness — Deliberate Gait:** **−8% movement speed** and **−8% attack speed**. Tools remain usable; magic access remains available.

**Presentation:** a pick striking a layered mountain; slate and pale iron. A short ground-burst start, then small chips only when eligible blocks are actually mined.

**Acceptance:** wrong tool, no tool, unbreakable blocks, claimed blocks, placed ores, canceled breaks, Silk Touch, Fortune, Haste, Mining Fatigue, vein-mining and area-mining mods. Count accepted block breaks once and do not increase drops or multiply another mod's mining traversal.

### 6.7 Moss One — Dwarven

**Identity:** a stocky keeper of sheltered moss gardens and fungal refuges. Provide communal recovery without importing an entire plant-person survival model.

**Active — Mycelial Respite:** create a stationary, non-block-replacing recovery patch at the caster's feet, radius **3 blocks**, for **8 seconds**, on a **55-second cooldown**. Every two seconds, heal **1 HP** to the caster and up to two eligible allies inside it, with **4 HP maximum per recipient per cast** before ordinary healing modifiers. Require line of sight from the patch and a supported placement point. Remove Poison once on the first successful recovery pulse. Do not remove unrelated harmful effects indiscriminately.

Use one shared recovery allowance per recipient across overlapping Moss One patches, so multiple players cannot multiply the pulse rate. Limit a field to **three distinct recipients including the caster** across its entire lifetime, selected deterministically as eligible recipients enter. Apply ordinary healing modifiers once, with a final ceiling of **6 HP restored per recipient per cast** even under amplification. Poison removal can be a valid recovery pulse at full health. Avoid a permanent block or block entity; use bounded server state and client presentation. The field offers no roots, hostile damage, crop growth, or loot multiplication.

**Passive — Living Loam:** **+2 armor**. Consuming a tagged mushroom stew or equivalent finished food adds **1 food point and 1 saturation point**, clamped to normal food limits. This is a small food affinity, not an eating restriction or inventory-based regeneration.

**Weakness — Desiccation:** **+25% fire damage taken** and **−5% movement speed**.

**Presentation:** a squat mushroom shelter, dull green and amber spores, visible field boundary at low density. The field should look like a temporary haven rather than a magical explosion.

**Acceptance:** overlapping fields, full-health players, beneficial potion effects, Poison immunity, food containers, modded soups, logout/death, unloaded chunks, and active healing amplifiers. Environmental placement must not consume items or alter another player's build.

### 6.8 Crystal One — Dwarven

**Identity:** a gem-attuned dwarf who catches and answers one hostile magical or projectile impact. Preserve Runic One's broader ward and crafting identity.

**Active — Prism Reprisal:** open a **6-second** counter window on a **40-second cooldown**. On the first accepted eligible magical/projectile hit, prevent **50% of that hit, capped at 4 HP**. If at least 1 HP was actually prevented and a valid hostile source is still within **8 blocks** and line of sight, answer with one **2 HP** resonance hit. Consume the counter whether or not a legal source exists for the answer.

Use a new, attributed, bounded damage action for the reply. Do not clone, redirect, or amplify an arbitrary modded projectile. Tag the reply so it cannot trigger another racial reprisal, feeding event, retaliatory charge, or infinite reflection chain. The incoming attack's valid hit processing must not be duplicated.

**Passive — Faceted Body:** **+2 armor** and **10% less classified magical damage**.

**Weakness — Fracture Lines:** **−2 maximum HP** and **+20% explosion damage taken**. Avoid an invented “blunt weapon” category unless a real, extensible classifier is implemented and documented.

**Presentation:** a split amethyst shield, one bright fracture at the accepted counter, and a quiet crystal note. Distinguish a counter consumed without a reply from a successful retaliatory hit.

**Acceptance:** shields, absorbed damage, canceled damage, damage-over-time, ownerless projectiles, friendly sources, bosses, magical arrows, chained retaliation, and sources that despawn. No damage may be produced if the triggering hit was rejected.

### 6.9 Bovine — Bestial

**Identity:** powerful horned folk whose signature is a committed charge. Use the minotaur archetype without requiring a nonhumanoid player model.

**Active — Hornrush:** after a short **6-tick readable wind-up**, rush along a server-validated direction for up to **5 blocks**, on a **30-second cooldown**. Limit steering, cap velocity, and test the swept body between positions. The first eligible collision receives **6 HP** ordinary physical damage and moderate knockback; the rush then ends. Never add the held weapon's damage to this collision attack.

Require a valid grounded start. Reject activation while riding, sleeping, swimming, or already executing incompatible movement. A wall or protected entity stops the rush safely. Do not break blocks, trample farmland, disable collision, launch through closed doors, or permit repeated collision hits while standing inside a target.

**Passive — Herd Strength:** **+2 maximum HP**, **+0.20 knockback resistance**, and **+10% movement speed only while sprinting on the ground**.

**Weakness — Heavy Appetite:** **+20% exhaustion** and **−8% attack speed**. Charge commitment is also meaningful counterplay.

**Presentation:** forward-facing horns, ochre and muted green, ground dust following the actual route. Impacts should communicate a single hit.

**Acceptance:** latency, narrow doorways, slopes, slabs, fences, target movement, ally interception, multiple creatures in line, knockback resistance, canceled damage, and charge interruption. Confirm that one activation cannot be converted into repeated full-strength attacks.

### 6.10 Saurian — Bestial

**Identity:** a scaled survivalist whose strength is patience and a close ambush. It should be useful without venom, a shed-skin cleanse, or another flight system.

**Active — Patient Ambush:** after being nearly stationary for **1 second**, press the active key to prepare a **5-second** opportunity, on a **30-second cooldown**. The next successful direct melee hit at at least **90% attack charge** adds **3 HP** and applies Slowness I for **2 seconds**. The player may move after arming; this is preparation, not a requirement to land a melee attack without moving. No invisibility, teleport, extra reach, or repeated on-hit rider.

**Passive — Scaled Survivor:** **+2 armor** and **15 additional seconds of air** before drowning. Implement this as finite breath capacity, preserving ordinary drowning and the distinction from fully aquatic races.

**Weakness — Cold-Blooded:** **−5% movement speed** from a heavy scaled gait; in cold-tagged environments, also **−15% attack speed** and **+25% freezing damage taken**. Provide mitigation through ordinary warming or insulating conditions verified for the target game. Expose a clear cold-state cue. Keep any temperature check bounded and deterministic.

**Presentation:** an alert reptilian eye above layered scales, clay green and warm amber. A held-breath cue marks preparation; a short tail-like motion accent marks the hit without requiring a tail model.

**Acceptance:** no stationary buildup while mounted or carried by another entity; no wall-triggered readiness exploits; no bonus on projectile, sweep, thorn, or secondary damage; warm/cold transitions; respiration effects; and packet replay during the ambush window.

### 6.11 Chelon — Bestial

**Identity:** deliberate turtle-like folk who survive by withdrawing. The shell is a temporary commitment, not a permanent second armor set with reflection.

**Active — Shellfast:** enter a defensive posture for up to **5 seconds**, on a **45-second cooldown**. Reduce eligible direct physical/projectile damage by **50%**, with a **12 HP total prevention budget per cast**. End the posture on expiry, budget exhaustion, or a second distinct active press.

While withdrawn, movement is capped at **20% of normal walking speed** and offensive attacks, shield blocking, item use, spell activation, and block interaction are suspended. Preserve camera control, chat, menus needed for accessibility, and the ability to cancel. Do not manipulate client controls as the only enforcement. No protection from void, drowning, starvation, suffocation, or bypass damage. No passive reflection or healing during the posture.

**Passive — Living Shell:** **+3 armor** and **10 additional seconds of air**. The race is comfortable near water but is not forced to live underwater.

**Weakness — Unhurried:** **−10% movement speed** and **−10% attack speed** outside the stance as well.

**Presentation:** a strong shell silhouette and folded posture cue, muted teal and bronze. Show remaining guard capacity clearly; do not imply immunity.

**Acceptance:** attacks and spells cannot slip through during withdrawal; shields do not create unbudgeted stacking; stuns and menus do not trap the player in the state; disconnected players do not retain it; another mod's movement abilities and effects are restored appropriately on exit.

### 6.12 Zephyr — Faeborne

**Identity:** a delicate air spirit who redirects personal movement with precision. Wind Wyrm must remain the strongest sustained flier.

**Active — Crosswind:** a controlled horizontal burst of up to **5 blocks over roughly 8 ticks**, on a **30-second cooldown**. Cap its vertical contribution to a small hop, not an Avian-style launch. Preserve collision and normal vulnerability. The burst deals no damage and does not automatically reflect projectiles or shove surrounding players.

**Passive — Airborne Essence:** innate gliding and a modest flap profile using the existing flight framework. Start testing around **0.30 upward flap velocity**, **40 ticks between flaps**, and **one feather per flap** when Feathers is active. Include **50% less fall damage**, not immunity to collision or the void. Tune movement through flight tests, because raw flap values alone do not establish travel performance.

**Weakness — Scattered Form:** **−4 maximum HP** and **1.40× knockback received**. Use the established knockback event mechanism rather than a negative resistance attribute that may clamp away.

**Presentation:** a curling breeze emblem, pearl white and fae teal. If represented by wing geometry, create readable air-ribbon or fin-like wings using the existing renderer; otherwise explicitly support a nonanatomical flight presentation. Every mechanical flight state must still be legible to observers.

**Acceptance:** both bound and fallback flight controls, depleted Feathers, Feathers absent/disabled, gliding cancellation, underwater entry, ceilings, repeated bursts, elytra equipment, and dedicated-server loading. Measure traversal against Avian, Sprite, Faerie, and Wind Wyrm.

### 6.13 Nightborn — Undead

**Identity:** nocturnal predators who briefly turn successful hunting into recovery. Do not duplicate Blood Elf's always-on lifesteal or create a compulsory blood economy.

**Active — Crimson Hunt:** an **8-second** hunting window, on a **45-second cooldown**, with **+10% movement speed** during the window. Up to **three** qualifying direct melee hits at at least **90% attack charge** can feed, with at least **20 ticks** between feeding procs. Each restores up to **25% of actual eligible health damage dealt**, capped at **2 HP per proc** and **6 HP per cast** before the race's healing penalty. Apply ordinary healing modifiers once and retain an additional final ceiling of **6 HP actually restored per cast**, even with external amplification. A consumed opportunity does not rearm when the player changes weapons or targets.

Feeding must exclude allies, pets, villagers, armor stands, invulnerable entities, undead/inorganic targets, and recursive ability damage. Protect normal MCA villager interactions through verified entity classification or extension tags. Eligible ordinary monsters and normal food must make solo survival practical. No infection, blood bottling, forced PvP, bat transformation, or evolution is required.

**Passive — Nocturnal Senses:** poison immunity, night vision in low light, and **+15% movement speed at night while food level is at least 12**. Define low-light and night predicates precisely; avoid flickering potion effects.

**Weakness — Sun-Starved:** **−20% healing received** at all times and **+20% damage taken in direct sunlight**. Ordinary food remains usable. Sun protection must have a documented, consistent rule shared with its HUD cue.

**Presentation:** a crescent and restrained crimson drop. Show the finite hunt opportunities and accepted feeding, without gore or constant heartbeat overlays.

**Acceptance:** overheal, absorption-only hits, canceled hits, overkill, high-damage weapons, low attack charge, sweeping, rapid attacks, pet targets, summoned entities, allied undead players, damage reflection, and normal solo play without optional mods.

### 6.14 Returned — Undead

**Identity:** a person sustained by unfinished purpose. Resolve is expressed through returning to a confrontation, never through another resurrection mechanic.

**Active — Unfinished Purpose:** designate the most recent eligible living aggressor who dealt actual health damage to the player within **30 seconds**, provided it is currently loaded, within **16 blocks**, and visible. For **10 seconds**, gain **+15% grounded movement speed while moving meaningfully toward that target**. The first successful fully charged direct melee hit against it adds **2 HP** and consumes the offensive opportunity. Cooldown: **40 seconds**, starting on valid designation.

Keep pursuit direction server-authoritative. Do not auto-walk, change camera aim, pathfind on the player's behalf, reveal remote coordinates, or carry the mark across dimensions. If no valid aggressor exists, explain the condition without charging the cooldown. Environmental, friendly, self-inflicted, and retaliation damage do not create a valid aggressor.

**Passive — Stubborn Remnant:** **+2 maximum HP**, poison immunity, and **+0.30 knockback resistance while below half health**. These are fixed situational traits, not accumulated vengeance stacks.

**Weakness — Imperfect Return:** **−25% healing received**. Keep food, potions, sleeping, and normal respawn behavior understandable; do not reverse healing and harming effects globally.

**Presentation:** an incomplete knot or broken circle pulled closed, muted violet and iron. Show the current target and expiry only while relevant.

**Acceptance:** legitimate ownership attribution for projectiles; aggressor switching; target death/unload; allies becoming hostile or vice versa; self-damage farming; dimension changes; and no interaction with Reaper's legacy revival data, death location, or cooldown.

### 6.15 Wailer — Undead

**Identity:** a sorrowful spirit whose cry disrupts an approaching threat. Preserve Wraith's spectral identity and avoid transforming the cry into a lethal execute.

**Active — Keening Cry:** after **10 ticks** of visible and audible anticipation, release a **6-block cone with a 35-degree half-angle**, on a **40-second cooldown**. Affect at most **six eligible targets**: **2 HP** of ordinary attributed damage, Weakness I for **4 seconds**, and one moderate knockback. Use line of sight and one unified target filter. Movement during anticipation is allowed at reduced speed; interruption or a race change cancels the release without resetting an accepted cooldown.

Bosses receive the bounded damage but no forced movement or broad AI interruption by default. PvP control requires the explicit server policy described later and uses reduced duration. Do not apply compulsory nausea, forced camera motion, instant death, or a universal spell-silence effect.

**Passive — Deathwatch:** poison immunity and short-range awareness of up to **three wounded hostile creatures** within **8 blocks** and line of sight. “Wounded” starts below **50% health**. Give the caster a brief, owner-only directional/target cue, checked at most once per second; no global glowing, ore detection, or world-wide tracking.

**Weakness — Fraying Spirit:** **−4 maximum HP** and **+15% classified physical damage taken**. If playtesting finds the information passive too weak for this cost, improve its situational usefulness or reduce the cost before adding broad damage bonuses.

**Presentation:** a mourning mask and expanding crescent, silver and violet. Provide subtitles and a visual anticipation cue so audio is not the only counterplay.

**Acceptance:** cover, ally interception, bosses, PvP off, successive Wailers, immunity to knockback, canceled damage, no target, and reduced-effects client settings. No target can receive multiple hits from one cry.

### 6.16 Scaleheir — Draconic

**Identity:** grounded humanoid dragonkind whose scales and presence matter more than flight or a chosen elemental breath. This broadens the heritage without weakening existing elemental identities.

**Active — Dominion Roar:** a **35-second cooldown** defensive declaration. Grant the caster a ward for **5 seconds**, preventing at most **4 HP**, and apply Weakness I for **3 seconds** to up to **three eligible threats** within **5 blocks** and line of sight. The roar does not deal damage, force a player to attack the caster, redirect every mob's AI, or bypass boss rules.

**Passive — Inherited Scales:** **+2 armor** and **+0.20 knockback resistance**. No flight, automatic poison, fire immunity, or extra selectable elemental branch.

**Weakness — Demanding Blood:** **+20% exhaustion** and **−8% attack speed**. A Scaleheir remains a capable traveler and has normal access to magic, tools, and equipment.

**Presentation:** an upright dragon crest, deep red with warm bronze. An exhale-shaped roar cue links it to the Draconic family; a restrained scale rim communicates the personal ward.

**Acceptance:** solo defensive use, unrelated neutral creatures, teammates, tame animals, bosses, and beneficial ward stacking. Compare with Valen and Iron One in both unarmored and equipped fights; ensure the shorter active cooldown does not offset every meaningful tradeoff.

Update family descriptions if they currently promise that all Draconic members have elemental breath and flight. Preserve the existing family ID and selection flow.

### 6.17 Wyvernkin — Draconic

**Identity:** lean dragonfolk who commit to one venomous strike. They are not Fire Drake with a different model or Serpen with unrestricted flight.

**Active — Venom Swoop:** on a **35-second cooldown**, perform a short, collision-safe strike. From the ground, lunge up to **3 blocks**; while already gliding, sweep up to **5 blocks** with bounded acceleration and descent. Strike the first eligible target reached along the swept path for **5 HP** physical damage plus **Poison I for 3 seconds**, then end the damaging opportunity. Stop safely on obstruction. No repeated contact damage, weapon-damage addition, automatic critical multiplier, terrain damage, or generic venom-on-every-hit metadata.

**Passive — Lean Wings:** innate gliding, **+1 armor**, and **25% less fall damage**. Start with **no independent powered flap**. The glide and occasional swoop provide mobility while leaving Wind Wyrm and Avian their aerial specialties. Ground use keeps the signature functional in caves without granting teleportation.

**Weakness — Exposed Membranes:** **−2 maximum HP** and **+15% projectile damage taken**. No added immunity solely because the attack uses poison.

**Presentation:** a hooked wing and stinger, dark bronze with restrained venom green. Reuse articulated membrane wing infrastructure with an original texture and distinct silhouette tuning. The sting's visual must match the actual struck target.

**Acceptance:** glide-to-ground transitions, ceilings, landing, mounted activation, water, poison-immune creatures, PvP, multi-entity collisions, and interactions with projectile defenses. Apply poison only through the accepted-hit policy, and make resisted poison readable without converting it into free extra damage.

## 7. Content and metadata manifest

### 7.1 Proposed stable power IDs

Each row represents exactly three files beneath `data/runic_races/powers/<race>/`. Origins reference all three. Hidden timer, state, and condition subpowers belong inside the appropriate bundle.

| Race ID | Active file | Passive file | Weakness file |
| --- | --- | --- | --- |
| `colossan` | `colossal_heave` | `giants_bearing` | `heavy_limbs` |
| `auroran` | `dawnward` | `inner_radiance` | `mortal_vessel` |
| `grove_elf` | `stillleaf_aim` | `canopy_strider` | `slender_frame` |
| `tide_elf` | `currentstep` | `amphibious_grace` | `drying_gills` |
| `astral_elf` | `starbound_thread` | `astral_poise` | `thin_tether` |
| `mountain_one` | `quarry_rhythm` | `bedrock_bearing` | `deliberate_gait` |
| `moss_one` | `mycelial_respite` | `living_loam` | `desiccation` |
| `crystal_one` | `prism_reprisal` | `faceted_body` | `fracture_lines` |
| `bovine` | `hornrush` | `herd_strength` | `heavy_appetite` |
| `saurian` | `patient_ambush` | `scaled_survivor` | `cold_blooded` |
| `chelon` | `shellfast` | `living_shell` | `unhurried` |
| `zephyr` | `crosswind` | `airborne_essence` | `scattered_form` |
| `nightborn` | `crimson_hunt` | `nocturnal_senses` | `sun_starved` |
| `returned` | `unfinished_purpose` | `stubborn_remnant` | `imperfect_return` |
| `wailer` | `keening_cry` | `deathwatch` | `fraying_spirit` |
| `scaleheir` | `dominion_roar` | `inherited_scales` | `demanding_blood` |
| `wyvernkin` | `venom_swoop` | `lean_wings` | `exposed_membranes` |

Keep the established cooldown convention: `runic_races:<race>/<active_file>_cooldown_timer`. The same identifier must resolve in generated JSON, Java, HUD, commands, persistence, and tests. A two-stage active uses that same cooldown identity.

### 7.2 Initial metadata and optional affinities

These are proposed additions, not statements about current metadata. Record every final value centrally rather than adding independent name switches throughout the codebase. `impact` measures lifestyle complexity and disruption, not a tier of strength.

Ars columns mean maximum-mana multiplier / spell-cost multiplier. ISS is the outgoing spell-damage multiplier. Values apply only when the relevant integration is enabled. Include them in balance testing; their magnitudes may need reduction if they stack with power-level modifiers.

| Race | Scale | Feathers pool | Impact | Ars mana / cost | ISS damage |
| --- | ---: | ---: | ---: | --- | ---: |
| Colossan | 1.20 | 26 | 2 | 1.00 / 1.00 | 1.00 |
| Auroran | 1.02 | 18 | 2 | 1.10 / 0.95 | 1.05 |
| Grove Elf | 1.06 | 20 | 1 | 1.10 / 0.92 | 1.00 |
| Tide Elf | 1.04 | 20 | 2 | 1.10 / 0.92 | 1.00 |
| Astral Elf | 1.07 | 16 | 2 | 1.15 / 0.90 | 1.05 |
| Mountain One | 0.76 | 26 | 1 | 0.90 / 1.00 | 1.00 |
| Moss One | 0.70 | 22 | 2 | 1.00 / 1.00 | 1.00 |
| Crystal One | 0.72 | 20 | 2 | 1.05 / 0.95 | 1.05 |
| Bovine | 1.15 | 26 | 2 | 1.00 / 1.00 | 1.00 |
| Saurian | 1.00 | 22 | 2 | 1.00 / 1.00 | 1.00 |
| Chelon | 1.05 | 24 | 2 | 1.00 / 1.00 | 1.00 |
| Zephyr | 0.80 | 14 | 3 | 1.15 / 0.90 | 1.00 |
| Nightborn | 1.00 | 20 | 2 | 1.00 / 1.00 | 1.00 |
| Returned | 1.00 | 24 | 2 | 1.00 / 1.00 | 1.00 |
| Wailer | 0.95 | 18 | 3 | 1.10 / 0.95 | 1.05 |
| Scaleheir | 1.10 | 24 | 1 | 0.90 / 1.10 | 1.00 |
| Wyvernkin | 1.08 | 22 | 2 | 0.90 / 1.10 | 1.00 |

Use **0.0 additional Apotheosis luck** for all 17 initially. Preserve the current Curios family grants: Elven necklace, Dwarven belt, Faeborne ring, Undead charm; no new family grant for Human, Bestial, or Draconic. Reuse the existing stable grant identities and cleanup path. Avoid granting a slot once per race definition rather than once per selected race.

Current Ars behavior combines specific race overrides with family defaults, while the inspected ISS adapter applies flat race-specific damage modifiers. It does **not** establish school-specific affinity support. Reconcile the new metadata with these real behaviors and do not advertise unimplemented school bonuses. [Ars adapter](https://github.com/otectus/runic-races/blob/1877c025265920f2800ac09ba7f194244e9a7e3c/src/main/java/com/otectus/runic_races/integration/ars/ArsNouveauIntegration.java), [ISS adapter](https://github.com/otectus/runic-races/blob/1877c025265920f2800ac09ba7f194244e9a7e3c/src/main/java/com/otectus/runic_races/integration/irons/IronsSpellsIntegration.java).

## 8. Implementation architecture

### 8.1 Extend existing systems with small, owned components

Keep Origins/Apoli JSON for supported declarative effects and Java for mechanics that need reliable server state, collision, attribution, or finite budgets. Do not build another general Origins engine or replace the mod's networking architecture just to add races.

Create a small set of reusable services or equivalent focused classes:

| Responsibility | Consumers | Required behavior |
| --- | --- | --- |
| Prepared strike/shot | Colossan, Grove Elf, Saurian, Returned | One accepted opportunity, explicit expiry, attack/projectile provenance, no repeated proc |
| Finite ward | Auroran, Crystal One, Chelon, Scaleheir | Owned state, damage classification, prevention caps, clear stacking and removal rules |
| Collision-safe short movement | Tide Elf, Bovine, Zephyr, Wyvernkin | Swept geometry, capped displacement, state cancellation, legal targets |
| Temporary anchor | Astral Elf | Same-dimension, finite range/lifetime, safe arrival, no chunk loading |
| Temporary recovery area | Moss One | Bounded recipients/pulses, overlap limits, no permanent terrain changes |
| Validated combat targeting | All harmful abilities | Teams, pets, PvP, ownership, claims, cover, bosses, dead/invulnerable targets |
| Environment predicates | Tide Elf, Grove Elf, Mountain One, Saurian, Nightborn | Shared authoritative conditions for mechanics and HUD |

These are responsibilities, not a demand for those exact class names. Prefer data/parameters over seventeen separate polling loops. Register new actions, conditions, or power factories in the existing registry classes. Validate codec ranges, finite numbers, entity limits, duration limits, and unknown enum values. Do not permit malformed datapack values to create unbounded work.

### 8.2 Combat correctness and targeting

Define explicit targeting modes rather than treating `!isProtectedAlly` as equivalent to hostility:

- **Support:** self, actual allies, and qualifying owned companions. Unrelated players are not automatically allies. Being an Undead race never makes an ally an enemy.
- **Threat-only area control:** legitimate hostile creatures or creatures attacking the caster, filtered for protection and permissions.
- **Aimed attacks:** explicitly aimed legal targets, with player targeting governed by server PvP and team rules. Area riders must not widen the target set silently.
- **Feeding:** a narrower, tag-extensible subset of legal combat targets that can actually bleed. Apply exclusions after permissions and before effects.

Default new racial area control must not displace or disable other players unless an explicit server option enables racial PvP control. Even with that option enabled, honor normal PvP permissions; reduce repeated player-control duration and provide resistance against immediate reapplication. Document this separately from ordinary PvP damage.

All new damaging abilities must preserve attacker ownership and death attribution. Classify physical, projectile, magical, explosion, fire, and bypass damage using the actual pinned game's damage types/tags and verified adapters. Define precedence for overlapping categories. For Auroran, a classified magical projectile uses the magical affinity and does not also gain the physical/projectile weakness; ordinary arrows use the weakness. Crystal One's explosion weakness remains applicable to classified explosions even if another affinity also applies, with that composition made explicit. Bypass damage is never reduced by a racial ward. Do not detect “holy,” “iron,” “cold,” or “blood” by arbitrary substrings in registry names.

Create an accepted-damage accounting path that proves whether damage occurred. A pre-damage amount or a positive proposed event value is not sufficient proof of health lost. Cap feeding by real eligible health loss, excluding overkill and absorption-only damage. Confirm Forge/Apoli event order in the pinned runtime, including later cancellations and other mods. If an event cannot supply the needed guarantee, use a narrowly scoped, audited hook rather than assuming it can.

Never grant healing, venom, counters, mark progress, charge consumption on successful-hit conditions, or retaliation from a rejected attack. Distinguish this from a valid cast that later misses and correctly keeps its cooldown. Internal racial damage must carry provenance so it cannot recursively trigger feeding, counters, prepared strikes, or damage-building systems.

For finite wards, define the mitigation stage and test it with armor, enchantments, shields, external Absorption, resistance, and other wards. Auroran and Scaleheir can intercept up to 100% of an eligible hit until their small HP budget is exhausted; Crystal One and Chelon use their stated percentage and budget. New racial wards **do not add their prevention budgets together** by default: select the candidate that would prevent the most of the current eligible hit, resolve ties deterministically, and consume only that ward's actual prevention. Do not apply a second racial ward to the same hit or replenish capacity merely by selecting a different candidate. Reapplications from the same ward family replace under a documented strongest-remaining-budget/expiry rule rather than adding capacity. Exclude bypass damage. Never erase an unrelated mod's shield or potion effect when racial protection expires.

### 8.3 Input, cooldowns, and state lifecycle

Use server-authoritative state machines, with explicit states such as ready, primed, executing, and cooling down. For multi-stage input, require release and repress; holding the primary key must not place and instantly recall an anchor or toggle a shell repeatedly.

The server validates race, power ownership, cooldown, activation context, range, target, position, and optional cost. Client packets request an action; they never dictate damage, targets, ward capacity, cooldown values, or teleport destinations. Rate-limit invalid requests as well as accepted ones.

Preserve the 1.6.3 performance correction: routine cooldown resources must not be changed and fully synchronized every tick. Current authoring uses 10-tick cooldown decay and 5-tick flap decay. Use short server-side expiry/state logic where movement needs tick precision, and send state changes rather than a full power container for every intermediate frame. [1.6.3 release changes](https://github.com/otectus/runic-races/commit/1877c025265920f2800ac09ba7f194244e9a7e3c).

Define all lifecycle policies before coding:

| Event | New active state | Cooldown obligation |
| --- | --- | --- |
| Death/respawn | Clear anchors, stances, movement, marks, fields, and unused strike opportunities | Preserve the already-spent cooldown using the correct clone/save path |
| Disconnect/reconnect | Clear active opportunities and owned transient effects | Preserve remaining cooldown; reconnect is not a reset |
| Dimension change | Clear dimension-bound state and safely stop movement | Preserve cooldown |
| Race loss/change | Remove only this race's modifiers/effects/state; clear its transient constructs | Retain remaining cooldown for restoration if that race is reselected |
| Data reload | Revalidate definitions, invalidate stale cached references, end incompatible states | Preserve or conservatively translate valid remaining cooldowns |
| Server stop/start | No stale entity references, queued tasks, or active constructs survive incorrectly | Persist cooldown state without wall-clock or dimension-time assumptions |

Default new cooldowns should follow the existing remaining-ticks model and freeze while offline; store enough owned state to avoid refresh exploits when powers are removed. Document any necessary, consistent change after checking actual Origins persistence. Do not copy all namespaced NBT blindly into new active-state systems. Separate durable cooldown data from ephemeral execution state, bound maps, and clear them when their server ends.

Do not shorten an existing cooldown on migration unless that is an explicit documented balance change. Preserve the semantics of old stable keys, especially those still named after removed archetypes.

### 8.4 Movement and scaling

All bursts and lunges must test the path, not just destination occupancy. Consider full player dimensions, slabs, fences, closed doors, liquids, world borders, claims where relevant, and rapidly moving targets. Never enable `noPhysics`, temporary spectator mode, or creative flight as a shortcut.

Pehkui remains optional. Use the project's owned scale type/modifier and cleanup behavior; never issue a global `scale reset`. Test visual size, bounding box, eye height, reach, crouching, swimming, riding, beds, and growth in confined spaces. The repository's scale-range test is a guardrail, not proof that a particular scaled player fits every passage. [Pehkui integration](https://github.com/otectus/runic-races/blob/1877c025265920f2800ac09ba7f194244e9a7e3c/src/main/java/com/otectus/runic_races/integration/pehkui/PehkuiIntegration.java), [scale test](https://github.com/otectus/runic-races/blob/1877c025265920f2800ac09ba7f194244e9a7e3c/src/test/java/com/otectus/runic_races/RaceScaleRangeTest.java).

Distinguish innate glide, powered flap, and rendered wings. In the inspected [`FlightConfig`](https://github.com/otectus/runic-races/blob/1877c025265920f2800ac09ba7f194244e9a7e3c/src/main/java/com/otectus/runic_races/flight/FlightConfig.java), powered flap entries exist for Sprite, Faerie, Avian, and Wind Wyrm; several other races still have gliding power. Do not assume every winged race needs a flap entry. Verify that fold/cancel controls work for both glide-only Wyvernkin and flapping Zephyr.

### 8.5 Environment, food, and data extensions

Use tags and bounded predicates for forest biomes, cold environments, stone support, quarry-eligible blocks, nourishing mushroom foods, blood-ineligible entities, control-resistant entities, and special damage categories. Begin with the following extension points; verify exact registry entries and nested tags before writing their defaults.

| Proposed tag path under `data/runic_races/tags/` | Purpose and default policy |
| --- | --- |
| `worldgen/biome/grove_homes.json` | Forest biomes, using verified vanilla/common biome tags where available |
| `worldgen/biome/cold_environments.json` | Explicit cold biomes; warming/insulation is a separate condition |
| `blocks/stone_support.json` | Ordinary stone-like support, not every solid block |
| `blocks/quarry_eligible.json` | Stone/ore blocks eligible for normal tool-based mining; never bypass hardness or harvest rules |
| `blocks/grove_underbrush.json` | Verified berry-bush-like movement hazards, not generic damaging blocks |
| `items/mushroom_nourishment.json` | Mushroom stew and verified equivalent prepared foods |
| `entity_types/cannot_bleed.json` | Inorganic entities and social entities; combine with undead, ownership, and invulnerability checks |
| `entity_types/racial_control_resistant.json` | Vanilla bosses and verified modded bosses; ordinary damage remains separately governed |
| `damage_type/racial_magic.json` | Verified magical damage types; no assumption that a universal upstream magic tag exists |
| `damage_type/racial_physical.json` | Verified ordinary physical attacks; apply the documented category precedence |

Allow datapacks to extend these with valid optional entries for absent mods. Do not replace a standard global tag merely to implement one race. Include examples for biome expansion, modded food, a modded boss, and feeding exclusions. If a tag is empty or invalid, diagnose it and use a documented conservative behavior rather than matching everything.

Environmental mechanics and their HUD flags must use the same authoritative result. Decide whether a rule checks feet, eyes, body immersion, sky visibility, sky light, time of day, precipitation, or biome tags. Explain the choice. Do not label a dry cave “cold,” a roofed room “direct sunlight,” or any fluid “water.” Dimensions without a normal sky cycle need a documented neutral/default behavior.

Cache infrequent environmental checks, typically every 10–40 ticks as appropriate. Deathwatch should check at most once per second. Avoid loading chunks, scanning entire dimensions, tracking every mob globally, or evaluating a large block volume every player tick.

Food bonuses occur after a real accepted consumption and preserve containers, stack sizes, food effects, and other mods' behavior. Do not create items for free or let repeatedly opening a menu produce nourishment. Temporary recovery areas do not fertilize, replace, or harvest blocks.

### 8.6 Configuration ownership

Keep one authoritative location per gameplay value. Prefer existing power JSON and parameters for race-specific effects. Use server configuration for shared policy, integration toggles, validated safety bounds, and clearly documented options such as racial PvP control. Client configuration owns presentation only.

Do not define a cooldown independently in TOML, Java, generated JSON, and localization. Generate descriptions from final authored values where practical or enforce consistency with tests. If a pack overrides values, diagnostics must show effective values; tooltips must not confidently show a false default.

Optional resource costs are not required for these new primary actives. Initially keep their core balance cooldown-based; Feathers continues to govern supported flaps. If testing justifies an optional resource cost, implement the existing resource-availability posture correctly and test absent, present-enabled, present-disabled, and insufficient-resource cases. Never make an unavailable integration silently block a race.

## 9. Integration and coexistence requirements

- **Origins Forge / Apoli / Calio:** validate every custom factory and JSON subpower in the pinned runtime. The old invalid `subpowers` index-array pattern must not return. Preserve the three-power structure and primary input contract.
- **Pehkui:** verify both present and absent behavior and interaction with other scale sources. Scaling must not alter unrelated modifiers or create suffocation on selection.
- **Feathers:** use current pool and flap mechanisms, honor disabled integrations, spend only valid costs, and provide clear denial. No free flaps from packet replay or accepted-cost double charging.
- **Curios:** apply family grants once, remove them safely on race changes, and respect occupied extra slots. Test changing between two races in the same family and then another family.
- **Ars Nouveau and Iron's Spellbooks:** account for existing power-level and integration-level damage effects so a nominal bonus is not applied twice. Keep common-side code loadable when either mod is absent. Test spell and ability interruption during Chelon withdrawal using verified events or adapters.
- **Apotheosis:** use neutral new luck metadata initially and test high-attribute equipment against finite damage/ward/healing caps. Do not grant extra affixes, gems, or enchantment levels as incidental race traits.
- **Other Origins add-ons/datapacks:** preserve `disableDefaultOriginLayer` and the documented world-load behavior. Keep foreign-origin injection supported. Native roster coverage checks must not reject legitimate external origins because the total exceeds 54.
- **Runic Skills / Runic Gods and other combat mods:** race does not gate their content or duplicate progression. Test representative attack-speed, damage, healing, movement, and spell modifiers. Add optional adapters only where an actual interaction requires one.
- **Claims, teams, pets, and MCA:** protect normal social entities and settlement gameplay. Do not make Undead automatically hostile to villagers or guards, disable trading, or introduce crime/reputation penalties. Feeding and area abilities must respect the explicit exclusions.

Inspect the project's current handling before inventing broad compatibility promises. A documented manual test is acceptable evidence; the mere absence of compilation errors is not.

## 10. Presentation, localization, and discoverability

Finish the experience for every race, including:

- Family/race selection entry, original race icon, registered icon item/model, and accurate impact rating.
- One original active HUD icon, readable at 16 and 32 pixels, consistent with the current pixel-art grammar. Avoid seventeen recolors of a generic rune.
- Names, origin descriptions, all three power descriptions, durations, caps, conditions, weaknesses, denial messages, command labels, and subtitles.
- `SignatureKey` / `SignatureRegistry` entry for the active; concise passive/weakness proc presentation only when useful.
- Ambience registration or a deliberate quiet entry, plus wing data/textures where the race actually uses them.
- State flags, owner-only state, HUD progress, notification rules, and rune textures that reflect real server state.

Retain the established family presentation grammar: Human snap, Elven rise/implode, Dwarven ground burst, Bestial lunge, Faeborne swirl/pop, Undead sink/drain, and Draconic exhale. Current authoring uses minor, major, and mythic presentation tiers. New routine actives generally belong in the **30–60 authored-particle major range**, counted across the complete cast and target impacts, not separately for every affected entity. [Presentation conventions](https://github.com/otectus/runic-races/blob/1877c025265920f2800ac09ba7f194244e9a7e3c/CLAUDE.md).

Honor reduced motion, simple cues, disabled shake/FOV effects, opacity, HUD placement, and density settings. Do not make screen effects carry essential information alone. Avoid repeated chat messages, constant denial sounds, looping weakness banners, and nausea. A Wailer cry needs a readable visual warning and subtitles even with quiet audio.

Keep icons and descriptions honest: a 4 HP finite ward must not be described as invulnerability; a reveal must not promise wall vision if it requires line of sight; a scale-only change must not claim longer reach. Spell out counterplay in plain language rather than hiding mechanics in poetic lore.

New state flags must have an actual setter and clear/removal path. Audit the existing bitfield capacity, retired bits, synchronization, command output, rune assets, and tests. Reuse a flag only when its meaning is identical; do not assign a different interpretation to a bit already used by clients. Owner-specific targets and anchors need bounded dedicated state rather than being forced into a boolean flag.

## 11. File-by-file completion map

Paths below are relative to the repository root. Inspect current equivalents if files have moved.

| Area | Existing locations to extend or verify |
| --- | --- |
| Version/build | `gradle.properties`, `build.gradle`, `src/main/resources/META-INF/mods.toml`, CI and dependency retrieval |
| Roster metadata | `src/main/java/com/otectus/runic_races/race/RaceDefinition.java`, `RaceRegistry.java` |
| Origins and layers | `src/main/resources/data/runic_races/origins/`, `origin_layers/family.json`, `origin_layers/race.json` |
| Powers | `src/main/resources/data/runic_races/powers/` and matching generator definitions |
| Custom execution | `action/`, `condition/`, `power/`, `event/`, `util/`, and `registry/ModEntityActions`, `ModEntityConditions`, `ModPowerFactories` |
| Persistent/transient state | `common/state/`, the current lifecycle hooks, and narrowly scoped new state components |
| Networking | `network/NetworkHandler.java`, new validated packet types only as necessary |
| Flight | `flight/FlightConfig.java`, `FlightServerHandler.java`, `client/FlightInputHandler.java`, and fold/cancel behavior |
| HUD and visuals | `client/AbilityIconRegistry.java`, cooldown/state overlays, ambience, `client/render/WingType.java`, `WingRenderLayer.java` |
| Signature and feedback | `presentation/`, `notification/`, `registry/ModSounds.java`, particle registration and providers |
| Asset metadata | `registry/ModItems.java`, `assets/runic_races/models/item/`, textures, sounds, language, rune/wing assets |
| Optional integrations | `integration/` with centralized new metadata and guarded loading |
| Generation | `tools/generate_races.py`, `race_lang.json`, `build_lang.py`, and applicable original-asset generators |
| Diagnostics/docs | `command/RRCommands.java`, `README.md`, `BALANCE.md`, `CHANGELOG.md`, pack-author examples |
| Verification | Existing `src/test/` coverage plus meaningful new behavior tests and runtime scenarios |

For the built-in dataset, the expected result is **54 race definitions, 61 origin JSONs including seven family selectors, and 162 top-level race power JSONs**. This means **17 new origins and 51 new power files**. Keep the two origin layers and existing family IDs. Verify origin ordering, scrolling, selection, back-to-family behavior, and confirmation with the larger lists.

Derive routine registry coverage from the registry, not scattered `37` literals. Keep explicit release-roster assertions where they test a real contract. Register packet IDs append-only and bump the protocol when additions or incompatible payload changes require it. Clients and servers must run compatible 1.7.0 builds; do not preserve a false compatibility claim merely to avoid a protocol bump.

## 12. Verification and balance plan

### 12.1 Automated verification

Run and preserve the existing meaningful tests, including data parity, JSON linting, resource gates, cooldown IDs, attribute UUIDs, language/duration consistency, icon/ambience/signature/wing coverage, state-rune parity, VFX budgets, packet registration, and version consistency. The repository already contains these categories; expand their expectations rather than deleting failing checks. [Existing tests](https://github.com/otectus/runic-races/tree/1877c025265920f2800ac09ba7f194244e9a7e3c/src/test/java/com/otectus/runic_races).

Add behavior-focused coverage for the concrete risks introduced here:

- Prepared strike and projectile empowerment are consumed once and cannot reenter on their own damage.
- Invalid/canceled damage produces no feeding, venom, retaliation, or successful-hit reward.
- Ward prevention equals the accepted amount prevented, stays within the finite budget, and respects replacement/ownership rules.
- Death, clone, relog, restart, dimension change, race change, and reload preserve cooldowns and remove transient state correctly.
- Multi-stage input cannot repeat through held keys or replayed packets.
- Movement and anchors reject obstructed, out-of-bounds, unloaded, or unsafe positions using the current player size.
- Target filters distinguish support, hostile area control, aimed attacks, bosses, pets, and feeding eligibility.
- Generator check mode reports no unintended semantic differences from committed data.

Prefer pure tests for math, state transitions, selection rules, and serialization, with GameTests or actual runtime tests for Forge events, collision, effects, networking, and optional APIs. Do not present a source-pattern test as proof of gameplay correctness.

### 12.2 Runtime matrix

At minimum, verify a dedicated server and a remote client, plus singleplayer where client lifecycle differs.

| Scenario | What must be demonstrated |
| --- | --- |
| Required dependency stack only | All 54 built-in races load; all 17 new actives and drawbacks work |
| Optional integrations enabled | Correct metadata, costs, extra slots, scale, spell effects, and stacking |
| Optional integrations absent/disabled | No classloading failures, unusable active gates, stale attributes, or misleading messages |
| Existing world upgraded from 1.6.3 | All old race selections, cooldowns, relevant persistent data, and items remain valid |
| Race cycling | Old modifiers, fields, anchors, stances, and slots are cleaned up; cooldowns cannot be refreshed |
| PvP disabled and enabled | Legal targets only, readable counters, bounded player control, no teammate or pet grief |
| Ordinary Overworld day/night and varied biomes | Correct sunlight, cold, dry, forest, and support-terrain conditions |
| Nether, End, and one modded dimension | Neutral/explicit environment behavior and viable travel |
| Resource/terrain/equipment interactions | Boats, mounts, water, doors, armor, shields, bows, magic, and containers remain usable |
| Reload/restart/disconnect during actives | No duplication, ghost state, stuck shell, remote anchor, free recast, or resource loss |
| Reduced effects and common GUI scales | Legible menus/HUD, no clipped names, usable controls, no mandatory disruptive effects |

### 12.3 Comparative balance scenarios

Use identical difficulty, equipment, effects, and optional mods for each comparison. Record the setup, sample count, and outcomes; do not invent precise test results.

1. **Early survival:** normal travel, resource gathering, food use, one hostile encounter, and escape from a disadvantageous environment. No required gear unavailable to a starting player.
2. **Equipped combat:** iron and late-game armor, shield and no shield, ordinary melee, ranged, and magic. Measure damage dealt, damage received, effective survival, healing, and ability uptime together.
3. **Traversal:** fixed ground, forest, mountain, water, cave, and aerial routes. Compare travel time, resource cost, fall/collision risk, and ability repetition.
4. **Support:** self plus two allies under controlled incoming damage. Compare Auroran, Moss One, Runic One, Dryad, and relevant existing healing sources. Test mixed support compositions for loops.
5. **Focused identities:** charge versus heavy strike; ambush versus pounce; shell versus passive armor; feeding window versus Blood Elf; anchored recall versus escape; aerial sting versus breath.
6. **Optional-mod stress:** high damage, high attack speed, healing amplification, extra armor, movement bonuses, and magic-cost changes. Fixed caps must remain fixed where intended.

For every new race, write a short final balance note identifying its best situations, weak situations, nearest comparisons, practical counterplay, and any changes from these starting values. “Balanced” means supported by these tests and tradeoffs; the design reference's zero-sum goal is not a literal formula proving equivalence.

### 12.4 Performance verification

Profile representative idle, travel, and combat scenarios with mixed races and repeated abilities. Include many eligible creatures nearby and several players using support or movement abilities simultaneously.

Require bounded target counts, bounded transient objects, cleanup of all per-player maps, no inactive per-tick entity scans, no forced chunk loading, and no full power-container sync every cooldown tick. Compare against the baseline under the same workload and record hardware, player counts, entity counts, tick timing, and packet behavior. Investigate concrete regressions rather than claiming performance from code structure alone.

## 13. Execution order

Complete shared foundations first, then implement the races in manageable groups while retaining a complete release checklist:

1. **Baseline and safeguards:** capture current source behavior; reconcile generator drift; establish damage classification, target policies, state ownership, and meaningful regression tests.
2. **Content plumbing:** add registry entries, layer membership, initial three-power bundles, metadata, localization, and validation. Temporary development placeholders must not survive into the release.
3. **Prepared strikes and work abilities:** Colossan, Grove Elf, Mountain One, Saurian, and Returned.
4. **Defense and support:** Auroran, Moss One, Crystal One, Chelon, and Scaleheir.
5. **Movement and aerial behavior:** Tide Elf, Astral Elf, Bovine, Zephyr, and Wyvernkin.
6. **Feeding and control:** Nightborn and Wailer, exercising the already-established accepted-hit and targeting rules.
7. **Presentation and integration completion:** original icons, signatures, state feedback, wings, optional metadata, pack-author configuration, and diagnostic commands.
8. **Release validation:** all automated gates, runtime matrix, comparative balance, upgrade tests, and performance review; resolve observed defects and recheck the affected gate.

The order is an implementation aid, not permission to release only the first groups. Maintain a per-race checklist covering data, gameplay, weakness, presentation, integrations, lifecycle, and verification until all 17 are complete.

## 14. Required deliverables and definition of done

Deliver:

- The complete, reviewable 1.7.0 source changes and generated assets/data.
- A correctly built and reobfuscated `runic_races-1.7.0.jar` when the environment can resolve dependencies and build it.
- Updated README roster/counts, balance reference, player-facing changelog, and accurate version metadata.
- Pack-author documentation for new tags, policy switches, optional affinities, and examples.
- An implementation/validation record with a 17-race completion matrix, final values, notable source findings, tests actually run, and any remaining release blockers.

Use the existing Gradle build/reobfuscation lifecycle. Resolve required compile/runtime dependency setup rather than treating missing local jars as a finished implementation. Do not claim a successful build, runtime test, artifact, or compatibility result without executing and observing it.

Version 1.7.0 is complete only when:

- All 17 named races are selectable under the correct heritage and behave as documented.
- All 54 built-in races have valid data and the old 37 retain their save identities.
- Every new race has one finished active, one coherent passive bundle, and one functioning weakness bundle.
- None of the new races silently inherits an unrelated default, missing icon, dead integration gate, generic placeholder power, or ineffective weakness.
- Active input, cooldowns, ownership, multiplayer targeting, lifecycle cleanup, and finite budgets are verified.
- Original presentation is complete and respects accessibility settings.
- Core play works without optional integrations, and enabled integrations behave as documented.
- Relevant automated tests, upgrade checks, and multiplayer runtime gates pass.
- Comparative testing supports meaningful choices alongside the existing roster, with no unrestricted flight/damage/healing loop or unbounded support stacking.

If a gate cannot be executed in the available environment, finish all work that can be completed, name the exact unresolved gate and reason, and provide reproduction steps. Keep the distinction between **implemented**, **build-verified**, **runtime-verified**, and **release-ready** explicit. Do not hide incomplete gameplay behind a feature flag or mark a stub as complete.

Proceed autonomously from inspection through implementation, testing, refinement, and release preparation. Be thoughtful and creative within these constraints. The goal is a roster that offers seventeen new ways to play while still feeling unmistakably like Runic Races.

---

## Research provenance and limits

All references were accessed on September 8, 2026. Runic Races links above are pinned to the inspected commit where applicable. The authoring-drift result was reproduced by executing that generator in a separate directory and comparing parsed committed/generated JSON; it was not inferred solely from documentation.

External sources are first-party project documentation, author-maintained repositories, or author-published project/release descriptions. Their mechanics informed the tradeoffs; none establishes measured balance for Runic Races. The Medieval source is the historical `1.20.1-multiloader` branch of `muon-rw/Medieval-Origins-Old`. The inspected Banshee power blob was `a6fd733a1b0d561826b1b1cba89d4ccbdee8fcf4`; its language blob was `5f801bf519cd36ce717f2394d9a4cf87af1d7245`. The inspected Stargazer README blob was `4dca23afd7d44410f26f7c2f601a26a6cbe3b5ee`; the Minataur charge and size blobs were `b1d69a72bd190cc41a078a9f27c932ef20c5a8bf` and `e3945b68918c3ff906712f940e57a6b61d9cb78b`.

Source authors/publishers: **otectus** for Runic Races; **apace100 / Origins documentation contributors** for base Origins and power documentation; **MoriyaShiine** for Extra Origins; **muon-rw and project contributors** for Medieval Origins Revival source; **Venekiel** for Origins: Vampire; **0vergrown** for Stargazer; **The-Architects727** for DnD Monster Origins; **lochnessdragon** for Origins+. Most project descriptions are undated live documents; no publication date is inferred from their wording. Extra Origins 1.20-7 has the explicit release date noted above.

No exact primary-source pack was required or established for every proposed race. Saurian's nonvenom ambush role and Crystal One's bounded counter, among other details, are original recommendations derived from this roster's open design space. Verify APIs, game behavior, and final numerical balance during development. Markdown structure, roster coverage, and reference consistency were reviewed for this deliverable; no game build or graphical document rendering is represented as performed.
