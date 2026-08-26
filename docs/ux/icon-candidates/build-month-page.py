#!/usr/bin/env python3
"""Builds month-test.html: the decided calendar screen for brief 3.

The month grid keeps the cell size settled in brief 2 and never moves; the area under it
belongs to the day panel, which shows nothing until a day is picked. Tapping a day is the
whole point of the page: whether the grid stays put is a question about the transition.
"""
import calendar
import io
import json
import os

import iconlib as L

# Filler for the day panel: one plausible workout per type. Title None means the source
# gave none, and the panel falls back to the type name.
TEMPLATES = {
    "running": ("Running", "Warsaw Running", "42 min", "7.8 km"),
    "cycling": ("Cycling", "Warsaw Cycling", "1 h 15 min", "24.6 km"),
    "walking": ("Walking", "Warsaw Walking", "58 min", "4.2 km"),
    "badminton": ("Badminton", "Badminton", "1 h 30 min", None),
    "strength": ("Strength", "Strength Training", "35 min", None),
    "yoga": ("Yoga", "Yoga", "25 min", None),
    "other": ("Other", None, "40 min", None),
}
START_TIMES = ["07:20", "12:05", "18:40", "20:10", "21:15"]


def day_data():
    """Every day of the six-week window: panel heading and its list of activities."""
    year, month = L.MONTH_SHOWN
    data = {}
    for week in calendar.Calendar(firstweekday=0).monthdatescalendar(year, month):
        for date in week:
            acts = []
            for i, t in enumerate(L.ACTIVITIES.get((date.month, date.day), [])):
                label, title, duration, distance = TEMPLATES[t]
                meta = [START_TIMES[i % len(START_TIMES)], duration]
                if distance:
                    meta.append(distance)
                acts.append({"type": t, "title": title or label, "meta": " · ".join(meta)})
            data[f"{date.month}-{date.day}"] = {
                "head": date.strftime("%A, %-d %B"), "acts": acts}
    return data


def gear(px=24):
    teeth = "".join(
        f'<rect x="11" y="1.2" width="2" height="3.4" rx=".6" '
        f'transform="rotate({a} 12 12)"/>' for a in range(0, 360, 45))
    return (f'<svg viewBox="0 0 24 24" width="{px}" height="{px}" aria-hidden="true">'
            f'<g fill="none" stroke="currentColor" stroke-width="1.8">'
            f'<circle cx="12" cy="12" r="3.2"/><circle cx="12" cy="12" r="6.6"/></g>'
            f'<g fill="currentColor">{teeth}</g></svg>')


def chevron(px=24, back=True):
    d = "M14.5 5l-7 7 7 7" if back else "M9.5 5l7 7-7 7"
    return (f'<svg viewBox="0 0 24 24" width="{px}" height="{px}" aria-hidden="true">'
            f'<path d="{d}" fill="none" stroke="currentColor" stroke-width="2" '
            f'stroke-linecap="round" stroke-linejoin="round"/></svg>')


def top_bar():
    """App bar plus a month row of its own: chosen 2026-08-25 over a month-as-title bar."""
    return f"""<div class="bar">
  <div class="apptitle">Activity Calendar</div>
  <div class="acts"><button class="ibtn">{gear()}</button></div>
</div>
<div class="mrow">
  <button class="ibtn">{chevron(back=True)}</button>
  <button class="mbtn">August 2026</button>
  <button class="ibtn">{chevron(back=False)}</button>
  <button class="tbtn">Today</button>
</div>"""


def screen(icons, dark):
    return f"""<div class="screen{' dark' if dark else ''}">
  {top_bar()}
  {L.month_grid(icons, "stack", True, selected=None, extra_class="fluid")}
  <div class="panel"><div class="pbody"></div></div>
</div>"""


def page_css():
    lt, dk = L.scheme(), L.scheme(dark=True)
    css = """
html, body { height:100%; margin:0; padding:0; overflow:hidden; }
body { padding:0; background:VAR_BG; -webkit-tap-highlight-color:transparent; }
:root { --primary:LT_PRIMARY; }
.pager { display:flex; height:100dvh; overflow-x:auto; overflow-y:hidden;
  scroll-snap-type:x mandatory; }
.screen { flex:0 0 100vw; scroll-snap-align:start; height:100dvh; padding:0 6px;
  display:flex; flex-direction:column; position:relative; overflow:hidden;
  background:var(--bg); color:var(--fg); touch-action:manipulation;
  -webkit-user-select:none; user-select:none; }
.screen.dark { --bg:DK_BG; --fg:DK_FG; --dim:DK_DIM; --line:DK_LINE; --cellbg:DK_CELL;
  --sel:DK_SEL; --primary:DK_PRIMARY; DK_ACTIVITY }

.bar { height:56px; display:flex; align-items:center; gap:6px; padding:0 4px;
  flex:0 0 auto; }
.apptitle { font:500 20px/1 Roboto,system-ui,sans-serif; padding:6px 8px; }
.acts { margin-left:auto; display:flex; align-items:center; gap:2px; }
.ibtn { background:none; border:0; color:var(--fg); padding:6px; display:flex;
  align-items:center; }
.mrow { display:flex; align-items:center; gap:2px; padding:0 4px 4px; flex:0 0 auto; }
.mbtn { font:500 17px Roboto,system-ui,sans-serif; color:var(--fg); background:none;
  border:0; padding:6px 8px; border-radius:20px; }
.tbtn { margin-left:auto; font:500 14px Roboto,system-ui,sans-serif; color:var(--primary);
  background:none; border:0; padding:8px 10px; border-radius:20px; }

/* The grid keeps the cell size settled in brief 2 and never stretches: what is left
   under it belongs to the panel, and stays empty while no day is picked. */
.grid { flex:0 0 auto; }
.panel { flex:1 1 auto; min-height:0; overflow-y:auto; }
.phead { font:500 13px Roboto,system-ui,sans-serif; color:var(--dim);
  padding:14px 6px 4px; }
.prow { display:flex; align-items:center; gap:12px; padding:0 6px; height:52px; }
.pttl { font-size:14px; }
.pmeta { font-size:12px; color:var(--dim); margin-top:2px; }
.pnone { font-size:14px; color:var(--dim); padding:8px 6px; }

.fsbtn { position:fixed; right:8px; bottom:8px; z-index:5; width:36px; height:36px;
  border-radius:18px; border:1px solid var(--line); background:var(--bg); color:var(--fg);
  font-size:15px; opacity:.35; }
:fullscreen .fsbtn { display:none; }
"""
    return (css.replace("VAR_BG", lt["surface"]).replace("LT_PRIMARY", lt["primary"])
            .replace("DK_BG", dk["surface"]).replace("DK_FG", dk["fg"])
            .replace("DK_DIM", dk["dim"]).replace("DK_LINE", dk["line"])
            .replace("DK_CELL", dk["cell"]).replace("DK_SEL", dk["sel"])
            .replace("DK_PRIMARY", dk["primary"])
            .replace("DK_ACTIVITY", "".join(
                f"--c-{t}:{L.lighten(L.COLORS[t])};" for t in L.TYPES)))


def build():
    icons = L.load_sets()["monocolor_set"]
    screens = [screen(icons, False), screen(icons, True)]

    script = """
const DAYS = %s, ICONS = %s;
function render(panel, key) {
  const d = DAYS[key];
  if (!d) return;
  const rows = d.acts.length
    ? d.acts.map(a => '<div class="prow"><span class="ic colored" data-type="' + a.type
        + '" style="width:20px;height:20px">' + ICONS[a.type] + '</span><div><div class="pttl">'
        + a.title + '</div><div class="pmeta">' + a.meta + '</div></div></div>').join('')
    : '<div class="pnone">No activities</div>';
  panel.querySelector('.pbody').innerHTML = '<div class="phead">' + d.head + '</div>' + rows;
}
document.querySelectorAll('.screen').forEach(scr => {
  const panel = scr.querySelector('.panel');
  scr.querySelectorAll('.daycell').forEach(cell => {
    cell.addEventListener('click', () => {
      const wasOn = cell.classList.contains('selected');
      scr.querySelectorAll('.daycell.selected').forEach(c => c.classList.remove('selected'));
      panel.querySelector('.pbody').innerHTML = '';
      if (!wasOn) { cell.classList.add('selected'); render(panel, cell.dataset.key); }
    });
  });
});
// ?day=8-16 opens the page with that day already picked, for screenshots.
const wanted = new URLSearchParams(location.search).get('day');
if (wanted) {
  document.querySelectorAll('.screen').forEach(scr => {
    const cell = scr.querySelector('[data-key="' + wanted + '"]');
    if (cell) cell.click();
  });
}
document.querySelector('.fsbtn').addEventListener('click', () => {
  document.documentElement.requestFullscreen && document.documentElement.requestFullscreen();
});
""" % (json.dumps(day_data(), ensure_ascii=False), json.dumps(icons))

    html = f"""<!doctype html>
<html lang="ru"><head><meta charset="utf-8">
<meta name="viewport" content="width=device-width, initial-scale=1, viewport-fit=cover">
<title>Сетка месяца и панель</title>
<style>{L.base_css()}{page_css()}</style></head><body>
<div class="pager">{"".join(screens)}</div>
<button class="fsbtn">&#9974;</button>
<script>{script}</script>
</body></html>"""
    path = os.path.join(L.HERE, "month-test.html")
    io.open(path, "w", encoding="utf-8").write(html)
    print("wrote", path, len(html), "bytes")


if __name__ == "__main__":
    build()
