package dev.danielkindl.ocho.data.audio

/**
 * Plays the workout cue sounds.
 *
 * Distinct sounds per event matter more than they look: mid-workout the user is
 * rarely looking at the screen, so the pitch is the only signal telling them
 * whether to start working, start resting, or stop.
 */
interface AudioPlayer {

    /** Marks an EMOM interval boundary. */
    fun playIntervalBeep()

    /** Longer, distinct tone marking the end of a workout. */
    fun playCompletionSound()

    /** High-pitched beep: signals the start of a Tabata work phase. */
    fun playWorkStartBeep()

    /** Low-pitched beep: signals the start of a Tabata rest phase. */
    fun playRestStartBeep()

    /** Frees the underlying audio resources. Call when the session ends. */
    fun release()
}
