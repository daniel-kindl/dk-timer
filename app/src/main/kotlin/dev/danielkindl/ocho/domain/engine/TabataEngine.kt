package dev.danielkindl.ocho.domain.engine

import dev.danielkindl.ocho.domain.model.TabataConfig
import dev.danielkindl.ocho.domain.model.TabataEvent
import kotlinx.coroutines.flow.SharedFlow

/**
 * Runs a Tabata workout, alternating work and rest phases.
 *
 * Mirrors [TimerEngine] in shape and timing guarantees; the difference is that it
 * ends only on a phase boundary rather than at an exact elapsed time.
 */
interface TabataEngine {

    /**
     * Session progress, including phase transitions. Hot: collect before calling
     * [start], or the opening work phase is missed.
     */
    val events: SharedFlow<TabataEvent>

    /** Begins a workout, cancelling any session already in progress. */
    fun start(config: TabataConfig)

    /** Freezes elapsed time. Time spent paused does not count toward the workout. */
    fun pause()

    /** Resumes from [pause], preserving drift-free accuracy across the gap. */
    fun resume()

    /** Ends the session immediately without emitting a completion event. */
    fun stop()
}
