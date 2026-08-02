package dev.danielkindl.ocho.domain.model

import dev.danielkindl.ocho.core.format.sessionProgress

/**
 * The complete state of a running session, in one mode-agnostic shape.
 *
 * Every consumer reads this and only this: the session screens, the ongoing
 * notification, and anything added later. That is the point. Without a single type,
 * a foreground service would need to know whether it was rendering EMOM or Tabata,
 * and every future mode would mean touching the notification code again.
 *
 * Both engines map onto it, which is also the first step of collapsing the two
 * parallel session stacks into one.
 *
 * @property mode which workout produced this, for labelling only. No consumer
 *   should branch on it to decide behaviour.
 * @property status where the session is in its lifecycle.
 * @property phase what the user is doing right now.
 * @property remainingInPhaseMillis time until the phase ends. This is the dominant
 *   numeral on screen and the notification's headline.
 * @property elapsedMillis time worked, excluding time spent paused.
 * @property totalDurationMillis the configured workout length.
 * @property currentRound 1-indexed round in progress.
 * @property totalRounds rounds this workout will run.
 * @property countdownSecondsRemaining seconds left in the pre-start countdown.
 *   Meaningful only while [status] is [SessionStatus.CountingDown].
 */
data class SessionSnapshot(
    val mode: WorkoutMode,
    val status: SessionStatus,
    val phase: Phase,
    val remainingInPhaseMillis: Long = 0L,
    val elapsedMillis: Long = 0L,
    val totalDurationMillis: Long = 0L,
    val currentRound: Int = 1,
    val totalRounds: Int = 0,
    val countdownSecondsRemaining: Int = PREPARE_COUNTDOWN_SECONDS,
) {
    /** Overall completion from 0f to 1f, for progress indicators. */
    val progressFraction: Float
        get() = sessionProgress(elapsedMillis, totalDurationMillis)

    /** True while the engine is running or frozen, as opposed to counting in or finished. */
    val isActive: Boolean
        get() = status == SessionStatus.Running || status == SessionStatus.Paused
}

/** Which workout produced a [SessionSnapshot]. Used for labels, never for behaviour. */
enum class WorkoutMode {
    /** Every minute on the minute: one continuous work phase with periodic boundaries. */
    EMOM,

    /** Alternating work and rest phases. */
    TABATA,

    /** As many rounds as possible: one unbroken effort with no interval beeps. */
    AMRAP,
}
