package dev.danielkindl.ocho.domain.model

/**
 * The two halves of a Tabata round.
 *
 * Internal to the engine, which uses it to pick the next phase duration. The rest of
 * the app works in terms of the domain-wide [Phase] instead.
 */
enum class TabataPhase {
    /** The effort interval. */
    Work,

    /** The recovery interval. */
    Rest,
}

/**
 * Everything a Tabata session reports as it runs.
 *
 * Phase changes are announced as their own events rather than inferred from
 * consecutive [Tick]s, so the beep fires exactly once per transition even if a tick
 * is dropped or delivered late.
 */
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

    /**
     * Emitted once per second over the last few seconds before a phase flips.
     *
     * Produced inside the engine loop, so it shares the phase boundary's drift-free
     * anchoring. Suppressed when the current phase is no longer than the lead-in.
     *
     * @property secondsRemaining counts down and never repeats within one phase.
     */
    data class CountdownTick(val secondsRemaining: Int) : TabataEvent()

    /**
     * Emitted once the workout is done (after the last phase finishes).
     *
     * @property elapsedMillis how long the workout actually ran, which is the sum of
     *   the phases and so can exceed the configured total, since a phase is never cut
     *   short. Carried here rather than left to the last [Tick] because ticks are
     *   samples and the final one lands before the true end.
     */
    data class WorkoutCompleted(val elapsedMillis: Long) : TabataEvent()
}
