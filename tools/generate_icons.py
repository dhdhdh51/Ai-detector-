#!/usr/bin/env python3
"""
Generates the legacy (pre-API-26) PNG launcher icons for FitBudget from the same
original brand mark used by the adaptive vector icon.

Requires Pillow:  pip install Pillow
Usage:            python3 tools/generate_icons.py
"""
import os

from PIL import Image, ImageDraw

RES = os.path.join(os.path.dirname(os.path.dirname(os.path.abspath(__file__))), "app", "src", "main", "res")

DENSITIES = {
    "mipmap-mdpi": 48,
    "mipmap-hdpi": 72,
    "mipmap-xhdpi": 96,
    "mipmap-xxhdpi": 144,
    "mipmap-xxxhdpi": 192,
}

TOP = (10, 58, 41)
BOTTOM = (4, 20, 15)
WHITE = (255, 255, 255)
MINT = (185, 232, 208)
AMBER = (242, 169, 59)
AMBER_LIGHT = (255, 200, 117)
AMBER_DARK = (107, 65, 0)

SS = 8  # supersampling factor


def gradient_square(size):
    img = Image.new("RGBA", (size, size))
    d = ImageDraw.Draw(img)
    for y in range(size):
        t = y / max(size - 1, 1)
        d.line(
            [(0, y), (size, y)],
            fill=(
                int(TOP[0] + (BOTTOM[0] - TOP[0]) * t),
                int(TOP[1] + (BOTTOM[1] - TOP[1]) * t),
                int(TOP[2] + (BOTTOM[2] - TOP[2]) * t),
                255,
            ),
        )
    return img


def draw_mark(img, size):
    """Draws the F + coin mark on an existing square image."""
    d = ImageDraw.Draw(img)
    u = size / 108.0  # brand mark is authored on a 108 unit grid

    def r(x0, y0, x1, y1, radius, fill):
        d.rounded_rectangle([x0 * u, y0 * u, x1 * u, y1 * u], radius=radius * u, fill=fill)

    # F stem + arms (shifted slightly left/up to balance the coin)
    r(24, 24, 37, 82, 4, WHITE)
    r(24, 24, 66, 35, 5, WHITE)
    r(24, 45, 58, 56, 5, MINT)

    # Dumbbell plate
    r(68, 20, 78, 39, 3, AMBER)

    # Coin
    cx, cy, rad = 68 * u, 68 * u, 15 * u
    d.ellipse([cx - rad, cy - rad, cx + rad, cy + rad], fill=AMBER)
    rad2 = 11 * u
    d.ellipse([cx - rad2, cy - rad2, cx + rad2, cy + rad2], fill=AMBER_LIGHT)

    # Rupee glyph
    w = max(int(2.4 * u), 1)
    d.line([(61 * u, 63 * u), (75 * u, 63 * u)], fill=AMBER_DARK, width=w)
    d.line([(61 * u, 67.5 * u), (75 * u, 67.5 * u)], fill=AMBER_DARK, width=w)
    d.arc([63 * u, 66 * u, 75 * u, 78 * u], start=-80, end=80, fill=AMBER_DARK, width=w)
    d.line([(64 * u, 74 * u), (73 * u, 82 * u)], fill=AMBER_DARK, width=w)


def make_icon(size, round_icon=False):
    big = size * SS
    base = gradient_square(big)

    mask = Image.new("L", (big, big), 0)
    md = ImageDraw.Draw(mask)
    if round_icon:
        md.ellipse([0, 0, big - 1, big - 1], fill=255)
    else:
        md.rounded_rectangle([0, 0, big - 1, big - 1], radius=int(big * 0.22), fill=255)

    draw_mark(base, big)

    out = Image.new("RGBA", (big, big), (0, 0, 0, 0))
    out.paste(base, (0, 0), mask)
    return out.resize((size, size), Image.LANCZOS)


def main():
    for folder, size in DENSITIES.items():
        target = os.path.join(RES, folder)
        os.makedirs(target, exist_ok=True)
        make_icon(size, False).save(os.path.join(target, "ic_launcher.png"))
        make_icon(size, True).save(os.path.join(target, "ic_launcher_round.png"))
        print("wrote", folder, size)

    # A larger marketing/README asset
    docs = os.path.join(os.path.dirname(RES), "..", "..", "..", "docs")
    docs = os.path.normpath(docs)
    os.makedirs(docs, exist_ok=True)
    make_icon(512, False).save(os.path.join(docs, "fitbudget-icon.png"))
    print("wrote", os.path.join(docs, "fitbudget-icon.png"))


if __name__ == "__main__":
    main()
