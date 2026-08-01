package dev.danielkindl.ocho.domain.model

/**
 * A saved EMOM configuration.
 *
 * Stores minutes and seconds separately rather than a single millisecond duration
 * because that is the shape the wheel pickers restore into — keeping the split
 * avoids a lossy round trip through milliseconds every time a preset is loaded.
 *
 * @property id stable identifier used to delete the preset; generated at save time.
 * @property name user-facing label, defaulted from the durations if left blank.
 * @property totalMinutes minutes component of the total workout duration.
 * @property totalSeconds seconds component of the total workout duration.
 * @property intervalMinutes minutes component of the interval length.
 * @property intervalSeconds seconds component of the interval length.
 */
data class Preset(
    val id: String,
    val name: String,
    val totalMinutes: Int,
    val totalSeconds: Int,
    val intervalMinutes: Int,
    val intervalSeconds: Int,
)
