#!/usr/bin/env python3
"""Generate the launcher icon resources from moalogo.png (needs Pillow).

    python3 app/icon-src/make_icons.py [--preview preview.png]

Writes into app/src/main/res:
  mipmap-<density>/ic_launcher_foreground.png   adaptive icon foreground (108dp)
  mipmap-<density>/ic_launcher_monochrome.png   themed icon layer (alpha only)
  mipmap-<density>/ic_launcher.png              legacy icon for Android 6-7 (48dp)
The adaptive icon itself is mipmap-anydpi-v26/ic_launcher.xml (white background).
"""
import argparse
import os

from PIL import Image, ImageDraw

HERE = os.path.dirname(os.path.abspath(__file__))
RES = os.path.join(HERE, "..", "src", "main", "res")
DENSITIES = {"mdpi": 1.0, "hdpi": 1.5, "xhdpi": 2.0, "xxhdpi": 3.0, "xxxhdpi": 4.0}

# The logo is drawn inside the 66dp safe zone of the 108dp adaptive canvas,
# so launcher masks (circle, squircle, ...) never cut it.
CANVAS_DP = 108
LOGO_DP = 54
LEGACY_DP = 48


def logo():
    return Image.open(os.path.join(HERE, "moalogo.png")).convert("RGBA")


def monochrome(src):
    """Alpha mask for themed icons: the system tints it and ignores colours.

    The dark Maxima letters become solid, the light Android robot faint, and
    white highlights and the soft drop shadow disappear, so the two shapes
    stay distinguishable instead of merging into one silhouette.
    """
    out = Image.new("RGBA", src.size, (0, 0, 0, 0))
    px, opx = src.load(), out.load()
    assert px is not None and opx is not None
    for y in range(src.height):
        for x in range(src.width):
            r, g, b, a = px[x, y]
            if a < 150:  # drop shadow and anti-aliasing fringe of it
                continue
            lum = (0.299 * r + 0.587 * g + 0.114 * b) / 255
            darkness = 1 - lum
            if darkness > 0.45:      # letters
                level = 1.0
            elif darkness > 0.08:    # robot
                level = 0.4
            else:                    # white highlights
                level = 0.0
            opx[x, y] = (255, 255, 255, int(a * level))
    return out


def on_canvas(img, canvas_px, logo_px):
    canvas = Image.new("RGBA", (canvas_px, canvas_px), (0, 0, 0, 0))
    scaled = img.resize((logo_px, logo_px), Image.Resampling.LANCZOS)
    off = (canvas_px - logo_px) // 2
    canvas.alpha_composite(scaled, (off, off))
    return canvas


def write_resources():
    src = logo()
    mono = monochrome(src)
    for name, scale in DENSITIES.items():
        d = os.path.join(RES, "mipmap-" + name)
        os.makedirs(d, exist_ok=True)
        canvas, logo_px = round(CANVAS_DP * scale), round(LOGO_DP * scale)
        on_canvas(src, canvas, logo_px).save(os.path.join(d, "ic_launcher_foreground.png"), optimize=True)
        on_canvas(mono, canvas, logo_px).save(os.path.join(d, "ic_launcher_monochrome.png"), optimize=True)
        # Legacy icon: logo on a white disc, like the adaptive icon in a circle mask.
        size = round(LEGACY_DP * scale)
        legacy = Image.new("RGBA", (size, size), (0, 0, 0, 0))
        ImageDraw.Draw(legacy).ellipse((0, 0, size - 1, size - 1), fill=(255, 255, 255, 255))
        legacy.alpha_composite(on_canvas(src, size, round(size * LOGO_DP / CANVAS_DP * 1.3)))
        legacy.save(os.path.join(d, "ic_launcher.png"), optimize=True)


def preview(path):
    """Unthemed and themed (light and dark) icons in circle and squircle masks."""
    size = 432
    src, mono = logo(), monochrome(logo())
    fg = on_canvas(src, size, round(size * LOGO_DP / CANVAS_DP))
    mono_fg = on_canvas(mono, size, round(size * LOGO_DP / CANVAS_DP))

    def masked(layer_bg, layer_fg, shape):
        icon = Image.new("RGBA", (size, size), layer_bg)
        icon.alpha_composite(layer_fg)
        mask = Image.new("L", (size, size), 0)
        inset = size * 18 // 108  # launchers show the inner 72dp of 108dp
        box = (inset, inset, size - inset, size - inset)
        if shape == "circle":
            ImageDraw.Draw(mask).ellipse(box, fill=255)
        else:
            ImageDraw.Draw(mask).rounded_rectangle(box, radius=size // 6, fill=255)
        out = Image.new("RGBA", (size, size), (0, 0, 0, 0))
        out.paste(icon, (0, 0), mask)
        return out

    def tinted(color):
        layer = Image.new("RGBA", (size, size), color + (0,))
        layer.putalpha(mono_fg.getchannel("A"))
        return layer

    tiles = [
        masked((255, 255, 255, 255), fg, "circle"),
        masked((255, 255, 255, 255), fg, "squircle"),
        masked((214, 232, 200, 255), tinted((40, 70, 40)), "circle"),   # themed, light
        masked((40, 52, 40, 255), tinted((200, 230, 190)), "circle"),   # themed, dark
    ]
    sheet = Image.new("RGBA", (size * len(tiles), size), (120, 120, 140, 255))
    for i, t in enumerate(tiles):
        sheet.alpha_composite(t, (i * size, 0))
    sheet.convert("RGB").resize((sheet.width // 2, sheet.height // 2), Image.Resampling.LANCZOS).save(path)


if __name__ == "__main__":
    ap = argparse.ArgumentParser()
    ap.add_argument("--preview", help="only write a preview image to this path")
    args = ap.parse_args()
    if args.preview:
        preview(args.preview)
    else:
        write_resources()
