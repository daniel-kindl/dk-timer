package dev.danielkindl.ocho.domain.model

/**
 * A saved workout configuration, for any mode.
 *
 * Replaces the separate EMOM and Tabata preset types. One type carrying every
 * duration field, with the ones a mode does not use left at zero, rather than
 * nullable per-mode blocks: the fields are cheap, and it keeps both the JSON shape
 * and the setup screen simple.
 *
 * Durations are stored as the minutes and seconds the pickers show rather than as
 * milliseconds, so loading a preset restores the wheels exactly without a lossy
 * round trip.
 *
 * @property id stable identifier used to delete the preset; generated at save time.
 * @property name user-facing label, defaulted from the durations if left blank.
 * @property mode which workout this configures. Setup screens list only their own.
 * @property totalMinutes minutes component of the total duration. All modes.
 * @property totalSeconds seconds component of the total duration. All modes.
 * @property intervalMinutes minutes component of the EMOM interval. Zero otherwise.
 * @property intervalSeconds seconds component of the EMOM interval. Zero otherwise.
 * @property workMinutes minutes component of the Tabata work phase. Zero otherwise.
 * @property workSeconds seconds component of the Tabata work phase. Zero otherwise.
 * @property restMinutes minutes component of the Tabata rest phase. Zero otherwise.
 * @property restSeconds seconds component of the Tabata rest phase. Zero otherwise.
 */
data class WorkoutPreset(
    val id: String,
    val name: String,
    val mode: WorkoutMode,
    val totalMinutes: Int,
    val totalSeconds: Int,
    val intervalMinutes: Int = 0,
    val intervalSeconds: Int = 0,
    val workMinutes: Int = 0,
    val workSeconds: Int = 0,
    val restMinutes: Int = 0,
    val restSeconds: Int = 0,
)
