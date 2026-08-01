package dev.danielkindl.ocho.domain.model

/**
 * Everything an EMOM session reports as it runs.
 *
 * A single stream carries both display updates and the moments that need sound or
 * vibration, so the view model cannot drift out of step with what the engine
 * believes is happening.
 */
sealed class TimerEvent {
    /**
     * Emitted ~every 100 ms to drive UI updates.
     *
     * @param elapsedMillis        total elapsed time since start
     * @param remainingInInterval  ms until the next interval beep
     * @param currentInterval      1-indexed number of the round currently in progress
     * @param totalIntervals       total number of rounds (ceil(total / interval))
     */
    data class Tick(
        val elapsedMillis: Long,
        val remainingInInterval: Long,
        val currentInterval: Int,
        val totalIntervals: Int,
    ) : TimerEvent()

    /**
     * Emitted at each interval boundary. No event for interval 0.
     *
     * @property intervalNumber the 1-indexed interval that just ended.
     */
    data class IntervalCompleted(val intervalNumber: Int) : TimerEvent()

    /** Emitted once when totalDurationMillis is reached. */
    object WorkoutCompleted : TimerEvent()
}
