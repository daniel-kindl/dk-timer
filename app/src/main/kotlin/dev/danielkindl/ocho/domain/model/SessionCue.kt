package dev.danielkindl.ocho.domain.model

/**
 * A moment in a session that should make a noise.
 *
 * Kept deliberately separate from [SessionSnapshot]. State and cues have opposite
 * requirements: the notification samples state at 1Hz and does not care if it misses
 * an intermediate value, while a cue must fire exactly once, at the instant it
 * happens, and never again. Conflating them is how a screen rotation ends up
 * replaying a beep.
 *
 * Cues carry no timing of their own. They are emitted from the engine loop, so they
 * inherit the same drift-free anchoring as the boundary that produced them.
 */
sealed interface SessionCue {

    /** An EMOM interval boundary. */
    data object IntervalBoundary : SessionCue

    /**
     * A Tabata phase flip.
     *
     * @property phase the phase now beginning, which selects the beep pitch.
     */
    data class PhaseChanged(val phase: Phase) : SessionCue

    /**
     * One tick of the 3-2-1 lead-in before a boundary.
     *
     * @property secondsRemaining 3, 2 or 1. Counts down, never repeats a value
     *   within one boundary.
     */
    data class Countdown(val secondsRemaining: Int) : SessionCue

    /** The workout finished. Gets the distinct completion tone and double vibration. */
    data object Completed : SessionCue
}
