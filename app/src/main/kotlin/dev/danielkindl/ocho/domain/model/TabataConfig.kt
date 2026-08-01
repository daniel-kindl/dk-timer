package dev.danielkindl.ocho.domain.model

/**
 * A Tabata workout: alternate [workMillis] and [restMillis] until
 * [totalDurationMillis] is reached.
 *
 * The workout ends only on a phase boundary, so the last phase runs to completion
 * even if that overshoots [totalDurationMillis] — an interval timer that cut the
 * final work phase short would be worse than one that ran a few seconds long.
 *
 * @property workMillis length of each work phase.
 * @property restMillis length of each rest phase.
 * @property totalDurationMillis target length; the actual workout rounds up to the
 *   next phase boundary.
 */
data class TabataConfig(
    val workMillis: Long,
    val restMillis: Long,
    val totalDurationMillis: Long,
)
