# Runic Races — Server-Side Optimization Implementation Plan

**Repository:** [otectus/runic-races][repo]  
**Audited revision:** [`1877c025265920f2800ac09ba7f194244e9a7e3c`][commit]  
**Audited version:** 1.6.3  
**Audit date:** September 23, 2026  
**Baseline platform:** Java 17, Minecraft 1.20.1, Forge 47.2.0  
**Document status:** Static audit and implementation specification; runtime validation outstanding.

## Instructions for the implementing coding agent

Use this document as a staged implementation specification. Preserve intended gameplay, multiplayer correctness, persistence, and compatibility throughout the work.

- Start by comparing the current checkout with the audited revision. Revalidate findings affected by intervening changes; do not assume every finding remains present.
- Complete Phase 0 before making profiling-dependent architectural changes. Do not claim measurements, test passes, or runtime compatibility without executing the relevant checks.
- Keep confirmed correctness fixes distinct from performance-only changes and measure optimizations independently.
- Preserve the behavioral contract below. Do not use asynchronous world, entity, capability, inventory, or attribute access.
- Keep implementation changes independently reviewable. Update generators and generated data together when both are affected.
- Record baseline/candidate results, regressions, dependency hashes, and unresolved limitations with the implementation.
- Defer a complex performance-only change when evidence does not justify it. Do not substitute gameplay reductions for equivalent optimization.

## 1. Audit basis and priorities

The highest-potential gains are **reducing cooldown synchronization and consolidating particle delivery**. First establish reliable lifecycle handling and state aggregation; otherwise caching and deferred updates could preserve stale race state or break multiplayer behavior.

This plan targets the revision identified above. The audit covered the repository's Java code, power/origin data, configuration, generators, build setup, and tests: 96 main Java files, including 19 client files; 111 power JSON files; and 25 test-source files.

**Verification limit:** this is a static audit. The execution environment was unavailable during the audit, so compilation, tests, dedicated-server operation, profiling, packet captures, and save migrations were not run. Binary assets were inventoried rather than visually inspected. No performance measurements are claimed below.

“Confirmed” means the behavior is visible in the inspected source. Expected performance benefits remain estimates until benchmarked.

| ID | Recommendation | Evidence | Expected benefit | Complexity / risk | Dependencies |
| --- | --- | --- | --- | --- | --- |
| R1 | Aggregate shared race flags before synchronization | Confirmed conflicting writers | Correct HUD state; eliminate artificial updates | Medium / medium | Lifecycle characterization |
| R2 | Establish explicit server, player, and reload lifecycles | Confirmed cleanup/configuration gaps | Correctness prerequisite; bounded state and cache lifetime | Medium–high / high | R1 state ownership |
| R3 | Replace repeated cooldown container snapshots with scoped synchronization | RR mutations confirmed; shipped Apoli behavior needs verification | Potentially high CPU, allocation, and network savings | High / high | Exact dependency inspection, R2, protocol tests |
| R4 | Batch shaped particle emissions | Confirmed repeated send calls | Potentially high in crowded combat | High / medium–high | Recipient/timing characterization, R2 |
| R5 | Precompute immutable power configuration data | Confirmed repeated derivation | Modest steady-state allocation and lookup reduction | Low–medium / medium | Reload invalidation |
| R6 | Resolve effects outside target loops; simplify cone calculations | Confirmed redundant work | Moderate in dense combat; small otherwise | Low–medium / medium | Target-set regression fixtures |
| R7 | Reconcile integration changes instead of removing/reapplying | Confirmed redundant mutations | Lower synchronization bursts and attribute traffic | Medium / medium–high | R2 |
| R8 | Replace trap block-entity polling with scheduled expiry | Confirmed per-tick dispatch | Small normally; substantial with many loaded traps | Medium / medium | Expiry/save/load characterization |
| R9 | Remove scheduler burst-compaction costs | Confirmed algorithmic issue; magnitude unknown | Small normally; potentially material during synchronized bursts | Low initially / medium | Ordering tests, R2 |
| R10 | Optimize minion metadata access only if profiling supports it | Repeated access confirmed; bottleneck unverified | Probably secondary to AI/pathfinding | Medium / medium–high | AI and allocation profiles |
| R11 | Bound denied-input feedback and optional-API failure work | Confirmed redundant/failure-path work | Low normally; useful under repeated failure or input | Low–medium / medium | Resource-gating regressions |

Complexity includes implementation and meaningful verification, not just the size of the code change.

Several previous optimizations should remain intact: cooldown decay already runs every 10 ticks, or 5 for flaps; custom attribute powers already avoid replacing unchanged modifiers; race lookup already has a per-tick memo; flap requests already have a two-tick guard; presentation queues already clear on logout, death, and server stop; and grave servants already enforce expiry. Reimplementing these would not constitute a new gain.

## 2. Behavioral contract

Establish the behavioral contract before changing execution or synchronization. Preserve these invariants throughout all phases:

- The server owns race selection, resource availability, ability activation, damage, effects, movement, summons, traps, and persistence.
- World, entity, capability, attribute, and inventory access stays on the appropriate game thread. No asynchronous entity scans, world reads, damage application, or capability mutation are proposed.
- Preserve resource identifiers, modifier UUID derivation, serialized keys, effect parameters, target selection, damage-event ordering, and authored tick intervals.
- Preserve existing distinctions between cooldown clocks. Resource countdowns, game-time timestamps, and ephemeral presentation delays must not be converted into one universal deadline mechanism.
- Preserve third-party powers and modifiers. Optimizations must operate only on state owned by Runic Races.
- Preserve current visual density and audible cues by default. Reducing particle counts, target counts, scan radius, or AI frequency is a gameplay/presentation tradeoff, not an equivalent optimization.
- Client-facing changes must preserve late tracking, reconnect, respawn, dimension changes, and datapack reload behavior—not merely the initiating player's HUD.

## 3. Baseline profiling and reproducibility

The build targets Java 17, Minecraft 1.20.1, and Forge 47.2.0. Dependency reproducibility needs attention: [build.gradle][build] uses floating ForgeGradle `6.0.+`, while [ci/fetch-deps.sh][deps] downloads Iron's Spellbooks 3.15.5.1 under a filename naming 3.15.4.

Before measuring:

1. Record the resolved Gradle/plugin versions and SHA-256 hashes of every server/client JAR, including nested Apoli and Calio dependencies. Identify actual versions from metadata, not filenames.
2. Establish two mod configurations: required dependencies only, and the intended full modpack with all six optional integrations.
3. Record CPU model, available cores, OS, exact JDK build, heap size, collector, JVM flags, view/simulation distances, compression threshold, and server configuration.
4. Freeze a benchmark world and fixture manifest: seed, loaded chunks, player positions, races, equipment, health, resources, entity populations, weather, time, and scripted actions.
5. Build and launch the packaged, reobfuscated JAR. The existing [CI workflow][ci] runs `compileJava test`; it does not establish production-JAR or multiplayer correctness.

Use three comparisons:

- Required dependencies without Runic Races, on a separate world copy, to identify background engine cost.
- The pinned Runic Races baseline.
- One optimization candidate at a time against that baseline.

The first comparison is attribution support, not a gameplay-equivalent performance comparison.

Collect unprofiled timing runs separately from CPU/allocation investigations. [Spark's command documentation][spark] describes execution profiles, allocation profiles, slow-tick filtering, and local profile export. Use those alongside JFR/GC information rather than inferring time saved from flamegraph percentages alone.

Add benchmark-only, aggregated counters around:

| Area | Measurements |
| --- | --- |
| Tick/event processing | Invocations, elapsed time, race, early exits, successful/denied activations |
| Power processing | Custom power evaluations, decay actions, resource mutations, full-container synchronizations |
| Attributes/integrations | Lookup counts, modifier additions/removals, unchanged reconciliations, integration sync reasons |
| Entity queries | Queries, returned candidates, accepted targets, applied effects, damage attempts |
| Presentation | Emission calls, packets, recipients, encoded bytes, delayed beats, cancellations |
| Traps/minions | Loaded count, ticker calls, scheduled expiry calls, active minions, expiry removals |
| Memory | Allocated bytes, post-GC live set, collection pauses, queue/map sizes, retained player/world references |
| Networking | RR packets, RR-triggered Apoli packets, vanilla particles/effects/attributes/sounds, outbound backlog |

Counters should use fixed categories and aggregate periodically. Avoid per-event logs, per-player metric labels, or an allocation-heavy timer around every target.

Measure both encoded application bytes and actual connection traffic with fixed compression settings. Counting only the Runic Races channel would miss its potentially larger Apoli and vanilla traffic.

## 4. Reproducible benchmark suite

All client-count tests must use connections that complete Forge/Origins initialization and hold real origin/power state. Fake players alone cannot establish tracking, serialization, or client correctness.

Use a five-minute warmup and ten-minute measurement window, with at least five paired baseline/candidate runs in alternating order. Restore the same starting world/player state for each pair. Record actual completed actions so reduced work caused by failed abilities cannot appear as a speedup.

| Scenario | Reproducible workload | Purpose |
| --- | --- | --- |
| Empty server | Zero players; fixed loaded spawn area; no RR-created entities | Detect fixed overhead |
| Idle players | 1, 8, and 32 players; full health/resources; ready cooldowns; stable environment | Measure player/power polling and unnecessary synchronization |
| Affinity isolation | Stationary Canine in forest, taiga, and neither; then boundary crossings; learning notifications both off and on | Reproduce R1 and establish transition counts |
| Typical multiplayer | 8 and 16 players; repeatable movement/combat route; mixed races; abilities used only when legitimately ready | Representative cost and behavior |
| Heavy clustered multiplayer | 32 and 64 players in shared combat arenas; eight hostile and two neutral mobs per player; fixed pets/teams; staggered casts plus synchronized bursts | Candidate scans, effect application, recipient fan-out, tail latency |
| Heavy distributed multiplayer | Same players, entities, and actions split between separated arenas/dimensions | Separate local-density effects from total online-player cost |
| Cooldown isolation | One caster with 0, 7, and 31 nearby observers; ordinary active, flap, and Nine Lives cases | Attribute synchronization/serialization traffic to resource decay |
| Summon isolation | 16 and 32 Skeleton players, using the authored two-servant summon and cooldown | Measure legitimate minion AI and ambient traffic |
| Trap isolation | Legitimate Arachnid placement cadence, plus separate artificial fixtures of 1,000 and 10,000 loaded traps | Distinguish normal benefit from extreme scaling |
| Presentation isolation | Every signature recipe; density settings; 1/8/32 observers; 100/1,000/10,000 queued beats in a separate synthetic test | Packet and scheduler scaling |
| Lifecycle/failure | Repeated race changes, respawns, dimension travel, reloads, joins/leaves, missing integrations, denied flaps | Correctness, retained state, failure-path cost |

For the typical mix, include Canine, Primian, Dark Elf, Arachnid, Fire Drake, Wind Wyrm, Skeleton, and Blood Elf; extend the 16-player mix with the other flight races, Wraith, Dryad, Deep One, Forge One, and Feline. Run a separate correctness sweep across all 37 races.

Keep synthetic resource resets, exaggerated trap counts, and artificial burst scheduling separate from ordinary gameplay results.

Run a two-hour lifecycle soak with repeated connection/race/dimension cycles. Include actual ticking through long cooldowns: simply changing day time does not simulate resource countdown processing.

## 5. Recommendations

### R1 — Aggregate flags by contributing power, then synchronize the final state

**Affected code:** [BiomeAffinityPower.java][biome], `tick`, `onRemoved`; [ScalingAttributePower.java][scaling], `tick`, `onRemoved`; [RaceStateTracker.java][tracker], `setFlag`, `clear`, `resync`.

**Confirmed issue:** [Canine's pack_hunter.json][canine] contains separate forest and taiga affinity powers. Both write `BIOME_HOME`. Where only one tag matches, one writer sets the bit and the other clears it. Repeated evaluation can generate artificial transitions while the player remains stationary. The attribute modifiers themselves are separate; the demonstrated conflict concerns shared state reporting.

Implement:

- Maintain contributions keyed by player lifecycle generation and configured power identity.
- Compute shared flags as the aggregate of their valid contributors. Removing one power removes only its contribution.
- Update logical state on the server and flush at most one final owner-state packet per player per server tick.
- Derive notifications and onset cues from aggregate transitions, not intermediate setter calls.
- Explicitly initialize/hydrate state after login, replacement of the player entity, dimension change, and race change.
- Clear removed/reloaded sources; never accumulate an OR of historical values.

Preserve the current evaluation cadence. Do not introduce additional biome scans merely to support aggregation.

**Benefit:** removes a real correctness defect and avoidable packets/notifications. Overall tick-time improvement is probably modest. **Risk:** clearing the wrong source or sending a stale snapshot during reload/clone. Require stationary-affinity and multi-power removal tests before merging.

### R2 — Make lifecycle ownership explicit before expanding caches

**Affected code:** [IntegrationManager.java][integrations], `init`, `tryLoad`, `syncPlayer`, `SyncHandler`; [RunicRacesMod.java][mod], constructor and `onConfigReloading`; [RaceHelper.java][racehelper], `getRaceId`, `invalidate`, `clearAll`; [RacialEventHandler.java][events], `onPlayerTick`, `applyAdaptationModifier`, `onPlayerClone`.

Confirmed problems and gaps:

- `IntegrationManager.initialized` and its active integration list persist for the JVM. A second integrated-server world cannot establish a fresh activation configuration.
- Reloading server configuration only calls `syncPlayer`; it does not consistently enable/disable integrations or their event handlers.
- Clearing a Primian's Runic race entirely causes `onPlayerTick` to return before removing the adaptation modifier.
- `RaceHelper` memoization uses UUID, side, and game time. Same-tick origin changes or player replacement need explicit invalidation before broader caching is safe.
- State hydration is not uniformly represented across login, respawn, dimension change, and reload.

Implement separate lifetimes:

| Lifetime | Owned state |
| --- | --- |
| Mod/JVM | Registered packet types, immutable race metadata, optional API discovery, registry registration |
| Server session | Integration activation/config generation, presentation scheduler, reload-dependent descriptors |
| Player lifecycle | Race memo, flag contributions, last-sent values, denial guards, pending cosmetic ownership |
| Persistent gameplay | Existing resource values, adaptation history, revival data, trap/minion owner and expiry |

Separate one-time integration/API registration from per-server activation. Do **not** reset `initialized` and repeatedly register listeners or Pehkui registry entries.

On configuration changes, reconcile only affected integrations. On race removal, remove owned transient modifiers even when the new race is absent. On player replacement or dimension change, invalidate cached identity before resynchronizing.

Keep the existing one-tick race memo initially. Adopt event-driven, longer-lived race caching only after verifying an Origins change hook that covers commands, selection, reloads, and external add-ons; otherwise retain the existing bounded polling fallback.

Also reset [ClientRaceState][clientstate] mirrors at connection/lifecycle boundaries and request/apply authoritative hydration. Missing explicit reset coverage is a gap, not proof that every dimension transfer currently displays stale state.

**Benefit:** primarily correctness and bounded ownership. This enables subsequent optimizations safely. **Risk:** high if lifecycle changes accidentally reset persistent cooldowns, duplicate integrations, or remove another mod's state.

### R3 — Reduce cooldown serialization without changing resource semantics

**Affected code/data:** [generate_races.py][generator], `cooldown_subpowers`, `active_power`, `resource_holder`, `wings_specs`; the generated [power JSON files][powers]; [OriginsPowerHelper.java][powerhelper], `setResourceValue`, `isResourceReady`; [ClientCooldownReader.java][cooldownreader], `read`.

**Confirmed RR behavior:** there are 42 cooldown resources: 38 decay schedules at ten ticks and four flap schedules at five ticks. These are catalog totals, not resources held by every player. While active, an ordinary timer requests two changes per second at 20 TPS; a flap timer requests four.

The inspected upstream Apoli [ChangeResourceAction.execute][apoli_change] synchronizes after the mutation. Its [S2CSynchronizePowerContainer][apoli_sync] constructs a snapshot containing powers, sources, and serialized power data.

**Not yet verified:** that upstream revision is not established as byte-for-byte equivalent to the shipped Apoli 2.9.0.8 JAR. Verify the actual packaged implementation and recipient behavior before implementing interception or replacement.

Proceed in stages:

1. **Attribute the cost.** Count RR-induced full synchronizations, serialized power/subpower counts, bytes, recipients, and client rebuild work.
2. **Introduce a scoped mutation/synchronization boundary.** A proposed RR decay action can retain the existing `origins:resource` objects and identifiers, perform exactly the same decrement on exactly the same due ticks, and collect RR-owned changes.
3. **First preserve full snapshots while coalescing redundant RR requests within a tick.** Measure this separately; it may offer limited benefit when only one timer changes.
4. **If justified, add resource deltas.** Send changed resource IDs and authoritative values to the owner and the recipients that currently receive the Apoli state. Apply updates to the real client power state, not only an RR HUD cache.
5. Keep full hydration for login, new tracking, clone, race/reload changes, and recovery. Retain normal Apoli synchronization for unrelated powers and unsupported/custom resource behavior.

Do not globally suppress `IPowerContainer.sync()`. Min/max callbacks or custom resource actions can change other power state; use a full-snapshot fallback whenever that behavior cannot be represented safely.

Keep current resource IDs, values, maximums, assignment/change behavior, save representation, and command compatibility. Update the generator and committed JSON together.

A deadline-backed cooldown is a **later, higher-risk option**, not the initial recommendation. It must reproduce current tick phase, offline behavior, clone semantics, external assignments, and callbacks. An absolute `gameTime` deadline could advance while an unloaded player's current resource would not.

Verify [RacialCooldownOverlay.java][cooldownoverlay], `updateResourceCache` and race-cache invalidation. Unknown state must not become a false “ready” indication during hydration.

**Benefit:** potentially the largest allocation/network improvement, especially with observers and long cooldowns. **Risk:** high—stale client powers, incorrect cooldown timing, broken external resource actions, or save incompatibility. Ship only after exact-dependency and lifecycle tests pass.

### R4 — Consolidate particle emissions while preserving timing, recipients, and geometry

**Affected code:** [RunicPresentation.java][presentation], `spawnShaped`, `spawnOneVfx`, `fire`; [SignatureRegistry.java][signatures], static recipes; [ConeBreathAction.java][cone], `execute`; [TremorPingAction.java][tremor], `execute`; [RacialEventHandler.java][events], `onMeleeDamageDealt`.

**Confirmed work:**

- Non-point shapes issue individual particle-send calls.
- Tremor presentation emits a 24-point ring through repeated sends.
- A range-seven breath issues 14 primary emission calls plus secondary and impact calls.
- Blood Elf siphon presentation emits five individual line points.

Particle count and packet count are different. Existing particle-budget tests do not establish network cost.

Implement a bounded S2C emission format:

- Send one shape/emission batch per due presentation beat, containing the necessary particle options, origin, direction, shape parameters, density, and fixed target data.
- Expand deterministic geometry on the client.
- Group repeated breath/line emissions where possible.
- Leave already-efficient point bursts alone unless measurement identifies a benefit.
- Retain server-side sound delivery and owner-only banners/screen cues.

Do not send an entire future animation anchored to the caster's initial position if the current delayed beat uses the caster's later position/look. Resolve each beat at its existing server due time.

Recipient selection must reproduce current particle visibility semantics. Caster tracking alone may exclude someone near an extended cone or line. Determine the union of eligible recipients for the emission and preserve per-emission distance filtering, dimension boundaries, and applicable visibility behavior.

**RNG constraint:** `spawnShaped` uses server randomness for some shapes. Moving those draws to a client seed changes the server's shared random sequence. Initially preserve the same server draws and batch their results, or transmit sufficient sampled parameters. Introducing a separate cosmetic random stream should be an explicit determinism change with its own validation.

Update [NetworkHandler.java][network], `init`, for R3/R4 packets: append registrations after the existing six, bump protocol version `"2"`, enforce bounded decoding, and reject incompatible clients cleanly.

**Benefit:** potentially high packet/encoding savings when many players observe casts. **Risk:** altered presentation, missed recipients, shared-RNG changes, client overload, or incompatible packet handling. Keep a legacy-transport fallback within the new compatible release while validating the target modpack.

### R5 — Precompute immutable configuration-derived values

**Affected methods:** [BiomeAffinityPower.java][biome], `Configuration.modifierUuid`, `resolveTag`, `tick`; [ScalingAttributePower.java][scaling], `Configuration.modifierUuid`, `resolveAttribute`, `lookupAttribute`, `resolveOperation`, `tick`.

**Confirmed work:**

- UUID caching avoids repeated hashing, but each call still constructs its string key.
- Biome affinity derives four role keys even when some configured roles are unused.
- Invalid/unavailable scaling attributes are not negatively cached, so lookup and warnings repeat.
- Static identifier/warning caches retain entries across reload generations.

Implement immutable derived configuration data:

- Precompute exactly the current UUID keys/UUIDs, parsed identifiers, operation enums, and configured role lists.
- Resolve registry objects at the appropriate registry-ready boundary.
- Represent failed resolution explicitly and warn once per configuration generation.
- Validate malformed identifiers and non-finite numeric input during loading, rather than allowing repeated failures inside ticks.
- Rebuild reload-dependent descriptors when their configuration changes.

Preserve UUID derivation byte-for-byte. Do not add power identity to modifier UUIDs as part of this optimization; that could change stacking behavior. Flag-contribution identity from R1 is a separate concern.

Continue checking actual attribute state before mutation so external modifier removal can be reconciled. Do not cache player `AttributeInstance` objects in process-wide descriptors.

Some factories run on both logical sides. Preserve thread safety through immutable publication or separate ownership; replacing shared concurrent caches with an unsynchronized map is not automatically safe in integrated play.

**Benefit:** predictable, modest steady-state savings and quieter invalid-configuration behavior. **Risk:** stale registry references, changed modifier identity, or missed cleanup when a formerly nonzero role becomes zero.

### R6 — Remove redundant per-target work without changing whom abilities affect

**Affected code:** [AfflictHostilesAction.java][afflict], `execute`, `resolveEffect`; [GlowHostilesAction.java][glow], `execute`; [ConeBreathAction.java][cone] and [TremorPingAction.java][tremor], `execute`; [Hostility.java][hostility], `isThreatTo`, `isProtectedAlly`.

Implement:

- Resolve each configured effect once per action/configuration generation, outside `AfflictHostilesAction`'s target loop. This changes resolution work from targets × effects to effects.
- Continue allocating a separate `MobEffectInstance` for each application; these are mutable.
- For cone tests, consider scalar deltas, squared-distance rejection, and one square root instead of temporary vector chains plus repeated normalization.
- Preserve behavior at the origin epsilon, range boundary, angle boundary, and unusual datapack angles.
- Keep entity queries local and transient. Do not cache live target lists across ticks or build a global entity index without evidence that it is needed.

Preserve these existing distinctions:

- Affliction, glow, and tremor use their current bounding-box queries; changing them to spheres changes affected targets.
- Breath deliberately includes unprotected neutral/passive entities.
- Other hostile-only actions use `Hostility.isThreatTo`.
- The breath impact-particle cap is not a damage-target cap.
- Terra Drake's cone and tremor have different regions/semantics; combining their queries is not automatically equivalent.
- Do not skip effect refreshes simply because an effect is present; duration, amplifier, immunity, and Forge events matter.

**Benefit:** moderate during crowded AoE; likely small at ordinary entity counts. **Risk:** altered hit sets, team/pet behavior, or effect ordering. Optimize arithmetic only if allocation/CPU profiles justify it; effect-resolution hoisting is the clearer first change.

### R7 — Make integration synchronization idempotent

**Affected code:** [CuriosIntegration.java][curios], `applySlotGrants`, `removeAllSlotGrants`; [ApotheosisIntegration.java][apotheosis], `syncPlayer`; [FeathersIntegration.java][feathers], `applyRacialStamina`; [RaceRegistry.java][raceregistry], `allSlotGrants`.

Confirmed redundant work:

- Curios removes all racial slot modifiers before applying the desired grants.
- `allSlotGrants()` reconstructs the grant collection by traversing the registry.
- Apotheosis removes/reapplies its modifier during synchronization.
- Feathers writes the maximum and applied marker on repeated syncs.

Implement desired-versus-actual reconciliation using owned UUID, amount, and operation. Precompute the immutable set of known slot grants. Skip unchanged stamina writes, using the optional API's actual current maximum.

Curios deserves particular care: temporary slot removal could have inventory consequences depending on the installed version. Verify filled slots across same-race sync, dimension change, and race changes that retain equivalent grants.

Feathers currently restores a hard-coded maximum of 20 when removing a previously applied racial pool. That can overwrite another pack's baseline. Define ownership/composition behavior before adding a cached “already applied” shortcut; otherwise the shortcut can conceal incorrect restoration.

[PehkuiIntegration.java][pehkui] already compares scale values before changing them. Preserve its collision/resize safety work.

**Benefit:** mostly smaller login, respawn, race-change, and reload bursts—not a large every-tick gain. **Risk:** slot/item loss, stale optional-mod attributes, or incorrect stamina restoration.

### R8 — Schedule trap expiry instead of ticking every loaded trap

**Affected code:** [TrapMarkerBlock.java][trapblock], `getTicker`, `entityInside`; [TrapMarkerBlockEntity.java][trapentity], `serverTick`, `setOwner`, `load`, `saveAdditional`; [PlaceTrapAction.java][placetrap], `execute`.

**Confirmed:** the block-entity ticker runs every tick; the modulo check only reduces expiry checks to once per second.

Replace normal ticking with a scheduled block tick:

- Keep persisted owner and `expiresAt` as canonical state.
- Schedule from placement/owner assignment and reestablish scheduling when loading legacy traps.
- Preserve the current expiry quantization to the next eligible 20-tick check unless a separately reviewed correction changes it.
- On execution, verify the block type and current deadline. An old scheduled tick must not remove a replacement trap.
- Preserve default expiry initialization for command-placed traps.
- Do not force-load chunks to expire traps. Check expiry when the chunk resumes normal processing.

Two existing compatibility hazards need explicit tests: owner lookup is limited to an online player in that level, and `mayInteract` alone does not establish compatibility with every claim mod. Slowness is also applied independently of whether damage succeeds. Define intended offline-owner, PvP, and cancellation behavior before restructuring trigger handling.

**Benefit:** removes continuous dispatch proportional to loaded trap count. Normal benefit may be small because authored trap lifetime/cooldown constrain accumulation. **Risk:** immortal legacy traps, premature expiry, stale scheduled ticks, or changed protection behavior.

### R9 — Fix scheduler compaction first; adopt deadline buckets only if needed

**Affected code:** [BeatQueue.java][beatqueue], `schedule`, `tick`, `cancel`, `clear`; [PresentationScheduler.java][scheduler], scheduling methods and `onServerTick`.

**Confirmed:** each active tick visits every queued beat, creates a due list, and removes due entries individually from an `ArrayList`. Many simultaneous removals repeatedly shift remaining elements.

First implement stable single-pass compaction and avoid allocating a due list when nothing is due. This addresses the clearest algorithmic issue with limited complexity.

If profiling still identifies queue traversal as meaningful, use absolute deadlines with 101 buckets, matching the existing maximum delay of 100 ticks. Preserve:

- Clamp behavior for delays outside 1–100.
- Same-tick insertion order.
- Existing semantics when scheduling occurs before the scheduler's tick callback.
- Reentrant scheduling without modifying the batch currently executing.
- Cancellation and release of references on death/logout/stop.

The queue already stores UUIDs rather than player/world objects. It is not a demonstrated permanent leak.

Add dimension/lifecycle ownership to delayed beats. Currently the scheduler resolves the player's current level, while a saved line target can contain coordinates from an earlier dimension. Define cancellation across dimension/race replacement and test it; the queue algorithm change should not silently decide this policy.

**Benefit:** primarily burst-tail improvement. **Risk:** off-by-one delays, order changes, or replaying stale presentation after lifecycle transitions.

### R10 — Treat minion optimization as a measured follow-up

**Affected code:** [GraveServantEntity.java][servant], `aiStep`, `isOwner`, `canAttack`, `setTarget`; [SummonMinionAction.java][summon], `execute`.

Confirmed repeated work includes persistent-tag access for expiry and UUID decoding during ownership checks. Whether this matters beside zombie AI/pathfinding is unverified.

If profiling justifies changes:

- Cache owner/expiry in entity fields with explicit save/load compatibility for the existing persistent tags.
- Preserve command/other-mod modifications to those tags, or document and validate a migration that maintains one authoritative representation.
- Consider checking an already-expired servant before expensive AI processing, with regression tests for expiry-tick behavior.
- Fold ambient particle transport into R4 if it contributes meaningful traffic.

Do not reduce targeting frequency, speed, range, summon count, or lifetime as an optimization. Do not force-load chunks for expiry.

The shipped summon creates two servants; the action already clamps configured count. Its generic entity option does not prove every arbitrary summoned type implements the grave servant's expiry logic.

**Benefit:** probably modest unless profiles show otherwise. **Risk:** stale ownership, changed AI behavior, or persistent immortal entities after migration.

### R11 — Bound feedback and failure work without weakening validation

**Affected code:** [FlightServerHandler.java][flight], `handleFlap`, `handleCancel`, `onLogout`; [ManaHelper.java][mana] and [StaminaHelper.java][stamina], API initialization, availability checks, reads, and consumption.

Implement:

- Put the exhausted-flap banner behind the same feedback debounce as its sound. Currently sound is debounced but the banner is sent on each eligible denied attempt.
- Retain every authoritative race, glide, cooldown, and cost check. Debouncing feedback must not authorize an action or consume a failed action's cooldown.
- Add explicit server-stop teardown for the flap guard map. Logout cleanup already exists; a normal-session leak is not established.
- Resolve optional reflection APIs atomically: mark an adapter available only after all required members are present.
- Disable or back off a demonstrably incompatible adapter instead of repeatedly throwing exceptions. Do not permanently disable it merely because a transient player capability is temporarily unavailable.
- Preserve existing standalone/resource-availability and fail-open/fail-closed policies.

Reflection method lookup is already cached; replacing it merely to “avoid reflection lookup every tick” would address a nonexistent issue.

For cooldown-feedback regression coverage, inspect [AbilityDenyHandler.java][deny], `onPrimaryActivePressed`: subtracting an initial `Long.MIN_VALUE` timestamp overflows. Fix/test initialization if changing this feedback path; that is a client correctness repair, not a server optimization.

Also investigate [C2SBackToFamilyPacket.java][backpacket], `handle`, for potentially duplicated container synchronization. This is a **hypothesis** until the shipped Origins `synchronize()` implementation is inspected. Preserve the ordered “clear family state, then open selection screen” behavior.

Do not attribute an extra tick of latency to nested `enqueueWork` merely from its presence: inspected Forge code executes it immediately when already on the target main thread.

**Benefit:** mainly failure-path and repeated-input savings. **Risk:** suppressed legitimate feedback, incorrect resource policy, or removed selection synchronization.

## 6. Persistence, allocation, caching, and retained memory

Keep persistence and memory changes narrow.

[RacialEventHandler.java][events], `tickHumanAdaptation`, stores gameplay state and a last-sent marker together. [IntegrationManager.java][integrations] likewise persists a last-synced race marker. Move synchronization bookkeeping into R2's lifecycle-owned state when those paths are changed, while retaining gameplay persistence and cleaning obsolete bookkeeping keys compatibly.

Avoid a wholesale NBT rewrite without evidence. Updating a `CompoundTag` is not equivalent to writing a file on every update.

Audit retained objects after lifecycle tests:

- Online-player maps must lose departed UUIDs.
- Presentation queues must release canceled entries.
- Reload caches must be bounded by the current configuration generation.
- No RR-owned server cache may retain departed `ServerPlayer`, `ServerLevel`, or player attribute/capability objects.
- Outbound buffers/backlogs must drain after bursts or disconnected clients.

No custom worker pool or recurring application-level file-open loop was identified in the main code. Existing [ProcDebounce][debounce] cleanup and scheduler cleanup should remain.

Separate client heap issues from dedicated-server costs. [ClientRacialAmbienceHandler.java][ambience] contains client-side entity/block scans; these are not dedicated-server tick hotspots. Review its retained entity lists on dimension/config changes if integrated-client memory grows. Weak player-keyed rendering maps are not, by themselves, evidence of leaks.

Rare paths should be profiled separately. [RacialEventHandler.java][events], `findSafeRevivalPosition`, performs a bounded upward search, but destination chunk access may affect respawn latency. Its negative-coordinate truncation and fixed clearance assumptions also deserve correctness fixtures. Do not “optimize” revival by skipping collision checks or moving world access off-thread.

## 7. Regression and compatibility checks

The existing [test suite][tests] provides useful data parity, identifier, asset, protocol-order, and pure queue checks. Many tests inspect JSON or source text; they do not establish actual event timing, packet behavior, or persistence.

Required additions:

| Area | Required checks |
| --- | --- |
| All races | All 37 races load, select, grant/remove their powers, and retain expected attributes, passives, weaknesses, and active effects |
| Cooldowns | Activation at every phase within the 5/10-tick intervals; exact readiness transition; four flap durations; Nine Lives; external set/change/reset; no negative values or double activation |
| Multiplayer state | Owner, existing observer, newly tracking observer, reconnect, respawn, dimension travel, and race change receive consistent authoritative state |
| Persistence | Save/restart mid-cooldown; death and non-death clone; offline duration behavior; old player/trap/minion NBT; missing dimensions; failed revival does not consume its cooldown |
| Attributes | Stable UUIDs; no duplicate modifiers; exact values/operations; removal to a different race and to no race; reload with changed/zero values; other-mod modifiers survive |
| Combat | Identical target sets, damage, effects, knockback, and callback ordering; pets, allies, neutrals, PvP settings, cancellation, immunities, boundary geometry |
| Flight/resources | Invalid sender/state; non-flight race; not gliding; insufficient resources; failed spending; spammed input; velocity sync; standalone and optional-mod configurations |
| Traps/minions | Unload/reload; expiry boundaries; stale scheduled ticks; offline/different-dimension owner; protected targets; missing expiry tags; no unintended chunk loading |
| Presentation | Same due ticks, same order, density settings, particle types/counts, sound/subtitles, recipient boundaries, teleport/death cancellation, shared-RNG behavior |
| Compatibility | Required-only install; each optional integration; all integrations together; supported dependency versions; API mismatch; foreign origins/layers; example datapack; reloads |
| Packaging | Reobfuscated dedicated-server launch; client join; clean rejection of incompatible protocol versions; malformed/bounded packet decoding |

Use dedicated-server integration/GameTests or an equivalent runtime harness for world-dependent behavior, plus connected-client tests for tracking and wire state. Retain pure unit tests for queue ordering, aggregation, encoding bounds, and migration helpers.

## 8. Measurable acceptance criteria

Freeze numerical acceptance gates after the baseline, before implementation.

The following are proposed engineering targets, not observed results. Record any baseline-driven adjustment before evaluating candidates.

| Metric | Acceptance gate |
| --- | --- |
| Correctness | Zero unexplained differences in authoritative action traces, resources, target sets, modifiers, persistence, or recipient state |
| Normal-load tick regression | For idle, typical, and heavy supported workloads, p95/p99 MSPT must not increase beyond the greater of 5% or 0.25 ms; compare paired-run distributions |
| Initial service target | On the fixed reference server at the chosen 32-player workload: p95 ≤40 ms and p99 ≤50 ms; report 64-player capacity separately |
| Tail events | No new repeatable RR-attributable >100 ms stall; report login, reload, save, and revival tails separately |
| Material optimization | A complex performance-only change must show a repeatable material improvement: target ≥20% in its attributable CPU cost, or meet its allocation/network gate; otherwise defer the complexity |
| Allocation | No >5% normal-load regression; target ≥25% reduction in a specifically optimized allocation hotspot when baseline volume is measurable |
| Retained memory | After the lifecycle soak, no monotonic RR-owned retention; zero departed player/world references; all lifecycle maps/queues return to their expected bounds |
| Whole-heap check | Post-GC live set after repeated cycles should remain within the greater of 5% or 32 MiB of the matched warmed baseline; investigate excess through retained-object attribution |
| Cooldown traffic | Target ≥80% reduction in bytes caused by RR decay in the isolated cooldown test, including replacement packets and all recipients |
| Particle traffic | Target ≥80% fewer messages for batched shaped effects and ≥50% fewer encoded bytes in that fixture, with equivalent presentation |
| Total traffic | No >5% increase in combined RR-caused application traffic in any normal workload |
| Stable state | After hydration, stationary unchanged Canine state generates zero flag-transition packets/notifications |
| Attribute stability | Repeated unchanged integration/power reconciliation produces zero modifier remove/add operations |
| Trap scheduling | Zero normal per-tick block-entity callbacks for waiting traps; expiry remains within the agreed original timing window |
| Scheduler | Exact delivery/order tests pass; burst processing avoids repeated array shifting; pending references clear after cancellation |
| Lifecycle recovery | Once all players leave and pending work is canceled/expired, RR player-owned state returns to zero without requiring a JVM restart |

If the reference machine cannot meet the service target because the control workload is already overloaded, report that explicitly. Do not present lower workload completion, dropped visuals, failed abilities, or fewer affected entities as an optimization.

## 9. Implementation phases and release gates

| Phase | Work | Exit condition |
| --- | --- | --- |
| 0 — Characterize | Pin dependencies; run packaged baseline; create fixtures and counters; verify shipped Apoli/Origins synchronization and lifecycle behavior | Reproducible baseline, behavioral contract, and frozen gates |
| 1 — Correct ownership and remove direct redundancy | R1, R2, R5, R7, and narrow R11 fixes; generator/data consistency | Lifecycle/attribute/resource regressions pass; no normal-load performance regression |
| 2 — Address measured transport cost | R3 and R4 as independently benchmarked changes; protocol update and client hydration | Exact gameplay/save parity plus network and allocation evidence |
| 3 — Address remaining profiled hotspots | R6; R8 where trap population warrants it; R9 compaction and optional buckets; R10 only if justified | Each change demonstrates its intended benefit without semantic drift |
| 4 — Release validation | Full compatibility matrix, two-hour soak, long-cooldown tests, production-JAR tests, repeated benchmark comparison | All acceptance gates met and remaining limitations documented |

Keep transport, lifecycle, and persistent-format changes independently reviewable. Prefer retaining the current resource/NBT representation so rollback does not require save conversion. New protocol releases require coordinated compatible clients and servers; transport fallback settings do not make mismatched packet protocols compatible.

## 10. Outstanding verification boundary

The unresolved items are the actual CPU/allocation/traffic magnitude, exact shipped dependency internals, runtime event ordering, supported modpack interactions, and migration behavior. Those are Phase 0 and release-gate obligations; none can be established conclusively from this static audit alone.

## 11. Implementation handoff checklist

- [ ] Compare current repository state with the audited commit and update affected findings.
- [ ] Pin and hash the actual build/runtime dependencies, including nested JARs.
- [ ] Establish the behavioral contract, reproducible fixtures, and baseline results.
- [ ] Verify shipped Apoli/Origins synchronization, callbacks, and lifecycle behavior.
- [ ] Implement and validate Phase 1 correctness and direct-redundancy changes.
- [ ] Implement R3/R4 only with supporting baseline evidence and independent comparisons.
- [ ] Implement remaining performance candidates only where profiling supports their complexity.
- [ ] Run all-race, multiplayer, persistence, compatibility, and packaged-JAR regressions.
- [ ] Complete the long-cooldown checks and two-hour lifecycle soak.
- [ ] Compare tick, memory, allocation, and total network metrics against frozen gates.
- [ ] Record passed checks, failed gates, deferred candidates, and unverified boundaries.
- [ ] Prepare coordinated client/server release and rollback notes if the protocol changes.

<!-- Commit-pinned source references. -->

[repo]: https://github.com/otectus/runic-races
[commit]: https://github.com/otectus/runic-races/commit/1877c025265920f2800ac09ba7f194244e9a7e3c
[build]: https://github.com/otectus/runic-races/blob/1877c025265920f2800ac09ba7f194244e9a7e3c/build.gradle
[deps]: https://github.com/otectus/runic-races/blob/1877c025265920f2800ac09ba7f194244e9a7e3c/ci/fetch-deps.sh
[ci]: https://github.com/otectus/runic-races/blob/1877c025265920f2800ac09ba7f194244e9a7e3c/.github/workflows/ci.yml
[spark]: https://spark.lucko.me/docs/Command-Usage
[biome]: https://github.com/otectus/runic-races/blob/1877c025265920f2800ac09ba7f194244e9a7e3c/src/main/java/com/otectus/runic_races/power/BiomeAffinityPower.java
[scaling]: https://github.com/otectus/runic-races/blob/1877c025265920f2800ac09ba7f194244e9a7e3c/src/main/java/com/otectus/runic_races/power/ScalingAttributePower.java
[tracker]: https://github.com/otectus/runic-races/blob/1877c025265920f2800ac09ba7f194244e9a7e3c/src/main/java/com/otectus/runic_races/common/state/RaceStateTracker.java
[canine]: https://github.com/otectus/runic-races/blob/1877c025265920f2800ac09ba7f194244e9a7e3c/src/main/resources/data/runic_races/powers/canine/pack_hunter.json
[integrations]: https://github.com/otectus/runic-races/blob/1877c025265920f2800ac09ba7f194244e9a7e3c/src/main/java/com/otectus/runic_races/integration/IntegrationManager.java
[mod]: https://github.com/otectus/runic-races/blob/1877c025265920f2800ac09ba7f194244e9a7e3c/src/main/java/com/otectus/runic_races/RunicRacesMod.java
[racehelper]: https://github.com/otectus/runic-races/blob/1877c025265920f2800ac09ba7f194244e9a7e3c/src/main/java/com/otectus/runic_races/util/RaceHelper.java
[events]: https://github.com/otectus/runic-races/blob/1877c025265920f2800ac09ba7f194244e9a7e3c/src/main/java/com/otectus/runic_races/event/RacialEventHandler.java
[clientstate]: https://github.com/otectus/runic-races/blob/1877c025265920f2800ac09ba7f194244e9a7e3c/src/main/java/com/otectus/runic_races/client/state/ClientRaceState.java
[generator]: https://github.com/otectus/runic-races/blob/1877c025265920f2800ac09ba7f194244e9a7e3c/tools/generate_races.py
[powers]: https://github.com/otectus/runic-races/tree/1877c025265920f2800ac09ba7f194244e9a7e3c/src/main/resources/data/runic_races/powers
[powerhelper]: https://github.com/otectus/runic-races/blob/1877c025265920f2800ac09ba7f194244e9a7e3c/src/main/java/com/otectus/runic_races/util/OriginsPowerHelper.java
[cooldownreader]: https://github.com/otectus/runic-races/blob/1877c025265920f2800ac09ba7f194244e9a7e3c/src/main/java/com/otectus/runic_races/client/state/ClientCooldownReader.java
[apoli_change]: https://github.com/EdwinMindcraft/apoli/blob/f1c8f409327f23c59aeeeb058fd381fd506b125a/src/main/java/io/github/edwinmindcraft/apoli/common/action/entity/ChangeResourceAction.java
[apoli_sync]: https://github.com/EdwinMindcraft/apoli/blob/f1c8f409327f23c59aeeeb058fd381fd506b125a/src/main/java/io/github/edwinmindcraft/apoli/common/network/S2CSynchronizePowerContainer.java
[cooldownoverlay]: https://github.com/otectus/runic-races/blob/1877c025265920f2800ac09ba7f194244e9a7e3c/src/main/java/com/otectus/runic_races/client/RacialCooldownOverlay.java
[presentation]: https://github.com/otectus/runic-races/blob/1877c025265920f2800ac09ba7f194244e9a7e3c/src/main/java/com/otectus/runic_races/presentation/RunicPresentation.java
[signatures]: https://github.com/otectus/runic-races/blob/1877c025265920f2800ac09ba7f194244e9a7e3c/src/main/java/com/otectus/runic_races/presentation/SignatureRegistry.java
[cone]: https://github.com/otectus/runic-races/blob/1877c025265920f2800ac09ba7f194244e9a7e3c/src/main/java/com/otectus/runic_races/action/ConeBreathAction.java
[tremor]: https://github.com/otectus/runic-races/blob/1877c025265920f2800ac09ba7f194244e9a7e3c/src/main/java/com/otectus/runic_races/action/TremorPingAction.java
[network]: https://github.com/otectus/runic-races/blob/1877c025265920f2800ac09ba7f194244e9a7e3c/src/main/java/com/otectus/runic_races/network/NetworkHandler.java
[afflict]: https://github.com/otectus/runic-races/blob/1877c025265920f2800ac09ba7f194244e9a7e3c/src/main/java/com/otectus/runic_races/action/AfflictHostilesAction.java
[glow]: https://github.com/otectus/runic-races/blob/1877c025265920f2800ac09ba7f194244e9a7e3c/src/main/java/com/otectus/runic_races/action/GlowHostilesAction.java
[hostility]: https://github.com/otectus/runic-races/blob/1877c025265920f2800ac09ba7f194244e9a7e3c/src/main/java/com/otectus/runic_races/util/Hostility.java
[curios]: https://github.com/otectus/runic-races/blob/1877c025265920f2800ac09ba7f194244e9a7e3c/src/main/java/com/otectus/runic_races/integration/curios/CuriosIntegration.java
[apotheosis]: https://github.com/otectus/runic-races/blob/1877c025265920f2800ac09ba7f194244e9a7e3c/src/main/java/com/otectus/runic_races/integration/apotheosis/ApotheosisIntegration.java
[feathers]: https://github.com/otectus/runic-races/blob/1877c025265920f2800ac09ba7f194244e9a7e3c/src/main/java/com/otectus/runic_races/integration/feathers/FeathersIntegration.java
[raceregistry]: https://github.com/otectus/runic-races/blob/1877c025265920f2800ac09ba7f194244e9a7e3c/src/main/java/com/otectus/runic_races/race/RaceRegistry.java
[pehkui]: https://github.com/otectus/runic-races/blob/1877c025265920f2800ac09ba7f194244e9a7e3c/src/main/java/com/otectus/runic_races/integration/pehkui/PehkuiIntegration.java
[trapblock]: https://github.com/otectus/runic-races/blob/1877c025265920f2800ac09ba7f194244e9a7e3c/src/main/java/com/otectus/runic_races/block/TrapMarkerBlock.java
[trapentity]: https://github.com/otectus/runic-races/blob/1877c025265920f2800ac09ba7f194244e9a7e3c/src/main/java/com/otectus/runic_races/block/TrapMarkerBlockEntity.java
[placetrap]: https://github.com/otectus/runic-races/blob/1877c025265920f2800ac09ba7f194244e9a7e3c/src/main/java/com/otectus/runic_races/action/PlaceTrapAction.java
[beatqueue]: https://github.com/otectus/runic-races/blob/1877c025265920f2800ac09ba7f194244e9a7e3c/src/main/java/com/otectus/runic_races/presentation/BeatQueue.java
[scheduler]: https://github.com/otectus/runic-races/blob/1877c025265920f2800ac09ba7f194244e9a7e3c/src/main/java/com/otectus/runic_races/presentation/PresentationScheduler.java
[servant]: https://github.com/otectus/runic-races/blob/1877c025265920f2800ac09ba7f194244e9a7e3c/src/main/java/com/otectus/runic_races/entity/GraveServantEntity.java
[summon]: https://github.com/otectus/runic-races/blob/1877c025265920f2800ac09ba7f194244e9a7e3c/src/main/java/com/otectus/runic_races/action/SummonMinionAction.java
[flight]: https://github.com/otectus/runic-races/blob/1877c025265920f2800ac09ba7f194244e9a7e3c/src/main/java/com/otectus/runic_races/flight/FlightServerHandler.java
[mana]: https://github.com/otectus/runic-races/blob/1877c025265920f2800ac09ba7f194244e9a7e3c/src/main/java/com/otectus/runic_races/util/ManaHelper.java
[stamina]: https://github.com/otectus/runic-races/blob/1877c025265920f2800ac09ba7f194244e9a7e3c/src/main/java/com/otectus/runic_races/util/StaminaHelper.java
[deny]: https://github.com/otectus/runic-races/blob/1877c025265920f2800ac09ba7f194244e9a7e3c/src/main/java/com/otectus/runic_races/client/AbilityDenyHandler.java
[backpacket]: https://github.com/otectus/runic-races/blob/1877c025265920f2800ac09ba7f194244e9a7e3c/src/main/java/com/otectus/runic_races/network/C2SBackToFamilyPacket.java
[debounce]: https://github.com/otectus/runic-races/blob/1877c025265920f2800ac09ba7f194244e9a7e3c/src/main/java/com/otectus/runic_races/presentation/ProcDebounce.java
[ambience]: https://github.com/otectus/runic-races/blob/1877c025265920f2800ac09ba7f194244e9a7e3c/src/main/java/com/otectus/runic_races/client/ClientRacialAmbienceHandler.java
[tests]: https://github.com/otectus/runic-races/tree/1877c025265920f2800ac09ba7f194244e9a7e3c/src/test/java/com/otectus/runic_races

