package dev.danielkindl.ocho.domain.model

import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

/**
 * Tests for the planner every other consumer of workout shape now reads.
 *
 * Worth pinning heavily, because the whole point of the type is that the engines,
 * the round counter and the timeline preview agree by construction. If the plan is
 * wrong, all four are wrong together and in the same direction, which is exactly the
 * failure that is hardest to notice.
 */
class WorkoutPlanTest {

    @Test
    fun `an EMOM is one unbroken work block that beeps at its interval`() {
        val plan = SessionRequest.Emom(
            TimerConfig(intervalMillis = 60_000, totalDurationMillis = 20 * 60_000L)
        ).toPlan()

        assertEquals(
            listOf(PlannedSegment(Phase.WORK, 20 * 60_000L, boundaryEveryMillis = 60_000)),
            plan.segments,
        )
        assertEquals(20, plan.totalRounds)
    }

    @Test
    fun `an EMOM rounds its final partial interval up, because it still beeps`() {
        // 65s on the minute is one full minute, then five seconds that still count.
        val plan = SessionRequest.Emom(
            TimerConfig(intervalMillis = 20_000, totalDurationMillis = 65_000)
        ).toPlan()

        assertEquals(4, plan.totalRounds)
    }

    @Test
    fun `an EMOM whose interval outlasts the workout runs a single round`() {
        // The boundary never arrives; the workout ends first. One round, not zero.
        val plan = SessionRequest.Emom(
            TimerConfig(intervalMillis = 5 * 60_000L, totalDurationMillis = 60_000)
        ).toPlan()

        assertEquals(1, plan.totalRounds)
    }

    @Test
    fun `an AMRAP is one work block with nothing to announce inside it`() {
        val plan = SessionRequest.Amrap(AmrapConfig(totalDurationMillis = 12 * 60_000L)).toPlan()

        assertEquals(
            listOf(PlannedSegment(Phase.WORK, 12 * 60_000L, boundaryEveryMillis = 0)),
            plan.segments,
        )
        // Rounds are whatever the athlete manages, so the app claims none.
        assertEquals(0, plan.totalRounds)
    }

    @Test
    fun `a Tabata alternates work and rest for the whole duration`() {
        // The classic 4min of 20s work and 10s rest: 8 cycles, 16 phases.
        val plan = SessionRequest.Tabata(
            TabataConfig(workMillis = 20_000, restMillis = 10_000, totalDurationMillis = 4 * 60_000L)
        ).toPlan()

        assertEquals(16, plan.segments.size)
        assertEquals(8, plan.totalRounds)
        assertTrue(
            "work and rest must strictly alternate from work",
            plan.segments.mapIndexed { index, segment ->
                segment.phase == if (index % 2 == 0) Phase.WORK else Phase.REST
            }.all { it },
        )
    }

    @Test
    fun `a Tabata runs its final phase to the end rather than truncating it`() {
        // 1min 30s of 40s/20s cycles is one full cycle plus half of another. The
        // phase is never cut short, so the plan overruns to 100s.
        val plan = SessionRequest.Tabata(
            TabataConfig(workMillis = 40_000, restMillis = 20_000, totalDurationMillis = 90_000)
        ).toPlan()

        assertEquals(
            listOf(Phase.WORK, Phase.REST, Phase.WORK),
            plan.segments.map { it.phase },
        )
        assertEquals(100_000L, plan.segments.sumOf { it.durationMillis })
        assertEquals(2, plan.totalRounds)
        // The configured target is what progress is measured against, not the overrun.
        assertEquals(90_000L, plan.totalDurationMillis)
    }

    @Test
    fun `a Tabata never plans interval boundaries inside a phase`() {
        val plan = SessionRequest.Tabata(
            TabataConfig(workMillis = 20_000, restMillis = 10_000, totalDurationMillis = 60_000)
        ).toPlan()

        assertTrue(plan.segments.all { it.boundaryEveryMillis == 0L })
    }

    @Test
    fun `a Tabata that cannot run plans nothing rather than looping forever`() {
        // TabataConfig does not validate itself, so the planner has to. A zero-length
        // phase would advance the alternation by nothing, forever.
        val cases = listOf(
            TabataConfig(workMillis = 0, restMillis = 10_000, totalDurationMillis = 60_000),
            TabataConfig(workMillis = 20_000, restMillis = 0, totalDurationMillis = 60_000),
            TabataConfig(workMillis = 20_000, restMillis = 10_000, totalDurationMillis = 0),
            TabataConfig(workMillis = 0, restMillis = 0, totalDurationMillis = 0),
        )

        cases.forEach { config ->
            val plan = SessionRequest.Tabata(config).toPlan()
            assertEquals(config.toString(), emptyList<PlannedSegment>(), plan.segments)
            assertEquals(config.toString(), 0, plan.totalRounds)
        }
    }
}
