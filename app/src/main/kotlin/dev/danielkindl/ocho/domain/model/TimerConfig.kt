package dev.danielkindl.ocho.domain.model

data class TimerConfig(
    val intervalMillis: Long,
    val totalDurationMillis: Long,
) {
    init {
        require(intervalMillis > 0) { "intervalMillis must be > 0" }
        require(totalDurationMillis > 0) { "totalDurationMillis must be > 0" }
    }
}
