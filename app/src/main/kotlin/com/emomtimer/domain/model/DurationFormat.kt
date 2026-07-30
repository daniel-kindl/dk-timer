package com.emomtimer.domain.model

private const val SECONDS_PER_MINUTE = 60L
private const val MILLIS_PER_SECOND = 1_000L

fun minutesSecondsToMillis(minutes: Int, seconds: Int): Long =
    (minutes * SECONDS_PER_MINUTE + seconds) * MILLIS_PER_SECOND

fun formatDuration(minutes: Int, seconds: Int): String = when {
    minutes > 0 && seconds > 0 -> "${minutes}min ${seconds}s"
    minutes > 0 -> "${minutes}min"
    else -> "${seconds}s"
}
