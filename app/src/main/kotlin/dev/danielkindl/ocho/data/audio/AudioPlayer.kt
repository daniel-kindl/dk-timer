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

    /** Short, quiet tick for the 3-2-1 lead-in. Must not be mistaken for a boundary. */
    fun playCountdownBeep()

    /** Longer, distinct tone marking the end of a workout. */
    fun playCompletionSound()

    /** High-pitched beep: signals the start of a Tabata work phase. */
    fun playWorkStartBeep()

    /** Low-pitched beep: signals the start of a Tabata rest phase. */
    fun playRestStartBeep()

    /**
     * Asks the system for transient audio focus, so music ducks while cues play
     * instead of drowning them.
     *
     * Held for the whole session rather than requested per beep. Per-beep requests
     * make other apps duck and un-duck several times a minute, which is audibly worse
     * than not ducking at all.
     */
    fun requestAudioFocus()

    /** Returns audio focus. Call when the session ends, or music stays ducked. */
    fun abandonAudioFocus()

    /** Frees the underlying audio resources. Call when the session ends. */
    fun release()
}
