#!/usr/bin/env python3
"""
Runic Races data generator.

Emits static Origins/Apoli JSON (origins, powers, layers) for the 37-race / 7-family
roster into src/main/resources, plus a race_lang.json fragment consumed by the en_us
merge step. This is an authoring aid only — it is NOT wired into the Gradle build; the
committed JSON it produces is the authoritative, hand-written-equivalent data.

All JSON shapes are copied verbatim from existing working power files so field names
and the `runic_races:<race>/<file>_cooldown_timer` resource-id convention stay exact.

Run from repo root:  python3 tools/generate_races.py
"""
import json, os, shutil

NS = "runic_races"
ROOT = os.path.join("src", "main", "resources", "data", NS)
POWERS = os.path.join(ROOT, "powers")
ORIGINS = os.path.join(ROOT, "origins")
LAYERS = os.path.join(ROOT, "origin_layers")

# Accumulates every lang key we mint, written to tools/race_lang.json.
LANG = {}

# ---------------------------------------------------------------- builders
def write_json(path, obj):
    os.makedirs(os.path.dirname(path), exist_ok=True)
    with open(path, "w") as f:
        json.dump(obj, f, indent=2)
        f.write("\n")

def eff(effect, dur, amp=0, particles=True):
    return {"type": "origins:apply_effect", "effect": {
        "effect": "minecraft:" + effect, "duration": dur, "amplifier": amp,
        "is_ambient": False, "show_particles": particles, "show_icon": True}}

def vel(x=0.0, y=0.0, z=0.0, space="local"):
    return {"type": "origins:add_velocity", "x": x, "y": y, "z": z, "space": space}

def heal(amount):
    return {"type": "origins:heal", "amount": amount}

def cone(element, rng, angle, dmg, fire):
    return {"type": "runic_races:cone_breath", "range": rng, "half_angle_degrees": angle,
            "damage": dmg, "fire_seconds": fire, "element": element}

def tremor(radius, ticks):
    return {"type": "runic_races:tremor_ping", "radius": radius, "duration_ticks": ticks}

def glow(radius, ticks):
    return {"type": "runic_races:glow_hostiles", "radius": radius, "duration_ticks": ticks}

def summon(entity, count, dur, radius=2.5):
    return {"type": "runic_races:summon_minion", "entity": entity, "count": count,
            "duration_ticks": dur, "radius": radius}

def place_trap(ticks):
    return {"type": "runic_races:place_trap", "duration_ticks": ticks}

def clear_cat(cat):
    return {"type": "runic_races:clear_effects_by_category", "category": cat}

def consume_mana(amount):
    return {"type": "runic_races:consume_mana", "amount": amount}

def show_banner(key, color, bold=True):
    return {"type": "runic_races:show_banner", "translation_key": key, "args": [], "color": color, "bold": bold}

def signature(key):
    return {"type": "runic_races:signature_presentation", "key": key}

def sound(s, vol, pitch):
    return {"type": "origins:play_sound", "sound": s, "volume": vol, "pitch": pitch}

def particles(p, count, speed=0.3, sx=0.6, sy=0.8, sz=0.6):
    # Bare names default to the minecraft namespace; pass "runic_races:x" for custom particles.
    pid = p if ":" in p else "minecraft:" + p
    return {"type": "origins:spawn_particles", "particle": pid, "count": count,
            "speed": speed, "spread": {"x": sx, "y": sy, "z": sz}}

def afflict(radius, target_effects=(), set_fire=None):
    # Offensive AoE via the mod's own hostile-filtered action.
    o = {"type": "runic_races:afflict_hostiles", "radius": radius,
         "effects": [{"effect": "minecraft:" + e, "duration_ticks": d, "amplifier": a}
                     for (e, d, a) in target_effects]}
    if set_fire:
        o["set_on_fire_seconds"] = set_fire
    return o

def cooldown_subpowers(resource_id, cd):
    timer = {"type": "origins:resource", "min": 0, "max": cd, "start_value": 0,
             "hud_render": {"should_render": False,
                            "sprite_location": "origins:textures/gui/community/spade.png", "bar_index": 2},
             "min_action": None, "max_action": None}
    decay = {"type": "origins:action_over_time", "interval": 1,
             "entity_action": {"type": "origins:if_else",
                               "condition": {"type": "origins:resource", "resource": resource_id,
                                             "comparison": ">", "compare_to": 0},
                               "if_action": {"type": "origins:change_resource", "resource": resource_id,
                                             "change": -1}}}
    return timer, decay

def active_power(race, file, cd, actions, name, desc, extra_conditions=None):
    rid = "%s:%s/%s_cooldown_timer" % (NS, race, file)
    timer, decay = cooldown_subpowers(rid, cd)
    full = list(actions) + [{"type": "origins:change_resource", "resource": rid, "change": cd}]
    condition = {"type": "origins:resource", "resource": rid, "comparison": "==", "compare_to": 0}
    if extra_conditions:
        condition = {"type": "origins:and", "conditions": [condition] + list(extra_conditions)}
    active = {"type": "origins:active_self",
              "key": {"key": "key.origins.primary_active", "continuous": False},
              "condition": condition,
              "entity_action": {"type": "origins:and", "actions": full}}
    LANG["power.%s.%s.%s.name" % (NS, race, file)] = name
    LANG["power.%s.%s.%s.description" % (NS, race, file)] = desc
    return {"type": "origins:multiple",
            "name": "power.%s.%s.%s.name" % (NS, race, file),
            "description": "power.%s.%s.%s.description" % (NS, race, file),
            "subpowers": ["cooldown_timer", "cooldown_decay", "active_ability"],
            "cooldown_timer": timer, "cooldown_decay": decay, "active_ability": active}

# ---- passive / weakness subpower helpers
def attr_sub(attribute, op, value, name):
    return {"type": "origins:attribute",
            "modifier": {"attribute": attribute, "operation": op, "value": value, "name": name}}

def health(value, name):   return attr_sub("minecraft:generic.max_health", "addition", value, name)
def speed(value, name):    return attr_sub("minecraft:generic.movement_speed", "multiply_total", value, name)
def atk_dmg(value, name):  return attr_sub("minecraft:generic.attack_damage", "multiply_total", value, name)
def atk_spd(value, name):  return attr_sub("minecraft:generic.attack_speed", "multiply_total", value, name)
def armor(value, name):    return attr_sub("minecraft:generic.armor", "addition", value, name)
def kb_resist(value, name):return attr_sub("minecraft:generic.knockback_resistance", "addition", value, name)
def luck(value, name):     return attr_sub("minecraft:generic.luck", "addition", value, name)

def imm(effect):
    return {"type": "origins:effect_immunity", "effect": "minecraft:" + effect}

def night_vision():
    return {"type": "origins:night_vision", "strength": 1.0}

def fire_immunity():
    # Negate all fire/lava damage via the verified modify_damage_taken path
    # (avoids relying on a fire_immunity power type that may not exist in this Apoli build).
    return dmg_taken_type(["inFire", "onFire", "lava", "hotFloor"], -1.0)

def magic_bonus(value):
    return {"type": "origins:modify_damage_dealt",
            "damage_condition": {"type": "origins:or", "conditions": [
                {"type": "origins:name", "name": "magic"},
                {"type": "origins:name", "name": "indirectMagic"}]},
            "modifier": {"operation": "multiply_total_multiplicative", "value": value}}

def elytra():
    return {"type": "origins:elytra_flight", "render_elytra": False}

def biome_aff(race, file, home_tag, sb, db, hostile_tag=None, sp=0.0, dp=0.0):
    # hostile_* keys are only emitted when a genuinely hostile biome tag is given.
    o = {"type": "runic_races:biome_affinity",
         "name": "power.%s.%s.%s.name" % (NS, race, file),
         "description": "power.%s.%s.%s.description" % (NS, race, file),
         "home_biome_tag": home_tag, "speed_bonus": sb, "damage_bonus": db}
    if hostile_tag:
        o["hostile_biome_tag"] = hostile_tag
        o["speed_penalty"] = sp
        o["damage_penalty"] = dp
    o["check_interval"] = 40
    return o

def scaling(race, file, attribute, day, night, op="multiply_total", require_sky=False):
    o = {"type": "runic_races:scaling_attribute",
         "name": "power.%s.%s.%s.name" % (NS, race, file),
         "description": "power.%s.%s.%s.description" % (NS, race, file),
         "attribute": attribute, "day_value": day, "night_value": night,
         "operation": op, "check_interval": 40}
    if require_sky:
        o["require_sky_exposure"] = True
    return o

def cond_attr(attribute, op, value, name, condition):
    return {"type": "origins:conditioned_attribute",
            "modifier": {"attribute": attribute, "operation": op, "value": value, "name": name},
            "condition": condition}

def heal_over_time(interval, amount, condition):
    return {"type": "origins:action_over_time", "interval": interval,
            "entity_action": {"type": "origins:if_else", "condition": condition,
                              "if_action": {"type": "origins:heal", "amount": amount}}}

def resource_holder(resource_id, cd):
    # a bare cooldown resource + decay (Java reads/sets it). Used by feline nine lives.
    return cooldown_subpowers(resource_id, cd)

# damage conditions / states
SUN_COND = {"type": "origins:and", "conditions": [
    {"type": "origins:exposed_to_sun"}, {"type": "origins:daytime"}]}
WATER_COND = {"type": "origins:submerged_in", "fluid": "minecraft:water"}
NO_SKY_COND = {"type": "origins:invert", "condition": {"type": "origins:exposed_to_sky"}}
FIRE_NAMES = ["inFire", "onFire", "lava", "hotFloor"]
COLD_NAMES = ["freeze"]
HOLY_NAMES = ["magic", "indirectMagic"]

def dmg_taken_self(condition, value):
    return {"type": "origins:modify_damage_taken", "condition": condition,
            "modifier": {"operation": "multiply_total_multiplicative", "value": value}}

def dmg_taken_type(names, value):
    conds = [{"type": "origins:name", "name": n} for n in names]
    dc = conds[0] if len(conds) == 1 else {"type": "origins:or", "conditions": conds}
    return {"type": "origins:modify_damage_taken", "damage_condition": dc,
            "modifier": {"operation": "multiply_total_multiplicative", "value": value}}

def sun_dot(dps=1.0):
    # exposed_to_sun already implies daytime; keep the condition minimal.
    return {"type": "origins:damage_over_time", "interval": 20, "damage": dps, "damage_easy": dps,
            "damage_type": "minecraft:on_fire", "condition": {"type": "origins:exposed_to_sun"}}

def exhaustion(value):
    return {"type": "origins:modify_exhaustion",
            "modifier": {"operation": "multiply_total_multiplicative", "value": value}}

def bundle(race, file, name, desc, specs):
    """specs: list of (subpower_key, dict)."""
    out = {"type": "origins:multiple",
           "name": "power.%s.%s.%s.name" % (NS, race, file),
           "description": "power.%s.%s.%s.description" % (NS, race, file),
           "subpowers": [k for k, _ in specs]}
    for k, v in specs:
        out[k] = v
    LANG["power.%s.%s.%s.name" % (NS, race, file)] = name
    LANG["power.%s.%s.%s.description" % (NS, race, file)] = desc
    return out

# v1.4.0: actives whose presentation moved into SignatureRegistry (Java). For these,
# present() keeps registering the banner lang string (the registry entry reuses the
# same message.<ns>.<race>.<file> key) but emits a single signature_presentation
# action instead of banner/sound/particle JSON. Sounds/parts args are ignored —
# the authoritative recipe lives in SignatureRegistry.java.
PRESENT_SIG = {
    ("primian", "stroke_of_fortune"): "PRIMIAN_FORTUNE",
    ("celeron", "messengers_dash"): "CELERON_DASH",
    ("magi", "arcane_overflow"): "MAGI_OVERFLOW",
    ("valen", "unbreakable_stand"): "VALEN_STAND",
    ("high_elf", "arcane_reflex"): "HIGH_ELF_REFLEX",
    ("dark_elf", "shadowmeld"): "DARK_ELF_SHADOWMELD",
    ("moon_elf", "moonlit_veil"): "MOON_ELF_VEIL",
    ("blood_elf", "blood_frenzy"): "BLOOD_ELF_FRENZY",
    ("ice_elf", "frostbind"): "ICE_ELF_FROSTBIND",
    ("deep_one", "tremorsense"): "DEEP_ONE_TREMOR",
    ("frost_one", "glacial_resolve"): "FROST_ONE_RESOLVE",
    ("iron_one", "shield_wall"): "IRON_ONE_SHIELD_WALL",
    ("sky_one", "mountain_leap"): "SKY_ONE_LEAP",
    ("arachnid", "web_snare"): "ARACHNID_WEB_SNARE",
    ("avian", "wind_burst"): "AVIAN_WIND_BURST",
    ("canine", "howl_of_the_pack"): "CANINE_HOWL",
    ("feline", "pounce"): "FELINE_POUNCE",
    ("kitsune", "foxfire_illusion"): "KITSUNE_FOXFIRE",
    ("serpen", "shed_skin"): "SERPEN_SHED",
    ("changeling", "mirror_shift"): "CHANGELING_MIRROR",
    ("dryad", "verdant_bloom"): "DRYAD_BLOOM",
    ("sprite", "phase_shift"): "SPRITE_PHASE",
    ("nymph", "sirens_charm"): "NYMPH_CHARM",
    ("zombie", "undying_hunger"): "ZOMBIE_HUNGER",
    ("skeleton", "conscript_the_dead"): "SKELETON_CONSCRIPT",
    ("reaper", "soul_harvest"): "REAPER_HARVEST",
}

def present(race, file, color, text, sounds=(), parts=()):
    key = "message.%s.%s.%s" % (NS, race, file)
    LANG[key] = text
    sig_key = PRESENT_SIG.get((race, file))
    if sig_key:
        return [signature(sig_key)]
    return [show_banner(key, color)] + list(sounds) + list(parts)

def sig(key):
    return [signature(key)]

NOFALL = dmg_taken_type(["fall"], -1.0)

DISPLAY = {
    "primian": "Primian", "celeron": "Celeron", "magi": "Magi", "valen": "Valen",
    "high_elf": "High Elf", "dark_elf": "Dark Elf", "moon_elf": "Moon Elf",
    "blood_elf": "Blood Elf", "ice_elf": "Ice Elf",
    "deep_one": "Deep One", "forge_one": "Forge One", "frost_one": "Frost One",
    "iron_one": "Iron One", "sky_one": "Sky One", "runic_one": "Runic One",
    "arachnid": "Arachnid", "avian": "Avian", "canine": "Canine", "feline": "Feline",
    "kitsune": "Kitsune", "serpen": "Serpen",
    "changeling": "Changeling", "dryad": "Dryad", "sprite": "Sprite", "nymph": "Nymph", "faerie": "Faerie",
    "zombie": "Zombie", "skeleton": "Skeleton", "wraith": "Wraith", "demon": "Demon", "reaper": "Reaper",
    "fire_drake": "Fire Drake", "ice_drake": "Ice Drake", "sea_serpen": "Sea Serpen",
    "terra_drake": "Terra Drake", "volt_drake": "Volt Drake", "wind_wyrm": "Wind Wyrm",
}
FAMILY_CAP = {"human": "Human", "elven": "Elven", "dwarven": "Dwarven", "bestial": "Bestial",
              "faeborne": "Faeborne", "undead": "Undead", "draconic": "Draconic"}
FAMILY_FIRST = {"human": "primian", "elven": "high_elf", "dwarven": "deep_one", "bestial": "arachnid",
                "faeborne": "changeling", "undead": "zombie", "draconic": "fire_drake"}
FAMILY_ORDER = {"human": 10, "elven": 20, "dwarven": 30, "bestial": 40, "faeborne": 50, "undead": 60, "draconic": 70}
FAMILY_RACES = {f: [] for f in FAMILY_ORDER}

def wings_specs(race, file, flap_cd=None):
    # flap_cd: per-race flap cooldown (ticks) for the flap resource, or falsy for glide-only wings.
    specs = [("elytra_flight", elytra())]
    if flap_cd:
        rid = "%s:%s/%s_flap_cooldown_timer" % (NS, race, file)
        t, d = cooldown_subpowers(rid, flap_cd)
        specs += [("flap_cooldown_timer", t), ("flap_cooldown_decay", d)]
    return specs

def emit(race, family, order, impact, origin_desc, active, passive, weakness):
    """active/passive/weakness are (file, obj) tuples."""
    for file, obj in (active, passive, weakness):
        write_json(os.path.join(POWERS, race, file + ".json"), obj)
    powers = ["%s:%s/%s" % (NS, race, p[0]) for p in (active, passive, weakness)]
    origin = {"icon": {"item": "%s:%s_icon" % (NS, race)}, "order": order, "impact": impact,
              "name": "origin.%s.%s.name" % (NS, race),
              "description": "origin.%s.%s.description" % (NS, race),
              "powers": powers}
    write_json(os.path.join(ORIGINS, race + ".json"), origin)
    LANG["origin.%s.%s.name" % (NS, race)] = "%s (%s)" % (DISPLAY[race], FAMILY_CAP[family])
    LANG["origin.%s.%s.description" % (NS, race)] = origin_desc
    LANG["item.%s.%s_icon" % (NS, race)] = DISPLAY[race] + " Icon"
    FAMILY_RACES[family].append(race)

# =================================================================== HUMAN
emit("primian", "human", 100, 1,
     "The first humans: fortune, charisma, and boundless adaptability incarnate; the world quietly bends toward them.",
     ("stroke_of_fortune",
      active_power("primian", "stroke_of_fortune", 2000,
                   [eff("luck", 200, 1), eff("absorption", 160, 0), eff("speed", 160, 0)]
                   + present("primian", "stroke_of_fortune", "gold", "Fortune favors you!",
                             [sound("minecraft:entity.player.levelup", 0.7, 1.4)],
                             [particles("totem_of_undying", 30, 0.5), particles("happy_villager", 15, 0.2)]),
                   "Stroke of Fortune",
                   "Fortune surges: Luck II for 10s, Absorption I and Speed I for 8s. 100-second cooldown.")),
     ("boundless_adaptability",
      bundle("primian", "boundless_adaptability", "Boundless Adaptability",
             "+1 heart, +1 Luck, +5% movement speed. You adapt to your surroundings, gaining brief speed as you explore new places.",
             [("health", health(2.0, "Primian Vitality")), ("luck", luck(1.0, "Primian Fortune")),
              ("speed", speed(0.05, "Primian Swiftness"))])),
     ("jack_of_all_trades",
      bundle("primian", "jack_of_all_trades", "Jack of All Trades",
             "Master of none: -10% magic damage and -5% melee damage. You hold no elemental affinity.",
             [("magic_penalty", magic_bonus(-0.10)), ("melee_penalty", atk_dmg(-0.05, "Generalist Melee Penalty"))])))

emit("celeron", "human", 101, 1,
     "Swift, witty wanderers of the messenger god's line; they prize speed, truth, and the open road.",
     ("messengers_dash",
      active_power("celeron", "messengers_dash", 500,
                   [vel(z=1.6, y=0.3), eff("speed", 80, 2)]
                   + present("celeron", "messengers_dash", "yellow", "You blur into motion!",
                             [sound("minecraft:entity.player.attack.sweep", 0.5, 1.4)],
                             [particles("cloud", 20, 0.2), particles("crit", 12, 0.3)]),
                   "Messenger's Dash",
                   "Dash forward with Speed III for 4s. 25-second cooldown.")),
     ("fleet_and_sure",
      bundle("celeron", "fleet_and_sure", "Fleet & Sure",
             "+12% movement speed and +10% attack speed. The road never tires you.",
             [("speed", speed(0.12, "Celeron Speed")), ("attack_speed", atk_spd(0.10, "Celeron Quickness"))])),
     ("featherweight_frame",
      bundle("celeron", "featherweight_frame", "Featherweight Frame",
             "-2 hearts and you are knocked back further than most.",
             [("health", health(-4.0, "Featherweight Health"))])))

emit("magi", "human", 102, 2,
     "Humans birthed from raw magic — Primian descendants who survived lethal arcane overexposure; magic flows where blood should.",
     ("arcane_overflow",
      active_power("magi", "arcane_overflow", 900,
                   [consume_mana(30), eff("strength", 160, 1),
                    afflict(6, [("weakness", 120, 1), ("glowing", 120, 0)])]
                   + present("magi", "arcane_overflow", "light_purple", "Arcane energy floods out!",
                             [sound("minecraft:block.amethyst_block.chime", 0.7, 1.2),
                              sound("minecraft:entity.illusioner.cast_spell", 0.5, 1.0)],
                             [particles("enchant", 40, 0.4, 2.0, 1.0, 2.0), particles("witch", 18, 0.2, 1.5, 1.0, 1.5)]),
                   "Arcane Overflow",
                   "Release an arcane nova: enemies within 6 blocks are Weakened and revealed while you gain Strength. Spends 30 mana if Iron's Spellbooks is present. 45-second cooldown.",
                   extra_conditions=[{"type": "runic_races:has_mana", "amount": 30}])),
     ("woven_of_magic",
      bundle("magi", "woven_of_magic", "Woven of Magic",
             "+15% magic damage. Your body is suffused with arcane power.",
             [("magic", magic_bonus(0.15))])),
     ("volatile_vessel",
      bundle("magi", "volatile_vessel", "Volatile Vessel",
             "-2 hearts and +20% physical damage taken. A frail magical frame.",
             [("health", health(-4.0, "Volatile Health")),
              ("phys", dmg_taken_type(["player", "mob", "arrow"], 0.20))])))

emit("valen", "human", 103, 2,
     "Valiant, honorable humans of unbending strength — the immovable rock that corners a foe — living in equilibrium with nature.",
     ("unbreakable_stand",
      active_power("valen", "unbreakable_stand", 1200,
                   [eff("resistance", 120, 2), eff("absorption", 120, 1), eff("slowness", 120, 0)]
                   + present("valen", "unbreakable_stand", "gold", "You stand immovable!",
                             [sound("minecraft:entity.iron_golem.attack", 0.6, 0.9),
                              sound("minecraft:block.anvil.land", 0.4, 1.2)],
                             [particles("crit", 30, 0.2), particles("enchanted_hit", 12, 0.2)]),
                   "Unbreakable Stand",
                   "Plant yourself: Resistance III, Absorption II, and Slowness I for 6s. 60-second cooldown.")),
     ("fortitude",
      bundle("valen", "fortitude", "Fortitude",
             "+2 hearts, +2 armor, knockback resistance, +10% melee damage, and a sprinting shoulder-check that staggers the first foe you strike.",
             [("health", health(4.0, "Valen Fortitude")), ("armor", armor(2.0, "Valen Armor")),
              ("kb", kb_resist(0.4, "Valen Stability")), ("melee", atk_dmg(0.10, "Valen Strength"))])),
     ("stalwart_not_swift",
      bundle("valen", "stalwart_not_swift", "Stalwart, Not Swift",
             "-10% movement speed and -10% attack speed. Heavy and deliberate.",
             [("speed", speed(-0.10, "Stalwart Slowness")), ("attack_speed", atk_spd(-0.10, "Stalwart Heft"))])))

# =================================================================== ELVEN
emit("high_elf", "elven", 200, 2,
     "Ancient, disciplined masters of arcane tradition; proud keepers of old magic.",
     ("arcane_reflex",
      active_power("high_elf", "arcane_reflex", 800,
                   [eff("absorption", 100, 1), eff("resistance", 100, 0),
                    afflict(4, [("weakness", 60, 0)])]
                   + present("high_elf", "arcane_reflex", "light_purple", "Arcane wards flare to life!",
                             [sound("minecraft:block.amethyst_block.chime", 0.7, 1.4)],
                             [particles("enchant", 35, 0.3, 1.0, 1.2, 1.0)]),
                   "Arcane Reflex",
                   "Snap up an arcane shield: Absorption II and Resistance I for 5s, weakening nearby foes. 40-second cooldown.")),
     ("arcane_mastery",
      bundle("high_elf", "arcane_mastery", "Arcane Mastery",
             "+15% magic damage and night vision. Centuries of study made manifest.",
             [("magic", magic_bonus(0.15)), ("night_vision", night_vision())])),
     ("fragile_grace",
      bundle("high_elf", "fragile_grace", "Fragile Grace",
             "-2 hearts and +15% physical damage taken. Grace bought with fragility.",
             [("health", health(-4.0, "Fragile Health")),
              ("phys", dmg_taken_type(["player", "mob", "arrow", "trident", "thrown", "sting", "mob_projectile"], 0.15))])))

emit("dark_elf", "elven", 201, 2,
     "Secretive masters of shadow and intrigue from moonless caverns; assassins who answer betrayal without mercy.",
     ("shadowmeld",
      active_power("dark_elf", "shadowmeld", 900,
                   [eff("invisibility", 120, 0, False), eff("speed", 120, 1)]
                   + present("dark_elf", "shadowmeld", "dark_purple", "You melt into shadow.",
                             [sound("minecraft:entity.illusioner.mirror_move", 0.6, 0.8)],
                             [particles("smoke", 22, 0.1), particles("squid_ink", 10, 0.05)]),
                   "Shadowmeld",
                   "Vanish into shadow: Invisibility and Speed II for 6s. 45-second cooldown.")),
     ("children_of_darkness",
      bundle("dark_elf", "children_of_darkness", "Children of Darkness",
             "Night vision, +10% movement speed, and your strikes hit harder in the dark of night — but falter under the open daytime sky.",
             [("night_vision", night_vision()), ("speed", speed(0.10, "Shadow Step")),
              ("night_power", scaling("dark_elf", "children_of_darkness", "generic.attack_damage", -0.05, 0.10,
                                      require_sky=True))])),
     ("sunlight_sensitivity",
      bundle("dark_elf", "sunlight_sensitivity", "Sunlight Sensitivity",
             "-1 heart and you are sluggish and weaker under direct daylight.",
             [("health", health(-2.0, "Sun-Averse Health")),
              ("sun_slow", cond_attr("minecraft:generic.movement_speed", "multiply_total", -0.10,
                                     "Sunlight Sluggishness", SUN_COND)),
              ("sun_weak", cond_attr("minecraft:generic.attack_damage", "multiply_total", -0.10,
                                     "Sunlight Weakness", SUN_COND))])))

emit("moon_elf", "elven", 202, 2,
     "Graceful elves bound to moonlight and tides; seers and silvered spellcasters, gentle yet dangerous beneath the stars.",
     ("moonlit_veil",
      active_power("moon_elf", "moonlit_veil", 1000,
                   [eff("invisibility", 120, 0, False), eff("slow_falling", 200, 0),
                    eff("regeneration", 100, 0), heal(4.0)]
                   + present("moon_elf", "moonlit_veil", "aqua", "Moonlight enfolds you.",
                             [sound("minecraft:block.amethyst_block.chime", 0.6, 1.6),
                              sound("minecraft:entity.allay.ambient_with_item", 0.4, 1.0)],
                             [particles("end_rod", 35, 0.15, 0.8, 1.2, 0.8), particles("snowflake", 12, 0.1)]),
                   "Moonlit Veil",
                   "Draw a veil of moonlight: Invisibility, Slow Falling, and Regeneration for several seconds, healing you. 50-second cooldown.")),
     ("tidecallers_grace",
      bundle("moon_elf", "tidecallers_grace", "Tidecaller's Grace",
             "Water breathing, night vision, and stronger natural healing under the night sky.",
             [("water_breathing", imm("drowning") if False else {"type": "origins:water_breathing"}),
              ("night_vision", night_vision()),
              ("night_regen", {"type": "origins:action_over_time",
                               "name": "power.%s.moon_elf.tidecallers_grace.name" % NS,
                               "description": "power.%s.moon_elf.tidecallers_grace.description" % NS,
                               "interval": 100,
                               "condition": {"type": "origins:daytime", "inverted": True},
                               "entity_action": {"type": "origins:heal", "amount": 1.0}})])),
     ("waning_by_day",
      bundle("moon_elf", "waning_by_day", "Waning by Day",
             "-1.5 hearts and -10% magic damage; your power wanes under the daytime sun.",
             [("health", health(-3.0, "Waning Health")), ("magic", magic_bonus(-0.10)),
              ("day_weak", cond_attr("minecraft:generic.attack_damage", "multiply_total", -0.08,
                                     "Daylight Waning", SUN_COND))])))

emit("blood_elf", "elven", 203, 2,
     "Proud elves of crimson rites and bloodcraft; relentless survivors who pay power's price in blood.",
     ("blood_frenzy",
      active_power("blood_elf", "blood_frenzy", 700,
                   [eff("strength", 160, 1), eff("speed", 160, 0), eff("regeneration", 100, 0)]
                   + present("blood_elf", "blood_frenzy", "dark_red", "Crimson power surges!",
                             [sound("minecraft:entity.ravager.roar", 0.5, 1.6),
                              sound("minecraft:entity.warden.heartbeat", 0.6, 1.2)],
                             [particles("damage_indicator", 40, 0.2, 0.6, 1.0, 0.6), particles("crimson_spore", 15, 0.1)]),
                   "Blood Frenzy",
                   "Burn your own vitality into power: Strength II and Speed I for 8s, Regeneration for 5s. 35-second cooldown.")),
     ("bloodcraft",
      bundle("blood_elf", "bloodcraft", "Bloodcraft",
             "+10% melee damage and +10% magic damage, and your strikes leech a fifth of the damage dealt. Blood is power.",
             [("melee", atk_dmg(0.10, "Bloodcraft Strength")), ("magic", magic_bonus(0.10))])),
     ("price_of_power",
      bundle("blood_elf", "price_of_power", "Price of Power",
             "-1.5 hearts and -30% natural healing. Power always has its price.",
             [("health", health(-3.0, "Blood Price")),
              ("healing", {"type": "origins:modify_healing",
                           "modifier": {"operation": "multiply_total_multiplicative", "value": -0.30}})])))

emit("ice_elf", "elven", 204, 2,
     "Cold, disciplined elves of glacial halls and northern lights; frost-mages and wardens, patient and steadfast.",
     ("frostbind",
      active_power("ice_elf", "frostbind", 800,
                   [afflict(6, [("slowness", 120, 2)]), eff("speed", 80, 0)]
                   + present("ice_elf", "frostbind", "aqua", "Frost erupts around you!",
                             [sound("minecraft:block.glass.break", 0.6, 0.8),
                              sound("minecraft:block.powder_snow.break", 0.6, 1.2)],
                             [particles("runic_races:frost_mote", 40, 0.2, 2.0, 1.0, 2.0), particles("item_snowball", 12, 0.2)]),
                   "Frostbind",
                   "Loose a frost nova: foes within 6 blocks are gripped by Slowness III. 40-second cooldown.")),
     ("winters_child",
      bundle("ice_elf", "winters_child", "Winter's Child",
             "Immune to freezing and +10% magic damage; the cold is your home.",
             [("freeze_imm", dmg_taken_type(COLD_NAMES, -1.0)), ("magic", magic_bonus(0.10)),
              ("cold_home", biome_aff("ice_elf", "winters_child", "forge:is_cold", 0.06, 0.05,
                                      "forge:is_hot", -0.06, 0.0))])),
     ("cold_blooded",
      bundle("ice_elf", "cold_blooded", "Cold-Blooded",
             "+25% fire damage taken. Heat is your undoing.",
             [("fire", dmg_taken_type(FIRE_NAMES + ["fireball"], 0.25))])))

def break_speed(value):
    return {"type": "origins:modify_break_speed",
            "modifier": {"operation": "multiply_total_multiplicative", "value": value}}

def water_breathing():
    return {"type": "origins:water_breathing"}

# =================================================================== DWARVEN
emit("deep_one", "dwarven", 300, 1,
     "Ancient dwarves of the world's lowest halls where sunlight is legend; unmatched miners with darkvision and stubborn will.",
     ("tremorsense",
      active_power("deep_one", "tremorsense", 300,
                   [tremor(18.0, 60)]
                   + present("deep_one", "tremorsense", "gray", "The stone reveals all.",
                             [sound("minecraft:entity.warden.heartbeat", 0.7, 0.8)],
                             [particles("crit", 30, 0.1, 1.0, 0.1, 1.0)]),
                   "Tremorsense",
                   "Read the stone: nearby hostiles are revealed through walls. Best underground. 15-second cooldown.")),
     ("deep_dweller",
      bundle("deep_one", "deep_dweller", "Deep Dweller",
             "Darkvision, +2 armor, immunity to Mining Fatigue, and faster mining underground.",
             [("night_vision", night_vision()), ("armor", armor(2.0, "Deep Plating")),
              ("mining_fatigue_imm", imm("mining_fatigue")), ("mining", break_speed(0.20))])),
     ("sunlight_blindness",
      bundle("deep_one", "sunlight_blindness", "Sun-Dazzled",
             "-5% speed always; under open sky you are slowed and weakened by the glare.",
             [("slow", speed(-0.05, "Stocky Gait")),
              ("sun_slow", cond_attr("minecraft:generic.movement_speed", "multiply_total", -0.10,
                                     "Surface Glare", SUN_COND)),
              ("sun_weak", cond_attr("minecraft:generic.attack_damage", "multiply_total", -0.10,
                                     "Sun-Dazzled", SUN_COND))])))

emit("forge_one", "dwarven", 301, 2,
     "Dwarves born to hammer and heat; master smiths whose work outlasts its maker, proven in fire.",
     ("forge_blessing",
      active_power("forge_one", "forge_blessing", 1000,
                   [eff("strength", 200, 0), eff("fire_resistance", 200, 0)] + sig("FORGE_BLESSING"),
                   "Forge Blessing",
                   "Call the forge's fire into your body: Strength and Fire Resistance for 10s. 50-second cooldown.")),
     ("ironhand_smith",
      bundle("forge_one", "ironhand_smith", "Ironhand Smith",
             "50% fire resistance, +2 armor, and +10% melee damage. Worth proven in fire.",
             [("fire_resist", dmg_taken_type(FIRE_NAMES, -0.50)), ("armor", armor(2.0, "Smith's Plate")),
              ("melee", atk_dmg(0.10, "Hammer Arm"))])),
     ("stone_heavy",
      bundle("forge_one", "stone_heavy", "Stone-Heavy",
             "-10% movement speed and -10% attack speed; you flounder in deep water.",
             [("speed", speed(-0.10, "Stone Weight")), ("attack_speed", atk_spd(-0.10, "Heavy Swing")),
              ("water_slow", cond_attr("minecraft:generic.movement_speed", "multiply_total", -0.20,
                                       "Sinks Like Stone", WATER_COND))])))

emit("frost_one", "dwarven", 302, 2,
     "Hardy dwarves of glacial halls and bitter winds; cold-immune survivors whose hearts burn warm beneath frost.",
     ("glacial_resolve",
      active_power("frost_one", "glacial_resolve", 1100,
                   [eff("resistance", 160, 1), eff("fire_resistance", 160, 0)]
                   + present("frost_one", "glacial_resolve", "aqua", "Frost steels your resolve!",
                             [sound("minecraft:block.beacon.activate", 0.5, 0.7),
                              sound("minecraft:block.powder_snow.break", 0.6, 0.9)],
                             [particles("snowflake", 35, 0.2, 0.8, 1.0, 0.8), particles("item_snowball", 10, 0.1)]),
                   "Glacial Resolve",
                   "Dig in against the cold: Resistance II and Fire Resistance for 8s. 55-second cooldown.")),
     ("frostborn",
      bundle("frost_one", "frostborn", "Frostborn",
             "Immune to freezing, +2 hearts, +1 armor, and at home in the cold.",
             [("freeze_imm", dmg_taken_type(COLD_NAMES, -1.0)), ("health", health(4.0, "Frost Vitality")),
              ("armor", armor(1.0, "Rimeplate")),
              ("cold_home", biome_aff("frost_one", "frostborn", "forge:is_cold", 0.06, 0.05,
                                      "forge:is_hot", -0.06, 0.0))])),
     ("forged_for_cold",
      bundle("frost_one", "forged_for_cold", "Forged for Cold",
             "+25% fire damage taken and -5% speed. Heat unmakes you.",
             [("fire", dmg_taken_type(FIRE_NAMES, 0.25)), ("speed", speed(-0.05, "Cold Limbs"))])))

emit("iron_one", "dwarven", 303, 2,
     "Stern, battle-hardened dwarves of fortress-holds; tough as forged plate, made to stand where others break.",
     ("shield_wall",
      active_power("iron_one", "shield_wall", 900,
                   [eff("resistance", 120, 2), eff("absorption", 120, 1)]
                   + present("iron_one", "shield_wall", "gray", "You raise an unbreakable guard!",
                             [sound("minecraft:item.shield.block", 0.7, 0.8),
                              sound("minecraft:block.anvil.place", 0.4, 1.0)],
                             [particles("crit", 30, 0.2), particles("enchanted_hit", 10, 0.2)]),
                   "Shield Wall",
                   "Raise an unbreakable guard: Resistance III and Absorption II for 6s. 45-second cooldown.")),
     ("forged_to_endure",
      bundle("iron_one", "forged_to_endure", "Forged to Endure",
             "+2 hearts, +3 armor, and strong knockback resistance.",
             [("health", health(4.0, "Iron Vitality")), ("armor", armor(3.0, "Fortress Plate")),
              ("kb", kb_resist(0.5, "Immovable"))])),
     ("heavy_as_the_mountain",
      bundle("iron_one", "heavy_as_the_mountain", "Heavy as the Mountain",
             "-10% movement speed, -10% attack speed, and -10% magic damage.",
             [("speed", speed(-0.10, "Mountain Weight")), ("attack_speed", atk_spd(-0.10, "Ponderous Swing")),
              ("magic", magic_bonus(-0.10))])))

emit("sky_one", "dwarven", 304, 2,
     "Proud mountain dwarves of wind-cut citadels above the clouds; sure-footed climbers and sharp-eyed sentries of the heights.",
     ("mountain_leap",
      active_power("sky_one", "mountain_leap", 400,
                   [vel(y=1.0, z=0.4), eff("slow_falling", 120, 0), eff("speed", 80, 0)]
                   + present("sky_one", "mountain_leap", "white", "You bound across the peaks!",
                             [sound("minecraft:entity.player.attack.sweep", 0.4, 1.5)],
                             [particles("cloud", 30, 0.2)]),
                   "Mountain Leap",
                   "Bound high with Slow Falling and Speed for a safe landing. 20-second cooldown.")),
     ("sure_footed_sentinel",
      bundle("sky_one", "sure_footed_sentinel", "Sure-Footed Sentinel",
             "Immune to fall damage, +1 armor, and swift in the mountains.",
             [("no_fall", NOFALL), ("armor", armor(1.0, "Sentinel Mail")),
              ("mountain_home", biome_aff("sky_one", "sure_footed_sentinel", "forge:is_mountain", 0.10, 0.05))])),
     ("thin_air_lungs",
      bundle("sky_one", "thin_air_lungs", "Thin-Air Lungs",
             "-1 heart; enclosed underground you are slowed and take more harm.",
             [("health", health(-2.0, "Thin-Air Health")),
              ("cave_slow", cond_attr("minecraft:generic.movement_speed", "multiply_total", -0.10,
                                      "Claustrophobia", NO_SKY_COND)),
              ("cave_weak", dmg_taken_self(NO_SKY_COND, 0.10))])))

emit("runic_one", "dwarven", 305, 3,
     "Dwarves steeped in stone-carved runes; rune-smiths who bind power into steel and stone, keepers of old knowledge.",
     ("rune_of_warding",
      active_power("runic_one", "rune_of_warding", 1200,
                   [eff("resistance", 160, 1), eff("regeneration", 160, 0), afflict(6, [("slowness", 80, 1)])]
                   + sig("RUNIC_WARD"),
                   "Rune of Warding",
                   "Inscribe a ward of stone: Resistance and Regeneration for you while nearby foes are slowed. 60-second cooldown.")),
     ("runebound",
      bundle("runic_one", "runebound", "Runebound",
             "+10% magic damage, +2 armor, and +1 Luck for enchanting fortune.",
             [("magic", magic_bonus(0.10)), ("armor", armor(2.0, "Runed Plate")),
              ("luck", luck(1.0, "Rune Fortune"))])),
     ("bound_by_tradition",
      bundle("runic_one", "bound_by_tradition", "Bound by Tradition",
             "-2 hearts and -10% speed. A scholar, not a warrior.",
             [("health", health(-4.0, "Scholar's Frailty")), ("speed", speed(-0.10, "Ponderous Study"))])))

# =================================================================== BESTIAL
emit("arachnid", "bestial", 400, 2,
     "Patient weavers born of silk and shadow who read the world through vibration; venomous, precise, oath-bound.",
     ("web_snare",
      active_power("arachnid", "web_snare", 700,
                   [place_trap(1200), afflict(5, [("slowness", 120, 3)]), eff("slow_falling", 100, 0)]
                   + present("arachnid", "web_snare", "white", "You cast a snaring web!",
                             [sound("minecraft:entity.spider.ambient", 0.6, 1.2),
                              sound("minecraft:block.wool.place", 0.5, 0.8)],
                             [particles("crit", 30, 0.2, 1.5, 0.8, 1.5)]),
                   "Web Snare",
                   "Cast a web: foes within 5 blocks are rooted by Slowness IV and a trap is left underfoot. 35-second cooldown.")),
     ("weavers_senses",
      bundle("arachnid", "weavers_senses", "Weaver's Senses",
             "Poison immunity, +10% attack speed, no fall damage, venomous fangs, and constant vibration-sense of nearby creatures.",
             [("poison_imm", imm("poison")), ("attack_speed", atk_spd(0.10, "Spider Quickness")),
              ("no_fall", NOFALL),
              ("vibration", {"type": "origins:action_over_time", "interval": 100,
                             "entity_action": glow(8.0, 120)})])),
     ("fragile_carapace",
      bundle("arachnid", "fragile_carapace", "Fragile Carapace",
             "-1.5 hearts and +20% fire damage taken. Spiders fear flame.",
             [("health", health(-3.0, "Fragile Shell")), ("fire", dmg_taken_type(FIRE_NAMES + ["fireball"], 0.20))])))

emit("avian", "bestial", 401, 2,
     "Proud, sharp-eyed folk of skyborne blood; scouts and storm-watchers bound to wind and song.",
     ("wind_burst",
      active_power("avian", "wind_burst", 400,
                   [vel(y=1.1), eff("slow_falling", 140, 0), eff("speed", 80, 0)]
                   + present("avian", "wind_burst", "aqua", "You burst skyward!",
                             [sound("minecraft:entity.phantom.flap", 0.4, 1.4)],
                             [particles("cloud", 24, 0.2), particles("poof", 8, 0.1)]),
                   "Wind Burst",
                   "Beat your wings for a skyward burst with Slow Falling. 20-second cooldown.")),
     ("skyborne",
      bundle("avian", "skyborne", "Skyborne",
             "Feathered wings for gliding flight, no fall damage, +10% speed, and keen night sight.",
             wings_specs("avian", "skyborne", 35)
             + [("no_fall", NOFALL), ("speed", speed(0.10, "Swift Flight")), ("night_vision", night_vision())])),
     ("hollow_bones",
      bundle("avian", "hollow_bones", "Hollow Bones",
             "-2 hearts, easily knocked back, and -10% melee damage. A light, hollow frame.",
             [("health", health(-4.0, "Hollow Frame")),
              ("melee", atk_dmg(-0.10, "Weak Grip"))])))

emit("canine", "bestial", 402, 1,
     "Loyal, resilient folk of wolf and hound blood; pack-bound hunters and trackers.",
     ("howl_of_the_pack",
      active_power("canine", "howl_of_the_pack", 900,
                   [eff("strength", 200, 0), eff("speed", 200, 0), glow(14.0, 200)]
                   + present("canine", "howl_of_the_pack", "green", "Your howl rallies the pack!",
                             [sound("minecraft:entity.wolf.howl", 0.8, 1.0) if False else sound("minecraft:entity.wolf.growl", 0.8, 0.8)],
                             [particles("crit", 30, 0.2), particles("note", 10, 0.3)]),
                   "Howl of the Pack",
                   "Howl: gain Strength and Speed and mark wounded prey for 10s. 45-second cooldown.")),
     ("pack_hunter",
      bundle("canine", "pack_hunter", "Pack Hunter",
             "+12% speed, night vision, +10% melee damage, and at home in the forest.",
             [("speed", speed(0.12, "Pack Speed")), ("night_vision", night_vision()),
              ("melee", atk_dmg(0.10, "Hunter's Bite")),
              ("forest_home", biome_aff("canine", "pack_hunter", "minecraft:is_forest", 0.06, 0.05))])),
     ("ravenous",
      bundle("canine", "ravenous", "Ravenous",
             "+40% hunger drain and -2 armor. Always hungry, thin of hide.",
             [("hunger", exhaustion(0.4)), ("armor", armor(-2.0, "Thin Hide"))])))

emit("feline", "bestial", 403, 2,
     "Graceful, instinctive folk of catlike blood; agile night-stalkers whose elegance hides predatory confidence.",
     ("pounce",
      active_power("feline", "pounce", 300,
                   [vel(z=1.5, y=0.5), eff("strength", 60, 0)]
                   + present("feline", "pounce", "gold", "You pounce!",
                             [sound("minecraft:entity.cat.hiss", 0.6, 1.5),
                              sound("minecraft:entity.player.attack.sweep", 0.4, 1.2)],
                             [particles("crit", 24, 0.3), particles("sweep_attack", 6, 0.2)]),
                   "Pounce",
                   "Leap at your prey with a burst of Strength. 15-second cooldown.")),
     ("nine_lives",
      bundle("feline", "nine_lives", "Nine Lives & Night Eyes",
             "Cheat death once every 10 minutes, landing on your feet. Night vision, +15% attack speed, and no fall damage.",
             [("cooldown_timer", cooldown_subpowers("%s:feline/nine_lives_cooldown_timer" % NS, 12000)[0]),
              ("cooldown_decay", cooldown_subpowers("%s:feline/nine_lives_cooldown_timer" % NS, 12000)[1]),
              ("night_vision", night_vision()), ("attack_speed", atk_spd(0.15, "Feline Quickness")),
              ("no_fall", NOFALL)])),
     ("hydrophobia",
      bundle("feline", "hydrophobia", "Hydrophobia",
             "-1.5 hearts and +30% damage taken while in water.",
             [("health", health(-3.0, "Lithe Frame")), ("water", dmg_taken_self(WATER_COND, 0.30))])))

emit("kitsune", "bestial", 404, 3,
     "Clever fox-spirits of enchanted blood; illusionists and wandering sages whose charm and hidden fire grow with age.",
     ("foxfire_illusion",
      active_power("kitsune", "foxfire_illusion", 800,
                   [eff("invisibility", 120, 0, False), eff("speed", 120, 1, False),
                    afflict(5, [("blindness", 80, 0), ("slowness", 80, 1)])]
                   + present("kitsune", "foxfire_illusion", "light_purple", "Foxfire wreathes you!",
                             [sound("minecraft:entity.allay.ambient_with_item", 0.6, 1.4),
                              sound("minecraft:block.fire.ambient", 0.5, 1.5)],
                             [particles("soul_fire_flame", 40, 0.2, 1.0, 1.2, 1.0), particles("enchant", 18, 0.3)]),
                   "Foxfire Illusion",
                   "Vanish in foxfire: gain Invisibility and Speed II while nearby foes are blinded and slowed. 40-second cooldown.")),
     ("spirit_of_the_fox",
      bundle("kitsune", "spirit_of_the_fox", "Spirit of the Fox",
             "+15% magic damage, night vision, and +10% speed.",
             [("magic", magic_bonus(0.15)), ("night_vision", night_vision()), ("speed", speed(0.10, "Fox Step"))])),
     ("untamed_spirit",
      bundle("kitsune", "untamed_spirit", "Untamed Spirit",
             "-2 hearts and +15% physical damage taken. A spirit-frail body.",
             [("health", health(-4.0, "Spirit Frailty")),
              ("phys", dmg_taken_type(["player", "mob", "arrow", "trident", "thrown", "sting", "mob_projectile"], 0.15))])))

emit("serpen", "bestial", 405, 2,
     "Calm, dangerous folk of snake blood; venomous mystics and assassins who strike with the quiet power of the coiled strike.",
     ("shed_skin",
      active_power("serpen", "shed_skin", 900,
                   [clear_cat("harmful"), eff("invisibility", 100, 0, False), eff("speed", 100, 0, False)]
                   + present("serpen", "shed_skin", "green", "You shed your skin.",
                             [sound("minecraft:entity.cat.hiss", 0.5, 0.6)],
                             [particles("item_slime", 22, 0.1), particles("smoke", 10, 0.1)]),
                   "Shed Skin",
                   "Slither free: cleanse all harmful effects and gain Invisibility and Speed for 5s. 45-second cooldown.")),
     ("venomous_coil",
      bundle("serpen", "venomous_coil", "Venomous Coil",
             "Poison immunity, venomous fangs, +10% speed, night vision, and thriving in the heat.",
             [("poison_imm", imm("poison")), ("speed", speed(0.10, "Coiled Speed")),
              ("night_vision", night_vision()),
              ("hot_home", biome_aff("serpen", "venomous_coil", "forge:is_hot", 0.06, 0.05,
                                     "forge:is_cold", -0.06, 0.0))])),
     ("cold_blooded",
      bundle("serpen", "cold_blooded", "Cold-Blooded",
             "-1.5 hearts and +25% freezing damage; the cold dulls your blood.",
             [("health", health(-3.0, "Lean Coils")), ("cold", dmg_taken_type(COLD_NAMES, 0.25))])))

# =================================================================== FAEBORNE
emit("changeling", "faeborne", 500, 2,
     "Mysterious shapeshifters who wear faces like masks; spies and infiltrators free to define themselves beyond blood.",
     ("mirror_shift",
      active_power("changeling", "mirror_shift", 800,
                   [eff("invisibility", 120, 0, False), eff("speed", 100, 0)]
                   + present("changeling", "mirror_shift", "light_purple", "You slip behind a borrowed face.",
                             [sound("minecraft:entity.illusioner.mirror_move", 0.6, 1.0)],
                             [particles("smoke", 20, 0.1), particles("portal", 12, 0.2)]),
                   "Mirror Shift",
                   "Slip away behind a glamour: Invisibility and Speed for 6s. 40-second cooldown.")),
     ("manyfaces",
      bundle("changeling", "manyfaces", "Manyfaces",
             "+10% movement speed and +1 Luck. You wear whatever face the moment needs.",
             [("speed", speed(0.10, "Fluid Step")), ("luck", luck(1.0, "Borrowed Fortune"))])),
     ("hollow_identity",
      bundle("changeling", "hollow_identity", "Hollow Identity",
             "-1 heart and -10% attack damage. With no true self, no strike lands with full conviction.",
             [("health", health(-2.0, "Hollow Health")), ("attack", atk_dmg(-0.1, "No True Self"))])))

emit("dryad", "faeborne", 501, 2,
     "Graceful forest-born fae bound to grove and root; gentle healers whose wrath is as old and unforgiving as the forest.",
     ("verdant_bloom",
      active_power("dryad", "verdant_bloom", 900,
                   [eff("regeneration", 120, 1), heal(4.0), afflict(6, [("slowness", 120, 1)])]
                   + present("dryad", "verdant_bloom", "green", "Life blooms around you!",
                             [sound("minecraft:block.azalea.place", 0.7, 1.0),
                              sound("minecraft:block.moss.place", 0.5, 0.8)],
                             [particles("happy_villager", 40, 0.2, 1.5, 1.0, 1.5), particles("composter", 15, 0.2)]),
                   "Verdant Bloom",
                   "Burst into bloom: heal yourself with Regeneration while roots slow nearby foes. 45-second cooldown.")),
     ("one_with_the_grove",
      bundle("dryad", "one_with_the_grove", "One with the Grove",
             "Poison immunity, at home in the forest, and you slowly heal in sunlight on the open ground.",
             [("poison_imm", imm("poison")),
              ("forest_home", biome_aff("dryad", "one_with_the_grove", "minecraft:is_forest", 0.06, 0.05)),
              ("sun_heal", heal_over_time(300, 1.0, SUN_COND))])),
     ("kindling",
      bundle("dryad", "kindling", "Kindling",
             "Fire deals triple damage to you. Bark and leaf burn fast.",
             [("fire", dmg_taken_type(FIRE_NAMES, 2.0))])))

emit("sprite", "faeborne", 502, 2,
     "Tiny, lively fae of wild magic and starlight; quick-winged pranksters far more dangerous than they appear.",
     ("phase_shift",
      active_power("sprite", "phase_shift", 1800,
                   [vel(z=1.2, y=0.4), eff("invisibility", 60, 0, False), eff("speed", 100, 1)]
                   + present("sprite", "phase_shift", "light_purple", "You flicker out of harm's way!",
                             [sound("minecraft:entity.allay.ambient_with_item", 0.6, 1.8),
                              sound("minecraft:block.amethyst_block.chime", 0.4, 1.8)],
                             [particles("end_rod", 35, 0.3, 0.6, 0.8, 0.6), particles("firework", 12, 0.2)]),
                   "Phase Shift",
                   "Flicker out of danger: blink forward with Invisibility and Speed II. 90-second cooldown.")),
     ("gossamer_wings",
      bundle("sprite", "gossamer_wings", "Gossamer Wings",
             "Gossamer wings for gliding flight, Slow Falling, +30% speed, and +15% attack speed.",
             wings_specs("sprite", "gossamer_wings", 30)
             + [("slow_fall", {"type": "origins:climbing"} if False else imm("none") if False else NOFALL),
                ("speed", speed(0.30, "Sprite Swiftness")), ("attack_speed", atk_spd(0.15, "Sprite Flurry"))])),
     ("fragile_essence",
      bundle("sprite", "fragile_essence", "Fragile Essence",
             "-3 hearts and easily knocked from the air. A wisp of a body.",
             [("health", health(-6.0, "Fragile Essence"))])))

emit("nymph", "faeborne", 503, 2,
     "Graceful nature spirits of rivers and springs; charming healers and enchantresses fierce in defense of their waters.",
     ("sirens_charm",
      active_power("nymph", "sirens_charm", 900,
                   [afflict(8, [("slowness", 160, 1), ("weakness", 160, 0)]), eff("regeneration", 120, 0), heal(2.0)]
                   + present("nymph", "sirens_charm", "aqua", "Your charm enthralls all who watch.",
                             [sound("minecraft:entity.allay.ambient_with_item", 0.7, 1.2),
                              sound("minecraft:block.water.ambient", 0.5, 1.4)],
                             [particles("heart", 30, 0.2, 1.5, 1.2, 1.5), particles("note", 12, 0.3),
                              particles("falling_water", 12, 0.1)]),
                   "Siren's Charm",
                   "Sing an enthralling charm: nearby foes are Slowed and Weakened while you mend with Regeneration. 45-second cooldown.")),
     ("spirit_of_spring",
      bundle("nymph", "spirit_of_spring", "Spirit of Spring",
             "Water breathing, +10% magic damage, and at home in and near the water.",
             [("water_breathing", water_breathing()), ("magic", magic_bonus(0.10)),
              ("water_home", biome_aff("nymph", "spirit_of_spring", "forge:is_water", 0.06, 0.05,
                                       "forge:is_hot", -0.10, 0.0))])),
     ("bound_to_water",
      bundle("nymph", "bound_to_water", "Bound to Water",
             "-1.5 hearts and +20% fire damage taken; away from water, far from home, you wither.",
             [("health", health(-3.0, "Spring-Bound")), ("fire", dmg_taken_type(FIRE_NAMES, 0.20))])))

emit("faerie", "faeborne", 504, 3,
     "Small, magical fae of ancient glades and moonlit flowers; whimsical tricksters whose magic can bless, confuse, protect, or punish.",
     ("faerie_bargain",
      active_power("faerie", "faerie_bargain", 1000,
                   [eff("regeneration", 120, 0), eff("absorption", 120, 0),
                    afflict(7, [("slowness", 120, 1), ("blindness", 120, 0), ("levitation", 40, 0)])]
                   + sig("FAERIE_GLAMOUR"),
                   "Faerie Bargain",
                   "Weave an old enchantment: bless yourself with Regeneration and Absorption while cursing nearby foes with Slowness, Blindness, and Levitation. 50-second cooldown.")),
     ("pixie_flight",
      bundle("faerie", "pixie_flight", "Pixie Flight",
             "Delicate wings for gliding flight, Slow Falling, +15% magic damage, +20% speed, and night vision.",
             wings_specs("faerie", "pixie_flight", 30)
             + [("magic", magic_bonus(0.15)), ("speed", speed(0.20, "Pixie Swiftness")),
                ("night_vision", night_vision())])),
     ("cold_iron",
      bundle("faerie", "cold_iron", "Cold Iron",
             "-2.5 hearts, +20% physical damage taken, and easily knocked from the air. Cold iron is anathema to the fae.",
             [("health", health(-5.0, "Delicate Form")),
              ("phys", dmg_taken_type(["player", "mob", "arrow"], 0.20))])))

# =================================================================== UNDEAD
emit("zombie", "undead", 600, 1,
     "Undead animated by plague and stubborn refusal to rest; tireless, pain-resistant, and grimly hard to put down.",
     ("undying_hunger",
      active_power("zombie", "undying_hunger", 1800,
                   [eff("resistance", 160, 1), eff("regeneration", 160, 0), eff("strength", 160, 0)]
                   + present("zombie", "undying_hunger", "dark_green", "You refuse to fall!",
                             [sound("minecraft:entity.zombie.ambient", 0.7, 0.6)],
                             [particles("sculk_soul", 30, 0.2), particles("crimson_spore", 12, 0.1)]),
                   "Undying Hunger",
                   "Refuse to die: Resistance II, Regeneration, and Strength for 8s. 90-second cooldown.")),
     ("deathless_flesh",
      bundle("zombie", "deathless_flesh", "Deathless Flesh",
             "+3 hearts, +1 armor, immune to Poison and Hunger, and hard to knock down.",
             [("health", health(6.0, "Deathless Vitality")), ("armor", armor(1.0, "Rotted Hide")),
              ("poison_imm", imm("poison")), ("hunger_imm", imm("hunger")), ("kb", kb_resist(0.3, "Dead Weight"))])),
     ("sunlight_decay",
      bundle("zombie", "sunlight_decay", "Sunlight Decay",
             "-10% speed and your flesh decays in direct sunlight; you heal slowly.",
             [("speed", speed(-0.10, "Shamble")), ("sun_burn", sun_dot(1.0)),
              ("healing", {"type": "origins:modify_healing",
                           "modifier": {"operation": "multiply_total_multiplicative", "value": -0.25}})])))

emit("skeleton", "undead", 601, 1,
     "Fleshless undead given motion by old oaths; eerie, tireless bonecrafters and unerring archers.",
     ("conscript_the_dead",
      active_power("skeleton", "conscript_the_dead", 1400,
                   [summon("runic_races:grave_servant", 2, 1200, 2.5)]
                   + present("skeleton", "conscript_the_dead", "white", "The dead answer your call!",
                             [sound("minecraft:entity.skeleton.ambient", 0.7, 0.8),
                              sound("minecraft:block.bone_block.break", 0.6, 0.8)],
                             [particles("soul", 30, 0.2, 1.5, 0.5, 1.5), particles("crit", 12, 0.2)]),
                   "Conscript the Dead",
                   "Raise two skeletal servants to fight at your side for a time. 70-second cooldown.")),
     ("marrow_deep_aim",
      bundle("skeleton", "marrow_deep_aim", "Marrow-Deep Aim",
             "+10% attack speed, immune to Poison and Hunger, and you need no breath.",
             [("attack_speed", atk_spd(0.10, "Bony Precision")), ("poison_imm", imm("poison")),
              ("hunger_imm", imm("hunger")), ("water_breathing", water_breathing())])),
     ("brittle_bones",
      bundle("skeleton", "brittle_bones", "Brittle Bones",
             "-2 hearts, +25% fall damage, and you take more harm in direct sunlight.",
             [("health", health(-4.0, "Brittle Frame")), ("fall", dmg_taken_type(["fall"], 0.25)),
              ("sun", dmg_taken_self(SUN_COND, 0.20))])))

emit("wraith", "undead", 602, 2,
     "Spectral undead shaped by sorrow and vengeance; silent, shadow-bound hunters who drain the soul and haunt forgotten graves.",
     ("spectral_phase",
      active_power("wraith", "spectral_phase", 900,
                   [eff("invisibility", 100, 0, False), eff("resistance", 100, 1, False),
                    eff("slow_falling", 100, 0, False), eff("speed", 100, 0, False),
                    afflict(5, [("wither", 60, 0)]), heal(2.0)]
                   + sig("WRAITH_PHASE"),
                   "Spectral Phase",
                   "Turn incorporeal: Invisibility, Resistance, and drifting Speed while you drain a sliver of life from the souls around you. 45-second cooldown.")),
     ("soul_touched",
      bundle("wraith", "soul_touched", "Soul-Touched",
             "Night vision, no fall damage, and your strikes grow deadlier in the dark of night.",
             [("night_vision", night_vision()), ("no_fall", NOFALL),
              ("night_power", scaling("wraith", "soul_touched", "generic.attack_damage", -0.05, 0.15))])),
     ("half_bound",
      bundle("wraith", "half_bound", "Half-Bound",
             "-2.5 hearts, weakened in direct sunlight, and +20% damage from holy magic.",
             [("health", health(-5.0, "Spectral Frailty")),
              ("sun_weak", cond_attr("minecraft:generic.attack_damage", "multiply_total", -0.15,
                                     "Sun-Banished", SUN_COND)),
              ("holy", dmg_taken_type(HOLY_NAMES, 0.20))])))

emit("demon", "undead", 603, 3,
     "Fierce infernal beings of cursed realms; horned and burning-eyed masters of fire and forbidden power who seize fate by force.",
     ("infernal_wrath",
      active_power("demon", "infernal_wrath", 1000,
                   [eff("strength", 160, 1), eff("fire_resistance", 160, 0),
                    afflict(6, [("weakness", 100, 0)], set_fire=6)]
                   + sig("DEMON_WRATH"),
                   "Infernal Wrath",
                   "Erupt in hellfire: gain Strength II and Fire Resistance while nearby foes are set ablaze and Weakened. 50-second cooldown.")),
     ("infernal_blood",
      bundle("demon", "infernal_blood", "Infernal Blood",
             "Immune to fire, +15% melee damage, and stronger in the heat.",
             [("fire_imm", fire_immunity()), ("melee", atk_dmg(0.15, "Infernal Strength")),
              ("hot_home", biome_aff("demon", "infernal_blood", "forge:is_hot", 0.05, 0.08))])),
     ("holy_vulnerability",
      bundle("demon", "holy_vulnerability", "Holy Vulnerability",
             "+25% damage from holy magic, +20% damage while in water, and -25% natural healing.",
             [("holy", dmg_taken_type(HOLY_NAMES, 0.25)), ("water", dmg_taken_self(WATER_COND, 0.20)),
              ("healing", {"type": "origins:modify_healing",
                           "modifier": {"operation": "multiply_total_multiplicative", "value": -0.25}})])))

emit("reaper", "undead", 604, 3,
     "Grim harbingers touched by death's silence; soul-reaping executioners and solemn guardians of the final passage.",
     ("soul_harvest",
      active_power("reaper", "soul_harvest", 800,
                   [afflict(6, [("wither", 100, 1), ("glowing", 100, 0)]), heal(4.0), eff("regeneration", 100, 0)]
                   + present("reaper", "soul_harvest", "dark_purple", "You reap the souls of the fallen!",
                             [sound("minecraft:block.bell.use", 0.6, 0.5),
                              sound("minecraft:entity.wither.ambient", 0.4, 1.4)],
                             [particles("soul", 40, 0.2, 1.5, 1.0, 1.5), particles("sculk_soul", 15, 0.2)]),
                   "Soul Harvest",
                   "Sweep your scythe: nearby foes are Withered and revealed while their souls mend you. 40-second cooldown.")),
     ("harbinger_of_death",
      bundle("reaper", "harbinger_of_death", "Harbinger of Death",
             "Immune to Wither, night vision, and +15% melee damage. Death once cheated returns you to the world (every 30 minutes).",
             [("wither_imm", imm("wither")), ("night_vision", night_vision()),
              ("melee", atk_dmg(0.15, "Reaper's Edge"))])),
     ("touch_of_the_grave",
      bundle("reaper", "touch_of_the_grave", "Touch of the Grave",
             "-2 hearts, -50% natural healing, and weakened in direct sunlight.",
             [("health", health(-4.0, "Grave-Touched")),
              ("healing", {"type": "origins:modify_healing",
                           "modifier": {"operation": "multiply_total_multiplicative", "value": -0.50}}),
              ("sun_weak", cond_attr("minecraft:generic.attack_damage", "multiply_total", -0.15,
                                     "Sun-Shunned", SUN_COND))])))

# =================================================================== DRACONIC
def drake_passive(race, file, name, desc, extra):
    return bundle(race, file, name, desc, extra)

emit("fire_drake", "draconic", 700, 2,
     "Drakes of ember and ash whose hearts burn like a forge; their breath is living flame and their scales drink the heat.",
     ("dragonfire_breath",
      active_power("fire_drake", "dragonfire_breath", 800,
                   [cone("fire", 7.0, 22.0, 7.0, 8)] + sig("FIRE_DRAKE_BREATH"),
                   "Dragonfire Breath",
                   "Breathe a cone of searing flame that ignites all it touches. 40-second cooldown.")),
     ("emberscale_hide",
      bundle("fire_drake", "emberscale_hide", "Emberscale Hide",
             "Immune to fire and lava, +3 armor, +10% melee damage, gliding wings, and stronger in the heat.",
             wings_specs("fire_drake", "emberscale_hide", False)
             + [("fire_imm", fire_immunity()), ("armor", armor(3.0, "Emberscale")),
                ("melee", atk_dmg(0.10, "Drake Strength")),
                ("hot_home", biome_aff("fire_drake", "emberscale_hide", "forge:is_hot", 0.05, 0.08,
                                       "forge:is_cold", -0.10, 0.0))])),
     ("cold_quenches_fire",
      bundle("fire_drake", "cold_quenches_fire", "Cold Quenches Fire",
             "+30% cold damage taken, +20% hunger drain. Cold and water are your bane.",
             [("cold", dmg_taken_type(COLD_NAMES, 0.30)), ("water", dmg_taken_self(WATER_COND, 0.30)),
              ("hunger", exhaustion(0.20))])))

emit("ice_drake", "draconic", 701, 2,
     "Pale drakes of glacier and rime; their breath is a killing cold and their scales never thaw.",
     ("frost_breath",
      active_power("ice_drake", "frost_breath", 800,
                   [cone("frost", 7.0, 22.0, 6.0, 0)] + sig("ICE_DRAKE_BREATH"),
                   "Frost Breath",
                   "Exhale a cone of killing frost that freezes and slows. 40-second cooldown.")),
     ("rimescale_hide",
      bundle("ice_drake", "rimescale_hide", "Rimescale Hide",
             "Immune to freezing, +3 armor, gliding wings, and at home in the cold.",
             wings_specs("ice_drake", "rimescale_hide", False)
             + [("freeze_imm", dmg_taken_type(COLD_NAMES, -1.0)), ("armor", armor(3.0, "Rimescale")),
                ("cold_home", biome_aff("ice_drake", "rimescale_hide", "forge:is_cold", 0.05, 0.08,
                                        "forge:is_hot", -0.10, 0.0))])),
     ("thaw",
      bundle("ice_drake", "thaw", "Thaw",
             "+30% fire damage taken and +20% hunger drain. Flame is your undoing.",
             [("fire", dmg_taken_type(FIRE_NAMES, 0.30)), ("hunger", exhaustion(0.20))])))

emit("sea_serpen", "draconic", 702, 2,
     "Great serpentine drakes of the deep; coiling masters of tide and current whose breath drowns the shore.",
     ("tidal_breath",
      active_power("sea_serpen", "tidal_breath", 800,
                   [cone("water", 7.0, 25.0, 6.0, 0)] + sig("SEA_SERPEN_BREATH"),
                   "Tidal Breath",
                   "Unleash a crushing tide that hurls foes back and drowns them in slowness. 40-second cooldown.")),
     ("leviathans_gift",
      bundle("sea_serpen", "leviathans_gift", "Leviathan's Gift",
             "Water breathing, +3 armor, and supreme power in the open ocean.",
             [("water_breathing", water_breathing()), ("armor", armor(3.0, "Serpent Scale")),
              ("ocean_home", biome_aff("sea_serpen", "leviathans_gift", "forge:is_water", 0.08, 0.10,
                                       "forge:is_hot", -0.05, 0.0))])),
     ("landbound_coils",
      bundle("sea_serpen", "landbound_coils", "Landbound Coils",
             "-1 heart and +20% fire damage taken; far from water your coils grow sluggish.",
             [("health", health(-2.0, "Landbound")), ("fire", dmg_taken_type(FIRE_NAMES, 0.20)),
              ("dry_slow", cond_attr("minecraft:generic.movement_speed", "multiply_total", -0.10,
                                     "Beached", {"type": "origins:invert", "condition": WATER_COND}))])))

emit("terra_drake", "draconic", 703, 2,
     "Mountainous drakes of stone and root; their hide is living rock and their breath shatters the earth.",
     ("seismic_breath",
      active_power("terra_drake", "seismic_breath", 800,
                   [cone("earth", 6.0, 28.0, 7.0, 0), tremor(14.0, 40)] + sig("TERRA_DRAKE_BREATH"),
                   "Seismic Breath",
                   "Breathe the weight of the mountain: foes are battered, slowed, and shaken from cover. 40-second cooldown.")),
     ("stonescale_hide",
      bundle("terra_drake", "stonescale_hide", "Stonescale Hide",
             "+4 armor, knockback immunity, Mining Fatigue immunity, faster mining, no fall damage, gliding wings, and at home in the mountains.",
             wings_specs("terra_drake", "stonescale_hide", False)
             + [("armor", armor(4.0, "Stonescale")), ("kb", kb_resist(1.0, "Immovable Bulk")),
                ("mining_fatigue_imm", imm("mining_fatigue")), ("mining", break_speed(0.15)),
                ("no_fall", NOFALL),
                ("mountain_home", biome_aff("terra_drake", "stonescale_hide", "forge:is_mountain", 0.05, 0.08))])),
     ("ponderous",
      bundle("terra_drake", "ponderous", "Ponderous",
             "-10% movement speed, -10% attack speed, and +20% hunger drain.",
             [("speed", speed(-0.10, "Stone Weight")), ("attack_speed", atk_spd(-0.10, "Slow Swing")),
              ("hunger", exhaustion(0.20))])))

emit("volt_drake", "draconic", 704, 2,
     "Swift drakes wreathed in storm-light; their breath is the thunderbolt and their wings outrun the gale.",
     ("lightning_breath",
      active_power("volt_drake", "lightning_breath", 800,
                   [cone("shock", 8.0, 18.0, 8.0, 0)] + sig("VOLT_DRAKE_BREATH"),
                   "Lightning Breath",
                   "Loose a bolt of stormfire that stuns and dazzles. 40-second cooldown.")),
     ("stormscale_hide",
      bundle("volt_drake", "stormscale_hide", "Stormscale Hide",
             "Immune to lightning, +2 armor, +15% speed, +10% attack speed, and gliding wings.",
             wings_specs("volt_drake", "stormscale_hide", False)
             + [("lightning_imm", dmg_taken_type(["lightningBolt"], -1.0)), ("armor", armor(2.0, "Stormscale")),
                ("speed", speed(0.15, "Storm Speed")), ("attack_speed", atk_spd(0.10, "Storm Flurry"))])),
     ("grounded",
      bundle("volt_drake", "grounded", "Grounded",
             "-1.5 hearts, +25% damage while wet, and weaker when shut away from the open sky.",
             [("health", health(-3.0, "Light Frame")), ("wet", dmg_taken_self(WATER_COND, 0.25)),
              ("no_sky_weak", cond_attr("minecraft:generic.attack_damage", "multiply_total", -0.10,
                                        "Grounded", NO_SKY_COND))])))

emit("wind_wyrm", "draconic", 705, 3,
     "Sinuous sky-wyrms born of the high winds; the freest and most ancient of drakes, riding the jetstreams above the clouds.",
     ("galeforce_breath",
      active_power("wind_wyrm", "galeforce_breath", 900,
                   [cone("wind", 7.0, 30.0, 6.0, 0), vel(y=0.8)] + sig("WIND_WYRM_BREATH"),
                   "Galeforce Breath",
                   "Summon a roaring gale that launches foes skyward as you ride the updraft. 45-second cooldown.")),
     ("skylord",
      bundle("wind_wyrm", "skylord", "Skylord",
             "The strongest wings of all, Slow Falling, no fall damage, and +15% speed. Born to soar.",
             wings_specs("wind_wyrm", "skylord", 50)
             + [("no_fall", NOFALL), ("speed", speed(0.15, "Skylord Swiftness")),
                ("soft_wings", {"type": "origins:action_over_time", "interval": 20,
                                "condition": {"type": "origins:and", "conditions": [
                                    {"type": "origins:on_block", "inverted": True},
                                    {"type": "origins:fall_flying", "inverted": True}]},
                                "entity_action": {"type": "origins:apply_effect", "effect": {
                                    "effect": "minecraft:slow_falling", "duration": 30, "amplifier": 0,
                                    "is_ambient": True, "show_particles": False, "show_icon": False}}})])),
     ("untethered",
      bundle("wind_wyrm", "untethered", "Untethered",
             "-2 hearts; shut away underground you are slowed, weakened, and take more harm.",
             [("health", health(-4.0, "Sinuous Frame")),
              ("cave_slow", cond_attr("minecraft:generic.movement_speed", "multiply_total", -0.15,
                                      "Caged", NO_SKY_COND)),
              ("cave_weak", dmg_taken_self(NO_SKY_COND, 0.15))])))

# =================================================================== FAMILIES + LAYERS
FAMILY_DESC = {
    "human": "The mortal races of humankind: adaptable, fortunate, and unspecialized, shaping the world to their will.",
    "elven": "Ancient and graceful, the elves command arcane tradition and keen senses at the cost of fragile bodies.",
    "dwarven": "Stout folk of stone and forge: armored, enduring, and unmatched below the earth, but slow and magic-poor.",
    "bestial": "Folk of ancient beast bloodlines, gifted with sharp senses, agility, and predatory instinct.",
    "faeborne": "Small fae of wild magic and illusion: swift, magical, and often winged, but delicate of body.",
    "undead": "Those who refused to rest: tireless and immune to many ills, but undone by sunlight and the holy.",
    "draconic": "Drakes and wyrms of elemental power: armored, breath-wielding flyers, each strong in one element and weak to its opposite.",
}
for fam, order in FAMILY_ORDER.items():
    first = FAMILY_FIRST[fam]
    fo = {"icon": {"item": "%s:%s_icon" % (NS, first)}, "order": order, "impact": 1,
          "name": "origin.%s.family_%s.name" % (NS, fam),
          "description": "origin.%s.family_%s.description" % (NS, fam), "powers": []}
    write_json(os.path.join(ORIGINS, "family_%s.json" % fam), fo)
    LANG["origin.%s.family_%s.name" % (NS, fam)] = FAMILY_CAP[fam]
    LANG["origin.%s.family_%s.description" % (NS, fam)] = FAMILY_DESC[fam]

family_layer = {"replace": True, "order": 0,
                "origins": ["%s:family_%s" % (NS, f) for f in FAMILY_ORDER],
                "name": "layer.runic_races.family.name",
                "missing_origin": {"name": "layer.runic_races.family.missing_origin.name",
                                   "description": "layer.runic_races.family.missing_origin.description"},
                "gui_title": {"choose_origin": "layer.runic_races.family.choose",
                              "view_origin": "layer.runic_races.family.view"}}
write_json(os.path.join(LAYERS, "family.json"), family_layer)

race_blocks = []
for fam in FAMILY_ORDER:
    race_blocks.append({
        "condition": {"type": "origins:origin", "origin": "%s:family_%s" % (NS, fam),
                      "layer": "%s:family" % NS},
        "origins": ["%s:%s" % (NS, r) for r in FAMILY_RACES[fam]]})
race_layer = {"replace": True, "order": 10, "origins": race_blocks,
              "name": "layer.runic_races.race.name",
              "missing_origin": {"name": "layer.runic_races.race.missing_origin.name",
                                 "description": "layer.runic_races.race.missing_origin.description"},
              "gui_title": {"choose_origin": "layer.runic_races.race.choose",
                            "view_origin": "layer.runic_races.race.view"}}
write_json(os.path.join(LAYERS, "race.json"), race_layer)

# signature banner lang
SIG_BANNERS = {
    "message.runic_races.signature.reaper.revival": "Death's door swings open — you rise again!",
    "message.runic_races.signature.reaper.revival_rejected": "The grave refuses you... not yet.",
    "message.runic_races.signature.wraith.phase": "You slip into the veil.",
    "message.runic_races.signature.demon.wrath": "Hellfire answers your fury!",
    "message.runic_races.signature.feline.nine_lives": "You land on your feet — a life spent!",
    "message.runic_races.signature.forge_one.forge_blessing": "The forge's fire fills you!",
    "message.runic_races.signature.runic_one.runic_ward": "A rune of power blazes to life!",
    "message.runic_races.signature.faerie.glamour": "You weave an old enchantment!",
    "message.runic_races.signature.fire_drake.breath": "You breathe searing flame!",
    "message.runic_races.signature.ice_drake.breath": "You exhale a killing frost!",
    "message.runic_races.signature.sea_serpen.breath": "You unleash the crushing tide!",
    "message.runic_races.signature.terra_drake.breath": "You breathe the weight of the mountain!",
    "message.runic_races.signature.volt_drake.breath": "You loose the thunder!",
    "message.runic_races.signature.wind_wyrm.breath": "You summon a roaring gale!",
    "message.runic_races.signature.sprite.wing_flap": "Your gossamer wings flutter!",
    "message.runic_races.signature.faerie.wing_flap": "Your pixie wings shimmer!",
    "message.runic_races.signature.avian.wing_flap": "You beat your wings and rise!",
    "message.runic_races.signature.wind_wyrm.wing_flap": "Your wings ride the gale!",
    "message.runic_races.signature.flight.cancel": "You fold your wings.",
}
LANG.update(SIG_BANNERS)

with open(os.path.join("tools", "race_lang.json"), "w") as f:
    json.dump(LANG, f, indent=2, sort_keys=True)
    f.write("\n")

races_total = sum(len(v) for v in FAMILY_RACES.values())
print("Generated %d races across %d families; %d lang keys -> tools/race_lang.json"
      % (races_total, len(FAMILY_ORDER), len(LANG)))
