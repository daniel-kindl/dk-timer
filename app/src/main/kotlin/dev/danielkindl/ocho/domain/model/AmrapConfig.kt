package dev.danielkindl.ocho.domain.model

/**
 * An AMRAP workout: one unbroken effort for [totalDurationMillis], then stop.
 *
 * "As many rounds as possible" counts nothing itself. The rounds are whatever the
 * athlete manages, so the timer's only job is to run down and signal the end
 * unambiguously. That makes it the simplest config in the app, and deliberately so:
 * an AMRAP with interval beeps would be an EMOM.
 *
 * @property totalDurationMillis how long the effort runs. Required positive, since
 *   an AMRAP of zero length has nothing to signal.
 */
data class AmrapConfig(
    val totalDurationMillis: Long,
) {
    init {
        require(totalDurationMillis > 0) { "totalDurationMillis must be > 0" }
    }
}
