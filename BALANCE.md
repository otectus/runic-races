# Runic Races — Balance Reference

## Power Budget per Race

Each race has a total power budget of roughly **0** when summing benefits and drawbacks.
Positive numbers = benefit, negative = drawback.

This is a design-side reference, not a line-by-line dump of the live JSON. When in doubt, treat the power files under `src/main/resources/data/runic_races/powers/` as authoritative.

| Race | HP | Speed | Armor | Damage | Utility | Drawback Summary | Net |
|------|-----|-------|-------|--------|---------|------------------|-----|
| Human | +1♥ | +5% | — | — | +1 luck, Determination (30% HP → Abs III + Regen I, 6s) | No active, no specialization | ~0 |
| Halfling | — | +10% | — | — | Lucky Dodge, stealth | +25% hunger | ~0 |
| Nomad | — | +10% | +1 | — | Pathfinding dash | Cold slow, armor penalty | ~0 |
| Giant-Blooded | +2♥ | -15% | — | — | KB immune, reach | Can't stealth, +25% hunger | ~0 |
| High Elf | -2♥ | — | — | +15% magic | Night vision, Arcane Reflex | Iron weakness, +15% phys taken | ~0 |
| Wood Elf | — | +15% | — | — | Canopy Meld, regen | +25% fire, underground slow | ~0 |
| Sprite | -3♥ | +30% | — | +15% atk spd | Slow falling, Phase Shift (90s CD) | 2x knockback | ~0 |
| Changeling | -1♥ | +10% | — | — | Invisibility, mirror | -0.5 luck, -15% god favor | ~0 |
| Dryad | — | -5% | — | — | Sun heal, poison immune | 3x fire, darkness penalty | ~0 |
| Wolfkin | — | +15% | -2 | — | Night vision, pack buff | +20% phys taken, no invis | ~0 |
| Dragonborn | -2♥ | — | +4 | -10% atk spd | Dragon Breath, -50% fire | +25% hunger | ~0 |
| Catfolk | -2♥ | — | — | +15% atk spd | Nine Lives, night vision | +30% water dmg | ~0 |
| Minotaur | +2♥ net | -5% | — | +25% melee | Labyrinthine Sense | +15% hunger | ~0 |
| Serpentfolk | -1♥ | +10% | — | — | Guaranteed poison on hit, Shed Skin | +50% freeze, +15% fire | ~0 |
| Mtn Dwarf | +2♥ | -5% | +2 | — | Darkvision, Forge Blessing | Magic cap -4, +15% hunger | ~0 |
| Deep Dwarf | +2♥ | -5% | — | — | Tremorsense, Mine Fatigue imm | Sunlight damage | ~0 |
| Goblin | -2♥ | +10% | — | +20% atk spd | +2 Curio slots, treasure sense | +15% damage taken | ~0 |
| Troll | +4♥ | -10% | +2 | -15% atk spd | Regen I (above 50% HP), Wither imm | 2x fire, -30% magic dmg | ~0 |
| Kobold | -3♥ | +15% | +1 | +1 flat | Traps, trap detection | +20% dmg in open, sun penalty | ~0 |
| Wyvern | -2♥ | +10% | — | — | Updraft dive-bomb, glide | Underground penalties | ~0 |
| Elder Drake | +3♥ | -10% | +3 | — | Fire immune, Roar, ancient wrath | -25% XP, +20% hunger, -2 luck | ~0 |
| Vampire | -2♥ | +15% | — | +20% night | Mesmerize, Blood Frenzy | Sun damage, +30% magic taken | ~0 |
| Lycanthrope | -1♥ | +10% | — | +1 flat | Beast form (+massive buffs) | Involuntary, +20% phys taken | ~0 |
| Revenant | -1♥ | — | +2 | — | Death revival, Wither imm, Spite (25% HP → Str II + Res I) | -50% healing, sunlight decay | ~0 |

## Integration Modifiers

### Runic Skills Starting Bonuses (max +3 total levels)
- Largest: Mountain Dwarf (+2 Building, +1 Endurance), Elder Drake (+2 Str, +1 Con)
- Smallest: Human (+1 to choice), Wood Elf (+1 Dex)

### Ars Nouveau Mana Modifiers
- Best: High Elf (+20% max, -15% cost), Sprite (+20% max)
- Worst: Troll (-15% max, +30% cost), Elder Drake (-10% max, +20% cost)

### Divine Affinity (Runic Gods)
- Strongest: Vampire+Mortyss (+20%), Mountain Dwarf+Aurex (+20%)
- Forbidden: Vampire+Viren (0x — blocked)
- Family penalty: Cursed + light gods (-15%)

### Apotheosis Luck
- Best: Goblin (+2.0), Halfling (+1.5)
- Worst: Elder Drake (-2.0), Troll (-1.0)

### Curios Extra Slots
- Goblin: +1 ring, +1 charm
- Dwarves: +1 belt
- Elves: +1 necklace

## Balance Principles

1. **Zero-sum within each race**: Every benefit is offset by a drawback of comparable impact
2. **No universally best race**: Every race has content it excels at and content it struggles with
3. **Diminishing racial advantage**: Race bonuses matter most early game; Skills/Gods/Gear dominate endgame
4. **Counterplay exists for all drawbacks**: Fire weakness → fire resistance gear. Sun weakness → helmets + underground play
5. **Multiplayer complementarity**: No single race handles all content efficiently; parties benefit from diversity
