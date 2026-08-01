# CLAUDE.md

Guidance for AI agents working in this repository.

## What this is

**Ocho** is a native Android interval timer (Kotlin, Compose, Hilt) with two modes:
EMOM and Tabata. Read `README.md` for the feature list and usage, and
`docs/ARCHITECTURE.md` for the package layout, session ownership, and the drift-free
timing explanation; neither is repeated here.

Package root is `dev.danielkindl.ocho`. The app was previously EMOM Timer, then DK
Timer. If you find those names anywhere, they are stale.

## Commands

```bash
./gradlew check                        # tests + detekt + lint. Run before calling anything done.
./gradlew assembleDebug                # debug APK
./gradlew assembleDev -PdevBuildNumber=1   # dev-channel APK
./gradlew assembleRelease              # signed; needs keystore.properties
```

If Gradle reports an invalid `JAVA_HOME`, point it at the installed JDK 17. The
path goes stale across JDK patch updates.

## Rules

**Warnings are errors.** Kotlin, detekt, and Android Lint all fail on warnings.
Three lint checks are disabled in `app/build.gradle.kts` because they report on the
environment, not the code; don't add to that list to make a build pass.

**Everything public in `src/main` needs KDoc**, enforced by detekt. Write *why*,
not *what*: the code already says what. `TimerEngineImpl`'s header is the standard.
Tests are exempt; their names already state intent.

**`domain/` must not import `android.*`.** `Clock` in `core/` exists so engine logic
is testable without Android. `BuildConfig` is read in exactly one place,
`di/AppModule.kt`, which converts it into plain values.

**No business logic in composables.** It belongs in the ViewModel or domain layer.

**The engines are drift-free.** They compute elapsed time as
`startTime + N × interval` and track `totalPausedMs` separately, never accumulating
per-tick deltas. Preserve this in `domain/engine/`.

**Never read, print, or commit** `keystore.properties`, `release.keystore`, or
`local.properties`. They hold real signing secrets and are gitignored.

## Layout

`core/` clock and formatting · `domain/` model, engines, repository interfaces ·
`data/` DataStore repos, audio, vibration, `session/` (foreground service), `update/`
(the only network code) · `ui/` screens and ViewModels · `di/` Hilt bindings.

## Parallelising with subagents

You may dispatch subagents freely, and they may dispatch their own. Independent work
should run in parallel rather than in sequence.

**One hard constraint: only one agent runs Gradle.** The daemon takes an exclusive
lock, so concurrent builds either block or fail confusingly. Tell every subagent not
to run Gradle, keep verification on your own thread, and run `./gradlew check` once
after their edits land.

Good candidates, because they touch disjoint files and need no build:

- Documentation (`README.md`, `docs/`, `CONTRIBUTING.md`, `SECURITY.md`)
- CI workflow YAML, which validates with `py -c "import yaml; ..."`
- Codebase exploration, where you want a conclusion rather than the file contents

Poor candidates: anything editing the same file as another agent, and anything whose
correctness only shows up in a build.

Subagents start cold, so give each one the file paths, the constraints it must not
break, and the repo's writing rules. Assume it knows nothing about this conversation.
A subagent that has to rediscover context costs more than doing the work yourself.

## Update channels

Two, mutually invisible. **Stable** reads `releases/latest`, which GitHub defines as
excluding prereleases. **Dev** installs as `dev.danielkindl.ocho.dev` alongside the
stable app and reads prereleases only. Every push to `dev` publishes one
automatically.

Dev tags contain a hyphen (`v3.1.0-dev.12`); `release.yml` skips those refs and
excludes them from its `git describe` calls. Don't remove either guard.

## Commits and releases

[Conventional Commits](https://www.conventionalcommits.org/en/v1.0.0/), enforced by
`.githooks/commit-msg` (enable with `git config core.hooksPath .githooks`) and by
`commit-lint.yml`. Types: `feat fix build chore ci docs style refactor perf test
revert`.

[SemVer](https://semver.org/). `release.yml` fails the build if the tag's bump level
is smaller than the commits since the last tag require. A `feat:` needs at least a
minor. Use `!` only when a release costs something on the device: a reinstall, or
wiped presets.

All work branches off `dev`. `main` advances only via release PR. See
`CONTRIBUTING.md` for the release checklist.

## Testing

`domain/engine`, `domain/model`, `data/repository`, `data/update`, and the update
ViewModel have unit tests. Composables are thin by design and untested.

**No emulator is available here.** Verify UI changes by reading the diff and
reasoning about recomposition, not by running the app.
