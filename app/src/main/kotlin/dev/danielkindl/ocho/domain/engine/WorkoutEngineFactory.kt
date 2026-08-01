package dev.danielkindl.ocho.domain.engine

import dev.danielkindl.ocho.domain.model.SessionRequest
import kotlinx.coroutines.CoroutineScope

/**
 * Builds the right [WorkoutEngine] for a request.
 *
 * This is the only place in the app that branches on which kind of workout is
 * running. Because [SessionRequest] is sealed, adding a mode turns the `when` below
 * into a compile error rather than a silent gap, and nothing downstream, the
 * controller, the service or the notification, has to change at all.
 */
fun interface WorkoutEngineFactory {
    /** Builds an engine whose coroutines run in, and are cancelled with, [scope]. */
    fun create(request: SessionRequest, scope: CoroutineScope): WorkoutEngine
}

/** Production factory, wiring each mode to its underlying drift-free engine. */
class DefaultWorkoutEngineFactory(
    private val timerEngineFactory: TimerEngineFactory,
    private val tabataEngineFactory: TabataEngineFactory,
) : WorkoutEngineFactory {

    override fun create(request: SessionRequest, scope: CoroutineScope): WorkoutEngine =
        when (request) {
            is SessionRequest.Emom ->
                EmomWorkoutEngine(request.config, timerEngineFactory, scope)

            is SessionRequest.Tabata ->
                TabataWorkoutEngine(request.config, tabataEngineFactory, scope)
        }
}
