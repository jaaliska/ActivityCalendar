"""Shared pieces for the icon test pages: the chosen set, its calibration and rendering."""
import calendar
import io
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
             adjacent=False):
    classes = ("daycell" + (" today" if today else "") + (" selected" if selected else "")
               + (" adj" if adjacent else ""))
    return (f'<div class="{classes}"><span class="num">{day}</span>'
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
                adjacent=(date.month != month)))
        rows.append('<div class="week">' + "".join(cells) + "</div>")
    return (f'<div class="grid {extra_class}"><div class="week wdrow">{head}</div>'
            + "".join(rows) + "</div>")


def base_css():
    scale_css = "".join(f".ic[data-type={t}]{{transform:scale({SCALES[t]});}}" for t in TYPES)
    color_css = "".join(f".ic.colored[data-type={t}]{{color:var(--c-{t});}}" for t in TYPES)
    vars_css = "".join(f"--c-{t}:{COLORS[t]};" for t in TYPES)
    return f"""
:root {{ {vars_css} --bg:#fff; --fg:#1b1b1f; --dim:#6b6b75; --line:#e3e3e8; --cellbg:#f4f4f8;
  --sel:#cfd0dc; }}
* {{ box-sizing:border-box; }}
body {{ margin:0; padding:8px; background:var(--bg); color:var(--fg);
  font:14px/1.4 -apple-system,Roboto,system-ui,sans-serif; }}
h1 {{ font-size:18px; margin:16px 0 4px; }}
h2 {{ font-size:12px; font-weight:600; color:var(--dim); text-transform:uppercase;
  letter-spacing:.04em; margin:18px 0 8px; }}
section {{ padding:10px; border:1px solid var(--line); border-radius:12px; margin-bottom:10px; }}
section.dark, .gridwrap.dark {{ --fg:#e4e2e6; --dim:#9a9aa4; --line:#33343a; --cellbg:#1d1f25;
  --sel:#40444f; background:#111318; color:#e4e2e6; }}
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
.wdrow {{ margin-bottom:2px; }}
.wd {{ width:53px; text-align:center; font-size:10px; color:var(--dim); }}
.hint {{ font-size:12px; color:var(--dim); margin:4px 0 12px; }}
.warn {{ color:#b00; font-size:12px; }}
"""
