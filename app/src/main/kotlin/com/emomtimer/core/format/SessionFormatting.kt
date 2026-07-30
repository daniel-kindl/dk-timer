package com.emomtimer.core.format

private const val MILLIS_PER_SECOND = 1_000L
private const val SECONDS_PER_MINUTE = 60L

fun sessionProgress(elapsedMillis: Long, totalDurationMillis: Long): Float {
    if (totalDurationMillis <= 0L) return 0f
    return (elapsedMillis.toFloat() / totalDurationMillis.toFloat()).coerceIn(0f, 1f)
}

fun Long.formatCountdown(): String {
    val totalSec = (this / MILLIS_PER_SECOND).coerceAtLeast(0)
    val min = totalSec / SECONDS_PER_MINUTE
    val sec = totalSec % SECONDS_PER_MINUTE
    return "%d:%02d".format(min, sec)
}

fun Long.formatElapsed(): String {
    val totalSec = this / MILLIS_PER_SECOND
    val min = totalSec / SECONDS_PER_MINUTE
    val sec = totalSec % SECONDS_PER_MINUTE
    return "%02d:%02d".format(min, sec)
}
