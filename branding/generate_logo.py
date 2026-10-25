"""
Generates the Lumen logo from one set of geometry:

  branding/lumen-logo.svg          full logo: tile + camera mark + wordmark
  branding/lumen-mark.svg          camera mark on its own (transparent)
  app/src/main/res/drawable/ic_launcher_foreground.xml   adaptive icon foreground
  app/src/main/res/drawable/ic_launcher_background.xml   adaptive icon background
  app/src/main/res/drawable/ic_launcher_monochrome.xml   themed-icon layer (Android 13+)

Run from the project root:  python branding/generate_logo.py
"""
import math
import os

from fontTools.pens.svgPathPen import SVGPathPen
from fontTools.pens.transformPen import TransformPen
from fontTools.ttLib import TTFont

ROOT = os.path.dirname(os.path.dirname(os.path.abspath(__file__)))
RES = os.path.join(ROOT, 'app', 'src', 'main', 'res', 'drawable')
BRAND = os.path.join(ROOT, 'branding')

# Palette: warm ink tile, champagne metal, one coral accent (matches the app's Coral theme).
INK_TOP = '#2B2723'
INK_BOTTOM = '#100F0D'
SEPARATOR = '#171513'
METAL = [(0.0, '#FFF7EA'), (0.38, '#E9DAC0'), (0.72, '#B49C76'), (1.0, '#EFE2CB')]
BLADE = [(0.0, '#FFF8EC'), (0.45, '#D9C6A4'), (0.8, '#8F7A57'), (1.0, '#E2D0B1')]
CORAL = '#FF7246'


def f(v):
    return f'{v:.2f}'.rstrip('0').rstrip('.')


# Mark geometry, in a 120 x 100 design space.

CAMERA = (
    'M96 88H20A10 10 0 0 1 10 78V40A10 10 0 0 1 20 30H36L42 20'
    'A4 4 0 0 1 45.6 17.8H74.4A4 4 0 0 1 78 20L84 30H100A10 10 0 0 1 110 40V72'
)
INNER_LINE = 'M16.5 44V76A6.5 6.5 0 0 0 23 82.5H62'
HOT_SHOE = 'M19 23.5H29A1.5 1.5 0 0 1 30.5 25V30H17.5V25A1.5 1.5 0 0 1 19 23.5Z'
CX, CY = 60.0, 59.0
RING_R = 22.0
OUTER_RING_R = 25.2
BLADE_R = 18.6
HOLE_R = 4.6
BLADES = 6
TWIST = 72.0  # how far each blade sweeps around the lens


def pt(radius, degrees):
    a = math.radians(degrees)
    return CX + radius * math.cos(a), CY + radius * math.sin(a)


def blade_geometry():
    """Each blade is bounded by two swept curves, a hex-hole edge and an arc of the lens."""
    base = -90.0
    hole = [pt(HOLE_R, base + i * 360 / BLADES) for i in range(BLADES)]
    outer = [pt(BLADE_R, base + i * 360 / BLADES + TWIST) for i in range(BLADES)]
    ctrl = [pt((HOLE_R + BLADE_R) * 0.62, base + i * 360 / BLADES + TWIST * 0.18) for i in range(BLADES)]
    blades, edges = [], []
    for i in range(BLADES):
        j = (i + 1) % BLADES
        vi, vj = hole[i], hole[j]
        oi, oj = outer[i], outer[j]
        qi, qj = ctrl[i], ctrl[j]
        blades.append(
            f'M{f(vi[0])} {f(vi[1])}L{f(vj[0])} {f(vj[1])}'
            f'Q{f(qj[0])} {f(qj[1])} {f(oj[0])} {f(oj[1])}'
            f'A{f(BLADE_R)} {f(BLADE_R)} 0 0 0 {f(oi[0])} {f(oi[1])}'
            f'Q{f(qi[0])} {f(qi[1])} {f(vi[0])} {f(vi[1])}Z'
        )
        edges.append(f'M{f(vi[0])} {f(vi[1])}Q{f(qi[0])} {f(qi[1])} {f(oi[0])} {f(oi[1])}')
    hole_path = 'M' + 'L'.join(f'{f(x)} {f(y)}' for x, y in hole) + 'Z'
    return blades, edges, hole_path


def circle(r, cx=CX, cy=CY):
    return f'M{f(cx - r)} {f(cy)}A{f(r)} {f(r)} 0 1 0 {f(cx + r)} {f(cy)}A{f(r)} {f(r)} 0 1 0 {f(cx - r)} {f(cy)}Z'


INDICATOR = (98.0, 41.0, 3.1)
BLADE_PATHS, BLADE_EDGES, HOLE = blade_geometry()

# Mark bounds (for centering): x 10..110, y 17.8..88
MARK_W, MARK_H = 100.0, 70.2
MARK_CX, MARK_CY = 60.0, 52.9


# Wordmark: Instrument Serif capitals, widely tracked, converted to outlines.

def wordmark(text, cap_height, tracking):
    font = TTFont(os.path.join(ROOT, 'app', 'src', 'main', 'res', 'font', 'instrument_serif.ttf'))
    glyphs = font.getGlyphSet()
    cmap = font.getBestCmap()
    cap = font['OS/2'].sCapHeight or 700
    scale = cap_height / cap
    x = 0.0
    parts = []
    for ch in text:
        name = cmap[ord(ch)]
        pen = SVGPathPen(glyphs)
        glyphs[name].draw(TransformPen(pen, (scale, 0, 0, -scale, x, cap_height)))
        parts.append(pen.getCommands())
        x += glyphs[name].width * scale + tracking
    return ''.join(parts), x - tracking


# SVG output

def svg_stops(stops):
    return ''.join(f'<stop offset="{o}" stop-color="{c}"/>' for o, c in stops)


def mark_svg_body(prefix):
    strokes = f'stroke="url(#{prefix}metal)" fill="none" stroke-linecap="round" stroke-linejoin="round"'
    blades = ''.join(
        f'<path d="{d}" fill="url(#{prefix}blade)" stroke="{SEPARATOR}" stroke-width="0.9" stroke-linejoin="round"/>'
        for d in BLADE_PATHS
    )
    return (
        f'<path d="{CAMERA}" {strokes} stroke-width="2.6"/>'
        f'<path d="{INNER_LINE}" {strokes} stroke-width="1" opacity="0.55"/>'
        f'<path d="{HOT_SHOE}" {strokes} stroke-width="1.8"/>'
        f'<circle cx="{INDICATOR[0]}" cy="{INDICATOR[1]}" r="{INDICATOR[2]}" fill="{CORAL}"/>'
        f'<circle cx="{CX}" cy="{CY}" r="{OUTER_RING_R}" {strokes} stroke-width="0.8" opacity="0.6"/>'
        f'<circle cx="{CX}" cy="{CY}" r="{RING_R}" {strokes} stroke-width="2.2"/>'
        f'<circle cx="{CX}" cy="{CY}" r="{BLADE_R + 0.6}" fill="{SEPARATOR}"/>'
        f'{blades}'
        f'<path d="{HOLE}" fill="{SEPARATOR}"/>'
    )


def mark_svg_defs(prefix):
    return (
        f'<linearGradient id="{prefix}metal" gradientUnits="userSpaceOnUse" x1="18" y1="14" x2="104" y2="92">{svg_stops(METAL)}</linearGradient>'
        f'<linearGradient id="{prefix}blade" gradientUnits="userSpaceOnUse" x1="44" y1="40" x2="78" y2="80">{svg_stops(BLADE)}</linearGradient>'
    )


def write_svgs():
    os.makedirs(BRAND, exist_ok=True)

    # Mark only, transparent, 120 x 100.
    mark = (
        '<svg xmlns="http://www.w3.org/2000/svg" viewBox="4 10 112 84" width="560" height="420">'
        f'<defs>{mark_svg_defs("m")}</defs>{mark_svg_body("m")}</svg>\n'
    )
    open(os.path.join(BRAND, 'lumen-mark.svg'), 'w', encoding='utf-8').write(mark)

    # Full logo on a 1024 canvas.
    size = 1024
    tile, radius = 800, 184
    t0 = (size - tile) / 2
    mark_scale = 4.6
    mark_tx = size / 2 - MARK_CX * mark_scale
    mark_ty = 398 - MARK_CY * mark_scale
    word, word_w = wordmark('LUMEN', cap_height=64, tracking=38)
    word_tx = size / 2 - word_w / 2
    word_ty = 700
    logo = f'''<svg xmlns="http://www.w3.org/2000/svg" viewBox="0 0 {size} {size}" width="{size}" height="{size}">
  <defs>
    <linearGradient id="tile" x1="0" y1="0" x2="0" y2="1">
      <stop offset="0" stop-color="{INK_TOP}"/><stop offset="1" stop-color="{INK_BOTTOM}"/>
    </linearGradient>
    <linearGradient id="rim" x1="0" y1="0" x2="0" y2="1">
      <stop offset="0" stop-color="#FFFFFF" stop-opacity="0.14"/><stop offset="0.5" stop-color="#FFFFFF" stop-opacity="0.02"/>
      <stop offset="1" stop-color="#FFFFFF" stop-opacity="0.06"/>
    </linearGradient>
    <linearGradient id="wordmark" gradientUnits="userSpaceOnUse" x1="0" y1="0" x2="{f(word_w)}" y2="60">{svg_stops(METAL)}</linearGradient>
    <radialGradient id="sheen" cx="0.3" cy="0.12" r="0.85">
      <stop offset="0" stop-color="#FFF3E0" stop-opacity="0.10"/><stop offset="1" stop-color="#FFF3E0" stop-opacity="0"/>
    </radialGradient>
    <filter id="shadow" x="-20%" y="-20%" width="140%" height="150%">
      <feDropShadow dx="0" dy="24" stdDeviation="30" flood-color="#000" flood-opacity="0.45"/>
    </filter>
    {mark_svg_defs("l")}
  </defs>
  <rect x="{t0}" y="{t0}" width="{tile}" height="{tile}" rx="{radius}" fill="url(#tile)" filter="url(#shadow)"/>
  <rect x="{t0}" y="{t0}" width="{tile}" height="{tile}" rx="{radius}" fill="url(#sheen)"/>
  <rect x="{t0 + 1.5}" y="{t0 + 1.5}" width="{tile - 3}" height="{tile - 3}" rx="{radius - 1.5}" fill="none" stroke="url(#rim)" stroke-width="3"/>
  <g transform="translate({f(mark_tx)} {f(mark_ty)}) scale({mark_scale})">{mark_svg_body("l")}</g>
  <path transform="translate({f(word_tx)} {word_ty})" d="{word}" fill="url(#wordmark)"/>
</svg>
'''
    open(os.path.join(BRAND, 'lumen-logo.svg'), 'w', encoding='utf-8').write(logo)


# Android vector drawables

def vd_gradient(attr, stops, x1, y1, x2, y2):
    items = ''.join(f'\n                    <item android:color="{c}" android:offset="{o}" />' for o, c in stops)
    return (
        f'\n            <aapt:attr name="android:{attr}">\n'
        f'                <gradient android:type="linear" android:startX="{x1}" android:startY="{y1}" '
        f'android:endX="{x2}" android:endY="{y2}">{items}\n                </gradient>\n            </aapt:attr>'
    )


def vd_path(d, fill=None, stroke=None, width=None, fill_gradient=None, stroke_gradient=None, alpha=None, cap=True):
    attrs = [f'android:pathData="{d}"']
    # A gradient replaces the solid colour; declaring both makes aapt fail.
    if fill and not fill_gradient:
        attrs.append(f'android:fillColor="{fill}"')
    if stroke and not stroke_gradient:
        attrs.append(f'android:strokeColor="{stroke}"')
    if width:
        attrs.append(f'android:strokeWidth="{width}"')
    if (stroke or stroke_gradient) and cap:
        attrs += ['android:strokeLineCap="round"', 'android:strokeLineJoin="round"']
    if alpha is not None:
        attrs.append(f'android:strokeAlpha="{alpha}"')
    body = ''
    if fill_gradient:
        body += vd_gradient('fillColor', *fill_gradient)
    if stroke_gradient:
        body += vd_gradient('strokeColor', *stroke_gradient)
    attrs_text = '\n            '.join(attrs)
    if body:
        return f'        <path\n            {attrs_text}>{body}\n        </path>\n'
    return f'        <path\n            {attrs_text} />\n'


def vector(paths, scale, tx, ty, viewport=108, size='108dp', mono=False):
    ns = 'xmlns:android="http://schemas.android.com/apk/res/android"'
    if not mono:
        ns += '\n    xmlns:aapt="http://schemas.android.com/aapt"'
    return (
        f'<?xml version="1.0" encoding="utf-8"?>\n'
        f'<!-- Generated by branding/generate_logo.py. Edit that script, not this file. -->\n'
        f'<vector {ns}\n    android:width="{size}"\n    android:height="{size}"\n'
        f'    android:viewportWidth="{viewport}"\n    android:viewportHeight="{viewport}">\n'
        f'    <group android:scaleX="{f(scale)}" android:scaleY="{f(scale)}" '
        f'android:translateX="{f(tx)}" android:translateY="{f(ty)}">\n'
        f'{paths}    </group>\n</vector>\n'
    )


def color_mark_paths():
    metal = (METAL, 18, 14, 104, 92)
    blade = (BLADE, 44, 40, 78, 80)
    out = vd_path(CAMERA, stroke_gradient=metal, stroke='#E9DAC0', width=2.6)
    out += vd_path(INNER_LINE, stroke_gradient=metal, stroke='#E9DAC0', width=1, alpha=0.55)
    out += vd_path(HOT_SHOE, stroke_gradient=metal, stroke='#E9DAC0', width=1.8)
    out += vd_path(circle(INDICATOR[2], INDICATOR[0], INDICATOR[1]), fill=CORAL)
    out += vd_path(circle(OUTER_RING_R), stroke_gradient=metal, stroke='#E9DAC0', width=0.8, alpha=0.6)
    out += vd_path(circle(RING_R), stroke_gradient=metal, stroke='#E9DAC0', width=2.2)
    out += vd_path(circle(BLADE_R + 0.6), fill=SEPARATOR)
    for d in BLADE_PATHS:
        out += vd_path(d, fill_gradient=blade, fill='#D9C6A4', stroke=SEPARATOR, width=0.9, cap=False)
    out += vd_path(HOLE, fill=SEPARATOR)
    return out


def mono_mark_paths():
    # Themed icons are a single tint, so the aperture is drawn as line art.
    out = vd_path(CAMERA, stroke='#FFFFFF', width=2.8)
    out += vd_path(HOT_SHOE, stroke='#FFFFFF', width=2)
    out += vd_path(circle(INDICATOR[2], INDICATOR[0], INDICATOR[1]), fill='#FFFFFF')
    out += vd_path(circle(RING_R), stroke='#FFFFFF', width=2.4)
    for d in BLADE_EDGES:
        out += vd_path(d, stroke='#FFFFFF', width=1.8)
    out += vd_path(HOLE, stroke='#FFFFFF', width=1.6)
    return out


def write_android():
    # Fit the mark inside the adaptive icon's 66dp safe circle (centred at 54,54).
    scale = 0.5
    tx = 54 - MARK_CX * scale
    ty = 54 - MARK_CY * scale
    open(os.path.join(RES, 'ic_launcher_foreground.xml'), 'w', encoding='utf-8').write(
        vector(color_mark_paths(), scale, tx, ty))
    open(os.path.join(RES, 'ic_launcher_monochrome.xml'), 'w', encoding='utf-8').write(
        vector(mono_mark_paths(), scale, tx, ty, mono=True))

    background = f'''<?xml version="1.0" encoding="utf-8"?>
<!-- Generated by branding/generate_logo.py. Edit that script, not this file. -->
<vector xmlns:android="http://schemas.android.com/apk/res/android"
    xmlns:aapt="http://schemas.android.com/aapt"
    android:width="108dp"
    android:height="108dp"
    android:viewportWidth="108"
    android:viewportHeight="108">
    <path android:pathData="M0,0h108v108h-108z">
        <aapt:attr name="android:fillColor">
            <gradient android:type="linear" android:startX="54" android:startY="0" android:endX="54" android:endY="108">
                <item android:color="{INK_TOP}" android:offset="0" />
                <item android:color="{INK_BOTTOM}" android:offset="1" />
            </gradient>
        </aapt:attr>
    </path>
</vector>
'''
    open(os.path.join(RES, 'ic_launcher_background.xml'), 'w', encoding='utf-8').write(background)



if __name__ == '__main__':
    write_svgs()
    write_android()
    print('Logo written to branding/ and app/src/main/res/drawable/')
