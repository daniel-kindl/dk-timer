# Handoff: Ocho — Brand & Design System v2

## Overview

Ocho is an Android workout interval timer (EMOM, Tabata, custom intervals), written in Kotlin with Jetpack Compose. Package: `dev.danielkindl.ocho`.

This package specifies **v2** of its visual system: the brand mark and app icon, the four-state phase colour model that drives the session screen, the type system, iconography, motion, and the Android asset set. It supersedes the earlier "DK Timer" naming and the hand-authored vector launcher art currently in the repo.

The single most important idea in this system: **the session screen's background colour is the primary information channel.** Everything else is subordinate to making the current phase unmistakable to a person three metres away, out of breath, not looking directly at the phone.

## About the design files

The files in this bundle are **design references created in HTML** — prototypes showing the intended look and behaviour. They are **not production code to copy**. HTML/CSS was the medium for specifying the design; it is not the target.

The task is to **implement this system in the existing Kotlin/Compose codebase**, using its established patterns: `Color.kt` / `Theme.kt` / `Type.kt` in `ui/theme/`, Compose `MaterialTheme`, and the composables already under `ui/`. Translate the values below into Compose primitives. Do not embed a WebView, and do not port the CSS.

`Ocho Brand.dc.html` is the visual reference. Open it in a browser to see every state rendered. It is organised newest-first in four sections:

- **4a** — the work/rest colour-blindness analysis and the corrected palette (**this is the final state**)
- **3a** — the four-state system developed: all four screens, dark theme, run timeline, error handling, Kotlin mapping
- **2a / 2b / 2c** — three palette directions that were explored; **2c was chosen**. 2a and 2b are rejected, kept for context only
- **1a** — the base brand: wordmark, app icon, full token palette, type, iconography, behaviour rules

Where 3a and 4a disagree with 1a or 2c, **4a wins** — it is the most recent decision.

## Fidelity

**High-fidelity.** Colours, typography, spacing, radii, and motion values are final and exact. Implement them precisely. Layout of the session screen is specified structurally rather than pixel-perfectly, because it must adapt across Android screen sizes — the proportional relationships and minimum sizes are the contract.

---

## Brand

### Name and wordmark

The app is **Ocho** — Spanish for eight, after the eight rounds of a Tabata.

The wordmark is set in type; **there is no drawn logo and none should be created.**

- Text: `ocho`, always lowercase, in all contexts including sentence-initial position
- Font: Space Grotesk 700
- Letter-spacing: `-0.03em`
- Colour: the first three letters take the current text colour; the final `o` takes the brand green (`#3D8560` on light, `#5CA47C` on dark)

The final `o` in green reads as the timer dial. Do not apply the green to any other letter.

### App icon

The icon is the numeral **8**, set in Space Grotesk 700, white, on a solid `#3D8560` plate. It is a typographic mark, not artwork — it stays legible at 48dp because it is one glyph and nothing else.

Adaptive icon geometry:

| Layer | Content | Notes |
|---|---|---|
| Background | Solid `#3D8560`, full 108×108dp bleed | No gradient, no texture |
| Foreground | White `8`, transparent elsewhere | Centred; glyph height ≈ 66dp, inside the 72dp safe circle |
| Monochrome | Black `8`, transparent elsewhere | For Android 13+ themed icons; the system applies its own tint |

The numeral must sit inside the centre 72dp safe zone so square, squircle, and circle launcher masks all crop cleanly. PNGs are supplied — see **Assets**.

---

## The phase colour system

This is the core of v2. The timer has exactly four states, each owning exactly one colour, applied as a **full-bleed screen background**.

### Light theme

| Phase | Plate | On-plate text | Notes |
|---|---|---|---|
| `PREPARE` | `#E8A317` (amber-500) | `#0C110F` (ink) | Countdown before round 1 |
| `WORK` | `#E5484D` (red-500) | `#FFFFFF` | The work interval |
| `REST` | `#B0D8BE` (green-200) | `#0C110F` (ink) | The rest interval |
| `COMPLETE` | `#7C6CF0` (violet-500) | `#FFFFFF` | Session finished |

### Dark theme

| Phase | Plate | On-plate text |
|---|---|---|
| `PREPARE` | `#A8720A` (amber-700) | `#FFFFFF` |
| `WORK` | `#B3282D` (red-700) | `#FFFFFF` |
| `REST` | `#5CA47C` (green-400) | `#0C110F` (ink) |
| `COMPLETE` | `#5645C4` (violet-700) | `#FFFFFF` |

### Why rest is light, not `#3D8560`

This is a deliberate correction and must not be "fixed" back to a mid-green.

Red-500 and green-500 sit at nearly the same lightness. Under deuteranopia — roughly 8% of men, in a category that skews male — they converge into two similar mid-tone plates, and the app's primary information channel fails for those users.

Moving rest to a **light** plate makes work and rest differ by **lightness as well as hue**, so they stay distinct with no colour vision at all. The on-plate text polarity flips with it (white on work, ink on rest), which is a second, redundant signal. Dark theme holds rest at green-400 for the same reason — it is the one phase that does *not* drop to the 700 step.

The consequence across a run is a light–dark–light–dark rhythm, which makes each interval flip catchable in peripheral vision.

### Errors must leave the phase layer

All four signal colours are spent on phases, so red can no longer mean "wrong". The separation is **structural, not chromatic**:

- **Phase colour** is only ever a full-bleed background.
- **Errors** are only ever a tinted plate *inside* the layout: surface `#FDE0E1` (red-100), text and glyph `#B3282D` (red-700), 8dp radius, a `octagon-alert` glyph, no border, no left-accent bar.

Additional rule: **a red error plate must never appear over a red work screen.** Errors raised mid-session either wait for the next rest interval or surface in the ongoing notification instead. The running clock is never covered.

### Dynamic colour is OFF

Do not enable Material You dynamic colour. A timer that encodes meaning in colour cannot delegate its palette to the user's wallpaper — work must be red on every device. Use a fixed `lightColorScheme` / `darkColorScheme`.

### Reference implementation

```kotlin
// ui/theme/PhaseColors.kt
enum class Phase { PREPARE, WORK, REST, COMPLETE }

data class PhaseTheme(val plate: Color, val onPlate: Color)

private val Ink = Color(0xFF0C110F)
private val Paper = Color(0xFFFFFFFF)

fun phaseTheme(p: Phase, dark: Boolean): PhaseTheme = when (p) {
    Phase.PREPARE  -> if (dark) PhaseTheme(Color(0xFFA8720A), Paper)
                      else      PhaseTheme(Color(0xFFE8A317), Ink)
    Phase.WORK     -> if (dark) PhaseTheme(Color(0xFFB3282D), Paper)
                      else      PhaseTheme(Color(0xFFE5484D), Paper)
    Phase.REST     -> if (dark) PhaseTheme(Color(0xFF5CA47C), Ink)
                      else      PhaseTheme(Color(0xFFB0D8BE), Ink)
    Phase.COMPLETE -> if (dark) PhaseTheme(Color(0xFF5645C4), Paper)
                      else      PhaseTheme(Color(0xFF7C6CF0), Paper)
}

// Errors never use the plate
val ErrorSurface   = Color(0xFFFDE0E1) // red-100
val OnErrorSurface = Color(0xFFB3282D) // red-700
```

---

## Screens

### Session screen

**Purpose.** The screen the user looks at during a workout. It answers one question: what am I doing right now, and for how much longer.

**Layout.** Full-bleed phase colour behind everything, edge to edge, including behind the status bar (draw edge-to-edge; tint system bars to match the plate). Vertical stack, generous padding (24–32dp horizontal):

1. **Phase label** — top. JetBrains Mono, 11–13sp, letter-spacing `0.08em`, uppercase (`WORK`, `REST`, `PREPARE`, `COMPLETE`). On-plate colour at ~82% opacity.
2. **Clock** — centre, the dominant element. Space Grotesk 700, as large as will fit (76sp is the reference on a phone; scale up on larger screens), letter-spacing `-0.03em`, line-height 1.0, **tabular figures** so digits never shift width. Format `MM:SS`.
3. **Round counter** — below the clock. JetBrains Mono, 12–16sp, on-plate colour at ~82% opacity. Format `round 3/8`.
4. **Controls** — bottom. Pause/resume primary target is **96dp**; all other targets minimum **48dp**.

**Behaviour.** The screen holds a wake lock while a session runs, and keeps a live ongoing notification, so the clock survives a lock and a backgrounding.

**Transition.** On phase change, the background colour crossfades over **340ms** on `cubic-bezier(.62,.02,.28,1)`. The audio cue and haptic fire **on the frame the interval flips** — never delayed by the animation. When the system animator duration scale is 0, the crossfade is zero and the colour swaps instantly; the cue is unaffected. Motion is decoration here, colour is information.

### Setup screen — run timeline

The configured workout is previewed as a proportional horizontal strip, so the user sees the shape of the session before starting it: an amber prepare segment, then alternating red work / light-green rest segments, capped by a violet complete segment. 44dp tall, 6dp radius, 2dp gaps between segments. Beneath it, three mono labels: start time, the pattern (`8 × (20s work / 10s rest)`), and end time.

### Empty state — presets

Dashed 1dp `#DDE3E0` border, 12dp radius, centred body text: *"Presets appear here after you save a workout."* Describe the trigger, never the emptiness.

---

## Typography

Three families, no exceptions. All three are already vendored at `app/src/main/res/font/` (`space_grotesk.ttf`, `ibm_plex_sans.ttf`, `jetbrains_mono.ttf`).

**The governing rule: if a human wrote it, IBM Plex Sans. If the timer produced it, JetBrains Mono. Space Grotesk is display only.**

| Role | Family | Weight | Size | Tracking | Used for |
|---|---|---|---|---|---|
| `displayLarge` | Space Grotesk | 700 | 76sp | `-0.03em` | The clock face. Tabular figures. |
| `displayMedium` | Space Grotesk | 700 | 40sp | `-0.03em` | Wordmark, completion figures |
| `titleLarge` | Space Grotesk | 600 | 20sp | `-0.022em` | Section headings |
| `titleMedium` | JetBrains Mono | 400 | 20sp | 0 | Round counts, durations, intervals |
| `bodyLarge` | IBM Plex Sans | 400 | 16sp | 0 | Prose, settings labels. Line-height 1.65 |
| `bodyMedium` | IBM Plex Sans | 400 | 14sp | 0 | UI text, secondary labels |
| `labelSmall` | JetBrains Mono | 400 | 11sp | `0.08em` | Uppercase eyebrows, phase labels |

Minimum on-screen text: 12sp. Never set the clock below 48sp on any screen size.

---

## Iconography

**Lucide** (ISC licence — free and open source). Glyphs are vendored as individual SVGs in `assets/icons/` in this bundle; convert to Android vector drawables (`res/drawable/`) on import.

**No icon is hand-drawn.** If a needed glyph is not in the Lucide set, add it from Lucide upstream — do not author one.

| Glyph | Use |
|---|---|
| `play` | Start session |
| `rotate-cw` | Rounds / repeat |
| `bell` | Audio cues |
| `zap` | Intensity |
| `activity` | Session / history |
| `settings` | Settings |
| `octagon-alert` | Error plates |

Rules: stroke width **1.75**, rounded caps and joins, `currentColor`. Sizes 16dp in controls, 20dp in nav rails and feature marks, 24dp maximum. **One weight per screen** — never mix filled and stroked glyphs. Icons label an action or encode state; they are never decorative. Body copy gets no icons.

**No emoji anywhere** — not in UI, not in notifications, not in copy.

---

## Design tokens

### Brand green

| Token | Hex | | Token | Hex |
|---|---|---|---|---|
| green-50 | `#EDF6F1` | | green-500 | `#3D8560` |
| green-100 | `#D6EBDD` | | green-600 | `#336E4F` |
| green-200 | `#B0D8BE` | | green-700 | `#2A5940` |
| green-300 | `#84BE9B` | | green-800 | `#214531` |
| green-400 | `#5CA47C` | | green-900 | `#173124` |
| | | | green-950 | `#0E1D16` |

`green-500` is the primary action colour. `green-600` is hover, `green-700` is pressed, `green-300` is the accent on dark surfaces.

### Neutrals (green-tinted graphite — not pure grey)

| Token | Hex | | Token | Hex |
|---|---|---|---|---|
| n-0 | `#FFFFFF` | | n-500 | `#6B7873` |
| n-50 | `#F7F9F8` | | n-600 | `#4E5A55` |
| n-100 | `#EDF1EF` | | n-700 | `#38423E` |
| n-200 | `#DDE3E0` | | n-800 | `#232B28` |
| n-300 | `#C2CBC7` | | n-900 | `#161C1A` |
| n-400 | `#93A09A` | | n-950 | `#0C110F` |

### Signal colours — state only, never decoration

| Token | Hex | Meaning in Ocho |
|---|---|---|
| amber-500 / 700 | `#E8A317` / `#A8720A` | Prepare phase |
| red-100 | `#FDE0E1` | Error surface |
| red-500 / 700 | `#E5484D` / `#B3282D` | Work phase / error text |
| violet-500 / 700 | `#7C6CF0` / `#5645C4` | Complete phase |
| blue-500 | `#2E82F6` | Reserved — informational only |

### Spacing scale (dp)

`2, 4, 6, 8, 12, 16, 20, 24, 32, 40, 48, 64, 80, 112, 160`

Controls pad 12. Cards pad 24. Screen sections 32–48.

### Radius (dp)

| Token | Value | Use |
|---|---|---|
| radius-1 | 3 | Tags |
| radius-2 | 6 | Controls, buttons, inputs |
| radius-3 | 8 | Callouts, error plates |
| radius-4 | 12 | Cards |
| radius-5 | 16 | Sheets, dialogs |
| radius-full | 999 | Pills |

### Elevation

| Token | Value |
|---|---|
| shadow-1 | `0 1px 2px rgba(12,17,15,.06)` |
| shadow-2 | `0 1px 2px rgba(12,17,15,.05), 0 4px 10px -3px rgba(12,17,15,.08)` |
| shadow-3 | `0 2px 4px rgba(12,17,15,.05), 0 12px 26px -8px rgba(12,17,15,.14)` |
| shadow-4 | `0 4px 8px rgba(12,17,15,.06), 0 28px 56px -16px rgba(12,17,15,.20)` |
| shadow-focus | `0 0 0 3px rgba(20,168,99,.24)` |

Compose has no direct multi-layer shadow equivalent — approximate with `Modifier.shadow(elevation, shape)` at roughly 1dp / 3dp / 8dp / 16dp respectively, or `Card` tonal elevation. Shadows only lift things off the page; **borders do the structural work.**

A card is: surface `#FFFFFF`, a **1dp `#DDE3E0` hairline border**, 12dp radius, shadow-2, 24dp padding. Never a coloured left border. Never a rounded box with an accent stripe.

### Motion

| Token | Value |
|---|---|
| ease-out | `cubic-bezier(.22,.75,.3,1)` — enter, hover |
| ease-in-out | `cubic-bezier(.62,.02,.28,1)` — movement, phase crossfade |
| ease-snap | `cubic-bezier(.2,1.1,.35,1)` — toggles only |
| dur-1 … dur-5 | 90 / 150 / 220 / 340 / 560 ms |

UI state changes use 150ms. The phase crossfade uses 340ms. Nothing loops except spinners. No parallax, no entrance animations. All durations collapse to 0 when the system animator duration scale is 0.

### States

- **Pressed** — fill darkens two ramp steps and translates 1dp down. **No scale transforms.**
- **Focused** — the green ring (`shadow-focus`), always visible, never suppressed.
- **Disabled** — 45% opacity, shadow removed.
- **Selected** — tinted plate + green glyph + a 2dp green bar.
- **Loading** — green ring spinner under 2s; anything longer gets a status line with real detail.

Opacity is never used to express hover or press on a fill — darken the ramp step instead.

---

## Voice

The voice is a competent engineer talking to another engineer. Applied to Ocho:

- **Sentence case everywhere** — headings, buttons, labels, settings. Never Title Case.
- **Buttons are verbs**: "Start", "Save preset", "Delete workout". Never "Submit" or "OK".
- **Errors state the cause and the next move**: *"Timer drifted while backgrounded. Battery optimisation is on for Ocho — exempt the app to keep the clock exact."* Never "Oops! Something went wrong."
- **Empty states describe the trigger**: *"Presets appear here after you save a workout."*
- **Numbers are specific**: "8 rounds", "20s work". Never "a few", "blazing fast".
- One idea per sentence. No exclamation marks. No emoji.

---

## Accessibility requirements

These are contractual, not aspirational:

1. **Phase is never encoded by colour alone.** Every phase change is carried simultaneously by the background colour, the uppercase text label, the audio cue, and the haptic. A user with no colour vision, or with the screen face-down, still knows the phase.
2. **Work and rest differ by lightness**, per the rationale above. Do not equalise them.
3. Minimum touch target **48dp**; primary session control **96dp**.
4. On-plate text meets 4.5:1 against its plate — this is why prepare and rest take ink text rather than white.
5. Respect the system animator duration scale; never let a reduced-motion setting delay an audio or haptic cue.
6. Support system font scaling on all text except the clock, which may cap its scale to avoid truncation — but must never fall below 48sp.

---

## Assets

All supplied under `assets/` in this bundle.

### `assets/android/` — ready to use

| File | Size | Notes |
|---|---|---|
| `ic_launcher_foreground.png` | 432×432 | Transparent; white `8`. This is xxxhdpi — let Android Studio's Image Asset tool generate the mdpi–xxhdpi buckets, or scale to 108/162/216/324. |
| `ic_launcher_background.png` | 432×432 | Solid `#3D8560`. May be replaced with a `<color>` drawable. |
| `ic_launcher_monochrome.png` | 432×432 | Transparent; black `8`. For Android 13+ themed icons. |
| `ic_notification.png` | 96×96 | White `8`, flat alpha. Status-bar icon — Android renders alpha only. |
| `play_store_512.png` | 512×512 | Full bleed, no rounding (Google Play applies its own mask). |
| `wordmark_light.png` | 640×220 | For splash and about screen, light background. |
| `wordmark_dark.png` | 640×220 | Dark background variant. |

### `assets/icons/` — Lucide source SVGs

`play.svg`, `bell.svg`, `rotate-cw.svg`, `settings.svg`, `zap.svg`, `activity.svg`. Convert to Android vector drawables on import. Additional glyphs (e.g. `octagon-alert`) should be taken from Lucide upstream at the same stroke weight.

### Fonts

Already present in the repo at `app/src/main/res/font/`. No action needed.

---

## Migration notes for the existing repo

1. **Delete the hand-authored launcher vectors**: `app/src/main/res/drawable/ic_launcher_foreground.xml`, `ic_launcher_background.xml`, `ic_launcher_monochrome.xml`. They depict a stopwatch that is no longer the mark. Replace with the supplied PNGs (or regenerate as vectors from the numeral if you prefer vector — but the glyph must be the `8` in Space Grotesk).
2. **Update `mipmap-anydpi/ic_launcher.xml` and `ic_launcher_round.xml`** to point at the new foreground/background/monochrome resources.
3. **Rename user-facing strings** from any "DK Timer" usage to "Ocho" in `res/values/strings.xml`. The package `dev.danielkindl.ocho` already matches.
4. **Add `PhaseColors.kt`** as above; refactor the session screen to read `phaseTheme(phase, isDark)` rather than hard-coded colours.
5. **Confirm dynamic colour is disabled** in `Theme.kt`.

---

## Files in this bundle

| Path | What it is |
|---|---|
| `README.md` | This document — self-sufficient specification |
| `Ocho Brand.dc.html` | Visual reference. Open in a browser. Sections 4a → 3a → 2a/2b/2c → 1a, newest first |
| `assets/android/*.png` | Launcher, notification, store, and wordmark assets |
| `assets/icons/*.svg` | Lucide source glyphs |

`Ocho Brand.dc.html` needs its sibling `_ds/` token stylesheets to render fully; if it is opened standalone and looks unstyled, rely on this README — every value in the prototype is transcribed above.
