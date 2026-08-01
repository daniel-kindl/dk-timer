package dev.danielkindl.ocho.ui.components

import dev.danielkindl.ocho.domain.model.Phase
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

/**
 * Tests for the segment builders behind [RunTimeline].
 *
 * The timeline is the one place a user sees the shape of a workout before starting
 * it, so a wrong preview is a wrong promise. The composable that draws the strip is
 * verified by reading, but these functions are ordinary list-building logic and worth
 * pinning: the Tabata builder in particular rounds up to a phase boundary exactly as
 * the engine does, and that agreement is easy to break from either side.
 */
class RunTimelineSegmentsTest {

    private val prepare = 10_000L

    @Test
    fun `an EMOM preview is prepare, one work block, then the completion cap`() {
        val segments = emomSegments(prepareMillis = prepare, totalMillis = 20 * 60 * 1_000L)

        assertEquals(
            listOf(Phase.PREPARE, Phase.WORK, Phase.COMPLETE),
            segments.map { it.phase },
        )
        assertEquals(prepare, segments.first().millis)
        assertEquals(1_200_000L, segments[1].millis)
    }

    @Test
    fun `an AMRAP preview matches the EMOM one`() {
        // Same picture: one work block. An AMRAP simply has no interval boundaries
        // inside it, which the timeline never drew anyway.
        assertEquals(
            emomSegments(prepare, 12 * 60 * 1_000L),
            amrapSegments(prepare, 12 * 60 * 1_000L),
        )
    }

    @Test
    fun `an EMOM preview with no duration shows only the prepare segment`() {
        val segments = emomSegments(prepareMillis = prepare, totalMillis = 0)
        assertEquals(listOf(RunSegment(Phase.PREPARE, prepare)), segments)
    }

    @Test
    fun `a Tabata preview alternates work and rest for the whole duration`() {
        // The classic 4min of 20s work and 10s rest: 8 cycles, 16 phases.
        val segments = tabataSegments(
            prepareMillis = prepare,
            workMillis = 20_000,
            restMillis = 10_000,
            totalMillis = 4 * 60 * 1_000L,
        )

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
        val segments = tabataSegments(
            prepareMillis = prepare,
            workMillis = 40_000,
            restMillis = 20_000,
            totalMillis = 90_000,
        )

        val body = segments.subList(1, segments.size - 1)
        assertEquals(listOf(Phase.WORK, Phase.REST, Phase.WORK), body.map { it.phase })
        assertEquals(100_000L, body.sumOf { it.millis })
    }

    @Test
    fun `a Tabata preview with an incomplete configuration shows only the prepare segment`() {
        val cases = listOf(
            Triple(0L, 10_000L, 60_000L),
            Triple(20_000L, 0L, 60_000L),
            Triple(20_000L, 10_000L, 0L),
        )

        cases.forEach { (work, rest, total) ->
            val segments = tabataSegments(prepare, work, rest, total)
            assertEquals(
                "work=$work rest=$rest total=$total",
                listOf(RunSegment(Phase.PREPARE, prepare)),
                segments,
            )
        }
    }

    @Test
    fun `the completion cap stays visible on a long workout and small on a short one`() {
        // It marks an instant, not a duration, so it has no natural width. A fixed
        // share of the run keeps the violet tail visible without distorting a short
        // session's proportions.
        val long = emomSegments(prepare, 60 * 60 * 1_000L).last().millis
        val short = emomSegments(prepare, 60 * 1_000L).last().millis

        assertEquals(150_000L, long)
        assertEquals(5_000L, short)
    }
}
