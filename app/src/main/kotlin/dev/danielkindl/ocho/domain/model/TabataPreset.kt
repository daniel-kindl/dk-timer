package dev.danielkindl.ocho.domain.model

/**
 * A saved Tabata configuration. The Tabata counterpart to [Preset], stored and
 * listed separately so the two setup screens never offer each other's presets.
 *
 * @property id stable identifier used to delete the preset; generated at save time.
 * @property name user-facing label, defaulted from the durations if left blank.
 * @property totalMinutes minutes component of the total workout duration.
 * @property totalSeconds seconds component of the total workout duration.
 * @property workMinutes minutes component of the work phase.
 * @property workSeconds seconds component of the work phase.
 * @property restMinutes minutes component of the rest phase.
 * @property restSeconds seconds component of the rest phase.
 */
data class TabataPreset(
    val id: String,
    val name: String,
    val totalMinutes: Int,
    val totalSeconds: Int,
    val workMinutes: Int,
    val workSeconds: Int,
    val restMinutes: Int,
    val restSeconds: Int,
)
