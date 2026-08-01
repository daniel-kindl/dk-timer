package dev.danielkindl.ocho.ui.components

import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.ProgressIndicatorDefaults
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color

/**
 * Thin wrapper around the modern lambda-based [LinearProgressIndicator] overload.
 *
 * [modifier] defaults to the bare [Modifier] per Compose convention — a default of
 * `Modifier.fillMaxWidth()` would be silently discarded the moment a caller passed
 * one of their own. The full width is applied below instead, so it survives either way.
 *
 * @param progress fraction of the session elapsed, from 0f to 1f.
 */
@Composable
fun SessionProgressBar(
    progress: Float,
    modifier: Modifier = Modifier,
    color: Color = ProgressIndicatorDefaults.linearColor,
    trackColor: Color = ProgressIndicatorDefaults.linearTrackColor,
) {
    LinearProgressIndicator(
        progress = { progress },
        modifier = modifier.fillMaxWidth(),
        color = color,
        trackColor = trackColor,
    )
}
