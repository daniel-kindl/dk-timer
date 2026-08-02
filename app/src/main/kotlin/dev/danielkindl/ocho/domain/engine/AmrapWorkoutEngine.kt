package dev.danielkindl.ocho.domain.engine

import dev.danielkindl.ocho.domain.model.AmrapConfig
import dev.danielkindl.ocho.domain.model.Phase
import dev.danielkindl.ocho.domain.model.SessionCue
import dev.danielkindl.ocho.domain.model.SessionSnapshot
import dev.danielkindl.ocho.domain.model.SessionStatus
import dev.danielkindl.ocho.domain.model.TimerConfig
import dev.danielkindl.ocho.domain.model.TimerEvent
import dev.danielkindl.ocho.domain.model.WorkoutMode
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharedFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asSharedFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.onSubscription
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

/**
 * Presents an AMRAP workout through the mode-agnostic [WorkoutEngine] interface.
 *
 * Adds no timing of its own. **An AMRAP is an EMOM whose interval equals its total**:
 * one interval, ending exactly when the workout does. Building the request that way
 * reuses [TimerEngineImpl] and its drift-free scheduling wholesale, including the
 * 3-2-1 lead-in, which lands on the finish rather than on an interval boundary and
 * is exactly where an AMRAP wants it.
 *
 * The one thing that needs suppressing is [TimerEvent.IntervalCompleted]. Because the
 * single interval ends at the same instant as the workout, leaving it in place would
 * fire a boundary beep and the completion tone together, and an AMRAP has no
 * boundaries to announce.
 *
 * Round counters are meaningless here: the rounds are whatever the athlete managed,
 * which the app has no way to know. [SessionSnapshot.totalRounds] stays zero, and the
 * session screen omits the counter when it is.
 */
class AmrapWorkoutEngine(
    config: AmrapConfig,
    engineFactory: TimerEngineFactory,
    private val scope: CoroutineScope,
) : WorkoutEngine {

    private val timerConfig = TimerConfig(
        intervalMillis = config.totalDurationMillis,
        totalDurationMillis = config.totalDurationMillis,
    )

    private val engine = engineFactory.create(scope)

    private val _snapshots = MutableStateFlow(
        SessionSnapshot(
            mode = WorkoutMode.AMRAP,
            status = SessionStatus.Running,
            phase = Phase.WORK,
            totalDurationMillis = config.totalDurationMillis,
        )
    )
    override val snapshots: StateFlow<SessionSnapshot> = _snapshots.asStateFlow()

    private val _cues = MutableSharedFlow<SessionCue>(extraBufferCapacity = CUE_BUFFER)
    override val cues: SharedFlow<SessionCue> = _cues.asSharedFlow()

    override fun start() {
        scope.launch {
            engine.events
                .onSubscription { engine.start(timerConfig) }
                .collect(::handle)
        }
    }

    private suspend fun handle(event: TimerEvent) {
        when (event) {
            is TimerEvent.Tick -> _snapshots.update {
                it.copy(
                    status = SessionStatus.Running,
                    remainingInPhaseMillis = event.remainingInInterval,
                    elapsedMillis = event.elapsedMillis,
                )
            }

            // Deliberately dropped: the sole interval ends when the workout does, so
            // this would double the completion cue. See the class comment.
            is TimerEvent.IntervalCompleted -> Unit

            is TimerEvent.CountdownTick -> _cues.emit(SessionCue.Countdown(event.secondsRemaining))

            is TimerEvent.WorkoutCompleted -> {
                _snapshots.update {
                    it.copy(
                        status = SessionStatus.Completed,
                        phase = Phase.COMPLETE,
                        elapsedMillis = event.elapsedMillis,
                    )
                }
                _cues.emit(SessionCue.Completed)
            }
        }
    }

    override fun pause() {
        engine.pause()
        _snapshots.update { it.copy(status = SessionStatus.Paused) }
    }

    override fun resume() {
        engine.resume()
        _snapshots.update { it.copy(status = SessionStatus.Running) }
    }

    override fun stop() {
        engine.stop()
        _snapshots.update { it.copy(status = SessionStatus.Stopped) }
    }

    private companion object {
        /** Room for a burst of cues if a collector is briefly slow. */
        const val CUE_BUFFER = 16
    }
}
