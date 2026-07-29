# CLAUDE.md

Guidance for Claude Code (and other AI agents) working in this repository.

## What this is

DK Timer is a native Android workout interval timer (Kotlin + Jetpack
Compose + Hilt), with two modes: **EMOM** (fixed-interval beeps over a
total duration) and **Tabata** (alternating work/rest phases). See
`README.md` for the full feature list, architecture diagram, and
drift-free timing explanation — don't duplicate that here, read it first
for context on `domain/engine`.

## Commands

```bash
./gradlew testDebugUnitTest   # unit tests (domain/engine, no Android deps)
./gradlew detekt              # static analysis, enforced in CI
./gradlew assembleDebug       # debug APK
./gradlew assembleRelease     # signed release APK (needs signing secrets/keystore.properties)
```

Run `testDebugUnitTest` and `detekt` before considering any change done.

## Architecture rules (enforced by convention, not tooling — respect them)

- Clean Architecture, MVVM. Packages: `domain/` (model, engine, repository
  interfaces), `data/` (Android-facing implementations: DataStore repos,
  `ToneAudioPlayer`, `VibrationManager`), `ui/` (Compose screens +
  ViewModels), `di/AppModule.kt` (Hilt bindings).
- **Domain layer must stay Android-free** (pure Kotlin, no `android.*`
  imports). `Clock`/`SystemClock` in `core/` exists specifically so engine
  logic is deterministic and testable without Android.
- No business logic in `ui/` composables — that belongs in the ViewModel or
  domain layer.
- Timer/Tabata engines are drift-free: they compute elapsed time from
  `startTime + N × intervalMillis` rather than accumulating per-tick deltas,
  and track `totalPausedMs` separately. Preserve this if touching
  `domain/engine/*`.

## Commit & release rules — these are enforced, not optional

- Every commit message must follow [Conventional Commits 1.0.0](https://www.conventionalcommits.org/en/v1.0.0/):
  `type(scope)!: description`. Allowed types: `feat fix build chore ci docs
  style refactor perf test revert`. A local hook (`.githooks/commit-msg`,
  enable via `git config core.hooksPath .githooks`) and
  `.github/workflows/commit-lint.yml` both reject non-conforming commits.
- Releases follow [SemVer 2.0.0](https://semver.org/). `.github/workflows/release.yml`
  validates on tag push that the tag is valid SemVer, strictly newer than
  the last tag, matches `versionName` in `app/build.gradle.kts`, and that
  the major/minor/patch bump matches what the commits since the last tag
  require (breaking → major, `feat:` → minor, else patch). See
  `CONTRIBUTING.md` for the full release checklist.
- Branch strategy: all work branches off `dev`; `main` only advances via
  PR from `dev` at release time. Delete feature branches after merge.

## Testing notes

- Only `domain/engine` has unit tests (`TimerEngineTest.kt`,
  `TabataEngineTest.kt`) — this is intentional, since that's the only layer
  with real logic to test; ViewModels and Compose UI are thin by design.
- This environment cannot launch an Android emulator, so UI changes can be
  verified by reading the composable diff and reasoning about
  recomposition/lifecycle, not by running the app.
