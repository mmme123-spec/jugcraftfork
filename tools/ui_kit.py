"""Themed screen parts (docs/features/themed-screens.md): nine-slice frames, buttons, tabs and backgrounds drawn by code
for the screens that were still plain rectangles and vanilla grey buttons.

Two looks, after the owner's art direction and the contributor's interface references of 18 October 2026, used for
mood only (nothing is traced or copied; every pixel here is drawn by this module):

- "arcane": the Concordance's screens. A gilt frame with small star corners round a deep indigo star-chart page, with a
  faint drawn sigil; violet plates edged in gold for buttons; ribbon bookmarks for tabs.
- "homestead": the companions', kitchen and farm screens. A carved oak frame with iron corner brackets round warm
  parchment; plank buttons with brass pins.
"""
import math
import random

from PIL import Image, ImageDraw

# Palettes, dark to light.
ARCANE = {
    "outline": (10, 6, 20), "gold": [(92, 64, 22), (150, 112, 44), (206, 166, 82), (246, 218, 140)],
    "page": [(16, 12, 32), (24, 18, 46), (32, 24, 60)], "rim": (52, 38, 92),
    "star": [(120, 104, 190), (190, 176, 250), (255, 246, 214)], "glow": (168, 120, 255),
    "plate": [(30, 20, 56), (52, 36, 96), (78, 56, 140), (110, 84, 186)],
    "text": (242, 236, 255), "title": (255, 217, 138),
}
HOMESTEAD = {
    "outline": (34, 20, 10), "wood": [(70, 42, 20), (104, 66, 32), (138, 92, 48), (170, 120, 66), (196, 148, 88)],
    "iron": [(40, 40, 44), (78, 78, 84), (122, 122, 128)],
    "paper": [(196, 170, 120), (222, 200, 152), (236, 220, 178), (246, 234, 202)],
    "brass": [(120, 84, 24), (196, 150, 58), (240, 206, 112)],
    "text": (64, 44, 24), "title": (104, 58, 20),
}


def _rng(seed):
    return random.Random(seed)


def arcane_frame(size=32, border=9):
    """The arcane nine-slice frame: outline, a two-tone gilt rim, an indigo inner rule, star corners. The centre is
    transparent; the page is drawn under it."""
    p = ARCANE
    img = Image.new("RGBA", (size, size), (0, 0, 0, 0))
    d = ImageDraw.Draw(img)
    s = size - 1
    d.rectangle((0, 0, s, s), outline=p["outline"])
    # Gilt rim: light on the top/left, dark on the bottom/right.
    for i, (light, dark) in enumerate(((p["gold"][3], p["gold"][1]), (p["gold"][2], p["gold"][0]))):
        o = 1 + i
        d.line((o, o, s - o, o), fill=light)
        d.line((o, o, o, s - o), fill=light)
        d.line((o, s - o, s - o, s - o), fill=dark)
        d.line((s - o, o, s - o, s - o), fill=dark)
    d.rectangle((3, 3, s - 3, s - 3), outline=p["outline"])
    d.rectangle((4, 4, s - 4, s - 4), outline=p["rim"])
    # Star corners: a small four-pointed gold star over each corner of the rim.
    for cx, cy in ((4, 4), (s - 4, 4), (4, s - 4), (s - 4, s - 4)):
        for dx, dy, c in ((0, 0, p["star"][2]), (-1, 0, p["gold"][3]), (1, 0, p["gold"][3]), (0, -1, p["gold"][3]),
                          (0, 1, p["gold"][3]), (-2, 0, p["gold"][2]), (2, 0, p["gold"][2]), (0, -2, p["gold"][2]),
                          (0, 2, p["gold"][2])):
            img.putpixel((cx + dx, cy + dy), c + (255,))
    return img


def arcane_page(w=256, h=256, seed=7):
    """The arcane page: deep indigo, darker to the edges, with a scatter of faint stars (tiles seamlessly)."""
    p = ARCANE
    img = Image.new("RGBA", (w, h), p["page"][1] + (255,))
    px = img.load()
    rng = _rng(seed)
    for y in range(h):
        for x in range(w):
            n = rng.random()
            px[x, y] = (p["page"][0] if n < 0.18 else p["page"][2] if n > 0.94 else p["page"][1]) + (255,)
    for _ in range(w * h // 260):
        x, y = rng.randrange(w), rng.randrange(h)
        px[x, y] = rng.choice(p["star"][:2]) + (255,)
    for _ in range(w * h // 4000):
        x, y = rng.randrange(1, w - 1), rng.randrange(1, h - 1)
        px[x, y] = p["star"][2] + (255,)
        for dx, dy in ((1, 0), (-1, 0), (0, 1), (0, -1)):
            px[x + dx, y + dy] = p["star"][0] + (255,)
    return img


def sigil(size=128):
    """A faint drawn sigil for the page's middle: two rings, an eight-pointed star and small nodes, in pale violet with
    transparency (drawn under the text)."""
    img = Image.new("RGBA", (size, size), (0, 0, 0, 0))
    d = ImageDraw.Draw(img)
    c = size / 2
    line = (150, 120, 230, 70)
    node = (200, 170, 255, 110)
    for r in (c - 2, c - 12):
        d.ellipse((c - r, c - r, c + r, c + r), outline=line)
    pts = [(c + (c - 12) * math.cos(a), c + (c - 12) * math.sin(a)) for a in [i * math.pi / 4 - math.pi / 2 for i in range(8)]]
    for i in range(8):
        d.line((pts[i], pts[(i + 3) % 8]), fill=line)
    for x, y in pts:
        d.ellipse((x - 2, y - 2, x + 2, y + 2), fill=node)
    d.ellipse((c - 6, c - 6, c + 6, c + 6), outline=node)
    return img


def arcane_button(state="normal", w=40, h=20):
    """An arcane button (nine-slice): a violet plate edged in gold; hovered, it lights up; inactive, it dims."""
    p = ARCANE
    img = Image.new("RGBA", (w, h), (0, 0, 0, 0))
    d = ImageDraw.Draw(img)
    plate = {"normal": p["plate"][1], "hover": p["plate"][2], "off": p["plate"][0]}[state]
    edge = {"normal": p["gold"][2], "hover": p["gold"][3], "off": p["gold"][0]}[state]
    d.rectangle((0, 0, w - 1, h - 1), fill=p["outline"])
    d.rectangle((1, 1, w - 2, h - 2), fill=edge)
    d.rectangle((2, 2, w - 3, h - 3), fill=plate)
    d.line((2, 2, w - 3, 2), fill=p["plate"][3] if state != "off" else p["plate"][1])
    d.line((2, h - 3, w - 3, h - 3), fill=p["plate"][0])
    # Cut corners.
    for x, y in ((0, 0), (w - 1, 0), (0, h - 1), (w - 1, h - 1)):
        img.putpixel((x, y), (0, 0, 0, 0))
    return img


def ribbon(color, active, w=22, h=24):
    """A bookmark ribbon tab hanging off the page's left edge, its tail notched; active, it is longer and lit."""
    img = Image.new("RGBA", (w, h), (0, 0, 0, 0))
    d = ImageDraw.Draw(img)
    dark = tuple(int(v * 0.55) for v in color)
    light = tuple(min(255, int(v * 1.3)) for v in color)
    reach = w if active else w - 4
    d.polygon([(w - reach, 0), (w - 1, 0), (w - 1, h - 1), (w - reach, h - 1), (w - reach + 4, h // 2)], fill=dark)
    d.polygon([(w - reach + 1, 1), (w - 2, 1), (w - 2, h - 2), (w - reach + 1, h - 2), (w - reach + 5, h // 2)],
              fill=light if active else color)
    return img


def homestead_frame(size=32):
    """The homestead nine-slice frame (border 5): oak boards with the grain running along each side, mitred at the corners,
    an iron bracket with a rivet on each corner. The centre is transparent; the parchment is drawn under it."""
    p = HOMESTEAD
    img = Image.new("RGBA", (size, size), (0, 0, 0, 0))
    d = ImageDraw.Draw(img)
    s = size - 1
    rng = _rng(31)
    wood = p["wood"]
    # Grain: runs of one tone along the board, two lines of grain per board.
    for i in range(1, 4):
        tone = wood[3] if i == 1 else wood[2] if i == 2 else wood[1]
        for a in range(i, size - i):
            run = rng.random() < 0.2
            c = wood[1] if run and i == 2 else tone
            for x, y in ((a, i), (a, s - i), (i, a), (s - i, a)):
                img.putpixel((x, y), c + (255,))
    d.rectangle((0, 0, s, s), outline=p["outline"])
    d.rectangle((4, 4, s - 4, s - 4), outline=p["outline"])
    # Mitre seams.
    for i in range(1, 4):
        for x, y in ((i, i), (s - i, i), (i, s - i), (s - i, s - i)):
            img.putpixel((x, y), wood[0] + (255,))
    # Iron corner brackets with a rivet.
    for cx, cy, sx, sy in ((1, 1, 1, 1), (s - 1, 1, -1, 1), (1, s - 1, 1, -1), (s - 1, s - 1, -1, -1)):
        for i in range(6):
            img.putpixel((cx + sx * i, cy), p["iron"][1] + (255,))
            img.putpixel((cx, cy + sy * i), p["iron"][1] + (255,))
        img.putpixel((cx + sx * 2, cy + sy * 2), p["iron"][2] + (255,))
        img.putpixel((cx + sx * 1, cy + sy * 1), p["iron"][0] + (255,))
    return img


def parchment(w=256, h=256, seed=11):
    """Warm parchment: soft blotches (a coarse noise smoothed out) and a sparse fleck, tiling seamlessly."""
    p = HOMESTEAD
    rng = _rng(seed)
    cell = 32
    gw, gh = w // cell, h // cell
    grid = [[rng.random() for _ in range(gw)] for _ in range(gh)]
    img = Image.new("RGBA", (w, h))
    px = img.load()
    for y in range(h):
        for x in range(w):
            fx, fy = x / cell, y / cell
            x0, y0 = int(fx) % gw, int(fy) % gh
            x1, y1 = (x0 + 1) % gw, (y0 + 1) % gh
            tx, ty = fx - int(fx), fy - int(fy)
            tx, ty = tx * tx * (3 - 2 * tx), ty * ty * (3 - 2 * ty)
            v = (grid[y0][x0] * (1 - tx) + grid[y0][x1] * tx) * (1 - ty) + (grid[y1][x0] * (1 - tx) + grid[y1][x1] * tx) * ty
            c = p["paper"][1] if v < 0.16 else p["paper"][3] if v > 0.84 else p["paper"][2]
            if rng.random() < 0.004:
                c = p["paper"][0]
            px[x, y] = c + (255,)
    return img


def plank_button(state="normal", w=40, h=20):
    """A homestead button (nine-slice): an oak plank with a brass pin at each end; hovered, it is lighter."""
    p = HOMESTEAD
    img = Image.new("RGBA", (w, h), (0, 0, 0, 0))
    d = ImageDraw.Draw(img)
    face = {"normal": p["wood"][2], "hover": p["wood"][3], "off": p["wood"][1]}[state]
    d.rectangle((0, 0, w - 1, h - 1), fill=p["outline"])
    d.rectangle((1, 1, w - 2, h - 2), fill=face)
    d.line((1, 1, w - 2, 1), fill=p["wood"][4] if state != "off" else p["wood"][2])
    d.line((1, h - 2, w - 2, h - 2), fill=p["wood"][0])
    d.line((1, h // 2, w - 2, h // 2), fill=p["wood"][1])
    for x in (3, w - 4):
        img.putpixel((x, 4), p["brass"][2] + (255,))
        img.putpixel((x, h - 5), p["brass"][1] + (255,))
    return img


def nine(img, src, box, border):
    """Paste a nine-slice `src` stretched over `box` (x, y, w, h) onto `img` (for the mock-ups)."""
    x, y, w, h = box
    sw, sh = src.size
    b = border
    parts = [((0, 0, b, b), (x, y, b, b)), ((sw - b, 0, sw, b), (x + w - b, y, b, b)),
             ((0, sh - b, b, sh), (x, y + h - b, b, b)), ((sw - b, sh - b, sw, sh), (x + w - b, y + h - b, b, b)),
             ((b, 0, sw - b, b), (x + b, y, w - 2 * b, b)), ((b, sh - b, sw - b, sh), (x + b, y + h - b, w - 2 * b, b)),
             ((0, b, b, sh - b), (x, y + b, b, h - 2 * b)), ((sw - b, b, sw, sh - b), (x + w - b, y + b, b, h - 2 * b)),
             ((b, b, sw - b, sh - b), (x + b, y + b, w - 2 * b, h - 2 * b))]
    for (sx0, sy0, sx1, sy1), (dx, dy, dw, dh) in parts:
        if dw > 0 and dh > 0:
            piece = src.crop((sx0, sy0, sx1, sy1)).resize((dw, dh), Image.NEAREST)
            img.alpha_composite(piece, (dx, dy))


def corner_star(size=9):
    """A small four-pointed gilt star for the arcane frame's corners: a pale centre, gold arms, dark tips."""
    p = ARCANE
    img = Image.new("RGBA", (size, size), (0, 0, 0, 0))
    c = size // 2
    for r in range(c + 1):
        tone = p["star"][2] if r == 0 else p["gold"][3] if r == 1 else p["gold"][2] if r < c else p["gold"][1]
        for dx, dy in ((r, 0), (-r, 0), (0, r), (0, -r)):
            img.putpixel((c + dx, c + dy), tone + (255,))
    for dx, dy in ((1, 1), (-1, 1), (1, -1), (-1, -1)):
        img.putpixel((c + dx, c + dy), p["gold"][1] + (255,))
    return img


# The textures the Concordance Journal's GuiLib stylesheet (assets/jugcraft/ui/journal.css) and its plain screen
# draw. Run `python tools/ui_kit.py` to write them again.
CONCORDANCE = {
    "textures/gui/concordance/star_page.png": lambda: arcane_page(128, 128),
    "textures/gui/concordance/sigil.png": lambda: sigil(128),
    "textures/gui/concordance/corner_star.png": corner_star,
}


def write_all(assets):
    for path, draw in CONCORDANCE.items():
        out = assets / path
        out.parent.mkdir(parents=True, exist_ok=True)
        draw().save(out)


if __name__ == "__main__":
    from pathlib import Path
    write_all(Path(__file__).resolve().parent.parent / "src" / "main" / "resources" / "assets" / "jugcraft")
