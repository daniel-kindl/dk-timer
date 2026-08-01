package dev.danielkindl.ocho.domain.model

/**
 * A validated EMOM workout: beep every [intervalMillis] until [totalDurationMillis]
 * elapses.
 *
 * Both bounds are required to be positive at construction. `TimerEngineImpl` divides
 * by [intervalMillis] on every tick, so rejecting zero here is what makes that
 * division provably safe rather than defensively guarded.
 *
 * @property intervalMillis spacing between interval beeps.
 * @property totalDurationMillis total workout length; the final interval fires even
 *   when it lands exactly on this boundary.
 */
data class TimerConfig(
    val intervalMillis: Long,
    val totalDurationMillis: Long,
) {
    init {
        require(intervalMillis > 0) { "intervalMillis must be > 0" }
        require(totalDurationMillis > 0) { "totalDurationMillis must be > 0" }
    }
}
