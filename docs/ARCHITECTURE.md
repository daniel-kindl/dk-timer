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
│   ├── model/          TimerConfig, TabataConfig, events, presets, SemVer, UpdateConfig,
│   │                   SessionRequest (sealed, one variant per mode), SessionSnapshot
│   ├── engine/         AbstractPausableEngine, TimerEngine + impl, TabataEngine + impl,
│   │                   and a factory each. WorkoutEngine is the mode-agnostic strategy
│   │                   interface over them, implemented by EmomWorkoutEngine and
│   │                   TabataWorkoutEngine and resolved by WorkoutEngineFactory
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
    ├── navigation/     AppNavigation
    ├── home/           Mode selection
    ├── setup/          EMOM setup + ViewModel
    ├── session/        EMOM session + ViewModel
    ├── tabata/         Tabata setup and session, each with a ViewModel
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
implemented by `EmomWorkoutEngine` and `TabataWorkoutEngine`, so the controller, the
service, and the notification never learn which mode is running. The single branch on
mode lives in `WorkoutEngineFactory`, as a `when` over the sealed `SessionRequest`.
Sealing the request means adding a mode turns that `when` into a compile error instead
of a silent gap.

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

## Contributing

Branch rules, commit conventions, the release process, and the contributor terms are in
[CONTRIBUTING.md](../CONTRIBUTING.md). Security policy: [SECURITY.md](../SECURITY.md).
