# Runic Races

A flagship Origins add-on for the **Runecraft** modpack. 24 deeply designed races across 6 families, each with unique mechanics, environmental interactions, and deep integration with Runecraft's mod ecosystem.

**Minecraft 1.20.1 | Forge | Requires Origins Forge**

## Races

### Mortal (4)
| Race | Fantasy | Signature Mechanic |
|------|---------|-------------------|
| **Human** | Adaptable generalist | +10% XP, no weaknesses, no specialization |
| **Halfling** | Lucky rogue | Lucky Dodge (10% evade), can't use two-handed weapons |
| **Nomad** | Desert survivor | Pathfinder's Mark teleport, desert biome affinity |
| **Giant-Blooded** | Towering tank | Earthshaker fall AOE, full knockback immunity, slow |

### Fae (5)
| Race | Fantasy | Signature Mechanic |
|------|---------|-------------------|
| **High Elf** | Arcane master | Passive Source regen, +15% Ars cost reduction, iron weakness |
| **Wood Elf** | Forest ranger | Canopy Meld stealth, forest biome affinity, fire vulnerability |
| **Sprite** | Glass cannon | Permanent gliding, +30% speed, -3 hearts, no chest/legs |
| **Changeling** | Shapeshifter | Assume Form disguise, gods distrust (-15% favor) |
| **Dryad** | Plant support | Photosynthesis healing, ally heal aura, 3x fire damage |

### Beast (5)
| Race | Fantasy | Signature Mechanic |
|------|---------|-------------------|
| **Wolfkin** | Pack hunter | Pack Howl ally buff, hostile detection through walls |
| **Dragonborn** | Elemental bruiser | Dragon Breath AOE, natural armor, can't wear helmets |
| **Catfolk** | Parkour assassin | **Nine Lives** (survive lethal blow, 10min CD), no heavy armor |
| **Minotaur** | Momentum fighter | Bullrush sprint damage, Labyrinthine Sense, can't use bows |
| **Serpentfolk** | Ambush predator | Venomstrike poison on hit, Shed Skin debuff removal |

### Underfolk (5)
| Race | Fantasy | Signature Mechanic |
|------|---------|-------------------|
| **Mountain Dwarf** | Forgemaster | **Forge Blessing** (crafting bonus), darkvision, magic cap |
| **Deep Dwarf** | Abyssal explorer | Mining Fatigue immune, Tremorsense, sunlight burns |
| **Goblin** | Treasure hunter | +2 Curios slots, +2 Apotheosis luck, fragile alone |
| **Troll** | Unkillable tank | Permanent Regen I, 2x fire damage, can't enchant |
| **Kobold** | Trapmaster | Improvised Trap placement, stronger in tight spaces |

### Dragon (2)
| Race | Fantasy | Signature Mechanic |
|------|---------|-------------------|
| **Wyvern-Blooded** | Aerial hunter | Updraft dive-bomb, permanent glide, claustrophobia |
| **Elder Drake** | Ancient power | Fire immune, +6 HP, -25% XP gain, scorns Apotheosis affixes |

### Cursed (3)
| Race | Fantasy | Signature Mechanic |
|------|---------|-------------------|
| **Vampire** | Nocturnal predator | Day/night power scaling, lifesteal, sunlight kills, blood diet |
| **Lycanthrope** | Involuntary werewolf | Beast form on low HP (buffs but locks inventory/spells) |
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
2. Drop `runic_races-1.0.0.jar` into the `mods/` folder
3. **Remove** the old KubeJS Runic Races datapack if present (`kubejs/data/runic_races/`)
4. Existing players with preserved race names will keep their selection

## Configuration

Config files are generated in `config/runic_races/`:

- `runic_races-server.toml` — Balance multipliers (cooldown, damage, healing) and integration toggles
- `runic_races-common.toml` — Disabled races list, debug logging

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
# Output: build/libs/runic_races-1.0.0.jar
```
