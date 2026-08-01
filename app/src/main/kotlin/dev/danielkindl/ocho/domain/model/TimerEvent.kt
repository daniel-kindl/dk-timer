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

    /**
     * Emitted once per second over the last few seconds before a boundary.
     *
     * Produced inside the engine loop rather than derived from [Tick] by a consumer,
     * so it inherits the same drift-free anchoring as the boundary it precedes. A
     * consumer counting down from tick values would drift exactly as much as the
     * accumulated-delay approach the engine exists to avoid.
     *
     * Suppressed entirely when the interval is no longer than the lead-in, since a
     * continuous countdown conveys nothing.
     *
     * @property secondsRemaining counts down and never repeats within one interval.
     */
    data class CountdownTick(val secondsRemaining: Int) : TimerEvent()

    /** Emitted once when totalDurationMillis is reached. */
    object WorkoutCompleted : TimerEvent()
}
