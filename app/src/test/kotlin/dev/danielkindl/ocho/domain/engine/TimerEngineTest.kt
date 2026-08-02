package dev.danielkindl.ocho.domain.engine

import dev.danielkindl.ocho.core.Clock
import dev.danielkindl.ocho.domain.model.TimerConfig
import dev.danielkindl.ocho.domain.model.TimerEvent
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.toList
import kotlinx.coroutines.launch
import kotlinx.coroutines.test.advanceTimeBy
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

/**
 * Tests for [TimerEngineImpl].
 *
 * Uses [kotlinx.coroutines.test.TestScope.currentTime] as the [Clock] source so that
 * virtual coroutine time and wall-clock readings are always in sync.  A single
 * [advanceTimeBy] call is enough to drive both the [delay] calls inside the engine
 * and the elapsed-time calculations.
 */
@OptIn(ExperimentalCoroutinesApi::class)
class TimerEngineTest {

    // ──────────────────────────────────────────────────────────────────────
    // Tests
    // ──────────────────────────────────────────────────────────────────────

    @Test
    fun `interval events fire at correct multiples`() = runTest {
        val engine = TimerEngineImpl(Clock { testScheduler.currentTime }, this)
        val events = mutableListOf<TimerEvent>()
        val job = launch { engine.events.toList(events) }

        engine.start(TimerConfig(intervalMillis = 1_000, totalDurationMillis = 3_000))
        advanceTimeBy(3_100)

        engine.stop()
        job.cancel()

        val completed = events.filterIsInstance<TimerEvent.IntervalCompleted>()
        assertEquals("Expected 3 interval events", 3, completed.size)
        assertEquals(listOf(1, 2, 3), completed.map { it.intervalNumber })
    }

    @Test
    fun `workout completes after total duration`() = runTest {
        val engine = TimerEngineImpl(Clock { testScheduler.currentTime }, this)
        val events = mutableListOf<TimerEvent>()
        val job = launch { engine.events.toList(events) }

        engine.start(TimerConfig(intervalMillis = 500, totalDurationMillis = 2_000))
        advanceTimeBy(2_200)

        engine.stop()
        job.cancel()

        assertTrue("WorkoutCompleted must be emitted", events.any { it is TimerEvent.WorkoutCompleted })
    }

    @Test
    fun `WorkoutCompleted carries the configured total, not the last tick`() = runTest {
        // Ticks are samples and the last one lands up to a tick before the end, so a
        // summary built from it reports the workout a second short once truncated.
        val engine = TimerEngineImpl(Clock { testScheduler.currentTime }, this)
        val events = mutableListOf<TimerEvent>()
        val job = launch { engine.events.toList(events) }

        engine.start(TimerConfig(intervalMillis = 1_000, totalDurationMillis = 10_000))
        advanceTimeBy(10_500)

        engine.stop()
        job.cancel()

        val completed = events.filterIsInstance<TimerEvent.WorkoutCompleted>().single()
        assertEquals(10_000L, completed.elapsedMillis)

        val lastTick = events.filterIsInstance<TimerEvent.Tick>().last()
        assertTrue(
            "the last tick must fall short of the end, or this test proves nothing",
            lastTick.elapsedMillis < 10_000L,
        )
    }

    @Test
    fun `no interval events when interval exceeds total duration`() = runTest {
        val engine = TimerEngineImpl(Clock { testScheduler.currentTime }, this)
        val events = mutableListOf<TimerEvent>()
        val job = launch { engine.events.toList(events) }

        engine.start(TimerConfig(intervalMillis = 5_000, totalDurationMillis = 3_000))
        advanceTimeBy(3_200)

        engine.stop()
        job.cancel()

        val intervalEvents = events.filterIsInstance<TimerEvent.IntervalCompleted>()
        assertTrue("No interval events when interval > total", intervalEvents.isEmpty())
        assertTrue("WorkoutCompleted still fires", events.any { it is TimerEvent.WorkoutCompleted })
    }

    @Test
    fun `non-divisible duration fires only complete intervals`() = runTest {
        val engine = TimerEngineImpl(Clock { testScheduler.currentTime }, this)
        val events = mutableListOf<TimerEvent>()
        val job = launch { engine.events.toList(events) }

        // 65s total, 20s interval → beeps at 20s, 40s, 60s; no beep at 65s
        engine.start(TimerConfig(intervalMillis = 20_000, totalDurationMillis = 65_000))
        advanceTimeBy(66_000)

        engine.stop()
        job.cancel()

        val intervals = events.filterIsInstance<TimerEvent.IntervalCompleted>()
        assertEquals("Expected exactly 3 interval events", 3, intervals.size)
        assertEquals(listOf(1, 2, 3), intervals.map { it.intervalNumber })
        assertTrue("WorkoutCompleted fires", events.any { it is TimerEvent.WorkoutCompleted })
    }

    @Test
    fun `interval equals total duration fires one interval then completes`() = runTest {
        val engine = TimerEngineImpl(Clock { testScheduler.currentTime }, this)
        val events = mutableListOf<TimerEvent>()
        val job = launch { engine.events.toList(events) }

        engine.start(TimerConfig(intervalMillis = 1_000, totalDurationMillis = 1_000))
        advanceTimeBy(1_200)

        engine.stop()
        job.cancel()

        val intervalEvents = events.filterIsInstance<TimerEvent.IntervalCompleted>()
        assertEquals("Exactly one interval event", 1, intervalEvents.size)
        assertTrue("WorkoutCompleted fires", events.any { it is TimerEvent.WorkoutCompleted })
    }

    @Test
    fun `tick events carry increasing elapsed time`() = runTest {
        val engine = TimerEngineImpl(Clock { testScheduler.currentTime }, this)
        val events = mutableListOf<TimerEvent>()
        val job = launch { engine.events.toList(events) }

        engine.start(TimerConfig(intervalMillis = 1_000, totalDurationMillis = 2_000))
        advanceTimeBy(2_200)

        engine.stop()
        job.cancel()

        val ticks = events.filterIsInstance<TimerEvent.Tick>()
        assertTrue("Should have tick events", ticks.isNotEmpty())
        // Elapsed time must be monotonically non-decreasing
        ticks.zipWithNext().forEach { (a, b) ->
            assertTrue("Elapsed must not decrease: ${a.elapsedMillis} → ${b.elapsedMillis}",
                b.elapsedMillis >= a.elapsedMillis)
        }
    }

    @Test
    fun `rapid stop then start does not emit events from previous run`() = runTest {
        val engine = TimerEngineImpl(Clock { testScheduler.currentTime }, this)
        val events = mutableListOf<TimerEvent>()
        val job = launch { engine.events.toList(events) }

        engine.start(TimerConfig(intervalMillis = 1_000, totalDurationMillis = 5_000))
        advanceTimeBy(50)
        engine.stop()

        events.clear()

        engine.start(TimerConfig(intervalMillis = 500, totalDurationMillis = 1_000))
        advanceTimeBy(1_200)

        engine.stop()
        job.cancel()

        val intervals = events.filterIsInstance<TimerEvent.IntervalCompleted>()
        // Second run: 1s total, 0.5s interval → max 2 interval events
        assertTrue("Interval events from second run only (≤ 2)", intervals.size <= 2)
        assertFalse("No ghost events with number > 2", intervals.any { it.intervalNumber > 2 })
    }

    @Test
    fun `totalIntervals is correctly computed in tick events`() = runTest {
        val engine = TimerEngineImpl(Clock { testScheduler.currentTime }, this)
        val events = mutableListOf<TimerEvent>()
        val job = launch { engine.events.toList(events) }

        engine.start(TimerConfig(intervalMillis = 1_000, totalDurationMillis = 4_000))
        advanceTimeBy(200)

        engine.stop()
        job.cancel()

        val tick = events.filterIsInstance<TimerEvent.Tick>().first()
        assertEquals("totalIntervals should be 4", 4, tick.totalIntervals)
    }

    // Note: totalDurationMillis = 0 / intervalMillis = 0 are not tested here — both are
    // rejected by TimerConfig's own init block (require(... > 0)), so they can't be
    // constructed at all, let alone reach the engine.

    // ──────────────────────────────────────────────────────────────────────
    // Pause / resume
    // ──────────────────────────────────────────────────────────────────────

    @Test
    fun `pausing and resuming does not cause extra interval events`() = runTest {
        val engine = TimerEngineImpl(Clock { testScheduler.currentTime }, this)
        val events = mutableListOf<TimerEvent>()
        val job = launch { engine.events.toList(events) }

        engine.start(TimerConfig(intervalMillis = 1_000, totalDurationMillis = 5_000))
        advanceTimeBy(400)   // 400ms into first interval
        engine.pause()
        advanceTimeBy(2_000) // 2s paused (should not count)
        engine.resume()
        advanceTimeBy(5_000) // advance enough for the rest of the workout

        engine.stop()
        job.cancel()

        val intervals = events.filterIsInstance<TimerEvent.IntervalCompleted>()
        assertEquals("Pause must not add extra interval events", 5, intervals.size)
        assertEquals(listOf(1, 2, 3, 4, 5), intervals.map { it.intervalNumber })
    }

    @Test
    fun `interval fires at correct time after a pause`() = runTest {
        val engine = TimerEngineImpl(Clock { testScheduler.currentTime }, this)
        val events = mutableListOf<TimerEvent>()
        val job = launch { engine.events.toList(events) }

        // 2s interval; pause 500ms in, resume 1s later. The interval boundary must
        // land at 2s of *active* time (500 + 1500 active), unshifted by the 1s pause.
        engine.start(TimerConfig(intervalMillis = 2_000, totalDurationMillis = 9_000))
        advanceTimeBy(500)   // 500ms into interval
        engine.pause()
        advanceTimeBy(1_000) // 1s paused
        engine.resume()
        advanceTimeBy(1_600) // 500 + 1600 = 2100ms active elapsed → boundary crossed once

        engine.stop()
        job.cancel()

        val intervals = events.filterIsInstance<TimerEvent.IntervalCompleted>()
        assertEquals("Exactly one interval event despite the pause", 1, intervals.size)
    }

    @Test
    fun `countdown ticks fire once each at three, two and one seconds`() = runTest {
        val engine = TimerEngineImpl(Clock { testScheduler.currentTime }, this)
        val events = mutableListOf<TimerEvent>()
        val job = launch { engine.events.toList(events) }

        // One 10s interval, so exactly one lead-in occurs.
        engine.start(TimerConfig(intervalMillis = 10_000, totalDurationMillis = 10_000))
        advanceTimeBy(10_100)

        engine.stop()
        job.cancel()

        val countdown = events.filterIsInstance<TimerEvent.CountdownTick>()
        assertEquals(
            "Expected exactly 3, 2, 1 with no repeats",
            listOf(3, 2, 1),
            countdown.map { it.secondsRemaining },
        )
    }

    @Test
    fun `countdown is suppressed when the interval is no longer than the lead-in`() = runTest {
        val engine = TimerEngineImpl(Clock { testScheduler.currentTime }, this)
        val events = mutableListOf<TimerEvent>()
        val job = launch { engine.events.toList(events) }

        // A 2s interval is shorter than the 3s lead-in. Counting down continuously
        // would convey nothing, so nothing should be emitted at all.
        engine.start(TimerConfig(intervalMillis = 2_000, totalDurationMillis = 6_000))
        advanceTimeBy(6_100)

        engine.stop()
        job.cancel()

        assertTrue(
            "No countdown ticks for an interval at or below the lead-in",
            events.filterIsInstance<TimerEvent.CountdownTick>().isEmpty(),
        )
    }

    @Test
    fun `countdown restarts cleanly for each interval`() = runTest {
        val engine = TimerEngineImpl(Clock { testScheduler.currentTime }, this)
        val events = mutableListOf<TimerEvent>()
        val job = launch { engine.events.toList(events) }

        // Two 10s intervals: the lead-in must run once per interval, not once total.
        engine.start(TimerConfig(intervalMillis = 10_000, totalDurationMillis = 20_000))
        advanceTimeBy(20_100)

        engine.stop()
        job.cancel()

        val countdown = events.filterIsInstance<TimerEvent.CountdownTick>()
        assertEquals(
            "Expected a full lead-in per interval",
            listOf(3, 2, 1, 3, 2, 1),
            countdown.map { it.secondsRemaining },
        )
    }

    @Test
    fun `the lead-in counts toward the end of a truncated final interval`() = runTest {
        val engine = TimerEngineImpl(Clock { testScheduler.currentTime }, this)
        val events = mutableListOf<TimerEvent>()
        val job = launch { engine.events.toList(events) }

        // 65s at 20s: boundaries at 20, 40 and 60, then a 5s remainder. The lead-in used
        // to be measured against the boundary at 80s, which this workout never reaches,
        // so the finish arrived unannounced. The remainder is longer than the lead-in and
        // gets one of its own.
        engine.start(TimerConfig(intervalMillis = 20_000, totalDurationMillis = 65_000))
        advanceTimeBy(65_100)

        engine.stop()
        job.cancel()

        val countdown = events.filterIsInstance<TimerEvent.CountdownTick>()
        assertEquals(
            "Three boundaries plus the finish, each with a full lead-in",
            listOf(3, 2, 1, 3, 2, 1, 3, 2, 1, 3, 2, 1),
            countdown.map { it.secondsRemaining },
        )
    }

    @Test
    fun `the remaining figure counts toward the end, not toward a boundary beyond it`() = runTest {
        val engine = TimerEngineImpl(Clock { testScheduler.currentTime }, this)
        val events = mutableListOf<TimerEvent>()
        val job = launch { engine.events.toList(events) }

        // Same 65s at 20s. The numeral used to read 0:15 as the workout ended, counting
        // toward the boundary at 80s. It must count the 5s that actually remain.
        engine.start(TimerConfig(intervalMillis = 20_000, totalDurationMillis = 65_000))
        advanceTimeBy(65_100)

        engine.stop()
        job.cancel()

        val lastTick = events.filterIsInstance<TimerEvent.Tick>().last()
        assertTrue(
            "The last tick must be counting down the remainder, got ${lastTick.remainingInInterval}",
            lastTick.remainingInInterval <= 1_000L,
        )
    }

    @Test
    fun `a final interval no longer than the lead-in stays silent`() = runTest {
        val engine = TimerEngineImpl(Clock { testScheduler.currentTime }, this)
        val events = mutableListOf<TimerEvent>()
        val job = launch { engine.events.toList(events) }

        // 63s at 20s leaves a 3s remainder, which is not longer than the lead-in. This is
        // the row that proves the gate is evaluated per interval: capping the boundary
        // alone would leave it keyed to the configured 20s and count this one down.
        engine.start(TimerConfig(intervalMillis = 20_000, totalDurationMillis = 63_000))
        advanceTimeBy(63_100)

        engine.stop()
        job.cancel()

        val countdown = events.filterIsInstance<TimerEvent.CountdownTick>()
        assertEquals(
            "Three boundaries count down; the 3s remainder does not",
            listOf(3, 2, 1, 3, 2, 1, 3, 2, 1),
            countdown.map { it.secondsRemaining },
        )
    }

    @Test
    fun `no lead-in when the whole workout is no longer than it`() = runTest {
        val engine = TimerEngineImpl(Clock { testScheduler.currentTime }, this)
        val events = mutableListOf<TimerEvent>()
        val job = launch { engine.events.toList(events) }

        // A 5s interval against a 3s total: the interval never completes, so the workout
        // is one 3s stretch. It used to emit a lone 3 with no 2 and no 1, because the
        // gate read the configured 5s while the remainder was measured to the boundary.
        engine.start(TimerConfig(intervalMillis = 5_000, totalDurationMillis = 3_000))
        advanceTimeBy(3_100)

        engine.stop()
        job.cancel()

        assertTrue(
            "A workout at or below the lead-in counts down not at all",
            events.filterIsInstance<TimerEvent.CountdownTick>().isEmpty(),
        )
    }

    @Test
    fun `the lead-in fires when the interval outlasts the workout but the workout is long enough`() =
        runTest {
            val engine = TimerEngineImpl(Clock { testScheduler.currentTime }, this)
            val events = mutableListOf<TimerEvent>()
            val job = launch { engine.events.toList(events) }

            // A 10s interval against a 5s total. The boundary never arrives, but 5s is
            // longer than the lead-in, so the finish is announced. This was silent before.
            engine.start(TimerConfig(intervalMillis = 10_000, totalDurationMillis = 5_000))
            advanceTimeBy(5_100)

            engine.stop()
            job.cancel()

            val countdown = events.filterIsInstance<TimerEvent.CountdownTick>()
            assertEquals(listOf(3, 2, 1), countdown.map { it.secondsRemaining })
        }

    @Test
    fun `countdown does not duplicate across a pause`() = runTest {
        val engine = TimerEngineImpl(Clock { testScheduler.currentTime }, this)
        val events = mutableListOf<TimerEvent>()
        val job = launch { engine.events.toList(events) }

        // Pause inside the lead-in. Resuming must continue the countdown rather than
        // replay the second it was already on.
        engine.start(TimerConfig(intervalMillis = 10_000, totalDurationMillis = 10_000))
        advanceTimeBy(7_500) // 2.5s remaining, so 3 has already fired
        engine.pause()
        advanceTimeBy(5_000)
        engine.resume()
        advanceTimeBy(2_600)

        engine.stop()
        job.cancel()

        val countdown = events.filterIsInstance<TimerEvent.CountdownTick>()
        assertEquals(
            "Each second fires exactly once across the pause",
            listOf(3, 2, 1),
            countdown.map { it.secondsRemaining },
        )
    }
}

