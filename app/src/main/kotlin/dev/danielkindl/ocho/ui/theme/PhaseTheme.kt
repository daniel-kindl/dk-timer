package dev.danielkindl.ocho.ui.theme

import androidx.compose.ui.graphics.Color
import dev.danielkindl.ocho.domain.model.Phase

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
