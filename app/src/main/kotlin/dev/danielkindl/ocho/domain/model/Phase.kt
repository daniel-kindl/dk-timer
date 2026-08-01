package dev.danielkindl.ocho.domain.model

/**
 * What the user is doing right now.
 *
 * This is a domain fact, not a presentation detail, which is why it lives here
 * rather than beside the colours it drives. `ui/theme/phaseTheme()` maps it to a
 * plate; the notification maps it to a title. Neither mapping belongs to the other.
 *
 * Deliberately separate from [SessionStatus], which tracks lifecycle. Pausing does
 * not change the phase: you are still inside the work interval, merely frozen in it.
 */
enum class Phase {
    /** Countdown before the first round. */
    PREPARE,

    /** A work interval. */
    WORK,

    /** A rest interval. Tabata only; EMOM has no rest phase. */
    REST,

    /** The session has finished. */
    COMPLETE,
}
