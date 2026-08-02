package dev.danielkindl.ocho.domain.engine

import dev.danielkindl.ocho.core.Clock
import dev.danielkindl.ocho.domain.model.TimerConfig
import dev.danielkindl.ocho.domain.model.TimerEvent
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.SharedFlow
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch
import kotlin.math.ceil

/**
 * Drift-free timer engine based on system clock anchoring.
 *
 * All interval boundaries are calculated as absolute timestamps from [startTime],
 * never by accumulating delays. If the system is lagged, missed interval events
 * are emitted in one burst before the next delay is calculated.
 *
 * Pause/resume works by tracking total accumulated pause duration and subtracting
 * it from the elapsed time, preserving drift-free behaviour across pauses.
 */
class TimerEngineImpl(
    clock: Clock,
    private val scope: CoroutineScope,
) : AbstractPausableEngine(clock), TimerEngine {

    private val _events = MutableSharedFlow<TimerEvent>(extraBufferCapacity = 64)
    override val events: SharedFlow<TimerEvent> = _events

    private var job: Job? = null

    override fun start(config: TimerConfig) {
        job?.cancel()
        resetPauseState()
        job = scope.launch {
            val startTime = clock.currentTimeMillis()
            val totalIntervals = ceil(
                config.totalDurationMillis.toDouble() / config.intervalMillis
            ).toInt()
            var lastCompletedInterval = 0
            var lastCountdownSecond = 0

            while (isActive) {
                // Suspend cheaply while paused; re-check on every tick.
                while (isPaused && isActive) {
                    delay(PAUSE_CHECK_MS)
                }
                if (!isActive) break

                val now = clock.currentTimeMillis()
                val elapsed = now - startTime - totalPausedMs

                // intervalMillis == 0 would divide by zero here; unreachable because
                // TimerConfig's init block requires intervalMillis > 0.
                val completedIntervals = (elapsed / config.intervalMillis).toInt()

                // Emit interval events before the completion check so that the final
                // interval always fires even when it coincides with totalDurationMillis.
                for (i in (lastCompletedInterval + 1)..completedIntervals) {
                    _events.emit(TimerEvent.IntervalCompleted(i))
                }
                if (completedIntervals != lastCompletedInterval) {
                    // New interval: the previous countdown is spent.
                    lastCountdownSecond = 0
                }
                lastCompletedInterval = completedIntervals

                if (elapsed >= config.totalDurationMillis) {
                    // Reports the configured total, not the sampled `elapsed`. The loop
                    // wakes at or just after the end, so `elapsed` carries whatever
                    // scheduling jitter the device contributed, and a summary should not
                    // vary by device. The workout ran its full length; that is what
                    // completing means.
                    _events.emit(TimerEvent.WorkoutCompleted(config.totalDurationMillis))
                    return@launch
                }

                // The boundary the user is counting toward is whichever comes first: the
                // next interval, or the end of the workout. They differ whenever the total
                // is not a multiple of the interval, and whenever the interval is longer
                // than the whole workout, whose boundary never arrives at all.
                val workoutEnd = startTime + totalPausedMs + config.totalDurationMillis
                val intervalStart =
                    startTime + totalPausedMs + completedIntervals * config.intervalMillis
                val nextIntervalAt = minOf(intervalStart + config.intervalMillis, workoutEnd)
                val remainingInInterval = (nextIntervalAt - now).coerceAtLeast(0L)

                // A lead-in only means something if the stretch it precedes is longer than
                // it is; at or below it the countdown would run continuously and convey
                // nothing. Measured per interval rather than from the configured length,
                // because a truncated final one can fall below the lead-in while every
                // full interval before it sits well above.
                val countdownEnabled = (nextIntervalAt - intervalStart) > COUNTDOWN_LEAD_MILLIS
                lastCountdownSecond =
                    emitCountdownIfDue(countdownEnabled, remainingInInterval, lastCountdownSecond)

                _events.emit(
                    TimerEvent.Tick(
                        elapsedMillis = elapsed,
                        remainingInInterval = remainingInInterval,
                        currentInterval = completedIntervals + 1,
                        totalIntervals = totalIntervals,
                    )
                )

                // nextIntervalAt is already capped at the workout's end, so it stands in
                // for both boundaries here.
                val nextUiTick = now + TICK_MS
                val sleepUntil = minOf(nextIntervalAt, nextUiTick)
                val sleepMs = (sleepUntil - clock.currentTimeMillis()).coerceAtLeast(0L)
                if (sleepMs > 0) delay(sleepMs)
            }
        }
    }

    override fun stop() {
        job?.cancel()
        job = null
    }

    /**
     * Emits one lead-in tick if the interval has crossed into a new whole second.
     *
     * @return the second just emitted, or [lastSecond] unchanged if nothing was due.
     */
    private suspend fun emitCountdownIfDue(
        enabled: Boolean,
        remainingInInterval: Long,
        lastSecond: Int,
    ): Int {
        if (!enabled) return lastSecond
        val seconds = ceil(remainingInInterval.toDouble() / MILLIS_PER_SECOND).toInt()
        if (seconds !in 1..COUNTDOWN_LEAD_SECONDS || seconds == lastSecond) return lastSecond
        _events.emit(TimerEvent.CountdownTick(seconds))
        return seconds
    }

    private companion object {
        const val TICK_MS = 100L
        const val PAUSE_CHECK_MS = 50L

        /** How many seconds of lead-in precede each boundary. */
        const val COUNTDOWN_LEAD_SECONDS = 3
        const val MILLIS_PER_SECOND = 1_000L
        const val COUNTDOWN_LEAD_MILLIS = COUNTDOWN_LEAD_SECONDS * MILLIS_PER_SECOND
    }
}
