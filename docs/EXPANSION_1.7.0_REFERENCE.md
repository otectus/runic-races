# Runic Races 1.7.0 — expansion reference

Generated from `tools/expansion_content.py`. Values are HP, blocks and seconds unless explicitly stated. These are final authored values and design comparisons; they are not a claim of measured balance. Runtime validation is recorded separately in [VALIDATION_1.7.0.md](VALIDATION_1.7.0.md).

| Race | Heritage | Scale | Feathers | Ars mana / cost | ISS damage |
|---|---|---:|---:|---|---:|
| Colossan | Human | 1.20 | 26 | 1× / 1× | 1× |
| Auroran | Human | 1.02 | 18 | 1.1× / 0.95× | 1.05× |
| Grove Elf | Elven | 1.06 | 20 | 1.1× / 0.92× | 1× |
| Tide Elf | Elven | 1.04 | 20 | 1.1× / 0.92× | 1× |
| Astral Elf | Elven | 1.07 | 16 | 1.15× / 0.9× | 1.05× |
| Mountain One | Dwarven | 0.76 | 26 | 0.9× / 1× | 1× |
| Moss One | Dwarven | 0.70 | 22 | 1× / 1× | 1× |
| Crystal One | Dwarven | 0.72 | 20 | 1.05× / 0.95× | 1.05× |
| Bovine | Bestial | 1.15 | 26 | 1× / 1× | 1× |
| Saurian | Bestial | 1.00 | 22 | 1× / 1× | 1× |
| Chelon | Bestial | 1.05 | 24 | 1× / 1× | 1× |
| Zephyr | Faeborne | 0.80 | 14 | 1.15× / 0.9× | 1× |
| Nightborn | Undead | 1.00 | 20 | 1× / 1× | 1× |
| Returned | Undead | 1.00 | 24 | 1× / 1× | 1× |
| Wailer | Undead | 0.95 | 18 | 1.1× / 0.95× | 1.05× |
| Scaleheir | Draconic | 1.10 | 24 | 0.9× / 1.1× | 1× |
| Wyvernkin | Draconic | 1.08 | 22 | 0.9× / 1.1× | 1× |

All additions have explicit neutral luck. Curios follows heritage: Elven necklace, Dwarven belt, Faeborne ring and Undead charm. Human, Bestial and Draconic receive no extra slot. Zephyr receives 1.4× incoming knockback; the other additions use 1×. Added size changes geometry without a racial reach increase.

## Colossan

`runic_races:colossan` · Human · impact 2

**Colossal Heave — 35s cooldown.** Prepare one direct melee strike for 5s. At 90% attack charge, the next successful primary hit adds 3 HP of physical damage and moderate knockback. Sweeping hits do not consume or repeat it.

**Giant's Bearing.** +4 maximum HP and +0.30 knockback resistance. With Pehkui, stand at 1.20 scale; this grants no racial reach bonus.

**Heavy Limbs.** -12% attack speed and +15% exhaustion. Time your swings and carry a good meal.

Best situations: Committed melee, door holding. Weak situations: Fast skirmishes and food-poor travel. Nearest comparisons: Valen and Bovine. Counterplay: Bait the charged swing; fight around cover.

No numerical change from the prompt. Clarifications and shared corrections are listed in the validation record.

## Auroran

`runic_races:auroran` · Human · impact 2

**Dawnward — 55s cooldown.** Ward yourself and up to two allies within 6 blocks and clear sight for 6s. Each ward prevents at most 4 HP after armor and absorption. Racial wards never add together on one hit.

**Inner Radiance.** +2 maximum HP and 15% less classified magical damage. A magical projectile uses this affinity.

**Mortal Vessel.** Take 15% more ordinary physical and projectile damage. Armor, shields and cover remain useful; magical projectiles do not also receive this penalty.

Best situations: Short, coordinated defensive exchanges. Weak situations: Sustained physical pressure. Nearest comparisons: Valen, Moss One and Runic One. Counterplay: Spread pressure across time; the 4 HP ward is finite.

No numerical change from the prompt. Clarifications and shared corrections are listed in the validation record.

## Grove Elf

`runic_races:grove_elf` · Elven · impact 1

**Stillleaf Aim — 35s cooldown.** For 6s, prepare one fully drawn bow or loaded crossbow arrow. Its first successful hit gains 20% damage, capped at +2 HP. For 3s, only you see a small target cue while it remains visible. Multishot empowers one arrow; Piercing cannot repeat the bonus.

**Canopy Strider.** +10% grounded speed in forest-tagged biomes. Tagged berry underbrush does not slow you; sweet berry bush damage is harmless. Leaves and webs still obstruct you.

**Slender Frame.** -3 maximum HP. Choose cover and distance before committing to a shot.

Best situations: Forest travel and deliberate ranged openings. Weak situations: Close combat and open terrain. Nearest comparisons: Canine and High Elf. Counterplay: Break sight before launch or block the single empowered arrow.

No numerical change from the prompt. Clarifications and shared corrections are listed in the validation record.

## Tide Elf

`runic_races:tide_elf` · Elven · impact 2

**Currentstep — 25s cooldown.** Travel up to 5 blocks through water over 0.5s, following your aim. On land, take a 2-block grounded step. Solid obstacles stop the movement; the current deals no damage.

**Amphibious Grace.** Breathe water, swim 20% faster, see clearly underwater, and remove the normal underwater mining penalty. Tool tiers, Aqua Affinity and Mining Fatigue still work normally. Jump to rise; release jump or sneak to descend.

**Drying Gills.** -2 maximum HP. After 60s out of water or rain, move 8% slower. One second in water, rain exposure, or drinking a potion or water bottle refreshes your gills.

Best situations: Water routes and ordinary underwater work. Weak situations: Long dry overland trips. Nearest comparisons: Sea Serpen and Nymph. Counterplay: Fight away from water; a bottle is accessible upkeep, not immunity.

No numerical change from the prompt. Clarifications and shared corrections are listed in the validation record.

## Astral Elf

`runic_races:astral_elf` · Elven · impact 2

**Starbound Thread — 40s cooldown.** Place a safe grounded anchor for 8s. Release and press again to return within 16 blocks in the same dimension, with clear sight and safe footing. Cooldown begins on placement. Blocked recall leaves the anchor until expiry.

**Astral Poise.** Take 30% less fall damage. Gain night vision during actual nighttime in dimensions with a normal day/night cycle.

**Thin Tether.** -3 maximum HP. A fragile tether cannot cross walls, dimensions or distant terrain; plan an escape before danger arrives.

Best situations: Prepared retreats across visible open ground. Weak situations: Walls, distance and forced displacement. Nearest comparisons: Celeron and Wraith. Counterplay: Block the anchor sightline; its lifetime and range are short.

No numerical change from the prompt. Clarifications and shared corrections are listed in the validation record.

## Mountain One

`runic_races:mountain_one` · Dwarven · impact 1

**Quarry Rhythm — 45s cooldown.** For 6s or 10 successfully broken tagged stone/ore blocks, mine 40% faster with a tool that can harvest them. This changes normal mining speed, never drops, tool tiers, durability or neighboring blocks.

**Bedrock Bearing.** +2 armor. Gain +0.25 knockback resistance while grounded on tagged stone support.

**Deliberate Gait.** -8% movement speed and -8% attack speed. Good footing rewards a deliberate pace.

Best situations: Normal stone/ore mining and stone footing. Weak situations: Pursuit and rapid attack trading. Nearest comparisons: Deep One and Iron One. Counterplay: Move the fight off stone; quarry speed cannot improve harvest tiers.

No numerical change from the prompt. Clarifications and shared corrections are listed in the validation record.

## Moss One

`runic_races:moss_one` · Dwarven · impact 2

**Mycelial Respite — 55s cooldown.** Create a stationary 3-block recovery patch for 8s. Every 2s, restore 1 HP to yourself and up to two allies in sight. Each cast admits three recipients total, removes Poison once per recipient, and offers at most 4 HP each (6 HP after amplification). Overlapping patches share a recovery interval.

**Living Loam.** +2 armor. Finishing tagged mushroom food adds 1 food point and 1 saturation point, within normal limits; containers and food effects are preserved.

**Desiccation.** Take 25% more fire damage and move 5% slower. Shelter from flame and recover where allies can remain nearby.

Best situations: Stationary small-party recovery. Weak situations: Fire and moving encounters. Nearest comparisons: Dryad, Runic One and Auroran. Counterplay: Displace the party or deny the patch; three lifetime recipient slots prevent raid-wide healing.

No numerical change from the prompt. Clarifications and shared corrections are listed in the validation record.

## Crystal One

`runic_races:crystal_one` · Dwarven · impact 2

**Prism Reprisal — 40s cooldown.** For 6s, counter one accepted magical or projectile hit: prevent 50%, capped at 4 HP after armor and absorption. Preventing at least 1 HP answers a legal attacker within 8 blocks and sight for 2 HP. The counter is spent even without a reply; replies cannot trigger racial retaliation.

**Faceted Body.** +2 armor and 10% less classified magical damage.

**Fracture Lines.** -2 maximum HP and 20% more explosion damage. Magical explosions apply both the magical affinity and explosion penalty; avoid blast centers.

Best situations: One predicted magical or projectile exchange. Weak situations: Explosions and sustained melee. Nearest comparisons: Auroran and Iron One. Counterplay: Bait the one counter and break reply sight/range.

No numerical change from the prompt. Clarifications and shared corrections are listed in the validation record.

## Bovine

`runic_races:bovine` · Bestial · impact 2

**Hornrush — 30s cooldown.** From dry ground, wind up for 0.3s and rush up to 5 blocks. The first legal body collision takes 6 HP physical damage and moderate knockback. Walls, allies and pets stop the rush. Your held weapon adds no damage.

**Herd Strength.** +2 maximum HP and +0.20 knockback resistance. Sprint 10% faster while grounded; Pehkui scale is 1.15 when installed.

**Heavy Appetite.** +20% exhaustion and -8% attack speed. A committed charge has little steering; keep food for the journey.

Best situations: A clear grounded charge lane. Weak situations: Corners, pets/allies in the lane and sustained hunger. Nearest comparisons: Colossan, Feline and Valen. Counterplay: Sidestep the windup or force a solid obstruction.

No numerical change from the prompt. Clarifications and shared corrections are listed in the validation record.

## Saurian

`runic_races:saurian` · Bestial · impact 2

**Patient Ambush — 30s cooldown.** After staying nearly still for 1s, prepare a 5s ambush. Your next successful primary melee hit at 90% charge adds 3 HP and Slowness I for 2s. You may move freely after arming.

**Scaled Survivor.** +2 armor and 15 additional seconds of air before drowning. Respiration still helps; this is finite breath, not water breathing.

**Cold-Blooded.** Move 5% slower. In tagged cold biomes, attack 15% slower and take 25% more freezing damage. Leather insulation, Fire Resistance, being on fire, or a lit campfire/fire/lava within 2 blocks prevents the cold state.

Best situations: Patient melee openings and finite underwater tasks. Weak situations: Uninsulated cold and mobile pursuit. Nearest comparisons: Serpen and Feline. Counterplay: Deny stillness before arming; the prepared strike itself permits movement.

No numerical change from the prompt. Clarifications and shared corrections are listed in the validation record.

## Chelon

`runic_races:chelon` · Bestial · impact 2

**Shellfast — 45s cooldown.** Withdraw for up to 5s, preventing 50% of ordinary physical/projectile health damage, with a 12 HP budget. Movement is capped at 20% of walking speed. Attacks, shields, item use, spells and block interaction are suspended. Release and press again to exit. No protection from suffocation, starvation, drowning or bypass damage.

**Living Shell.** +3 armor and 10 additional seconds of air before drowning. Normal land life and all ordinary equipment remain available.

**Unhurried.** -10% movement speed and -10% attack speed, including outside Shellfast. Choose a safe moment to re-enter a fight.

Best situations: Brief physical pressure while withdrawing. Weak situations: Magic, waiting opponents and objective interaction. Nearest comparisons: Iron One and Terra Drake. Counterplay: Wait out the shell or use magic; the stance blocks offense and item use.

No numerical change from the prompt. Clarifications and shared corrections are listed in the validation record.

## Zephyr

`runic_races:zephyr` · Faeborne · impact 3

**Crosswind — 30s cooldown.** Redirect yourself horizontally up to 5 blocks over 0.4s. Solid obstacles stop you. Crosswind deals no damage, grants no invulnerability, and cannot launch you upward.

**Airborne Essence.** Glide on visible air-ribbon wings and take 50% less fall damage. Flap for +0.30 upward velocity every 2s; each flap costs one feather when enabled. Wind Wyrm remains the stronger powered flier.

**Scattered Form.** -4 maximum HP and 1.40 times knockback received. Avoid exposed melee and keep room to recover your direction.

Best situations: Gentle aerial repositioning and brief lateral evasion. Weak situations: Heavy hits, knockback and long ascents. Nearest comparisons: Sprite, Avian and Wind Wyrm. Counterplay: Control landing space; low health and bounded lift remain relevant.

No numerical change from the prompt. Clarifications and shared corrections are listed in the validation record.

## Nightborn

`runic_races:nightborn` · Undead · impact 2

**Crimson Hunt — 45s cooldown.** Hunt for 8s with +10% speed. Up to three 90%-charged primary melee hits feed, at least 1s apart: restore 25% of real health damage, capped at 2 HP per hit and 6 HP per cast before healing modifiers, and 6 HP restored after them. Allies, pets, villagers, undead and tagged inorganic creatures cannot feed you.

**Nocturnal Senses.** Poison immunity and night vision in light level 7 or below. Move 15% faster at actual nighttime with at least 12 food points.

**Sun-Starved.** Receive 20% less healing and take 20% more damage in direct sunlight. A helmet, roof, night or rain shields you from direct sun. Normal meals remain useful.

Best situations: Fed nighttime travel and three deliberate living-target hits. Weak situations: Daylight, undead/nonliving targets and empty hunger. Nearest comparisons: Blood Elf and Zombie. Counterplay: Use shields, spacing or an ineligible target; sunlight and food have accessible remedies.

No numerical change from the prompt. Clarifications and shared corrections are listed in the validation record.

## Returned

`runic_races:returned` · Undead · impact 2

**Unfinished Purpose — 40s cooldown.** Designate your latest legal living aggressor from the last 30s, within 16 blocks and sight. For 10s, gain 15% grounded speed while moving toward them; one 90%-charged primary melee hit adds 2 HP. No valid aggressor means no cooldown spent.

**Stubborn Remnant.** +2 maximum HP and poison immunity. Below half health, gain +0.30 knockback resistance.

**Imperfect Return.** Receive 25% less healing. Food and potions work normally at reduced strength; this heritage does not change respawn or Reaper revival.

Best situations: Pursuing a recent visible aggressor. Weak situations: Fresh targets, cover and recovery-heavy attrition. Nearest comparisons: Reaper and Zombie. Counterplay: Break sight or exceed the 16-block mark range; this race has no revival.

No numerical change from the prompt. Clarifications and shared corrections are listed in the validation record.

## Wailer

`runic_races:wailer` · Undead · impact 3

**Keening Cry — 40s cooldown.** After a visible and audible 0.5s warning, cry into a 6-block cone with a 35-degree half-angle. Up to six threats in sight take 2 HP, Weakness I for 4s and moderate knockback. Move 30% slower during the warning. Bosses resist control; player control requires server permission and has half duration.

**Deathwatch.** Poison immunity. Once per second, sense up to three hostile creatures below half health, within 8 blocks and sight, through small owner-only target wisps.

**Fraying Spirit.** -4 maximum HP and 15% more ordinary physical damage taken. Use cover and space during the cry's warning.

Best situations: An anticipated cone against nearby threats. Weak situations: Physical burst, interruptions and cover. Nearest comparisons: Wraith and Canine. Counterplay: Damage the windup or leave the cone; bosses resist control.

No numerical change from the prompt. Clarifications and shared corrections are listed in the validation record.

## Scaleheir

`runic_races:scaleheir` · Draconic · impact 1

**Dominion Roar — 35s cooldown.** Ward yourself for 5s, preventing at most 4 HP after armor and absorption. Up to three threats within 5 blocks and sight receive Weakness I for 3s. No damage, forced targeting or boss control; player control follows server policy.

**Inherited Scales.** +2 armor and +0.20 knockback resistance. This grounded lineage has no racial flight, breath weapon or elemental immunity.

**Demanding Blood.** +20% exhaustion and -8% attack speed. Good meals and measured swings sustain demanding draconic blood.

Best situations: A short grounded defensive/control exchange. Weak situations: Travel and sustained pressure after the finite guard. Nearest comparisons: Terra Drake and Valen. Counterplay: Wait out the guard; there are no wings or breath attacks.

No numerical change from the prompt. Clarifications and shared corrections are listed in the validation record.

## Wyvernkin

`runic_races:wyvernkin` · Draconic · impact 2

**Venom Swoop — 35s cooldown.** Lunge up to 3 blocks from the ground or sweep up to 5 blocks while already gliding. The first legal collision takes 5 HP physical damage and Poison I for 3s. Walls and protected bodies stop you. No weapon damage, repeated contact hits or powered ascent.

**Lean Wings.** Innate gliding wings, +1 armor and 25% less fall damage. No independent powered flap; use terrain and the occasional swoop.

**Exposed Membranes.** -2 maximum HP and 15% more projectile damage taken. Cover your exposed membranes from arrows and magical projectiles.

Best situations: Terrain-assisted gliding approaches. Weak situations: Projectile exposure and sustained upward travel. Nearest comparisons: Serpen, Avian and elemental drakes. Counterplay: Use cover, armor and ranged pressure; it cannot flap or spam a breath.

No numerical change from the prompt. Clarifications and shared corrections are listed in the validation record.
