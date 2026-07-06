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


def web_strand(frame):
    """White silk strand: a taut diagonal thread with anchor nodes and a slight sag."""
    img = canvas()
    d = ImageDraw.Draw(img)
    sag = [1, 2, 1][frame]
    color = (245, 245, 240, 220)
    dim = (200, 200, 195, 130)
    # main thread from top-left to bottom-right with a midpoint sag
    d.line([(2, 3), (8, 8 + sag)], fill=color, width=1)
    d.line([(8, 8 + sag), (14, 12)], fill=color, width=1)
    # cross-fibre glints
    d.line([(5, 8), (7, 6 + sag)], fill=dim, width=1)
    d.line([(9, 11), (11, 9 + sag)], fill=dim, width=1)
    # anchor nodes
    for x, y in ((2, 3), (8, 8 + sag), (14, 12)):
        d.point((x, y), fill=(255, 255, 255, 255))
    return img


def leaf_petal(frame):
    """Small green leaf with a lighter mid-vein, rotating per frame."""
    img = canvas()
    d = ImageDraw.Draw(img)
    rot = [0.0, math.pi / 6, math.pi / 3][frame]
    cx, cy = 8, 8
    # leaf as two arcs approximated by a rotated ellipse polygon
    pts = []
    for k in range(12):
        a = 2 * math.pi * k / 12
        rx, ry = 5.0, 2.6
        x = rx * math.cos(a)
        y = ry * math.sin(a)
        pts.append((cx + x * math.cos(rot) - y * math.sin(rot),
                    cy + x * math.sin(rot) + y * math.cos(rot)))
    d.polygon(pts, fill=(80, 170, 60, 235), outline=(45, 110, 35, 255))
    # mid-vein
    vx, vy = 4.6 * math.cos(rot), 4.6 * math.sin(rot)
    d.line([(cx - vx, cy - vy), (cx + vx, cy + vy)], fill=(150, 220, 120, 220), width=1)
    return img


def feather_down(frame):
    """Soft white down feather: pale shaft with wispy barbs."""
    img = canvas()
    d = ImageDraw.Draw(img)
    lean = [-1, 0, 1][frame]
    shaft = (235, 232, 225, 235)
    barb = (250, 250, 245, 140)
    d.line([(8 - lean, 3), (8 + lean, 13)], fill=shaft, width=1)
    for i in range(4):
        y = 4 + i * 2
        w = 3 - i // 2
        d.line([(8 - lean, y), (8 - lean - w, y + 2)], fill=barb, width=1)
        d.line([(8 - lean, y), (8 - lean + w, y + 2)], fill=barb, width=1)
    d.point((8 - lean, 3), fill=(255, 255, 255, 255))
    return img


def shadow_wisp(frame):
    """Dark indigo curl — soul-wisp silhouette in shadow tones, faint edge glow."""
    img = canvas()
    d = ImageDraw.Draw(img)
    sway = [-1, 0, 1][frame]
    soft_dot(d, 8 + sway, 7, 4.5, (40, 30, 70, 210))
    for i, alpha in enumerate((130, 85, 45)):
        soft_dot(d, 8 - sway, 10 + i * 2, 2.0 - i * 0.4, (25, 18, 45, alpha), layers=2)
    # thin violet rim so it reads against dark backgrounds
    d.ellipse([4 + sway, 3, 12 + sway, 11], outline=(110, 80, 160, 90))
    return img


def foxfire(frame):
    """Teal-white spirit flame with a licking tip."""
    img = canvas()
    d = ImageDraw.Draw(img)
    flick = [0, 1, -1][frame]
    # flame body
    soft_dot(d, 8, 9, 4.0, (60, 210, 190, 230))
    soft_dot(d, 8 + flick, 6, 2.6, (140, 250, 235, 240), layers=3)
    # tip
    d.line([(8 + flick, 3), (8, 6)], fill=(200, 255, 250, 220), width=1)
    d.point((8 + flick, 3), fill=(240, 255, 255, 255))
    return img


def arcane_glint(frame):
    """Azure 4-point star with a halo — arcane cousin of fae_sparkle."""
    img = canvas()
    d = ImageDraw.Draw(img)
    cx = cy = 8
    length = [6, 5, 6][frame]
    rot = [math.pi / 4, math.pi / 8, 0.0][frame]
    color = (120, 190, 255, 245)
    for k in range(4):
        a = rot + k * math.pi / 2
        d.line([(cx, cy), (cx + length * math.cos(a), cy + length * math.sin(a))],
               fill=color, width=1)
    d.ellipse([cx - 3, cy - 3, cx + 3, cy + 3], outline=(90, 150, 230, 90))
    soft_dot(d, cx, cy, 2.0, (200, 230, 255, 255), layers=3)
    return img


def bone_chip(frame):
    """Bone-white angular shard with a darker fracture line."""
    img = canvas()
    d = ImageDraw.Draw(img)
    rng = random.Random(4200 + frame)
    cx, cy = 8, 8
    pts = []
    for k in range(5):
        a = 2 * math.pi * k / 5 + rng.uniform(-0.35, 0.35)
        r = 3.5 + rng.uniform(-1.0, 1.0)
        pts.append((cx + r * math.cos(a), cy + r * math.sin(a)))
    d.polygon(pts, fill=(228, 220, 200, 240), outline=(150, 140, 120, 255))
    # fracture line
    d.line([pts[0], pts[2]], fill=(170, 160, 140, 200), width=1)
    d.point((cx, cy - 1), fill=(255, 250, 240, 255))
    return img


def mirror_shard(frame):
    """Pale glassy fragment with a bright reflective diagonal, rotating per frame."""
    img = canvas()
    d = ImageDraw.Draw(img)
    rng = random.Random(5100 + frame)
    cx, cy = 8, 8
    rot = [0.0, math.pi / 5, math.pi / 2.5][frame]
    pts = []
    for k in range(4):
        a = rot + 2 * math.pi * k / 4 + rng.uniform(-0.4, 0.4)
        r = 4.2 + rng.uniform(-1.2, 1.2)
        pts.append((cx + r * math.cos(a), cy + r * math.sin(a)))
    d.polygon(pts, fill=(205, 225, 240, 200), outline=(150, 180, 205, 255))
    # reflective streak across the glass
    d.line([pts[0], pts[2]], fill=(255, 255, 255, 230), width=1)
    d.point((int((pts[0][0] + pts[2][0]) / 2), int((pts[0][1] + pts[2][1]) / 2)),
            fill=(255, 255, 255, 255))
    return img


def moon_sliver(frame):
    """Silver crescent mote with a soft halo, waxing slightly per frame."""
    img = canvas()
    d = ImageDraw.Draw(img)
    cx, cy = 8, 8
    outer = 5.0
    inner_off = [2.4, 2.0, 2.8][frame]
    # crescent polygon: outer arc out, inner (offset) arc back
    pts = []
    steps = 14
    for k in range(steps + 1):
        a = math.pi * (-0.5 + k / steps)  # right-side arc, top to bottom
        pts.append((cx + outer * math.cos(a), cy + outer * math.sin(a)))
    for k in range(steps + 1):
        a = math.pi * (0.5 - k / steps)
        pts.append((cx - inner_off + (outer - 0.9) * math.cos(a),
                    cy + (outer - 0.9) * math.sin(a)))
    d.polygon(pts, fill=(215, 225, 245, 235), outline=(240, 245, 255, 255))
    soft_dot(d, cx + 2, cy - 2, 1.4, (255, 255, 255, 220), layers=2)
    return img


def rock_chip(frame):
    """Grey stone shard with a darker fracture line — bone_chip's mineral cousin."""
    img = canvas()
    d = ImageDraw.Draw(img)
    rng = random.Random(5300 + frame)
    cx, cy = 8, 8
    pts = []
    for k in range(5):
        a = 2 * math.pi * k / 5 + rng.uniform(-0.35, 0.35)
        r = 3.5 + rng.uniform(-1.0, 1.0)
        pts.append((cx + r * math.cos(a), cy + r * math.sin(a)))
    d.polygon(pts, fill=(135, 130, 122, 245), outline=(80, 76, 70, 255))
    d.line([pts[1], pts[3]], fill=(95, 90, 84, 220), width=1)
    d.point((cx - 1, cy - 1), fill=(180, 176, 168, 255))
    return img


def pollen_mote(frame):
    """Golden pollen fleck: warm core with drifting satellite specks."""
    img = canvas()
    d = ImageDraw.Draw(img)
    rng = random.Random(5500 + frame)
    soft_dot(d, 8, 8, 2.8, (240, 200, 80, 230), layers=3)
    for _ in range(3):
        ox = rng.randint(-4, 4)
        oy = rng.randint(-4, 4)
        d.point((8 + ox, 8 + oy), fill=(255, 225, 130, 200))
    d.point((8, 8), fill=(255, 245, 190, 255))
    return img


def gale_streak(frame):
    """White wind dash: a tapering horizontal streak with a trailing curl."""
    img = canvas()
    d = ImageDraw.Draw(img)
    length = [10, 12, 9][frame]
    lift = [0, -1, 1][frame]
    x0 = 8 - length // 2
    core = (245, 250, 255, 235)
    fade = (225, 235, 245, 120)
    d.line([(x0, 8 + lift), (x0 + length, 8)], fill=core, width=1)
    d.line([(x0 + 2, 9 + lift), (x0 + length - 3, 9)], fill=fade, width=1)
    # trailing curl
    d.line([(x0, 8 + lift), (x0 + 2, 6 + lift)], fill=fade, width=1)
    d.point((x0 + length, 8), fill=(255, 255, 255, 255))
    return img


GENERATORS = {
    "rune_glyph": rune_glyph,
    "soul_wisp": soul_wisp,
    "fae_sparkle": fae_sparkle,
    "ember_scale": ember_scale,
    "frost_mote": frost_mote,
    "venom_drip": venom_drip,
    "web_strand": web_strand,
    "leaf_petal": leaf_petal,
    "feather_down": feather_down,
    "shadow_wisp": shadow_wisp,
    "foxfire": foxfire,
    "arcane_glint": arcane_glint,
    "bone_chip": bone_chip,
    "mirror_shard": mirror_shard,
    "moon_sliver": moon_sliver,
    "rock_chip": rock_chip,
    "pollen_mote": pollen_mote,
    "gale_streak": gale_streak,
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
