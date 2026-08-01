# Ocho

A minimal, production-quality Android workout interval timer — built to be reliable
while you're mid-effort and not looking at the screen.

[![Dev CI](https://github.com/daniel-kindl/ocho/actions/workflows/dev-ci.yml/badge.svg?branch=dev)](https://github.com/daniel-kindl/ocho/actions/workflows/dev-ci.yml)

*An **ocho** is a figure-eight step in tango. It's also Spanish for **eight** — the
round count of a classic Tabata.*

---

## Features

| Feature | Detail |
|---------|--------|
| ⏱ EMOM timer | Total duration and interval length, set with drum-roll mm:ss pickers |
| 🔁 Tabata timer | Total, work, and rest durations; phases alternate automatically |
| 🎨 Phase colours | Full-screen amber / red / green / violet plate per phase, readable across a room and distinguishable without colour vision |
| 📊 Run timeline | Proportional preview of a workout's shape before you start it |
| 🔔 Sound feedback | Distinct tones per event, on the alarm stream so silent mode can't mute them |
| 📳 Vibration feedback | Different patterns for intervals and for completion |
| ⏸ Pause & resume | Freeze mid-session without drift or losing interval alignment |
| 3️⃣ Pre-start countdown | 3-2-1 before the first interval, to get into position |
| 💾 Presets | Save, name, load, and delete configurations, separately per mode |
| 📊 Progress & summary | Overall progress bar during, and a recap on completion |
| 🛡 Exit confirmation | Back gesture or STOP asks before ending a running session |
| ⬆️ In-app updates | Checks GitHub Releases and installs updates without a store |
| 🖥 Workout-first UI | Large high-contrast display, screen stays on, one-hand friendly |

---

## Install

Download the APK from the [latest release](https://github.com/daniel-kindl/ocho/releases/latest).

Ocho is not on Google Play, so Android will ask you to allow installing unknown
apps. Once installed it updates itself: Settings → Check for updates.

### Update channels

| Channel | Installs as | Source | Published |
|---------|-------------|--------|-----------|
| **Stable** | `Ocho` | `releases/latest` | On each tagged release from `main` |
| **Dev** | `Ocho Dev` | Newest prerelease | On every push to `dev` |

The two are separate apps with separate `applicationId`s and separate data, so a dev
build can be installed alongside the stable one and neither will offer the other's
updates. Dev builds are for testing changes before they reach `main` — expect them
to break.

---

## Build

| Tool | Version |
|------|---------|
| Android Studio | Hedgehog 2023.1+ |
| JDK | 17 |
| Min Android SDK | 26 (Android 8.0) |
| Target SDK | 34 |

```bash
./gradlew check                            # tests + detekt + lint
./gradlew assembleDebug                    # debug APK
./gradlew assembleDev -PdevBuildNumber=1   # dev-channel APK
./gradlew assembleRelease                  # signed release APK
```

Warnings fail the build — Kotlin, detekt, and Android Lint all run with
warnings-as-errors, and every public declaration in `src/main` must have KDoc.

### Release signing

Create `keystore.properties` in the project root (gitignored):

```properties
storeFile=release.keystore
storePassword=yourStorePassword
keyAlias=yourKeyAlias
keyPassword=yourKeyPassword
```

CI uses the environment variables `KEYSTORE_FILE`, `KEYSTORE_PASSWORD`,
`KEY_ALIAS`, and `KEY_PASSWORD` instead.

---

## Usage

**EMOM** — set total duration and interval, tap START. The app beeps and vibrates at
every interval boundary. PAUSE freezes without drift; STOP ends early after
confirming.

**Tabata** — set total, work, and rest. Phases alternate automatically with distinct
high and low beeps, and the whole background switches red for work, green for rest.

**Presets** — tap Save in the Presets row to store the current configuration. The
name is pre-filled from the durations; edit it or accept it. Tap a chip to load,
✕ to delete.

**Settings** — the gear icon on the home screen toggles sound and vibration
independently, and holds the update checker.

---

## Architecture

Clean Architecture with MVVM. The domain layer contains no `android.*` imports,
which is what makes the timing logic testable without an emulator.

```
app/src/main/kotlin/dev/danielkindl/ocho/
├── core/               Clock (injectable, for deterministic tests), duration formatting
├── domain/
│   ├── model/          TimerConfig, TabataConfig, events, presets, SemVer, UpdateConfig
│   ├── engine/         AbstractPausableEngine, TimerEngine + impl, TabataEngine + impl,
│   │                   and a factory each so engines are scoped to their view model
│   └── repository/     Repository interfaces
├── data/
│   ├── audio/          ToneAudioPlayer (ToneGenerator on STREAM_ALARM)
│   ├── vibration/      VibrationManager
│   ├── feedback/       FeedbackTrigger — settings-gated sound + vibration, shared by both modes
│   ├── repository/     DataStore implementations over a shared JsonListDataStore
│   └── update/         The only network code: GitHub Releases API, DownloadManager,
│                       PackageInstaller
├── di/                 AppModule — Hilt bindings, and the only reader of BuildConfig
└── ui/
    ├── navigation/     AppNavigation
    ├── home/           Mode selection
    ├── setup/          EMOM setup + ViewModel
    ├── session/        EMOM session + ViewModel
    ├── tabata/         Tabata setup and session, each with a ViewModel
    ├── settings/       Settings + update flow ViewModels
    ├── components/     WheelPicker, DurationPicker, PresetsSection, session scaffolding
    └── theme/          Colour ramp, three-typeface system, Material 3 scale
```

### The phase colour system

The session screen's background is the primary information channel, not decoration.
Four states, each owning one full-bleed plate — prepare amber, work red, rest light
green, complete violet — resolved in `ui/theme/PhaseColors.kt`.

Rest is a **light** plate on purpose. Red and mid-green sit at nearly the same
lightness, so under deuteranopia they collapse into two similar mid-tone plates and
the signal fails. Separating them by lightness as well as hue keeps them distinct
with no colour vision at all, and flips the on-plate text from white to ink as a
second, redundant cue.

Phase is never carried by colour alone: the plate, the uppercase label, the audio
cue, and the haptic all say the same thing. Material You dynamic colour is
deliberately disabled — work must be red on every device.

The full specification lives in `ocho-design-system/`.

### Drift-free timing

Interval boundaries are absolute timestamps computed from the session start —
`startTime + N × intervalMillis` — never a sum of `delay()` calls. Each loop
iteration recalculates from the real clock, so a late or missed tick self-corrects
instead of compounding. Over a 20-minute workout the difference is the gap between
finishing on the minute and finishing several seconds late.

Pause works by accumulating total paused time and subtracting it:

```
effectiveElapsed = now - startTime - totalPausedMs
```

This keeps the anchoring intact across any number of pauses.

---

## Contributing

See [CONTRIBUTING.md](CONTRIBUTING.md) for branch rules, commit conventions, and the
release process. Security policy: [SECURITY.md](SECURITY.md).

Note the [contributor terms](CONTRIBUTING.md#contributor-terms): contributions are
accepted under GPL-3.0 plus a licence grant that lets the project be dual-licensed
later. You keep your copyright.

---

## License

Ocho is free software under the **[GNU General Public License v3.0](LICENSE)**. You
may use, study, modify and redistribute it; anything you redistribute must also be
GPL-3.0 with source available.

Copyright © 2026 Daniel Kindl, sole copyright holder. Ocho may also be offered under
separate commercial terms.

**The name "Ocho", the wordmark, and the numeral-8 icon are not covered by the GPL.**
Fork freely — but rename and re-brand.

Bundled fonts (IBM Plex Sans, JetBrains Mono, Space Grotesk — all SIL OFL 1.1) and
icons (Lucide, ISC) keep their own licences. Full texts are in
[THIRD-PARTY-NOTICES.md](THIRD-PARTY-NOTICES.md) and in the app under
**Settings → Licences**.
