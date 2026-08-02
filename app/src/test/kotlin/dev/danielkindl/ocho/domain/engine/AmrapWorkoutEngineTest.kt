package dev.danielkindl.ocho.domain.engine

import dev.danielkindl.ocho.core.Clock
import dev.danielkindl.ocho.domain.model.AmrapConfig
import dev.danielkindl.ocho.domain.model.SessionCue
import dev.danielkindl.ocho.domain.model.SessionStatus
import dev.danielkindl.ocho.domain.model.WorkoutMode
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.toList
import kotlinx.coroutines.launch
import kotlinx.coroutines.test.TestScope
import kotlinx.coroutines.test.advanceTimeBy
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

/**
 * Tests for [AmrapWorkoutEngine].
 *
 * The engine adds no timing of its own, so this covers only what it changes about the
 * timer engine it delegates to: the suppressed boundary cue and the absent round
 * count. The drift-free scheduling stays covered by [TimerEngineTest].
 *
 * Same virtual-clock arrangement as the other engine tests. The [Clock] reads the test
 * scheduler, so one `advanceTimeBy` drives both the delays and the elapsed-time maths.
 */
@OptIn(ExperimentalCoroutinesApi::class)
class AmrapWorkoutEngineTest {

    /**
     * Builds an engine on [TestScope.backgroundScope].
     *
     * The engine collects a [kotlinx.coroutines.flow.SharedFlow] that never completes,
     * so on the test's own scope `runTest` would wait forever for that child. The
     * background scope is cancelled when the test body ends, which is what stopping a
     * session does in production too.
     */
    private fun TestScope.amrapEngine(totalMillis: Long): AmrapWorkoutEngine {
        val clock = Clock { testScheduler.currentTime }
        return AmrapWorkoutEngine(
            config = AmrapConfig(totalDurationMillis = totalMillis),
            engineFactory = { scope: CoroutineScope -> TimerEngineImpl(clock, scope) },
            scope = backgroundScope,
        )
    }

    @Test
    fun `emits no interval boundary cue`() = runTest {
        val engine = amrapEngine(totalMillis = TOTAL_MILLIS)
        val cues = mutableListOf<SessionCue>()
        val job = launch { engine.cues.toList(cues) }

        engine.start()
        advanceTimeBy(PAST_END_MILLIS)
        engine.stop()
        job.cancel()

        // The single interval ends exactly when the workout does. Left in place it
        // would beep alongside the completion tone, which is what this suppresses.
        assertTrue(
            "AMRAP must announce no boundaries, got $cues",
            cues.none { it is SessionCue.IntervalBoundary },
        )
    }

    @Test
    fun `emits the completion cue exactly once`() = runTest {
        val engine = amrapEngine(totalMillis = TOTAL_MILLIS)
        val cues = mutableListOf<SessionCue>()
        val job = launch { engine.cues.toList(cues) }

        engine.start()
        advanceTimeBy(PAST_END_MILLIS)
        engine.stop()
        job.cancel()

        assertEquals(1, cues.count { it is SessionCue.Completed })
    }

    @Test
    fun `emits the three two one lead-in before finishing`() = runTest {
        val engine = amrapEngine(totalMillis = TOTAL_MILLIS)
        val cues = mutableListOf<SessionCue>()
        val job = launch { engine.cues.toList(cues) }

        engine.start()
        advanceTimeBy(PAST_END_MILLIS)
        engine.stop()
        job.cancel()

        // The lead-in comes free from the timer engine, counting down to the finish
        // rather than to an interval boundary, which is where an AMRAP wants it.
        val countdown = cues.filterIsInstance<SessionCue.Countdown>().map { it.secondsRemaining }
        assertEquals(listOf(3, 2, 1), countdown)
    }

    @Test
    fun `reports no round count`() = runTest {
        val engine = amrapEngine(totalMillis = TOTAL_MILLIS)

        engine.start()
        advanceTimeBy(TOTAL_MILLIS / 2)

        // Zero total rounds is what makes the session screen omit the counter.
        val snapshot = engine.snapshots.value
        assertEquals(WorkoutMode.AMRAP, snapshot.mode)
        assertEquals(0, snapshot.totalRounds)

        engine.stop()
    }

    @Test
    fun `reaches the completed status`() = runTest {
        val engine = amrapEngine(totalMillis = TOTAL_MILLIS)

        engine.start()
        advanceTimeBy(PAST_END_MILLIS)

        assertEquals(SessionStatus.Completed, engine.snapshots.value.status)
    }

    @Test
    fun `reports the full duration on completion, not the last sampled tick`() = runTest {
        // Regression: the completion summary read whatever the final Tick happened to
        // carry, and ticks land up to one tick before the end. Truncated to whole
        // seconds that made a 10 second workout report 9. Ten seconds is in the table
        // because that is the length at which a sub-second error becomes a whole
        // number the user can see.
        val totals = listOf(10_000L, 60_000L, 20 * 60 * 1_000L)
        val engines = totals.map { amrapEngine(totalMillis = it) }

        engines.forEach { it.start() }
        advanceTimeBy(totals.max() + 500)

        assertEquals(totals, engines.map { it.snapshots.value.elapsedMillis })
    }

    private companion object {
        const val TOTAL_MILLIS = 10_000L

        /** Far enough past the end that the final tick and completion have both landed. */
        const val PAST_END_MILLIS = 10_200L
    }
}
