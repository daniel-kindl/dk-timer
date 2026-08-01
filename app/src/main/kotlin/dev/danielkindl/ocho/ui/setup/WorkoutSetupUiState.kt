package dev.danielkindl.ocho.ui.setup

import dev.danielkindl.ocho.domain.model.AmrapConfig
import dev.danielkindl.ocho.domain.model.SessionRequest
import dev.danielkindl.ocho.domain.model.TabataConfig
import dev.danielkindl.ocho.domain.model.TimerConfig
import dev.danielkindl.ocho.domain.model.WorkoutMode
import dev.danielkindl.ocho.domain.model.WorkoutPreset
import dev.danielkindl.ocho.domain.model.formatDuration
import dev.danielkindl.ocho.domain.model.minutesSecondsToMillis
import kotlin.math.ceil

/**
 * Picker state for every workout mode.
 *
 * Replaces the separate EMOM and Tabata setup states. All duration fields exist on
 * one type and each mode reads the subset it needs, which is what lets one screen
 * and one view model serve every mode. The alternative, a sealed state per mode,
 * would have pushed a `when` into every consumer for no benefit: the fields are
 * cheap and the pickers produce them all identically.
 *
 * Defaults are per-mode sensible: a 20 minute EMOM on the minute, 45s/15s Tabata
 * cycles, a 20 minute AMRAP. All startable without touching a picker.
 *
 * @property mode which workout is being configured. Fixed for the lifetime of the
 *   screen; changing mode means navigating to a different setup destination.
 * @property totalMinutes minutes component of the total duration. Read by all modes.
 * @property totalSeconds seconds component of the total duration. Read by all modes.
 * @property intervalMinutes minutes component of the EMOM interval. Ignored elsewhere.
 * @property intervalSeconds seconds component of the EMOM interval. Ignored elsewhere.
 * @property workMinutes minutes component of the Tabata work phase. Ignored elsewhere.
 * @property workSeconds seconds component of the Tabata work phase. Ignored elsewhere.
 * @property restMinutes minutes component of the Tabata rest phase. Ignored elsewhere.
 * @property restSeconds seconds component of the Tabata rest phase. Ignored elsewhere.
 */
data class WorkoutSetupUiState(
    val mode: WorkoutMode,
    val totalMinutes: Int = 20,
    val totalSeconds: Int = 0,
    val intervalMinutes: Int = 1,
    val intervalSeconds: Int = 0,
    val workMinutes: Int = 0,
    val workSeconds: Int = 45,
    val restMinutes: Int = 0,
    val restSeconds: Int = 15,
) {
    /** Total duration in milliseconds, as the engines want it. All modes. */
    val totalDurationMillis: Long
        get() = minutesSecondsToMillis(totalMinutes, totalSeconds)

    /** EMOM interval length in milliseconds. */
    val intervalMillis: Long
        get() = minutesSecondsToMillis(intervalMinutes, intervalSeconds)

    /** Tabata work phase length in milliseconds. */
    val workMillis: Long
        get() = minutesSecondsToMillis(workMinutes, workSeconds)

    /** Tabata rest phase length in milliseconds. */
    val restMillis: Long
        get() = minutesSecondsToMillis(restMinutes, restSeconds)

    /**
     * Whether START may be enabled.
     *
     * Each mode validates only the fields it uses, so a leftover zero in an unused
     * picker cannot block a workout that does not read it.
     */
    val isValid: Boolean
        get() = totalDurationMillis > 0 && when (mode) {
            WorkoutMode.EMOM -> intervalMillis > 0
            WorkoutMode.TABATA -> workMillis > 0 && restMillis > 0
            WorkoutMode.AMRAP -> true
        }

    /** True when an EMOM interval exceeds its total, so no interval events will fire. */
    val intervalExceedsTotal: Boolean
        get() = mode == WorkoutMode.EMOM && isValid && intervalMillis > totalDurationMillis

    /**
     * Rounds this configuration will run.
     *
     * Rounds up in both counted modes, matching the engines: a partial final interval
     * still gets a beep, and a phase is never cut short. AMRAP reports zero, because
     * its rounds are whatever the athlete manages and the app cannot know that.
     */
    val roundCount: Int
        get() {
            if (!isValid) return 0
            return when (mode) {
                WorkoutMode.EMOM ->
                    ceil(totalDurationMillis.toDouble() / intervalMillis).toInt()

                WorkoutMode.TABATA -> {
                    val cycle = workMillis + restMillis
                    var elapsed = 0L
                    var rounds = 0
                    while (elapsed < totalDurationMillis) {
                        rounds++
                        elapsed += cycle
                    }
                    rounds
                }

                WorkoutMode.AMRAP -> 0
            }
        }

    /** Structure summary for the run timeline. */
    val patternLabel: String
        get() = when (mode) {
            WorkoutMode.EMOM ->
                "$roundCount × ${formatDuration(intervalMinutes, intervalSeconds)}"

            WorkoutMode.TABATA -> {
                val work = formatDuration(workMinutes, workSeconds)
                val rest = formatDuration(restMinutes, restSeconds)
                "$roundCount × ($work work / $rest rest)"
            }

            WorkoutMode.AMRAP -> "as many rounds as possible"
        }

    /** Suggested preset name, used when the user leaves the field blank. */
    fun defaultPresetName(): String {
        val total = formatDuration(totalMinutes, totalSeconds)
        return when (mode) {
            WorkoutMode.EMOM ->
                "$total / ${formatDuration(intervalMinutes, intervalSeconds)}"

            WorkoutMode.TABATA -> {
                val work = formatDuration(workMinutes, workSeconds)
                val rest = formatDuration(restMinutes, restSeconds)
                "$total / $work work / $rest rest"
            }

            WorkoutMode.AMRAP -> total
        }
    }

    /**
     * Converts these picker values into the request the engines consume.
     *
     * The single point where setup meets the session layer, which is why
     * `SessionRequestCharacterizationTest` pins its output.
     */
    fun toRequest(): SessionRequest = when (mode) {
        WorkoutMode.EMOM -> SessionRequest.Emom(
            TimerConfig(
                intervalMillis = intervalMillis,
                totalDurationMillis = totalDurationMillis,
            )
        )

        WorkoutMode.TABATA -> SessionRequest.Tabata(
            TabataConfig(
                workMillis = workMillis,
                restMillis = restMillis,
                totalDurationMillis = totalDurationMillis,
            )
        )

        WorkoutMode.AMRAP -> SessionRequest.Amrap(
            AmrapConfig(totalDurationMillis = totalDurationMillis)
        )
    }

    /** Replaces the picker values with a saved preset's. */
    fun withPreset(preset: WorkoutPreset): WorkoutSetupUiState = copy(
        totalMinutes = preset.totalMinutes,
        totalSeconds = preset.totalSeconds,
        intervalMinutes = preset.intervalMinutes,
        intervalSeconds = preset.intervalSeconds,
        workMinutes = preset.workMinutes,
        workSeconds = preset.workSeconds,
        restMinutes = preset.restMinutes,
        restSeconds = preset.restSeconds,
    )

    /** Captures the current pickers as a saveable preset under [name] and [id]. */
    fun toPreset(id: String, name: String): WorkoutPreset = WorkoutPreset(
        id = id,
        name = name.trim().ifEmpty { defaultPresetName() },
        mode = mode,
        totalMinutes = totalMinutes,
        totalSeconds = totalSeconds,
        intervalMinutes = intervalMinutes,
        intervalSeconds = intervalSeconds,
        workMinutes = workMinutes,
        workSeconds = workSeconds,
        restMinutes = restMinutes,
        restSeconds = restSeconds,
    )
}
