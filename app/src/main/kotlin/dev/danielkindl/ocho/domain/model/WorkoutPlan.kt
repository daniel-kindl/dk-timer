package dev.danielkindl.ocho.domain.model

import kotlin.math.ceil

/**
 * One stretch of a workout spent in a single [Phase].
 *
 * Boundaries live *inside* a segment rather than between segments, which is the one
 * decision this type turns on. An EMOM is a single twenty-minute work block that
 * beeps every minute, not twenty one-minute blocks: the athlete never stops, and the
 * timeline draws it as one bar. Modelling those beeps as segment edges would have
 * split a continuous effort into pieces that exist only in the data structure.
 *
 * @property phase what the athlete is doing for the length of this segment.
 * @property durationMillis how long it runs. Always positive; the planner drops
 *   empty segments rather than emitting them.
 * @property boundaryEveryMillis spacing of interval beeps within the segment, or
 *   zero for a segment that runs unbroken. A final partial interval still beeps,
 *   which is why the count rounds up rather than down.
 */
data class PlannedSegment(
    val phase: Phase,
    val durationMillis: Long,
    val boundaryEveryMillis: Long = 0L,
)

/**
 * The complete shape of a workout, derived once from the request that describes it.
 *
 * **This type exists to be the only answer to "what does this workout look like?"**
 * That question used to be answered five times over, in mutually mirroring code: the
 * two engines each walked their own structure, the Tabata engine walked it a second
 * time to count rounds, the setup screen walked it again to label the pickers, and
 * the run timeline walked it once more to draw the preview. Each copy carried a
 * comment promising it matched the others, which is the sort of promise that only
 * holds until someone edits one of them.
 *
 * The engines and the preview now read the same plan, so a preview that disagrees
 * with the session it previews is no longer expressible.
 *
 * @property segments the workout in running order, excluding the pre-start
 *   countdown. Empty when the configuration is not yet runnable, which the setup
 *   screen relies on while the pickers are mid-edit.
 * @property totalDurationMillis the configured target. Tabata can overrun it, since
 *   a phase is never cut short, so this is what progress is measured against rather
 *   than what the segments necessarily sum to.
 * @property totalRounds rounds this workout will run, or zero when rounds are not
 *   counted. An AMRAP's rounds are whatever the athlete manages, which the app has
 *   no way to know.
 */
data class WorkoutPlan(
    val segments: List<PlannedSegment>,
    val totalDurationMillis: Long,
    val totalRounds: Int,
)

/**
 * Works out the shape of this request.
 *
 * The single `when` over [SessionRequest] that describes workout structure. Adding a
 * mode produces a compile error here, and everything reading the plan — both
 * engines, the round counter and the timeline preview — picks the new mode up
 * without being touched.
 */
fun SessionRequest.toPlan(): WorkoutPlan = when (this) {
    is SessionRequest.Emom -> WorkoutPlan(
        segments = listOf(
            PlannedSegment(
                phase = Phase.WORK,
                durationMillis = config.totalDurationMillis,
                boundaryEveryMillis = config.intervalMillis,
            )
        ),
        totalDurationMillis = config.totalDurationMillis,
        // Rounds up: a truncated final interval still gets its beep. An interval
        // longer than the workout yields the one round that never completes.
        totalRounds = ceil(
            config.totalDurationMillis.toDouble() / config.intervalMillis
        ).toInt(),
    )

    is SessionRequest.Amrap -> WorkoutPlan(
        segments = listOf(
            PlannedSegment(
                phase = Phase.WORK,
                durationMillis = config.totalDurationMillis,
            )
        ),
        totalDurationMillis = config.totalDurationMillis,
        totalRounds = 0,
    )

    is SessionRequest.Tabata -> {
        val segments = config.alternatingPhases()
        WorkoutPlan(
            segments = segments,
            totalDurationMillis = config.totalDurationMillis,
            totalRounds = segments.count { it.phase == Phase.WORK },
        )
    }
}

/**
 * Alternates work and rest until the target is reached or passed.
 *
 * Overruns rather than truncates, matching the rule that a phase always runs to its
 * end: an interval timer that cut the final work phase short would be worse than one
 * that ran a few seconds long.
 *
 * Returns nothing at all for a configuration that cannot run. [TabataConfig] does not
 * validate its own durations, and a zero-length phase would advance the loop below by
 * nothing forever, so the guard is what makes the loop provably terminate rather than
 * merely unreachable in practice.
 */
private fun TabataConfig.alternatingPhases(): List<PlannedSegment> {
    if (workMillis <= 0 || restMillis <= 0 || totalDurationMillis <= 0) return emptyList()

    val segments = mutableListOf<PlannedSegment>()
    var planned = 0L
    var working = true
    while (planned < totalDurationMillis) {
        val duration = if (working) workMillis else restMillis
        segments += PlannedSegment(
            phase = if (working) Phase.WORK else Phase.REST,
            durationMillis = duration,
        )
        planned += duration
        working = !working
    }
    return segments
}
