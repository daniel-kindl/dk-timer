# Architecture

Developer documentation for Ocho: how to build it, how the code is laid out, and why
the parts that look unusual are the way they are. For installing and using the app,
see the [README](../README.md).

---

## Build requirements

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

Warnings fail the build. Kotlin, detekt, and Android Lint all run with
warnings-as-errors, and every public declaration in `src/main` must have KDoc.

---

## Release signing

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

## Package layout

Clean Architecture with MVVM. The domain layer contains no `android.*` imports,
which is what makes the timing logic testable without an emulator.

```
app/src/main/kotlin/dev/danielkindl/ocho/
├── core/               Clock (injectable, for deterministic tests), duration formatting
├── domain/
│   ├── model/          TimerConfig, TabataConfig, AmrapConfig, events, WorkoutPreset,
│   │                   SemVer, UpdateConfig, SessionRequest (sealed, one variant per
│   │                   mode), SessionSnapshot
│   ├── engine/         AbstractPausableEngine, TimerEngine + impl, TabataEngine + impl,
│   │                   and a factory each. WorkoutEngine is the mode-agnostic strategy
│   │                   interface over them, implemented by EmomWorkoutEngine,
│   │                   TabataWorkoutEngine and AmrapWorkoutEngine, resolved by
│   │                   WorkoutEngineFactory
│   └── repository/     Repository interfaces
├── data/
│   ├── audio/          ToneAudioPlayer (ToneGenerator on STREAM_ALARM)
│   ├── vibration/      VibrationManager
│   ├── feedback/       FeedbackTrigger, settings-gated sound and vibration used by both modes
│   ├── session/        SessionController (singleton owner of the running session),
│   │                   SessionService (foreground service + partial wake lock),
│   │                   SessionNotifications (ongoing notification and its controls),
│   │                   AndroidSessionServiceLauncher (starts and stops the service)
│   ├── repository/     DataStore implementations over a shared JsonListDataStore
│   └── update/         The only network code: GitHub Releases API, DownloadManager,
│                       PackageInstaller
├── di/                 AppModule: Hilt bindings, and the only reader of BuildConfig
└── ui/
    ├── navigation/     AppNavigation, two routes total: setup/{mode} and session/{...}
    ├── home/           Mode selection
    ├── setup/          One setup screen, state and ViewModel for every mode
    ├── session/        One session screen and ViewModel for every mode
    ├── settings/       Settings + update flow ViewModels
    ├── licenses/       Third-party licence notices
    ├── components/     WheelPicker, DurationPicker, PresetsSection, session scaffolding
    └── theme/          Colour ramp, three-typeface system, Material 3 scale
```

---

## Session architecture

A workout has to outlive the screen that started it. Rotation, backgrounding, and the
system reclaiming the activity all destroy the session screen, and none of them are a
reason to end someone's set. Three pieces exist for that.

**`SessionController` owns the session.** It is a `@Singleton` and it runs the workout
on its own `CoroutineScope`, not on a `viewModelScope`. A scope tied to the UI is
cancelled when the UI goes away, which would take the engine coroutine with it. Because
the controller is the owner, the session ViewModels are observers of its snapshot flow
rather than holders of a timer.

**`SessionService` keeps the process and the CPU alive.** It is a foreground service, so
Android will not freeze or kill the process, and it additionally holds a
`PARTIAL_WAKE_LOCK`. The second part is the one that is easy to miss: foreground status
does not stop the CPU sleeping, and the engines advance with `delay()`, which does not
fire while the device dozes. Without the wake lock, a locked screen means the clock
silently falls behind and interval cues go missing. The service owns neither the session
nor its timing, it observes the controller and posts the ongoing notification.

**`WorkoutEngine` keeps mode out of everything downstream.** It is a strategy interface
implemented by `EmomWorkoutEngine`, `TabataWorkoutEngine` and `AmrapWorkoutEngine`, so
the controller, the service, and the notification never learn which mode is running.
The single branch on mode lives in `WorkoutEngineFactory`, as a `when` over the sealed
`SessionRequest`. Sealing the request means adding a mode turns that `when` into a
compile error instead of a silent gap.

---

## One stack for every mode

Above the session layer there is one of everything: one setup screen and state, one
session screen, one preset type and one preset store. Modes differ only in which
duration fields they use, so a screen per mode meant copying validation, preset
handling and session rendering for each one. Adding AMRAP under that arrangement would
have cost roughly 300 lines of near-identical code; it cost an adapter and a card.

The mode travels as a navigation argument rather than as a destination, which is why
there are two routes rather than six:

```
setup/{mode}
session/{mode}/{total}/{first}/{second}
```

`first` and `second` carry whatever the mode needs: interval for EMOM, work and rest
for Tabata, neither for AMRAP. The names are deliberately generic, because naming them
after one mode's meaning would mislead in the other two. Configuration travelling as
route arguments is also what lets the session view model read its durations from
`SavedStateHandle` and survive process recreation with no save and restore code.

`AmrapWorkoutEngine` adds no timing at all. An AMRAP is an EMOM whose interval equals
its total: one interval, ending exactly when the workout does. Building the request
that way reuses `TimerEngineImpl` wholesale, including the 3-2-1 lead-in, which then
lands on the finish rather than on a boundary, which is where an AMRAP wants it. The
one thing it suppresses is the interval-boundary cue, since that boundary and the
finish are the same instant and would otherwise beep twice.

## One answer to "what shape is this workout?"

`SessionRequest.toPlan()` derives a `WorkoutPlan`: a list of labelled `PlannedSegment`s
plus the round count. It is the only place that structure is expressed. Five places
used to derive it independently — both engines, a second pass inside the Tabata engine
that existed purely to count rounds, the round count under the setup pickers, and the
timeline preview — each carrying a comment asserting it agreed with the others. Two of
the last three releases fixed bugs that were exactly that agreement lapsing.

Boundaries live inside a segment rather than between segments. An EMOM is one
twenty-minute work block with `boundaryEveryMillis` set, not twenty one-minute blocks:
the athlete does not stop at the beep, and the timeline draws one bar. Splitting it
would have invented a structure that exists only in the data.

The preview is the strongest case for the plan. `RunTimeline` renders the plan the
session will run rather than a reconstruction of it, so a bar that misrepresents its
own workout is no longer expressible.

Still parallel, and deliberately so: `TimerEngineImpl` and `TabataEngineImpl` remain
two timing loops rather than one. The Tabata engine now walks the plan's segments, but
the EMOM engine keeps its own boundary arithmetic, because that is the code the 3.2.1
lead-in fix landed in and it is verified on hardware. Collapsing the two loops is
deferred without a version attached; it buys nothing visible until a feature needs
segments the plan cannot already describe. Warmup and cooldown blocks, or custom
circuits with per-block labels, would be such features. Neither is planned.

The pre-start countdown is the one piece of workout shape the plan does not own.
`SessionController.runPrepareCountdown()` still runs it, and the timeline still draws
its amber lead as a segment the plan did not supply. Moving it in would change
behaviour rather than preserve it — the prepare beeps deliberately do not duck other
audio, while every in-workout cue does — so it was left for a release that can carry a
behaviour change.

---

## The phase colour system

The session screen's background is the primary information channel, not decoration.
There are four states, each owning one full-bleed plate: prepare amber, work red,
rest light green, complete violet. All of them resolve in `ui/theme/PhaseColors.kt`.

Rest is a light plate on purpose. Red and mid-green sit at nearly the same
lightness, so under deuteranopia they collapse into two similar mid-tone plates and
the signal fails. Separating them by lightness as well as hue keeps them distinct
with no colour vision at all, and it flips the on-plate text from white to ink as a
second, redundant cue.

Phase is never carried by colour alone. The plate, the uppercase label, the audio
cue, and the haptic all say the same thing. Material You dynamic colour is
deliberately disabled, because work must be red on every device.

The full specification lives in `ocho-design-system/`.

---

## Drift-free timing

Interval boundaries are absolute timestamps computed from the session start,
`startTime + N × intervalMillis`, never a sum of `delay()` calls. Each loop
iteration recalculates from the real clock, so a late or missed tick self-corrects
instead of compounding. Over a 20-minute workout that is the difference between
finishing on the minute and finishing several seconds late.

Pause works by accumulating total paused time and subtracting it:

```
effectiveElapsed = now - startTime - totalPausedMs
```

This keeps the anchoring intact across any number of pauses.

---

## Testing and coverage

`domain/` and `data/` carry the unit tests, because they carry the logic. Composables
and view models are thin by design and verified by reading, which is also the only
option here: the development environment has no emulator.

`./gradlew koverLogDebug` prints line coverage per package. Coverage is reported and
never gated, in CI or locally. A percentage is easy to move by writing tests that
touch lines without asserting anything, so a threshold would reward exactly the tests
worth least. It is grouped by package rather than shown as one figure for the same
reason: the aggregate mixes code tested deliberately with code left untested
deliberately, and only the breakdown distinguishes them.

The exclusions in the `kover` block of `app/build.gradle.kts` decide whether the
number means anything. Composables are excluded by annotation rather than by package,
so plain logic that happens to live under `ui/`, the setup state in particular, still
counts.

---

## Contributing

Branch rules, commit conventions, the release process, and the contributor terms are in
[CONTRIBUTING.md](../CONTRIBUTING.md). Security policy: [SECURITY.md](../SECURITY.md).
