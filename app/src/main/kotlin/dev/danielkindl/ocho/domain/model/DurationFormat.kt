package dev.danielkindl.ocho.domain.model

private const val SECONDS_PER_MINUTE = 60L
private const val MILLIS_PER_SECOND = 1_000L

/**
 * Converts a picker's minutes and seconds into the milliseconds the engines use.
 * Shared by both setup screens so the two cannot drift apart on rounding.
 */
fun minutesSecondsToMillis(minutes: Int, seconds: Int): Long =
    (minutes * SECONDS_PER_MINUTE + seconds) * MILLIS_PER_SECOND

/**
 * Renders a duration for preset labels, e.g. `20min`, `45s`, `1min 30s`.
 *
 * Omits zero components so a round duration reads as `20min` rather than
 * `20min 0s`. This is the label form; the countdown display uses
 * `formatCountdown` instead.
 */
fun formatDuration(minutes: Int, seconds: Int): String = when {
    minutes > 0 && seconds > 0 -> "${minutes}min ${seconds}s"
    minutes > 0 -> "${minutes}min"
    else -> "${seconds}s"
}
