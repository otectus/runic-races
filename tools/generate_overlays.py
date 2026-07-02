#!/usr/bin/env python3
"""
Generates the screen-overlay textures used by ScreenCueRenderer into
src/main/resources/assets/runic_races/textures/gui/overlay/.

All textures are white-with-alpha so Java tints them per cue via
RenderSystem.setShaderColor. Bespoke art can overwrite any output PNG.
Run from repo root:  python3 tools/generate_overlays.py

- vignette_radial.png (256x256): soft radial vignette — transparent center,
  opaque edges (smoothstep falloff). Replaces the old flat edge-bar rects.
- frost_rime.png (256x256): crystalline dendrites growing in from the edges.
- heat_haze.png (64x256): horizontally-tileable soft noise bands for the
  heat-shimmer wobble strips.
"""
import math
import os
import random

from PIL import Image, ImageDraw, ImageFilter

OUT = "src/main/resources/assets/runic_races/textures/gui/overlay"


def smoothstep(t):
    t = max(0.0, min(1.0, t))
    return t * t * (3 - 2 * t)


def vignette_radial(size=256):
    img = Image.new("RGBA", (size, size), (255, 255, 255, 0))
    px = img.load()
    c = (size - 1) / 2.0
    # Alpha 0 out to ~45% radius, smoothstep to full at the corners.
    max_r = math.hypot(c, c)
    for y in range(size):
        for x in range(size):
            r = math.hypot(x - c, y - c) / max_r
            a = smoothstep((r - 0.45) / 0.55)
            px[x, y] = (255, 255, 255, int(a * 255))
    return img


def frost_rime(size=256):
    """Dendrites: random walks growing inward from each edge, blurred once."""
    img = Image.new("RGBA", (size, size), (255, 255, 255, 0))
    d = ImageDraw.Draw(img)
    rng = random.Random(1420)

    def grow(x, y, angle, depth, length):
        if depth <= 0:
            return
        nx = x + math.cos(angle) * length
        ny = y + math.sin(angle) * length
        alpha = int(200 * depth / 6)
        d.line([(x, y), (nx, ny)], fill=(255, 255, 255, alpha), width=max(1, depth // 3))
        # branch
        for da in (-0.5, 0.5):
            if rng.random() < 0.65:
                grow(nx, ny, angle + da + rng.uniform(-0.2, 0.2),
                     depth - 1, length * 0.75)

    for edge in range(4):
        for i in range(10):
            t = (i + 0.5) / 10 * size
            if edge == 0:
                grow(t, 0, math.pi / 2 + rng.uniform(-0.4, 0.4), 6, size * 0.09)
            elif edge == 1:
                grow(t, size - 1, -math.pi / 2 + rng.uniform(-0.4, 0.4), 6, size * 0.09)
            elif edge == 2:
                grow(0, t, 0 + rng.uniform(-0.4, 0.4), 6, size * 0.09)
            else:
                grow(size - 1, t, math.pi + rng.uniform(-0.4, 0.4), 6, size * 0.09)

    img = img.filter(ImageFilter.GaussianBlur(0.6))
    # Layer a faint radial vignette under the dendrites so the frosting reads at the rim.
    vig = vignette_radial(size)
    vig.putalpha(vig.split()[3].point(lambda a: a // 3))
    base = Image.new("RGBA", (size, size), (255, 255, 255, 0))
    base.alpha_composite(vig)
    base.alpha_composite(img)
    return base


def heat_haze(w=64, h=256):
    """Soft horizontal noise bands; tileable in X (noise sampled on a cylinder)."""
    img = Image.new("RGBA", (w, h), (255, 255, 255, 0))
    px = img.load()
    rng = random.Random(777)
    phases = [(rng.uniform(0, math.tau), rng.uniform(1, 3), rng.uniform(0.05, 0.2))
              for _ in range(5)]
    for y in range(h):
        for x in range(w):
            theta = x / w * math.tau
            v = 0.0
            for (p, freq, yfreq) in phases:
                v += math.sin(theta * freq + p + y * yfreq)
            v = (v / 5 + 1) / 2  # 0..1
            a = smoothstep((v - 0.55) / 0.45) * 0.85
            px[x, y] = (255, 255, 255, int(a * 255))
    return img


def main():
    os.makedirs(OUT, exist_ok=True)
    for name, img in (("vignette_radial.png", vignette_radial()),
                      ("frost_rime.png", frost_rime()),
                      ("heat_haze.png", heat_haze())):
        path = os.path.join(OUT, name)
        img.save(path)
        print("wrote", path)


if __name__ == "__main__":
    main()
