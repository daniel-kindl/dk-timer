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

## Screenshots

<p align="center">
  <img src="docs/screenshots/home.png" width="24%" alt="Home screen with cards for EMOM, Tabata and AMRAP.">
  <img src="docs/screenshots/setup.png" width="24%" alt="Tabata setup: pickers at 4:00 total, 20 seconds work and 10 seconds rest, above a bar previewing 8 rounds as alternating red and green blocks.">
  <img src="docs/screenshots/work.png" width="24%" alt="Work phase: a full-screen red plate reading 0:13, round 3 of 8.">
  <img src="docs/screenshots/rest.png" width="24%" alt="Rest phase: a full-screen green plate reading 0:08, round 3 of 8.">
</p>

Choosing a mode, a Tabata set to the classic 4:00 of 20 seconds work and 10 seconds
rest, then the work and rest plates mid-session. The bar above Start previews the
shape of the workout before it begins.

While a session runs the plate is the entire interface, which is what makes it
readable across a room. Work and rest are separated by lightness as well as hue, so
they stay distinct with no colour vision at all. The same two screens again, with
every trace of colour removed:

<p align="center">
  <img src="docs/screenshots/colour-vision.png" width="88%" alt="The work and rest screens side by side in greyscale. The work plate is clearly darker than the rest plate, and its text is white where the rest plate's text is black.">
</p>

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
| No ads, no tracking | No accounts, no backend, no analytics, no telemetry; one network request, and it's the update check |

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

## Privacy

Ocho collects nothing. No ads, no accounts, no backend, no analytics, no telemetry, no
advertising ID. Your presets and settings live in the app's private storage and are
deleted when you uninstall it.

The app makes exactly one kind of network request: it asks `api.github.com` whether a
newer release exists, and downloads the APK from `objects.githubusercontent.com` if you
tap Update. No request body, no identifier, no cookie, no account.

The one thing that can't be promised away is that GitHub, as host, sees the IP address
of that request the way any website does. Full detail, including every permission and
why it exists, is in [PRIVACY.md](PRIVACY.md).

Usage is estimated from GitHub's public download counts rather than from anything on
your device; how that works, and what it deliberately cannot tell us, is in
[docs/METRICS.md](docs/METRICS.md).

---

## Building from source

Build requirements, the Gradle commands, release signing, the package layout, and the
reasoning behind the timing, session, and colour design are in
[docs/ARCHITECTURE.md](docs/ARCHITECTURE.md).

---

## Contributing

See [CONTRIBUTING.md](CONTRIBUTING.md) for branch rules, commit conventions, the
release process, and the [contributor terms](CONTRIBUTING.md#contributor-terms).
Security policy: [SECURITY.md](SECURITY.md). Privacy policy: [PRIVACY.md](PRIVACY.md).

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
