# Privacy policy

Ocho is a workout timer. It has no accounts, no backend, no analytics, and no
telemetry. It does not show ads, and there is nothing in it that could show one.

This document says exactly what that means, including the one thing the app cannot
promise away.

## What Ocho collects

Nothing. There is no analytics SDK, no crash reporter, no event tracking, no
advertising ID, no device fingerprint, and no server of ours for anything to be sent
to. No data about you or your workouts leaves your device.

The maintainer cannot see how you use the app, when you use it, or whether you use it
at all. Usage is estimated from GitHub's public release download counts, the same
numbers anyone can read; how and why is in [docs/METRICS.md](docs/METRICS.md).

## What Ocho stores, and where

On your device only, in Android's `DataStore`, in the app's private storage:

- Your saved workout presets: a name and the durations you chose.
- Three switches: sound, vibration, and countdown beeps.

That is the complete list. It is never uploaded, never backed up to anything of ours,
and it is deleted when you uninstall the app.

## Network requests

Ocho makes exactly one kind of request, and only to check for and download its own
updates:

| Host | Why |
|------|-----|
| `api.github.com` | Reads the latest release for `daniel-kindl/ocho` to see whether a newer version exists |
| `objects.githubusercontent.com` | Downloads the release APK, but only after you tap Update |

The check runs once when the app starts and carries no request body, no identifier, no
cookie, and no account. It sends only a `User-Agent` of `ocho-android`. If it fails, it
fails silently and the app carries on.

**The honest caveat.** GitHub is the host, so like any website it can see the IP address
your request comes from, along with the timing and the `User-Agent`. That is inherent to
fetching anything over the internet, and it is GitHub's data, not ours — we never see
it. If you would rather not make the request at all, don't tap Check for updates and
install new versions manually from the
[releases page](https://github.com/daniel-kindl/ocho/releases); the launch-time check
is the only automatic one.

## Permissions, and why each exists

| Permission | Why |
|------------|-----|
| `INTERNET` | The update check and download above |
| `REQUEST_INSTALL_PACKAGES` | Installing a downloaded update, which Android also prompts you to allow |
| `VIBRATE` | Haptic feedback at interval boundaries |
| `FOREGROUND_SERVICE`, `FOREGROUND_SERVICE_SPECIAL_USE` | Keeping a running session alive when you leave the app |
| `WAKE_LOCK` | Keeping the CPU awake so a running timer stays accurate while the screen is off |
| `POST_NOTIFICATIONS` | The session notification with its pause, resume and stop controls |

There is no location, camera, microphone, contacts, storage, or network-state
permission, and no permission that identifies your device.

## Third parties

None. Ocho bundles no advertising, attribution, or analytics library. Its only
dependencies are AndroidX, Compose, Hilt and Kotlin coroutines, none of which phone
home. The full dependency list is in `gradle/libs.versions.toml`, and the bundled fonts
and icons are catalogued in [THIRD-PARTY-NOTICES.md](THIRD-PARTY-NOTICES.md).

Ocho is not distributed through Google Play, so no store SDK is present either.

## Children

Ocho collects no data from anyone, of any age.

## Changes

Ocho is GPL-3.0 and its source is public, so this document is verifiable rather than
merely promised — the network code is one file, `data/update/UpdateRepositoryImpl.kt`.
If the app's data behaviour ever changes, this file changes with it in the same commit,
and the change appears in [CHANGELOG.md](CHANGELOG.md).

Questions, or something here that does not match the code: open an issue. Security
reports go through the process in [SECURITY.md](SECURITY.md).
