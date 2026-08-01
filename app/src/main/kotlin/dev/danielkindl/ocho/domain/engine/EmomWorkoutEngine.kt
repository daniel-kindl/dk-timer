package dev.danielkindl.ocho.domain.engine

import dev.danielkindl.ocho.domain.model.Phase
import dev.danielkindl.ocho.domain.model.SessionCue
import dev.danielkindl.ocho.domain.model.SessionSnapshot
import dev.danielkindl.ocho.domain.model.SessionStatus
import dev.danielkindl.ocho.domain.model.TimerConfig
import dev.danielkindl.ocho.domain.model.TimerEvent
import dev.danielkindl.ocho.domain.model.WorkoutMode
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.flow.SharedFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asSharedFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.onSubscription
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

/**
 * Presents an EMOM workout through the mode-agnostic [WorkoutEngine] interface.
 *
 * A pure adapter: it owns a [TimerEngine] and translates its events, and contains no
 * timing of its own. The drift-free scheduling stays in [TimerEngineImpl], which is
 * the code least worth disturbing.
 *
 * EMOM never reports [Phase.REST]. The whole session is one work phase, punctuated
 * by boundaries rather than divided by them.
 */
class EmomWorkoutEngine(
    private val config: TimerConfig,
    engineFactory: TimerEngineFactory,
    private val scope: CoroutineScope,
) : WorkoutEngine {

    private val engine = engineFactory.create(scope)

    private val _snapshots = MutableStateFlow(
        SessionSnapshot(
            mode = WorkoutMode.EMOM,
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
            // onSubscription starts the engine only once this collector is registered.
            // The engine's event flow has no replay, so anything emitted before
            // subscription is dropped, and starting it beforehand would race the
            // opening events away.
            engine.events
                .onSubscription { engine.start(config) }
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
                    currentRound = event.currentInterval,
                    totalRounds = event.totalIntervals,
                )
            }

            is TimerEvent.IntervalCompleted -> _cues.emit(SessionCue.IntervalBoundary)

            is TimerEvent.CountdownTick -> _cues.emit(SessionCue.Countdown(event.secondsRemaining))

            is TimerEvent.WorkoutCompleted -> {
                _snapshots.update {
                    it.copy(status = SessionStatus.Completed, phase = Phase.COMPLETE)
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
