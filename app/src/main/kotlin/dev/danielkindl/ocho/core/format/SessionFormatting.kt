package dev.danielkindl.ocho.core.format

private const val MILLIS_PER_SECOND = 1_000L
private const val SECONDS_PER_MINUTE = 60L

/**
 * Fraction of a session completed, clamped to `0f..1f` for direct use as a progress
 * value. A non-positive [totalDurationMillis] yields `0f` rather than dividing by
 * zero, so a half-built config cannot crash a progress bar mid-composition.
 */
fun sessionProgress(elapsedMillis: Long, totalDurationMillis: Long): Float {
    if (totalDurationMillis <= 0L) return 0f
    return (elapsedMillis.toFloat() / totalDurationMillis.toFloat()).coerceIn(0f, 1f)
}

/**
 * Formats remaining time for the large countdown numeral, as `M:SS`.
 *
 * Unpadded minutes keep the glyph count down on the dominant on-screen element.
 * Negative input clamps to zero: the engine can report a slightly negative
 * remainder at a boundary, and `-1:59` would be alarming mid-workout.
 */
fun Long.formatCountdown(): String {
    val totalSec = (this / MILLIS_PER_SECOND).coerceAtLeast(0)
    val min = totalSec / SECONDS_PER_MINUTE
    val sec = totalSec % SECONDS_PER_MINUTE
    return "%d:%02d".format(min, sec)
}

/**
 * Formats elapsed time as `MM:SS`, zero-padded so the value does not change width
 * as it counts up and shift the surrounding layout on every tick.
 */
fun Long.formatElapsed(): String {
    val totalSec = this / MILLIS_PER_SECOND
    val min = totalSec / SECONDS_PER_MINUTE
    val sec = totalSec % SECONDS_PER_MINUTE
    return "%02d:%02d".format(min, sec)
}
