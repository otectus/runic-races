#!/usr/bin/env python3
"""
Generates the 16x16 state-rune glyphs used by StateRuneOverlay into
src/main/resources/assets/runic_races/textures/gui/rune/<flag_name>.png
(one per RaceStateFlags constant, lower-cased).

Glyphs are white-on-transparent so Java tints them with the rune's semantic
color. Each is a hand-designed thematic motif (sun rays, wave lines, fangs...)
rather than a letter box. Bespoke art can overwrite any output PNG.
Run from repo root:  python3 tools/generate_state_runes.py
"""
import os

from PIL import Image, ImageDraw

OUT = "src/main/resources/assets/runic_races/textures/gui/rune"
S = 16
W = (255, 255, 255, 255)
DIM = (255, 255, 255, 150)


def canvas():
    img = Image.new("RGBA", (S, S), (0, 0, 0, 0))
    return img, ImageDraw.Draw(img)


def biome_home():
    img, d = canvas()
    d.line([(3, 8), (8, 3), (13, 8)], fill=W, width=1)          # roof
    d.rectangle([5, 8, 11, 13], outline=W)                       # walls
    d.line([(8, 10), (8, 13)], fill=DIM, width=1)                # door
    return img


def biome_hostile():
    img, d = canvas()
    d.line([(4, 4), (12, 12)], fill=W, width=2)
    d.line([(12, 4), (4, 12)], fill=W, width=2)
    return img


def night_empowered():
    img, d = canvas()
    d.arc([3, 3, 13, 13], 300, 120, fill=W, width=2)             # crescent
    d.point((12, 4), fill=DIM)
    d.point((13, 7), fill=DIM)
    return img


def tight_space():
    img, d = canvas()
    d.line([(5, 3), (3, 3), (3, 13), (5, 13)], fill=W, width=1)  # brackets closing in
    d.line([(11, 3), (13, 3), (13, 13), (11, 13)], fill=W, width=1)
    d.line([(6, 8), (10, 8)], fill=DIM, width=1)
    return img


def sunlight_burning():
    img, d = canvas()
    d.ellipse([5, 5, 11, 11], outline=W)
    for dx, dy in ((0, -4), (0, 4), (-4, 0), (4, 0), (-3, -3), (3, -3), (-3, 3), (3, 3)):
        d.line([(8 + dx * 0.75, 8 + dy * 0.75), (8 + dx, 8 + dy)], fill=W, width=1)
    return img


def fire_vulnerable():
    img, d = canvas()
    d.line([(8, 2), (5, 7), (7, 8), (5, 12), (8, 10), (10, 13), (11, 8), (9, 7), (10, 3), (8, 2)],
           fill=W, width=1)
    return img


def adaptation_active():
    img, d = canvas()
    d.polygon([(8, 2), (14, 8), (8, 14), (2, 8)], outline=W)      # diamond frame; count draws over
    return img


def open_sky():
    img, d = canvas()
    d.arc([2, 6, 14, 18], 200, 340, fill=W, width=1)              # horizon arc
    d.line([(8, 10), (8, 3)], fill=W, width=1)                    # rising arrow
    d.line([(5, 6), (8, 3), (11, 6)], fill=W, width=1)
    return img


def submerged_weak():
    img, d = canvas()
    for y in (5, 9, 13):
        d.line([(3, y), (6, y - 2), (10, y), (13, y - 2)], fill=W, width=1)  # waves
    return img


def dry_sluggish():
    img, d = canvas()
    d.line([(2, 12), (14, 12)], fill=W, width=1)                  # cracked ground
    d.line([(6, 12), (5, 8)], fill=W, width=1)
    d.line([(9, 12), (10, 7), (12, 5)], fill=W, width=1)
    d.line([(9, 12), (8, 9)], fill=DIM, width=1)
    return img


def ravenous():
    img, d = canvas()
    d.arc([3, 2, 13, 12], 0, 180, fill=W, width=1)                # open maw
    for x in (5, 8, 11):
        d.line([(x, 7), (x + 1, 10)], fill=W, width=1)            # fangs
    return img


def cold_iron_grip():
    img, d = canvas()
    d.polygon([(4, 9), (6, 6), (12, 6), (10, 9)], outline=W)      # ingot
    for dx, dy in ((-2, -2), (2, -2), (0, -3)):
        d.line([(8 + dx, 4 + dy), (8 + dx * 2, 4 + dy * 2)], fill=DIM, width=1)  # sear sparks
    return img


GLYPHS = {
    "biome_home": biome_home,
    "biome_hostile": biome_hostile,
    "night_empowered": night_empowered,
    "tight_space": tight_space,
    "sunlight_burning": sunlight_burning,
    "fire_vulnerable": fire_vulnerable,
    "adaptation_active": adaptation_active,
    "open_sky": open_sky,
    "submerged_weak": submerged_weak,
    "dry_sluggish": dry_sluggish,
    "ravenous": ravenous,
    "cold_iron_grip": cold_iron_grip,
}


def main():
    os.makedirs(OUT, exist_ok=True)
    for name, gen in GLYPHS.items():
        path = os.path.join(OUT, "%s.png" % name)
        gen().save(path)
        print("wrote", path)


if __name__ == "__main__":
    main()
