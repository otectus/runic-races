# Small-race jump and fall correction

## Behavior

- Server setting: `movement.smallRaceJumpCompensation`, default `true`.
- The applied racial BASE contribution receives `1 / sqrt(scale)` jump compensation.
  With vanilla gravity and drag, the native short-race sizes rise about 1.15–1.25 blocks.
  Body-only expansion races receive no jump correction. Pehkui's `scaledMotion = false`
  also disables compensation.
- A separate FALLING modifier cancels only the inverse fall-distance contribution
  from racial shrinking. It applies with either jump setting. External motion,
  jump and falling scales still compose; ordinary fall damage and racial weaknesses remain.
- The jump contribution uses the owned, Pehkui-synchronized `runic_races:race_jump`
  type. Clients and servers must both update. No server-only launch packet is sent.
- Config reload re-syncs the Pehkui adapter when either its integration switch or
  the jump switch changes. Jump-only sync does not resize, reset fall distance or
  refresh resize protection. A blocked resize uses the size actually applied.

## Automated checks

- `python3 tools/generate_races.py --check`: passed (54 races).
- `python3 tools/build_lang.py --check`: passed (704 keys).
- `RacialMovementScalesTest`: all six JUnit tests passed in a standalone Java 17
  run. Covers every native short-race size, the old oversized Sprite jump,
  toggle-off behavior, ordinary landings and real falls, external scale composition,
  Jump Boost headroom, tall/body-only baselines, and invalid scale inputs.
- Forge build and GameTest results must also be checked in CI. Local Gradle
  dependency resolution was blocked by the execution environment's proxy setup;
  the standalone calculations do not establish runtime/client correctness.
- `racialJumpsAndFallsComposeWithPehkui` covers native jump velocity without
  Pehkui; with Pehkui it checks real derived scales, the real jump method, fall
  damage, stable repeated sync and external modifiers for legacy/expansion small
  races, normal races and tall races. CI runs the Pehkui profile explicitly.

## Runtime and client checks

1. Run `./gradlew test build runGameTestServer`, then
   `./gradlew -PrrRuntime=pehkui runGameTestServer` (Forge 47.2.0 / Pehkui 3.8.2).
2. After stopping the test server, set `movement.smallRaceJumpCompensation = false`
   in that profile world's server config and repeat the Pehkui GameTests. Do not
   write Forge config values from within GameTests; autosave races the file watcher.
3. On a dedicated server with a remote client, test Deep One, Sprite, Faerie,
   Mountain One, Moss One and a normal/tall race. Repeated ordinary jumps, one-block
   ledges, slab/stair landings and a one-block step-down must not cause self-damage.
   With compensation off, legacy short races use their native shorter jumps.
4. Confirm a real four/six/ten-block drop still hurts, with and without Feather
   Falling and Jump Boost. Check honey/slime, water and Slow Falling behavior.
5. Toggle compensation and the Pehkui integration live; switch races, clear the
   origin, respawn, change dimension and reconnect. Confirm no stale jump/fall
   modifier, prediction snap or repeated resize invulnerability. Repeat a race
   change under a low ceiling, then move into room to finish the deferred resize.
6. Repeat with external BASE, MOTION, JUMP_HEIGHT and FALLING scales, and with
   Pehkui scaled-motion/scaled-fall-damage switches disabled. Check single-player
   world switching so one world's server setting does not leak into the next.
