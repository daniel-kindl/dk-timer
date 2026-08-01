package dev.danielkindl.ocho.ui.components

import android.app.Activity
import android.provider.Settings
import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.CubicBezierEasing
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.systemBarsPadding
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.SideEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.luminance
import androidx.compose.ui.graphics.toArgb
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalView
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.core.view.WindowCompat
import dev.danielkindl.ocho.domain.model.Phase
import dev.danielkindl.ocho.ui.theme.PhaseTheme
import dev.danielkindl.ocho.ui.theme.phaseTheme

/** Duration of the phase colour crossfade. */
private const val PHASE_CROSSFADE_MILLIS = 340

/** `ease-in-out` from the design system, used for movement and the phase crossfade. */
private val PhaseEasing = CubicBezierEasing(0.62f, 0.02f, 0.28f, 1f)

/** Opacity for text subordinate to the clock: the phase label and round counter. */
const val SUBDUED_ON_PLATE = 0.82f

/**
 * True when the user has turned system animations off.
 *
 * Read once per composition rather than observed: the setting cannot change while a
 * workout is on screen, and polling it every frame would cost more than it saves.
 */
@Composable
fun rememberReducedMotionEnabled(): Boolean {
    val contentResolver = LocalContext.current.contentResolver
    return remember {
        Settings.Global.getFloat(contentResolver, Settings.Global.ANIMATOR_DURATION_SCALE, 1f) == 0f
    }
}

/**
 * Full-bleed phase background shared by both session screens.
 *
 * Crossfades the plate over 340ms when [phase] changes, and tints the system bars to
 * match so the colour runs edge to edge. Motion here is decoration; the colour is
 * the information. Audio and haptic cues fire on the frame the interval flips and
 * are never gated on this animation — with animations off the swap is instant and
 * the cue is unaffected.
 *
 * @param content receives the resolved [PhaseTheme] so children can colour text
 *   against the current plate.
 */
@Composable
fun PhaseScaffold(
    phase: Phase,
    content: @Composable (PhaseTheme) -> Unit,
) {
    val target = phaseTheme(phase, isSystemInDarkTheme())
    val reducedMotion = rememberReducedMotionEnabled()

    val plate by animateColorAsState(
        targetValue = target.plate,
        animationSpec = if (reducedMotion) {
            tween(durationMillis = 0)
        } else {
            tween(durationMillis = PHASE_CROSSFADE_MILLIS, easing = PhaseEasing)
        },
        label = "phase-plate",
    )
    val onPlate by animateColorAsState(
        targetValue = target.onPlate,
        animationSpec = if (reducedMotion) {
            tween(durationMillis = 0)
        } else {
            tween(durationMillis = PHASE_CROSSFADE_MILLIS, easing = PhaseEasing)
        },
        label = "phase-on-plate",
    )

    // The plate runs behind the status and navigation bars, so their icons have to
    // follow the plate's polarity rather than the app theme's.
    val view = LocalView.current
    if (!view.isInEditMode) {
        val lightIcons = target.onPlate.luminance() < HALF
        SideEffect {
            val window = (view.context as Activity).window
            window.statusBarColor = Color.Transparent.toArgb()
            WindowCompat.getInsetsController(window, view).apply {
                isAppearanceLightStatusBars = lightIcons
                isAppearanceLightNavigationBars = lightIcons
            }
        }
    }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(plate),
        contentAlignment = Alignment.Center,
    ) {
        content(PhaseTheme(plate = plate, onPlate = onPlate))
    }
}

/** Midpoint of the 0..1 luminance range, used to pick system-bar icon polarity. */
private const val HALF = 0.5f

/**
 * The uppercase eyebrow naming the current phase.
 *
 * This is the redundant, non-colour carrier of the same information the plate
 * conveys, and it is why the phase system stays legible with no colour vision at
 * all. It is never omitted, even when the colour seems unambiguous.
 */
@Composable
fun PhaseLabel(text: String, onPlate: Color, modifier: Modifier = Modifier) {
    Text(
        text = text.uppercase(),
        style = MaterialTheme.typography.labelSmall,
        color = onPlate.copy(alpha = SUBDUED_ON_PLATE),
        textAlign = TextAlign.Center,
        modifier = modifier,
    )
}

/**
 * The dominant clock numeral, in `MM:SS`.
 *
 * Takes `displayLarge`, which carries tabular figures so the digits do not shift
 * width as they count down.
 */
@Composable
fun PhaseClock(text: String, onPlate: Color, modifier: Modifier = Modifier) {
    Text(
        text = text,
        style = MaterialTheme.typography.displayLarge,
        color = onPlate,
        textAlign = TextAlign.Center,
        modifier = modifier,
    )
}

/**
 * Standard vertical arrangement for a session screen: label at the top, clock in the
 * middle, controls at the bottom, with generous horizontal padding.
 */
@Composable
fun SessionColumn(
    modifier: Modifier = Modifier,
    content: @Composable () -> Unit,
) {
    Column(
        modifier = modifier
            .fillMaxSize()
            .systemBarsPadding()
            .padding(horizontal = 24.dp, vertical = 32.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.SpaceBetween,
    ) {
        content()
    }
}
