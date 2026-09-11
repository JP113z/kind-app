"""
Genera las tarjetas diarias de Kind (imagen + frase) y los iconos de la app.

Uso:
    python tools/generate_cards.py

Lee  content/quotes.json  y escribe:
    www/img/quotes/<id>.png        -> imagen que se ve en el widget
    www/assets/img/icon-*.png      -> iconos PWA / Android
    android/app/src/main/res/...   -> iconos del launcher

Para agregar una frase nueva: agregala en content/quotes.json y volvé a correr
este script. Si preferís usar una foto propia en lugar del fondo generado,
poné la foto en tools/photos/<id>.jpg y el script la usa como fondo.
"""
import json
import math
import os
from pathlib import Path

from PIL import Image, ImageDraw, ImageFilter, ImageFont

ROOT = Path(__file__).resolve().parents[1]
CONTENT = ROOT / "content" / "quotes.json"
OUT = ROOT / "www" / "img" / "quotes"
ICONS = ROOT / "www" / "assets" / "img"
PHOTOS = ROOT / "tools" / "photos"
FONT_ITALIC = ROOT / "www" / "assets" / "fonts" / "Nunito-Italic.ttf"
FONT_ROMAN = ROOT / "www" / "assets" / "fonts" / "Nunito.ttf"

SIZE = 1080

# Paleta de marca (tomada del manual visual de Kind)
BROWN = (125, 96, 74)
TEXT = (92, 70, 54)
GREEN = (142, 171, 28)
CREAM = (246, 244, 236)
SAND = (236, 225, 190)
MINT = (190, 230, 221)
AMBER = (255, 179, 0)

PALETTES = {
    "menta": ((190, 230, 221), (238, 249, 245)),
    "arena": ((236, 225, 190), (250, 244, 226)),
    "verde": ((198, 217, 138), (240, 246, 220)),
    "sol":   ((255, 224, 138), (255, 248, 226)),
}


def load_font(path, size, weight=700):
    f = ImageFont.truetype(str(path), size)
    try:
        f.set_variation_by_axes([weight])
    except Exception:
        pass
    return f


def vertical_gradient(size, top, bottom):
    img = Image.new("RGB", (1, size), top)
    d = ImageDraw.Draw(img)
    for y in range(size):
        t = y / max(1, size - 1)
        d.point((0, y), tuple(int(top[i] + (bottom[i] - top[i]) * t) for i in range(3)))
    return img.resize((size, size), Image.BICUBIC)


def sun(size, glow=True):
    """El icono de Kind: circulito amarillo con degradado radial."""
    s = size * 4
    core = (255, 214, 0)
    edge = (255, 245, 130)
    # el lienzo arranca en amarillo transparente (no en negro transparente):
    # asi el reescalado no mezcla negro y no aparece un borde gris.
    img = Image.new("RGBA", (s, s), edge + (0,))
    px = img.load()
    cx = cy = s / 2
    r = s / 2
    for y in range(s):
        for x in range(s):
            d = math.hypot(x - cx, y - cy) / r
            if d > 1:
                continue
            t = min(1.0, d ** 0.9)
            c = tuple(int(core[i] + (edge[i] - core[i]) * t) for i in range(3))
            a = 255 if d < 0.97 else int(255 * (1 - (d - 0.97) / 0.03))
            px[x, y] = c + (a,)
    img = img.resize((size, size), Image.LANCZOS)
    if not glow:
        return img
    # el halo se hace difuminando SOLO el canal alfa: si difuminamos el RGBA
    # completo, el negro transparente se mezcla y aparece un borde gris.
    halo_a = img.getchannel("A").filter(ImageFilter.GaussianBlur(size * 0.05)).point(lambda v: int(v * 0.55))
    out = Image.new("RGBA", (size, size), (255, 232, 120, 0))
    out.paste(Image.new("RGBA", (size, size), (255, 232, 120, 255)), (0, 0), halo_a)
    out.alpha_composite(img)
    return out


def wrap(draw, text, font, max_w):
    words, lines, cur = text.split(), [], ""
    for w in words:
        probe = f"{cur} {w}".strip()
        if draw.textlength(probe, font=font) <= max_w or not cur:
            cur = probe
        else:
            lines.append(cur)
            cur = w
    if cur:
        lines.append(cur)
    return lines


def round_corners(img, radius):
    mask = Image.new("L", img.size, 0)
    ImageDraw.Draw(mask).rounded_rectangle([0, 0, img.size[0] - 1, img.size[1] - 1], radius, fill=255)
    out = img.convert("RGBA")
    out.putalpha(mask)
    return out


def make_card(quote):
    pal = PALETTES.get(quote.get("palette"), PALETTES["menta"])
    photo = PHOTOS / f"{quote['id']}.jpg"

    if photo.exists():
        bg = Image.open(photo).convert("RGB")
        side = min(bg.size)
        bg = bg.crop(((bg.width - side) // 2, (bg.height - side) // 2,
                      (bg.width + side) // 2, (bg.height + side) // 2)).resize((SIZE, SIZE), Image.LANCZOS)
        veil = Image.new("RGBA", (SIZE, SIZE), (246, 244, 236, 120))
        bg = Image.alpha_composite(bg.convert("RGBA"), veil).convert("RGB")
    else:
        bg = vertical_gradient(SIZE, pal[0], pal[1])
        # manchas suaves de color para que no sea un degradado plano
        bg = bg.convert("RGBA")
        for box, color, alpha in (([-260, 520, 520, 1300], pal[0], 150),
                                  ([620, -220, 1340, 500], (255, 255, 255), 130)):
            mask = Image.new("L", (SIZE, SIZE), 0)
            ImageDraw.Draw(mask).ellipse(box, fill=alpha)
            mask = mask.filter(ImageFilter.GaussianBlur(90))
            bg.paste(Image.new("RGBA", (SIZE, SIZE), color + (255,)), (0, 0), mask)
        bg = bg.convert("RGB")

    card = bg.convert("RGBA")

    # panel de papel para que la frase siempre se lea
    panel = Image.new("RGBA", (SIZE, SIZE), (0, 0, 0, 0))
    ImageDraw.Draw(panel).rounded_rectangle([70, 190, SIZE - 70, SIZE - 150], 64, fill=(252, 251, 246, 232))
    card.alpha_composite(panel)

    card.alpha_composite(sun(150), (int(SIZE / 2 - 75), 78))

    d = ImageDraw.Draw(card)
    size = 74
    while size > 34:
        font = load_font(FONT_ITALIC, size, 700)
        lines = wrap(d, quote["text"], font, SIZE - 260)
        line_h = int(size * 1.42)
        if len(lines) * line_h <= 470:
            break
        size -= 4
    y = (190 + (SIZE - 150)) / 2 - (len(lines) * line_h) / 2 + 26
    for ln in lines:
        w = d.textlength(ln, font=font)
        d.text(((SIZE - w) / 2, y), ln, font=font, fill=TEXT)
        y += line_h

    sig = load_font(FONT_ITALIC, 40, 800)
    txt = "Att: Tiger"
    d.text((SIZE - 118 - d.textlength(txt, font=sig), SIZE - 250), txt, font=sig, fill=GREEN)

    brand = load_font(FONT_ROMAN, 42, 800)
    b = "Kind"
    d.text(((SIZE - d.textlength(b, font=brand)) / 2, SIZE - 118), b, font=brand, fill=GREEN)

    return round_corners(card, 96).convert("RGB")


def make_icons():
    ICONS.mkdir(parents=True, exist_ok=True)
    for px in (192, 512):
        base = Image.new("RGBA", (px, px), CREAM + (255,))
        s = sun(int(px * 0.62))
        base.alpha_composite(s, ((px - s.width) // 2, (px - s.height) // 2))
        base.convert("RGB").save(ICONS / f"icon-{px}.png")
    # maskable: mismo icono con más aire alrededor
    px = 512
    base = Image.new("RGBA", (px, px), CREAM + (255,))
    s = sun(int(px * 0.44))
    base.alpha_composite(s, ((px - s.width) // 2, (px - s.height) // 2))
    base.convert("RGB").save(ICONS / "icon-maskable-512.png")

    # iconos del launcher de Android (adaptive icon: solo el foreground)
    res = ROOT / "android" / "app" / "src" / "main" / "res"
    for folder, px in [("mipmap-mdpi", 108), ("mipmap-hdpi", 162), ("mipmap-xhdpi", 216),
                       ("mipmap-xxhdpi", 324), ("mipmap-xxxhdpi", 432)]:
        (res / folder).mkdir(parents=True, exist_ok=True)
        fg = Image.new("RGBA", (px, px), (0, 0, 0, 0))
        s = sun(int(px * 0.46))
        fg.alpha_composite(s, ((px - s.width) // 2, (px - s.height) // 2))
        fg.save(res / folder / "ic_launcher_foreground.png")


def make_widget_preview(first_card):
    """Imagen que Android muestra en el selector de widgets."""
    res = ROOT / "android" / "app" / "src" / "main" / "res" / "drawable-nodpi"
    res.mkdir(parents=True, exist_ok=True)
    first_card.resize((512, 512), Image.LANCZOS).save(res / "widget_preview.png", optimize=True)


def make_logo():
    """Logo de Kind: la palabra en verde con el sol como punto de la i."""
    size = 220
    font = load_font(FONT_ROMAN, size, 800)
    tmp = Image.new("RGBA", (10, 10))
    d0 = ImageDraw.Draw(tmp)
    w = int(d0.textlength("Kind", font=font))
    img = Image.new("RGBA", (w + 40, int(size * 1.5)), (0, 0, 0, 0))
    d = ImageDraw.Draw(img)
    d.text((20, size * 0.18), "Kind", font=font, fill=GREEN)

    # el sol se coloca justo encima de la i, tapando el punto original
    x_i = 20 + d.textlength("K", font=font)
    w_i = d.textlength("i", font=font)
    s = sun(int(size * 0.37))
    img.alpha_composite(s, (int(x_i + w_i / 2 - s.width / 2), int(size * 0.235)))

    img.crop(img.getbbox()).save(ICONS / "kind-logo.png")


def sync_content():
    """El JSON vive en content/ y se copia a www/ para que lo lean tanto la
    web como el WebView y el widget de Android (que leen desde assets/)."""
    dest = ROOT / "www" / "content"
    dest.mkdir(parents=True, exist_ok=True)
    (dest / "quotes.json").write_text(CONTENT.read_text(encoding="utf-8"), encoding="utf-8")


def main():
    data = json.loads(CONTENT.read_text(encoding="utf-8"))
    OUT.mkdir(parents=True, exist_ok=True)
    first = None
    for q in data["quotes"]:
        img = make_card(q)
        img.save(OUT / f"{q['id']}.png", optimize=True)
        first = first or img
        print("ok", q["id"], "-", q["text"][:44])
    if first is not None:
        make_widget_preview(first)
    make_icons()
    make_logo()
    sync_content()
    print(f"\n{len(data['quotes'])} tarjetas en {OUT}")


if __name__ == "__main__":
    main()
