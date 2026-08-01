package dev.danielkindl.ocho.ui.theme

import androidx.compose.ui.graphics.Color

/**
 * The four states a session can be in, each owning exactly one full-bleed
 * background colour.
 *
 * This is the app's primary information channel. The session screen is meant to be
 * readable by someone three metres away, out of breath, not looking directly at the
 * phone — the colour answers "what am I doing right now" before any text is read.
 *
 * Deliberately separate from `SessionStatus`, which tracks lifecycle (paused,
 * stopped) rather than what the user is doing. Pausing does not change the phase:
 * you are still in the work interval, just frozen in it.
 */
enum class Phase {
    /** Countdown before the first round. */
    PREPARE,

    /** A work interval. */
    WORK,

    /** A rest interval. Tabata only — EMOM has no rest phase. */
    REST,

    /** The session has finished. */
    COMPLETE,
}

/**
 * A phase's background plate and the text colour that goes on it.
 *
 * @property plate full-bleed background colour.
 * @property onPlate text and glyph colour, chosen to clear 4.5:1 against [plate].
 */
data class PhaseTheme(val plate: Color, val onPlate: Color)

private val Ink = N950
private val Paper = N0

/**
 * Resolves the plate and on-plate colours for [phase].
 *
 * **Rest is a light plate, and must not be "corrected" to a mid-green.** Red-500 and
 * green-500 sit at almost the same lightness, so under deuteranopia — around 8% of
 * men, in a category that skews male — work and rest converge into two similar
 * mid-tone plates and the primary information channel fails outright.
 *
 * Putting rest on a light plate separates the two by lightness as well as hue, so
 * they stay distinct with no colour vision at all. The on-plate polarity flips with
 * it (white on work, ink on rest), which is a second redundant signal. Rest is also
 * the one phase that does not drop to a 700 step in dark theme, for the same reason.
 *
 * The run-level consequence is a light-dark-light-dark rhythm that makes each
 * interval flip catchable in peripheral vision.
 */
fun phaseTheme(phase: Phase, dark: Boolean): PhaseTheme = when (phase) {
    Phase.PREPARE -> if (dark) PhaseTheme(Amber700, Paper) else PhaseTheme(Amber500, Ink)
    Phase.WORK -> if (dark) PhaseTheme(Red700, Paper) else PhaseTheme(Red500, Paper)
    Phase.REST -> if (dark) PhaseTheme(Green400, Ink) else PhaseTheme(Green200, Ink)
    Phase.COMPLETE -> if (dark) PhaseTheme(Violet700, Paper) else PhaseTheme(Violet500, Paper)
}

/**
 * Error plate surface.
 *
 * Errors never use a phase plate. A red error background over a red work screen
 * would be unreadable as either, so errors are a tinted plate *inside* the layout
 * instead — and an error raised mid-session waits for the next rest interval or
 * goes to the notification. The running clock is never covered.
 */
val ErrorSurface = Red100

/** Error plate text and glyph colour. */
val OnErrorSurface = Red700
