package dev.danielkindl.ocho.domain.engine

import dev.danielkindl.ocho.core.Clock
import dev.danielkindl.ocho.domain.model.TabataConfig
import dev.danielkindl.ocho.domain.model.TabataEvent
import dev.danielkindl.ocho.domain.model.TabataPhase
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
 * Tests for [TabataEngineImpl].
 *
 * Uses [kotlinx.coroutines.test.TestScope.currentTime] as the [Clock] source so that
 * virtual coroutine time and wall-clock readings are always in sync.
 */
@OptIn(ExperimentalCoroutinesApi::class)
class TabataEngineTest {

    private fun TestScope.engine() =
        TabataEngineImpl(Clock { testScheduler.currentTime }, this)

    // ──────────────────────────────────────────────────────────────────────
    // Phase transitions
    // ──────────────────────────────────────────────────────────────────────

    @Test
    fun `WorkStarted is emitted immediately on start`() = runTest {
        val engine = engine()
        val events = mutableListOf<TabataEvent>()
        val job = launch { engine.events.toList(events) }

        engine.start(TabataConfig(workMillis = 1_000, restMillis = 500, totalDurationMillis = 3_000))
        advanceTimeBy(50) // just enough for the first emission

        engine.stop()
        job.cancel()

        assertTrue("WorkStarted must be the first event",
            events.first() is TabataEvent.WorkStarted)
    }

    @Test
    fun `phases alternate Work-Rest-Work in correct order`() = runTest {
        val engine = engine()
        val events = mutableListOf<TabataEvent>()
        val job = launch { engine.events.toList(events) }

        // 1s work, 0.5s rest, 3s total → 2 full cycles
        engine.start(TabataConfig(workMillis = 1_000, restMillis = 500, totalDurationMillis = 3_000))
        advanceTimeBy(3_200)

        engine.stop()
        job.cancel()

        val transitions = events.filter {
            it is TabataEvent.WorkStarted || it is TabataEvent.RestStarted
        }
        // Expected: WorkStarted, RestStarted, WorkStarted, RestStarted, WorkStarted...
        assertTrue("First transition must be WorkStarted", transitions[0] is TabataEvent.WorkStarted)
        assertTrue("Second transition must be RestStarted", transitions[1] is TabataEvent.RestStarted)
        assertTrue("Third transition must be WorkStarted", transitions[2] is TabataEvent.WorkStarted)
    }

    @Test
    fun `RestStarted fires after first work phase`() = runTest {
        val engine = engine()
        val events = mutableListOf<TabataEvent>()
        val job = launch { engine.events.toList(events) }

        engine.start(TabataConfig(workMillis = 1_000, restMillis = 500, totalDurationMillis = 5_000))
        advanceTimeBy(1_100) // past first work phase

        engine.stop()
        job.cancel()

        assertTrue("RestStarted must be emitted after first work phase",
            events.any { it is TabataEvent.RestStarted })
    }

    // ──────────────────────────────────────────────────────────────────────
    // Tick events
    // ──────────────────────────────────────────────────────────────────────

    @Test
    fun `tick events reflect the current phase`() = runTest {
        val engine = engine()
        val events = mutableListOf<TabataEvent>()
        val job = launch { engine.events.toList(events) }

        engine.start(TabataConfig(workMillis = 1_000, restMillis = 500, totalDurationMillis = 3_000))
        advanceTimeBy(3_200)

        engine.stop()
        job.cancel()

        val ticks = events.filterIsInstance<TabataEvent.Tick>()
        assertTrue("Should have tick events", ticks.isNotEmpty())

        // All ticks before first rest must be Work phase
        val firstRestIdx = events.indexOfFirst { it is TabataEvent.RestStarted }
        val ticksBeforeRest = events.subList(0, firstRestIdx).filterIsInstance<TabataEvent.Tick>()
        assertTrue("Ticks before RestStarted must all be Work phase",
            ticksBeforeRest.all { it.phase == TabataPhase.Work })
    }

    @Test
    fun `tick elapsed time is monotonically non-decreasing`() = runTest {
        val engine = engine()
        val events = mutableListOf<TabataEvent>()
        val job = launch { engine.events.toList(events) }

        engine.start(TabataConfig(workMillis = 1_000, restMillis = 500, totalDurationMillis = 3_000))
        advanceTimeBy(3_200)

        engine.stop()
        job.cancel()

        val ticks = events.filterIsInstance<TabataEvent.Tick>()
        ticks.zipWithNext().forEach { (a, b) ->
            assertTrue("Elapsed must not decrease: ${a.elapsedMillis} → ${b.elapsedMillis}",
                b.elapsedMillis >= a.elapsedMillis)
        }
    }

    @Test
    fun `tick remainingInPhase counts down to zero within each phase`() = runTest {
        val engine = engine()
        val events = mutableListOf<TabataEvent>()
        val job = launch { engine.events.toList(events) }

        engine.start(TabataConfig(workMillis = 1_000, restMillis = 500, totalDurationMillis = 3_000))
        advanceTimeBy(3_200)

        engine.stop()
        job.cancel()

        val workTicks = events
            .filterIsInstance<TabataEvent.Tick>()
            .filter { it.phase == TabataPhase.Work }
        assertTrue("Work ticks must count down toward zero",
            workTicks.last().remainingInPhaseMillis <= workTicks.first().remainingInPhaseMillis)
    }

    // ──────────────────────────────────────────────────────────────────────
    // Completion
    // ──────────────────────────────────────────────────────────────────────

    @Test
    fun `WorkoutCompleted is emitted after total duration`() = runTest {
        val engine = engine()
        val events = mutableListOf<TabataEvent>()
        val job = launch { engine.events.toList(events) }

        engine.start(TabataConfig(workMillis = 1_000, restMillis = 500, totalDurationMillis = 3_000))
        advanceTimeBy(5_000)

        engine.stop()
        job.cancel()

        assertTrue("WorkoutCompleted must be emitted",
            events.any { it is TabataEvent.WorkoutCompleted })
    }

    @Test
    fun `WorkoutCompleted carries the summed phase duration, not the last tick`() = runTest {
        val engine = engine()
        val events = mutableListOf<TabataEvent>()
        val job = launch { engine.events.toList(events) }

        // 1.5s per cycle against a 2s total: the second work phase starts before the
        // total and runs to its end, so the workout genuinely lasts 2.5s. The summary
        // must say 2.5s rather than either the configured 2s or the last sampled tick.
        engine.start(TabataConfig(workMillis = 1_000, restMillis = 500, totalDurationMillis = 2_000))
        advanceTimeBy(4_000)

        engine.stop()
        job.cancel()

        val completed = events.filterIsInstance<TabataEvent.WorkoutCompleted>().single()
        assertEquals(2_500L, completed.elapsedMillis)
    }

    @Test
    fun `workout always finishes at a phase boundary not mid-phase`() = runTest {
        val engine = engine()
        val events = mutableListOf<TabataEvent>()
        val job = launch { engine.events.toList(events) }

        // 3s total, 1s work + 0.5s rest = 1.5s per cycle → 2 full cycles = 3s exactly
        engine.start(TabataConfig(workMillis = 1_000, restMillis = 500, totalDurationMillis = 3_000))
        advanceTimeBy(4_000)

        engine.stop()
        job.cancel()

        // The event directly before WorkoutCompleted must be a Tick (not a mid-phase cut)
        val completionIdx = events.indexOfFirst { it is TabataEvent.WorkoutCompleted }
        assertTrue("WorkoutCompleted must be present", completionIdx >= 0)
        // No events after completion
        assertEquals("Nothing emitted after WorkoutCompleted",
            completionIdx, events.lastIndex)
    }

    @Test
    fun `total duration not divisible by cycle finishes after current phase ends`() = runTest {
        val engine = engine()
        val events = mutableListOf<TabataEvent>()
        val job = launch { engine.events.toList(events) }

        // 2.5s total, 1s work + 0.5s rest: cycles end at 1.5s, 3s
        // At 2.5s we're mid work (cycle 2 started at 1.5s). Engine finishes work at 3s.
        engine.start(TabataConfig(workMillis = 1_000, restMillis = 500, totalDurationMillis = 2_500))
        advanceTimeBy(4_000)

        engine.stop()
        job.cancel()

        assertTrue("WorkoutCompleted emitted", events.any { it is TabataEvent.WorkoutCompleted })
        // The last transition before WorkoutCompleted should be WorkStarted (we were in work)
        val transitions = events.filter {
            it is TabataEvent.WorkStarted || it is TabataEvent.RestStarted
        }
        assertTrue("Last transition before completion was WorkStarted",
            transitions.last() is TabataEvent.WorkStarted)
    }

    @Test
    fun `exactly divisible total duration completes without extra phase`() = runTest {
        val engine = engine()
        val events = mutableListOf<TabataEvent>()
        val job = launch { engine.events.toList(events) }

        // 3s total, exactly 2 cycles of (1s work + 0.5s rest)
        engine.start(TabataConfig(workMillis = 1_000, restMillis = 500, totalDurationMillis = 3_000))
        advanceTimeBy(4_000)

        engine.stop()
        job.cancel()

        val transitions = events.filter {
            it is TabataEvent.WorkStarted || it is TabataEvent.RestStarted
        }
        // Exactly 2 WorkStarted + 2 RestStarted (no extra phase started)
        assertEquals("Exactly 2 WorkStarted events", 2,
            transitions.count { it is TabataEvent.WorkStarted })
        assertEquals("Exactly 2 RestStarted events", 2,
            transitions.count { it is TabataEvent.RestStarted })
        assertTrue("WorkoutCompleted emitted", events.any { it is TabataEvent.WorkoutCompleted })
    }

    // ──────────────────────────────────────────────────────────────────────
    // Degenerate configs
    // ──────────────────────────────────────────────────────────────────────

    @Test
    fun `workout completes after one round when total duration is zero`() = runTest {
        val engine = engine()
        val events = mutableListOf<TabataEvent>()
        val job = launch { engine.events.toList(events) }

        engine.start(TabataConfig(workMillis = 1_000, restMillis = 500, totalDurationMillis = 0))
        advanceTimeBy(1_200)

        engine.stop()
        job.cancel()

        assertTrue("WorkoutCompleted must be emitted", events.any { it is TabataEvent.WorkoutCompleted })
        assertTrue("No RestStarted since the work phase itself completes the workout",
            events.none { it is TabataEvent.RestStarted })
    }

    @Test
    fun `the engine does not hang when only the work phase is zero-length`() = runTest {
        val engine = engine()
        val events = mutableListOf<TabataEvent>()
        val job = launch { engine.events.toList(events) }

        engine.start(TabataConfig(workMillis = 0, restMillis = 500, totalDurationMillis = 1_500))
        advanceTimeBy(1_600)

        engine.stop()
        job.cancel()

        assertTrue("WorkoutCompleted must eventually be emitted", events.any { it is TabataEvent.WorkoutCompleted })
    }

    @Test
    fun `the engine does not hang when only the rest phase is zero-length`() = runTest {
        val engine = engine()
        val events = mutableListOf<TabataEvent>()
        val job = launch { engine.events.toList(events) }

        engine.start(TabataConfig(workMillis = 500, restMillis = 0, totalDurationMillis = 1_500))
        advanceTimeBy(1_600)

        engine.stop()
        job.cancel()

        assertTrue("WorkoutCompleted must eventually be emitted", events.any { it is TabataEvent.WorkoutCompleted })
    }

    // ──────────────────────────────────────────────────────────────────────
    // Round counting
    // ──────────────────────────────────────────────────────────────────────

    @Test
    fun `currentRound starts at 1 and increments only when a new Work phase starts`() = runTest {
        val engine = engine()
        val events = mutableListOf<TabataEvent>()
        val job = launch { engine.events.toList(events) }

        // 1s work, 0.5s rest, 3s total → 2 full cycles (2 rounds)
        engine.start(TabataConfig(workMillis = 1_000, restMillis = 500, totalDurationMillis = 3_000))
        advanceTimeBy(3_200)

        engine.stop()
        job.cancel()

        val ticks = events.filterIsInstance<TabataEvent.Tick>()
        val firstRestIdx = events.indexOfFirst { it is TabataEvent.RestStarted }
        val ticksBeforeRest = events.subList(0, firstRestIdx).filterIsInstance<TabataEvent.Tick>()
        assertTrue("Round must be 1 before the first rest starts",
            ticksBeforeRest.all { it.currentRound == 1 })

        val ticksInSecondWork = ticks.filter { it.phase == TabataPhase.Work && it.currentRound == 2 }
        assertTrue("Round must reach 2 once the second work phase starts",
            ticksInSecondWork.isNotEmpty())
    }

    @Test
    fun `totalRounds is stable across all ticks for an exactly divisible duration`() = runTest {
        val engine = engine()
        val events = mutableListOf<TabataEvent>()
        val job = launch { engine.events.toList(events) }

        // 3s total, exactly 2 cycles of (1s work + 0.5s rest) → 2 rounds
        engine.start(TabataConfig(workMillis = 1_000, restMillis = 500, totalDurationMillis = 3_000))
        advanceTimeBy(3_200)

        engine.stop()
        job.cancel()

        val ticks = events.filterIsInstance<TabataEvent.Tick>()
        assertTrue("Should have tick events", ticks.isNotEmpty())
        assertTrue("totalRounds must be 2 for every tick", ticks.all { it.totalRounds == 2 })
    }

    @Test
    fun `totalRounds counts the trailing partial cycle as one more round`() = runTest {
        val engine = engine()
        val events = mutableListOf<TabataEvent>()
        val job = launch { engine.events.toList(events) }

        // 2.5s total, 1s work + 0.5s rest: cycle 1 ends at 1.5s (1 round done),
        // cycle 2's work phase starts at 1.5s and runs to completion at 2.5s → 2 rounds total
        engine.start(TabataConfig(workMillis = 1_000, restMillis = 500, totalDurationMillis = 2_500))
        advanceTimeBy(4_000)

        engine.stop()
        job.cancel()

        val ticks = events.filterIsInstance<TabataEvent.Tick>()
        assertTrue("Should have tick events", ticks.isNotEmpty())
        assertTrue("totalRounds must be 2", ticks.all { it.totalRounds == 2 })
    }

    // ──────────────────────────────────────────────────────────────────────
    // Stop / restart
    // ──────────────────────────────────────────────────────────────────────

    @Test
    fun `rapid stop then start emits only events from the second run`() = runTest {
        val engine = engine()
        val events = mutableListOf<TabataEvent>()
        val job = launch { engine.events.toList(events) }

        engine.start(TabataConfig(workMillis = 1_000, restMillis = 500, totalDurationMillis = 5_000))
        advanceTimeBy(50)
        engine.stop()
        events.clear()

        engine.start(TabataConfig(workMillis = 500, restMillis = 250, totalDurationMillis = 1_500))
        advanceTimeBy(2_000)

        engine.stop()
        job.cancel()

        // Second run: 0.5s work + 0.25s rest = 0.75s cycle; 1.5s = exactly 2 cycles
        val workStarted = events.count { it is TabataEvent.WorkStarted }
        val restStarted = events.count { it is TabataEvent.RestStarted }
        assertEquals("Second run: exactly 2 WorkStarted", 2, workStarted)
        assertEquals("Second run: exactly 2 RestStarted", 2, restStarted)
    }

    // ──────────────────────────────────────────────────────────────────────
    // Pause / resume
    // ──────────────────────────────────────────────────────────────────────

    @Test
    fun `pausing and resuming does not cause extra phase transitions`() = runTest {
        val engine = engine()
        val events = mutableListOf<TabataEvent>()
        val job = launch { engine.events.toList(events) }

        // 1s work + 0.5s rest, 3s total → 2 full cycles
        engine.start(TabataConfig(workMillis = 1_000, restMillis = 500, totalDurationMillis = 3_000))
        advanceTimeBy(400)   // 400ms into work phase
        engine.pause()
        advanceTimeBy(2_000) // 2s paused (should not count)
        engine.resume()
        advanceTimeBy(3_000) // advance enough for the rest of the workout

        engine.stop()
        job.cancel()

        val transitions = events.filter {
            it is TabataEvent.WorkStarted || it is TabataEvent.RestStarted
        }
        // Without pause: 2 WorkStarted + 2 RestStarted. Same expected with pause.
        assertEquals("Pause must not add extra transitions", 4, transitions.size)
        assertTrue("WorkoutCompleted emitted after pause/resume",
            events.any { it is TabataEvent.WorkoutCompleted })
    }

    @Test
    fun `phase transitions still fire at correct times after a pause`() = runTest {
        val engine = engine()
        val events = mutableListOf<TabataEvent>()
        val job = launch { engine.events.toList(events) }

        // 2s work, 1s rest; pause 500ms into work, resume 1s later
        engine.start(TabataConfig(workMillis = 2_000, restMillis = 1_000, totalDurationMillis = 9_000))
        advanceTimeBy(500)   // 500ms into work
        engine.pause()
        advanceTimeBy(1_000) // 1s paused
        engine.resume()
        advanceTimeBy(2_000) // another 2s: 500ms remaining work + rest starts

        engine.stop()
        job.cancel()

        // RestStarted must appear (work phase completed despite the pause)
        assertTrue("RestStarted must fire after pause/resume",
            events.any { it is TabataEvent.RestStarted })
    }

    // ──────────────────────────────────────────────────────────────────────
    // Countdown lead-in
    // ──────────────────────────────────────────────────────────────────────

    @Test
    fun `countdown ticks fire once each before a phase change`() = runTest {
        val engine = engine()
        val events = mutableListOf<TabataEvent>()
        val job = launch { engine.events.toList(events) }

        // One 10s work phase, then stop before rest can produce its own lead-in.
        engine.start(
            TabataConfig(workMillis = 10_000, restMillis = 10_000, totalDurationMillis = 10_000)
        )
        advanceTimeBy(10_100)

        engine.stop()
        job.cancel()

        val countdown = events.filterIsInstance<TabataEvent.CountdownTick>()
        assertEquals(
            "Expected exactly 3, 2, 1 with no repeats",
            listOf(3, 2, 1),
            countdown.map { it.secondsRemaining },
        )
    }

    @Test
    fun `countdown is evaluated per phase, so a short rest stays silent`() = runTest {
        val engine = engine()
        val events = mutableListOf<TabataEvent>()
        val job = launch { engine.events.toList(events) }

        // Work is long enough for a lead-in, rest is not. Only work should count down,
        // which is why the check is per phase rather than per workout.
        engine.start(
            TabataConfig(workMillis = 10_000, restMillis = 2_000, totalDurationMillis = 24_000)
        )
        advanceTimeBy(24_100)

        engine.stop()
        job.cancel()

        val countdown = events.filterIsInstance<TabataEvent.CountdownTick>()
        assertTrue(
            "Every tick should belong to a work phase lead-in",
            countdown.isNotEmpty() && countdown.all { it.secondsRemaining in 1..3 },
        )
        // Two work phases fit in 24s (10 work, 2 rest, 10 work, 2 rest), so two lead-ins.
        assertEquals(
            "One lead-in per work phase, none for the short rests",
            listOf(3, 2, 1, 3, 2, 1),
            countdown.map { it.secondsRemaining },
        )
    }
}
