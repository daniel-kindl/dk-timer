package com.emomtimer.domain.model

enum class TabataPhase { Work, Rest }

sealed class TabataEvent {
    /**
     * Emitted ~every 100 ms to drive UI updates.
     *
     * @param phase                 current phase (Work or Rest)
     * @param remainingInPhaseMillis ms until this phase ends
     * @param elapsedMillis         total effective elapsed time (excluding pauses)
     * @param currentRound          1-based round number (a round starts at each Work phase)
     * @param totalRounds           total number of rounds this workout will run
     */
    data class Tick(
        val phase: TabataPhase,
        val remainingInPhaseMillis: Long,
        val elapsedMillis: Long,
        val currentRound: Int,
        val totalRounds: Int,
    ) : TabataEvent()

    /** Emitted at every work-phase start (after the first rest). Triggers high-pitch beep. */
    data object WorkStarted : TabataEvent()

    /** Emitted at every rest-phase start. Triggers low-pitch beep. */
    data object RestStarted : TabataEvent()

    /** Emitted once the workout is done (after the last phase finishes). */
    data object WorkoutCompleted : TabataEvent()
}
