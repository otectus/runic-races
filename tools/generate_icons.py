#!/usr/bin/env python3
"""
Copies + downscales the per-race art beside each design note into the mod's item
texture path (64x64) and emits the matching item-model JSON. Old (pre-overhaul)
icon textures/models are cleared first. Run from repo root.
"""
import json, os, glob
from PIL import Image

NOTES = os.environ.get("RR_ART_DIR", os.path.expanduser("~/Notes/Runic Races"))
TEX = "src/main/resources/assets/runic_races/textures/item"
MODELS = "src/main/resources/assets/runic_races/models/item"

# notes file (relative to NOTES) -> race id
MAP = {
    "Human/Primian.png": "primian", "Human/Celeron.png": "celeron",
    "Human/Magi.png": "magi", "Human/Valen.png": "valen",
    "Elven/HighElf.png": "high_elf", "Elven/DarkElf.png": "dark_elf",
    "Elven/MoonElf.png": "moon_elf", "Elven/BloodElf.png": "blood_elf", "Elven/IceElf.png": "ice_elf",
    "Dwarven/DeepOnes.png": "deep_one", "Dwarven/ForgeOnes.png": "forge_one",
    "Dwarven/FrostOnes.png": "frost_one", "Dwarven/IronOnes.png": "iron_one",
    "Dwarven/SkyOnes.png": "sky_one", "Dwarven/RunicOnes.png": "runic_one",
    "Bestial/Arachnid.png": "arachnid", "Bestial/Avian.png": "avian", "Bestial/Canine.png": "canine",
    "Bestial/Feline.png": "feline", "Bestial/Kitsune.png": "kitsune", "Bestial/Serpen.png": "serpen",
    "Faeborne/Changeling.png": "changeling", "Faeborne/Dryad.png": "dryad",
    "Faeborne/Sprite.png": "sprite", "Faeborne/Nymph.png": "nymph", "Faeborne/Faerie.png": "faerie",
    "Undead/Zombie.png": "zombie", "Undead/Skeleton.png": "skeleton", "Undead/Wraith.png": "wraith",
    "Undead/Demon.png": "demon", "Undead/Reaper.png": "reaper",
    "Draconic/Fire Drake.png": "fire_drake", "Draconic/IceDrake.png": "ice_drake",
    "Draconic/SeaSerpen.png": "sea_serpen", "Draconic/TerraDrake.png": "terra_drake",
    "Draconic/VoltDrake.png": "volt_drake", "Draconic/Wind Wyrm.png": "wind_wyrm",
}

os.makedirs(TEX, exist_ok=True)
os.makedirs(MODELS, exist_ok=True)

# clear old icons/models
for p in glob.glob(os.path.join(TEX, "*.png")):
    os.remove(p)
for p in glob.glob(os.path.join(MODELS, "*.png")):
    os.remove(p)
for p in glob.glob(os.path.join(MODELS, "*_icon.json")):
    os.remove(p)

missing = []
for rel, rid in MAP.items():
    src = os.path.join(NOTES, rel)
    if not os.path.exists(src):
        missing.append(rel); continue
    img = Image.open(src).convert("RGBA").resize((64, 64), Image.LANCZOS)
    img.save(os.path.join(TEX, rid + ".png"))
    with open(os.path.join(MODELS, rid + "_icon.json"), "w") as f:
        json.dump({"parent": "minecraft:item/generated",
                   "textures": {"layer0": "runic_races:item/" + rid}}, f, indent=2)
        f.write("\n")

print("Wrote %d icons (%d missing)" % (len(MAP) - len(missing), len(missing)))
if missing:
    print("MISSING:", missing)
