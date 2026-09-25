#!/usr/bin/env python3
"""
Generates every raster launcher icon for FitBudget from one original brand mark:

  * Android legacy (pre-API-26) PNG mipmaps, square and round
  * iOS AppIcon (1024x1024, fully opaque as the App Store requires)
  * iOS launch-screen logo (used by UILaunchScreen)
  * a docs/README asset

The adaptive Android icon and the SwiftUI in-app logo are vectors and are not produced here.

Requires Pillow:  pip install Pillow
Usage:            python3 tools/generate_icons.py
"""
import os

from PIL import Image, ImageDraw

ROOT = os.path.dirname(os.path.dirname(os.path.abspath(__file__)))
ANDROID_RES = os.path.join(ROOT, "app", "src", "main", "res")
IOS_ASSETS = os.path.join(ROOT, "ios", "FitBudget", "Resources", "Assets.xcassets")
DOCS = os.path.join(ROOT, "docs")

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


def draw_mark(img, size, scale=1.0):
    """Draws the F + coin mark on an existing square image."""
    d = ImageDraw.Draw(img)
    u = size / 108.0 * scale  # brand mark is authored on a 108 unit grid
    offset = (size - size * scale) / 2

    def r(x0, y0, x1, y1, radius, fill):
        d.rounded_rectangle(
            [x0 * u + offset, y0 * u + offset, x1 * u + offset, y1 * u + offset],
            radius=radius * u,
            fill=fill,
        )

    # F stem + arms (shifted slightly left/up to balance the coin)
    r(24, 24, 37, 82, 4, WHITE)
    r(24, 24, 66, 35, 5, WHITE)
    r(24, 45, 58, 56, 5, MINT)

    # Dumbbell plate
    r(68, 20, 78, 39, 3, AMBER)

    # Coin
    cx, cy, rad = 68 * u + offset, 68 * u + offset, 15 * u
    d.ellipse([cx - rad, cy - rad, cx + rad, cy + rad], fill=AMBER)
    rad2 = 11 * u
    d.ellipse([cx - rad2, cy - rad2, cx + rad2, cy + rad2], fill=AMBER_LIGHT)

    # Rupee glyph
    w = max(int(2.4 * u), 1)
    d.line([(61 * u + offset, 63 * u + offset), (75 * u + offset, 63 * u + offset)], fill=AMBER_DARK, width=w)
    d.line([(61 * u + offset, 67.5 * u + offset), (75 * u + offset, 67.5 * u + offset)], fill=AMBER_DARK, width=w)
    d.arc(
        [63 * u + offset, 66 * u + offset, 75 * u + offset, 78 * u + offset],
        start=-80, end=80, fill=AMBER_DARK, width=w,
    )
    d.line([(64 * u + offset, 74 * u + offset), (73 * u + offset, 82 * u + offset)], fill=AMBER_DARK, width=w)


def make_icon(size, round_icon=False, squircle=True, scale=1.0):
    big = size * SS
    base = gradient_square(big)
    draw_mark(base, big, scale=scale)

    if not squircle and not round_icon:
        # Fully opaque square: what the iOS App Store requires (the OS masks it itself).
        return base.convert("RGB").resize((size, size), Image.LANCZOS)

    mask = Image.new("L", (big, big), 0)
    md = ImageDraw.Draw(mask)
    if round_icon:
        md.ellipse([0, 0, big - 1, big - 1], fill=255)
    else:
        md.rounded_rectangle([0, 0, big - 1, big - 1], radius=int(big * 0.22), fill=255)

    out = Image.new("RGBA", (big, big), (0, 0, 0, 0))
    out.paste(base, (0, 0), mask)
    return out.resize((size, size), Image.LANCZOS)


def make_launch_logo(size):
    """Transparent-background mark for the iOS launch screen."""
    big = size * SS
    canvas = Image.new("RGBA", (big, big), (0, 0, 0, 0))
    badge = Image.new("RGBA", (big, big))
    bd = ImageDraw.Draw(badge)
    bd.rounded_rectangle([0, 0, big - 1, big - 1], radius=int(big * 0.24), fill=(0, 135, 90, 255))
    draw_mark(badge, big, scale=0.82)
    mask = Image.new("L", (big, big), 0)
    ImageDraw.Draw(mask).rounded_rectangle([0, 0, big - 1, big - 1], radius=int(big * 0.24), fill=255)
    canvas.paste(badge, (0, 0), mask)
    return canvas.resize((size, size), Image.LANCZOS)


def main():
    # ---- Android legacy mipmaps ----
    for folder, size in DENSITIES.items():
        target = os.path.join(ANDROID_RES, folder)
        os.makedirs(target, exist_ok=True)
        make_icon(size, round_icon=False).save(os.path.join(target, "ic_launcher.png"))
        make_icon(size, round_icon=True).save(os.path.join(target, "ic_launcher_round.png"))
        print("android:", folder, size)

    # ---- iOS app icon (opaque, single size) ----
    appicon = os.path.join(IOS_ASSETS, "AppIcon.appiconset")
    os.makedirs(appicon, exist_ok=True)
    make_icon(1024, squircle=False).save(os.path.join(appicon, "AppIcon-1024.png"))
    print("ios: AppIcon-1024.png")

    # ---- iOS launch screen logo ----
    launch = os.path.join(IOS_ASSETS, "LaunchLogo.imageset")
    os.makedirs(launch, exist_ok=True)
    for scale, name in ((1, "LaunchLogo.png"), (2, "LaunchLogo@2x.png"), (3, "LaunchLogo@3x.png")):
        make_launch_logo(160 * scale).save(os.path.join(launch, name))
    print("ios: LaunchLogo @1x/@2x/@3x")

    # ---- shared docs asset ----
    os.makedirs(DOCS, exist_ok=True)
    make_icon(512).save(os.path.join(DOCS, "fitbudget-icon.png"))
    print("docs: fitbudget-icon.png")


if __name__ == "__main__":
    main()
