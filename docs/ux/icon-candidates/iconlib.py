"""Shared pieces for the icon test pages: the chosen set, its calibration and rendering."""
import calendar
import io
import math
import os
import re

HERE = os.path.dirname(os.path.abspath(__file__))
TYPES = ["badminton", "cycling", "walking", "running", "strength", "yoga", "other"]
LABELS = {
    "badminton": "Badminton", "cycling": "Cycling", "walking": "Walking",
    "running": "Running", "strength": "Strength", "yoga": "Yoga", "other": "Other",
}
# Optical size correction, measured on a real phone at 16-20dp
SCALES = {
    "badminton": 0.90, "cycling": 1.00, "walking": 1.00, "running": 1.00,
    "strength": 1.00, "yoga": 0.84, "other": 1.00,
}
# Chosen 2026-08-25 on the phone
COLORS = {
    "badminton": "#C2185B", "cycling": "#0288D1", "walking": "#00897B",
    "running": "#EF6C00", "strength": "#6D4C41", "yoga": "#3949AB", "other": "#546E7A",
}
# The dark theme lifts the activity palette towards white; no separate palette exists.
DARK_LIGHTEN = 0.42

# (month, day) -> activity types, in start-time order. July and September are the
# neighbouring days the August grid shows: they carry their own activities.
ACTIVITIES = {
    (7, 28): ["running"], (7, 30): ["badminton"], (7, 31): ["cycling"],
    (8, 3): ["running"], (8, 5): ["strength", "yoga"], (8, 8): ["cycling"],
    (8, 11): ["walking"], (8, 12): ["running"], (8, 14): ["badminton"],
    (8, 16): ["running", "strength", "yoga"], (8, 19): ["walking"], (8, 21): ["yoga"],
    (8, 24): ["cycling"], (8, 26): ["other"], (8, 29): ["running"],
    (8, 30): ["walking", "yoga", "running", "cycling", "strength"],
    (9, 2): ["walking"], (9, 5): ["yoga", "strength"],
}
MONTH_SHOWN = (2026, 8)
TODAY = 23
MAX_ICONS = 2
WEEKDAYS = ["Mon", "Tue", "Wed", "Thu", "Fri", "Sat", "Sun"]

# Material 3 theme in use. Other candidates live in docs/ux/theme.md; switching themes
# is a matter of changing this pair.
SEED_HUE, SEED_CHROMA = 257, 60


def lighten(hex_color, amount=DARK_LIGHTEN):
    """Mixes a colour towards white. How an activity colour moves into the dark theme."""
    parts = (int(hex_color[i:i + 2], 16) for i in (1, 3, 5))
    return "#%02X%02X%02X" % tuple(round(v + (255 - v) * amount) for v in parts)


def _lch_to_hex(tone_value, chroma, hue):
    """One tone of a tonal palette: keeps the hue, drops chroma until sRGB can hold it."""
    h = math.radians(hue)
    for c in range(int(chroma), -1, -1):
        fy = (tone_value + 16) / 116
        fx = fy + math.cos(h) * c / 500
        fz = fy - math.sin(h) * c / 200

        def f(t):
            return t ** 3 if t ** 3 > 0.008856 else (t - 16 / 116) / 7.787

        x, y, z = f(fx) * 0.95047, f(fy), f(fz) * 1.08883
        lin = (x * 3.2406 + y * -1.5372 + z * -0.4986,
               x * -0.9689 + y * 1.8758 + z * 0.0415,
               x * 0.0557 + y * -0.2040 + z * 1.0570)
        if all(-0.001 <= v <= 1.001 for v in lin):
            def enc(v):
                v = max(0.0, min(1.0, v))
                v = v * 12.92 if v <= 0.0031308 else 1.055 * v ** (1 / 2.4) - 0.055
                return max(0, min(255, round(v * 255)))
            return "#%02X%02X%02X" % tuple(enc(v) for v in lin)
    return "#000000"


def scheme(dark=False):
    """The Material 3 roles this preview needs, grown from the seed above."""
    hue, chroma = SEED_HUE, SEED_CHROMA
    p = lambda t: _lch_to_hex(t, chroma, hue)
    sec = lambda t: _lch_to_hex(t, max(8, chroma // 3), hue)
    n = lambda t: _lch_to_hex(t, 4, hue)
    nv = lambda t: _lch_to_hex(t, 8, hue)
    if dark:
        return {"surface": n(6), "cell": n(17), "sel": sec(30), "fg": n(90),
                "dim": nv(80), "line": nv(60), "primary": p(80)}
    return {"surface": n(98), "cell": n(92), "sel": sec(90), "fg": n(10),
            "dim": nv(30), "line": nv(50), "primary": p(40)}


def normalize(svg_text):
    """Strips fixed sizes and hardcoded black so the icon inherits size and currentColor."""
    svg_text = re.sub(r"<\?xml.*?\?>", "", svg_text, flags=re.S)
    svg_text = re.sub(r"<!DOCTYPE.*?>", "", svg_text, flags=re.S)
    svg_text = re.sub(r"<!--.*?-->", "", svg_text, flags=re.S)
    svg_text = re.sub(r'\s(width|height)="[^"]*"', "", svg_text, count=2)
    svg_text = re.sub(r"#000000|#000\b|black", "currentColor", svg_text, flags=re.I)
    svg_text = svg_text.replace("<svg", '<svg preserveAspectRatio="xMidYMid meet"', 1)
    return svg_text.strip()


def load_sets():
    """Returns {folder name: {type: inline svg}} for every candidate folder."""
    sets = {}
    for name in sorted(os.listdir(HERE)):
        folder = os.path.join(HERE, name)
        if not os.path.isdir(folder):
            continue
        icons = {}
        for t in TYPES:
            path = os.path.join(folder, t + ".svg")
            if os.path.exists(path):
                icons[t] = normalize(io.open(path, encoding="utf-8", errors="replace").read())
        if icons:
            sets[name] = icons
    return sets


def icon(icons, t, px, colored=False):
    if t not in icons:
        return f'<span class="missing" style="width:{px}px;height:{px}px">?</span>'
    cls = "ic colored" if colored else "ic"
    return (f'<span class="{cls}" data-type="{t}" style="width:{px}px;height:{px}px">'
            f"{icons[t]}</span>")


def cell_body(icons, acts, variant, colored):
    """Renders what sits at the bottom of a day cell for the given multiplicity variant."""
    if not acts:
        return ""
    if variant == "stack":
        shown = acts[:MAX_ICONS]
        px = 18 if len(shown) == 1 else 16
        rest = len(acts) - len(shown)
        more = f'<span class="more">+{rest}</span>' if rest else ""
        return "".join(icon(icons, t, px, colored) for t in shown) + more
    if variant == "count":
        badge = f'<span class="badge">{len(acts)}</span>' if len(acts) > 1 else ""
        return icon(icons, acts[0], 18, colored) + badge
    if variant == "dots":
        dots = ""
        if len(acts) > 1:
            dots = '<span class="dots">' + "".join(
                '<i></i>' for _ in acts[:4]) + "</span>"
        return icon(icons, acts[0], 16, colored) + dots
    raise ValueError(variant)


def day_cell(icons, day, acts, variant, colored, today=False, selected=False,
             adjacent=False, key=None):
    """key is a (month, day) pair; it lands in data-key so a page can address the cell."""
    classes = ("daycell" + (" today" if today else "") + (" selected" if selected else "")
               + (" adj" if adjacent else ""))
    attr = f' data-key="{key[0]}-{key[1]}"' if key else ""
    return (f'<div class="{classes}"{attr}><span class="num">{day}</span>'
            f'<div class="icons">{cell_body(icons, acts, variant, colored)}</div></div>')


def month_grid(icons, variant="stack", colored=True, selected=None, extra_class=""):
    """Renders the six-week window: neighbouring days keep their own activities, muted.

    selected is a (month, day) pair or None.
    """
    year, month = MONTH_SHOWN
    weeks = calendar.Calendar(firstweekday=0).monthdatescalendar(year, month)
    head = "".join(f'<div class="wd">{d}</div>' for d in WEEKDAYS)
    rows = []
    for week in weeks:
        cells = []
        for date in week:
            key = (date.month, date.day)
            cells.append(day_cell(
                icons, date.day, ACTIVITIES.get(key, []), variant, colored,
                today=(key == (month, TODAY)),
                selected=(key == selected),
                adjacent=(date.month != month), key=key))
        rows.append('<div class="week">' + "".join(cells) + "</div>")
    return (f'<div class="grid {extra_class}"><div class="week wdrow">{head}</div>'
            + "".join(rows) + "</div>")


def base_css():
    scale_css = "".join(f".ic[data-type={t}]{{transform:scale({SCALES[t]});}}" for t in TYPES)
    color_css = "".join(f".ic.colored[data-type={t}]{{color:var(--c-{t});}}" for t in TYPES)
    vars_css = "".join(f"--c-{t}:{COLORS[t]};" for t in TYPES)
    lt, dk = scheme(), scheme(dark=True)
    return f"""
:root {{ {vars_css} --bg:{lt["surface"]}; --fg:{lt["fg"]}; --dim:{lt["dim"]};
  --line:{lt["line"]}; --cellbg:{lt["cell"]}; --sel:{lt["sel"]}; }}
* {{ box-sizing:border-box; }}
body {{ margin:0; padding:8px; background:var(--bg); color:var(--fg);
  font:14px/1.4 -apple-system,Roboto,system-ui,sans-serif; }}
h1 {{ font-size:18px; margin:16px 0 4px; }}
h2 {{ font-size:12px; font-weight:600; color:var(--dim); text-transform:uppercase;
  letter-spacing:.04em; margin:18px 0 8px; }}
section {{ padding:10px; border:1px solid var(--line); border-radius:12px; margin-bottom:10px; }}
section.dark, .gridwrap.dark {{ --fg:{dk["fg"]}; --dim:{dk["dim"]}; --line:{dk["line"]};
  --cellbg:{dk["cell"]}; --sel:{dk["sel"]}; background:{dk["surface"]}; color:{dk["fg"]}; }}
.ic {{ display:inline-block; vertical-align:middle; color:var(--fg); }}
.ic svg {{ width:100%; height:100%; display:block; }}
{scale_css}
{color_css}
.missing {{ display:inline-block; background:#fdd; color:#900; text-align:center; }}
.strip, .week {{ display:flex; gap:1px; }}
.daycell {{ width:53px; height:56px; background:var(--cellbg); border-radius:8px;
  padding:3px; position:relative; }}
.daycell.today {{ outline:2px solid var(--fg); }}
.daycell.selected {{ background:var(--sel); }}
.daycell.adj {{ opacity:.45; }}
.num {{ font-size:11px; color:var(--dim); }}
.icons {{ position:absolute; inset:auto 0 4px 0; display:flex; justify-content:center;
  align-items:center; gap:1px; }}
.more {{ font-size:9px; color:var(--dim); margin-left:1px; }}
.badge {{ font-size:10px; font-weight:600; color:var(--dim); margin-left:2px; }}
.dots {{ display:flex; gap:2px; margin-left:3px; }}
.dots i {{ width:3px; height:3px; border-radius:50%; background:currentColor;
  color:var(--dim); display:block; }}
.grid {{ display:flex; flex-direction:column; gap:1px; }}
.grid.fluid .daycell, .grid.fluid .wd {{ width:auto; flex:1 1 0; min-width:0; }}
.wdrow {{ margin-bottom:2px; }}
.wd {{ width:53px; text-align:center; font-size:10px; color:var(--dim); }}
.hint {{ font-size:12px; color:var(--dim); margin:4px 0 12px; }}
.warn {{ color:#b00; font-size:12px; }}
"""
