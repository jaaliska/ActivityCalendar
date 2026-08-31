#!/usr/bin/env python3
"""Prints the Material 3 colour scheme of one theme as a Kotlin file.

    python3 build-color-scheme.py blue > BlueColorScheme.kt

Same CIE LCh maths as build-theme-page.py, which is where the schemes were picked.
Tones follow the Material 3 role table; the error palette is the Material baseline
and does not depend on the seed.
"""
import math
import sys

# Hue and chroma of every scheme in docs/ux/theme.md.
SCHEMES = {
    "crimson": (10, 43, "Crimson"),
    "blue": (257, 60, "Blue"),
    "orange": (55, 35, "Orange"),
    "green": (140, 43, "Green"),
}

# What the scheme is checked against: the values written down in docs/ux/theme.md.
ANCHORS = {
    "crimson": ("#FEF8F9", "#191112", "#F1E6E7", "#302829", "#FEDADE", "#5D3F43",
                "#9E3D54", "#FFB2BE"),
    "blue": ("#F5FAFF", "#0E1419", "#E3E9EF", "#252B2F", "#CCE6FE", "#244A65",
             "#076491", "#90CDFE"),
    "orange": ("#FFF8F5", "#19120E", "#EFE6E2", "#2F2925", "#F7DDD0", "#564338",
               "#885030", "#FBB792"),
    "green": ("#F5FBF4", "#10150F", "#E4EAE3", "#272B26", "#D4E8D0", "#3A4B38",
              "#2F6B2F", "#98D691"),
}

ANCHOR_ROLES = (
    ("surface", "light"), ("surface", "dark"),
    ("surfaceContainerHigh", "light"), ("surfaceContainerHigh", "dark"),
    ("secondaryContainer", "light"), ("secondaryContainer", "dark"),
    ("primary", "light"), ("primary", "dark"),
)


def _lin_to_srgb(v):
    v = v * 12.92 if v <= 0.0031308 else 1.055 * (v ** (1 / 2.4)) - 0.055
    return max(0, min(255, round(v * 255)))


def _lab_to_xyz(lightness, a, b):
    fy = (lightness + 16) / 116
    fx, fz = fy + a / 500, fy - b / 200

    def f(t):
        cubed = t ** 3
        return cubed if cubed > 0.008856 else (t - 16 / 116) / 7.787

    return f(fx) * 0.95047, f(fy) * 1.0, f(fz) * 1.08883


def _xyz_to_linear(x, y, z):
    return (x * 3.2406 + y * -1.5372 + z * -0.4986,
            x * -0.9689 + y * 1.8758 + z * 0.0415,
            x * 0.0557 + y * -0.2040 + z * 1.0570)


def tone(value, chroma, hue):
    """One tone of a tonal palette: keeps the hue, drops chroma until it fits sRGB."""
    radians = math.radians(hue)
    chroma = float(chroma)
    while chroma >= 0:
        xyz = _lab_to_xyz(value, math.cos(radians) * chroma, math.sin(radians) * chroma)
        if all(-0.001 <= channel <= 1.001 for channel in _xyz_to_linear(*xyz)):
            break
        chroma -= 1
    else:
        xyz = _lab_to_xyz(value, 0, 0)
    return "#" + "".join(f"{_lin_to_srgb(c):02X}" for c in _xyz_to_linear(*xyz))


def scheme(hue, chroma):
    """Both themes of one scheme, as role name to hex string."""
    primary = lambda t: tone(t, chroma, hue)
    secondary = lambda t: tone(t, max(8, chroma / 3), hue)
    tertiary = lambda t: tone(t, max(8, chroma / 2), hue)
    neutral = lambda t: tone(t, 4, hue)
    variant = lambda t: tone(t, 8, hue)

    light = dict(
        primary=primary(40), onPrimary=primary(100),
        primaryContainer=primary(90), onPrimaryContainer=primary(10),
        secondary=secondary(40), onSecondary=secondary(100),
        secondaryContainer=secondary(90), onSecondaryContainer=secondary(10),
        tertiary=tertiary(40), onTertiary=tertiary(100),
        tertiaryContainer=tertiary(90), onTertiaryContainer=tertiary(10),
        error="#B3261E", onError="#FFFFFF",
        errorContainer="#F9DEDC", onErrorContainer="#410E0B",
        background=neutral(98), onBackground=neutral(10),
        surface=neutral(98), onSurface=neutral(10),
        surfaceVariant=variant(90), onSurfaceVariant=variant(30),
        surfaceTint=primary(40), inverseSurface=neutral(20),
        inverseOnSurface=neutral(95), inversePrimary=primary(80),
        outline=variant(50), outlineVariant=variant(80), scrim="#000000",
        surfaceDim=neutral(87), surfaceBright=neutral(98),
        surfaceContainerLowest=neutral(100), surfaceContainerLow=neutral(96),
        surfaceContainer=neutral(94), surfaceContainerHigh=neutral(92),
        surfaceContainerHighest=neutral(90),
    )
    dark = dict(
        primary=primary(80), onPrimary=primary(20),
        primaryContainer=primary(30), onPrimaryContainer=primary(90),
        secondary=secondary(80), onSecondary=secondary(20),
        secondaryContainer=secondary(30), onSecondaryContainer=secondary(90),
        tertiary=tertiary(80), onTertiary=tertiary(20),
        tertiaryContainer=tertiary(30), onTertiaryContainer=tertiary(90),
        error="#F2B8B5", onError="#601410",
        errorContainer="#8C1D18", onErrorContainer="#F9DEDC",
        background=neutral(6), onBackground=neutral(90),
        surface=neutral(6), onSurface=neutral(90),
        surfaceVariant=variant(30), onSurfaceVariant=variant(80),
        surfaceTint=primary(80), inverseSurface=neutral(90),
        inverseOnSurface=neutral(20), inversePrimary=primary(40),
        outline=variant(60), outlineVariant=variant(30), scrim="#000000",
        surfaceDim=neutral(6), surfaceBright=neutral(24),
        surfaceContainerLowest=neutral(4), surfaceContainerLow=neutral(10),
        surfaceContainer=neutral(12), surfaceContainerHigh=neutral(17),
        surfaceContainerHighest=neutral(22),
    )
    return {"light": light, "dark": dark}


def check(name):
    """Compares the computed scheme with the values recorded in theme.md."""
    hue, chroma, _ = SCHEMES[name]
    built = scheme(hue, chroma)
    lines = []
    for (role, theme), expected in zip(ANCHOR_ROLES, ANCHORS[name]):
        got = built[theme][role]
        lines.append(f"{'ok  ' if got == expected else 'DIFF'} {name:8} {theme:5} "
                     f"{role:22} {got} theme.md {expected}")
    return lines


def kotlin(name):
    hue, chroma, title = SCHEMES[name]
    built = scheme(hue, chroma)
    out = [
        "package com.jaaliska.activitycalendar.ui.theme",
        "",
        "import androidx.compose.material3.darkColorScheme",
        "import androidx.compose.material3.lightColorScheme",
        "import androidx.compose.ui.graphics.Color",
        "",
        f"// Generated by docs/ux/icon-candidates/build-color-scheme.py {name}",
        "",
    ]
    for theme, builder in (("Light", "lightColorScheme"), ("Dark", "darkColorScheme")):
        out.append(f"val {title}{theme}Colors = {builder}(")
        for role, value in built[theme.lower()].items():
            out.append(f"    {role} = Color(0xFF{value[1:]}),")
        out.append(")")
        out.append("")
    return "\n".join(out)


if __name__ == "__main__":
    argument = sys.argv[1] if len(sys.argv) > 1 else "check"
    if argument == "check":
        for name in SCHEMES:
            print("\n".join(check(name)))
    elif argument in SCHEMES:
        print(kotlin(argument))
    else:
        sys.exit(f"usage: build-color-scheme.py [check|{'|'.join(SCHEMES)}]")
