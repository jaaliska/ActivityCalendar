#!/usr/bin/env python3
"""Builds cell-test.html: the day cell anatomy for brief 2.

Three ways to show several activities in one day, each rendered as cell states,
as a strip at real size and as a whole month.
"""
import io
import os

import iconlib as L

VARIANTS = [("stack", "Стопка: до двух значков, дальше «+N»", "")]
# name -> (day number, activity types, today, selected, adjacent)
STATES = [
    ("Пусто", 4, [], False, False, False),
    ("Одна", 3, ["running"], False, False, False),
    ("Две одного типа", 7, ["yoga", "yoga"], False, False, False),
    ("Две разных", 5, ["strength", "yoga"], False, False, False),
    ("Три разных", 16, ["running", "strength", "yoga"], False, False, False),
    ("Пять", 30, ["walking", "yoga", "running", "cycling", "strength"], False, False, False),
    ("Other", 26, ["other"], False, False, False),
    ("Сегодня", 23, [], True, False, False),
    ("Сегодня с тренировкой", 23, ["cycling"], True, False, False),
    ("Выбран", 12, ["running"], False, True, False),
    ("Выбран, пустой", 13, [], False, True, False),
    ("Сегодня и выбран", 23, ["cycling"], True, True, False),
    ("Соседний месяц", 30, ["badminton"], False, False, True),
    ("Соседний, выбран", 30, ["badminton"], False, True, True),
]


def states_block(icons, variant, colored, zoom=1):
    cells = []
    for name, day, acts, today, selected, adjacent in STATES:
        cell = L.day_cell(icons, day, acts, variant, colored, today, selected, adjacent)
        cells.append(f'<div class="statecell"><div class="sname">{name}</div>'
                     f'<div class="zoom{zoom}">{cell}</div></div>')
    css = "states z3" if zoom == 3 else "states"
    return f'<div class="{css}">{"".join(cells)}</div>'


def build():
    sets = L.load_sets()
    blocks = []
    for name, icons in sets.items():
        parts = [f"<h1>{name}</h1>"]
        for key, title, subtitle in VARIANTS:
            parts.append(f"""
<section>
  <h2>{title}</h2>
  {states_block(icons, key, True)}
  <h2>Август 2026: неделя целиком, соседние месяцы приглушены</h2>
  {L.month_grid(icons, key, True, selected=(8, 12))}
  <h2>То же, выбран день соседнего месяца — 30 июля</h2>
  {L.month_grid(icons, key, True, selected=(7, 30))}
</section>
<section class="dark">
  <h2>{title} — тёмная тема</h2>
  {L.month_grid(icons, key, True, selected=(8, 12))}
</section>
<section>
  <h2>Крупно: те же состояния втрое</h2>
  {states_block(icons, key, True, zoom=3)}
</section>""")
        blocks.append('<article class="set">' + "".join(parts) + "</article>")

    html = f"""<!doctype html>
<html lang="ru"><head><meta charset="utf-8">
<meta name="viewport" content="width=device-width, initial-scale=1">
<title>Ячейка дня: варианты</title>
<style>{L.base_css()}
.states {{ display:flex; flex-wrap:wrap; gap:10px; margin-bottom:8px; }}
.statecell {{ width:calc(33.33% - 7px); }}
.sname {{ font-size:9px; color:var(--dim); margin-bottom:2px; height:22px; }}
.zoom1 {{ }}
.zoom3 {{ zoom:3; margin-bottom:6px; }}
.states.z3 .statecell {{ width:calc(50% - 5px); }}
</style></head><body>
<p class="hint">Смотри на месяц целиком, а не на отдельную ячейку: вопрос не «красиво ли»,
а «видно ли с одного взгляда, где день с одной тренировкой, а где с тремя». 12 августа
показан как выбранный день, 23 — сегодня, 30 — пять тренировок.</p>
{"".join(blocks)}
</body></html>"""
    path = os.path.join(L.HERE, "cell-test.html")
    io.open(path, "w", encoding="utf-8").write(html)
    print("wrote", path, len(html), "bytes; sets:", ", ".join(sets) or "none")


if __name__ == "__main__":
    build()
