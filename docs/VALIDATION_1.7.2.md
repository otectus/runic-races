# Runic Races 1.7.2 — Server Optimization Validation Record

**Scope:** first incremental release against [the server optimization plan](Runic_Races_Server_Optimization_Implementation_Plan.md).
**Base:** the uncommitted 1.7.1 working tree (the plan audited 1.6.3, commit `1877c02`).
**Status:** implemented; unit tests, three GameTest dependency profiles, build and release audit
pass. The plan's **measured multiplayer benchmarks, soak and packaged-JAR server launch were not
run** (see *Outstanding gates*). The byte and packet figures below are encoded application traffic
from a deterministic GameTest fixture, not tick-time measurements.

## 1. Plan findings against the current tree

The plan targeted 1.6.3. The 1.7.0/1.7.1 work had already changed several findings before this pass:

| Finding | State found in 1.7.1 | 1.7.2 action |
|---|---|---|
| R1 conflicting flag writers | Still present: `canine/pack_hunter` `forest_home` and `taiga_home` both write `BIOME_HOME` on the same 40-tick cadence | Fixed (aggregation) |
| R2 lifecycle gaps | `IntegrationManager.initialized` JVM-wide; reload only resynced; Primian modifier kept on race clear; no client state reset | Fixed |
| R3 cooldown full syncs | Present (43 decaying timers) | Fixed (delta transport) |
| R4 per-particle packets | Breath already batched in 1.7.0 (`S2CBreathVfxPacket`); shaped signatures, tremor ring, siphon and 1.7.0 feedback rings/trails/cry still per-particle | Fixed for all per-point loops |
| R5 derived config | Still derived per tick | Fixed |
| R6 per-target effect resolution | Present in `AfflictHostilesAction` | Fixed. The plan's note that affliction/glow/tremor queries are boxes is out of date: 1.7.1 made them spherical. |
| R7 integration churn | Curios/Feathers already reconciled in 1.7.0; Apotheosis still remove-then-add | Fixed (Apotheosis, grant union, Feathers marker writes) |
| R8 trap ticker | Present | **Deferred** (see §4) |
| R9 queue compaction | Present | Fixed |
| R10 minion metadata | Present | **Deferred** (needs profiles) |
| R11 flap banner / adapters | Banner undebounced; adapters non-atomic; `AbilityDenyHandler` overflow already fixed in 1.7.1 | Fixed |
| Network protocol | Docs said 3; the shipped 1.7.1 jar uses **4** | Now **5**; README corrected |

## 2. Phase 0 evidence

**Build pins.** ForgeGradle `6.0.+` → `6.0.54` and MixinGradle `0.7.+` → `0.7.38`: the versions
`gradlew buildEnvironment` already resolved, so the build is unchanged.

**Dependency hashes** (SHA-256; versions from `META-INF/mods.toml` / manifest, not filenames):

| Jar | Declared version | SHA-256 |
|---|---|---|
| `origins-forge-1.20.1-1.10.0.9-all.jar` | 1.20.1-1.10.0.9 | `5244b8b4434cd1bb6f4ebb71a36008218e700ed3245c14f867b8dc5487137c64` |
| `apoli-forge-1.20.1-2.9.0.8.jar` (identical to the copy nested in Origins) | 1.20.1-2.9.0.8 | `95f41fb0f685789f21f56afc8e69062f05925965f957d8005c6de9252a1329f1` |
| `calio-forge-1.20.1-1.11.0.5.jar` (identical to nested) | 1.20.1-1.11.0.5 | `684609a02154ee987f3faae719adf2d196b815b178d77c827214c39b90717aa7` |
| `additionalentityattributes-forge-1.4.0.5+1.20.1.jar` (identical to nested) | 1.4.0.5+1.20.1 | `d3c9cd3d4691bd0dd2bc9bc895407e8569269b3b76bd3388492f5c59b6f8c03e` |
| `curios-forge-5.14.1+1.20.1.jar` | 5.14.1+1.20.1 | `1e817919a35b37cf30524aaec73f0ca5130452f23f168f844854df282eb8e51f` |
| `ars_nouveau-1.20.1-4.12.7-all.jar` | 4.12.7 | `1f1debc282a0c379c1141f2840ea294eede6f6b544c589663f40bbe17b59a1af` |
| `irons_spellbooks-1.20.1-3.15.4.jar` | **1.20.1-3.15.5.1** (filename is wrong) | `339f7e14c970ac99ae397c7cd4a7b7bad12a448a90065e4385b41b52c3a8edc6` |

**Shipped Apoli behavior (bytecode of the jar above):**
- `ChangeResourceAction.execute` always ends with `ApoliAPI.synchronizePowerContainer(living)`.
- That builds `S2CSynchronizePowerContainer.forEntity` and sends it with `PacketDistributor.TRACKING_ENTITY_AND_SELF`.
- The client's `PowerContainer.handle` clears and rebuilds every power, source and datum.
- Resource data serializes as `{"Value": int}` through `VariableIntPowerFactory.Simple`.
- An absent `min_action`/`max_action` defaults to the `ApoliEntityActions.NOTHING` configured action.
- Resource decay runs server-side only (`PowerContainer.serverTick`).

**Origins `synchronize()`** (for the plan's `C2SBackToFamilyPacket` hypothesis) only raises a flag.
The next container tick then sends the origin packet to trackers and self, plus a full power sync.
The packet's explicit immediate send duplicates the owner's origin packet once per back-out. It
was **kept**: it is a rare UI action, and it preserves the "confirm the clear, then reopen the
screen" ordering.

**Counters.** `/runicraces diagnostics [reset]` (permission 2) reports fixed-category counters with
per-second rates, plus the size of every per-player map Runic Races owns.

## 3. What changed (behavioral contract preserved)

- **R3.** Decay steps use `runic_races:cooldown_decay`. It is the same mutation as
  `origins:change_resource`: same holder, `change`, ownership check and `action_over_time`
  interval. Changed resources are sent once per entity per tick as `S2CPowerDataPacket` to
  `TRACKING_ENTITY_AND_SELF`. Each value is serialized and read at flush time, and the client applies
  it with `ConfiguredPower.deserialize`, the per-power path of Apoli's own full sync. Any resource
  that is not a plain resource with no-op min/max actions keeps the full sync. Activation keeps
  `origins:change_resource`. Resource ids, values, maxima, save data and commands are unchanged.
  The generator and the 43 decay JSONs were updated together: one line each, verified by diff and
  `generate_races.py --check`.
- **R4.** `ParticleBatch` collects the points of one emission. Server geometry, `level.random`
  draws and block clips stay in the same order. Delivery is one `S2CParticleBatchPacket` per player,
  and each point keeps vanilla's `closerToCenterThan(32)` rule, so a fringe player gets a subset
  packet. Client motion is `float speed × float delta`, as in vanilla's handler. Point bursts stay
  single vanilla packets. Decoding is bounded at 512 points.
- **R1.** `FlagAggregate` ORs per-power contributions (keyed by power registry name), with
  direct single-writer bits beside them. One flush per player per tick sends the packet and fires
  notifications and onset cues from the net change. After `/reload`, contributions from powers
  the player no longer holds are dropped. Respawn and dimension change re-deliver the last state.
- **R2.** Integration adapters load once per JVM. Activation is read from the live toggle, and a
  config reload re-syncs only integrations whose toggle flipped. Pehkui keeps its always-loaded
  cleanup adapter. Other fixes:
  - The race memo is invalidated on login, respawn, dimension change and clone.
  - The Primian modifier is removed when a race is cleared.
  - `ClientRaceState` resets on disconnect.
  - The flap guard map clears when the server stops.
- **R5.** Biome affinity and scaling configurations compute UUIDs, tags, active roles and the
  operation once, when the data loads. The UUID keys are byte-for-byte the old strings, checked by
  GameTest. Unknown attributes are negatively cached and warn once per configuration generation.
  Malformed ids and non-finite values fail at load; validation sits on the `MapCodec`, because
  Apoli's `IFactory.asMap` only merges a `MapCodec.MapCodecCodec`.
- **R6.** Afflictions resolve their effects once per cast; each target still gets a new
  `MobEffectInstance`.
- **R7.**
  - Apotheosis luck is reconciled by UUID, amount and operation.
  - The Curios grant union is computed once from the immutable race table.
  - Feathers skips unchanged modifier and marker writes.
- **R9.** `BeatQueue.tick` compacts in one stable pass and allocates nothing when no beat is due.
- **R11.**
  - The flap "no stamina" banner shares the sound's debounce; cooldown and cost checks are unchanged.
  - Mana/stamina adapters are all-or-nothing. A signature mismatch latches the configured fail
    policy; exceptions raised inside the other mod stay transient and are retried.
- **HUD.** A cooldown the server has not hydrated renders as pending, not ready, and the first
  hydrated value does not flash.
- **Fallbacks.** `network.cooldownDeltaSync` and `network.batchedParticles` (server config, default
  on) restore the previous transport. Neither makes mismatched protocols compatible.

## 4. Deferred, with reasons

- **R8 trap scheduled ticks.** Authored traps are bounded by cooldown and lifetime, so the ticker
  costs one dispatch per loaded trap. Legacy traps would need schedules re-established during chunk
  load, and a stale scheduled tick must never remove a replacement trap. Deferred until the trap
  isolation benchmark shows a need.
- **R10 minion metadata caching.** No profile yet separates it from zombie AI and pathfinding.
- **R6 cone arithmetic.** It is allocation-only and the plan requires profiles first. Breath target
  selection is untouched.
- **R4 client-side geometry expansion.** Points are sent pre-computed instead. This keeps
  server RNG and clip results exact, and still met the byte gate below.
- **Deadline-backed cooldowns.** Not attempted, as the plan advises.
- Persisted sync markers (`human_adapt_synced_stacks`, `last_synced_race`) were left as they are.
  Those paths were not changed, and Feathers migration reads `last_synced_race`.

## 5. Executed checks

Host: Linux 7.2.6 x86_64, Intel i7-13700HX (24 threads), OpenJDK 17.0.19, Gradle 8.6.

| Command | Result |
|---|---|
| `python3 tools/generate_races.py --check` | Passed: 54 races, semantic parity |
| `python3 tools/build_lang.py --check` | Passed: 704 keys |
| `gradlew-quiet.sh . build` (includes `test`) | Passed. JUnit **99 tests, 0 failures/errors/skipped** |
| `gradlew-quiet.sh . runGameTestServer` (required, Forge 47.2.0) | **All 32 required tests passed** |
| `… runGameTestServer -PrrRuntime=pehkui,curios,feathers` | **All 32 passed**; log: "3 integrations active (3 adapters loaded)" |
| `… runGameTestServer -PrrRuntime=curios,feathers,ars,irons -Pforge_version=47.4.10` | **All 32 passed**; "4 integrations active (4 adapters loaded)" |
| `python3 tools/audit_release.py` | Passed: 61 origins, 162 powers, 6 mixins / 6 refmap groups |

Artifact: `build/libs/runic_races-1.7.2.jar`, 1,159,416 bytes, SHA-256
`f65dc2aa6c4634f23b18ef836e1a9b695500757ecb3c2f921de26737e7940862`. The artifact is built against
Forge 47.2.0; rebuilding may change the checksum.

Problems found and fixed during validation:
- The first GameTest run caught the `MapCodec` issue above, which failed Dark Elf and Canine
  subpowers.
- An early fixture toggled `ForgeConfigSpec` values. Autosave raced Forge's config watcher
  (`Table with path [network] has been declared twice`). The GameTests now never write config:
  the legacy cooldown step is emulated exactly, and particles use `ParticleBatch.overrideTransport`.

## 6. Measurements (encoded application bytes, GameTest fixture)

Wire bytes are the packet id plus body plus length prefix, uncompressed.

| Fixture | Previous transport | 1.7.2 | Change |
|---|---|---|---|
| Fire Drake cooldown, 10 decay steps, caster + 3 tracking observers | 40 packets / 44,200 bytes (1,105 per full sync) | 40 packets / 3,800 bytes | **−91.4 % bytes** (gate: ≥80 %) |
| All 82 signature recipes, caster + near observer + observer on the 32-block edge | 2,656 packets / 137,184 bytes | 231 packets / 57,519 bytes | **−91.3 % messages, −58.1 % bytes** (gates: ≥80 %, ≥50 %) |

- **Equivalence.** In the particle fixture, both transports ran under the same `level.random`
  seed. Every recipient received the same particle type, limiter flag and bit-identical motion,
  with positions within 1e-4. In the cooldown fixture, the final values match (800 → 700). Clients
  were last told the authoritative value, and a same-tick activation after a decay reaches clients
  as 800.
- **Figures not measured:** MSPT, CPU, allocation, compression effects and real socket traffic.

## 7. Outstanding gates

1. Phase 0 baseline and paired benchmarks: 1/8/32/64 real clients, Spark/JFR, MSPT p95/p99,
   allocation and connection traffic, with 1.7.1 as the baseline.
2. Real connected clients covering late tracking, reconnect, respawn and dimension travel. In
   particular, confirm that the HUD and client-side Apoli conditions track deltas.
3. Launching the reobfuscated jar on a dedicated server and joining with a client. No Forge server
   install exists on this host, and starting one requires accepting the Minecraft EULA. Also confirm
   that a protocol-4 client is rejected cleanly.
4. Two-hour lifecycle soak. `/runicraces diagnostics` should show every owned map at zero after
   all players leave.
5. Apotheosis runtime (no profile includes it); existing-world upgrade from 1.7.1.

**Rollback.** No save data or resource ids changed. Downgrading to 1.7.1 requires reverting the
client and server together (protocol 4).
