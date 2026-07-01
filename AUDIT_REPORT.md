# Runic Races Full Bug / Stability / Consistency Audit

**Audit date:** 2026-07-01 · **Version audited:** 1.1.0 (`main` @ d565aa7) · **Method:** full build + test run, static analysis of all 78 Java classes, all 44 origins / 111 power JSONs / 2 layers, assets, docs, and bytecode-level verification against the bundled Origins/Apoli/Calio jars. Runtime game launches (client/server) were not possible in this environment; everything runtime-dependent is marked with its confidence level.

Findings from parallel review passes were **re-verified before inclusion**. Notably, four plausible-sounding claims were checked against decompiled dependency jars and *refuted* (they do NOT appear as issues below):
- `origins:modify_healing` "does not exist" — **false**: Apoli 2.9.0.8 registers `modify_healing` (`ApoliPowers.MODIFY_HEALING`, a `ModifyValuePower`) and hooks it into `LivingHealEvent` via `ApoliPowerEventHandler`. The blood_elf/reaper/demon/zombie healing weaknesses load and work.
- "AoE actives hit the caster" — **false**: `AreaOfEffectAction` bytecode skips the acting entity unless `include_target` is true (default false). No Runic power self-applies its AoE. (Ally/pet targeting is a real, separate issue — see Major #7.)
- "Forge Blessing cooldown resets on relog" — **false**: Forge serializes `getPersistentData()` under the entity's `ForgeData` NBT, and `RacialEventHandler.onPlayerClone` copies all `runic_races:*` keys across death. Cooldowns persist correctly.
- "Nine Lives triggers before absorption is consumed" — **false**: `LivingDamageEvent` fires post-armor and post-absorption in 1.20.1, so the lethality check is accurate.

---

## Executive Summary

Runic Races is in **much better engineering shape than a typical content mod of this size**. The infrastructure layer — networking, client/server separation, race resolution, integration guards, attribute-modifier hygiene, persistence — is genuinely well built: server-authoritative flight with rate limiting and cooldown re-checks, zero client-class references from common code, reflection-gated optional integrations, UUID-diffed transient modifiers with `onRemoved` cleanup, and a fully consistent data layer (0 malformed JSON, 0 dangling references, 42/42 cooldown resource IDs matching between Java and JSON in both directions, 42/42 HUD icon textures present, 0 missing lang keys out of 454).

The problems are concentrated in the **content/gameplay layer**, where several abilities and weaknesses are silently broken:

1. **Skeleton's active summons hostile vanilla skeletons that attack the player** — the purpose-built friendly `GraveServantEntity` is dead code referenced by no JSON.
2. **Arachnid's Web Snare trap block can physically never trigger** (empty collision shape + `stepOn` semantics).
3. **Two races' signature biome passives reference a tag that doesn't exist** (`forge:is_forest`), and **four races' knockback weaknesses are no-ops** (vanilla attribute clamp).
4. A datapack-facing **divide-by-zero crash vector** exists in both custom power types (`check_interval: 0`), though shipped data never triggers it.

**Stability verdict:** safe for singleplayer, multiplayer, and dedicated servers — no crash paths reachable from shipped data, no dedicated-server classloading hazards, no packet abuse vectors found. **Gameplay verdict:** roughly 6 of 37 races have a significantly broken or misfiring core mechanic, and several weaknesses that the balance model depends on don't actually apply.

---

## Build and Runtime Status

- **Build result:** ✅ `./gradlew build` — BUILD SUCCESSFUL (35s, 9 tasks). ⚠️ Only after overriding `org.gradle.java.home`: `gradle.properties:5` pins the machine-specific path `/usr/lib/jvm/java-17-openjdk`, which fails on any machine where that exact symlink doesn't exist (here: `java-17-openjdk-amd64`). This is a committed portability bug.
- **Test result:** ✅ `:test` passed — 3 JUnit 5 classes (`RaceStateFlagsTest`, `I18nKeyPresenceTest`, `NotificationRegistryTest`).
- **Client load status:** not launchable here. Static analysis: all client classes registered via `@Mod.EventBusSubscriber(value = Dist.CLIENT)` on the correct buses; overlays, keybinds, wing layer, and entity renderer registered in `ClientEvents` (MOD bus). High confidence it loads.
- **Dedicated server load status:** not launchable here. Static analysis: **no common code imports any `client/` class** (grep-verified). S2C packet handlers use the nested-`ClientHandler`-behind-`DistExecutor.unsafeRunWhenOn` pattern, so client classes are never classloaded server-side. High confidence it loads.
- **Optional dependency status:** all six integrations (`ars_nouveau`, `irons_spellbooks`, `curios`, `apotheosis`, `pehkui`, `feathers`) are `compileOnly`, gated by `ModList.isLoaded()` + config + `Class.forName` reflection in `IntegrationManager.tryLoad`. Direct mod-type imports exist only inside the reflectively-loaded classes — the correct pattern. Feathers/Iron's resource gating is fully reflection-based with a configurable fail-closed posture.
- **Scenario matrix (static assessment):** fresh world ✅; existing world after update ✅ (persistent data is namespaced NBT + Origins-owned origin storage); datapacks ✅ (Origins/Apoli own power reload); malformed race JSON — Origins/Apoli logs and skips bad powers, but see Data/Reload review for the `check_interval` exception; compat mods absent/present ✅.
- **Blockers:** none for building. `gradle.properties` java.home pin is the only thing standing between a fresh checkout and a green build.

---

## Critical Issues

### C1. Skeleton "Conscript the Dead" summons hostile vanilla skeletons that attack their summoner
- **Location:** `data/runic_races/powers/skeleton/conscript_the_dead.json:58` (`"entity": "minecraft:skeleton"`); `action/SummonMinionAction.java`; `entity/GraveServantEntity.java`
- **Problem:** `SummonMinionAction` only stamps owner/expiry NBT tags; the *ally AI, expiry handling, and no-sunburn behavior live in `GraveServantEntity`* (its javadoc says so, `SummonMinionAction.java:22-24`). The JSON summons `minecraft:skeleton`, which ignores those tags. `GraveServantEntity` (`registry/ModEntities.java`, attributes wired in `RunicRacesMod`) is referenced by **zero** JSON files — verified by grep across `data/` and `assets/`.
- **Why it matters:** the race's signature active spawns two armed, fully hostile skeletons 2.5 blocks from the caster that target the player, never expire, and burn in daylight. The ability is strictly self-harming; the in-game description ("skeletal servants to fight at your side") is false.
- **Reproduction path:** select Skeleton, press the ability key, get shot by your own "conscripts."
- **Recommended fix:** change line 58 to `"entity": "runic_races:grave_servant"`. Also add the missing `entity.runic_races.grave_servant` lang key.
- **Confidence:** high (JSON + code verified; not run in-game).

### C2. Arachnid "Web Snare" trap block can never trigger
- **Location:** `block/TrapMarkerBlock.java:53-55` (`getCollisionShape` → `Shapes.empty()`) + `:63` (`stepOn` trigger)
- **Problem:** in 1.20.1, `stepOn` is invoked on the block at `Entity.getOnPosLegacy()` — the **supporting** block below the entity's feet. A collisionless block occupying the feet cell is never the supporting block, so walking onto/through the trap never calls `stepOn`. `TrapMarkerBlockEntity.serverTick` only handles expiry; there is no other trigger path. The trap sits inert for its 200-tick lifetime and despawns.
- **Why it matters:** arachnid's active is reduced to a self Slow-Falling + AoE slow; the placed-trap half (4.0 dmg + Slowness III on victims) is dead, and the race plays far below its documented power.
- **Reproduction path:** place a trap, walk any mob across it — nothing happens.
- **Recommended fix:** override `entityInside(BlockState, Level, BlockPos, Entity)` (the pressure-plate/wither-rose pattern) instead of `stepOn`. While there: the 200t (10s) lifetime vs. the 700t (35s) cooldown makes the trap near-useless even once fixed — consider 1200–2400t.
- **Confidence:** high (mechanics verified against vanilla `stepOn` call sites; not run in-game).

### C3. Divide-by-zero crash vector in both custom power types (`check_interval: 0`)
- **Location:** `power/BiomeAffinityPower.java:76`, `power/ScalingAttributePower.java:64` — `entity.tickCount % power.getConfiguration().checkInterval() == 0`
- **Problem:** the codec (`optionalFieldOf("check_interval", 20)`) accepts any int, including 0. A power JSON with `"check_interval": 0` throws `ArithmeticException` in `canTick` every tick, on the server.
- **Why it matters:** the mod explicitly supports third-party race datapacks (`examples/datapacks/runic_races_extra_races/`). A pack author's typo hard-crashes the server tick loop. Shipped data always uses 40, so vanilla installs are safe.
- **Reproduction path:** add-on datapack power with `"type": "runic_races:biome_affinity", "check_interval": 0` → join world → server crash.
- **Recommended fix:** clamp at read (`Math.max(1, checkInterval())`) or validate in the codec (`Codec.intRange(1, Integer.MAX_VALUE)`).
- **Confidence:** high (arithmetic is unconditional; only third-party data can trigger it).

No crashes, data corruption, or packet-abuse vectors were found in shipped code + data. `FlightServerHandler` re-validates race, flight config, airborne state, a 2-tick rate limit, and the Origins cooldown resource on every flap packet — a malicious client cannot fly as a non-flyer or bypass cooldowns.

---

## Major Issues

### M1. `forge:is_forest` biome tag does not exist — canine and dryad biome passives are dead code
- **Location:** `powers/canine/pack_hunter.json:37,40`; `powers/dryad/one_with_the_grove.json:18,21`
- **Problem:** Forge 1.20.1 defines `forge:is_hot/is_cold/is_mountain/is_water` etc., but **no `forge:is_forest`** (the forest tag is vanilla `minecraft:is_forest`). The mod ships no tag JSON of its own. `BiomeAffinityPower.resolveTag` creates the TagKey unconditionally and `Holder.is(unboundTag)` is silently false forever.
- **Why it matters:** Canine's "+6% speed / +5% damage in forests" and Dryad's grove affinity — both advertised in README/BALANCE — never activate. Silent failure, no log.
- **Fix:** change both to `minecraft:is_forest`, or ship `data/forge/tags/worldgen/biome/is_forest.json`. Consider a warn-once in `resolveTag` when a tag resolves to no biomes after registry load.
- **Confidence:** high (Forge tag set verified against the 47.2.0 universal jar).

### M2. Negative knockback-resistance "weaknesses" are no-ops on four races
- **Location:** `powers/celeron/featherweight_frame.json` (−0.4), `powers/sprite/fragile_essence.json` (−0.5), `powers/faerie/cold_iron.json` (−0.4), `powers/avian/hollow_bones.json` (−0.4)
- **Problem:** `generic.knockback_resistance` is a `RangedAttribute` clamped to [0.0, 1.0] with base 0.0. An additive negative modifier clamps back to 0 and does nothing until the player wears KB-resist gear (netherite), and even then only partially.
- **Why it matters:** four races' documented tradeoffs ("knocked back further", BALANCE.md's "2× knockback" for sprite) don't exist. Sprite in particular ends up with the strongest survivability kit in its family (0.45-scale hitbox, permanent flight, fall immunity, +30% speed) against an effective weakness of just −3♥ — at **impact 1**, the lowest rating in a family of 2s and a 3.
- **Fix:** implement a Java `LivingKnockBackEvent` handler that multiplies knockback strength for these races (read a per-race multiplier from `RaceRegistry` so it stays metadata-driven), and remove the dead attribute modifiers. Re-rate sprite's impact to 2 (or give the weakness real teeth first).
- **Confidence:** high (vanilla attribute range is definitive).

### M3. Valen shoulder-check violates its own contract and is exploitable
- **Location:** `event/RacialEventHandler.java:554-572` (`onLivingAttack`)
- **Problem:** the javadoc says "sprinting into a *hostile mob* … extra knockback on the *first hit* of a sprint." The code has: no hostility check (pushes players, villagers, pets), no once-per-sprint gating (**every** sprint hit adds a (0.6, 0.25, 0.6)·look push), fires on `LivingAttackEvent` for **any** damage source owned by the player — a sprinting Valen's **arrows and tridents apply the melee shove at any range** (indirect sources return the owner from `getSource().getEntity()`) — and applies even if the attack event is subsequently cancelled.
- **Why it matters:** permanent juggle/stun-lock of mobs and players by spam-clicking while sprinting; ranged knockback with no enchantment; PvP griefing. Also entirely undocumented in lang/README.
- **Fix:** require `event.getSource().isDirect()`-style check (source entity == direct entity), an `Enemy` instanceof filter (reuse `GlowHostilesAction.isHostile`), and a per-sprint latch (persistent-data flag cleared when `isSprinting()` goes false).
- **Confidence:** high.

### M4. Reaper revival fail-path burns the 30-minute cooldown and leaks attempts forever
- **Location:** `event/RacialEventHandler.java:158-234`
- **Problem:** at death the cooldown is stamped and `REVENANT_REVIVAL_ATTEMPTS` incremented (`:174,181`) before the respawn hook knows whether revival will succeed. On failure (no safe position `:220-224`, or unresolvable dimension `:225-228`): (a) the next 30 minutes of deaths get no revival at all; (b) attempts are only reset on success (`:216`), so failures accumulate **across sessions** — after every 3 accumulated failures, one otherwise-valid future revival is silently skipped by the `MAX_REVIVAL_ATTEMPTS` branch (`:168-173`). The bad-dimension path also fires no player-facing cue (`REAPER_REVIVAL_REJECTED` only fires on the no-safe-position path).
- **Compounding lang bug:** `power.runic_races.reaper.harbinger_of_death.description` promises revival "**every 10 minutes**"; `REVENANT_REVIVAL_CD_TICKS = 36000` = **30 minutes** (`:91`).
- **Fix:** move cooldown-stamping and attempt-increment to the *success* path of `onPlayerRespawn` (or refund both on failure); fire `REAPER_REVIVAL_REJECTED` on the bad-dimension path too; align the lang text with the real cooldown.
- **Confidence:** high (logic), design-intent medium.

### M5. Moon Elf's `night_regen` passive is a configured no-op that its tooltip advertises
- **Location:** `powers/moon_elf/tidecallers_grace.json` — `runic_races:scaling_attribute` on `generic.max_health` with `day_value: 0.0` **and** `night_value: 0.0`
- **Problem:** `ScalingAttributePower` never adds a modifier for value 0. The subpower does literally nothing (and max_health isn't a healing mechanic anyway), while `en_us.json` promises "stronger natural healing under the night sky." Side effect: the `NIGHT_EMPOWERED` HUD flag can never fire for moon_elf.
- **Why it matters:** a third of the race's passive is missing; moon_elf pays one of the family's heavier weaknesses (−1.5♥, −10% magic dealt, day attack malus) for the thinnest delivered passive.
- **Fix:** implement it — e.g. a conditioned `origins:regeneration`-style tick (heal 1.0 every N ticks at night) or repoint scaling_attribute at a real value pair.
- **Confidence:** high.

### M6. Tremor abilities silently no-op while consuming their cooldown
- **Location:** `action/TremorPingAction.java:88-101` (`canPing`: sneaking OR stone underfoot OR no sky) with callers `powers/deep_one/tremorsense.json` (300t CD) and `powers/terra_drake/seismic_breath.json` (800t CD)
- **Problem:** the gate lives in Java, but the JSON `origins:and` unconditionally fires the banner, sound, particles, and sets the cooldown. Standing on grass in daylight: deep_one burns 15s of cooldown with full presentation and zero effect; terra_drake's breath still lands but the promised "shaken from cover" glow never happens.
- **Fix:** either replicate the gate as a JSON condition on the active (so the key-press is rejected before the cooldown is set), or set the cooldown from Java only when `canPing` passes, or drop the gate for the seismic_breath caller (the class doc says it was designed for Tremorsense only).
- **Confidence:** high.

### M7. Rune of Warding (and other support AoEs) hit allies, pets, and villagers
- **Location:** worst case `powers/runic_one/rune_of_warding.json` (Slowness II r6, docs call it a "party ward"); same pattern in `magi/arcane_overflow`, `high_elf/arcane_reflex`, `ice_elf/frostbind`, `kitsune/foxfire_illusion`, `nymph/sirens_charm`, `demon/infernal_wrath` (weakens + **sets allies on fire**), `wraith/spectral_phase`, `reaper/soul_harvest`, `dryad/verdant_bloom`
- **Problem:** every offensive AoE's `bientity_condition` is only `origins:living`. The caster is excluded (verified), but party members, tamed pets, and villagers all eat the debuffs. For runic_one this inverts the documented design ("party ward" that slows your party).
- **Fix:** minimum: fix runic_one (this one contradicts docs). Better: add a shared hostile filter — Apoli `origins:entity_type`/tag-based bientity condition, or route these through a Java action reusing `GlowHostilesAction.isHostile`.
- **Confidence:** high (pattern), medium on Apoli-side best filter choice.

### M8. Wind Wyrm's Skylord promises Slow Falling that is never granted
- **Location:** `en_us.json` `power.runic_races.wind_wyrm.skylord.description` vs `powers/wind_wyrm/skylord.json` (no slow-falling effect; no Java hook — 0 grep hits; `tools/generate_races.py:803` maps the `slow_fall` slot to a NOFALL damage modifier instead)
- **Problem:** player-facing promise unfulfilled; the race has fall-damage immunity instead, which reads very differently mid-fall.
- **Fix:** correct the lang line (cheapest) or grant conditioned Slow Falling.
- **Confidence:** high.

---

## Moderate Issues

### MO1. `OriginsPowerHelper.isResourceReady` fails open on a present-but-empty resource value
`util/OriginsPowerHelper.java:37`: `return value.isEmpty() || value.getAsInt() <= 0;` — container/holder-missing paths fail closed (`:22,:29`), but an empty `OptionalInt` (misconfigured value provider) reads as "ready." Gates Nine Lives and all four flight cooldowns. Tighten to `value.isPresent() && value.getAsInt() <= 0`.

### MO2. `GlowHostilesAction` lacks the server-side guard all sibling actions have
`action/GlowHostilesAction.java:52` runs `getEntitiesOfClass` + `addEffect` without the `level() instanceof ServerLevel` check present in `TremorPingAction`, `ConeBreathAction`, `SummonMinionAction`, `PlaceTrapAction`. Low practical risk (Apoli fires entity actions server-side) but a client-ghost-effect hazard if ever invoked from a ticking condition context. Normalize.

### MO3. `hostile_biome_tag == home_biome_tag` pattern lights the "hostile biome" HUD rune in home biomes
Five powers set the hostile tag equal to the home tag with 0.0 penalties: `sky_one/sure_footed_sentinel` and `terra_drake/stonescale_hide` (`forge:is_mountain` — **live bug now**), `demon/infernal_blood` (`forge:is_hot` — live now), `canine/pack_hunter` and `dryad/one_with_the_grove` (latent until M1 is fixed). `BiomeAffinityPower.tick` sets `BIOME_HOSTILE` purely from tag membership, so the HUD shows home + hostile runes simultaneously. The codec makes `hostile_biome_tag` optional — omit it.

### MO4. Flap cooldown HUD misrepresents readiness on all four flying races
Flap resources declare `max: 120` (`sprite/gossamer_wings`, `faerie/pixie_flight`, `avian/skyborne`, `wind_wyrm/skylord`) but Java cooldowns are 30/30/35/50 (`flight/FlightConfig.java`). `RacialCooldownOverlay` renders fill as value/max, so the wing icon never exceeds 25–42% "on cooldown." Set each resource max to the actual `FlightConfig` cooldown (the project's own resource-id rule file says max should equal the recharge).

### MO5. Magi's Arcane Overflow has no mana gate
`powers/magi/arcane_overflow.json`'s only firing condition is the cooldown; `ConsumeManaAction.execute` (`ConsumeManaAction.java:32-33`) calls `ManaHelper.consumePlayerMana` without a has-enough check, relying on Iron's clamping semantics. The `runic_races:has_mana` condition exists and is registered — wire it into the active's condition.

### MO6. Nine Lives wastes its 10-minute charge on unavoidable deaths
`RacialEventHandler.java:116-134` triggers on any lethal `LivingDamageEvent`, including void damage and `/kill` — Resistance V doesn't stop the void, so the charge burns and the player dies ~1s later. Exclude sources that `is(DamageTypeTags.BYPASSES_INVULNERABILITY)` / void.

### MO7. Invisibility actives telegraph the user via companion-buff particles
`wraith/spectral_phase` (Resistance/Slow Falling/Speed with `show_particles: true` while Invisibility is particle-less), same for `kitsune/foxfire_illusion` (Speed II) and `serpen/shed_skin` (Speed I). Potion swirls broadcast the "invisible" player's position for the full duration. Set `show_particles: false` on all effects bundled with invisibility.

### MO8. VFX tier violations (Java-side presentation)
- `SignatureRegistry` DEMON_WRATH = 75 particles @ `Intensity.MYTHIC` and WIND_WYRM_BREATH = 75 @ MYTHIC — both are routine 45–50s actives (guideline: Major 30–60, Mythic reserved for rare life-saving moments and 80+). FAERIE_GLAMOUR = 75 @ MYTHIC fits neither band.
- Every dragon breath emits **~170–220 particles per cast**: `ConeBreathAction`'s cone fill (range×3 steps × 6/step = 108–144) *plus* a 55–75-particle signature burst — 3–4× the Major budget, and effectively two VFX paths for one ability.
- Correctly-tiered reference points: NINE_LIVES = 90 @ MYTHIC ✓, REAPER_REVIVAL = 85 @ MYTHIC ✓.
Trim the cone density (2–3/step), re-tier the three 75s to ≤60 MAJOR.

### MO9. Balance outliers (specific, per-race)
- **Demon (impact 3):** permanent fire+lava immunity (Nether trivialized), up to ~+33% melee (base +15% × night +10% × hot-biome +8%), vs the family's mildest weakness column (+25% magic taken — rare in vanilla — water submersion, −25% healing) and *no* sun weakness. The family's balance lever if one is needed.
- **Sprite (impact 1):** see M2 — currently the faeborne family's best race at the lowest advertised impact.
- **Serpen:** "+25% freeze damage taken" is nearly zero exposure in vanilla; its real cost is only −1.5♥ against a 30s full-cleanse + invisibility active.
- **Canine (impact 1):** +12% speed/+10% melee/night vision vs only +25% exhaustion and −2 armor — the family's lightest tradeoff, and stronger than feline's passive at lower impact.
- **Changeling:** weakness luck (−0.5) is over-cancelled by its own passive (+1.0); net weakness ≈ −1♥.
- **Primian (impact 1):** two entirely undocumented Java perks (Universal Palate; Adaptation kill-stacks) + registry luck stacking = quietly over-delivered; also `onAnyLivingDeath` (`RacialEventHandler.java:144-156`) bumps on *any* kill though the javadoc says "distinct mob type" — a zombie farm maxes stacks.
- **Avian/Wind Wyrm flight vs progression:** permanent no-item flight from minute one bypasses elytra progression entirely; taxed (−2♥/−10% melee; −2♥/indoor penalties/−2 luck) but the tax never touches the flight axis. Acknowledged design choice — flag for pack authors.

### MO10. Dark Elf day-malus is broader than advertised and location-blind
`ScalingAttributePower` uses `level().isDay()` (`:72`), so the −5% attack applies all day even deep underground — for a cave-themed race — while `en_us.json` only advertises the night bonus. In fixed-time dimensions (`isDay()` false in the Nether) the +10% night bonus is permanent. Document or condition on sky exposure.

### MO11. HUD state-flag heuristics diverge from the JSON penalty conditions
`RacialEventHandler.java:408-412` sets TIGHT_SPACE for sky_one/wind_wyrm only when `!canSeeSky && y < seaLevel`, but their JSON penalties apply under *any* roof at any height (`!exposed_to_sky`) — the warning rune stays dark inside a surface house while the debuff is active. Align the flag computation with the JSON condition.

### MO12. Build portability: machine-specific `org.gradle.java.home` committed
`gradle.properties` pins `/usr/lib/jvm/java-17-openjdk`. Remove it (toolchains already request Java 17) or move it to an untracked `gradle.properties` override / `~/.gradle/gradle.properties`.

### MO13. Docs advertise mechanics that don't exist ("venom", "lifesteal", "drain")
- Arachnid + Serpen: README/BALANCE credit "venom" — no venom-on-hit exists in any JSON (poison *immunity* only).
- Blood Elf: README:23 / BALANCE.md:27 say "lifesteal" — none exists in JSON or Java.
- Wraith: BALANCE.md:64 and the lang description promise "drain the life from nearby foes" — the AoE Wither is pure damage; reaper got the heal-on-harvest, wraith got the flavor text.
Either implement (a small Java `LivingHurtEvent` heal for wraith/blood_elf would be on-theme) or fix the three docs.

---

## Minor Issues and Polish

1. **Stale assets:** `assets/runic_races/models/origin/` holds 24 pre-overhaul models (`catfolk`, `vampire`, `goblin`, `kobold`, …) referenced by nothing; `textures/entity/dragonborn_wings.png` is referenced by no `WingType`. Delete.
2. **Stale naming/docs in Java:** `VAMPIRE_SUN_TICKS` constant for the zombie race (`RacialEventHandler.java:72`); "Revenant" naming throughout the reaper hooks; `RaceHelper` javadocs citing `runic_races:vampire`; `GraveServantEntity` javadoc citing nonexistent `grave_call.json`; `PlaceTrapAction` javadoc citing "Kobold improvised_trap.json"; `onItemCrafted` javadoc says "Deep Dwarves get a weaker version" but the 15% tier goes to `runic_one` (`:256-258`).
3. **Dead code:** `RacialEventHandler.isFireDamage` (`:513`) never called; `PlaceTrapAction.java:57-61` first occupancy check subsumed by the next line; `WingType.cosmeticOnly` is false for all five types (dead flag, also read by `BodyBankingHandler:40`).
4. **Redundant double main-thread scheduling:** the three S2C packets register with `.consumerMainThread(...)` *and* call `ctx.enqueueWork` inside `handle` — harmless, one layer can go.
5. **Lang/value mismatches:** primian Stroke of Fortune says "Luck II for 8s" (actual 200t = 10s); blood_elf Blood Frenzy says "Regeneration for 8s" (actual 100t = 5s); skeleton origin flavor "unerring archers" has no ranged bonus (+10% attack *speed*); BALANCE.md: volt_drake luck listed as −1.0 (actual −0.5, `RaceRegistry.java:68`), faerie scale listed 0.45 (actual 0.50; `PehkuiIntegration.java:27` comment too), frost_one +1 armor omitted, ice_elf "+10% magic/**ranged**" (no ranged bonus exists), "dry-weak" for nymph overstates a hot-*biome* speed penalty.
6. **msgId coverage gaps in weaknesses:** high_elf/kitsune "physical" lists (player/mob/arrow) miss `trident`/`thrown`/`sting`/`mob_projectile`; ice_elf and arachnid fire lists miss `fireball`. Narrower than the docs' "physical"/"fire" wording.
7. **VFX slight band misses (JSON):** pounce 19, mirror_shift 28, wind_burst 28, shed_skin 26, shadowmeld 26, mountain_leap 25 (all < Major 30); RUNIC_WARD 65 (> 60); tremorsense splits 16 JSON + 24 Java particles across two paths.
8. **Arachnid vibration-sense flicker:** glow duration 80t < reapply interval 100t → hostiles visibly un-glow 1s in every 5.
9. **Forge Blessing edge:** `ItemStack.enchant` appends — an already-Unbreaking-I item crafted by a forge_one rolling level 2 gets a duplicate Unbreaking entry (`RacialEventHandler.java:273-275`). Use `EnchantmentHelper.setEnchantments` semantics or skip when `existing > 0`.
10. **Example datapack broken reference:** `examples/datapacks/runic_races_extra_races/.../race.json` gates on `runic_races:family_fae` — the real ID is `family_faeborne`. As shipped, the example wouldn't inject. (The `examplemod:angel` origin is intentionally fictional.)
11. **No LICENSE file** despite "All Rights Reserved" in mods.toml/README; `Dependencies/Pehkui-*.jar` present but unused (Gradle pulls Pehkui from Modrinth); `origins` dependency version range in mods.toml is `[0,)` — consider `[1.10,)` to fail fast on old Origins.
12. **Serpen's shed-skin sound is `entity.spider.ambient`** — a spider hiss on the snake race (identity blur with arachnid).
13. **Zombie `sunlight_decay.json`** conditions on `exposed_to_sun` AND `daytime` (redundant); the Java `sunTicks` accumulator only gates two audio cues — "escalation" comment overpromises.

---

## Race-by-Race Review

Format: **status / notable traits / bugs / balance / assets**. All 37 races are selectable, discoverable (two-layer family→race flow), persistent (Origins-owned), and have complete icons + lang. "✓ clean" = no functional bugs found.

### Human family
- **Primian** (impact 1): complete. +1♥/+1 luck/+5% spd passive; Luck/Absorption/Speed active (100s); −10% magic dealt/−5% melee weakness; Java adaptation stacks + Universal Palate. Bugs: kill-bump not distinct-mob (doc); Luck duration lang (10s vs 8s); two undocumented Java perks. Balance: quietly over-delivered for impact 1.
- **Celeron** (impact 1): complete. +12% spd/+10% atk-spd; dash active (25s); −2♥ + dead KB weakness (M2). Balance: fine once KB is real.
- **Magi** (impact 2): complete. +15% magic dealt; mana-costed nova (45s); −2♥/+20% physical taken. Bugs: no mana gate (MO5); nova hits allies (M7). Balance: intended caster, within budget.
- **Valen** (impact 2): complete. +2♥/+2 armor/+0.4 KB-res/+10% melee; Resistance III+Absorption II stand (60s); −10% spd/−10% atk-spd. Bugs: **M3 shoulder-check**. Balance: over budget until M3 fixed.

### Elven family (all impact 2, Curios necklace, Ars family mana bonus)
- **High Elf**: ✓ clean. Best caster ceiling (+15% magic, ×1.10 Iron's, ×1.20 Ars mana); −2♥/+15% physical. Minor: trident msgId gap; AoE Weakness hits allies.
- **Dark Elf**: complete. Night kit + Shadowmeld invis (45s); sun weakness. Bugs: MO10 day-malus; 26-particle VFX undercount.
- **Moon Elf**: **partial — M5 dead night_regen**. Otherwise good invis/heal active. Under-budget until fixed.
- **Blood Elf**: complete. +10%/+10% passive, best-uptime active (35s Str II). Weakness (−1.5♥, −30% healing) **verified working** (modify_healing exists). Minor: regen lang 5s vs 8s; "lifesteal" docs vaporware.
- **Ice Elf**: ✓ clean. Freeze-damage immunity, cold affinity (tags valid); +25% fire-family taken. Minor: fireball msgId gap; Frostbind hits allies.

### Dwarven family (all belt Curios, 0.68–0.74 scale)
- **Deep One** (impact 1): complete. Tremorsense recon + dark-dweller passive + sun penalties. Bugs: **M6 cooldown burn on no-op**. Minor: "Sunlight Blindness" applies no blindness.
- **Forge One** (impact 2): complete. Fire-resist smith + crafting proc (25% Unbreaking, 10-min CD — persistence verified OK). Minor: enchant-append edge; proc undocumented.
- **Frost One** (impact 2): ✓ clean. Freeze-damage immunity + cold affinity; +25% fire taken. Minor: "freeze immune" is damage-only (powder-snow slow still applies).
- **Iron One** (impact 2): ✓ clean — the family's reference implementation. Fat defensive block, taxed by magic penalties.
- **Sky One** (impact 2): complete. Leap + fall immunity + mountain affinity; underground penalties. Bugs: MO3 hostile==home tag; MO11 rune mismatch; 25-particle active.
- **Runic One** (impact 3): complete. Richest kit + steepest weakness. Bugs: **M7 ward slows the party**; undocumented craft proc.

### Bestial family
- **Arachnid** (impact 2): **C2 — trap never triggers**; glow-pulse flicker; 10s trap vs 35s CD; "venom" docs vaporware. Currently far under power.
- **Avian** (impact 2): complete. Flight verified end-to-end (resource IDs exact). Bugs: M2 dead KB weakness; MO4 flap HUD; 28-particle burst. Balance: free flight at impact 2 — see MO9.
- **Canine** (impact 1): **M1 dead forest affinity**; MO3 latent. Lightest weakness in family.
- **Feline** (impact 2): ✓ clean — Nine Lives (JSON+Java hybrid) is reference-quality; 90-particle MYTHIC correct. Minor: MO6 void waste; pounce 19 particles; javadoc "health to 1" vs 2.0f.
- **Kitsune** (impact 3): complete. Strongest bestial kit, correctly impact 3. Minor: MO7 invis particle leak; physical msgId gaps; AoE blind hits allies.
- **Serpen** (impact 2): complete. Bugs: MO7 leak. Balance: near-free weakness (MO9). Minor: spider sound.

### Faeborne family (Curios ring)
- **Changeling** (impact 2): ✓ clean. Balance: self-cancelling luck weakness (MO9). 28-particle active.
- **Dryad** (impact 2): **M1 dead grove affinity** + MO3 latent; 3× fire weakness real and brutal (correct). Otherwise solid.
- **Sprite** (impact 1): complete; flight verified. **M2 dead KB weakness + impact misrating** — family's best race at lowest rating.
- **Nymph** (impact 2): ✓ clean — tags valid, 54-particle Major band, proportionate weakness. Minor: "dry-weak" doc overstatement.
- **Faerie** (impact 3): complete; flight verified. Bugs: M2 dead KB component; FAERIE_GLAMOUR 75-particle tier misfit. Real glass-cannon tradeoff otherwise — impact 3 honest.

### Undead family (Curios charm, night theme)
- **Zombie** (impact 1): ✓ clean. Sun DoT + −25% healing verified working. Minor: naming debt (#2 above).
- **Skeleton** (impact 1): **C1 — hostile summons**. After the one-line fix: coherent budget.
- **Wraith** (impact 2): complete. Bugs: MO7 invis leak; "drain" docs vaporware. AoE Wither hits pets (M7 cluster).
- **Demon** (impact 3): complete. Bugs: MO8 mythic mislabel. Balance: **the roster's most generous kit** (MO9).
- **Reaper** (impact 3): complete. **M4 revival fail-path + "10 minutes" lang lie**. Note: −50% modify_healing self-applies to Soul Harvest's own heal (4.0 lands as ~2.0) — keeps impact 3 honest but should be a conscious decision. REAPER_REVIVAL VFX (85 @ MYTHIC) is the guideline's model citizen.

### Draconic family (breath grammar shared via `ConeBreathAction`)
- **Fire Drake** (impact 2): ✓ clean. Exact mirror symmetry with ice_drake. 7.0 dmg + ignite, 40s.
- **Ice Drake** (impact 2): ✓ clean. 6.0 dmg + frozen-ticks + Slowness III rider.
- **Sea Serpen** (impact 2): ✓ clean. Water-breathing verified against decompiled Origins-Forge mixin — **no land-drowning on this port** (weakness is speed-only when dry, matching BALANCE.md). Minor: lang says "slow and weak" but no attack penalty exists.
- **Terra Drake** (impact 2): complete. Bugs: MO3 hostile==home (live); M6 tremor_ping no-op inside breath. Tankiest passive (+4 armor, KB-res 1.0, fall immunity) vs real speed taxes.
- **Volt Drake** (impact 2): ✓ clean. Longest/narrowest/hardest breath (8.0 dmg, 18°); channeling weakness (indoor −10% atk, wet +25% taken) works and is correctly notified. Minor: BALANCE luck figure.
- **Wind Wyrm** (impact 3): complete; flight verified exact. Bugs: **M8 Slow Falling lang lie**; MO4 flap HUD; MO11 rune mismatch; 75 @ MYTHIC breath tier. Heaviest taxes in the roster (−2♥, indoor penalties, −2 luck) — impact 3 honest.

---

## Race Family Consistency Review

- **Human** — coherent "baseline" quadrant (no immunities, no Curios, ~1.0 scale, single-axis specialists with opposing-axis weaknesses). Budget wobbles: primian over (undocumented perks), valen over (M3), celeron under (M2). Fix the three code issues and it aligns tightly.
- **Elven** — strong identity (night vision ×3, +magic dealt ×4, necklace, 1.04–1.08 scale, 700–1000t CDs). blood_elf runs slightly hot / moon_elf cold, entirely due to M5 (blood_elf's weakness verified working).
- **Dwarven** — the tightest family: armor-stacked, slow, belt curio, 0.68–0.74 scale, defensive actives with CDs scaling by payload. Deliberate impact-1 lightweight (deep_one) and impact-3 heavyweight (runic_one) both correctly costed.
- **Bestial** — "senses + mobility vs fragility" holds, but two of six actives are broken (C2, M1) and weakness weight is uneven (kitsune/feline/avian pay real costs; serpen/canine nearly free).
- **Faeborne** — structurally the cleanest (all assets/IDs perfect) but *enforcement*-broken: both "weightless" weaknesses are dead clamps and the dryad's signature tag is dead, so the family plays tankier and blander than documented; sprite's impact rating inverts the family's ranking.
- **Undead** — most thematically tight: everyone pays in health or healing, four of five carry an escalating-form sun penalty. Breakers: C1 (skeleton) and demon (no sun weakness + lightest cost at impact 3).
- **Draconic** — strong: uniform breath grammar, per-element riders in one shared action, clean fire↔ice symmetry, differentiated niches. Only moderate issues (M6/M8/MO3/MO8).

---

## Client/Server Boundary Review

**No violations found.** Specifics:
- Common → client crossings occur *only* through `DistExecutor.unsafeRunWhenOn(Dist.CLIENT, () -> () -> ClientHandler.receive(...))` with the client code in nested holder classes (all three S2C packets) — never classloaded on dedicated servers.
- All client classes carry `@OnlyIn(Dist.CLIENT)` or `@Mod.EventBusSubscriber(value = Dist.CLIENT)` with correct bus choices (`ClientEvents` MOD bus for registration events; input/render handlers on FORGE bus).
- `FlightConfig` correctly lives in common (`flight/`), used server-side; only the input handler is client-only.
- Server logic never trusts the client: flap/cancel packets carry no payload and are fully re-validated server-side (race, config, airborne state, 2-tick rate limit via `WeakHashMap`, Origins cooldown resource).
- Custom Apoli actions run server-side; the one missing belt-and-suspenders guard is MO2.
- `RaceStateTracker` is server-only state keyed by UUID, cleared on logout, resynced on login.

---

## Rendering and Wings Review

- **Registration:** correct — layer definition via `RegisterLayerDefinitions`, wing layer added to both player skins via `AddLayers`, all on MOD bus / Dist.CLIENT (`ClientEvents.java:46-70`).
- **Coverage:** 8 winged races map to 4 live `WingType`s (sprite/faerie → pixie, avian → wyvern-tex at 0.95, wind_wyrm → wyvern, fire/ice/terra/volt drakes → drake). All three referenced textures exist (`textures/entity/pixie_wings.png`, `wyvern_wings.png`, `drake_wings.png`). sea_serpen intentionally wingless. `dragonborn_wings.png` is orphaned.
- **Animation:** four-state machine (flap/hover/glide/ground) with per-tick smoothing, banking from yaw delta, speed-responsive glide oscillation, wind buffeting via seeded sine noise, breathing idle, crouch fold — implemented per-player in a `WeakHashMap` (GC-safe). Flap detection keys off upward velocity spikes, so grounded drakes get a wing snap when jumping — arguably charming, technically "animating while not flying" for cosmetic-wing races.
- **Compatibility:** elytra equipped → wings skipped (`:65`) ✓; invisible player → skipped ✓; armor: wings are drawn 0.125 behind the body with no chestplate offset — expect mild clipping with bulky armor models (cosmetic, medium confidence).
- **Multiplayer visibility:** wings render for other players iff Origins syncs their origin to the observing client (it does, via its capability sync) — `RaceHelper.getRaceName` is side-agnostic. Race changes propagate within 100 ticks (`RACE_CACHE_TICKS`) — a ~5s stale-wings window after a race change, acceptable.
- **First person:** `RenderLayer`s don't render in first person — correct by construction.
- **Dedicated server:** all wing classes `@OnlyIn(Dist.CLIENT)`, referenced only from `ClientEvents` — safe.
- **Dead flag:** `WingType.cosmeticOnly` is false for all five constants; drake wings therefore run flap/hover/glide detection they can never satisfy (isFallFlying false without elytra). Harmless, but either set drakes cosmetic-only or delete the flag.

---

## Race Data, Config, and Reload Review

- **Schema/validation:** all 111 powers use Origins/Apoli's data-driven schema and parse (0 malformed). Custom codecs use sensible defaults; the two gaps are C3 (`check_interval` unvalidated) and `ScalingAttributePower.resolveAttribute`'s graceful unknown-attribute warn (good pattern — replicate for C3 and for `BiomeAffinityPower.resolveTag` unbound tags).
- **Malformed data handling:** Origins logs-and-skips bad power JSON (fail-soft); the mod's own registry (`RaceRegistry`) is hardcoded Java, so it cannot desync from a *removed* origin JSON — but nothing warns if a `RaceRegistry` race has no matching origin file (add a common-setup cross-check; the count log at startup is a start).
- **Reload behavior:** `/reload` re-reads origins/powers (Origins-owned). Mod-side caches survive correctly: `RaceHelper`'s memo is per-tick by design (no stale window), `IntegrationManager.SyncHandler` polls race identity every 20t and resyncs. `AbilityIconRegistry` is static-final Java — a datapack that *changes* cooldown resource IDs won't move the HUD (documented limitation of the hybrid approach; acceptable). The builtin clean-two-layer pack is world-load-time only, and README correctly says `/reload` won't pick it up.
- **Config:** three specs, reasonable comments matching behavior. `OriginLayerPacks` reads common config during `AddPackFindersEvent` — works because server-data pack discovery happens after config load, but the class comment's justification ("loads at mod construction") is inaccurate; worth a defensive `spec.isLoaded()` guard.
- **Datapack extension:** supported and exampled, but the example is broken (Minor #10) and there is no validation command for pack authors (see Commands).

---

## Multiplayer and Persistence Review

- **Selection/persistence/sync of the race itself** is delegated to Origins (capability-synced, persisted) — correct choice; the mod never stores a shadow copy that could desync (`LAST_SYNCED_RACE` is a sync-trigger cache, not authority, and is deliberately excluded from non-death clones).
- **Login:** `RaceStateTracker.onLogin` resyncs HUD flags; `IntegrationManager.SyncHandler.onPlayerLogin` reapplies integration state (Pehkui scale, Curios slots, luck). ✓
- **Respawn/dimension change:** SyncHandler covers both; state flags recompute within 10 ticks. ✓
- **Death/clone:** `onPlayerClone` copies all `runic_races:*` NBT (cooldowns survive death — intentional anti-exploit), skipping ephemeral keys on end-portal clones. ✓
- **Race change:** 20-tick poll detects origin change and resyncs integrations; wing visuals lag ≤100 ticks; attribute modifiers from Apoli powers are removed by Apoli on power loss; custom powers clean up in `onRemoved`. ✓
- **Tracking:** no custom per-tracking sync needed (S2C packets are owner-only HUD state; wings derive from Origins' own sync). ✓
- **Packet authority:** all four ability-relevant inputs (2 packets + Origins' own active-key path) are server-validated. `NetworkHandler` uses exact-version predicates — the mod is required on both sides (consistent with mods.toml side BOTH; vanilla clients cannot join, which is correct for an Origins add-on but worth stating in README).
- **FakePlayers:** `RacialEventHandler` paths gate on `instanceof ServerPlayer`, which FakePlayer passes — a FakePlayer with an origin could theoretically proc racial hooks, but FakePlayers don't select origins, so `RaceHelper` returns null and every hook exits. Effectively safe.
- **Leaks:** `RACE_MEMO` and `FLAGS` cleared on logout; `lastFlapTick` is a `WeakHashMap`. No unbounded growth found.

---

## Ability and Attribute Review

- **Attribute hygiene:** all Java-applied modifiers are transient, fixed-UUID, diffed-before-reapply, and removed on zero/`onRemoved` (`BiomeAffinityPower`, `ScalingAttributePower`, `applyAdaptationModifier`, `ApotheosisIntegration`). No stacking-on-relog or UUID-collision issues found (UUIDs are distinct across the four subsystems).
- **Cooldowns:** the resource pattern (timer max == recharge, decay interval 1, `== 0` gate) is followed by all 42 resources; server re-checks on flight; Nine Lives sets its resource from Java and the JSON decays it. The only mismatches are display-layer (MO4) and consume-on-no-op (M6).
- **Effect flicker:** action_over_time reapply intervals ≥ effect durations everywhere except arachnid's glow pulse (Minor #8).
- **Weaknesses that don't apply:** M1 (forest), M2 (knockback ×4), M5 (moon_elf) — plus narrow msgId lists (Minor #6). Everything else verified live, including all `modify_healing` uses.
- **Immunities:** implemented as ×0 damage multipliers (fire drake, frost/ice freeze, wraith/terra fall) or `effect_immunity` (poison/hunger/wither/mining-fatigue) — all resolve to real Apoli types. Note "freeze immune" races still get powder-snow slowdown (damage-only immunity).
- **Creative/spectator:** overlays skip spectators; trap skips creative players; flight/racial hooks don't special-case creative (racial passives applying in creative is standard Origins behavior).
- **Resource gating (mana/stamina):** fail-closed-by-default posture with clear logs is well designed; the gap is MO5 (magi's active not condition-gated on mana).

---

## Compatibility Review

| Integration | Mechanism | Safe? | Notes |
|---|---|---|---|
| Origins/Apoli/Calio (required) | Direct API | ✅ | Capability reads wrapped in try/catch; per-tick memo; no shadow state. |
| Curios | Reflection-loaded class, direct imports inside | ✅ | Removes all known slot grants then reapplies per race — no slot leaks on race change. |
| Iron's Spellbooks | Reflection-loaded; also reflection-based `ManaHelper` | ✅ | Race multipliers via switch; fail-closed gating configurable. |
| Ars Nouveau | Reflection-loaded event subscriber | ✅ | Mana/cost multipliers, try/catch-wrapped. |
| Apotheosis | Reflection-loaded | ✅ | Luck attribute by fixed UUID, remove-then-add. |
| Pehkui | Reflection-loaded | ✅ | Custom `race_scale` ScaleType, epsilon-guarded, growth relocation + resize contact-damage suppression window. Jump compensation gated on `PEHKUI_LOADED`. |
| Feathers | Fully reflection-based (`StaminaHelper`) | ✅ | One-shot resolution, fail-open/closed configurable. |
| Epic Fight / PlayerEx / seasons / damage-type mods | none | n/a | No implicit coupling found; damage modifiers use vanilla msgIds so modded damage types simply bypass race resistances *and* weaknesses symmetrically. |

Cross-mod stacking watch: Magi/High Elf JSON +15% magic × Iron's ×1.10–1.15 is intended; verify no Iron's spell resolves to vanilla `indirectMagic` (would double-dip to ~1.32×) — low probability, runtime check recommended.

---

## Performance Review

- **Server tick:** `RacialEventHandler.onPlayerTick` gates on `%10` + `ServerPlayer` and does only biome/sky/NBT reads; `SyncHandler.onPlayerTick` gates on `%20` with a per-tick-memoized race lookup. `RaceHelper.RACE_MEMO` dedupes the Origins capability walk to ~1/player/tick. All fine at scale.
- **Custom powers:** biome/scaling ticks every 40t per configured power; tag lookups cached (`TAG_CACHE`). Fine.
- **Client:** `RacialCooldownOverlay` refreshes resources every ~4 ticks with texture-existence caching; `ClientRacialAmbienceHandler`'s 24-block entity scan is movement/staleness-cached and config-gated; wing anim state is per-tick-gated inside the render call. Fine.
- **Packets:** all HUD sync is edge-triggered (diffed) — S2C volume is minimal. Flap packets rate-limited server-side.
- **Particles:** the dragon-breath 170–220-particle bursts (MO8) are the only VFX volume worth trimming, more for grammar than for frame time.
- **Memory:** per-player maps cleaned on logout; `WeakHashMap`s for entity-keyed state. No retention issues found.
- Singleplayer nit: `RaceHelper`'s memo `client` flag causes client/server queries in the same tick to evict each other — correctness preserved, negligible cost. Could key by `(UUID, side)` pair instead.

---

## Documentation and Resource Consistency Review

Verified consistent: 37 races / 7 families / 111 powers / 44 origins counts everywhere; all 42 cooldown resource IDs; all 42 HUD ability icons; all 37 item icons + models; 454 lang keys with 0 missing (matches passing `I18nKeyPresenceTest`); README command list matches `RRCommands`; README config section matches the three specs; README's `/reload`-won't-move-the-builtin-pack caveat is accurate; BALANCE.md draconic table matches JSON line-for-line.

Mismatches (consolidated from above): "venom" ×2, "lifesteal", wraith "drain", skylord "Slow Falling", reaper "10 minutes", primian Luck duration, blood_elf Regen duration, sprite "2× knockback", canine/dryad forest bonuses (dead), BALANCE volt-drake luck / faerie scale / frost armor / ice_elf "ranged" / nymph "dry-weak", "Sunlight Blindness" (no blindness), example datapack `family_fae`, undocumented mechanics (Universal Palate, adaptation kill-stacks, valen shove/thud, runic_one craft proc, faerie night vision, avian night vision).

---

## Recommended Fix Plan

1. **Immediate crash/build fixes:** C3 `check_interval` clamp (both power classes); remove `org.gradle.java.home` from `gradle.properties` (MO12).
2. **Dedicated server / boundary fixes:** MO2 `ServerLevel` guard in `GlowHostilesAction` (only boundary gap found).
3. **Race persistence and synchronization fixes:** none required — verified sound. (Optional: align MO11 HUD flags with JSON conditions.)
4. **Attribute/effect cleanup fixes:** M2 knockback via `LivingKnockBackEvent` + remove dead modifiers; M5 moon_elf night_regen; Minor #8 glow flicker; Minor #9 enchant-append.
5. **Ability and packet validation fixes:** C1 grave_servant JSON swap (+ lang key); C2 trap `entityInside`; M3 valen gating; M4 reaper fail-path refund; M6 tremor gate-vs-cooldown; MO5 mana gate; MO1 `isResourceReady` tighten; MO6 Nine Lives void exclusion.
6. **Rendering/model/texture/wing fixes:** delete 24 stale origin models + `dragonborn_wings.png`; decide `cosmeticOnly` for drakes; MO4 flap resource max = FlightConfig cooldown.
7. **Data validation and reload fixes:** M1 forest tag; MO3 remove five hostile==home tags; warn-once on unbound biome tags; startup cross-check RaceRegistry ↔ origins files; fix example datapack `family_faeborne`.
8. **Balance and family consistency:** MO9 items — demon weakness column (add a sun/holy tooth or trim night-stacking), sprite impact→2, serpen/canine weakness weight, changeling luck, primian doc-or-trim; M7 runic_one ward hostile filter (then the other AoEs).
9. **Compatibility fixes:** none blocking; runtime-verify Iron's/vanilla `indirectMagic` non-overlap.
10. **Documentation and diagnostics:** all Minor #5/#6 lang fixes + MO13; add `/runicraces validate` (cross-check registry↔origins↔powers↔icons↔lang) and `/runicraces state <player>` (dump flags, cooldown resources, adaptation stacks, integration sync status); add a LICENSE file.

## Suggested Regression Tests

**JUnit (extend the existing suite):**
1. `RaceRegistryOriginParityTest` — every `RaceRegistry` race has `origins/<race>.json`, an icon model/texture, and 3 power files; every origin's powers list resolves to files (would have caught C1's dead entity had it covered entity IDs too).
2. `CooldownResourceIdTest` — every `*_cooldown_timer` string in Java (FlightConfig, AbilityIconRegistry, RacialEventHandler) exists in exactly one power JSON, and resource `max` == the Java cooldown where Java owns it (catches MO4).
3. `PowerJsonLintTest` — parse all 111 files: `check_interval >= 1`; referenced entity IDs are `runic_races:` or vanilla-and-intended (allowlist); biome tags in an allowlist of known-existing tags (catches M1); `apply_effect` duration ≥ reapply interval (catches Minor #8); AoE powers carry a bientity filter beyond `living` (tracks M7).
4. `VfxBudgetTest` — sum JSON particle counts per active and assert tier bands; assert SignatureRegistry intensities match particle budgets (catches MO8).
5. `LangConsistencyTest` — extend `I18nKeyPresenceTest` to scan lang descriptions for durations ("for Ns") and compare against JSON ticks (catches Minor #5).

**Manual in-game (per family, ~30 min):**
6. Skeleton: summon conscripts, verify they attack a zombie and never the caster, expire at 60s.
7. Arachnid: place trap, walk a cow across it, verify trigger + owner immunity.
8. Each flyer: flap cadence, HUD wipe reaches 100%, dedicated-server flap while a second client watches wings.
9. Reaper: die over lava/void (fail path) → verify cooldown refund and rejection cue; die normally → revival + counter reset; relog between death and respawn.
10. Feline: lethal hit with absorption up (should not trigger), void death (should not consume after MO6 fix).
11. Valen PvP: sprint-spam melee and sprint-shot arrows at a second player — no shove after M3 fix.
12. `/reload` with a race datapack override; join a dedicated server with each optional mod removed; flip `failClosedWhenResourceModMissing` both ways on a magi.

## Top 10 Fixes to Implement First

1. **C1** — skeleton summon → `runic_races:grave_servant` (one line; a race is actively self-harming).
2. **C2** — trap `entityInside` trigger (signature ability inert).
3. **M1** — `minecraft:is_forest` (two races' documented passives dead).
4. **M2** — real knockback weakness hook (four races' tradeoffs dead; unlocks sprite re-rating).
5. **M3** — valen shoulder-check gating (PvP/mob-juggle exploit).
6. **M4** — reaper revival fail-path refund + "10 minutes" lang (flagship mechanic quietly self-degrading).
7. **C3 + MO1 + MO2** — hardening trio (crash clamp, fail-open read, side guard) — small, same PR.
8. **M5 + M8** — moon_elf night_regen and skylord Slow Falling (tooltip promises).
9. **M6 + M7** — tremor cooldown-on-no-op and runic_one ward ally filter.
10. **MO4 + MO8** — flap HUD max and VFX tier normalization (visible polish).

## Files Most Likely Needing Changes

| File | Why |
|---|---|
| `data/runic_races/powers/skeleton/conscript_the_dead.json` | C1 entity swap |
| `block/TrapMarkerBlock.java` | C2 `entityInside`; trap lifetime |
| `event/RacialEventHandler.java` | M3 valen gating, M4 reaper fail-path, MO6 void exclusion, naming/dead-code cleanup |
| `power/BiomeAffinityPower.java`, `power/ScalingAttributePower.java` | C3 clamp; unbound-tag warn |
| `powers/canine/pack_hunter.json`, `powers/dryad/one_with_the_grove.json` | M1 tag + MO3 hostile-tag removal |
| `powers/{celeron,sprite,faerie,avian}/…` + new `LivingKnockBackEvent` handler | M2 |
| `powers/moon_elf/tidecallers_grace.json` | M5 |
| `action/TremorPingAction.java` + `powers/{deep_one/tremorsense,terra_drake/seismic_breath}.json` | M6 |
| `powers/runic_one/rune_of_warding.json` (then the other 9 AoE powers) | M7 |
| `util/OriginsPowerHelper.java` | MO1 |
| `action/GlowHostilesAction.java` | MO2 |
| `powers/{sky_one,terra_drake,demon}/…` | MO3 |
| `powers/{sprite,faerie,avian,wind_wyrm}/<flight>.json` | MO4 flap max |
| `presentation/SignatureRegistry.java`, `action/ConeBreathAction.java` | MO8 VFX tiers |
| `assets/runic_races/lang/en_us.json` + `tools/race_lang.json` + README/BALANCE | all lang/doc fixes (regenerate via `tools/build_lang.py`) |
| `gradle.properties` | MO12 |
| `assets/runic_races/models/origin/`, `textures/entity/dragonborn_wings.png` | stale asset removal |
| `command/RRCommands.java` | add `validate` / `state` subcommands |
| `examples/datapacks/runic_races_extra_races/` | `family_faeborne` fix |

---

## Verdict

**Beta testing: yes, after the two one-day fixes (C1, C2). Public release: not yet.**

The engineering foundation — networking, side safety, persistence, integration guards, data discipline — is already at public-release quality, and nothing found here threatens server stability or player data. What blocks a public release is content correctness: two races with broken signature abilities, six dead weakness/passive components that the balance model depends on, and a cluster of tooltip promises the code doesn't keep. All of it is cheap to fix (mostly one-line JSON edits and one small knockback event handler), and the fix plan above is ordered so that items 1–6 alone would make every race play as documented. Ship those, run the manual family passes in the regression list, and this is a releasable mod.
