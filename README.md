# Ocho

A minimal, production-quality Android workout interval timer, built to stay readable
when you're mid-effort and not looking at the screen.

[![Dev CI](https://github.com/daniel-kindl/ocho/actions/workflows/dev-ci.yml/badge.svg?branch=dev)](https://github.com/daniel-kindl/ocho/actions/workflows/dev-ci.yml)
[![Release](https://github.com/daniel-kindl/ocho/actions/workflows/release.yml/badge.svg)](https://github.com/daniel-kindl/ocho/actions/workflows/release.yml)
[![Latest release](https://img.shields.io/github/v/release/daniel-kindl/ocho?label=latest)](https://github.com/daniel-kindl/ocho/releases/latest)
[![License: GPL-3.0](https://img.shields.io/badge/license-GPL--3.0-blue)](LICENSE)
[![API 26+](https://img.shields.io/badge/API-26%2B-brightgreen)](https://developer.android.com/)
[![Kotlin](https://img.shields.io/badge/Kotlin-2.4.10-7F52FF?logo=kotlin&logoColor=white)](https://kotlinlang.org)

*An **ocho** is a figure-eight step in tango. It's also Spanish for **eight**, the
round count of a classic Tabata.*

---

## Features

| Feature | Detail |
|---------|--------|
| EMOM timer | Total duration and interval length, set with drum-roll mm:ss pickers |
| Tabata timer | Total, work, and rest durations; phases alternate automatically |
| AMRAP timer | Total duration only; one unbroken block with no interval beeps |
| Phase colours | A full-screen amber, red, green, or violet plate per phase, readable across a room and distinguishable without colour vision |
| Run timeline | Proportional preview of a workout's shape before you start it |
| Sound feedback | Distinct tones per event, on the alarm stream so silent mode can't mute them |
| Vibration feedback | Different patterns for intervals and for completion |
| Pause and resume | Freeze mid-session without drift or losing interval alignment |
| Pre-start countdown | Three seconds before the first interval, to get into position |
| Presets | Save, name, load, and delete configurations, separately per mode |
| Progress and summary | A progress bar during the session, and a recap on completion |
| Exit confirmation | The back gesture and Stop both ask before ending a running session |
| In-app updates | Checks GitHub Releases and installs updates without a store |
| Workout-first UI | Large high-contrast display, screen stays on, one-hand friendly |

---

## Install

Download the APK from the [latest release](https://github.com/daniel-kindl/ocho/releases/latest).

Ocho is not on Google Play, so Android will ask you to allow installing unknown
apps. Once installed it updates itself: Settings, then Check for updates.

### Update channels

| Channel | Installs as | Source | Published |
|---------|-------------|--------|-----------|
| Stable | `Ocho` | `releases/latest` | On each tagged release from `main` |
| Dev | `Ocho Dev` | Newest prerelease | On every push to `dev` |

The two are separate apps with separate `applicationId`s and separate data, so a dev
build can be installed alongside the stable one and neither will offer the other's
updates. Dev builds exist to test changes before they reach `main`, so expect them
to break.

---

## Usage

**EMOM.** Set total duration and interval, then Start. The app beeps and vibrates at
every interval boundary. Pause freezes without drift; Stop ends early after
confirming.

**Tabata.** Set total, work, and rest. Phases alternate automatically with distinct
high and low beeps, and the whole screen switches between a dark red work plate and
a light green rest one.

**AMRAP.** Set total duration and go. Nothing interrupts you: no interval beeps, no
round counter, just the clock counting down and a 3-2-1 before it stops. Count your
own rounds.

**Presets.** Tap Save in the Presets row to store the current configuration. The
name is pre-filled from the durations, so edit it or accept it. Tap a chip to load,
or the delete control to remove it.

**Settings.** The icon on the home screen toggles sound and vibration
independently, and holds the update checker and the licence notices.

---

## Building from source

Build requirements, the Gradle commands, release signing, the package layout, and the
reasoning behind the timing, session, and colour design are in
[docs/ARCHITECTURE.md](docs/ARCHITECTURE.md).

---

## Contributing

See [CONTRIBUTING.md](CONTRIBUTING.md) for branch rules, commit conventions, the
release process, and the [contributor terms](CONTRIBUTING.md#contributor-terms).
Security policy: [SECURITY.md](SECURITY.md).

---

## License

Ocho is free software under the [GNU General Public License v3.0](LICENSE). You
may use, study, modify and redistribute it. Anything you redistribute must also be
GPL-3.0 with source available.

Copyright © 2026 Daniel Kindl, sole copyright holder. Ocho may also be offered under
separate commercial terms.

The name "Ocho", the wordmark, and the numeral-8 icon are not covered by the GPL.
Fork freely, but rename and re-brand.

Bundled fonts (IBM Plex Sans, JetBrains Mono, and Space Grotesk, all SIL OFL 1.1)
and icons (Lucide, ISC) keep their own licences. Full texts are in
[THIRD-PARTY-NOTICES.md](THIRD-PARTY-NOTICES.md) and in the app under Settings,
then Licences.
