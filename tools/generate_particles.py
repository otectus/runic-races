#!/usr/bin/env python3
"""
Generates the 16x16 sprite frames for the six runic_races custom particles into
src/main/resources/assets/runic_races/textures/particle/ (3 frames each, matching
the frame lists in assets/runic_races/particles/*.json).

Procedural, shippable-quality art in the same spirit as
generate_icons.py — bespoke art can overwrite these PNGs at any time without
code changes. Run from repo root:  python3 tools/generate_particles.py
"""
import math
import os
import random

from PIL import Image, ImageDraw

OUT = "src/main/resources/assets/runic_races/textures/particle"
SIZE = 16
FRAMES = 3


def canvas():
    return Image.new("RGBA", (SIZE, SIZE), (0, 0, 0, 0))


def soft_dot(draw, cx, cy, r, color, layers=4):
    """Radial soft glow: stacked translucent circles."""
    cr, cg, cb, ca = color
    for i in range(layers, 0, -1):
        t = i / layers
        rad = r * t
        alpha = int(ca * (1.0 - t * 0.75))
        draw.ellipse([cx - rad, cy - rad, cx + rad, cy + rad], fill=(cr, cg, cb, alpha))
    draw.ellipse([cx - r * 0.3, cy - r * 0.3, cx + r * 0.3, cy + r * 0.3],
                 fill=(min(cr + 80, 255), min(cg + 80, 255), min(cb + 80, 255), ca))


def rune_glyph(frame):
    """Angular cyan glyph strokes, one variant per frame."""
    img = canvas()
    d = ImageDraw.Draw(img)
    rng = random.Random(1300 + frame)
    color = (110, 230, 255, 235)
    glow = (60, 140, 200, 70)
    # faint square glow backdrop
    d.rectangle([3, 3, 12, 12], fill=glow)
    # 4-5 angular strokes hitting a 3x3 lattice of anchor points
    pts = [(4 + 4 * i, 4 + 4 * j) for i in range(3) for j in range(3)]
    strokes = 4 + rng.randint(0, 1)
    cur = rng.choice(pts)
    for _ in range(strokes):
        nxt = rng.choice([p for p in pts if p != cur])
        d.line([cur, nxt], fill=color, width=1)
        cur = nxt
    # bright anchor pixel
    d.point(cur, fill=(220, 255, 255, 255))
    return img


def soul_wisp(frame):
    """Soft teal flame blob with a trailing tail."""
    img = canvas()
    d = ImageDraw.Draw(img)
    sway = [-1, 0, 1][frame]
    soft_dot(d, 8 + sway, 6, 4.5, (90, 220, 200, 220))
    # tail fading downward
    for i, alpha in enumerate((150, 100, 55)):
        soft_dot(d, 8 - sway, 9 + i * 2, 2.2 - i * 0.5, (70, 180, 170, alpha), layers=2)
    return img


def fae_sparkle(frame):
    """4-point white-pink star that rotates slightly per frame."""
    img = canvas()
    d = ImageDraw.Draw(img)
    cx = cy = 8
    length = [6, 5, 6][frame]
    rot = [0.0, math.pi / 8, math.pi / 4][frame]
    color = (255, 215, 245, 240)
    for k in range(4):
        a = rot + k * math.pi / 2
        d.line([(cx, cy), (cx + length * math.cos(a), cy + length * math.sin(a))],
               fill=color, width=1)
    soft_dot(d, cx, cy, 2.2, (255, 240, 255, 255), layers=3)
    return img


def ember_scale(frame):
    """Small orange-red flake with a bright core and dark rim."""
    img = canvas()
    d = ImageDraw.Draw(img)
    rng = random.Random(2600 + frame)
    # flake: irregular quad
    cx, cy = 8, 8
    pts = []
    for k in range(4):
        a = k * math.pi / 2 + rng.uniform(-0.3, 0.3)
        r = 4 + rng.uniform(-0.8, 0.8)
        pts.append((cx + r * math.cos(a), cy + r * math.sin(a)))
    d.polygon(pts, fill=(120, 40, 20, 230), outline=(60, 20, 10, 255))
    soft_dot(d, cx, cy, 2.6, (255, 140, 40, 255), layers=3)
    d.point((cx, cy), fill=(255, 230, 150, 255))
    return img


def frost_mote(frame):
    """Pale-cyan hexagonal crystal mote."""
    img = canvas()
    d = ImageDraw.Draw(img)
    cx, cy = 8, 8
    r = [4.5, 5.0, 4.0][frame]
    rot = [0.0, math.pi / 12, math.pi / 6][frame]
    pts = [(cx + r * math.cos(rot + k * math.pi / 3), cy + r * math.sin(rot + k * math.pi / 3))
           for k in range(6)]
    d.polygon(pts, outline=(190, 240, 255, 240))
    for k in range(3):
        a = rot + k * math.pi / 3
        d.line([(cx - r * math.cos(a), cy - r * math.sin(a)),
                (cx + r * math.cos(a), cy + r * math.sin(a))],
               fill=(160, 225, 255, 180), width=1)
    soft_dot(d, cx, cy, 1.8, (230, 250, 255, 255), layers=2)
    return img


def venom_drip(frame):
    """Green droplet elongating as it falls."""
    img = canvas()
    d = ImageDraw.Draw(img)
    cx = 8
    stretch = [0, 1, 2][frame]
    top, bottom = 5 - stretch, 11 + stretch
    d.polygon([(cx, top), (cx + 3, bottom - 3), (cx, bottom), (cx - 3, bottom - 3)],
              fill=(70, 180, 40, 230), outline=(40, 110, 20, 255))
    d.point((cx - 1, bottom - 4), fill=(180, 255, 140, 255))
    return img


GENERATORS = {
    "rune_glyph": rune_glyph,
    "soul_wisp": soul_wisp,
    "fae_sparkle": fae_sparkle,
    "ember_scale": ember_scale,
    "frost_mote": frost_mote,
    "venom_drip": venom_drip,
}


def main():
    os.makedirs(OUT, exist_ok=True)
    for name, gen in GENERATORS.items():
        for frame in range(FRAMES):
            path = os.path.join(OUT, "%s_%d.png" % (name, frame))
            gen(frame).save(path)
            print("wrote", path)


if __name__ == "__main__":
    main()
