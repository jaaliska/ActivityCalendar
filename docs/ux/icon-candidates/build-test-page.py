#!/usr/bin/env python3
"""Builds icon-test.html: legibility of the icon set at 16-20dp and the colour per type.

Every subfolder next to this script is one candidate set and must contain
badminton.svg, cycling.svg, walking.svg, running.svg, strength.svg, yoga.svg, other.svg.
"""
import io
import os

import iconlib as L

SWATCHES = [
    "#E53935", "#EF6C00", "#F9A825", "#7CB342", "#2E7D32", "#00897B",
    "#0288D1", "#3949AB", "#7B4DFF", "#C2185B", "#6D4C41", "#546E7A",
]
PRESETS = {"Выбранная": dict(L.COLORS)}
# Neighbours in the list are from different families: the test is about the glyph alone.
BLIND_ORDER = [
    "yoga", "basketball", "running", "swimming", "badminton", "boxing", "walking",
    "table_tennis", "strength", "skiing", "dancing", "soccer", "hiking",
    "cycling", "martial_arts", "stretching", "tennis", "other", "snowboarding",
    "volleyball",
]


PER_ROW = 7


def size_rows(icons):
    """The set at every size that matters, seven types at a time so a phone shows them whole."""
    out = []
    for start in range(0, len(L.TYPES), PER_ROW):
        chunk = L.TYPES[start:start + PER_ROW]
        out.append('<div class="row heads">'
                   + "".join(f'<div class="cap">{L.LABELS[t]}</div>' for t in chunk) + "</div>")
        for px in (40, 24, 20, 16):
            cells = "".join(f'<div class="cell">{L.icon(icons, t, px)}</div>' for t in chunk)
            out.append(f'<div class="row"><div class="sz">{px}</div>{cells}</div>')
        out.append('<div class="gap"></div>')
    return "\n".join(out)


def day_strip(icons):
    cells = "".join(
        f'<div class="daycell"><span class="num">{n}</span>'
        f'<div class="icons">{L.icon(icons, t, 18)}</div></div>'
        for n, t in enumerate(L.TYPES, start=3))
    return f'<div class="strip">{cells}</div>'


def blind(icons):
    cells = "".join(
        f'<div class="blindcell"><div class="bnum">{i}</div>{L.icon(icons, t, 16)}</div>'
        for i, t in enumerate(BLIND_ORDER, start=1))
    answers = ", ".join(f"{i} — {L.LABELS[t]}" for i, t in enumerate(BLIND_ORDER, start=1))
    return (f'<div class="blindrow">{cells}</div>'
            f"<details><summary>Показать ответы</summary><p>{answers}</p></details>")


def color_tool(icons):
    presets = "".join(f'<button type="button" class="preset" data-p="{n}">{n}</button>'
                      for n in PRESETS)
    pickers = []
    for t in L.TYPES:
        sws = "".join(f'<button type="button" class="sw" data-t="{t}" data-c="{c}" '
                      f'style="background:{c}"></button>' for c in SWATCHES)
        pickers.append(f'<div class="pick"><div class="pname">{L.LABELS[t]}'
                       f'<span class="dot" id="dot-{t}"></span></div>'
                       f'<div class="sws">{sws}</div></div>')
    return (f'<div class="presets">{presets}</div>'
            f'<div class="gridwrap light" id="gwl">{L.month_grid(icons)}</div>'
            f'<div class="gridwrap dark" id="gwd">{L.month_grid(icons)}</div>'
            f'<div class="pickers">{"".join(pickers)}</div>'
            f'<textarea id="colorout" rows="3" readonly></textarea>'
            f'<button id="colorcopy" type="button">Скопировать цвета</button>')


def section(title, body, dark=False):
    return f'<section class="{"dark" if dark else ""}"><h2>{title}</h2>{body}</section>'


def build():
    sets = L.load_sets()
    presets_js = ",".join(
        "'%s':{%s}" % (n, ",".join(f"'{t}':'{c}'" for t, c in cols.items()))
        for n, cols in PRESETS.items())

    blocks = []
    for name, icons in sets.items():
        missing = [t for t in L.TYPES if t not in icons]
        warn = f'<p class="warn">Нет файлов: {", ".join(missing)}</p>' if missing else ""
        blocks.append(f"""
<article class="set">
  <h1>{name}</h1>{warn}
  {section("1. Слепая проверка на 16dp", blind(icons))}
  {section("2. Цвет на семейство", color_tool(icons))}
  {section("3. Размеры, монохром", size_rows(icons))}
  {section("4. Ячейка календаря, реальный размер", day_strip(icons))}
  {section("5. Август 2026, монохром", L.month_grid(icons, colored=False))}
  {section("6. Август 2026, монохром, тёмная тема",
           L.month_grid(icons, colored=False), dark=True)}
</article>""")

    html = f"""<!doctype html>
<html lang="ru"><head><meta charset="utf-8">
<meta name="viewport" content="width=device-width, initial-scale=1">
<title>Проверка иконок 16-20dp</title>
<style>{L.base_css()}
.row {{ display:flex; align-items:center; gap:4px; margin-bottom:6px; }}
.gap {{ height:14px; }}
.strip {{ flex-wrap:wrap; row-gap:4px; }}
.row .sz {{ width:20px; font-size:10px; color:var(--dim); text-align:right; }}
.row.heads {{ padding-left:24px; }}
.cap {{ flex:1; font-size:9px; color:var(--dim); text-align:center; }}
.cell {{ flex:1; height:44px; display:flex; align-items:center; justify-content:center; }}
.blindrow {{ display:flex; gap:6px; flex-wrap:wrap; margin-bottom:8px; }}
.blindcell {{ width:44px; height:52px; border:1px solid var(--line); border-radius:8px;
  display:flex; flex-direction:column; align-items:center; justify-content:center; gap:4px; }}
.bnum {{ font-size:10px; color:var(--dim); }}
details {{ font-size:12px; color:var(--dim); }}
.presets {{ display:flex; gap:6px; margin-bottom:8px; flex-wrap:wrap; }}
.preset {{ font-size:12px; padding:5px 10px; border:1px solid var(--line); border-radius:16px;
  background:transparent; color:var(--fg); }}
.gridwrap {{ padding:6px; border-radius:10px; margin-bottom:6px; }}
.pickers {{ margin-top:10px; }}
.pick {{ margin-bottom:6px; }}
.pname {{ font-size:11px; color:var(--dim); display:flex; align-items:center; gap:6px; }}
.pname .dot {{ width:10px; height:10px; border-radius:50%; display:inline-block; }}
.sws {{ display:flex; gap:4px; margin-top:3px; }}
.sw {{ flex:1; height:26px; border:2px solid transparent; border-radius:6px; padding:0; }}
.sw.on {{ border-color:var(--fg); }}
#colorout {{ width:100%; font-size:11px; margin-top:8px; border:1px solid var(--line);
  border-radius:6px; padding:4px; font-family:ui-monospace,monospace; }}
#colorcopy {{ font-size:11px; padding:4px 8px; margin-top:4px; }}
</style></head><body>
<p class="hint">Блок 2 — примерочная цветов: тап по кружку красит обе сетки сразу.
Тёмный вариант цвета считается автоматически. Ячейка дня и её варианты — на соседней
странице <a href="cell-test.html">cell-test.html</a>.</p>
{"".join(blocks)}
<script>
var TYPES = {L.TYPES};
var PRESETS = {{{presets_js}}};
var current = Object.assign({{}}, PRESETS['Выбранная']);
var out = document.getElementById('colorout');

function lighten(hex, amount) {{
  var n = parseInt(hex.slice(1), 16);
  var r = (n >> 16) & 255, g = (n >> 8) & 255, b = n & 255;
  r = Math.round(r + (255 - r) * amount);
  g = Math.round(g + (255 - g) * amount);
  b = Math.round(b + (255 - b) * amount);
  return 'rgb(' + r + ',' + g + ',' + b + ')';
}}

function apply() {{
  var light = document.getElementById('gwl');
  var dark = document.getElementById('gwd');
  TYPES.forEach(function (t) {{
    light.style.setProperty('--c-' + t, current[t]);
    dark.style.setProperty('--c-' + t, lighten(current[t], 0.42));
    document.getElementById('dot-' + t).style.background = current[t];
  }});
  document.querySelectorAll('.sw').forEach(function (b) {{
    b.classList.toggle('on', current[b.dataset.t] === b.dataset.c);
  }});
  out.value = TYPES.map(function (t) {{ return t + '=' + current[t]; }}).join(' ');
  try {{ localStorage.setItem('iconColors2', out.value); }} catch (e) {{}}
}}

try {{
  var saved = localStorage.getItem('iconColors2');
  if (saved) {{
    saved.split(' ').forEach(function (p) {{
      var kv = p.split('=');
      if (current[kv[0]]) {{ current[kv[0]] = kv[1]; }}
    }});
  }}
}} catch (e) {{}}

document.querySelectorAll('.sw').forEach(function (b) {{
  b.addEventListener('click', function () {{ current[b.dataset.t] = b.dataset.c; apply(); }});
}});
document.querySelectorAll('.preset').forEach(function (b) {{
  b.addEventListener('click', function () {{
    current = Object.assign({{}}, PRESETS[b.dataset.p]); apply();
  }});
}});
document.getElementById('colorcopy').addEventListener('click', function () {{
  out.select();
  if (navigator.clipboard) {{ navigator.clipboard.writeText(out.value); }}
  else {{ document.execCommand('copy'); }}
}});
apply();
</script>
</body></html>"""
    path = os.path.join(L.HERE, "icon-test.html")
    io.open(path, "w", encoding="utf-8").write(html)
    print("wrote", path, len(html), "bytes; sets:", ", ".join(sets) or "none")


if __name__ == "__main__":
    build()
