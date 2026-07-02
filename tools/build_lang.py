#!/usr/bin/env python3
"""
Rebuilds en_us.json for the overhauled roster:
  - drops every old race key (origin./power./item.runic_races./message.runic_races.* banners)
  - preserves infra keys (layer.*, tooltip.*, message.runic_races.learning.*, key./config./gui./etc.)
  - adds the generated race/family/power/item/signature keys (tools/race_lang.json)
  - adds notification start/stop copy for the new harmful-state topics
Run from repo root after generate_races.py.
"""
import json, os

LANG = "src/main/resources/assets/runic_races/lang/en_us.json"

with open(LANG) as f:
    cur = json.load(f)
with open("tools/race_lang.json") as f:
    race_lang = json.load(f)


def keep(k):
    if k.startswith("layer.") or k.startswith("tooltip.") or k.startswith("entity."):
        return True
    if k.startswith("message.runic_races.learning."):
        return True
    # drop all overhauled namespaces; everything else (key./config./gui./itemGroup.) stays
    if k.startswith("origin.") or k.startswith("power.") \
            or k.startswith("item.runic_races.") or k.startswith("message.runic_races."):
        return False
    return True


merged = {k: v for k, v in cur.items() if keep(k)}
merged.update(race_lang)

# Notification start/stop copy for new harmful-state topics (NotificationRegistry).
NOTIF = {
    "zombie.sunlight":      ("Your flesh begins to rot in the sunlight.", "You shrink back from the killing light."),
    "skeleton.sunlight":    ("Sunlight sears your brittle bones.", "You slip back into the shade."),
    "wraith.sunlight":      ("Daylight banishes your spectral form.", "Shadow returns to shield you."),
    "reaper.sunlight":      ("The living sun shuns your grave-touched flesh.", "Twilight mercy returns."),
    "deep_one.sunlight":    ("The surface glare dazzles and slows you.", "Blessed darkness again."),
    "dark_elf.sunlight":    ("Sunlight saps your shadowed strength.", "You return to welcome gloom."),
    "sky_one.tight_space":  ("The close dark presses in — you long for the peaks.", "Open air at last."),
    "wind_wyrm.tight_space":("Walls close around your wings — you cannot soar.", "The open sky is yours again."),
    "dryad.fire":           ("Fire! Your bark and leaves catch fast.", "The flames are out."),
    "arachnid.fire":        ("Fire! Your carapace blisters.", "The flames are out."),
    "nymph.fire":           ("Fire! The heat withers your spirit.", "The flames are out."),
    "ice_drake.fire":       ("Fire! Your rimescale hisses and cracks.", "The flames are out."),
    "frost_one.fire":       ("Fire! The heat unmakes your cold flesh.", "The flames are out."),
    "sea_serpen.fire":      ("Fire! Your scales dry and split.", "The flames are out."),
    "volt_drake.open_sky":  ("Under the open sky, the storm answers your call.", "Cut off from the sky, your power dims."),
    "feline.submerged":     ("Water soaks your fur — every blow lands harder.", "You shake yourself dry."),
    "volt_drake.submerged": ("Water grounds your storm — your scales spark and sputter.", "Dry again, the current rebuilds."),
}
for topic, (start, stop) in NOTIF.items():
    merged["message.runic_races.%s.start" % topic] = start
    merged["message.runic_races.%s.stop" % topic] = stop

with open(LANG, "w") as f:
    json.dump(merged, f, indent=2, ensure_ascii=False, sort_keys=True)
    f.write("\n")

print("en_us.json rebuilt: %d keys (was %d)" % (len(merged), len(cur)))
