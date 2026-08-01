package dev.danielkindl.ocho.ui.theme

import androidx.compose.ui.graphics.Color

/*
 * Raw palette. These are the only literal colours in the app; everything else
 * reaches them through the Material scheme in Theme.kt.
 *
 * Numbered by lightness on the usual 50..950 scale, so light and dark schemes can
 * pick mirrored steps of the same hue. Steps with no current consumer are kept so
 * the ramp stays complete and evenly spaced — the gaps would otherwise have to be
 * re-derived by eye the first time a new surface needs one.
 */

/** Lightest green tint. Unused; reserved ramp step. */
val Green50 = Color(0xFFEDF6F1)

/** Pale green, used for text and icons on dark green surfaces. */
val Green100 = Color(0xFFD6EBDD)

/** Unused; reserved ramp step. */
val Green200 = Color(0xFFB0D8BE)

/** Unused; reserved ramp step. */
val Green300 = Color(0xFF84BE9B)

/** Light green primary for the dark scheme, where the 500 step is too dim to read. */
val Green400 = Color(0xFF5CA47C)

/** Brand green. Primary in the light scheme and the Tabata rest phase. */
val Green500 = Color(0xFF3D8560)

/** Unused; reserved ramp step. */
val Green600 = Color(0xFF336E4F)

/** Deep green for pressed and container states in the light scheme. */
val Green700 = Color(0xFF2A5940)

/** Darker green container fill. */
val Green800 = Color(0xFF214531)

/** Near-black green, used as a container behind pale green content in the dark scheme. */
val Green900 = Color(0xFF173124)

/** Darkest green. Unused; reserved ramp step. */
val Green950 = Color(0xFF0E1D16)

/*
 * Signal colours. State only, never decoration.
 *
 * All four hues below are spent on session phases, which is why an error can no
 * longer be "the red one". Errors are separated structurally instead: a phase
 * colour is only ever a full-bleed background, an error is only ever a tinted plate
 * inside the layout. See PhaseTheme.kt.
 */

/** Prepare phase, light theme. */
val Amber500 = Color(0xFFE8A317)

/** Prepare phase, dark theme. */
val Amber700 = Color(0xFFA8720A)

/** Error plate surface. The only red that is not a work phase. */
val Red100 = Color(0xFFFDE0E1)

/** Work phase, light theme. */
val Red500 = Color(0xFFE5484D)

/** Work phase, dark theme; also error plate text and glyphs. */
val Red700 = Color(0xFFB3282D)

/** Complete phase, light theme. */
val Violet500 = Color(0xFF7C6CF0)

/** Complete phase, dark theme. */
val Violet700 = Color(0xFF5645C4)

/** Reserved for informational states. No current consumer. */
val Blue500 = Color(0xFF2E82F6)

/*
 * Green-tinted graphite neutrals. Slightly desaturated toward the brand hue rather
 * than pure grey, so surfaces sit with the green rather than beside it.
 */

/** Pure white: the lightest surface, and content on coloured backgrounds. */
val N0 = Color(0xFFFFFFFF)

/** Off-white app background in the light scheme. */
val N50 = Color(0xFFF7F9F8)

/** Raised surface in the light scheme, e.g. cards. */
val N100 = Color(0xFFEDF1EF)

/** Unused; reserved ramp step. */
val N200 = Color(0xFFDDE3E0)

/** Hairline borders and dividers. */
val N300 = Color(0xFFC2CBC7)

/** Unused; reserved ramp step. */
val N400 = Color(0xFF93A09A)

/** Unused; reserved ramp step. */
val N500 = Color(0xFF6B7873)

/** Secondary text and inactive controls in the dark scheme. */
val N600 = Color(0xFF4E5A55)

/** Dividers in the dark scheme. */
val N700 = Color(0xFF38423E)

/** Raised surface in the dark scheme. */
val N800 = Color(0xFF232B28)

/** App background in the dark scheme, and primary text in the light one. */
val N900 = Color(0xFF161C1A)

/** Darkest neutral: the deepest dark-scheme surface. */
val N950 = Color(0xFF0C110F)
