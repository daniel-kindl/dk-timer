# Changelog

All notable changes to Ocho are documented here.

Format follows [Keep a Changelog](https://keepachangelog.com/en/1.0.0/).
Versioning follows [Semantic Versioning](https://semver.org/).

---

## [Unreleased]

---

## [3.1.0] - 2026-08-01

### Added
- **Workouts now survive a locked screen and a backgrounded app.** This was the one
  failure that actually ruined a session. Previously the timer only kept time while
  the app was in the foreground: press the power button or switch to a music app,
  and the clock silently fell behind while interval beeps went missing.

  Two separate things were wrong, and both had to be fixed. A foreground service
  stops Android freezing or killing the process. A partial wake lock stops the CPU
  sleeping, which is what stalled the engine's timing between beeps. A foreground
  service alone does not prevent that, which is why this is one release rather than
  two.
- **An ongoing notification** showing the phase, round and remaining time, with
  pause, resume and stop controls, so a session can be driven from the lock screen
  without reopening the app. Tapping it returns you to the running workout.
- **Countdown beeps**: the last three seconds before each interval or phase change
  tick down, using a shorter, quieter tone than the boundary itself. They have their
  own setting, since opinion on them divides sharply, and the sound switch silences
  them along with everything else.
- **Music now ducks for cues** instead of drowning them. Audio focus is held across
  a burst, so a three-second lead-in and the beep that follows read as one dip
  rather than four, and music returns between intervals.
- The dev build's launcher icon is amber, so it is distinguishable at a glance from
  the stable app sitting next to it.

### Changed
- A session no longer belongs to the screen that started it. It lives in a singleton
  on its own scope, which is what allows it to outlive the screen at all, and the
  session view models became observers.
- Toolchain upgraded: AGP 9.3.1, Gradle 9.6.1, Kotlin 2.4.10, Hilt 2.60.1, Compose
  BOM 2026.06.01, compileSdk 37, targetSdk 36. These were not separable. Hilt 2.60.1
  refuses to apply below AGP 9, and the newer AndroidX libraries then require a
  compile SDK that AGP 8 could not provide.
- Release and prerelease downloads now carry a SHA-256 checksum and a GitHub build
  provenance attestation. `SECURITY.md` explains what each does and does not prove.
- Documentation split by audience: `README.md` is for installing and using Ocho,
  with build instructions and internals moved to `docs/ARCHITECTURE.md`.

### Fixed
- Notification permission is requested when a session starts rather than at launch,
  and denying it costs only the notification. Timing stays exact.
- An unsafe cast of the Compose context to an `Activity`, replaced with
  `LocalActivity`.

---

## [3.0.0] - 2026-08-01

### Breaking: you must reinstall

The app is now **Ocho**, and its `applicationId` changed from `com.emomtimer` to
`dev.danielkindl.ocho`. Android treats that as a different app, so:

- **This build installs alongside DK Timer rather than upgrading it.** You will
  briefly have two icons.
- **Saved presets and settings do not carry over.** Note anything you want to keep
  before switching; there is no migration.
- Uninstall DK Timer once Ocho is working. The old install will not receive further
  updates.

This is the last time a rename will force a reinstall: the new `applicationId` is
derived from a domain rather than the product name, so future renames are cosmetic.

### Added
- **A new visual system, built around the session screen's background colour.** The
  timer has four states and each owns one full-bleed plate: prepare amber, work red,
  rest light green, complete violet. The colour answers "what am I doing right now"
  from across a room, before any text is read.
- **Work and rest now differ by lightness, not just hue.** Red and mid-green sit at
  nearly the same lightness, so under deuteranopia they converged into two similar
  plates and the app's main signal failed for roughly 8% of men. Rest moved to a
  light plate, which also flips the text from white to ink as a second, redundant
  cue. Across a workout this reads as a light–dark–light–dark rhythm that is
  catchable in peripheral vision.
- **Run timeline** on both setup screens: a proportional preview of the configured
  workout in the same phase colours the session will use, so its shape is visible
  before starting.
- **Dev update channel.** Every push to `dev` publishes a prerelease that installs
  as *Ocho Dev*, alongside the stable app and with its own data. Lets changes be
  tested on a real device before they reach `main`. The channels cannot see each
  other: stable reads `releases/latest`, which excludes prereleases by definition.
- `SemVer` now parses and orders prerelease versions per SemVer 2.0.0 §11, and
  discards build metadata per §10.
- Dependabot for Gradle and GitHub Actions, and a security policy documenting the
  APK self-update flow.
- **A licence.** Ocho is now GPL-3.0. It previously had none, which under copyright
  law meant all rights reserved by default, so nobody could legally build it and
  nothing stated whether that was deliberate. Daniel Kindl remains sole copyright
  holder; the name, wordmark and numeral-8 icon are excluded from the GPL grant.
- **Third-party licence notices**, in `THIRD-PARTY-NOTICES.md` and readable in the
  app under Settings, then Licences. This closes an obligation the app was not
  meeting: the APK embeds three SIL OFL 1.1 fonts and Lucide's ISC icons, and both
  licences require their notices to accompany every copy. The XML comments
  crediting Lucide did not count, since AAPT compiles vector XML to binary and
  strips them.
- Contributor terms in `CONTRIBUTING.md`. Contributions are accepted under GPL-3.0
  plus a licence grant permitting relicensing, so that a merged pull request cannot
  permanently foreclose commercial licensing.

### Fixed
- **In-app updates were broken in 2.3.0.** The app polled `daniel-kindl/dk-timer`,
  which does not exist, so every check returned a 404. The repository is now read
  from `BuildConfig` and cannot drift from the real one again.
- `ApkInstaller` called an API 31 method from a helper whose version guard lived in
  its caller. The code was already safe; the contract is now declared.
- `UpdateViewModel` held a `Context` only to read its own version name.
- `SessionProgressBar` defaulted its modifier to `Modifier.fillMaxWidth()`, which
  any caller-supplied modifier would have silently discarded.
- Release notes linked to the wrong comparison range, and releases were still named
  "EMOM Timer".

### Changed
- Renamed throughout: display name, `applicationId`, Kotlin package, repository,
  and every stale "EMOM Timer" / "DK Timer" string.
- **The launcher icon is now the numeral 8**, set in type on brand green, replacing
  the stopwatch.
- The clock is set in Space Grotesk at 76sp with tabular figures, so the digits stop
  shifting width as it counts down.
- All icons come from Lucide. The app previously mixed a filled icon set with the
  new stroked one, which the design system explicitly forbids.
- Copy follows the new voice: sentence case, buttons as verbs, no emoji, and empty
  states that describe the trigger — "Presets appear here after you save a workout"
  rather than "no presets".
- The build now fails on warnings — Kotlin, detekt, and Android Lint. Three lint
  checks that report on the environment rather than the code are excluded.
- Every public declaration in `src/main` requires KDoc, enforced by detekt. 265
  were added, recording why decisions were made rather than restating the code.

---

## [2.3.0] - 2026-07-30

### Changed
- **DK Timer brand design**: adopted the new visual identity across the app —
  a stopwatch launcher icon on brand green, a green/graphite-neutral/red
  color palette driving the light and dark themes, and a three-typeface
  system (Space Grotesk for the dominant countdown numeral, JetBrains Mono
  for computed values like round counts and elapsed time, IBM Plex Sans for
  general UI text)
- Tabata's phase-transition color swap now uses the brand palette, runs on a
  340ms ease-in-out curve, and respects the system's reduced-motion setting
- Preset empty state now reads "Save a configuration above to see it here."

### Internal
- Added test coverage for the highest-value gaps: engine pause/resume and
  degenerate-duration handling, setup UI state, preset persistence, and the
  entire update-flow state machine
- Removed ~300+ duplicated lines between EMOM and Tabata via shared helpers
  (`DurationFormat`, `JsonListDataStore`, `core/format/SessionFormatting`) and
  shared composables (`ExitConfirmDialog`, `SessionProgressBar`, preset
  save/delete dialogs, `SessionLifecycleScaffold`)
- Removed the remaining `@Suppress("DEPRECATION")` sites for the old
  `LinearProgressIndicator` overload

---

## [2.2.0] - 2026-07-30

### Added
- **Pre-start countdown**: EMOM and Tabata sessions now count down 3-2-1
  before the first interval begins, giving you time to get into position
- **Progress bar**: both session screens now show an overall progress bar
  alongside the elapsed time
- **Tabata round counter**: the Tabata session screen now shows "ROUND X / Y",
  matching EMOM
- **Exit confirmation**: leaving an in-progress session (back gesture or the
  STOP button) now asks for confirmation instead of exiting immediately
- **Completion summary**: finishing a workout now shows a "Workout Complete!"
  recap with total time instead of navigating away instantly

---

## [2.1.0] - 2026-07-30

### Added
- **In-app updates**: the app now checks GitHub for new releases on startup and
  surfaces available updates in the Settings screen with release notes
- Downloads updates via Android's `DownloadManager` with progress tracking, then
  installs via `PackageInstaller` (Android 12+) or an intent-based fallback on
  older versions, handling the "install unknown apps" permission flow as needed

---

## [2.0.1] - 2026-07-29

### Fixed
- **Audio**: synchronized `ToneAudioPlayer`'s `ToneGenerator` lifecycle so concurrent
  release/recreate from independent session ViewModels can no longer race
- **Session**: scoped the keep-screen-on flag to active EMOM/Tabata session screens
  instead of the whole app, removing unnecessary battery drain on Home/Setup/Settings
- **Tabata engine**: restructured the main timing loop to resolve a detekt readability
  finding, with no change to the drift-free timing behavior

### Refactored
- Extracted a shared `FeedbackTrigger` used by both `SessionViewModel` and
  `TabataSessionViewModel`, eliminating duplicated settings-gated sound/vibration logic

### CI
- Conventional Commits are now enforced via a local hook and a PR check
- The release workflow validates SemVer tag/version consistency and builds a signed
  release APK instead of a debug build

---

## [2.0.0] - 2026-05-06

### Added
- **Tabata timer**: configure total duration, work interval, and rest interval; automatic
  work/rest phase alternation with distinct high/low audio beeps per phase
- **Full-screen phase backgrounds**: animated red (work) / green (rest) background in
  Tabata session; colours dim when paused
- **Tabata presets**: save, name, load, and delete Tabata configurations, mirroring
  the EMOM preset system
- **HomeScreen**: new app entry point with EMOM and Tabata timer cards; settings ⚙
  icon moved here from the setup screen
- **Drum-roll wheel pickers**: replaced +/− steppers with infinite-scroll snap pickers
  for all mm:ss duration fields
- **Shared `PresetsSection` component**: generic chip row used by both EMOM and Tabata
  setup screens, eliminating duplicate code
- **11 unit tests** covering Tabata engine accuracy, phase transitions, pause/resume,
  and edge cases

### Changed
- App renamed from **EMOM Timer** to **DK Timer**
- EMOM setup screen: Settings icon replaced with a back-arrow; title shortened to "EMOM"
- Both setup screens now fit within the visible viewport (no scrolling required)
- Settings descriptions updated to say "each timer event" (applies to both timers)
- Complete Material 3 typography scale defined (all 15 slots); explicit weights throughout

### Refactored
- Extracted `AbstractPausableEngine` base class — shared by `TimerEngineImpl` and
  `TabataEngineImpl`, eliminating duplicated pause/resume logic
- `SessionStatus` moved from `SessionViewModel` to `domain/model/` so both timer
  view models can reference it without UI coupling

---

## [1.0.0] - 2026-05-05

### Added
- Initial project setup with MVVM + Clean Architecture
- Drift-free timer engine based on system clock anchoring
- Setup screen with mm:ss duration pickers
- Active session screen with round counter and countdown
- Settings screen with sound and vibration toggles
- ToneGenerator audio using STREAM_ALARM (ignores silent mode)
- Vibration feedback on intervals and workout completion
- FLAG_KEEP_SCREEN_ON during sessions
- GitHub Actions CI/CD pipeline (dev CI + tagged release APK)
- Unit tests covering timer accuracy and edge cases
- Preset system: save, name, load, and delete workout configurations
- Pause/resume support with drift-free accuracy preserved across pauses
- App info section in Settings (version, author with website link)

---

[Unreleased]: https://github.com/daniel-kindl/ocho/compare/v3.1.0...HEAD
[3.1.0]: https://github.com/daniel-kindl/ocho/compare/v3.0.0...v3.1.0
[3.0.0]: https://github.com/daniel-kindl/ocho/compare/v2.3.0...v3.0.0
[2.3.0]: https://github.com/daniel-kindl/ocho/compare/v2.2.0...v2.3.0
[2.2.0]: https://github.com/daniel-kindl/ocho/compare/v2.1.0...v2.2.0
[2.1.0]: https://github.com/daniel-kindl/ocho/compare/v2.0.1...v2.1.0
[2.0.1]: https://github.com/daniel-kindl/ocho/compare/v2.0.0...v2.0.1
[2.0.0]: https://github.com/daniel-kindl/ocho/compare/v1.0.0...v2.0.0
[1.0.0]: https://github.com/daniel-kindl/ocho/releases/tag/v1.0.0
