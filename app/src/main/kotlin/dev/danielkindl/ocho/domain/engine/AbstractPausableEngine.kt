package dev.danielkindl.ocho.domain.engine

import dev.danielkindl.ocho.core.Clock

/**
 * Base class that provides drift-free pause/resume state shared by all timer engines.
 *
 * Subclasses read [isPaused] and [totalPausedMs] inside their timer loop to compute
 * effective elapsed time as: `now - startTime - totalPausedMs`.
 */
abstract class AbstractPausableEngine(protected val clock: Clock) {

    @Volatile protected var isPaused = false
    @Volatile private var pauseStartTime = 0L
    @Volatile protected var totalPausedMs = 0L

    protected fun resetPauseState() {
        isPaused = false
        pauseStartTime = 0L
        totalPausedMs = 0L
    }

    open fun pause() {
        if (!isPaused) {
            pauseStartTime = clock.currentTimeMillis()
            isPaused = true
        }
    }

    open fun resume() {
        if (isPaused) {
            // Update totalPausedMs BEFORE clearing isPaused so the timer loop
            // sees the correct offset as soon as it exits the pause-check loop.
            totalPausedMs += clock.currentTimeMillis() - pauseStartTime
            isPaused = false
        }
    }
}
