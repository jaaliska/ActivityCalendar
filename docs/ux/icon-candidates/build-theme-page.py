#!/usr/bin/env python3
"""Builds theme-test.html: pick the Material 3 seed colour and see the whole screen in it.

Tonal palettes are approximated in CIE LCh: the tone axis is L*, which is what Material's
HCT uses too, so the preview is close enough to choose by. The exported values go through
the official Material Theme Builder before they reach the app.
"""
import io
import os

import iconlib as L

SEEDS = [
    ("Янтарный", 80), ("Оранжевый", 55), ("Малиновый", 10), ("Сиреневый", 300),
    ("Синий", 260), ("Бирюзовый", 195), ("Зелёный", 140),
]

PAGE = r"""<!doctype html>
<html lang="ru"><head><meta charset="utf-8">
<meta name="viewport" content="width=device-width, initial-scale=1">
<title>Тема приложения: выбор seed</title>
<style>
@@BASECSS@@
body { background:var(--bg); }
#controls { position:sticky; top:0; z-index:3; background:#fff; border-bottom:1px solid #e3e3e8;
  padding:8px 4px; }
#seeds { display:flex; gap:6px; flex-wrap:wrap; margin-bottom:6px; }
.seed { font-size:12px; padding:4px 8px; border:1px solid #ddd; border-radius:16px;
  background:transparent; display:flex; align-items:center; gap:5px; color:#1b1b1f; }
.seed.on { border-color:#1b1b1f; }
.seed i { width:12px; height:12px; border-radius:50%; display:block; }
#controls label { display:flex; align-items:center; gap:8px; font-size:11px; color:#6b6b75; }
#controls input { flex:1; }
#out { width:100%; font-size:11px; margin-top:6px; border:1px solid #e3e3e8; border-radius:6px;
  padding:4px; font-family:ui-monospace,monospace; }
.phone { border-radius:16px; padding:10px; margin:10px 0; }
.appbar { display:flex; align-items:center; justify-content:space-between; margin-bottom:10px; }
.apptitle { font-size:20px; font-weight:600; }
.iconbtn { width:32px; height:32px; border-radius:50%; display:flex; align-items:center;
  justify-content:center; font-size:14px; }
.panel { margin-top:10px; border-radius:12px; padding:10px; }
.panel h4 { margin:0 0 6px; font-size:13px; }
.actrow { display:flex; align-items:center; gap:8px; font-size:12px; margin-bottom:4px; }
.filled { border:none; border-radius:20px; padding:9px 16px; font-size:13px; font-weight:600; }
.tonal { border:none; border-radius:20px; padding:9px 16px; font-size:13px; margin-left:6px; }
.chiprow { margin-top:10px; display:flex; gap:6px; }
.roles { display:flex; flex-wrap:wrap; gap:3px; margin-top:8px; }
.roles div { width:calc(25% - 3px); height:26px; border-radius:4px; font-size:8px;
  display:flex; align-items:flex-end; padding:2px; }
</style></head><body>
<div id="controls">
  <div id="seeds">@@SEEDBTNS@@</div>
  <label>Тон <input id="hue" type="range" min="0" max="359" value="80"><output id="hval">80</output></label>
  <label>Насыщенность <input id="chroma" type="range" min="0" max="60" value="40"><output id="cval">40</output></label>
  <textarea id="out" rows="3" readonly></textarea>
</div>

<div class="phone light" id="pl">@@SCREEN@@</div>
<div class="phone dark" id="pd">@@SCREEN@@</div>

<script>
function srgbToLin(v){v/=255;return v<=0.04045?v/12.92:Math.pow((v+0.055)/1.055,2.4);}
function linToSrgb(v){v=v<=0.0031308?v*12.92:1.055*Math.pow(v,1/2.4)-0.055;
  return Math.max(0,Math.min(255,Math.round(v*255)));}
function labToXyz(l,a,b){var fy=(l+16)/116,fx=fy+a/500,fz=fy-b/200;
  function f(t){var t3=t*t*t;return t3>0.008856?t3:(t-16/116)/7.787;}
  return [f(fx)*0.95047,f(fy)*1.0,f(fz)*1.08883];}
function xyzToRgb(x,y,z){
  var r=x*3.2406+y*-1.5372+z*-0.4986, g=x*-0.9689+y*1.8758+z*0.0415,
      b=x*0.0557+y*-0.2040+z*1.0570;
  return [linToSrgb(r),linToSrgb(g),linToSrgb(b)];}
function inGamut(x,y,z){
  var r=x*3.2406+y*-1.5372+z*-0.4986, g=x*-0.9689+y*1.8758+z*0.0415,
      b=x*0.0557+y*-0.2040+z*1.0570;
  return r>=-0.001&&g>=-0.001&&b>=-0.001&&r<=1.001&&g<=1.001&&b<=1.001;}
function hex(rgb){return '#'+rgb.map(function(v){return ('0'+v.toString(16)).slice(-2);})
  .join('').toUpperCase();}

/* One tone of a tonal palette: keep hue, drop chroma until the colour fits sRGB. */
function tone(t, chroma, hue) {
  var h = hue * Math.PI / 180;
  for (var c = chroma; c >= 0; c -= 1) {
    var xyz = labToXyz(t, Math.cos(h) * c, Math.sin(h) * c);
    if (inGamut(xyz[0], xyz[1], xyz[2])) { return hex(xyzToRgb(xyz[0], xyz[1], xyz[2])); }
  }
  return hex(xyzToRgb.apply(null, labToXyz(t, 0, 0)));
}

function scheme(hue, chroma) {
  var P = function (t) { return tone(t, chroma, hue); };            /* primary */
  var S = function (t) { return tone(t, Math.max(8, chroma / 3), hue); };  /* secondary */
  var N = function (t) { return tone(t, 4, hue); };                 /* neutral */
  var NV = function (t) { return tone(t, 8, hue); };                /* neutral variant */
  return {
    light: {
      primary: P(40), onPrimary: P(100), primaryContainer: P(90), onPrimaryContainer: P(10),
      secondaryContainer: S(90), onSecondaryContainer: S(10),
      surface: N(98), surfaceContainer: N(94), surfaceContainerHigh: N(92),
      onSurface: N(10), onSurfaceVariant: NV(30), outline: NV(50)
    },
    dark: {
      primary: P(80), onPrimary: P(20), primaryContainer: P(30), onPrimaryContainer: P(90),
      secondaryContainer: S(30), onSecondaryContainer: S(90),
      surface: N(6), surfaceContainer: N(12), surfaceContainerHigh: N(17),
      onSurface: N(90), onSurfaceVariant: NV(80), outline: NV(60)
    }
  };
}

function paint(el, s) {
  el.style.background = s.surface;
  el.style.color = s.onSurface;
  el.style.setProperty('--bg', s.surface);
  el.style.setProperty('--fg', s.onSurface);
  el.style.setProperty('--dim', s.onSurfaceVariant);
  el.style.setProperty('--cellbg', s.surfaceContainerHigh);
  el.style.setProperty('--sel', s.secondaryContainer);
  el.style.setProperty('--line', s.outline);
  el.querySelectorAll('.filled').forEach(function (b) {
    b.style.background = s.primary; b.style.color = s.onPrimary; });
  el.querySelectorAll('.tonal').forEach(function (b) {
    b.style.background = s.secondaryContainer; b.style.color = s.onSecondaryContainer; });
  el.querySelectorAll('.panel').forEach(function (p) {
    p.style.background = s.surfaceContainer; });
  el.querySelectorAll('.iconbtn').forEach(function (p) {
    p.style.background = s.secondaryContainer; p.style.color = s.onSecondaryContainer; });
  var roles = el.querySelector('.roles');
  roles.innerHTML = Object.keys(s).map(function (k) {
    return '<div style="background:' + s[k] + ';color:' + s.onSurface + '">' + k + '</div>';
  }).join('');
}

var hueEl = document.getElementById('hue'), chrEl = document.getElementById('chroma');

function render() {
  var hue = +hueEl.value, chroma = +chrEl.value;
  var sc = scheme(hue, chroma);
  paint(document.getElementById('pl'), sc.light);
  paint(document.getElementById('pd'), sc.dark);
  document.getElementById('hval').textContent = hue;
  document.getElementById('cval').textContent = chroma;
  document.getElementById('out').value =
    'seed=' + tone(50, chroma, hue) + ' hue=' + hue + ' chroma=' + chroma +
    '\nsurface ' + sc.light.surface + ' / ' + sc.dark.surface +
    '  ячейка ' + sc.light.surfaceContainerHigh + ' / ' + sc.dark.surfaceContainerHigh +
    '\nвыбранный день ' + sc.light.secondaryContainer + ' / ' + sc.dark.secondaryContainer +
    '  primary ' + sc.light.primary + ' / ' + sc.dark.primary;
  document.querySelectorAll('.seed').forEach(function (b) {
    b.classList.toggle('on', +b.dataset.h === hue); });
  try { localStorage.setItem('themeSeed', hue + ',' + chroma); } catch (e) {}
}

document.querySelectorAll('.seed').forEach(function (b) {
  b.querySelector('i').style.background = tone(50, 40, +b.dataset.h);
  b.addEventListener('click', function () { hueEl.value = b.dataset.h; render(); });
});
hueEl.addEventListener('input', render);
chrEl.addEventListener('input', render);
try {
  var saved = localStorage.getItem('themeSeed');
  if (saved) { hueEl.value = saved.split(',')[0]; chrEl.value = saved.split(',')[1]; }
} catch (e) {}
render();
</script>
</body></html>
"""


def screen(icons):
    return f"""
  <div class="appbar">
    <div class="apptitle">August 2026</div>
    <div style="display:flex;gap:6px">
      <div class="iconbtn">◷</div><div class="iconbtn">⚙</div>
    </div>
  </div>
  {L.month_grid(icons, "stack", True, selected=(8, 12))}
  <div class="panel">
    <h4>Wednesday, 12 August</h4>
    <div class="actrow">{L.icon(icons, "running", 18, True)} Running · 07:30 · 42 min · 6.4 km</div>
    <div class="actrow">{L.icon(icons, "yoga", 18, True)} Yoga · 20:15 · 30 min</div>
  </div>
  <div class="chiprow">
    <button class="filled">Import CSV</button>
    <button class="tonal">Connect</button>
  </div>
  <div class="roles"></div>
"""


def build():
    sets = L.load_sets()
    icons = sets.get("monocolor_set") or next(iter(sets.values()))
    buttons = "".join(
        f'<button type="button" class="seed" data-h="{h}"><i></i>{name}</button>'
        for name, h in SEEDS)
    html = (PAGE
            .replace("@@BASECSS@@", L.base_css())
            .replace("@@SEEDBTNS@@", buttons)
            .replace("@@SCREEN@@", screen(icons)))
    path = os.path.join(L.HERE, "theme-test.html")
    io.open(path, "w", encoding="utf-8").write(html)
    print("wrote", path, len(html), "bytes")


if __name__ == "__main__":
    build()
