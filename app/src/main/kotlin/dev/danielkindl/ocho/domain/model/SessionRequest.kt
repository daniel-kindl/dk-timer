package dev.danielkindl.ocho.domain.model

/**
 * A workout to run, carrying whichever config its mode needs.
 *
 * Sealed on purpose. This is the one place in the app that knows more than one kind
 * of workout exists, so adding a mode produces a compile error at the single `when`
 * that dispatches on it rather than a runtime surprise. Everything downstream works
 * in terms of [SessionSnapshot] and [SessionCue] and never branches on mode.
 */
sealed interface SessionRequest {

    /** @property config the validated EMOM durations. */
    data class Emom(val config: TimerConfig) : SessionRequest

    /** @property config the validated Tabata durations. */
    data class Tabata(val config: TabataConfig) : SessionRequest
}
