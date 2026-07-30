package com.emomtimer.ui.theme

import android.app.Activity
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.SideEffect
import androidx.compose.ui.graphics.toArgb
import androidx.compose.ui.platform.LocalView
import androidx.core.view.WindowCompat

private val DarkColors = darkColorScheme(
    primary = Green400,
    onPrimary = N950,
    primaryContainer = Green800,
    onPrimaryContainer = Green100,
    secondary = N300,
    background = N950,
    onBackground = N50,
    surface = N900,
    onSurface = N50,
    surfaceVariant = N800,
    onSurfaceVariant = N300,
    error = Red500,
    onError = N0,
    outline = N700,
)

private val LightColors = lightColorScheme(
    primary = Green500,
    onPrimary = N0,
    primaryContainer = Green100,
    onPrimaryContainer = Green900,
    secondary = N600,
    background = N50,
    onBackground = N900,
    surface = N0,
    onSurface = N900,
    surfaceVariant = N100,
    onSurfaceVariant = N600,
    error = Red500,
    onError = N0,
    outline = N300,
)

@Composable
fun EmomTimerTheme(
    darkTheme: Boolean = isSystemInDarkTheme(),
    content: @Composable () -> Unit,
) {
    val colorScheme = if (darkTheme) DarkColors else LightColors
    val view = LocalView.current
    if (!view.isInEditMode) {
        SideEffect {
            val window = (view.context as Activity).window
            window.statusBarColor = colorScheme.background.toArgb()
            WindowCompat.getInsetsController(window, view).isAppearanceLightStatusBars = !darkTheme
        }
    }

    MaterialTheme(
        colorScheme = colorScheme,
        typography = EmomTypography,
        content = content,
    )
}
