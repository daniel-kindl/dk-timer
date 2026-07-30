package com.emomtimer.domain.engine

import com.emomtimer.core.Clock
import com.emomtimer.domain.model.TabataConfig
import com.emomtimer.domain.model.TabataEvent
import com.emomtimer.domain.model.TabataPhase
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.SharedFlow
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch

/**
 * Drift-free Tabata engine that alternates WORK and REST phases.
 *
 * Timing is anchored to the real clock (not accumulated delays) using the
 * same pause-safe approach as [TimerEngineImpl]: effective elapsed time is
 * `now - startTime - totalPausedMs`.
 *
 * **Completion policy**: the workout ends only at a phase boundary. When
 * accumulated phase time reaches or exceeds [TabataConfig.totalDurationMillis],
 * [TabataEvent.WorkoutCompleted] is emitted and the engine stops — it never
 * cuts a phase short mid-way.
 */
class TabataEngineImpl(
    clock: Clock,
    private val scope: CoroutineScope,
) : AbstractPausableEngine(clock), TabataEngine {

    private val _events = MutableSharedFlow<TabataEvent>(extraBufferCapacity = 64)
    override val events: SharedFlow<TabataEvent> = _events

    private var job: Job? = null

    override fun start(config: TabataConfig) {
        job?.cancel()
        resetPauseState()
        val totalRounds = computeTotalRounds(config)
        job = scope.launch {
            val startTime = clock.currentTimeMillis()
            var phase = TabataPhase.Work
            // Effective elapsed at the start of the current phase (excludes pauses).
            var phaseStartElapsed = 0L
            // A round starts at each Work phase; the initial Work phase is round 1.
            var currentRound = 1

            // Emit the initial WorkStarted so the screen shows "WORK" immediately
            // and the audio player fires the high-pitch beep at T=0.
            _events.emit(TabataEvent.WorkStarted)

            while (isActive) {
                while (isPaused && isActive) {
                    delay(PAUSE_CHECK_MS)
                }

                if (isActive) {
                    val now = clock.currentTimeMillis()
                    val elapsed = now - startTime - totalPausedMs
                    val phaseDuration = if (phase == TabataPhase.Work) config.workMillis else config.restMillis
                    val phaseEnd = phaseStartElapsed + phaseDuration
                    val remainingInPhase = (phaseEnd - elapsed).coerceAtLeast(0L)

                    _events.emit(
                        TabataEvent.Tick(
                            phase = phase,
                            remainingInPhaseMillis = remainingInPhase,
                            elapsedMillis = elapsed,
                            currentRound = currentRound,
                            totalRounds = totalRounds,
                        )
                    )

                    if (elapsed >= phaseEnd) {
                        // Phase complete — advance the accumulated phase clock.
                        phaseStartElapsed += phaseDuration

                        // Check for workout completion BEFORE starting the next phase.
                        if (phaseStartElapsed >= config.totalDurationMillis) {
                            _events.emit(TabataEvent.WorkoutCompleted)
                            return@launch
                        }

                        // Switch phase and announce it.
                        phase = if (phase == TabataPhase.Work) TabataPhase.Rest else TabataPhase.Work
                        if (phase == TabataPhase.Work) {
                            currentRound++
                            _events.emit(TabataEvent.WorkStarted)
                        } else {
                            _events.emit(TabataEvent.RestStarted)
                        }
                    } else {
                        // Sleep until the earlier of the next UI tick or the end of this phase.
                        val absolutePhaseEnd = startTime + totalPausedMs + phaseEnd
                        val nextUiTick = now + TICK_MS
                        val sleepUntil = minOf(absolutePhaseEnd, nextUiTick)
                        val sleepMs = (sleepUntil - clock.currentTimeMillis()).coerceAtLeast(0L)
                        if (sleepMs > 0) delay(sleepMs)
                    }
                }
            }
        }
    }

    override fun stop() {
        job?.cancel()
        job = null
    }

    /**
     * Mirrors the phase-alternation loop above (without real-time delays) to determine
     * how many Work phases (rounds) this workout will run, consistent with the engine's
     * own "never cut a phase short" completion policy.
     */
    private fun computeTotalRounds(config: TabataConfig): Int {
        var phase = TabataPhase.Work
        var phaseStartElapsed = 0L
        var rounds = 0
        while (true) {
            if (phase == TabataPhase.Work) rounds++
            phaseStartElapsed += if (phase == TabataPhase.Work) config.workMillis else config.restMillis
            if (phaseStartElapsed >= config.totalDurationMillis) break
            phase = if (phase == TabataPhase.Work) TabataPhase.Rest else TabataPhase.Work
        }
        return rounds
    }

    private companion object {
        const val TICK_MS = 100L
        const val PAUSE_CHECK_MS = 50L
    }
}
