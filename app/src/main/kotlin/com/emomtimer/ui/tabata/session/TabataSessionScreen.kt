package com.emomtimer.ui.tabata.session

import android.app.Activity
import android.view.WindowManager
import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Pause
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.Stop
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.emomtimer.domain.model.SessionStatus
import com.emomtimer.domain.model.TabataPhase

private val WorkBackground = Color(0xFFB71C1C)   // deep red
private val RestBackground = Color(0xFF1B5E20)   // deep green
private val WorkBackgroundPaused = Color(0xFF7F1010)
private val RestBackgroundPaused = Color(0xFF0D3A10)
private val OnPhaseBackground = Color.White

@Composable
fun TabataSessionScreen(
    onSessionFinished: () -> Unit,
    viewModel: TabataSessionViewModel = hiltViewModel(),
) {
    val state by viewModel.uiState.collectAsStateWithLifecycle()

    // Keep the screen on only while this session is active
    val activity = LocalContext.current as Activity
    DisposableEffect(Unit) {
        activity.window.addFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON)
        onDispose {
            activity.window.clearFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON)
        }
    }

    LaunchedEffect(state.status) {
        if (state.status == SessionStatus.Stopped || state.status == SessionStatus.Completed) {
            onSessionFinished()
        }
    }

    val isPaused = state.status == SessionStatus.Paused

    val targetBackground = when {
        state.phase == TabataPhase.Work && isPaused -> WorkBackgroundPaused
        state.phase == TabataPhase.Work -> WorkBackground
        isPaused -> RestBackgroundPaused
        else -> RestBackground
    }

    val background by animateColorAsState(
        targetValue = targetBackground,
        animationSpec = tween(durationMillis = 300),
        label = "phase-background",
    )

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(background),
        contentAlignment = Alignment.Center,
    ) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(horizontal = 32.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.SpaceEvenly,
        ) {
            // Phase label
            Text(
                text = if (isPaused) "⏸  PAUSED"
                       else if (state.phase == TabataPhase.Work) "WORK" else "REST",
                style = MaterialTheme.typography.displayMedium,
                color = OnPhaseBackground,
                textAlign = TextAlign.Center,
            )

            // Countdown within the current phase
            Text(
                text = state.remainingInPhaseMillis.formatCountdown(),
                style = MaterialTheme.typography.displayLarge,
                color = OnPhaseBackground,
                textAlign = TextAlign.Center,
            )

            HorizontalDivider(color = OnPhaseBackground.copy(alpha = 0.3f))

            // Elapsed time
            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                Text(
                    text = "ELAPSED",
                    style = MaterialTheme.typography.titleLarge,
                    color = OnPhaseBackground.copy(alpha = 0.7f),
                )
                Text(
                    text = state.elapsedMillis.formatElapsed(),
                    style = MaterialTheme.typography.headlineLarge,
                    color = OnPhaseBackground,
                )
            }

            Spacer(Modifier.height(8.dp))

            // Controls
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(12.dp),
            ) {
                Button(
                    onClick = { if (isPaused) viewModel.resumeSession() else viewModel.pauseSession() },
                    modifier = Modifier
                        .weight(1f)
                        .height(64.dp),
                    colors = ButtonDefaults.buttonColors(
                        containerColor = OnPhaseBackground.copy(alpha = 0.2f),
                        contentColor = OnPhaseBackground,
                    ),
                ) {
                    Icon(
                        if (isPaused) Icons.Default.PlayArrow else Icons.Default.Pause,
                        contentDescription = if (isPaused) "Resume" else "Pause",
                        modifier = Modifier.size(28.dp),
                    )
                    Spacer(Modifier.width(8.dp))
                    Text(
                        if (isPaused) "RESUME" else "PAUSE",
                        style = MaterialTheme.typography.titleMedium,
                    )
                }

                Button(
                    onClick = viewModel::stopSession,
                    modifier = Modifier
                        .weight(1f)
                        .height(64.dp),
                    colors = ButtonDefaults.buttonColors(
                        containerColor = OnPhaseBackground.copy(alpha = 0.2f),
                        contentColor = OnPhaseBackground,
                    ),
                ) {
                    Icon(
                        Icons.Default.Stop,
                        contentDescription = "Stop",
                        modifier = Modifier.size(28.dp),
                    )
                    Spacer(Modifier.width(8.dp))
                    Text("STOP", style = MaterialTheme.typography.titleMedium)
                }
            }
        }
    }
}

private fun Long.formatCountdown(): String {
    val totalSec = (this / 1_000).coerceAtLeast(0)
    val min = totalSec / 60
    val sec = totalSec % 60
    return "%d:%02d".format(min, sec)
}

private fun Long.formatElapsed(): String {
    val totalSec = this / 1_000
    val min = totalSec / 60
    val sec = totalSec % 60
    return "%02d:%02d".format(min, sec)
}
