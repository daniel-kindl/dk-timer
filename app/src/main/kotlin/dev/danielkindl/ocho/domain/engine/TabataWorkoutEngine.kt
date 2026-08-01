package dev.danielkindl.ocho.domain.engine

import dev.danielkindl.ocho.domain.model.Phase
import dev.danielkindl.ocho.domain.model.SessionCue
import dev.danielkindl.ocho.domain.model.SessionSnapshot
import dev.danielkindl.ocho.domain.model.SessionStatus
import dev.danielkindl.ocho.domain.model.TabataConfig
import dev.danielkindl.ocho.domain.model.TabataEvent
import dev.danielkindl.ocho.domain.model.TabataPhase
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
 * Presents a Tabata workout through the mode-agnostic [WorkoutEngine] interface.
 *
 * A pure adapter over [TabataEngine], with no timing of its own. Its one piece of
 * translation worth noting is mapping [TabataPhase], which the engine uses to decide
 * durations, onto the domain [Phase] that the rest of the app renders.
 */
class TabataWorkoutEngine(
    private val config: TabataConfig,
    engineFactory: TabataEngineFactory,
    private val scope: CoroutineScope,
) : WorkoutEngine {

    private val engine = engineFactory.create(scope)

    private val _snapshots = MutableStateFlow(
        SessionSnapshot(
            mode = WorkoutMode.TABATA,
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
            // Subscribe before starting: the engine emits its opening WorkStarted
            // immediately, and its flow has no replay to catch it afterwards.
            engine.events
                .onSubscription { engine.start(config) }
                .collect(::handle)
        }
    }

    private suspend fun handle(event: TabataEvent) {
        when (event) {
            is TabataEvent.Tick -> _snapshots.update {
                it.copy(
                    status = SessionStatus.Running,
                    phase = event.phase.toDomainPhase(),
                    remainingInPhaseMillis = event.remainingInPhaseMillis,
                    elapsedMillis = event.elapsedMillis,
                    currentRound = event.currentRound,
                    totalRounds = event.totalRounds,
                )
            }

            is TabataEvent.WorkStarted -> _cues.emit(SessionCue.PhaseChanged(Phase.WORK))

            is TabataEvent.RestStarted -> _cues.emit(SessionCue.PhaseChanged(Phase.REST))

            is TabataEvent.CountdownTick -> _cues.emit(SessionCue.Countdown(event.secondsRemaining))

            is TabataEvent.WorkoutCompleted -> {
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

    private fun TabataPhase.toDomainPhase(): Phase =
        if (this == TabataPhase.Work) Phase.WORK else Phase.REST

    private companion object {
        /** Room for a burst of cues if a collector is briefly slow. */
        const val CUE_BUFFER = 16
    }
}
