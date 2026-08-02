package dev.danielkindl.ocho.domain.model

/**
 * Presets a build ships with, as opposed to ones the user saved.
 *
 * A type of its own rather than a bare `List<WorkoutPreset>`, which would say nothing
 * at the injection site and would collide with any other list binding. Which presets it
 * holds, or whether it holds any, is decided once in `di/AppModule`; the repository
 * simply serves whatever it is given, so on the stable channel the list is empty and
 * nothing branches on the build type outside DI.
 *
 * A data class rather than a value class on purpose: an inline wrapper erases to
 * `List` in its JVM signature, so Hilt resolves the binding key to the wrong type and
 * fails to provide it.
 *
 * @property presets the built-in configurations, in the order their chips appear.
 */
data class BuiltInPresets(val presets: List<WorkoutPreset>)

/**
 * The workouts a testing build offers with no picker work, one tap from Start.
 *
 * These exist for the device checks that CI cannot perform: no emulator runs here, so
 * timing and audio behaviour is confirmed by hand on a phone, and dialling four wheels
 * to reach a 63 second EMOM before every check is how a check gets skipped.
 *
 * The EMOM entries are the interesting cases rather than realistic workouts. A total
 * that is an exact multiple of the interval, one whose remainder is longer than the
 * lead-in, one whose remainder is shorter than it, and one whose interval outlasts the
 * whole workout. Those four are where the lead-in and the final numeral are decided,
 * and where a regression would otherwise go unheard. All but the last run in about a
 * minute, so a full pass costs a few minutes rather than an afternoon.
 */
val DEVICE_CHECK_PRESETS: List<WorkoutPreset> = listOf(
    WorkoutPreset(
        id = "built-in-emom-even",
        name = "EMOM 1:00 ÷ 0:20",
        mode = WorkoutMode.EMOM,
        totalMinutes = 1,
        totalSeconds = 0,
        intervalSeconds = 20,
        builtIn = true,
    ),
    WorkoutPreset(
        id = "built-in-emom-tail",
        name = "EMOM 1:05 tail 5s",
        mode = WorkoutMode.EMOM,
        totalMinutes = 1,
        totalSeconds = 5,
        intervalSeconds = 20,
        builtIn = true,
    ),
    WorkoutPreset(
        id = "built-in-emom-silent-tail",
        name = "EMOM 1:03 tail 3s",
        mode = WorkoutMode.EMOM,
        totalMinutes = 1,
        totalSeconds = 3,
        intervalSeconds = 20,
        builtIn = true,
    ),
    WorkoutPreset(
        id = "built-in-emom-over",
        name = "EMOM 0:03 ÷ 0:05",
        mode = WorkoutMode.EMOM,
        totalMinutes = 0,
        totalSeconds = 3,
        intervalSeconds = 5,
        builtIn = true,
    ),
    WorkoutPreset(
        id = "built-in-tabata",
        name = "Tabata 20/10",
        mode = WorkoutMode.TABATA,
        totalMinutes = 1,
        totalSeconds = 0,
        workSeconds = 20,
        restSeconds = 10,
        builtIn = true,
    ),
    WorkoutPreset(
        id = "built-in-amrap",
        name = "AMRAP 1:00",
        mode = WorkoutMode.AMRAP,
        totalMinutes = 1,
        totalSeconds = 0,
        builtIn = true,
    ),
)
