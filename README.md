# Runic Races

A flagship Origins add-on for the **Runecraft** modpack. 37 deeply designed races across 7 families, each with exactly one active ability, one passive strength, and one weakness — plus environmental interactions and deep integration with Runecraft's mod ecosystem. Pick a **family** first, then a **race** within it. With Pehkui installed, each race also has its own **height** (0.45–1.30), clustered by family so a party reads at a glance.

**Minecraft 1.20.1 | Forge | Requires Origins Forge**

## Races

### Human (4) — adaptable generalists
| Race | Active | Passive / Weakness |
|------|--------|--------------------|
| **Primian** | Stroke of Fortune | Luck + adaptability / no specialization |
| **Celeron** | Messenger's Dash | speed & wit / fragile frame |
| **Magi** | Arcane Overflow | born of magic / frail body |
| **Valen** | Unbreakable Stand | armored bulwark / slow |

### Elven (5) — arcane grace, frail bodies
| Race | Active | Passive / Weakness |
|------|--------|--------------------|
| **High Elf** | Arcane Reflex | arcane mastery / physical frailty |
| **Dark Elf** | Shadowmeld | night power / sunlight |
| **Moon Elf** | Moonlit Veil | moon/tide magic / waning by day |
| **Blood Elf** | Blood Frenzy | bloodcraft lifesteal / reduced healing |
| **Ice Elf** | Frostbind | frost mastery / fire |

### Dwarven (6) — tough, subterranean
| Race | Active | Passive / Weakness |
|------|--------|--------------------|
| **Deep One** | Tremorsense | darkvision miner / sunlight |
| **Forge One** | Forge Blessing | fire-forged smith / heavy in water |
| **Frost One** | Glacial Resolve | cold-immune / fire |
| **Iron One** | Shield Wall | fortress tank / slow & magic-poor |
| **Sky One** | Mountain Leap | sure-footed climber / claustrophobia |
| **Runic One** | Rune of Warding | rune-smith party support / scholarly frailty |

### Bestial (6) — sharp senses, predatory
| Race | Active | Passive / Weakness |
|------|--------|--------------------|
| **Arachnid** | Web Snare | venom & vibration sense / fire |
| **Avian** | Wind Burst | feathered flight / hollow bones |
| **Canine** | Howl of the Pack | pack hunter / ravenous |
| **Feline** | Pounce | **Nine Lives** / hydrophobia |
| **Kitsune** | Foxfire Illusion | fox-spirit magic / spirit-frail |
| **Serpen** | Shed Skin | venom & heat / cold-blooded |

### Faeborne (5) — magic, illusion, often winged
| Race | Active | Passive / Weakness |
|------|--------|--------------------|
| **Changeling** | Mirror Shift | many faces / hollow identity |
| **Dryad** | Verdant Bloom | grove healer / 3× fire |
| **Sprite** | Phase Shift | gossamer flight / glass cannon |
| **Nymph** | Siren's Charm | water spirit / bound to water |
| **Faerie** | Faerie Bargain | pixie glamour / cold iron |

### Undead (5) — undeath immunities, night power
| Race | Active | Passive / Weakness |
|------|--------|--------------------|
| **Zombie** | Undying Hunger | deathless flesh / sunlight decay |
| **Skeleton** | Conscript the Dead | grave-servant summoner / brittle bones |
| **Wraith** | Spectral Phase | soul-touched / sunlight & holy |
| **Demon** | Infernal Wrath | infernal fire / holy & water |
| **Reaper** | Soul Harvest | **death revival** / −50% healing |

### Draconic (6) — elemental breath, scales, flight
| Race | Active | Passive / Weakness |
|------|--------|--------------------|
| **Fire Drake** | Dragonfire Breath | fire immune / cold & water |
| **Ice Drake** | Frost Breath | freeze immune / fire |
| **Sea Serpen** | Tidal Breath | ocean leviathan / landbound |
| **Terra Drake** | Seismic Breath | living stone / ponderous |
| **Volt Drake** | Lightning Breath | storm speed / grounded & wet |
| **Wind Wyrm** | Galeforce Breath | supreme flyer / caged underground |

## Mod Integrations

All integrations are optional and config-toggleable. If a mod is absent, its features are silently skipped.

| Mod | Integration |
|-----|-------------|
| **Ars Nouveau** | Casters: −15% Source cost, +20% max mana. Iron One: +15% cost |
| **Iron's Spellbooks** | Per-race spell damage modifiers (Magi +15%) |
| **Curios** | Elven +necklace, Dwarven +belt, Faeborne +ring, Undead +charm |
| **Apotheosis** | Luck modifiers (Primian/Faerie +1, Wind Wyrm −2) |

## Installation

1. Requires **Origins Forge** (1.10.0.9+) installed in the modpack
2. Drop `runic_races-1.2.0.jar` into the `mods/` folder (required on **both** client and server)
3. **Remove** the old KubeJS Runic Races datapack if present (`kubejs/data/runic_races/`)
4. Existing players with preserved race names will keep their selection

## Configuration

Config files are generated in `config/runic_races/`:

- `runic_races-server.toml` — Integration toggles, resource gating posture (`failClosedWhenResourceModMissing` defaults to **true** for predictable standalone play; flip to `false` inside a pack that guarantees Iron's Spellbooks / Feather's are present).
- `runic_races-client.toml` — Racial HUD (anchor, offset, scale, opacity, minimal mode, ability names, ready glow), ambient state effects (passive state particles, screen cues), and state-rune pulse.
- `runic_races-common.toml` — Debug logging; **`disableDefaultOriginLayer`** (see Origins Compatibility below).

## Origins Compatibility

By default Runic Races shows only its two-layer **Family → Race** selection and hides the
vanilla `origins:origin` layer — which is where most other Origins add-ons register their
origins, so they don't appear. If that happens, the server log warns at startup with the
full list of hidden origins and the config remedy, so the overlap is never silent.

### Let other origin mods coexist

Set `disableDefaultOriginLayer = false` in `runic_races-common.toml`. Other add-ons' origins
then reappear as the standard Origins screen alongside Family/Race. This is shipped as a built-in
datapack toggled by the config, so the change is read at **world load** — quit to title and rejoin
(or restart the server) for it to take effect; `/reload` alone won't pick it up.

### Add origins into a Runic Races family

Origins merges layer membership across datapacks, so a datapack can append origins (from any mod,
or your own) into a family's race list with `"replace": false`. See the ready-to-edit template at
[`examples/datapacks/runic_races_extra_races/`](examples/datapacks/runic_races_extra_races/).
Note: an injected foreign origin keeps its own powers but isn't treated as a native race (no Runic
Races HUD/notifications/scale/Curios slots).

## Keybinds

Runic Races adds a **Runic Races** category to the vanilla Controls screen:

- **Flap Wings** *(unbound by default)* — while gliding, press to flap upward.
- **Fold Wings** *(unbound by default)* — while gliding, press to cancel the glide.

If you leave both unbound, the legacy fallback stays active: while gliding, press **Jump** to flap and **double-tap Jump** within 4 ticks to fold your wings.

## Commands

- `/runicraces info [player]` — Show selected race
- `/runicraces list` — List all 37 races by family
- `/runicraces debug` — Show current attribute values
- `/runicraces validate` — (op) Cross-check the race registry against loaded origins/powers — run after editing datapacks
- `/runicraces state <player>` — (op) Dump a player's effective race state (flags, cooldowns, adaptation stacks)

## Design Philosophy

- **Race = what you ARE** (permanent identity, not progression)
- Racial traits are static and don't scale — prevents competing with Skills/Gods
- Drawbacks pass the "of course" test — every penalty is narratively justified
- Integration through **affinity, not gating** — race makes paths easier, never blocks them
- Every race changes **how** you play, not just **how well** you play

## Building

```bash
# Place the compile-only dependency jars in Dependencies/ (gitignored), then:
./gradlew build
# Output: build/libs/runic_races-1.2.0.jar
```

**Required:** `origins-forge`, `apoli-forge`, `calio-forge`. Apoli and Calio ship *inside*
the Origins `-all` jar under `META-INF/jarjar/` — extract them:
`unzip -o -j Dependencies/origins-forge-*-all.jar 'META-INF/jarjar/apoli-forge-*.jar' 'META-INF/jarjar/calio-forge-*.jar' -d Dependencies/`.

**Optional** (only needed to compile their integration classes): `ars_nouveau`, `irons_spellbooks`,
`curios` (local jars from Modrinth). **Apotheosis**, **Pehkui**, and **Feathers** resolve from
Maven automatically (CurseMaven / Modrinth) — no local jar needed.
