package dev.danielkindl.ocho.domain.model

/**
 * Lifecycle of a running session, shared by the EMOM and Tabata view models.
 *
 * Distinct from [Phase], which describes what the user is doing. Pausing changes the
 * status but not the phase: you are still inside the work interval, merely frozen
 * in it.
 */
enum class SessionStatus {
    /** Counting in, before the first interval starts. */
    CountingDown,

    /** The engine is advancing. */
    Running,

    /** Frozen. Paused time does not count toward the workout. */
    Paused,

    /**
     * Ended early by the user.
     *
     * Deliberately distinct from [Completed]: stopping navigates away immediately,
     * while completing shows a summary and waits to be dismissed.
     */
    Stopped,

    /** Ran to the end of its configured duration. */
    Completed,
}
