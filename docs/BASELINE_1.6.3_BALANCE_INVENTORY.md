# 1.6.3 source balance inventory

Baseline: 1877c025265920f2800ac09ba7f194244e9a7e3c. Amounts are HP; time values are ticks. This is source inventory, not runtime or comparative-balance evidence.

The JSON excerpts below preserve baseline inputs, including inputs later found to fail native parsing. The 1.7.0 server tests found unsupported negation wrappers in Magi Arcane Overflow, Sky One Thin-Air Lungs, Sea Serpen Landbound Coils, Volt Drake Grounded and Wind Wyrm Untethered. Their conditions now use the supported `inverted: true` field with unchanged numerical values. See [the implementation record](VALIDATION_1.7.0.md) for that deliberate runtime repair and the optional-integration ownership corrections; these excerpts should not be copied back over the repaired generator output.

## arachnid

### web_snare

Authored description: Cast a web: foes within 5 blocks are rooted by Slowness IV and a trap is left underfoot. 35-second cooldown.

Executed JSON inputs:

- `/cooldown_timer`: `{"type":"origins:resource","min":0,"max":700,"start_value":0,"hud_render":{"should_render":false,"sprite_location":"origins:textures/gui/community/spade.png","bar_index":2},"min_action":null,"max_action":null}`
- `/cooldown_decay/entity_action/condition`: `{"type":"origins:resource","resource":"runic_races:arachnid/web_snare_cooldown_timer","comparison":">","compare_to":0}`
- `/active_ability/condition`: `{"type":"origins:resource","resource":"runic_races:arachnid/web_snare_cooldown_timer","comparison":"==","compare_to":0}`
- `/active_ability/entity_action/actions/0`: `{"type":"runic_races:place_trap","duration_ticks":1200}`
- `/active_ability/entity_action/actions/1`: `{"type":"runic_races:afflict_hostiles","radius":5,"effects":[{"effect":"minecraft:slowness","duration_ticks":120,"amplifier":3}]}`
- `/active_ability/entity_action/actions/2`: `{"type":"origins:apply_effect","effect":{"effect":"minecraft:slow_falling","duration":100,"amplifier":0,"is_ambient":false,"show_particles":true,"show_icon":true}}`
- `/active_ability/entity_action/actions/3`: `{"type":"runic_races:signature_presentation","key":"ARACHNID_WEB_SNARE"}`

### weavers_senses

Authored description: Poison immunity, +10% attack speed, no fall damage, venomous fangs, and constant vibration-sense of nearby creatures.

Executed JSON inputs:

- `/poison_imm`: `{"type":"origins:effect_immunity","effect":"minecraft:poison"}`
- `/attack_speed`: `{"type":"origins:attribute","modifier":{"attribute":"minecraft:generic.attack_speed","operation":"multiply_total","value":0.1,"name":"Spider Quickness"}}`
- `/no_fall`: `{"type":"origins:modify_damage_taken","damage_condition":{"type":"origins:name","name":"fall"},"modifier":{"operation":"multiply_total_multiplicative","value":-1.0}}`
- `/vibration/entity_action`: `{"type":"runic_races:glow_hostiles","radius":8.0,"duration_ticks":120}`

### fragile_carapace

Authored description: -1.5 hearts and +20% fire damage taken. Spiders fear flame.

Executed JSON inputs:

- `/health`: `{"type":"origins:attribute","modifier":{"attribute":"minecraft:generic.max_health","operation":"addition","value":-3.0,"name":"Fragile Shell"}}`
- `/fire`: `{"type":"origins:modify_damage_taken","damage_condition":{"type":"origins:or","conditions":[{"type":"origins:name","name":"inFire"},{"type":"origins:name","name":"onFire"},{"type":"origins:name","name":"lava"},{"type":"origins:name","name":"hotFloor"},{"type":"origins:name","name":"fireball"}]},"modifier":{"operation":"multiply_total_multiplicative","value":0.2}}`

## avian

### wind_burst

Authored description: Beat your wings for a skyward burst with Slow Falling. 20-second cooldown.

Executed JSON inputs:

- `/cooldown_timer`: `{"type":"origins:resource","min":0,"max":400,"start_value":0,"hud_render":{"should_render":false,"sprite_location":"origins:textures/gui/community/spade.png","bar_index":2},"min_action":null,"max_action":null}`
- `/cooldown_decay/entity_action/condition`: `{"type":"origins:resource","resource":"runic_races:avian/wind_burst_cooldown_timer","comparison":">","compare_to":0}`
- `/active_ability/condition`: `{"type":"origins:resource","resource":"runic_races:avian/wind_burst_cooldown_timer","comparison":"==","compare_to":0}`
- `/active_ability/entity_action/actions/0`: `{"type":"origins:add_velocity","x":0.0,"y":1.1,"z":0.0,"space":"local"}`
- `/active_ability/entity_action/actions/1`: `{"type":"origins:apply_effect","effect":{"effect":"minecraft:slow_falling","duration":140,"amplifier":0,"is_ambient":false,"show_particles":true,"show_icon":true}}`
- `/active_ability/entity_action/actions/2`: `{"type":"origins:apply_effect","effect":{"effect":"minecraft:speed","duration":80,"amplifier":0,"is_ambient":false,"show_particles":true,"show_icon":true}}`
- `/active_ability/entity_action/actions/3`: `{"type":"runic_races:signature_presentation","key":"AVIAN_WIND_BURST"}`

### skyborne

Authored description: Feathered wings for gliding flight, no fall damage, +10% speed, and keen night sight.

Executed JSON inputs:

- `/elytra_flight`: `{"type":"origins:elytra_flight","render_elytra":false}`
- `/flap_cooldown_timer`: `{"type":"origins:resource","min":0,"max":35,"start_value":0,"hud_render":{"should_render":false,"sprite_location":"origins:textures/gui/community/spade.png","bar_index":2},"min_action":null,"max_action":null}`
- `/flap_cooldown_decay/entity_action/condition`: `{"type":"origins:resource","resource":"runic_races:avian/skyborne_flap_cooldown_timer","comparison":">","compare_to":0}`
- `/no_fall`: `{"type":"origins:modify_damage_taken","damage_condition":{"type":"origins:name","name":"fall"},"modifier":{"operation":"multiply_total_multiplicative","value":-1.0}}`
- `/speed`: `{"type":"origins:attribute","modifier":{"attribute":"minecraft:generic.movement_speed","operation":"multiply_total","value":0.1,"name":"Swift Flight"}}`
- `/night_vision`: `{"type":"origins:night_vision","strength":1.0}`

### hollow_bones

Authored description: -2 hearts, easily knocked back, and -10% melee damage. A light, hollow frame.

Executed JSON inputs:

- `/health`: `{"type":"origins:attribute","modifier":{"attribute":"minecraft:generic.max_health","operation":"addition","value":-4.0,"name":"Hollow Frame"}}`
- `/melee`: `{"type":"origins:attribute","modifier":{"attribute":"minecraft:generic.attack_damage","operation":"multiply_total","value":-0.1,"name":"Weak Grip"}}`

## blood_elf

### blood_frenzy

Authored description: Burn your own vitality into power: Strength II and Speed I for 8s, Regeneration for 5s. 35-second cooldown.

Executed JSON inputs:

- `/cooldown_timer`: `{"type":"origins:resource","min":0,"max":700,"start_value":0,"hud_render":{"should_render":false,"sprite_location":"origins:textures/gui/community/spade.png","bar_index":2},"min_action":null,"max_action":null}`
- `/cooldown_decay/entity_action/condition`: `{"type":"origins:resource","resource":"runic_races:blood_elf/blood_frenzy_cooldown_timer","comparison":">","compare_to":0}`
- `/active_ability/condition`: `{"type":"origins:resource","resource":"runic_races:blood_elf/blood_frenzy_cooldown_timer","comparison":"==","compare_to":0}`
- `/active_ability/entity_action/actions/0`: `{"type":"origins:apply_effect","effect":{"effect":"minecraft:strength","duration":160,"amplifier":1,"is_ambient":false,"show_particles":true,"show_icon":true}}`
- `/active_ability/entity_action/actions/1`: `{"type":"origins:apply_effect","effect":{"effect":"minecraft:speed","duration":160,"amplifier":0,"is_ambient":false,"show_particles":true,"show_icon":true}}`
- `/active_ability/entity_action/actions/2`: `{"type":"origins:apply_effect","effect":{"effect":"minecraft:regeneration","duration":100,"amplifier":0,"is_ambient":false,"show_particles":true,"show_icon":true}}`
- `/active_ability/entity_action/actions/3`: `{"type":"runic_races:signature_presentation","key":"BLOOD_ELF_FRENZY"}`

### bloodcraft

Authored description: +10% melee damage and +10% magic damage, and your strikes leech a fifth of the damage dealt. Blood is power.

Executed JSON inputs:

- `/melee`: `{"type":"origins:attribute","modifier":{"attribute":"minecraft:generic.attack_damage","operation":"multiply_total","value":0.1,"name":"Bloodcraft Strength"}}`
- `/magic`: `{"type":"origins:modify_damage_dealt","damage_condition":{"type":"origins:or","conditions":[{"type":"origins:name","name":"magic"},{"type":"origins:name","name":"indirectMagic"}]},"modifier":{"operation":"multiply_total_multiplicative","value":0.1}}`

### price_of_power

Authored description: -1.5 hearts and -30% natural healing. Power always has its price.

Executed JSON inputs:

- `/health`: `{"type":"origins:attribute","modifier":{"attribute":"minecraft:generic.max_health","operation":"addition","value":-3.0,"name":"Blood Price"}}`
- `/healing`: `{"type":"origins:modify_healing","modifier":{"operation":"multiply_total_multiplicative","value":-0.3}}`

## canine

### howl_of_the_pack

Authored description: Howl: gain Strength and Speed and mark wounded prey for 10s. 45-second cooldown.

Executed JSON inputs:

- `/cooldown_timer`: `{"type":"origins:resource","min":0,"max":900,"start_value":0,"hud_render":{"should_render":false,"sprite_location":"origins:textures/gui/community/spade.png","bar_index":2},"min_action":null,"max_action":null}`
- `/cooldown_decay/entity_action/condition`: `{"type":"origins:resource","resource":"runic_races:canine/howl_of_the_pack_cooldown_timer","comparison":">","compare_to":0}`
- `/active_ability/condition`: `{"type":"origins:resource","resource":"runic_races:canine/howl_of_the_pack_cooldown_timer","comparison":"==","compare_to":0}`
- `/active_ability/entity_action/actions/0`: `{"type":"origins:apply_effect","effect":{"effect":"minecraft:strength","duration":200,"amplifier":0,"is_ambient":false,"show_particles":true,"show_icon":true}}`
- `/active_ability/entity_action/actions/1`: `{"type":"origins:apply_effect","effect":{"effect":"minecraft:speed","duration":200,"amplifier":0,"is_ambient":false,"show_particles":true,"show_icon":true}}`
- `/active_ability/entity_action/actions/2`: `{"type":"runic_races:glow_hostiles","radius":14.0,"duration_ticks":200}`
- `/active_ability/entity_action/actions/3`: `{"type":"runic_races:signature_presentation","key":"CANINE_HOWL"}`

### pack_hunter

Authored description: +12% speed, night vision, +10% melee damage, and at home in forests and taigas.

Executed JSON inputs:

- `/speed`: `{"type":"origins:attribute","modifier":{"attribute":"minecraft:generic.movement_speed","operation":"multiply_total","value":0.12,"name":"Pack Speed"}}`
- `/night_vision`: `{"type":"origins:night_vision","strength":1.0}`
- `/melee`: `{"type":"origins:attribute","modifier":{"attribute":"minecraft:generic.attack_damage","operation":"multiply_total","value":0.1,"name":"Hunter's Bite"}}`
- `/forest_home`: `{"type":"runic_races:biome_affinity","name":"power.runic_races.canine.pack_hunter.name","description":"power.runic_races.canine.pack_hunter.description","home_biome_tag":"minecraft:is_forest","speed_bonus":0.06,"damage_bonus":0.05,"check_interval":40}`
- `/taiga_home`: `{"type":"runic_races:biome_affinity","name":"power.runic_races.canine.pack_hunter.name","description":"power.runic_races.canine.pack_hunter.description","home_biome_tag":"minecraft:is_taiga","speed_bonus":0.06,"damage_bonus":0.05,"check_interval":40}`

### ravenous

Authored description: +25% hunger drain and -2 armor. Always hungry, thin of hide.

Executed JSON inputs:

- `/hunger`: `{"type":"origins:modify_exhaustion","modifier":{"operation":"multiply_total_multiplicative","value":0.25}}`
- `/armor`: `{"type":"origins:attribute","modifier":{"attribute":"minecraft:generic.armor","operation":"addition","value":-2.0,"name":"Thin Hide"}}`

## celeron

### messengers_dash

Authored description: Dash forward with Speed III for 4s. 25-second cooldown.

Executed JSON inputs:

- `/cooldown_timer`: `{"type":"origins:resource","min":0,"max":500,"start_value":0,"hud_render":{"should_render":false,"sprite_location":"origins:textures/gui/community/spade.png","bar_index":2},"min_action":null,"max_action":null}`
- `/cooldown_decay/entity_action/condition`: `{"type":"origins:resource","resource":"runic_races:celeron/messengers_dash_cooldown_timer","comparison":">","compare_to":0}`
- `/active_ability/condition`: `{"type":"origins:resource","resource":"runic_races:celeron/messengers_dash_cooldown_timer","comparison":"==","compare_to":0}`
- `/active_ability/entity_action/actions/0`: `{"type":"origins:add_velocity","x":0.0,"y":0.3,"z":1.6,"space":"local"}`
- `/active_ability/entity_action/actions/1`: `{"type":"origins:apply_effect","effect":{"effect":"minecraft:speed","duration":80,"amplifier":2,"is_ambient":false,"show_particles":true,"show_icon":true}}`
- `/active_ability/entity_action/actions/2`: `{"type":"runic_races:signature_presentation","key":"CELERON_DASH"}`

### fleet_and_sure

Authored description: +12% movement speed and +10% attack speed. The road never tires you.

Executed JSON inputs:

- `/speed`: `{"type":"origins:attribute","modifier":{"attribute":"minecraft:generic.movement_speed","operation":"multiply_total","value":0.12,"name":"Celeron Speed"}}`
- `/attack_speed`: `{"type":"origins:attribute","modifier":{"attribute":"minecraft:generic.attack_speed","operation":"multiply_total","value":0.1,"name":"Celeron Quickness"}}`

### featherweight_frame

Authored description: -2 hearts and you are knocked back further than most.

Executed JSON inputs:

- `/health`: `{"type":"origins:attribute","modifier":{"attribute":"minecraft:generic.max_health","operation":"addition","value":-4.0,"name":"Featherweight Health"}}`

## changeling

### mirror_shift

Authored description: Slip away behind a glamour: Invisibility and Speed for 6s. 25-second cooldown.

Executed JSON inputs:

- `/cooldown_timer`: `{"type":"origins:resource","min":0,"max":500,"start_value":0,"hud_render":{"should_render":false,"sprite_location":"origins:textures/gui/community/spade.png","bar_index":2},"min_action":null,"max_action":null}`
- `/cooldown_decay/entity_action/condition`: `{"type":"origins:resource","resource":"runic_races:changeling/mirror_shift_cooldown_timer","comparison":">","compare_to":0}`
- `/active_ability/condition`: `{"type":"origins:resource","resource":"runic_races:changeling/mirror_shift_cooldown_timer","comparison":"==","compare_to":0}`
- `/active_ability/entity_action/actions/0`: `{"type":"origins:apply_effect","effect":{"effect":"minecraft:invisibility","duration":120,"amplifier":0,"is_ambient":false,"show_particles":false,"show_icon":true}}`
- `/active_ability/entity_action/actions/1`: `{"type":"origins:apply_effect","effect":{"effect":"minecraft:speed","duration":100,"amplifier":0,"is_ambient":false,"show_particles":true,"show_icon":true}}`
- `/active_ability/entity_action/actions/2`: `{"type":"runic_races:signature_presentation","key":"CHANGELING_MIRROR"}`

### manyfaces

Authored description: +10% movement speed and +1 Luck. You wear whatever face the moment needs.

Executed JSON inputs:

- `/speed`: `{"type":"origins:attribute","modifier":{"attribute":"minecraft:generic.movement_speed","operation":"multiply_total","value":0.1,"name":"Fluid Step"}}`
- `/luck`: `{"type":"origins:attribute","modifier":{"attribute":"minecraft:generic.luck","operation":"addition","value":1.0,"name":"Borrowed Fortune"}}`

### hollow_identity

Authored description: -1 heart and -5% attack damage. With no true self, no strike lands with full conviction.

Executed JSON inputs:

- `/health`: `{"type":"origins:attribute","modifier":{"attribute":"minecraft:generic.max_health","operation":"addition","value":-2.0,"name":"Hollow Health"}}`
- `/attack`: `{"type":"origins:attribute","modifier":{"attribute":"minecraft:generic.attack_damage","operation":"multiply_total","value":-0.05,"name":"No True Self"}}`

## dark_elf

### shadowmeld

Authored description: Vanish into shadow: Invisibility and Speed II for 6s. 45-second cooldown.

Executed JSON inputs:

- `/cooldown_timer`: `{"type":"origins:resource","min":0,"max":900,"start_value":0,"hud_render":{"should_render":false,"sprite_location":"origins:textures/gui/community/spade.png","bar_index":2},"min_action":null,"max_action":null}`
- `/cooldown_decay/entity_action/condition`: `{"type":"origins:resource","resource":"runic_races:dark_elf/shadowmeld_cooldown_timer","comparison":">","compare_to":0}`
- `/active_ability/condition`: `{"type":"origins:resource","resource":"runic_races:dark_elf/shadowmeld_cooldown_timer","comparison":"==","compare_to":0}`
- `/active_ability/entity_action/actions/0`: `{"type":"origins:apply_effect","effect":{"effect":"minecraft:invisibility","duration":120,"amplifier":0,"is_ambient":false,"show_particles":false,"show_icon":true}}`
- `/active_ability/entity_action/actions/1`: `{"type":"origins:apply_effect","effect":{"effect":"minecraft:speed","duration":120,"amplifier":1,"is_ambient":false,"show_particles":true,"show_icon":true}}`
- `/active_ability/entity_action/actions/2`: `{"type":"runic_races:signature_presentation","key":"DARK_ELF_SHADOWMELD"}`

### children_of_darkness

Authored description: Night vision, +10% movement speed, and your strikes hit harder in the dark of night — but falter under the open daytime sky.

Executed JSON inputs:

- `/night_vision`: `{"type":"origins:night_vision","strength":1.0}`
- `/speed`: `{"type":"origins:attribute","modifier":{"attribute":"minecraft:generic.movement_speed","operation":"multiply_total","value":0.1,"name":"Shadow Step"}}`
- `/night_power`: `{"type":"runic_races:scaling_attribute","name":"power.runic_races.dark_elf.children_of_darkness.name","description":"power.runic_races.dark_elf.children_of_darkness.description","attribute":"generic.attack_damage","day_value":-0.05,"night_value":0.1,"operation":"multiply_total","check_interval":40,"require_sky_exposure":true}`

### sunlight_sensitivity

Authored description: -1 heart and you are sluggish and weaker under direct daylight.

Executed JSON inputs:

- `/health`: `{"type":"origins:attribute","modifier":{"attribute":"minecraft:generic.max_health","operation":"addition","value":-2.0,"name":"Sun-Averse Health"}}`
- `/sun_slow`: `{"type":"origins:conditioned_attribute","modifier":{"attribute":"minecraft:generic.movement_speed","operation":"multiply_total","value":-0.1,"name":"Sunlight Sluggishness"},"condition":{"type":"origins:and","conditions":[{"type":"origins:exposed_to_sun"},{"type":"origins:daytime"}]}}`
- `/sun_weak`: `{"type":"origins:conditioned_attribute","modifier":{"attribute":"minecraft:generic.attack_damage","operation":"multiply_total","value":-0.1,"name":"Sunlight Weakness"},"condition":{"type":"origins:and","conditions":[{"type":"origins:exposed_to_sun"},{"type":"origins:daytime"}]}}`

## deep_one

### tremorsense

Authored description: Read the stone: nearby hostiles are revealed through walls. Best underground. 15-second cooldown.

Executed JSON inputs:

- `/cooldown_timer`: `{"type":"origins:resource","min":0,"max":300,"start_value":0,"hud_render":{"should_render":false,"sprite_location":"origins:textures/gui/community/spade.png","bar_index":2},"min_action":null,"max_action":null}`
- `/cooldown_decay/entity_action/condition`: `{"type":"origins:resource","resource":"runic_races:deep_one/tremorsense_cooldown_timer","comparison":">","compare_to":0}`
- `/active_ability/condition`: `{"type":"origins:resource","resource":"runic_races:deep_one/tremorsense_cooldown_timer","comparison":"==","compare_to":0}`
- `/active_ability/entity_action/actions/0`: `{"type":"runic_races:tremor_ping","radius":18.0,"duration_ticks":60}`
- `/active_ability/entity_action/actions/1`: `{"type":"runic_races:signature_presentation","key":"DEEP_ONE_TREMOR"}`

### deep_dweller

Authored description: Darkvision, +2 armor, immunity to Mining Fatigue, and faster mining underground.

Executed JSON inputs:

- `/night_vision`: `{"type":"origins:night_vision","strength":1.0}`
- `/armor`: `{"type":"origins:attribute","modifier":{"attribute":"minecraft:generic.armor","operation":"addition","value":2.0,"name":"Deep Plating"}}`
- `/mining_fatigue_imm`: `{"type":"origins:effect_immunity","effect":"minecraft:mining_fatigue"}`
- `/mining`: `{"type":"origins:modify_break_speed","modifier":{"operation":"multiply_total_multiplicative","value":0.2}}`

### sunlight_blindness

Authored description: -5% speed always; under open sky you are slowed and weakened by the glare.

Executed JSON inputs:

- `/slow`: `{"type":"origins:attribute","modifier":{"attribute":"minecraft:generic.movement_speed","operation":"multiply_total","value":-0.05,"name":"Stocky Gait"}}`
- `/sun_slow`: `{"type":"origins:conditioned_attribute","modifier":{"attribute":"minecraft:generic.movement_speed","operation":"multiply_total","value":-0.1,"name":"Surface Glare"},"condition":{"type":"origins:and","conditions":[{"type":"origins:exposed_to_sun"},{"type":"origins:daytime"}]}}`
- `/sun_weak`: `{"type":"origins:conditioned_attribute","modifier":{"attribute":"minecraft:generic.attack_damage","operation":"multiply_total","value":-0.1,"name":"Sun-Dazzled"},"condition":{"type":"origins:and","conditions":[{"type":"origins:exposed_to_sun"},{"type":"origins:daytime"}]}}`

## demon

### infernal_wrath

Authored description: Erupt in hellfire: gain Strength II and Fire Resistance while nearby foes are set ablaze and Weakened. 50-second cooldown.

Executed JSON inputs:

- `/cooldown_timer`: `{"type":"origins:resource","min":0,"max":1000,"start_value":0,"hud_render":{"should_render":false,"sprite_location":"origins:textures/gui/community/spade.png","bar_index":2},"min_action":null,"max_action":null}`
- `/cooldown_decay/entity_action/condition`: `{"type":"origins:resource","resource":"runic_races:demon/infernal_wrath_cooldown_timer","comparison":">","compare_to":0}`
- `/active_ability/condition`: `{"type":"origins:resource","resource":"runic_races:demon/infernal_wrath_cooldown_timer","comparison":"==","compare_to":0}`
- `/active_ability/entity_action/actions/0`: `{"type":"origins:apply_effect","effect":{"effect":"minecraft:strength","duration":160,"amplifier":1,"is_ambient":false,"show_particles":true,"show_icon":true}}`
- `/active_ability/entity_action/actions/1`: `{"type":"origins:apply_effect","effect":{"effect":"minecraft:fire_resistance","duration":160,"amplifier":0,"is_ambient":false,"show_particles":true,"show_icon":true}}`
- `/active_ability/entity_action/actions/2`: `{"type":"runic_races:afflict_hostiles","radius":6,"effects":[{"effect":"minecraft:weakness","duration_ticks":100,"amplifier":0}],"set_on_fire_seconds":6}`
- `/active_ability/entity_action/actions/3`: `{"type":"runic_races:signature_presentation","key":"DEMON_WRATH"}`

### infernal_blood

Authored description: Immune to fire, +15% melee damage, and stronger in the heat.

Executed JSON inputs:

- `/fire_imm`: `{"type":"origins:modify_damage_taken","damage_condition":{"type":"origins:or","conditions":[{"type":"origins:name","name":"inFire"},{"type":"origins:name","name":"onFire"},{"type":"origins:name","name":"lava"},{"type":"origins:name","name":"hotFloor"}]},"modifier":{"operation":"multiply_total_multiplicative","value":-1.0}}`
- `/melee`: `{"type":"origins:attribute","modifier":{"attribute":"minecraft:generic.attack_damage","operation":"multiply_total","value":0.15,"name":"Infernal Strength"}}`
- `/hot_home`: `{"type":"runic_races:biome_affinity","name":"power.runic_races.demon.infernal_blood.name","description":"power.runic_races.demon.infernal_blood.description","home_biome_tag":"forge:is_hot","speed_bonus":0.05,"damage_bonus":0.08,"check_interval":40}`

### holy_vulnerability

Authored description: +25% damage from holy magic, +20% damage while in water, and -25% natural healing.

Executed JSON inputs:

- `/holy`: `{"type":"origins:modify_damage_taken","damage_condition":{"type":"origins:or","conditions":[{"type":"origins:name","name":"magic"},{"type":"origins:name","name":"indirectMagic"}]},"modifier":{"operation":"multiply_total_multiplicative","value":0.25}}`
- `/water`: `{"type":"origins:modify_damage_taken","condition":{"type":"origins:submerged_in","fluid":"minecraft:water"},"modifier":{"operation":"multiply_total_multiplicative","value":0.2}}`
- `/healing`: `{"type":"origins:modify_healing","modifier":{"operation":"multiply_total_multiplicative","value":-0.25}}`

## dryad

### verdant_bloom

Authored description: Burst into bloom: heal yourself with Regeneration while roots slow nearby foes. 45-second cooldown.

Executed JSON inputs:

- `/cooldown_timer`: `{"type":"origins:resource","min":0,"max":900,"start_value":0,"hud_render":{"should_render":false,"sprite_location":"origins:textures/gui/community/spade.png","bar_index":2},"min_action":null,"max_action":null}`
- `/cooldown_decay/entity_action/condition`: `{"type":"origins:resource","resource":"runic_races:dryad/verdant_bloom_cooldown_timer","comparison":">","compare_to":0}`
- `/active_ability/condition`: `{"type":"origins:resource","resource":"runic_races:dryad/verdant_bloom_cooldown_timer","comparison":"==","compare_to":0}`
- `/active_ability/entity_action/actions/0`: `{"type":"origins:apply_effect","effect":{"effect":"minecraft:regeneration","duration":120,"amplifier":1,"is_ambient":false,"show_particles":true,"show_icon":true}}`
- `/active_ability/entity_action/actions/1`: `{"type":"origins:heal","amount":4.0}`
- `/active_ability/entity_action/actions/2`: `{"type":"runic_races:afflict_hostiles","radius":6,"effects":[{"effect":"minecraft:slowness","duration_ticks":120,"amplifier":1}]}`
- `/active_ability/entity_action/actions/3`: `{"type":"runic_races:signature_presentation","key":"DRYAD_BLOOM"}`

### one_with_the_grove

Authored description: Poison immunity, at home in the forest, and you slowly heal in sunlight on the open ground.

Executed JSON inputs:

- `/poison_imm`: `{"type":"origins:effect_immunity","effect":"minecraft:poison"}`
- `/forest_home`: `{"type":"runic_races:biome_affinity","name":"power.runic_races.dryad.one_with_the_grove.name","description":"power.runic_races.dryad.one_with_the_grove.description","home_biome_tag":"minecraft:is_forest","speed_bonus":0.06,"damage_bonus":0.05,"check_interval":40}`
- `/sun_heal/entity_action/if_action`: `{"type":"origins:heal","amount":1.0}`

### kindling

Authored description: Fire deals triple damage to you. Bark and leaf burn fast.

Executed JSON inputs:

- `/fire`: `{"type":"origins:modify_damage_taken","damage_condition":{"type":"origins:or","conditions":[{"type":"origins:name","name":"inFire"},{"type":"origins:name","name":"onFire"},{"type":"origins:name","name":"lava"},{"type":"origins:name","name":"hotFloor"}]},"modifier":{"operation":"multiply_total_multiplicative","value":2.0}}`

## faerie

### faerie_bargain

Authored description: Weave an old enchantment: bless yourself with Regeneration and Absorption while cursing nearby foes with Slowness, Blindness, and Levitation. 50-second cooldown.

Executed JSON inputs:

- `/cooldown_timer`: `{"type":"origins:resource","min":0,"max":1000,"start_value":0,"hud_render":{"should_render":false,"sprite_location":"origins:textures/gui/community/spade.png","bar_index":2},"min_action":null,"max_action":null}`
- `/cooldown_decay/entity_action/condition`: `{"type":"origins:resource","resource":"runic_races:faerie/faerie_bargain_cooldown_timer","comparison":">","compare_to":0}`
- `/active_ability/condition`: `{"type":"origins:resource","resource":"runic_races:faerie/faerie_bargain_cooldown_timer","comparison":"==","compare_to":0}`
- `/active_ability/entity_action/actions/0`: `{"type":"origins:apply_effect","effect":{"effect":"minecraft:regeneration","duration":120,"amplifier":0,"is_ambient":false,"show_particles":true,"show_icon":true}}`
- `/active_ability/entity_action/actions/1`: `{"type":"origins:apply_effect","effect":{"effect":"minecraft:absorption","duration":120,"amplifier":0,"is_ambient":false,"show_particles":true,"show_icon":true}}`
- `/active_ability/entity_action/actions/2`: `{"type":"runic_races:afflict_hostiles","radius":7,"effects":[{"effect":"minecraft:slowness","duration_ticks":120,"amplifier":1},{"effect":"minecraft:blindness","duration_ticks":120,"amplifier":0},{"effect":"minecraft:levitation","duration_ticks":40,"amplifier":0}]}`
- `/active_ability/entity_action/actions/3`: `{"type":"runic_races:signature_presentation","key":"FAERIE_GLAMOUR"}`

### pixie_flight

Authored description: Delicate wings for gliding flight, Slow Falling, +15% magic damage, +15% speed, and night vision.

Executed JSON inputs:

- `/elytra_flight`: `{"type":"origins:elytra_flight","render_elytra":false}`
- `/flap_cooldown_timer`: `{"type":"origins:resource","min":0,"max":30,"start_value":0,"hud_render":{"should_render":false,"sprite_location":"origins:textures/gui/community/spade.png","bar_index":2},"min_action":null,"max_action":null}`
- `/flap_cooldown_decay/entity_action/condition`: `{"type":"origins:resource","resource":"runic_races:faerie/pixie_flight_flap_cooldown_timer","comparison":">","compare_to":0}`
- `/magic`: `{"type":"origins:modify_damage_dealt","damage_condition":{"type":"origins:or","conditions":[{"type":"origins:name","name":"magic"},{"type":"origins:name","name":"indirectMagic"}]},"modifier":{"operation":"multiply_total_multiplicative","value":0.15}}`
- `/speed`: `{"type":"origins:attribute","modifier":{"attribute":"minecraft:generic.movement_speed","operation":"multiply_total","value":0.15,"name":"Pixie Swiftness"}}`
- `/night_vision`: `{"type":"origins:night_vision","strength":1.0}`

### cold_iron

Authored description: -2.5 hearts, +20% physical damage taken, and easily knocked from the air. Cold iron is anathema to the fae.

Executed JSON inputs:

- `/health`: `{"type":"origins:attribute","modifier":{"attribute":"minecraft:generic.max_health","operation":"addition","value":-5.0,"name":"Delicate Form"}}`
- `/phys`: `{"type":"origins:modify_damage_taken","damage_condition":{"type":"origins:or","conditions":[{"type":"origins:name","name":"player"},{"type":"origins:name","name":"mob"},{"type":"origins:name","name":"arrow"}]},"modifier":{"operation":"multiply_total_multiplicative","value":0.2}}`

## feline

### pounce

Authored description: Leap at your prey with a burst of Strength. 15-second cooldown.

Executed JSON inputs:

- `/cooldown_timer`: `{"type":"origins:resource","min":0,"max":300,"start_value":0,"hud_render":{"should_render":false,"sprite_location":"origins:textures/gui/community/spade.png","bar_index":2},"min_action":null,"max_action":null}`
- `/cooldown_decay/entity_action/condition`: `{"type":"origins:resource","resource":"runic_races:feline/pounce_cooldown_timer","comparison":">","compare_to":0}`
- `/active_ability/condition`: `{"type":"origins:resource","resource":"runic_races:feline/pounce_cooldown_timer","comparison":"==","compare_to":0}`
- `/active_ability/entity_action/actions/0`: `{"type":"origins:add_velocity","x":0.0,"y":0.5,"z":1.5,"space":"local"}`
- `/active_ability/entity_action/actions/1`: `{"type":"origins:apply_effect","effect":{"effect":"minecraft:strength","duration":60,"amplifier":0,"is_ambient":false,"show_particles":true,"show_icon":true}}`
- `/active_ability/entity_action/actions/2`: `{"type":"runic_races:signature_presentation","key":"FELINE_POUNCE"}`

### nine_lives

Authored description: Cheat death once every 15 minutes, landing on your feet briefly weakened. Night vision, +15% attack speed, and no fall damage.

Executed JSON inputs:

- `/cooldown_timer`: `{"type":"origins:resource","min":0,"max":18000,"start_value":0,"hud_render":{"should_render":false,"sprite_location":"origins:textures/gui/community/spade.png","bar_index":2},"min_action":null,"max_action":null}`
- `/cooldown_decay/entity_action/condition`: `{"type":"origins:resource","resource":"runic_races:feline/nine_lives_cooldown_timer","comparison":">","compare_to":0}`
- `/night_vision`: `{"type":"origins:night_vision","strength":1.0}`
- `/attack_speed`: `{"type":"origins:attribute","modifier":{"attribute":"minecraft:generic.attack_speed","operation":"multiply_total","value":0.15,"name":"Feline Quickness"}}`
- `/no_fall`: `{"type":"origins:modify_damage_taken","damage_condition":{"type":"origins:name","name":"fall"},"modifier":{"operation":"multiply_total_multiplicative","value":-1.0}}`

### hydrophobia

Authored description: -1.5 hearts and +30% damage taken while in water.

Executed JSON inputs:

- `/health`: `{"type":"origins:attribute","modifier":{"attribute":"minecraft:generic.max_health","operation":"addition","value":-3.0,"name":"Lithe Frame"}}`
- `/water`: `{"type":"origins:modify_damage_taken","condition":{"type":"origins:submerged_in","fluid":"minecraft:water"},"modifier":{"operation":"multiply_total_multiplicative","value":0.3}}`

## fire_drake

### dragonfire_breath

Authored description: Breathe a cone of searing flame that ignites all it touches. 40-second cooldown.

Executed JSON inputs:

- `/cooldown_timer`: `{"type":"origins:resource","min":0,"max":800,"start_value":0,"hud_render":{"should_render":false,"sprite_location":"origins:textures/gui/community/spade.png","bar_index":2},"min_action":null,"max_action":null}`
- `/cooldown_decay/entity_action/condition`: `{"type":"origins:resource","resource":"runic_races:fire_drake/dragonfire_breath_cooldown_timer","comparison":">","compare_to":0}`
- `/active_ability/condition`: `{"type":"origins:resource","resource":"runic_races:fire_drake/dragonfire_breath_cooldown_timer","comparison":"==","compare_to":0}`
- `/active_ability/entity_action/actions/0`: `{"type":"runic_races:cone_breath","range":7.0,"half_angle_degrees":22.0,"damage":6.0,"fire_seconds":8,"element":"fire"}`
- `/active_ability/entity_action/actions/1`: `{"type":"runic_races:signature_presentation","key":"FIRE_DRAKE_BREATH"}`

### emberscale_hide

Authored description: Immune to fire and lava, +3 armor, +10% melee damage, gliding wings, and stronger in the heat.

Executed JSON inputs:

- `/elytra_flight`: `{"type":"origins:elytra_flight","render_elytra":false}`
- `/fire_imm`: `{"type":"origins:modify_damage_taken","damage_condition":{"type":"origins:or","conditions":[{"type":"origins:name","name":"inFire"},{"type":"origins:name","name":"onFire"},{"type":"origins:name","name":"lava"},{"type":"origins:name","name":"hotFloor"}]},"modifier":{"operation":"multiply_total_multiplicative","value":-1.0}}`
- `/armor`: `{"type":"origins:attribute","modifier":{"attribute":"minecraft:generic.armor","operation":"addition","value":3.0,"name":"Emberscale"}}`
- `/melee`: `{"type":"origins:attribute","modifier":{"attribute":"minecraft:generic.attack_damage","operation":"multiply_total","value":0.1,"name":"Drake Strength"}}`
- `/hot_home`: `{"type":"runic_races:biome_affinity","name":"power.runic_races.fire_drake.emberscale_hide.name","description":"power.runic_races.fire_drake.emberscale_hide.description","home_biome_tag":"forge:is_hot","speed_bonus":0.05,"damage_bonus":0.08,"hostile_biome_tag":"forge:is_cold","speed_penalty":-0.1,"check_interval":40}`

### cold_quenches_fire

Authored description: +35% cold damage taken, +35% damage while wet, +20% hunger drain. Cold and water are your bane.

Executed JSON inputs:

- `/cold`: `{"type":"origins:modify_damage_taken","damage_condition":{"type":"origins:name","name":"freeze"},"modifier":{"operation":"multiply_total_multiplicative","value":0.35}}`
- `/water`: `{"type":"origins:modify_damage_taken","condition":{"type":"origins:submerged_in","fluid":"minecraft:water"},"modifier":{"operation":"multiply_total_multiplicative","value":0.35}}`
- `/hunger`: `{"type":"origins:modify_exhaustion","modifier":{"operation":"multiply_total_multiplicative","value":0.2}}`

## forge_one

### forge_blessing

Authored description: Call the forge's fire into your body: Strength and Fire Resistance for 10s. 50-second cooldown.

Executed JSON inputs:

- `/cooldown_timer`: `{"type":"origins:resource","min":0,"max":1000,"start_value":0,"hud_render":{"should_render":false,"sprite_location":"origins:textures/gui/community/spade.png","bar_index":2},"min_action":null,"max_action":null}`
- `/cooldown_decay/entity_action/condition`: `{"type":"origins:resource","resource":"runic_races:forge_one/forge_blessing_cooldown_timer","comparison":">","compare_to":0}`
- `/active_ability/condition`: `{"type":"origins:resource","resource":"runic_races:forge_one/forge_blessing_cooldown_timer","comparison":"==","compare_to":0}`
- `/active_ability/entity_action/actions/0`: `{"type":"origins:apply_effect","effect":{"effect":"minecraft:strength","duration":200,"amplifier":0,"is_ambient":false,"show_particles":true,"show_icon":true}}`
- `/active_ability/entity_action/actions/1`: `{"type":"origins:apply_effect","effect":{"effect":"minecraft:fire_resistance","duration":200,"amplifier":0,"is_ambient":false,"show_particles":true,"show_icon":true}}`
- `/active_ability/entity_action/actions/2`: `{"type":"runic_races:signature_presentation","key":"FORGE_BLESSING"}`

### ironhand_smith

Authored description: 50% fire resistance, +2 armor, and +10% melee damage. Worth proven in fire.

Executed JSON inputs:

- `/fire_resist`: `{"type":"origins:modify_damage_taken","damage_condition":{"type":"origins:or","conditions":[{"type":"origins:name","name":"inFire"},{"type":"origins:name","name":"onFire"},{"type":"origins:name","name":"lava"},{"type":"origins:name","name":"hotFloor"}]},"modifier":{"operation":"multiply_total_multiplicative","value":-0.5}}`
- `/armor`: `{"type":"origins:attribute","modifier":{"attribute":"minecraft:generic.armor","operation":"addition","value":2.0,"name":"Smith's Plate"}}`
- `/melee`: `{"type":"origins:attribute","modifier":{"attribute":"minecraft:generic.attack_damage","operation":"multiply_total","value":0.1,"name":"Hammer Arm"}}`

### stone_heavy

Authored description: -10% movement speed and -10% attack speed; you flounder in deep water.

Executed JSON inputs:

- `/speed`: `{"type":"origins:attribute","modifier":{"attribute":"minecraft:generic.movement_speed","operation":"multiply_total","value":-0.1,"name":"Stone Weight"}}`
- `/attack_speed`: `{"type":"origins:attribute","modifier":{"attribute":"minecraft:generic.attack_speed","operation":"multiply_total","value":-0.1,"name":"Heavy Swing"}}`
- `/water_slow`: `{"type":"origins:conditioned_attribute","modifier":{"attribute":"minecraft:generic.movement_speed","operation":"multiply_total","value":-0.2,"name":"Sinks Like Stone"},"condition":{"type":"origins:submerged_in","fluid":"minecraft:water"}}`

## frost_one

### glacial_resolve

Authored description: Dig in against the cold: Resistance II and Fire Resistance for 8s. 55-second cooldown.

Executed JSON inputs:

- `/cooldown_timer`: `{"type":"origins:resource","min":0,"max":1100,"start_value":0,"hud_render":{"should_render":false,"sprite_location":"origins:textures/gui/community/spade.png","bar_index":2},"min_action":null,"max_action":null}`
- `/cooldown_decay/entity_action/condition`: `{"type":"origins:resource","resource":"runic_races:frost_one/glacial_resolve_cooldown_timer","comparison":">","compare_to":0}`
- `/active_ability/condition`: `{"type":"origins:resource","resource":"runic_races:frost_one/glacial_resolve_cooldown_timer","comparison":"==","compare_to":0}`
- `/active_ability/entity_action/actions/0`: `{"type":"origins:apply_effect","effect":{"effect":"minecraft:resistance","duration":160,"amplifier":1,"is_ambient":false,"show_particles":true,"show_icon":true}}`
- `/active_ability/entity_action/actions/1`: `{"type":"origins:apply_effect","effect":{"effect":"minecraft:fire_resistance","duration":160,"amplifier":0,"is_ambient":false,"show_particles":true,"show_icon":true}}`
- `/active_ability/entity_action/actions/2`: `{"type":"runic_races:signature_presentation","key":"FROST_ONE_RESOLVE"}`

### frostborn

Authored description: Immune to freezing, +2 hearts, +1 armor, and at home in the cold.

Executed JSON inputs:

- `/freeze_imm`: `{"type":"origins:modify_damage_taken","damage_condition":{"type":"origins:name","name":"freeze"},"modifier":{"operation":"multiply_total_multiplicative","value":-1.0}}`
- `/health`: `{"type":"origins:attribute","modifier":{"attribute":"minecraft:generic.max_health","operation":"addition","value":4.0,"name":"Frost Vitality"}}`
- `/armor`: `{"type":"origins:attribute","modifier":{"attribute":"minecraft:generic.armor","operation":"addition","value":1.0,"name":"Rimeplate"}}`
- `/cold_home`: `{"type":"runic_races:biome_affinity","name":"power.runic_races.frost_one.frostborn.name","description":"power.runic_races.frost_one.frostborn.description","home_biome_tag":"forge:is_cold","speed_bonus":0.06,"damage_bonus":0.05,"hostile_biome_tag":"forge:is_hot","speed_penalty":-0.06,"check_interval":40}`

### forged_for_cold

Authored description: +25% fire damage taken and -5% speed. Heat unmakes you.

Executed JSON inputs:

- `/fire`: `{"type":"origins:modify_damage_taken","damage_condition":{"type":"origins:or","conditions":[{"type":"origins:name","name":"inFire"},{"type":"origins:name","name":"onFire"},{"type":"origins:name","name":"lava"},{"type":"origins:name","name":"hotFloor"}]},"modifier":{"operation":"multiply_total_multiplicative","value":0.25}}`
- `/speed`: `{"type":"origins:attribute","modifier":{"attribute":"minecraft:generic.movement_speed","operation":"multiply_total","value":-0.05,"name":"Cold Limbs"}}`

## high_elf

### arcane_reflex

Authored description: Snap up an arcane shield: Absorption II and Resistance I for 5s, weakening nearby foes. 40-second cooldown.

Executed JSON inputs:

- `/cooldown_timer`: `{"type":"origins:resource","min":0,"max":800,"start_value":0,"hud_render":{"should_render":false,"sprite_location":"origins:textures/gui/community/spade.png","bar_index":2},"min_action":null,"max_action":null}`
- `/cooldown_decay/entity_action/condition`: `{"type":"origins:resource","resource":"runic_races:high_elf/arcane_reflex_cooldown_timer","comparison":">","compare_to":0}`
- `/active_ability/condition`: `{"type":"origins:resource","resource":"runic_races:high_elf/arcane_reflex_cooldown_timer","comparison":"==","compare_to":0}`
- `/active_ability/entity_action/actions/0`: `{"type":"origins:apply_effect","effect":{"effect":"minecraft:absorption","duration":100,"amplifier":1,"is_ambient":false,"show_particles":true,"show_icon":true}}`
- `/active_ability/entity_action/actions/1`: `{"type":"origins:apply_effect","effect":{"effect":"minecraft:resistance","duration":100,"amplifier":0,"is_ambient":false,"show_particles":true,"show_icon":true}}`
- `/active_ability/entity_action/actions/2`: `{"type":"runic_races:afflict_hostiles","radius":4,"effects":[{"effect":"minecraft:weakness","duration_ticks":60,"amplifier":0}]}`
- `/active_ability/entity_action/actions/3`: `{"type":"runic_races:signature_presentation","key":"HIGH_ELF_REFLEX"}`

### arcane_mastery

Authored description: +15% magic damage and night vision. Centuries of study made manifest.

Executed JSON inputs:

- `/magic`: `{"type":"origins:modify_damage_dealt","damage_condition":{"type":"origins:or","conditions":[{"type":"origins:name","name":"magic"},{"type":"origins:name","name":"indirectMagic"}]},"modifier":{"operation":"multiply_total_multiplicative","value":0.15}}`
- `/night_vision`: `{"type":"origins:night_vision","strength":1.0}`

### fragile_grace

Authored description: -2 hearts and +15% physical damage taken. Grace bought with fragility.

Executed JSON inputs:

- `/health`: `{"type":"origins:attribute","modifier":{"attribute":"minecraft:generic.max_health","operation":"addition","value":-4.0,"name":"Fragile Health"}}`
- `/phys`: `{"type":"origins:modify_damage_taken","damage_condition":{"type":"origins:or","conditions":[{"type":"origins:name","name":"player"},{"type":"origins:name","name":"mob"},{"type":"origins:name","name":"arrow"},{"type":"origins:name","name":"trident"},{"type":"origins:name","name":"thrown"},{"type":"origins:name","name":"sting"},{"type":"origins:name","name":"mob_projectile"}]},"modifier":{"operation":"multiply_total_multiplicative","value":0.15}}`

## ice_drake

### frost_breath

Authored description: Exhale a cone of killing frost that freezes and slows. 40-second cooldown.

Executed JSON inputs:

- `/cooldown_timer`: `{"type":"origins:resource","min":0,"max":800,"start_value":0,"hud_render":{"should_render":false,"sprite_location":"origins:textures/gui/community/spade.png","bar_index":2},"min_action":null,"max_action":null}`
- `/cooldown_decay/entity_action/condition`: `{"type":"origins:resource","resource":"runic_races:ice_drake/frost_breath_cooldown_timer","comparison":">","compare_to":0}`
- `/active_ability/condition`: `{"type":"origins:resource","resource":"runic_races:ice_drake/frost_breath_cooldown_timer","comparison":"==","compare_to":0}`
- `/active_ability/entity_action/actions/0`: `{"type":"runic_races:cone_breath","range":7.0,"half_angle_degrees":22.0,"damage":6.0,"fire_seconds":0,"element":"frost"}`
- `/active_ability/entity_action/actions/1`: `{"type":"runic_races:signature_presentation","key":"ICE_DRAKE_BREATH"}`

### rimescale_hide

Authored description: Immune to freezing, +3 armor, gliding wings, and at home in the cold.

Executed JSON inputs:

- `/elytra_flight`: `{"type":"origins:elytra_flight","render_elytra":false}`
- `/freeze_imm`: `{"type":"origins:modify_damage_taken","damage_condition":{"type":"origins:name","name":"freeze"},"modifier":{"operation":"multiply_total_multiplicative","value":-1.0}}`
- `/armor`: `{"type":"origins:attribute","modifier":{"attribute":"minecraft:generic.armor","operation":"addition","value":3.0,"name":"Rimescale"}}`
- `/cold_home`: `{"type":"runic_races:biome_affinity","name":"power.runic_races.ice_drake.rimescale_hide.name","description":"power.runic_races.ice_drake.rimescale_hide.description","home_biome_tag":"forge:is_cold","speed_bonus":0.05,"damage_bonus":0.08,"hostile_biome_tag":"forge:is_hot","speed_penalty":-0.1,"check_interval":40}`

### thaw

Authored description: +30% fire damage taken and +20% hunger drain. Flame is your undoing.

Executed JSON inputs:

- `/fire`: `{"type":"origins:modify_damage_taken","damage_condition":{"type":"origins:or","conditions":[{"type":"origins:name","name":"inFire"},{"type":"origins:name","name":"onFire"},{"type":"origins:name","name":"lava"},{"type":"origins:name","name":"hotFloor"}]},"modifier":{"operation":"multiply_total_multiplicative","value":0.3}}`
- `/hunger`: `{"type":"origins:modify_exhaustion","modifier":{"operation":"multiply_total_multiplicative","value":0.2}}`

## ice_elf

### frostbind

Authored description: Loose a frost nova: foes within 6 blocks are gripped by Slowness III. 40-second cooldown.

Executed JSON inputs:

- `/cooldown_timer`: `{"type":"origins:resource","min":0,"max":800,"start_value":0,"hud_render":{"should_render":false,"sprite_location":"origins:textures/gui/community/spade.png","bar_index":2},"min_action":null,"max_action":null}`
- `/cooldown_decay/entity_action/condition`: `{"type":"origins:resource","resource":"runic_races:ice_elf/frostbind_cooldown_timer","comparison":">","compare_to":0}`
- `/active_ability/condition`: `{"type":"origins:resource","resource":"runic_races:ice_elf/frostbind_cooldown_timer","comparison":"==","compare_to":0}`
- `/active_ability/entity_action/actions/0`: `{"type":"runic_races:afflict_hostiles","radius":6,"effects":[{"effect":"minecraft:slowness","duration_ticks":120,"amplifier":2}]}`
- `/active_ability/entity_action/actions/1`: `{"type":"origins:apply_effect","effect":{"effect":"minecraft:speed","duration":80,"amplifier":0,"is_ambient":false,"show_particles":true,"show_icon":true}}`
- `/active_ability/entity_action/actions/2`: `{"type":"runic_races:signature_presentation","key":"ICE_ELF_FROSTBIND"}`

### winters_child

Authored description: Immune to freezing and +10% magic damage; the cold is your home.

Executed JSON inputs:

- `/freeze_imm`: `{"type":"origins:modify_damage_taken","damage_condition":{"type":"origins:name","name":"freeze"},"modifier":{"operation":"multiply_total_multiplicative","value":-1.0}}`
- `/magic`: `{"type":"origins:modify_damage_dealt","damage_condition":{"type":"origins:or","conditions":[{"type":"origins:name","name":"magic"},{"type":"origins:name","name":"indirectMagic"}]},"modifier":{"operation":"multiply_total_multiplicative","value":0.1}}`
- `/cold_home`: `{"type":"runic_races:biome_affinity","name":"power.runic_races.ice_elf.winters_child.name","description":"power.runic_races.ice_elf.winters_child.description","home_biome_tag":"forge:is_cold","speed_bonus":0.06,"damage_bonus":0.05,"hostile_biome_tag":"forge:is_hot","speed_penalty":-0.06,"check_interval":40}`

### cold_blooded

Authored description: +25% fire damage taken. Heat is your undoing.

Executed JSON inputs:

- `/fire`: `{"type":"origins:modify_damage_taken","damage_condition":{"type":"origins:or","conditions":[{"type":"origins:name","name":"inFire"},{"type":"origins:name","name":"onFire"},{"type":"origins:name","name":"lava"},{"type":"origins:name","name":"hotFloor"},{"type":"origins:name","name":"fireball"}]},"modifier":{"operation":"multiply_total_multiplicative","value":0.25}}`

## iron_one

### shield_wall

Authored description: Raise an unbreakable guard: Resistance III and Absorption II for 6s. 45-second cooldown.

Executed JSON inputs:

- `/cooldown_timer`: `{"type":"origins:resource","min":0,"max":900,"start_value":0,"hud_render":{"should_render":false,"sprite_location":"origins:textures/gui/community/spade.png","bar_index":2},"min_action":null,"max_action":null}`
- `/cooldown_decay/entity_action/condition`: `{"type":"origins:resource","resource":"runic_races:iron_one/shield_wall_cooldown_timer","comparison":">","compare_to":0}`
- `/active_ability/condition`: `{"type":"origins:resource","resource":"runic_races:iron_one/shield_wall_cooldown_timer","comparison":"==","compare_to":0}`
- `/active_ability/entity_action/actions/0`: `{"type":"origins:apply_effect","effect":{"effect":"minecraft:resistance","duration":120,"amplifier":2,"is_ambient":false,"show_particles":true,"show_icon":true}}`
- `/active_ability/entity_action/actions/1`: `{"type":"origins:apply_effect","effect":{"effect":"minecraft:absorption","duration":120,"amplifier":1,"is_ambient":false,"show_particles":true,"show_icon":true}}`
- `/active_ability/entity_action/actions/2`: `{"type":"runic_races:signature_presentation","key":"IRON_ONE_SHIELD_WALL"}`

### forged_to_endure

Authored description: +2 hearts, +3 armor, and strong knockback resistance.

Executed JSON inputs:

- `/health`: `{"type":"origins:attribute","modifier":{"attribute":"minecraft:generic.max_health","operation":"addition","value":4.0,"name":"Iron Vitality"}}`
- `/armor`: `{"type":"origins:attribute","modifier":{"attribute":"minecraft:generic.armor","operation":"addition","value":3.0,"name":"Fortress Plate"}}`
- `/kb`: `{"type":"origins:attribute","modifier":{"attribute":"minecraft:generic.knockback_resistance","operation":"addition","value":0.5,"name":"Immovable"}}`

### heavy_as_the_mountain

Authored description: -10% movement speed, -10% attack speed, and -10% magic damage.

Executed JSON inputs:

- `/speed`: `{"type":"origins:attribute","modifier":{"attribute":"minecraft:generic.movement_speed","operation":"multiply_total","value":-0.1,"name":"Mountain Weight"}}`
- `/attack_speed`: `{"type":"origins:attribute","modifier":{"attribute":"minecraft:generic.attack_speed","operation":"multiply_total","value":-0.1,"name":"Ponderous Swing"}}`
- `/magic`: `{"type":"origins:modify_damage_dealt","damage_condition":{"type":"origins:or","conditions":[{"type":"origins:name","name":"magic"},{"type":"origins:name","name":"indirectMagic"}]},"modifier":{"operation":"multiply_total_multiplicative","value":-0.1}}`

## kitsune

### foxfire_illusion

Authored description: Vanish in foxfire: gain Invisibility and Speed II while nearby foes are blinded and slowed. 40-second cooldown.

Executed JSON inputs:

- `/cooldown_timer`: `{"type":"origins:resource","min":0,"max":800,"start_value":0,"hud_render":{"should_render":false,"sprite_location":"origins:textures/gui/community/spade.png","bar_index":2},"min_action":null,"max_action":null}`
- `/cooldown_decay/entity_action/condition`: `{"type":"origins:resource","resource":"runic_races:kitsune/foxfire_illusion_cooldown_timer","comparison":">","compare_to":0}`
- `/active_ability/condition`: `{"type":"origins:resource","resource":"runic_races:kitsune/foxfire_illusion_cooldown_timer","comparison":"==","compare_to":0}`
- `/active_ability/entity_action/actions/0`: `{"type":"origins:apply_effect","effect":{"effect":"minecraft:invisibility","duration":120,"amplifier":0,"is_ambient":false,"show_particles":false,"show_icon":true}}`
- `/active_ability/entity_action/actions/1`: `{"type":"origins:apply_effect","effect":{"effect":"minecraft:speed","duration":120,"amplifier":1,"is_ambient":false,"show_particles":false,"show_icon":true}}`
- `/active_ability/entity_action/actions/2`: `{"type":"runic_races:afflict_hostiles","radius":5,"effects":[{"effect":"minecraft:blindness","duration_ticks":80,"amplifier":0},{"effect":"minecraft:slowness","duration_ticks":80,"amplifier":1}]}`
- `/active_ability/entity_action/actions/3`: `{"type":"runic_races:signature_presentation","key":"KITSUNE_FOXFIRE"}`

### spirit_of_the_fox

Authored description: +15% magic damage, night vision, and +10% speed.

Executed JSON inputs:

- `/magic`: `{"type":"origins:modify_damage_dealt","damage_condition":{"type":"origins:or","conditions":[{"type":"origins:name","name":"magic"},{"type":"origins:name","name":"indirectMagic"}]},"modifier":{"operation":"multiply_total_multiplicative","value":0.15}}`
- `/night_vision`: `{"type":"origins:night_vision","strength":1.0}`
- `/speed`: `{"type":"origins:attribute","modifier":{"attribute":"minecraft:generic.movement_speed","operation":"multiply_total","value":0.1,"name":"Fox Step"}}`

### untamed_spirit

Authored description: -2 hearts, and +20% damage taken under the open daytime sky. A night spirit, exposed by daylight.

Executed JSON inputs:

- `/health`: `{"type":"origins:attribute","modifier":{"attribute":"minecraft:generic.max_health","operation":"addition","value":-4.0,"name":"Spirit Frailty"}}`
- `/daylight`: `{"type":"origins:modify_damage_taken","condition":{"type":"origins:and","conditions":[{"type":"origins:exposed_to_sun"},{"type":"origins:daytime"}]},"modifier":{"operation":"multiply_total_multiplicative","value":0.2}}`

## magi

### arcane_overflow

Authored description: Release an arcane nova: enemies within 6 blocks are Weakened and revealed while you gain Strength. Spends 30 mana if Iron's Spellbooks is present. 45-second cooldown.

Executed JSON inputs:

- `/cooldown_timer`: `{"type":"origins:resource","min":0,"max":900,"start_value":0,"hud_render":{"should_render":false,"sprite_location":"origins:textures/gui/community/spade.png","bar_index":2},"min_action":null,"max_action":null}`
- `/cooldown_decay/entity_action/condition`: `{"type":"origins:resource","resource":"runic_races:magi/arcane_overflow_cooldown_timer","comparison":">","compare_to":0}`
- `/active_ability/condition`: `{"type":"origins:resource","resource":"runic_races:magi/arcane_overflow_cooldown_timer","comparison":"==","compare_to":0}`
- `/active_ability/entity_action/condition/conditions/0/condition`: `{"type":"runic_races:resource_available","resource":"mana"}`
- `/active_ability/entity_action/condition/conditions/1`: `{"type":"runic_races:has_mana","amount":30}`
- `/active_ability/entity_action/if_action/actions/0`: `{"type":"runic_races:consume_mana","amount":30}`
- `/active_ability/entity_action/if_action/actions/1`: `{"type":"origins:apply_effect","effect":{"effect":"minecraft:strength","duration":160,"amplifier":1,"is_ambient":false,"show_particles":true,"show_icon":true}}`
- `/active_ability/entity_action/if_action/actions/2`: `{"type":"runic_races:afflict_hostiles","radius":6,"effects":[{"effect":"minecraft:weakness","duration_ticks":120,"amplifier":1},{"effect":"minecraft:glowing","duration_ticks":120,"amplifier":0}]}`
- `/active_ability/entity_action/if_action/actions/3`: `{"type":"runic_races:signature_presentation","key":"MAGI_OVERFLOW"}`
- `/active_ability/entity_action/else_action`: `{"type":"runic_races:show_banner","translation_key":"message.runic_races.ability.no_mana","color":"red","bold":true,"learning_hint":"message.runic_races.learning.arcane_overflow_mana"}`

### woven_of_magic

Authored description: +15% magic damage. Your body is suffused with arcane power.

Executed JSON inputs:

- `/magic`: `{"type":"origins:modify_damage_dealt","damage_condition":{"type":"origins:or","conditions":[{"type":"origins:name","name":"magic"},{"type":"origins:name","name":"indirectMagic"}]},"modifier":{"operation":"multiply_total_multiplicative","value":0.15}}`

### volatile_vessel

Authored description: -2 hearts and +20% physical damage taken. A frail magical frame.

Executed JSON inputs:

- `/health`: `{"type":"origins:attribute","modifier":{"attribute":"minecraft:generic.max_health","operation":"addition","value":-4.0,"name":"Volatile Health"}}`
- `/phys`: `{"type":"origins:modify_damage_taken","damage_condition":{"type":"origins:or","conditions":[{"type":"origins:name","name":"player"},{"type":"origins:name","name":"mob"},{"type":"origins:name","name":"arrow"}]},"modifier":{"operation":"multiply_total_multiplicative","value":0.2}}`

## moon_elf

### moonlit_veil

Authored description: Draw a veil of moonlight: Invisibility, Slow Falling, and Regeneration for several seconds, healing you. 50-second cooldown.

Executed JSON inputs:

- `/cooldown_timer`: `{"type":"origins:resource","min":0,"max":1000,"start_value":0,"hud_render":{"should_render":false,"sprite_location":"origins:textures/gui/community/spade.png","bar_index":2},"min_action":null,"max_action":null}`
- `/cooldown_decay/entity_action/condition`: `{"type":"origins:resource","resource":"runic_races:moon_elf/moonlit_veil_cooldown_timer","comparison":">","compare_to":0}`
- `/active_ability/condition`: `{"type":"origins:resource","resource":"runic_races:moon_elf/moonlit_veil_cooldown_timer","comparison":"==","compare_to":0}`
- `/active_ability/entity_action/actions/0`: `{"type":"origins:apply_effect","effect":{"effect":"minecraft:invisibility","duration":120,"amplifier":0,"is_ambient":false,"show_particles":false,"show_icon":true}}`
- `/active_ability/entity_action/actions/1`: `{"type":"origins:apply_effect","effect":{"effect":"minecraft:slow_falling","duration":200,"amplifier":0,"is_ambient":false,"show_particles":true,"show_icon":true}}`
- `/active_ability/entity_action/actions/2`: `{"type":"origins:apply_effect","effect":{"effect":"minecraft:regeneration","duration":100,"amplifier":0,"is_ambient":false,"show_particles":true,"show_icon":true}}`
- `/active_ability/entity_action/actions/3`: `{"type":"origins:heal","amount":4.0}`
- `/active_ability/entity_action/actions/4`: `{"type":"runic_races:signature_presentation","key":"MOON_ELF_VEIL"}`

### tidecallers_grace

Authored description: Water breathing, night vision, and stronger natural healing under the night sky.

Executed JSON inputs:

- `/night_vision`: `{"type":"origins:night_vision","strength":1.0}`
- `/night_regen/entity_action`: `{"type":"origins:heal","amount":1.0}`

### waning_by_day

Authored description: -1.5 hearts and -10% magic damage; your power wanes under the daytime sun.

Executed JSON inputs:

- `/health`: `{"type":"origins:attribute","modifier":{"attribute":"minecraft:generic.max_health","operation":"addition","value":-3.0,"name":"Waning Health"}}`
- `/magic`: `{"type":"origins:modify_damage_dealt","damage_condition":{"type":"origins:or","conditions":[{"type":"origins:name","name":"magic"},{"type":"origins:name","name":"indirectMagic"}]},"modifier":{"operation":"multiply_total_multiplicative","value":-0.1}}`
- `/day_weak`: `{"type":"origins:conditioned_attribute","modifier":{"attribute":"minecraft:generic.attack_damage","operation":"multiply_total","value":-0.08,"name":"Daylight Waning"},"condition":{"type":"origins:and","conditions":[{"type":"origins:exposed_to_sun"},{"type":"origins:daytime"}]}}`

## nymph

### sirens_charm

Authored description: Sing an enthralling charm: nearby foes are Slowed and Weakened while you mend with Regeneration. 45-second cooldown.

Executed JSON inputs:

- `/cooldown_timer`: `{"type":"origins:resource","min":0,"max":900,"start_value":0,"hud_render":{"should_render":false,"sprite_location":"origins:textures/gui/community/spade.png","bar_index":2},"min_action":null,"max_action":null}`
- `/cooldown_decay/entity_action/condition`: `{"type":"origins:resource","resource":"runic_races:nymph/sirens_charm_cooldown_timer","comparison":">","compare_to":0}`
- `/active_ability/condition`: `{"type":"origins:resource","resource":"runic_races:nymph/sirens_charm_cooldown_timer","comparison":"==","compare_to":0}`
- `/active_ability/entity_action/actions/0`: `{"type":"runic_races:afflict_hostiles","radius":8,"effects":[{"effect":"minecraft:slowness","duration_ticks":160,"amplifier":1},{"effect":"minecraft:weakness","duration_ticks":160,"amplifier":0}]}`
- `/active_ability/entity_action/actions/1`: `{"type":"origins:apply_effect","effect":{"effect":"minecraft:regeneration","duration":120,"amplifier":0,"is_ambient":false,"show_particles":true,"show_icon":true}}`
- `/active_ability/entity_action/actions/2`: `{"type":"origins:heal","amount":2.0}`
- `/active_ability/entity_action/actions/3`: `{"type":"runic_races:signature_presentation","key":"NYMPH_CHARM"}`

### spirit_of_spring

Authored description: Water breathing, +10% magic damage, and at home in and near the water.

Executed JSON inputs:

- `/magic`: `{"type":"origins:modify_damage_dealt","damage_condition":{"type":"origins:or","conditions":[{"type":"origins:name","name":"magic"},{"type":"origins:name","name":"indirectMagic"}]},"modifier":{"operation":"multiply_total_multiplicative","value":0.1}}`
- `/water_home`: `{"type":"runic_races:biome_affinity","name":"power.runic_races.nymph.spirit_of_spring.name","description":"power.runic_races.nymph.spirit_of_spring.description","home_biome_tag":"forge:is_water","speed_bonus":0.06,"damage_bonus":0.05,"hostile_biome_tag":"forge:is_hot","speed_penalty":-0.1,"check_interval":40}`

### bound_to_water

Authored description: -1.5 hearts and +20% fire damage taken; away from water, far from home, you wither.

Executed JSON inputs:

- `/health`: `{"type":"origins:attribute","modifier":{"attribute":"minecraft:generic.max_health","operation":"addition","value":-3.0,"name":"Spring-Bound"}}`
- `/fire`: `{"type":"origins:modify_damage_taken","damage_condition":{"type":"origins:or","conditions":[{"type":"origins:name","name":"inFire"},{"type":"origins:name","name":"onFire"},{"type":"origins:name","name":"lava"},{"type":"origins:name","name":"hotFloor"}]},"modifier":{"operation":"multiply_total_multiplicative","value":0.2}}`

## primian

### stroke_of_fortune

Authored description: Fortune surges: Luck II for 10s, Absorption I and Speed I for 8s. 100-second cooldown.

Executed JSON inputs:

- `/cooldown_timer`: `{"type":"origins:resource","min":0,"max":2000,"start_value":0,"hud_render":{"should_render":false,"sprite_location":"origins:textures/gui/community/spade.png","bar_index":2},"min_action":null,"max_action":null}`
- `/cooldown_decay/entity_action/condition`: `{"type":"origins:resource","resource":"runic_races:primian/stroke_of_fortune_cooldown_timer","comparison":">","compare_to":0}`
- `/active_ability/condition`: `{"type":"origins:resource","resource":"runic_races:primian/stroke_of_fortune_cooldown_timer","comparison":"==","compare_to":0}`
- `/active_ability/entity_action/actions/0`: `{"type":"origins:apply_effect","effect":{"effect":"minecraft:luck","duration":200,"amplifier":1,"is_ambient":false,"show_particles":true,"show_icon":true}}`
- `/active_ability/entity_action/actions/1`: `{"type":"origins:apply_effect","effect":{"effect":"minecraft:absorption","duration":160,"amplifier":0,"is_ambient":false,"show_particles":true,"show_icon":true}}`
- `/active_ability/entity_action/actions/2`: `{"type":"origins:apply_effect","effect":{"effect":"minecraft:speed","duration":160,"amplifier":0,"is_ambient":false,"show_particles":true,"show_icon":true}}`
- `/active_ability/entity_action/actions/3`: `{"type":"runic_races:signature_presentation","key":"PRIMIAN_FORTUNE"}`

### boundless_adaptability

Authored description: +1 heart, +1 Luck, +5% movement speed. You adapt to your surroundings, gaining brief speed as you explore new places.

Executed JSON inputs:

- `/health`: `{"type":"origins:attribute","modifier":{"attribute":"minecraft:generic.max_health","operation":"addition","value":2.0,"name":"Primian Vitality"}}`
- `/luck`: `{"type":"origins:attribute","modifier":{"attribute":"minecraft:generic.luck","operation":"addition","value":1.0,"name":"Primian Fortune"}}`
- `/speed`: `{"type":"origins:attribute","modifier":{"attribute":"minecraft:generic.movement_speed","operation":"multiply_total","value":0.05,"name":"Primian Swiftness"}}`

### jack_of_all_trades

Authored description: Master of none: -10% magic damage and -5% melee damage. You hold no elemental affinity.

Executed JSON inputs:

- `/magic_penalty`: `{"type":"origins:modify_damage_dealt","damage_condition":{"type":"origins:or","conditions":[{"type":"origins:name","name":"magic"},{"type":"origins:name","name":"indirectMagic"}]},"modifier":{"operation":"multiply_total_multiplicative","value":-0.1}}`
- `/melee_penalty`: `{"type":"origins:attribute","modifier":{"attribute":"minecraft:generic.attack_damage","operation":"multiply_total","value":-0.05,"name":"Generalist Melee Penalty"}}`

## reaper

### soul_harvest

Authored description: Sweep your scythe: nearby foes are Withered and revealed while their souls mend you. 40-second cooldown.

Executed JSON inputs:

- `/cooldown_timer`: `{"type":"origins:resource","min":0,"max":800,"start_value":0,"hud_render":{"should_render":false,"sprite_location":"origins:textures/gui/community/spade.png","bar_index":2},"min_action":null,"max_action":null}`
- `/cooldown_decay/entity_action/condition`: `{"type":"origins:resource","resource":"runic_races:reaper/soul_harvest_cooldown_timer","comparison":">","compare_to":0}`
- `/active_ability/condition`: `{"type":"origins:resource","resource":"runic_races:reaper/soul_harvest_cooldown_timer","comparison":"==","compare_to":0}`
- `/active_ability/entity_action/actions/0`: `{"type":"runic_races:afflict_hostiles","radius":6,"effects":[{"effect":"minecraft:wither","duration_ticks":100,"amplifier":1},{"effect":"minecraft:glowing","duration_ticks":100,"amplifier":0}]}`
- `/active_ability/entity_action/actions/1`: `{"type":"origins:heal","amount":4.0}`
- `/active_ability/entity_action/actions/2`: `{"type":"origins:apply_effect","effect":{"effect":"minecraft:regeneration","duration":100,"amplifier":0,"is_ambient":false,"show_particles":true,"show_icon":true}}`
- `/active_ability/entity_action/actions/3`: `{"type":"runic_races:signature_presentation","key":"REAPER_HARVEST"}`

### harbinger_of_death

Authored description: Immune to Wither, night vision, and +15% melee damage. Death once cheated returns you to the world (every 30 minutes).

Executed JSON inputs:

- `/wither_imm`: `{"type":"origins:effect_immunity","effect":"minecraft:wither"}`
- `/night_vision`: `{"type":"origins:night_vision","strength":1.0}`
- `/melee`: `{"type":"origins:attribute","modifier":{"attribute":"minecraft:generic.attack_damage","operation":"multiply_total","value":0.15,"name":"Reaper's Edge"}}`

### touch_of_the_grave

Authored description: -2 hearts, -50% natural healing, and weakened in direct sunlight.

Executed JSON inputs:

- `/health`: `{"type":"origins:attribute","modifier":{"attribute":"minecraft:generic.max_health","operation":"addition","value":-4.0,"name":"Grave-Touched"}}`
- `/healing`: `{"type":"origins:modify_healing","modifier":{"operation":"multiply_total_multiplicative","value":-0.5}}`
- `/sun_weak`: `{"type":"origins:conditioned_attribute","modifier":{"attribute":"minecraft:generic.attack_damage","operation":"multiply_total","value":-0.15,"name":"Sun-Shunned"},"condition":{"type":"origins:and","conditions":[{"type":"origins:exposed_to_sun"},{"type":"origins:daytime"}]}}`

## runic_one

### rune_of_warding

Authored description: Inscribe a ward of stone: Resistance and Regeneration for you while nearby foes are slowed. 60-second cooldown.

Executed JSON inputs:

- `/cooldown_timer`: `{"type":"origins:resource","min":0,"max":1200,"start_value":0,"hud_render":{"should_render":false,"sprite_location":"origins:textures/gui/community/spade.png","bar_index":2},"min_action":null,"max_action":null}`
- `/cooldown_decay/entity_action/condition`: `{"type":"origins:resource","resource":"runic_races:runic_one/rune_of_warding_cooldown_timer","comparison":">","compare_to":0}`
- `/active_ability/condition`: `{"type":"origins:resource","resource":"runic_races:runic_one/rune_of_warding_cooldown_timer","comparison":"==","compare_to":0}`
- `/active_ability/entity_action/actions/0`: `{"type":"origins:apply_effect","effect":{"effect":"minecraft:resistance","duration":160,"amplifier":1,"is_ambient":false,"show_particles":true,"show_icon":true}}`
- `/active_ability/entity_action/actions/1`: `{"type":"origins:apply_effect","effect":{"effect":"minecraft:regeneration","duration":160,"amplifier":0,"is_ambient":false,"show_particles":true,"show_icon":true}}`
- `/active_ability/entity_action/actions/2`: `{"type":"runic_races:afflict_hostiles","radius":6,"effects":[{"effect":"minecraft:slowness","duration_ticks":80,"amplifier":1}]}`
- `/active_ability/entity_action/actions/3`: `{"type":"runic_races:signature_presentation","key":"RUNIC_WARD"}`

### runebound

Authored description: +10% magic damage, +2 armor, and +1 Luck for enchanting fortune.

Executed JSON inputs:

- `/magic`: `{"type":"origins:modify_damage_dealt","damage_condition":{"type":"origins:or","conditions":[{"type":"origins:name","name":"magic"},{"type":"origins:name","name":"indirectMagic"}]},"modifier":{"operation":"multiply_total_multiplicative","value":0.1}}`
- `/armor`: `{"type":"origins:attribute","modifier":{"attribute":"minecraft:generic.armor","operation":"addition","value":2.0,"name":"Runed Plate"}}`
- `/luck`: `{"type":"origins:attribute","modifier":{"attribute":"minecraft:generic.luck","operation":"addition","value":1.0,"name":"Rune Fortune"}}`

### bound_by_tradition

Authored description: -2 hearts and -10% speed. A scholar, not a warrior.

Executed JSON inputs:

- `/health`: `{"type":"origins:attribute","modifier":{"attribute":"minecraft:generic.max_health","operation":"addition","value":-4.0,"name":"Scholar's Frailty"}}`
- `/speed`: `{"type":"origins:attribute","modifier":{"attribute":"minecraft:generic.movement_speed","operation":"multiply_total","value":-0.1,"name":"Ponderous Study"}}`

## sea_serpen

### tidal_breath

Authored description: Unleash a crushing tide that hurls foes back and drowns them in slowness. 40-second cooldown.

Executed JSON inputs:

- `/cooldown_timer`: `{"type":"origins:resource","min":0,"max":800,"start_value":0,"hud_render":{"should_render":false,"sprite_location":"origins:textures/gui/community/spade.png","bar_index":2},"min_action":null,"max_action":null}`
- `/cooldown_decay/entity_action/condition`: `{"type":"origins:resource","resource":"runic_races:sea_serpen/tidal_breath_cooldown_timer","comparison":">","compare_to":0}`
- `/active_ability/condition`: `{"type":"origins:resource","resource":"runic_races:sea_serpen/tidal_breath_cooldown_timer","comparison":"==","compare_to":0}`
- `/active_ability/entity_action/actions/0`: `{"type":"runic_races:cone_breath","range":7.0,"half_angle_degrees":25.0,"damage":6.0,"fire_seconds":0,"element":"water"}`
- `/active_ability/entity_action/actions/1`: `{"type":"runic_races:signature_presentation","key":"SEA_SERPEN_BREATH"}`

### leviathans_gift

Authored description: Water breathing, +3 armor, and supreme power in the open ocean.

Executed JSON inputs:

- `/armor`: `{"type":"origins:attribute","modifier":{"attribute":"minecraft:generic.armor","operation":"addition","value":3.0,"name":"Serpent Scale"}}`
- `/ocean_home`: `{"type":"runic_races:biome_affinity","name":"power.runic_races.sea_serpen.leviathans_gift.name","description":"power.runic_races.sea_serpen.leviathans_gift.description","home_biome_tag":"forge:is_water","speed_bonus":0.08,"damage_bonus":0.1,"hostile_biome_tag":"forge:is_hot","speed_penalty":-0.05,"check_interval":40}`

### landbound_coils

Authored description: -1 heart and +20% fire damage taken; far from water your coils grow sluggish.

Executed JSON inputs:

- `/health`: `{"type":"origins:attribute","modifier":{"attribute":"minecraft:generic.max_health","operation":"addition","value":-2.0,"name":"Landbound"}}`
- `/fire`: `{"type":"origins:modify_damage_taken","damage_condition":{"type":"origins:or","conditions":[{"type":"origins:name","name":"inFire"},{"type":"origins:name","name":"onFire"},{"type":"origins:name","name":"lava"},{"type":"origins:name","name":"hotFloor"}]},"modifier":{"operation":"multiply_total_multiplicative","value":0.2}}`
- `/dry_slow`: `{"type":"origins:conditioned_attribute","modifier":{"attribute":"minecraft:generic.movement_speed","operation":"multiply_total","value":-0.1,"name":"Beached"},"condition":{"type":"origins:invert","condition":{"type":"origins:submerged_in","fluid":"minecraft:water"}}}`

## serpen

### shed_skin

Authored description: Slither free: cleanse all harmful effects and gain Invisibility and Speed for 5s. 45-second cooldown.

Executed JSON inputs:

- `/cooldown_timer`: `{"type":"origins:resource","min":0,"max":900,"start_value":0,"hud_render":{"should_render":false,"sprite_location":"origins:textures/gui/community/spade.png","bar_index":2},"min_action":null,"max_action":null}`
- `/cooldown_decay/entity_action/condition`: `{"type":"origins:resource","resource":"runic_races:serpen/shed_skin_cooldown_timer","comparison":">","compare_to":0}`
- `/active_ability/condition`: `{"type":"origins:resource","resource":"runic_races:serpen/shed_skin_cooldown_timer","comparison":"==","compare_to":0}`
- `/active_ability/entity_action/actions/0`: `{"type":"runic_races:clear_effects_by_category","category":"harmful"}`
- `/active_ability/entity_action/actions/1`: `{"type":"origins:apply_effect","effect":{"effect":"minecraft:invisibility","duration":100,"amplifier":0,"is_ambient":false,"show_particles":false,"show_icon":true}}`
- `/active_ability/entity_action/actions/2`: `{"type":"origins:apply_effect","effect":{"effect":"minecraft:speed","duration":100,"amplifier":0,"is_ambient":false,"show_particles":false,"show_icon":true}}`
- `/active_ability/entity_action/actions/3`: `{"type":"runic_races:signature_presentation","key":"SERPEN_SHED"}`

### venomous_coil

Authored description: Poison immunity, venomous fangs, +10% speed, night vision, and thriving in the heat.

Executed JSON inputs:

- `/poison_imm`: `{"type":"origins:effect_immunity","effect":"minecraft:poison"}`
- `/speed`: `{"type":"origins:attribute","modifier":{"attribute":"minecraft:generic.movement_speed","operation":"multiply_total","value":0.1,"name":"Coiled Speed"}}`
- `/night_vision`: `{"type":"origins:night_vision","strength":1.0}`
- `/hot_home`: `{"type":"runic_races:biome_affinity","name":"power.runic_races.serpen.venomous_coil.name","description":"power.runic_races.serpen.venomous_coil.description","home_biome_tag":"forge:is_hot","speed_bonus":0.06,"damage_bonus":0.05,"hostile_biome_tag":"forge:is_cold","speed_penalty":-0.06,"check_interval":40}`

### cold_blooded

Authored description: -1.5 hearts and +25% freezing damage; the cold dulls your blood.

Executed JSON inputs:

- `/health`: `{"type":"origins:attribute","modifier":{"attribute":"minecraft:generic.max_health","operation":"addition","value":-3.0,"name":"Lean Coils"}}`
- `/cold`: `{"type":"origins:modify_damage_taken","damage_condition":{"type":"origins:name","name":"freeze"},"modifier":{"operation":"multiply_total_multiplicative","value":0.25}}`

## skeleton

### conscript_the_dead

Authored description: Raise two skeletal servants to fight at your side for a time. 70-second cooldown.

Executed JSON inputs:

- `/cooldown_timer`: `{"type":"origins:resource","min":0,"max":1400,"start_value":0,"hud_render":{"should_render":false,"sprite_location":"origins:textures/gui/community/spade.png","bar_index":2},"min_action":null,"max_action":null}`
- `/cooldown_decay/entity_action/condition`: `{"type":"origins:resource","resource":"runic_races:skeleton/conscript_the_dead_cooldown_timer","comparison":">","compare_to":0}`
- `/active_ability/condition`: `{"type":"origins:resource","resource":"runic_races:skeleton/conscript_the_dead_cooldown_timer","comparison":"==","compare_to":0}`
- `/active_ability/entity_action/actions/0`: `{"type":"runic_races:summon_minion","entity":"runic_races:grave_servant","count":2,"duration_ticks":1200,"radius":2.5}`
- `/active_ability/entity_action/actions/1`: `{"type":"runic_races:signature_presentation","key":"SKELETON_CONSCRIPT"}`

### marrow_deep_aim

Authored description: +10% attack speed, immune to Poison and Hunger, and you need no breath.

Executed JSON inputs:

- `/attack_speed`: `{"type":"origins:attribute","modifier":{"attribute":"minecraft:generic.attack_speed","operation":"multiply_total","value":0.1,"name":"Bony Precision"}}`
- `/poison_imm`: `{"type":"origins:effect_immunity","effect":"minecraft:poison"}`
- `/hunger_imm`: `{"type":"origins:effect_immunity","effect":"minecraft:hunger"}`

### brittle_bones

Authored description: -2 hearts, +25% fall damage, and you take more harm in direct sunlight.

Executed JSON inputs:

- `/health`: `{"type":"origins:attribute","modifier":{"attribute":"minecraft:generic.max_health","operation":"addition","value":-4.0,"name":"Brittle Frame"}}`
- `/fall`: `{"type":"origins:modify_damage_taken","damage_condition":{"type":"origins:name","name":"fall"},"modifier":{"operation":"multiply_total_multiplicative","value":0.25}}`
- `/sun`: `{"type":"origins:modify_damage_taken","condition":{"type":"origins:and","conditions":[{"type":"origins:exposed_to_sun"},{"type":"origins:daytime"}]},"modifier":{"operation":"multiply_total_multiplicative","value":0.2}}`

## sky_one

### mountain_leap

Authored description: Bound high with Slow Falling and Speed for a safe landing. 20-second cooldown.

Executed JSON inputs:

- `/cooldown_timer`: `{"type":"origins:resource","min":0,"max":400,"start_value":0,"hud_render":{"should_render":false,"sprite_location":"origins:textures/gui/community/spade.png","bar_index":2},"min_action":null,"max_action":null}`
- `/cooldown_decay/entity_action/condition`: `{"type":"origins:resource","resource":"runic_races:sky_one/mountain_leap_cooldown_timer","comparison":">","compare_to":0}`
- `/active_ability/condition`: `{"type":"origins:resource","resource":"runic_races:sky_one/mountain_leap_cooldown_timer","comparison":"==","compare_to":0}`
- `/active_ability/entity_action/actions/0`: `{"type":"origins:add_velocity","x":0.0,"y":1.0,"z":0.4,"space":"local"}`
- `/active_ability/entity_action/actions/1`: `{"type":"origins:apply_effect","effect":{"effect":"minecraft:slow_falling","duration":120,"amplifier":0,"is_ambient":false,"show_particles":true,"show_icon":true}}`
- `/active_ability/entity_action/actions/2`: `{"type":"origins:apply_effect","effect":{"effect":"minecraft:speed","duration":80,"amplifier":0,"is_ambient":false,"show_particles":true,"show_icon":true}}`
- `/active_ability/entity_action/actions/3`: `{"type":"runic_races:signature_presentation","key":"SKY_ONE_LEAP"}`

### sure_footed_sentinel

Authored description: Immune to fall damage, +1 armor, and swift in the mountains.

Executed JSON inputs:

- `/no_fall`: `{"type":"origins:modify_damage_taken","damage_condition":{"type":"origins:name","name":"fall"},"modifier":{"operation":"multiply_total_multiplicative","value":-1.0}}`
- `/armor`: `{"type":"origins:attribute","modifier":{"attribute":"minecraft:generic.armor","operation":"addition","value":1.0,"name":"Sentinel Mail"}}`
- `/mountain_home`: `{"type":"runic_races:biome_affinity","name":"power.runic_races.sky_one.sure_footed_sentinel.name","description":"power.runic_races.sky_one.sure_footed_sentinel.description","home_biome_tag":"forge:is_mountain","speed_bonus":0.1,"damage_bonus":0.05,"check_interval":40}`

### thin_air_lungs

Authored description: -1 heart; enclosed underground you are slowed and take more harm.

Executed JSON inputs:

- `/health`: `{"type":"origins:attribute","modifier":{"attribute":"minecraft:generic.max_health","operation":"addition","value":-2.0,"name":"Thin-Air Health"}}`
- `/cave_slow`: `{"type":"origins:conditioned_attribute","modifier":{"attribute":"minecraft:generic.movement_speed","operation":"multiply_total","value":-0.1,"name":"Claustrophobia"},"condition":{"type":"origins:invert","condition":{"type":"origins:exposed_to_sky"}}}`
- `/cave_weak`: `{"type":"origins:modify_damage_taken","condition":{"type":"origins:invert","condition":{"type":"origins:exposed_to_sky"}},"modifier":{"operation":"multiply_total_multiplicative","value":0.1}}`

## sprite

### phase_shift

Authored description: Flicker out of danger: blink forward with Invisibility and Speed II. 90-second cooldown.

Executed JSON inputs:

- `/cooldown_timer`: `{"type":"origins:resource","min":0,"max":1800,"start_value":0,"hud_render":{"should_render":false,"sprite_location":"origins:textures/gui/community/spade.png","bar_index":2},"min_action":null,"max_action":null}`
- `/cooldown_decay/entity_action/condition`: `{"type":"origins:resource","resource":"runic_races:sprite/phase_shift_cooldown_timer","comparison":">","compare_to":0}`
- `/active_ability/condition`: `{"type":"origins:resource","resource":"runic_races:sprite/phase_shift_cooldown_timer","comparison":"==","compare_to":0}`
- `/active_ability/entity_action/actions/0`: `{"type":"origins:add_velocity","x":0.0,"y":0.4,"z":1.2,"space":"local"}`
- `/active_ability/entity_action/actions/1`: `{"type":"origins:apply_effect","effect":{"effect":"minecraft:invisibility","duration":60,"amplifier":0,"is_ambient":false,"show_particles":false,"show_icon":true}}`
- `/active_ability/entity_action/actions/2`: `{"type":"origins:apply_effect","effect":{"effect":"minecraft:speed","duration":100,"amplifier":1,"is_ambient":false,"show_particles":true,"show_icon":true}}`
- `/active_ability/entity_action/actions/3`: `{"type":"runic_races:signature_presentation","key":"SPRITE_PHASE"}`

### gossamer_wings

Authored description: Gossamer wings for gliding flight, Slow Falling, +20% speed, and +10% attack speed.

Executed JSON inputs:

- `/elytra_flight`: `{"type":"origins:elytra_flight","render_elytra":false}`
- `/flap_cooldown_timer`: `{"type":"origins:resource","min":0,"max":30,"start_value":0,"hud_render":{"should_render":false,"sprite_location":"origins:textures/gui/community/spade.png","bar_index":2},"min_action":null,"max_action":null}`
- `/flap_cooldown_decay/entity_action/condition`: `{"type":"origins:resource","resource":"runic_races:sprite/gossamer_wings_flap_cooldown_timer","comparison":">","compare_to":0}`
- `/slow_fall`: `{"type":"origins:modify_damage_taken","damage_condition":{"type":"origins:name","name":"fall"},"modifier":{"operation":"multiply_total_multiplicative","value":-1.0}}`
- `/speed`: `{"type":"origins:attribute","modifier":{"attribute":"minecraft:generic.movement_speed","operation":"multiply_total","value":0.2,"name":"Sprite Swiftness"}}`
- `/attack_speed`: `{"type":"origins:attribute","modifier":{"attribute":"minecraft:generic.attack_speed","operation":"multiply_total","value":0.1,"name":"Sprite Flurry"}}`

### fragile_essence

Authored description: -3 hearts and easily knocked from the air. A wisp of a body.

Executed JSON inputs:

- `/health`: `{"type":"origins:attribute","modifier":{"attribute":"minecraft:generic.max_health","operation":"addition","value":-6.0,"name":"Fragile Essence"}}`

## terra_drake

### seismic_breath

Authored description: Breathe the weight of the mountain: foes are battered, slowed, and shaken from cover. 40-second cooldown.

Executed JSON inputs:

- `/cooldown_timer`: `{"type":"origins:resource","min":0,"max":800,"start_value":0,"hud_render":{"should_render":false,"sprite_location":"origins:textures/gui/community/spade.png","bar_index":2},"min_action":null,"max_action":null}`
- `/cooldown_decay/entity_action/condition`: `{"type":"origins:resource","resource":"runic_races:terra_drake/seismic_breath_cooldown_timer","comparison":">","compare_to":0}`
- `/active_ability/condition`: `{"type":"origins:resource","resource":"runic_races:terra_drake/seismic_breath_cooldown_timer","comparison":"==","compare_to":0}`
- `/active_ability/entity_action/actions/0`: `{"type":"runic_races:cone_breath","range":6.0,"half_angle_degrees":28.0,"damage":7.0,"fire_seconds":0,"element":"earth"}`
- `/active_ability/entity_action/actions/1`: `{"type":"runic_races:tremor_ping","radius":14.0,"duration_ticks":40}`
- `/active_ability/entity_action/actions/2`: `{"type":"runic_races:signature_presentation","key":"TERRA_DRAKE_BREATH"}`

### stonescale_hide

Authored description: +4 armor, knockback immunity, Mining Fatigue immunity, faster mining, no fall damage, gliding wings, and at home in the mountains.

Executed JSON inputs:

- `/elytra_flight`: `{"type":"origins:elytra_flight","render_elytra":false}`
- `/armor`: `{"type":"origins:attribute","modifier":{"attribute":"minecraft:generic.armor","operation":"addition","value":4.0,"name":"Stonescale"}}`
- `/kb`: `{"type":"origins:attribute","modifier":{"attribute":"minecraft:generic.knockback_resistance","operation":"addition","value":1.0,"name":"Immovable Bulk"}}`
- `/mining_fatigue_imm`: `{"type":"origins:effect_immunity","effect":"minecraft:mining_fatigue"}`
- `/mining`: `{"type":"origins:modify_break_speed","modifier":{"operation":"multiply_total_multiplicative","value":0.15}}`
- `/no_fall`: `{"type":"origins:modify_damage_taken","damage_condition":{"type":"origins:name","name":"fall"},"modifier":{"operation":"multiply_total_multiplicative","value":-1.0}}`
- `/mountain_home`: `{"type":"runic_races:biome_affinity","name":"power.runic_races.terra_drake.stonescale_hide.name","description":"power.runic_races.terra_drake.stonescale_hide.description","home_biome_tag":"forge:is_mountain","speed_bonus":0.05,"damage_bonus":0.08,"check_interval":40}`

### ponderous

Authored description: -10% movement speed, -10% attack speed, and +20% hunger drain.

Executed JSON inputs:

- `/speed`: `{"type":"origins:attribute","modifier":{"attribute":"minecraft:generic.movement_speed","operation":"multiply_total","value":-0.1,"name":"Stone Weight"}}`
- `/attack_speed`: `{"type":"origins:attribute","modifier":{"attribute":"minecraft:generic.attack_speed","operation":"multiply_total","value":-0.1,"name":"Slow Swing"}}`
- `/hunger`: `{"type":"origins:modify_exhaustion","modifier":{"operation":"multiply_total_multiplicative","value":0.2}}`

## valen

### unbreakable_stand

Authored description: Plant yourself: Resistance III, Absorption II, and Slowness I for 6s. 60-second cooldown.

Executed JSON inputs:

- `/cooldown_timer`: `{"type":"origins:resource","min":0,"max":1200,"start_value":0,"hud_render":{"should_render":false,"sprite_location":"origins:textures/gui/community/spade.png","bar_index":2},"min_action":null,"max_action":null}`
- `/cooldown_decay/entity_action/condition`: `{"type":"origins:resource","resource":"runic_races:valen/unbreakable_stand_cooldown_timer","comparison":">","compare_to":0}`
- `/active_ability/condition`: `{"type":"origins:resource","resource":"runic_races:valen/unbreakable_stand_cooldown_timer","comparison":"==","compare_to":0}`
- `/active_ability/entity_action/actions/0`: `{"type":"origins:apply_effect","effect":{"effect":"minecraft:resistance","duration":120,"amplifier":2,"is_ambient":false,"show_particles":true,"show_icon":true}}`
- `/active_ability/entity_action/actions/1`: `{"type":"origins:apply_effect","effect":{"effect":"minecraft:absorption","duration":120,"amplifier":1,"is_ambient":false,"show_particles":true,"show_icon":true}}`
- `/active_ability/entity_action/actions/2`: `{"type":"origins:apply_effect","effect":{"effect":"minecraft:slowness","duration":120,"amplifier":0,"is_ambient":false,"show_particles":true,"show_icon":true}}`
- `/active_ability/entity_action/actions/3`: `{"type":"runic_races:signature_presentation","key":"VALEN_STAND"}`

### fortitude

Authored description: +2 hearts, +2 armor, knockback resistance, +10% melee damage, and a sprinting shoulder-check that staggers the first foe you strike.

Executed JSON inputs:

- `/health`: `{"type":"origins:attribute","modifier":{"attribute":"minecraft:generic.max_health","operation":"addition","value":4.0,"name":"Valen Fortitude"}}`
- `/armor`: `{"type":"origins:attribute","modifier":{"attribute":"minecraft:generic.armor","operation":"addition","value":2.0,"name":"Valen Armor"}}`
- `/kb`: `{"type":"origins:attribute","modifier":{"attribute":"minecraft:generic.knockback_resistance","operation":"addition","value":0.4,"name":"Valen Stability"}}`
- `/melee`: `{"type":"origins:attribute","modifier":{"attribute":"minecraft:generic.attack_damage","operation":"multiply_total","value":0.1,"name":"Valen Strength"}}`

### stalwart_not_swift

Authored description: -8% movement speed and -8% attack speed. Heavy and deliberate.

Executed JSON inputs:

- `/speed`: `{"type":"origins:attribute","modifier":{"attribute":"minecraft:generic.movement_speed","operation":"multiply_total","value":-0.08,"name":"Stalwart Slowness"}}`
- `/attack_speed`: `{"type":"origins:attribute","modifier":{"attribute":"minecraft:generic.attack_speed","operation":"multiply_total","value":-0.08,"name":"Stalwart Heft"}}`

## volt_drake

### lightning_breath

Authored description: Loose a bolt of stormfire that stuns and dazzles. 40-second cooldown.

Executed JSON inputs:

- `/cooldown_timer`: `{"type":"origins:resource","min":0,"max":800,"start_value":0,"hud_render":{"should_render":false,"sprite_location":"origins:textures/gui/community/spade.png","bar_index":2},"min_action":null,"max_action":null}`
- `/cooldown_decay/entity_action/condition`: `{"type":"origins:resource","resource":"runic_races:volt_drake/lightning_breath_cooldown_timer","comparison":">","compare_to":0}`
- `/active_ability/condition`: `{"type":"origins:resource","resource":"runic_races:volt_drake/lightning_breath_cooldown_timer","comparison":"==","compare_to":0}`
- `/active_ability/entity_action/actions/0`: `{"type":"runic_races:cone_breath","range":8.0,"half_angle_degrees":18.0,"damage":8.0,"fire_seconds":0,"element":"shock"}`
- `/active_ability/entity_action/actions/1`: `{"type":"runic_races:signature_presentation","key":"VOLT_DRAKE_BREATH"}`

### stormscale_hide

Authored description: Immune to lightning, +2 armor, +15% speed, +10% attack speed (doubled in the rain), and gliding wings.

Executed JSON inputs:

- `/elytra_flight`: `{"type":"origins:elytra_flight","render_elytra":false}`
- `/lightning_imm`: `{"type":"origins:modify_damage_taken","damage_condition":{"type":"origins:name","name":"lightningBolt"},"modifier":{"operation":"multiply_total_multiplicative","value":-1.0}}`
- `/armor`: `{"type":"origins:attribute","modifier":{"attribute":"minecraft:generic.armor","operation":"addition","value":2.0,"name":"Stormscale"}}`
- `/speed`: `{"type":"origins:attribute","modifier":{"attribute":"minecraft:generic.movement_speed","operation":"multiply_total","value":0.15,"name":"Storm Speed"}}`
- `/attack_speed`: `{"type":"origins:attribute","modifier":{"attribute":"minecraft:generic.attack_speed","operation":"multiply_total","value":0.1,"name":"Storm Flurry"}}`
- `/storm_charged`: `{"type":"origins:conditioned_attribute","modifier":{"attribute":"minecraft:generic.attack_speed","operation":"multiply_total","value":0.1,"name":"Storm Charged"},"condition":{"type":"origins:in_rain"}}`

### grounded

Authored description: -1.5 hearts, +25% damage while wet, and weaker when shut away from the open sky.

Executed JSON inputs:

- `/health`: `{"type":"origins:attribute","modifier":{"attribute":"minecraft:generic.max_health","operation":"addition","value":-3.0,"name":"Light Frame"}}`
- `/wet`: `{"type":"origins:modify_damage_taken","condition":{"type":"origins:submerged_in","fluid":"minecraft:water"},"modifier":{"operation":"multiply_total_multiplicative","value":0.25}}`
- `/no_sky_weak`: `{"type":"origins:conditioned_attribute","modifier":{"attribute":"minecraft:generic.attack_damage","operation":"multiply_total","value":-0.1,"name":"Grounded"},"condition":{"type":"origins:invert","condition":{"type":"origins:exposed_to_sky"}}}`

## wind_wyrm

### galeforce_breath

Authored description: Summon a roaring gale that launches foes skyward as you ride the updraft. 45-second cooldown.

Executed JSON inputs:

- `/cooldown_timer`: `{"type":"origins:resource","min":0,"max":900,"start_value":0,"hud_render":{"should_render":false,"sprite_location":"origins:textures/gui/community/spade.png","bar_index":2},"min_action":null,"max_action":null}`
- `/cooldown_decay/entity_action/condition`: `{"type":"origins:resource","resource":"runic_races:wind_wyrm/galeforce_breath_cooldown_timer","comparison":">","compare_to":0}`
- `/active_ability/condition`: `{"type":"origins:resource","resource":"runic_races:wind_wyrm/galeforce_breath_cooldown_timer","comparison":"==","compare_to":0}`
- `/active_ability/entity_action/actions/0`: `{"type":"runic_races:cone_breath","range":7.0,"half_angle_degrees":30.0,"damage":6.0,"fire_seconds":0,"element":"wind"}`
- `/active_ability/entity_action/actions/1`: `{"type":"origins:add_velocity","x":0.0,"y":0.8,"z":0.0,"space":"local"}`
- `/active_ability/entity_action/actions/2`: `{"type":"runic_races:signature_presentation","key":"WIND_WYRM_BREATH"}`

### skylord

Authored description: The strongest wings of all, Slow Falling, no fall damage, and +15% speed. Born to soar.

Executed JSON inputs:

- `/elytra_flight`: `{"type":"origins:elytra_flight","render_elytra":false}`
- `/flap_cooldown_timer`: `{"type":"origins:resource","min":0,"max":50,"start_value":0,"hud_render":{"should_render":false,"sprite_location":"origins:textures/gui/community/spade.png","bar_index":2},"min_action":null,"max_action":null}`
- `/flap_cooldown_decay/entity_action/condition`: `{"type":"origins:resource","resource":"runic_races:wind_wyrm/skylord_flap_cooldown_timer","comparison":">","compare_to":0}`
- `/no_fall`: `{"type":"origins:modify_damage_taken","damage_condition":{"type":"origins:name","name":"fall"},"modifier":{"operation":"multiply_total_multiplicative","value":-1.0}}`
- `/speed`: `{"type":"origins:attribute","modifier":{"attribute":"minecraft:generic.movement_speed","operation":"multiply_total","value":0.15,"name":"Skylord Swiftness"}}`
- `/soft_wings/entity_action`: `{"type":"origins:apply_effect","effect":{"effect":"minecraft:slow_falling","duration":30,"amplifier":0,"is_ambient":true,"show_particles":false,"show_icon":false}}`

### untethered

Authored description: -2 hearts; shut away underground you are slowed, weakened, and take more harm.

Executed JSON inputs:

- `/health`: `{"type":"origins:attribute","modifier":{"attribute":"minecraft:generic.max_health","operation":"addition","value":-4.0,"name":"Sinuous Frame"}}`
- `/cave_slow`: `{"type":"origins:conditioned_attribute","modifier":{"attribute":"minecraft:generic.movement_speed","operation":"multiply_total","value":-0.15,"name":"Caged"},"condition":{"type":"origins:invert","condition":{"type":"origins:exposed_to_sky"}}}`
- `/cave_weak`: `{"type":"origins:modify_damage_taken","condition":{"type":"origins:invert","condition":{"type":"origins:exposed_to_sky"}},"modifier":{"operation":"multiply_total_multiplicative","value":0.15}}`

## wraith

### spectral_phase

Authored description: Turn incorporeal: Invisibility, Resistance, and drifting Speed while you drain a sliver of life from the souls around you. 45-second cooldown.

Executed JSON inputs:

- `/cooldown_timer`: `{"type":"origins:resource","min":0,"max":900,"start_value":0,"hud_render":{"should_render":false,"sprite_location":"origins:textures/gui/community/spade.png","bar_index":2},"min_action":null,"max_action":null}`
- `/cooldown_decay/entity_action/condition`: `{"type":"origins:resource","resource":"runic_races:wraith/spectral_phase_cooldown_timer","comparison":">","compare_to":0}`
- `/active_ability/condition`: `{"type":"origins:resource","resource":"runic_races:wraith/spectral_phase_cooldown_timer","comparison":"==","compare_to":0}`
- `/active_ability/entity_action/actions/0`: `{"type":"origins:apply_effect","effect":{"effect":"minecraft:invisibility","duration":100,"amplifier":0,"is_ambient":false,"show_particles":false,"show_icon":true}}`
- `/active_ability/entity_action/actions/1`: `{"type":"origins:apply_effect","effect":{"effect":"minecraft:resistance","duration":100,"amplifier":1,"is_ambient":false,"show_particles":false,"show_icon":true}}`
- `/active_ability/entity_action/actions/2`: `{"type":"origins:apply_effect","effect":{"effect":"minecraft:slow_falling","duration":100,"amplifier":0,"is_ambient":false,"show_particles":false,"show_icon":true}}`
- `/active_ability/entity_action/actions/3`: `{"type":"origins:apply_effect","effect":{"effect":"minecraft:speed","duration":100,"amplifier":0,"is_ambient":false,"show_particles":false,"show_icon":true}}`
- `/active_ability/entity_action/actions/4`: `{"type":"runic_races:afflict_hostiles","radius":5,"effects":[{"effect":"minecraft:wither","duration_ticks":60,"amplifier":0}]}`
- `/active_ability/entity_action/actions/5`: `{"type":"origins:heal","amount":2.0}`
- `/active_ability/entity_action/actions/6`: `{"type":"runic_races:signature_presentation","key":"WRAITH_PHASE"}`

### soul_touched

Authored description: Night vision, no fall damage, and your strikes grow deadlier in the dark of night.

Executed JSON inputs:

- `/night_vision`: `{"type":"origins:night_vision","strength":1.0}`
- `/no_fall`: `{"type":"origins:modify_damage_taken","damage_condition":{"type":"origins:name","name":"fall"},"modifier":{"operation":"multiply_total_multiplicative","value":-1.0}}`
- `/night_power`: `{"type":"runic_races:scaling_attribute","name":"power.runic_races.wraith.soul_touched.name","description":"power.runic_races.wraith.soul_touched.description","attribute":"generic.attack_damage","day_value":-0.05,"night_value":0.15,"operation":"multiply_total","check_interval":40}`

### half_bound

Authored description: -2.5 hearts, weakened in direct sunlight, and +20% damage from holy magic.

Executed JSON inputs:

- `/health`: `{"type":"origins:attribute","modifier":{"attribute":"minecraft:generic.max_health","operation":"addition","value":-5.0,"name":"Spectral Frailty"}}`
- `/sun_weak`: `{"type":"origins:conditioned_attribute","modifier":{"attribute":"minecraft:generic.attack_damage","operation":"multiply_total","value":-0.15,"name":"Sun-Banished"},"condition":{"type":"origins:and","conditions":[{"type":"origins:exposed_to_sun"},{"type":"origins:daytime"}]}}`
- `/holy`: `{"type":"origins:modify_damage_taken","damage_condition":{"type":"origins:or","conditions":[{"type":"origins:name","name":"magic"},{"type":"origins:name","name":"indirectMagic"}]},"modifier":{"operation":"multiply_total_multiplicative","value":0.2}}`

## zombie

### undying_hunger

Authored description: Refuse to die: Resistance II, Regeneration, and Strength for 8s. 90-second cooldown.

Executed JSON inputs:

- `/cooldown_timer`: `{"type":"origins:resource","min":0,"max":1800,"start_value":0,"hud_render":{"should_render":false,"sprite_location":"origins:textures/gui/community/spade.png","bar_index":2},"min_action":null,"max_action":null}`
- `/cooldown_decay/entity_action/condition`: `{"type":"origins:resource","resource":"runic_races:zombie/undying_hunger_cooldown_timer","comparison":">","compare_to":0}`
- `/active_ability/condition`: `{"type":"origins:resource","resource":"runic_races:zombie/undying_hunger_cooldown_timer","comparison":"==","compare_to":0}`
- `/active_ability/entity_action/actions/0`: `{"type":"origins:apply_effect","effect":{"effect":"minecraft:resistance","duration":160,"amplifier":1,"is_ambient":false,"show_particles":true,"show_icon":true}}`
- `/active_ability/entity_action/actions/1`: `{"type":"origins:apply_effect","effect":{"effect":"minecraft:regeneration","duration":160,"amplifier":0,"is_ambient":false,"show_particles":true,"show_icon":true}}`
- `/active_ability/entity_action/actions/2`: `{"type":"origins:apply_effect","effect":{"effect":"minecraft:strength","duration":160,"amplifier":0,"is_ambient":false,"show_particles":true,"show_icon":true}}`
- `/active_ability/entity_action/actions/3`: `{"type":"runic_races:signature_presentation","key":"ZOMBIE_HUNGER"}`

### deathless_flesh

Authored description: +3 hearts, +1 armor, immune to Poison and Hunger, and hard to knock down.

Executed JSON inputs:

- `/health`: `{"type":"origins:attribute","modifier":{"attribute":"minecraft:generic.max_health","operation":"addition","value":6.0,"name":"Deathless Vitality"}}`
- `/armor`: `{"type":"origins:attribute","modifier":{"attribute":"minecraft:generic.armor","operation":"addition","value":1.0,"name":"Rotted Hide"}}`
- `/poison_imm`: `{"type":"origins:effect_immunity","effect":"minecraft:poison"}`
- `/hunger_imm`: `{"type":"origins:effect_immunity","effect":"minecraft:hunger"}`
- `/kb`: `{"type":"origins:attribute","modifier":{"attribute":"minecraft:generic.knockback_resistance","operation":"addition","value":0.3,"name":"Dead Weight"}}`

### sunlight_decay

Authored description: -10% speed, -15% healing, and your flesh decays in direct sunlight.

Executed JSON inputs:

- `/speed`: `{"type":"origins:attribute","modifier":{"attribute":"minecraft:generic.movement_speed","operation":"multiply_total","value":-0.1,"name":"Shamble"}}`
- `/sun_burn`: `{"type":"origins:damage_over_time","interval":20,"damage":1.0,"damage_easy":1.0,"damage_type":"minecraft:on_fire","condition":{"type":"origins:exposed_to_sun"}}`
- `/healing`: `{"type":"origins:modify_healing","modifier":{"operation":"multiply_total_multiplicative","value":-0.15}}`

## Additional executed Java behavior

- RacialEventHandler: Primian adaptation, Reaper revival (legacy revenant key; 36000 ticks), Feline Nine Lives (18000 ticks), Forge One crafting bonus, Runic One crafting bonus, Valen shoulder-check, Arachnid/Serpen Poison I for 60 ticks, Blood Elf 20% direct melee lifesteal before its healing penalty, resize contact protection, and small-race jump compensation. These supplement the JSON inventory above.
- Volt Drake has +10% unconditional attack speed AND a separate +10% rain-conditioned modifier in shipped JSON. Preserve this baseline rather than describing the rain modifier as its only attack-speed bonus.
- Several legacy prose themes exceed actual effects: Nymph uses hostile debuffs, not a universal charm AI; Wraith uses timed effects, not permission to pass through solid blocks; Runic One uses self protection and nearby debuffs, not a party-healing engine.
- Optional metadata in RaceRegistry: exact scale, feather pool, luck, knockback multiplier and family slot grants. Ars uses race overrides then family mana/cost defaults; ISS uses flat race spell damage. They do not establish school-specific affinities. New races use explicit affinity records to prevent accidental inheritance.
- The old melee reward hook read proposed LivingDamageEvent amounts before the final health write, allowing canceled or absorbed damage to reward. 1.7.0 moves rewards to completed health accounting. Cone breath likewise lacked cover checks and applied riders after rejected hits; these are scoped corrections, not legacy-kit redesigns.
- All 111 baseline power JSON files now match isolated generator output semantically. Reconciled 21 divergent files without reverting shipped values; generator and language tools support isolated output and check modes.
