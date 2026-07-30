# Changelog

All notable changes to DK Timer are documented here.

Format follows [Keep a Changelog](https://keepachangelog.com/en/1.0.0/).
Versioning follows [Semantic Versioning](https://semver.org/).

---

## [Unreleased]

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

[Unreleased]: https://github.com/daniel-kindl/emom-timer/compare/v2.0.0...HEAD
[2.0.0]: https://github.com/daniel-kindl/emom-timer/compare/v1.0.0...v2.0.0
[1.0.0]: https://github.com/daniel-kindl/emom-timer/releases/tag/v1.0.0
