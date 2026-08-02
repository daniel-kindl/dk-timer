package dev.danielkindl.ocho.domain.engine

import dev.danielkindl.ocho.core.Clock
import dev.danielkindl.ocho.domain.model.Phase
import dev.danielkindl.ocho.domain.model.SessionRequest
import dev.danielkindl.ocho.domain.model.TabataConfig
import dev.danielkindl.ocho.domain.model.TabataEvent
import dev.danielkindl.ocho.domain.model.TabataPhase
import dev.danielkindl.ocho.domain.model.toPlan
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.SharedFlow
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch
import kotlin.math.ceil

/**
 * Drift-free Tabata engine that alternates WORK and REST phases.
 *
 * Timing is anchored to the real clock (not accumulated delays) using the
 * same pause-safe approach as [TimerEngineImpl]: effective elapsed time is
 * `now - startTime - totalPausedMs`.
 *
 * **Completion policy**: the workout ends only at a phase boundary, never mid-phase.
 * The plan already encodes that rule — it stops alternating at the first phase whose
 * end reaches [TabataConfig.totalDurationMillis] — so this engine simply runs out of
 * segments and reports the time they actually took.
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
        val plan = SessionRequest.Tabata(config).toPlan()
        val segments = plan.segments
        job = scope.launch {
            val startTime = clock.currentTimeMillis()
            // Which planned phase is running. The plan is never empty, so this always
            // indexes something, and running off its end is what completion means.
            var index = 0
            // Effective elapsed at the start of the current phase (excludes pauses).
            var phaseStartElapsed = 0L
            // A round starts at each Work phase; the initial Work phase is round 1.
            var currentRound = 1
            var lastCountdownSecond = 0

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
                    val segment = segments[index]
                    val phaseDuration = segment.durationMillis
                    val phaseEnd = phaseStartElapsed + phaseDuration
                    val remainingInPhase = (phaseEnd - elapsed).coerceAtLeast(0L)

                    lastCountdownSecond =
                        emitCountdownIfDue(phaseDuration, remainingInPhase, lastCountdownSecond)

                    _events.emit(
                        TabataEvent.Tick(
                            phase = segment.phase.toTabataPhase(),
                            remainingInPhaseMillis = remainingInPhase,
                            elapsedMillis = elapsed,
                            currentRound = currentRound,
                            totalRounds = plan.totalRounds,
                        )
                    )

                    if (elapsed >= phaseEnd) {
                        // Phase complete — advance the accumulated phase clock.
                        phaseStartElapsed += phaseDuration
                        // The countdown belonged to the phase that just ended.
                        lastCountdownSecond = 0

                        // Check for workout completion BEFORE starting the next phase.
                        index++
                        if (index == segments.size) {
                            // phaseStartElapsed, not the configured total: phases run to
                            // their end, so a workout whose last phase crosses the total
                            // genuinely lasts longer than configured, and the summary
                            // should say so. It is also exact rather than sampled.
                            _events.emit(TabataEvent.WorkoutCompleted(phaseStartElapsed))
                            return@launch
                        }

                        // Announce whatever the plan says comes next.
                        if (segments[index].phase == Phase.WORK) {
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
     * Emits one lead-in tick if the phase has crossed into a new whole second.
     *
     * Evaluated per phase, since work and rest can differ in length: a rest short
     * enough to be covered entirely by the lead-in should not count itself down.
     *
     * @return the second just emitted, or [lastSecond] unchanged if nothing was due.
     */
    private suspend fun emitCountdownIfDue(
        phaseDuration: Long,
        remainingInPhase: Long,
        lastSecond: Int,
    ): Int {
        if (phaseDuration <= COUNTDOWN_LEAD_MILLIS) return lastSecond
        val seconds = ceil(remainingInPhase.toDouble() / MILLIS_PER_SECOND).toInt()
        if (seconds !in 1..COUNTDOWN_LEAD_SECONDS || seconds == lastSecond) return lastSecond
        _events.emit(TabataEvent.CountdownTick(seconds))
        return seconds
    }

    /**
     * Narrows the domain-wide phase to the two this engine can be in.
     *
     * The plan speaks in [Phase] because every consumer of it does; [TabataEvent]
     * keeps [TabataPhase] because that is what a Tabata tick has always carried. Only
     * WORK and REST are ever planned, so nothing is lost across the boundary.
     */
    private fun Phase.toTabataPhase(): TabataPhase =
        if (this == Phase.WORK) TabataPhase.Work else TabataPhase.Rest

    private companion object {
        const val TICK_MS = 100L
        const val PAUSE_CHECK_MS = 50L

        /** How many seconds of lead-in precede each phase change. */
        const val COUNTDOWN_LEAD_SECONDS = 3
        const val MILLIS_PER_SECOND = 1_000L
        const val COUNTDOWN_LEAD_MILLIS = COUNTDOWN_LEAD_SECONDS * MILLIS_PER_SECOND
    }
}
