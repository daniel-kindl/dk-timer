package dev.danielkindl.ocho.ui.theme

import androidx.compose.material3.Typography
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.Font
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.em
import androidx.compose.ui.unit.sp
import dev.danielkindl.ocho.R

/**
 * OpenType feature giving every digit the same advance width.
 *
 * Without it a proportional `1` is narrower than an `8`, and the clock visibly
 * jitters as it counts down — the most-looked-at element on screen shifting on
 * every tick. Applied to the clock and every other live numeral.
 */
private const val TABULAR_FIGURES = "tnum"

/*
 * Three typefaces, each with one job. The split is what lets a glance at the
 * session screen separate the number that matters from everything around it.
 */

/**
 * Reserved for the single biggest display element on a screen — the active-session
 * countdown numeral and the wordmark. Always weight 700; only one use per screen,
 * or it stops signalling primacy.
 */
val SpaceGroteskFamily = FontFamily(
    Font(R.font.space_grotesk, FontWeight.Bold),
)

/**
 * Anything the timer computed rather than a human wrote: interval labels, round
 * counts, elapsed time, preset durations.
 *
 * Monospaced so digits share a width and values do not shift the layout as they
 * tick — a proportional font makes a running counter visibly jitter.
 */
val JetBrainsMonoFamily = FontFamily(
    Font(R.font.jetbrains_mono, FontWeight.Normal),
    Font(R.font.jetbrains_mono, FontWeight.Medium),
)

/** General UI text: buttons, body copy, headings. */
val IbmPlexSansFamily = FontFamily(
    Font(R.font.ibm_plex_sans, FontWeight.Normal),
    Font(R.font.ibm_plex_sans, FontWeight.Medium),
    Font(R.font.ibm_plex_sans, FontWeight.SemiBold),
)

/**
 * The Material 3 type scale.
 *
 * Governing rule: **if a human wrote it, IBM Plex Sans; if the timer produced it,
 * JetBrains Mono; Space Grotesk is display only.** The three families are never
 * mixed within a role — the split is what lets a glance at the session screen
 * separate the number that matters from the words around it.
 *
 * Slots the design system does not name (`displaySmall`, the `headline*` family,
 * `titleSmall`, `bodySmall`, `labelLarge`, `labelMedium`) are interpolated from
 * their neighbours so Material components that reach for them still land inside
 * the system.
 */
val OchoTypography = Typography(
    // The clock face. Never below 48sp on any screen size.
    displayLarge = TextStyle(
        fontFamily = SpaceGroteskFamily,
        fontWeight = FontWeight.Bold,
        fontSize = 76.sp,
        lineHeight = 76.sp,
        letterSpacing = (-0.03).em,
        fontFeatureSettings = TABULAR_FIGURES,
    ),
    // Wordmark and completion figures.
    displayMedium = TextStyle(
        fontFamily = SpaceGroteskFamily,
        fontWeight = FontWeight.Bold,
        fontSize = 40.sp,
        lineHeight = 44.sp,
        letterSpacing = (-0.03).em,
    ),
    displaySmall = TextStyle(
        fontFamily = SpaceGroteskFamily,
        fontWeight = FontWeight.Bold,
        fontSize = 32.sp,
        lineHeight = 38.sp,
        letterSpacing = (-0.03).em,
        fontFeatureSettings = TABULAR_FIGURES,
    ),

    // Headlines are computed values more often than prose here, so they take mono.
    headlineLarge = TextStyle(
        fontFamily = JetBrainsMonoFamily,
        fontWeight = FontWeight.Normal,
        fontSize = 28.sp,
        lineHeight = 34.sp,
        fontFeatureSettings = TABULAR_FIGURES,
    ),
    headlineMedium = TextStyle(
        fontFamily = JetBrainsMonoFamily,
        fontWeight = FontWeight.Normal,
        fontSize = 24.sp,
        lineHeight = 30.sp,
        fontFeatureSettings = TABULAR_FIGURES,
    ),
    headlineSmall = TextStyle(
        fontFamily = SpaceGroteskFamily,
        fontWeight = FontWeight.SemiBold,
        fontSize = 22.sp,
        lineHeight = 28.sp,
        letterSpacing = (-0.022).em,
    ),

    // Section headings.
    titleLarge = TextStyle(
        fontFamily = SpaceGroteskFamily,
        fontWeight = FontWeight.SemiBold,
        fontSize = 20.sp,
        lineHeight = 26.sp,
        letterSpacing = (-0.022).em,
    ),
    // Round counts, durations, intervals.
    titleMedium = TextStyle(
        fontFamily = JetBrainsMonoFamily,
        fontWeight = FontWeight.Normal,
        fontSize = 20.sp,
        lineHeight = 26.sp,
        fontFeatureSettings = TABULAR_FIGURES,
    ),
    titleSmall = TextStyle(
        fontFamily = JetBrainsMonoFamily,
        fontWeight = FontWeight.Normal,
        fontSize = 16.sp,
        lineHeight = 22.sp,
        fontFeatureSettings = TABULAR_FIGURES,
    ),

    // Prose and settings labels.
    bodyLarge = TextStyle(
        fontFamily = IbmPlexSansFamily,
        fontWeight = FontWeight.Normal,
        fontSize = 16.sp,
        lineHeight = 26.sp, // 1.65
    ),
    bodyMedium = TextStyle(
        fontFamily = IbmPlexSansFamily,
        fontWeight = FontWeight.Normal,
        fontSize = 14.sp,
        lineHeight = 22.sp,
    ),
    // 12sp is the floor for on-screen text.
    bodySmall = TextStyle(
        fontFamily = IbmPlexSansFamily,
        fontWeight = FontWeight.Normal,
        fontSize = 12.sp,
        lineHeight = 18.sp,
    ),

    // Button labels are written by a human, so they stay in Plex.
    labelLarge = TextStyle(
        fontFamily = IbmPlexSansFamily,
        fontWeight = FontWeight.Medium,
        fontSize = 14.sp,
        lineHeight = 20.sp,
    ),
    labelMedium = TextStyle(
        fontFamily = IbmPlexSansFamily,
        fontWeight = FontWeight.Medium,
        fontSize = 12.sp,
        lineHeight = 16.sp,
    ),
    // Uppercase eyebrows and phase labels.
    labelSmall = TextStyle(
        fontFamily = JetBrainsMonoFamily,
        fontWeight = FontWeight.Normal,
        fontSize = 11.sp,
        lineHeight = 16.sp,
        letterSpacing = 0.08.em,
    ),
)
