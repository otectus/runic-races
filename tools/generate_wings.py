#!/usr/bin/env python3
"""
Generates the 64x64 articulated wing sheets into
src/main/resources/assets/runic_races/textures/entity/ from the three hand-made
source textures (pixie_wings_base.png, wyvern_wings_base.png, drake_wings.png —
all in the legacy 64x32 elytra layout).

The v1.4.0 WingModel splits each wing into articulated parts (arm + lagging
tip, feathered layers, gossamer fore/hind pairs). Every output sheet carries
ALL part islands (painted from crops of the source's front-face art) so one
LayerDefinition serves every silhouette; a race's WingType picks which group
renders. Recoloring is the same luminance-mapped palette lerp as before, with
feather banding constrained to the avian plumage islands.

Bespoke art can overwrite any output PNG without code changes.
Run from repo root:  python3 tools/generate_wings.py

UV islands (must match WingModel.createLayer):
  membrane arm  (0,0)   5x20x2      membrane tip (14,0)  6x20x2
  feather arm   (30,0)  4x18x2      primaries    (42,0)  7x20x1
  coverts       (0,24)  5x12x1      forewing     (12,24) 8x14x1
  hindwing      (30,24) 6x10x1
"""
import os

from PIL import Image, ImageEnhance

DIR = "src/main/resources/assets/runic_races/textures/entity"
SHEET = 64

# Legacy elytra box (texOffs 22,0; 10x20x2): front face pixels.
LEGACY_FRONT = (24, 2, 34, 22)  # 10x20

# island -> (u, v, w, h, d, art crop from the 10x20 front face, brighten)
# crop = (x0, y0, x1, y1) within the 10x20 front-face art.
ISLANDS = {
    "membrane_arm": (0, 0, 5, 20, 2, (5, 0, 10, 20), 1.0),
    "membrane_tip": (14, 0, 6, 20, 2, (0, 0, 5, 20), 1.0),
    "feather_arm": (30, 0, 4, 18, 2, (6, 0, 10, 18), 1.0),
    "primaries": (42, 0, 7, 20, 1, (0, 0, 7, 20), 1.0),
    "coverts": (0, 24, 5, 12, 1, (3, 0, 8, 12), 1.12),
    "forewing": (12, 24, 8, 14, 1, (0, 0, 8, 14), 1.0),
    "hindwing": (30, 24, 6, 10, 1, (2, 8, 8, 18), 1.0),
}

# Islands that get the avian feather banding (plumage only, not membrane parts).
BAND_ISLANDS = ("feather_arm", "primaries", "coverts")


def island_rect(u, v, w, h, d):
    """Full box-UV footprint of an island (all six faces)."""
    return (u, v, u + 2 * (w + d), v + d + h)


def paint_box(sheet, u, v, w, h, d, art):
    """Paint a box's UV layout from a w x h front-face art image."""
    front = art.resize((w, h), Image.NEAREST)
    back = front.transpose(Image.FLIP_LEFT_RIGHT)
    dark = ImageEnhance.Brightness(front).enhance(0.75)

    # Vanilla box layout: up (u+d,v), down (u+d+w,v), right (u,v+d),
    # front (u+d,v+d), left (u+d+w,v+d), back (u+2d+w,v+d).
    sheet.paste(dark.resize((w, d), Image.NEAREST), (u + d, v))
    sheet.paste(dark.resize((w, d), Image.NEAREST), (u + d + w, v))
    sheet.paste(dark.resize((d, h), Image.NEAREST), (u, v + d))
    sheet.paste(front, (u + d, v + d))
    sheet.paste(dark.resize((d, h), Image.NEAREST), (u + d + w, v + d))
    sheet.paste(back, (u + 2 * d + w, v + d))


def build_sheet(base_path):
    """Build the 64x64 articulated sheet from a legacy 64x32 base texture."""
    base = Image.open(base_path).convert("RGBA")
    face = base.crop(LEGACY_FRONT)  # 10x20 front-face art
    sheet = Image.new("RGBA", (SHEET, SHEET), (0, 0, 0, 0))
    for name, (u, v, w, h, d, crop, brighten) in ISLANDS.items():
        art = face.crop(crop)
        if brighten != 1.0:
            art = ImageEnhance.Brightness(art).enhance(brighten)
        paint_box(sheet, u, v, w, h, d, art)
    return sheet


def colorize(img, dark, light, band=False):
    """Map luminance onto a dark->light palette, preserving alpha; optional
    feather banding constrained to the plumage islands."""
    px = img.load()
    band_rects = [island_rect(*ISLANDS[n][:5]) for n in BAND_ISLANDS] if band else []
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
            if band and y % 3 == 2 and any(x0 <= x < x1 and y0 <= y < y1
                                           for (x0, y0, x1, y1) in band_rects):
                nr, ng, nb = nr * 0.82, ng * 0.82, nb * 0.82
            px[x, y] = (int(nr), int(ng), int(nb), a)
    return img


# output name -> (source base file, palette dark, palette light, feather banding)
# palette None = keep the source art's own colors.
VARIANTS = {
    "pixie_wings.png": ("pixie_wings_base.png", None, None, False),
    "wyvern_wings.png": ("wyvern_wings_base.png", None, None, False),
    # feathered warm-brown plumage from the wyvern art
    "avian_wings.png": ("wyvern_wings_base.png", (62, 42, 26), (214, 178, 132), True),
    # pink-lavender shift of the pixie gossamer for faeries
    "faerie_wings.png": ("pixie_wings_base.png", (96, 44, 96), (255, 196, 240), False),
    # elemental drake recolors
    "fire_drake_wings.png": ("drake_wings.png", (64, 12, 6), (255, 126, 44), False),
    "ice_drake_wings.png": ("drake_wings.png", (22, 52, 84), (176, 232, 255), False),
    "terra_drake_wings.png": ("drake_wings.png", (42, 36, 28), (172, 150, 118), False),
    "volt_drake_wings.png": ("drake_wings.png", (44, 38, 12), (250, 222, 96), False),
}


def main():
    for out_name, (base_name, dark, light, band) in VARIANTS.items():
        sheet = build_sheet(os.path.join(DIR, base_name))
        if dark is not None:
            sheet = colorize(sheet, dark, light, band)
        sheet.save(os.path.join(DIR, out_name))
        print("wrote", os.path.join(DIR, out_name))


if __name__ == "__main__":
    main()
