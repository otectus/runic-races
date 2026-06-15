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
| Primian | Stroke of Fortune (Luck/Abs/Speed) | +1♥, +1 luck, +5% spd, Adaptation | −10% magic, −5% melee |
| Celeron | Messenger's Dash (Speed III) | +12% spd, +10% atk spd | −2♥, +knockback |
| Magi | Arcane Overflow (AoE nova) | +15% magic | −2♥, +20% physical taken |
| Valen | Unbreakable Stand (Res III) | +2♥, +2 armor, KB resist, +10% melee | −10% spd, −10% atk spd |

### Elven — magenta · arcane grace, frail bodies
| Race | Active | Passive | Weakness |
|------|--------|---------|----------|
| High Elf | Arcane Reflex (Abs+Res) | +15% magic, night vision | −2♥, +15% physical taken |
| Dark Elf | Shadowmeld (invis+spd) | night vision, +10% spd, +night dmg | −1♥, sun-weakened |
| Moon Elf | Moonlit Veil (invis/heal) | water breathing, night vision | −1.5♥, −10% magic, day-weak |
| Blood Elf | Blood Frenzy (Str/lifesteal) | +10% melee, +10% magic | −1.5♥, −30% healing |
| Ice Elf | Frostbind (AoE Slow III) | freeze immune, +10% magic/ranged | +25% fire taken |

### Dwarven — slate · tough, subterranean, slow
| Race | Active | Passive | Weakness |
|------|--------|---------|----------|
| Deep One | Tremorsense | night vision, +2 armor, mining | −5% spd, sun-blinded |
| Forge One | Forge Blessing (Str/fire res) | 50% fire res, +2 armor, +10% melee | −10% spd/atk, water-slow |
| Frost One | Glacial Resolve (Res/fire res) | freeze immune, +2♥, cold-home | +25% fire, −5% spd |
| Iron One | Shield Wall (Res III/Abs) | +2♥, +3 armor, KB resist | −10% spd/atk, −10% magic |
| Sky One | Mountain Leap | no fall dmg, mountain-home, +1 armor | −1♥, cave claustrophobia |
| Runic One | Rune of Warding (party ward) | +10% magic, +2 armor, +1 luck | −2♥, −10% spd |

### Bestial — green · senses + agility, predatory
| Race | Active | Passive | Weakness |
|------|--------|---------|----------|
| Arachnid | Web Snare (root) | poison immune, venom, no fall, sense | −1.5♥, +20% fire |
| Avian | Wind Burst | wings/glide, no fall, +10% spd | −2♥, +KB, −10% melee |
| Canine | Howl of the Pack | scent, +12% spd, +10% melee, forest | +25% hunger, −2 armor |
| Feline | Pounce | Nine Lives, night vision, +15% atk | −1.5♥, +30% water dmg |
| Kitsune | Foxfire Illusion | +15% magic, night vision, +10% spd | −2♥, +15% physical |
| Serpen | Shed Skin (cleanse) | venom, poison immune, hot-home | −1.5♥, +25% cold |

### Faeborne — teal · magic + illusion, fragile, often winged
| Race | Active | Passive | Weakness |
|------|--------|---------|----------|
| Changeling | Mirror Shift | +10% spd, +1 luck | −1♥, −0.5 luck |
| Dryad | Verdant Bloom (heal/root) | poison immune, forest, sun-heal | 3× fire damage |
| Sprite | Phase Shift (blink) | wings, +30% spd, +15% atk | −3♥, 2× knockback |
| Nymph | Siren's Charm (pacify) | water breathing, +10% magic, water | −1.5♥, +20% fire, dry-weak |
| Faerie | Faerie Bargain (glamour) | wings, +15% magic, +20% spd | −2.5♥, +20% physical |

### Undead — purple · undeath immunities, night power
| Race | Active | Passive | Weakness |
|------|--------|---------|----------|
| Zombie | Undying Hunger | +3♥, poison/hunger immune, KB resist | sun decay, −10% spd, −25% heal |
| Skeleton | Conscript the Dead (summon) | +10% atk, poison/hunger immune | −2♥, +sun/fall damage |
| Wraith | Spectral Phase (drain) | +night dmg, life drain, no fall | −2.5♥, sun-weak, +holy |
| Demon | Infernal Wrath (hellfire) | fire immune, +15% melee, nether | +25% holy, +20% water, −25% heal |
| Reaper | Soul Harvest (reap) | revival, wither immune, +15% melee | −2♥, −50% healing, sun-weak |

### Draconic — red · elemental breath + scales + flight
| Race | Active | Passive | Weakness |
|------|--------|---------|----------|
| Fire Drake | Dragonfire Breath | fire immune, +3 armor, wings, hot | +30% cold/water, +hunger |
| Ice Drake | Frost Breath | freeze immune, +3 armor, wings, cold | +30% fire, +hunger |
| Sea Serpen | Tidal Breath | water breathing, +3 armor, ocean | +20% fire, dry-weak, −1♥ |
| Terra Drake | Seismic Breath | +4 armor, KB immune, mining, wings | −10% spd/atk, +hunger |
| Volt Drake | Lightning Breath | lightning immune, +15% spd, wings | −1.5♥, +25% wet, sky-dependent |
| Wind Wyrm | Galeforce Breath | best wings, +15% spd, no fall | −2♥, cave-crippled |

## Integration Modifiers
- **Ars Nouveau** mana: best Magi/High Elf/Sprite/Faerie (+20%); worst Iron One (−15%), dwarven/draconic (−10%).
- **Iron's Spellbooks** spell damage: Magi +15%, casters +5–10%; Iron One −15%, Valen −10%.
- **Apotheosis** luck (in RaceRegistry): best Primian/Faerie (+1.0); worst Wind Wyrm (−2.0), drakes/demon/reaper (−1.0).
- **Curios** slots: Elven +necklace, Dwarven +belt, Faeborne +ring, Undead +charm.
- **Pehkui** heights: per-race `scale` in `RaceRegistry`, clustered by family — 0.45 (Sprite/Faerie) → 1.30 (Terra Drake); dwarves ~0.70, elves ~1.06, drakes 1.10–1.30. Cosmetic + hitbox; jump height is auto-compensated for small races.

## Balance Principles
1. **Zero-sum within each race**: every benefit is offset by a comparable drawback.
2. **No universally best race**: each excels at some content and struggles with other.
3. **Diminishing racial advantage**: race matters most early; Skills/Gods/Gear dominate endgame.
4. **Counterplay for all drawbacks**: fire weakness → fire-res gear; sun weakness → helmets/underground; cave weakness → fight in the open.
5. **Multiplayer complementarity**: parties benefit from family diversity.
