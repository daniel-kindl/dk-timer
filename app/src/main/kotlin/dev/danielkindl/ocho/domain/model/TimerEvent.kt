package dev.danielkindl.ocho.domain.model

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

    /** Emitted at each interval boundary (1-indexed). No event for interval 0. */
    data class IntervalCompleted(val intervalNumber: Int) : TimerEvent()

    /** Emitted once when totalDurationMillis is reached. */
    object WorkoutCompleted : TimerEvent()
}
