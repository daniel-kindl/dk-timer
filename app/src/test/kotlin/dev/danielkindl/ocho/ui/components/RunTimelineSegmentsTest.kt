package dev.danielkindl.ocho.ui.components

import dev.danielkindl.ocho.domain.model.AmrapConfig
import dev.danielkindl.ocho.domain.model.Phase
import dev.danielkindl.ocho.domain.model.SessionRequest
import dev.danielkindl.ocho.domain.model.TabataConfig
import dev.danielkindl.ocho.domain.model.TimerConfig
import dev.danielkindl.ocho.domain.model.toPlan
import org.junit.Assert.assertEquals
import org.junit.Assert.assertThrows
import org.junit.Assert.assertTrue
import org.junit.Test

/**
 * Tests for the strip [RunTimeline] draws.
 *
 * The timeline is the one place a user sees the shape of a workout before starting
 * it, so a wrong preview is a wrong promise. It no longer derives that shape itself —
 * it renders the plan the session will run — which turns most of the old agreement
 * problem into a type-level fact. What remains worth pinning is the framing the
 * preview adds on top: the prepare lead and the completion cap.
 */
class RunTimelineSegmentsTest {

    private val prepare = 10_000L

    private fun emom(intervalMillis: Long, totalMillis: Long) =
        SessionRequest.Emom(TimerConfig(intervalMillis, totalMillis)).toPlan()

    private fun tabata(workMillis: Long, restMillis: Long, totalMillis: Long) =
        SessionRequest.Tabata(TabataConfig(workMillis, restMillis, totalMillis)).toPlan()

    @Test
    fun `an EMOM preview is prepare, one work block, then the completion cap`() {
        val segments = emom(60_000, 20 * 60 * 1_000L).toRunSegments(prepare)

        assertEquals(
            listOf(Phase.PREPARE, Phase.WORK, Phase.COMPLETE),
            segments.map { it.phase },
        )
        assertEquals(prepare, segments.first().millis)
        // One unbroken block: the minute boundaries beep but do not divide the bar.
        assertEquals(1_200_000L, segments[1].millis)
    }

    @Test
    fun `an AMRAP preview matches the EMOM one`() {
        // Same picture: one work block. An AMRAP simply has no interval boundaries
        // inside it, which the timeline never drew anyway.
        val amrap = SessionRequest.Amrap(AmrapConfig(12 * 60 * 1_000L)).toPlan()

        assertEquals(
            emom(60_000, 12 * 60 * 1_000L).toRunSegments(prepare),
            amrap.toRunSegments(prepare),
        )
    }

    @Test
    fun `a workout with no duration cannot be previewed because it cannot be built`() {
        // This used to be a guard inside the segment builder. The configs now reject
        // a zero-length workout outright, so the preview never sees one.
        assertThrows(IllegalArgumentException::class.java) {
            TimerConfig(intervalMillis = 60_000, totalDurationMillis = 0)
        }
        assertThrows(IllegalArgumentException::class.java) {
            AmrapConfig(totalDurationMillis = 0)
        }
    }

    @Test
    fun `a Tabata preview alternates work and rest for the whole duration`() {
        // The classic 4min of 20s work and 10s rest: 8 cycles, 16 phases.
        val segments = tabata(20_000, 10_000, 4 * 60 * 1_000L).toRunSegments(prepare)

        val phases = segments.map { it.phase }
        assertEquals(Phase.PREPARE, phases.first())
        assertEquals(Phase.COMPLETE, phases.last())

        val body = segments.subList(1, segments.size - 1)
        assertEquals(16, body.size)
        assertEquals(Phase.WORK, body.first().phase)
        assertTrue(
            "work and rest must strictly alternate",
            body.mapIndexed { index, segment ->
                segment.phase == if (index % 2 == 0) Phase.WORK else Phase.REST
            }.all { it },
        )
    }

    @Test
    fun `a Tabata preview runs its final phase to the end rather than truncating it`() {
        // 1min 30s of 40s/20s cycles is one full cycle plus half of another. The engine
        // never cuts a phase short, so the preview must overrun rather than clip.
        val segments = tabata(40_000, 20_000, 90_000).toRunSegments(prepare)

        val body = segments.subList(1, segments.size - 1)
        assertEquals(listOf(Phase.WORK, Phase.REST, Phase.WORK), body.map { it.phase })
        assertEquals(100_000L, body.sumOf { it.millis })
    }

    @Test
    fun `the preview body is the plan itself, so it cannot disagree with the session`() {
        // The point of the refactor, stated as a test: everything between the prepare
        // lead and the completion cap is the plan, copied rather than re-derived.
        val plan = tabata(45_000, 15_000, 7 * 60 * 1_000L)
        val body = plan.toRunSegments(prepare).let { it.subList(1, it.size - 1) }

        assertEquals(
            plan.segments.map { RunSegment(it.phase, it.durationMillis) },
            body,
        )
    }

    @Test
    fun `the completion cap stays visible on a long workout and small on a short one`() {
        // It marks an instant, not a duration, so it has no natural width. A fixed
        // share of the run keeps the violet tail visible without distorting a short
        // session's proportions.
        val long = emom(60_000, 60 * 60 * 1_000L).toRunSegments(prepare).last().millis
        val short = emom(60_000, 60 * 1_000L).toRunSegments(prepare).last().millis

        assertEquals(150_000L, long)
        assertEquals(5_000L, short)
    }
}
