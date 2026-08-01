package dev.danielkindl.ocho.domain.engine

import dev.danielkindl.ocho.domain.model.SessionCue
import dev.danielkindl.ocho.domain.model.SessionSnapshot
import kotlinx.coroutines.flow.SharedFlow
import kotlinx.coroutines.flow.StateFlow

/**
 * A running workout, whatever kind it is.
 *
 * The abstraction that lets everything downstream stay mode-blind. The session
 * controller, the foreground service and the ongoing notification all consume this
 * interface and never learn whether they are showing EMOM or Tabata, so adding a
 * mode does not mean revisiting any of them.
 *
 * Implementations are thin adapters over the existing [TimerEngine] and
 * [TabataEngine], which keep their own drift-free timing. Nothing here re-implements
 * timing.
 *
 * The two output streams are separate because they have opposite needs. [snapshots]
 * is state, sampled by whoever is watching, and a missed intermediate value costs
 * nothing. [cues] are discrete events that must fire exactly once at the moment they
 * occur, because each one makes a sound.
 */
interface WorkoutEngine {

    /**
     * Current session state, emitted roughly every 100ms while running.
     *
     * A [StateFlow] because it always has a current value: a late collector gets the
     * present state rather than waiting for the next tick.
     */
    val snapshots: StateFlow<SessionSnapshot>

    /**
     * Moments that should produce sound or vibration. Each fires exactly once.
     *
     * A [SharedFlow] with no replay, deliberately. A cue that arrived late would beep
     * for something already past, so callers must subscribe before [start] — use
     * `onSubscription` to make that ordering explicit rather than hoping.
     */
    val cues: SharedFlow<SessionCue>

    /** Begins the workout. Collect [snapshots] and [cues] before calling this. */
    fun start()

    /** Freezes elapsed time. Paused time does not count toward the workout. */
    fun pause()

    /** Resumes, preserving interval alignment across the gap. */
    fun resume()

    /** Ends the session immediately, without a completion cue. */
    fun stop()
}
