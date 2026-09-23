# Wings and ability presentation

This pass covers all 54 activated race powers and all 10 wing mappings. It keeps
Minecraft's pixel sprites and block geometry. Damage, cooldowns, targeting and
movement tuning retain their existing behavior.

## Wings

The shared model uses a 64 × 64 atlas with one texel per model unit. Membrane wings
have raised spars, finger bones, knuckles and claws; Avian wings have separate,
overlapping flight feathers and coverts; gossamer wings have stepped fore/hind
lobes, raised veins and independent splay. Existing flap, glide, fold, bank and
reduced-motion controls still drive the articulated model.

| Races | Surface treatment |
|---|---|
| Sprite, Faerie | Translucent jewel palettes, branching veins and bright pixel edges |
| Zephyr | Open wind ribbons in pale teal |
| Avian | Banded brown/gold feathers with pale stepped tips |
| Wind Wyrm, Wyvernkin | Green or olive membranes with contrasting ribs |
| Fire Drake | Charcoal/red membranes, orange heat fissures |
| Ice Drake | Blue membranes and angular pale frost markings |
| Terra Drake | Stone/earth layers and moss accents |
| Volt Drake | Purple-gray membranes and yellow lightning veins |

`tools/generate_wings.py` authors all ten sheets. The expansion art generator uses
the same function for Zephyr and Wyvernkin, so regeneration cannot restore their
old sheets. Optional `--preview` writes an atlas contact sheet for inspection;
that sheet is not an in-game screenshot.

## Activated powers and confirmed effects

Each activation has immediate feedback and subsequent impact/settle beats. The
recipe registry uses rings, helices, domes, fountains and directional cones,
plus aimed shields, forward crescents, ground sigils and traveling wave rings.
Activation recipes stay within their established authored particle budgets.

All six breath attacks additionally broadcast one cast snapshot to viewers within
64 blocks. Clients render a 16-tick directional torrent with a moving core,
elemental edge sprites and surface plumes. Fire uses actual flames, embers and
smoke; frost uses snow/rime; water uses particles that remain visible in air;
earth uses stone chips; shock uses electrical sparks; wind uses clouds/streaks.
The snapshot keeps the original aim and origin because damage is instantaneous.
Ray checks clip emission at solid surfaces. First-person emission leaves space
around the caster's eyes.

Execution feedback appears at successful hits, healed allies, ward recipients,
summon locations and trap triggers. Grove Elf's prepared arrows and successful
movement powers leave trails. Astral Elf has a fixed anchor and departure/arrival
effects; Moss One's boundary stays at the actual field center. Wailer's outward
fronts fire only after its interruptible windup succeeds. Private reveal and
deathwatch indicators remain private.

## Limits and settings

- `vfx.signatureParticleDensity` scales activation and confirmed-effect particles;
  `vfx.breathParticleDensity` scales torrents. Zero disables the respective path.
- Breath clients cap active streams at 12 and emission at 192 particles per tick.
  One caster replaces its previous stream; all streams expire after 16 ticks.
- Vanilla particle quality and `effects.heavyEffects` reduce breath detail.
  Death, lost entities, distance and world changes clear active streams.
- Confirmed effects have a per-owner tick budget. Delayed signatures are discarded
  on logout, death, dimension change or a different race.
- Network protocol 4 appends the breath packet after the existing eight packets.
  Clients and servers need matching mod builds.

## Verification

The regression suite covers all 54 activation routes, recipe budgets/staging,
wing coverage and UV layout, density scaling, vertical aim geometry and breath
packet round trips. Generator parity, language parity, a release build and the
required-dependency Forge GameTests are the automated checks for this pass.
The release build and all 77 JUnit tests passed, along with the four Python wing
checks, both generator/language parity checks and all 23 required Forge 47.2.0
GameTests. Interactive first/third-person and remote multiplayer visual review remains a
separate check; automated tests do not establish appearance or frame rate in-game.
