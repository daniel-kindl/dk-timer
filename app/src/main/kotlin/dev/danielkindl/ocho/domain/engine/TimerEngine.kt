package dev.danielkindl.ocho.domain.engine

import dev.danielkindl.ocho.domain.model.TimerConfig
import dev.danielkindl.ocho.domain.model.TimerEvent
import kotlinx.coroutines.flow.SharedFlow

/**
 * Runs an EMOM workout and reports its progress.
 *
 * An interface so sessions can be driven by a fake engine in tests without waiting
 * in real time. The implementation is drift-free: boundaries are anchored to
 * absolute timestamps rather than accumulated delays.
 */
interface TimerEngine {

    /**
     * Session progress. Hot: events emitted before collection starts are missed, so
     * collect before calling [start].
     */
    val events: SharedFlow<TimerEvent>

    /** Begins a workout, cancelling any session already in progress. */
    fun start(config: TimerConfig)

    /** Freezes elapsed time. Time spent paused does not count toward the workout. */
    fun pause()

    /** Resumes from [pause], preserving drift-free accuracy across the gap. */
    fun resume()

    /** Ends the session immediately without emitting a completion event. */
    fun stop()
}
