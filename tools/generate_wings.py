#!/usr/bin/env python3
"""
Generates per-race wing texture variants into
src/main/resources/assets/runic_races/textures/entity/ from the three
hand-made bases (pixie_wings.png, wyvern_wings.png, drake_wings.png).

Each variant is a luminance-mapped recolor (dark->light palette lerp over the
base texture's brightness, alpha preserved), plus a feather-banding pass for
the avian wings. Bespoke art can overwrite any output PNG without code changes.
Run from repo root:  python3 tools/generate_wings.py
"""
import os

from PIL import Image

DIR = "src/main/resources/assets/runic_races/textures/entity"


def colorize(base, dark, light, band=False):
    """Map base luminance onto a dark->light palette, preserving alpha."""
    img = base.convert("RGBA")
    px = img.load()
    w, h = img.size
    for y in range(h):
        for x in range(w):
            r, g, b, a = px[x, y]
            if a == 0:
                continue
            lum = (0.299 * r + 0.587 * g + 0.114 * b) / 255.0
            nr = dark[0] + (light[0] - dark[0]) * lum
            ng = dark[1] + (light[1] - dark[1]) * lum
            nb = dark[2] + (light[2] - dark[2]) * lum
            if band and y % 3 == 2:
                nr, ng, nb = nr * 0.82, ng * 0.82, nb * 0.82
            px[x, y] = (int(nr), int(ng), int(nb), a)
    return img


# output name -> (base file, dark, light, feather banding)
VARIANTS = {
    # feathered warm-brown wings over the wyvern silhouette
    "avian_wings.png": ("wyvern_wings.png", (62, 42, 26), (214, 178, 132), True),
    # pink-lavender shift of the pixie gossamer for faeries
    "faerie_wings.png": ("pixie_wings.png", (96, 44, 96), (255, 196, 240), False),
    # elemental drake recolors
    "fire_drake_wings.png": ("drake_wings.png", (64, 12, 6), (255, 126, 44), False),
    "ice_drake_wings.png": ("drake_wings.png", (22, 52, 84), (176, 232, 255), False),
    "terra_drake_wings.png": ("drake_wings.png", (42, 36, 28), (172, 150, 118), False),
    "volt_drake_wings.png": ("drake_wings.png", (44, 38, 12), (250, 222, 96), False),
}


def main():
    for out_name, (base_name, dark, light, band) in VARIANTS.items():
        base = Image.open(os.path.join(DIR, base_name))
        colorize(base, dark, light, band).save(os.path.join(DIR, out_name))
        print("wrote", os.path.join(DIR, out_name))


if __name__ == "__main__":
    main()
