# Runic Races

A flagship Origins add-on for the **Runecraft** modpack. 24 deeply designed races across 6 families, each with unique mechanics, environmental interactions, and deep integration with Runecraft's mod ecosystem.

**Minecraft 1.20.1 | Forge | Requires Origins Forge**

## Races

### Mortal (4)
| Race | Fantasy | Signature Mechanic |
|------|---------|-------------------|
| **Human** | Adaptable generalist | +1 Luck, +5% speed, +1 heart, Determination low-HP surge |
| **Halfling** | Lucky rogue | Lucky Dodge (10% evade), Lightfoot stealth burst |
| **Nomad** | Desert survivor | Pathfinder's Mark dash, desert biome affinity |
| **Giant-Blooded** | Towering tank | Massive reach, knockback immunity, slow bulk |

### Fae (5)
| Race | Fantasy | Signature Mechanic |
|------|---------|-------------------|
| **High Elf** | Arcane master | Arcane reflex, caster affinity, iron weakness |
| **Wood Elf** | Forest ranger | Canopy Meld stealth, forest biome affinity, fire vulnerability |
| **Sprite** | Glass cannon | Permanent gliding, +30% speed, -3 hearts |
| **Changeling** | Shapeshifter | Assume Form invisibility burst, gods distrust (-15% favor) |
| **Dryad** | Plant support | Photosynthesis healing, player heal burst, 3x fire damage |

### Beast (5)
| Race | Fantasy | Signature Mechanic |
|------|---------|-------------------|
| **Wolfkin** | Pack hunter | Pack Howl speed buff, pursuit mobility |
| **Dragonborn** | Fire-blooded bruiser | Dragon Breath AOE, natural armor |
| **Catfolk** | Parkour assassin | **Nine Lives** (survive lethal blow, 10min CD), water aversion |
| **Minotaur** | Momentum fighter | Bullrush melee power, Labyrinthine Sense |
| **Serpentfolk** | Ambush predator | Venomstrike poison on hit, Shed Skin debuff removal |

### Underfolk (5)
| Race | Fantasy | Signature Mechanic |
|------|---------|-------------------|
| **Mountain Dwarf** | Forgemaster | **Forge Blessing** (crafting bonus), darkvision, magic cap |
| **Deep Dwarf** | Abyssal explorer | Mining Fatigue immune, Tremorsense, sunlight burns |
| **Goblin** | Treasure hunter | +2 Curios slots, +2 Apotheosis luck, frail frame |
| **Troll** | Unkillable tank | Permanent Regen I, 2x fire damage |
| **Kobold** | Trapmaster | Improvised Trap placement, stronger in tight spaces |

### Dragon (2)
| Race | Fantasy | Signature Mechanic |
|------|---------|-------------------|
| **Wyvern-Blooded** | Aerial hunter | Updraft soar, permanent glide, claustrophobia |
| **Elder Drake** | Ancient power | Fire immune, powerful roar, ancient pride penalties |

### Cursed (3)
| Race | Fantasy | Signature Mechanic |
|------|---------|-------------------|
| **Vampire** | Nocturnal predator | Day/night power scaling, frenzy, sunlight kills |
| **Lycanthrope** | Involuntary werewolf | Beast surge on low HP (buffs with reduced control) |
| **Revenant** | Undying returned | **Death-site revival** (respawn where you died), healing halved |

## Mod Integrations

All integrations are optional and config-toggleable. If a mod is absent, its features are silently skipped.

| Mod | Integration |
|-----|-------------|
| **Runic Skills** | Starting skill bonuses per race (e.g. Dwarf: +2 Building) |
| **Runic Gods** | Divine affinity (+20% favor for aligned gods), Vampire/Viren forbidden |
| **Ars Nouveau** | Fae: -15% Source cost, +20% max mana. Trolls: +30% cost |
| **Iron's Spellbooks** | Per-race spell damage modifiers |
| **Curios** | Goblin +ring/+charm, Dwarves +belt, Elves +necklace |
| **Apotheosis** | Luck modifiers (Goblin +2, Elder Drake -2) |

## Installation

1. Requires **Origins Forge** (1.10.0.9+) installed in the modpack
2. Drop `runic_races-0.9.0.jar` into the `mods/` folder
3. **Remove** the old KubeJS Runic Races datapack if present (`kubejs/data/runic_races/`)
4. Existing players with preserved race names will keep their selection

## Configuration

Config files are generated in `config/runic_races/`:

- `runic_races-server.toml` — Integration toggles, resource gating posture (`failClosedWhenResourceModMissing` defaults to **true** for predictable standalone play; flip to `false` inside a pack that guarantees Iron's Spellbooks / Feather's are present).
- `runic_races-client.toml` — Racial HUD (anchor, offset, scale, opacity, minimal mode, ability names, ready glow) and ambient state effects (passive state particles, screen cues).
- `runic_races-common.toml` — Debug logging.

## Keybinds

Runic Races adds a **Runic Races** category to the vanilla Controls screen:

- **Flap Wings** *(unbound by default)* — while gliding, press to flap upward.
- **Fold Wings** *(unbound by default)* — while gliding, press to cancel the glide.

If you leave both unbound, the legacy fallback stays active: while gliding, press **Jump** to flap and **double-tap Jump** within 4 ticks to fold your wings.

## Commands

- `/runicraces info [player]` — Show selected race
- `/runicraces list` — List all 24 races by family
- `/runicraces debug` — Show current attribute values

## Design Philosophy

- **Race = what you ARE** (permanent identity, not progression)
- Racial traits are static and don't scale — prevents competing with Skills/Gods
- Drawbacks pass the "of course" test — every penalty is narratively justified
- Integration through **affinity, not gating** — race makes paths easier, never blocks them
- Every race changes **how** you play, not just **how well** you play

## Building

```bash
# Requires Origins, Apoli, Calio jars in Dependencies/
./gradlew build
# Output: build/libs/runic_races-0.9.0.jar
```
