package dev.danielkindl.ocho.data.session

/**
 * Starts and stops the foreground service that keeps a session alive.
 *
 * An interface purely so [SessionController] stays free of `Context` and remains
 * unit-testable. The Android implementation is the only thing that knows a service
 * exists.
 */
interface SessionServiceLauncher {

    /** Brings the foreground service up. Safe to call when it is already running. */
    fun start()

    /** Takes the foreground service down. Safe to call when nothing is running. */
    fun stop()
}
