# Runic Races — Balance Reference

## Power Budget per Race

Each race nets **≈ 0** when summing benefits and drawbacks. Every race has exactly
three powers: **one active** (cooldown-gated signature), **one passive positive**, and
**one weakness** (passive negative, usually a conditional vulnerability). `impact` (1–3)
rates intensity/uniqueness, not raw strength.

This is a design-side reference. The power JSON under
`src/main/resources/data/runic_races/powers/` is authoritative.

### Human — gold · generalists, fortune, versatility
| Race | Active | Passive | Weakness |
|------|--------|---------|----------|
| Primian | Stroke of Fortune (Luck/Abs/Speed) | +1♥, +1 luck, +5% spd, Adaptation stacks (new biomes/varied kills), Universal Palate (no food debuffs) | −10% magic, −5% melee |
| Celeron | Messenger's Dash (Speed III) | +12% spd, +10% atk spd | −2♥, 1.3× knockback taken |
| Magi | Arcane Overflow (AoE nova) | +15% magic | −2♥, +20% physical taken |
| Valen | Unbreakable Stand (Res III) | +2♥, +2 armor, KB resist, +10% melee, sprint shoulder-check (once per sprint, hostiles only) | −8% spd, −8% atk spd |

### Elven — magenta · arcane grace, frail bodies
| Race | Active | Passive | Weakness |
|------|--------|---------|----------|
| High Elf | Arcane Reflex (Abs+Res) | +15% magic, night vision | −2♥, +15% physical taken |
| Dark Elf | Shadowmeld (invis+spd) | night vision, +10% spd, +night dmg | −1♥, sun-weakened |
| Moon Elf | Moonlit Veil (invis/heal) | water breathing, night vision, night regeneration | −1.5♥, −10% magic, day-weak |
| Blood Elf | Blood Frenzy (Str/Speed/Regen) | +10% melee, +10% magic, 20% melee lifesteal | −1.5♥, −30% healing (taxes the lifesteal too) |
| Ice Elf | Frostbind (AoE Slow III) | freeze-damage immune, +10% magic | +25% fire taken |

### Dwarven — slate · tough, subterranean, slow
| Race | Active | Passive | Weakness |
|------|--------|---------|----------|
| Deep One | Tremorsense | night vision, +2 armor, mining | −5% spd, sun-dazzled (slow/weak in sun) |
| Forge One | Forge Blessing (Str/fire res) | 50% fire res, +2 armor, +10% melee, crafting proc (25% Unbreaking, 10-min CD) | −10% spd/atk, water-slow |
| Frost One | Glacial Resolve (Res/fire res) | freeze-damage immune, +2♥, +1 armor, cold-home | +25% fire, −5% spd |
| Iron One | Shield Wall (Res III/Abs) | +2♥, +3 armor, KB resist | −10% spd/atk, −10% magic |
| Sky One | Mountain Leap | no fall dmg, mountain-home, +1 armor | −1♥, cave claustrophobia |
| Runic One | Rune of Warding (self ward + slows hostiles) | +10% magic, +2 armor, +1 luck, crafting proc (15% Unbreaking I) | −2♥, −10% spd |

### Bestial — green · senses + agility, predatory
| Race | Active | Passive | Weakness |
|------|--------|---------|----------|
| Arachnid | Web Snare (trap + AoE slow) | poison immune, venomous melee (Poison I), no fall, sense | −1.5♥, +20% fire |
| Avian | Wind Burst | wings/glide, no fall, +10% spd, night vision | −2♥, 1.4× knockback taken, −10% melee |
| Canine | Howl of the Pack | scent, +12% spd, +10% melee, forest/taiga-home | +25% hunger, −2 armor |
| Feline | Pounce | Nine Lives (15 min, 5s Weakness on proc), night vision, +15% atk spd, no fall | −1.5♥, +30% damage while submerged |
| Kitsune | Foxfire Illusion | +15% magic, night vision, +10% spd | −2♥, +20% taken in daylight |
| Serpen | Shed Skin (cleanse, 45s CD) | venomous melee (Poison I), poison immune, hot-home | −1.5♥, +25% freeze |

### Faeborne — teal · magic + illusion, fragile, often winged
| Race | Active | Passive | Weakness |
|------|--------|---------|----------|
| Changeling | Mirror Shift (25s) | +10% spd, +1 luck | −1♥, −5% attack damage |
| Dryad | Verdant Bloom (heal/root) | poison immune, forest, sun-heal | 3× fire damage |
| Sprite | Phase Shift (blink) | wings, +20% spd, +10% atk spd | −3♥, 1.5× knockback taken |
| Nymph | Siren's Charm (pacify) | water breathing, +10% magic, water-biome home | −1.5♥, +20% fire, hot-biome slow |
| Faerie | Faerie Bargain (glamour) | wings, +15% magic, +15% spd, night vision | −2.5♥, +20% physical, 1.4× knockback taken |

### Undead — purple · undeath immunities, night power
| Race | Active | Passive | Weakness |
|------|--------|---------|----------|
| Zombie | Undying Hunger | +3♥, poison/hunger immune, KB resist | sun decay, −10% spd, −15% heal |
| Skeleton | Conscript the Dead (2 Grave Servants) | +10% atk spd, poison/hunger immune, water breathing | −2♥, +sun/fall damage |
| Wraith | Spectral Phase (phase + drain heal) | +night dmg, no fall, night vision | −2.5♥, sun-weak, +holy |
| Demon | Infernal Wrath (hellfire) | fire immune, +15% melee, hot-biome home | +25% holy, +20% water, −25% heal |
| Reaper | Soul Harvest (reap) | revival (30-min CD, consumed only on success), wither immune, +15% melee | −2♥, −50% healing, sun-weak |

### Draconic — red · elemental breath + scales + flight
| Race | Active | Passive | Weakness |
|------|--------|---------|----------|
| Fire Drake | Dragonfire Breath | fire immune, +3 armor, wings, hot | +35% cold/water, +hunger |
| Ice Drake | Frost Breath | freeze immune, +3 armor, wings, cold | +30% fire, +hunger |
| Sea Serpen | Tidal Breath | water breathing, +3 armor, ocean | +20% fire, dry-weak, −1♥ |
| Terra Drake | Seismic Breath | +4 armor, KB immune, mining, wings | −10% spd/atk, +hunger |
| Volt Drake | Lightning Breath | lightning immune, storm-charged (+10% atk spd in rain), +15% spd, wings | −1.5♥, +25% wet, sky-dependent |
| Wind Wyrm | Galeforce Breath | best wings, slow-fall drift, +15% spd, no fall | −2♥, cave-crippled |

## Integration Modifiers
- **Ars Nouveau** mana: best Magi/High Elf/Sprite/Faerie (+20%); worst Iron One (−15%), dwarven/draconic (−10%).
- **Iron's Spellbooks** spell damage: Magi +15%, casters +5–10%; Iron One −10%, Valen −10%.
- **Apotheosis** luck (in RaceRegistry): best Primian/Faerie (+1.0); worst Wind Wyrm (−2.0); fire/ice/terra drakes, demon, reaper (−1.0); sea serpen/volt drake (−0.5).
- **Curios** slots: Elven +necklace, Dwarven +belt, Faeborne +ring, Undead +charm.
- **Pehkui** heights: per-race `scale` in `RaceRegistry`, clustered by family — 0.45 (Sprite) / 0.50 (Faerie) → 1.30 (Terra Drake); dwarves ~0.70, elves ~1.06, drakes 1.10–1.30. Cosmetic + hitbox; jump height is auto-compensated for small races.

## Balance Principles
1. **Zero-sum within each race**: every benefit is offset by a comparable drawback.
2. **No universally best race**: each excels at some content and struggles with other.
3. **Diminishing racial advantage**: race matters most early; Skills/Gods/Gear dominate endgame.
4. **Counterplay for all drawbacks**: fire weakness → fire-res gear; sun weakness → helmets/underground; cave weakness → fight in the open.
5. **Multiplayer complementarity**: parties benefit from family diversity.
