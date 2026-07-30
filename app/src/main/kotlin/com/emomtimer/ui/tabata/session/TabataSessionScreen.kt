package com.emomtimer.ui.tabata.session

import android.app.Activity
import android.view.WindowManager
import androidx.activity.compose.BackHandler
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
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.Pause
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.Stop
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.emomtimer.core.format.formatCountdown
import com.emomtimer.core.format.formatElapsed
import com.emomtimer.domain.model.SessionStatus
import com.emomtimer.domain.model.TabataPhase

private val WorkBackground = Color(0xFFB71C1C)   // deep red
private val RestBackground = Color(0xFF1B5E20)   // deep green
private val WorkBackgroundPaused = Color(0xFF7F1010)
private val RestBackgroundPaused = Color(0xFF0D3A10)
private val NeutralBackground = Color(0xFF121212)
private val OnPhaseBackground = Color.White

@Composable
fun TabataSessionScreen(
    onSessionFinished: () -> Unit,
    viewModel: TabataSessionViewModel = hiltViewModel(),
) {
    val state by viewModel.uiState.collectAsStateWithLifecycle()

    // Keep the screen on for the whole lifecycle of this screen (countdown through completion)
    val activity = LocalContext.current as Activity
    DisposableEffect(Unit) {
        activity.window.addFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON)
        onDispose {
            activity.window.clearFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON)
        }
    }

    // Navigate back only on an explicit stop; Completed shows its own summary first
    LaunchedEffect(state.status) {
        if (state.status == SessionStatus.Stopped) {
            onSessionFinished()
        }
    }

    var showExitConfirm by rememberSaveable { mutableStateOf(false) }
    val canExit = state.status == SessionStatus.Running || state.status == SessionStatus.Paused

    BackHandler(enabled = canExit) { showExitConfirm = true }

    if (showExitConfirm) {
        ExitConfirmDialog(
            onConfirm = {
                showExitConfirm = false
                viewModel.stopSession()
            },
            onDismiss = { showExitConfirm = false },
        )
    }

    val isPaused = state.status == SessionStatus.Paused
    val background by animateColorAsState(
        targetValue = tabataBackgroundColor(status = state.status, phase = state.phase, isPaused = isPaused),
        animationSpec = tween(durationMillis = 300),
        label = "phase-background",
    )

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(background),
        contentAlignment = Alignment.Center,
    ) {
        when (state.status) {
            SessionStatus.CountingDown -> CountdownContent(
                secondsRemaining = state.countdownSecondsRemaining,
                onStop = { showExitConfirm = true },
            )

            SessionStatus.Completed -> CompletionContent(
                totalElapsedMillis = state.elapsedMillis,
                onDone = onSessionFinished,
            )

            else -> RunningContent(
                state = state,
                onPauseResume = {
                    if (isPaused) viewModel.resumeSession() else viewModel.pauseSession()
                },
                onStop = { showExitConfirm = true },
            )
        }
    }
}

private fun tabataBackgroundColor(status: SessionStatus, phase: TabataPhase, isPaused: Boolean): Color = when {
    status != SessionStatus.Running && status != SessionStatus.Paused -> NeutralBackground
    phase == TabataPhase.Work && isPaused -> WorkBackgroundPaused
    phase == TabataPhase.Work -> WorkBackground
    isPaused -> RestBackgroundPaused
    else -> RestBackground
}

@Composable
private fun CountdownContent(secondsRemaining: Int, onStop: () -> Unit) {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(horizontal = 32.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.SpaceEvenly,
    ) {
        Text(
            text = "GET READY",
            style = MaterialTheme.typography.headlineMedium,
            color = OnPhaseBackground,
        )
        Text(
            text = "$secondsRemaining",
            style = MaterialTheme.typography.displayLarge,
            color = OnPhaseBackground,
        )
        Button(
            onClick = onStop,
            modifier = Modifier.fillMaxWidth().height(64.dp),
            colors = ButtonDefaults.buttonColors(
                containerColor = OnPhaseBackground.copy(alpha = 0.2f),
                contentColor = OnPhaseBackground,
            ),
        ) {
            Icon(Icons.Default.Stop, contentDescription = "Stop", modifier = Modifier.size(28.dp))
            Spacer(Modifier.width(8.dp))
            Text("STOP", style = MaterialTheme.typography.titleMedium)
        }
    }
}

@Composable
private fun CompletionContent(totalElapsedMillis: Long, onDone: () -> Unit) {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(horizontal = 32.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.SpaceEvenly,
    ) {
        Icon(
            Icons.Default.CheckCircle,
            contentDescription = null,
            modifier = Modifier.size(72.dp),
            tint = OnPhaseBackground,
        )
        Text(
            text = "Workout Complete!",
            style = MaterialTheme.typography.headlineMedium,
            color = OnPhaseBackground,
            textAlign = TextAlign.Center,
        )
        Column(horizontalAlignment = Alignment.CenterHorizontally) {
            Text(
                text = "TOTAL TIME",
                style = MaterialTheme.typography.titleLarge,
                color = OnPhaseBackground.copy(alpha = 0.7f),
            )
            Text(
                text = totalElapsedMillis.formatElapsed(),
                style = MaterialTheme.typography.headlineLarge,
                color = OnPhaseBackground,
            )
        }
        Button(
            onClick = onDone,
            modifier = Modifier.fillMaxWidth().height(64.dp),
            colors = ButtonDefaults.buttonColors(
                containerColor = OnPhaseBackground.copy(alpha = 0.2f),
                contentColor = OnPhaseBackground,
            ),
        ) {
            Text("DONE", style = MaterialTheme.typography.titleLarge)
        }
    }
}

@Composable
private fun RunningContent(
    state: TabataSessionUiState,
    onPauseResume: () -> Unit,
    onStop: () -> Unit,
) {
    val isPaused = state.status == SessionStatus.Paused

    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(horizontal = 32.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.SpaceEvenly,
    ) {
        // Round counter
        Column(horizontalAlignment = Alignment.CenterHorizontally) {
            Text(
                text = "ROUND",
                style = MaterialTheme.typography.headlineMedium,
                color = OnPhaseBackground.copy(alpha = 0.7f),
            )
            Text(
                text = "${state.currentRound} / ${state.totalRounds}",
                style = MaterialTheme.typography.displaySmall,
                color = OnPhaseBackground,
                textAlign = TextAlign.Center,
            )
        }

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

        // Elapsed time + overall progress
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
            Spacer(Modifier.height(8.dp))
            @Suppress("DEPRECATION")
            LinearProgressIndicator(
                progress = state.progressFraction,
                modifier = Modifier.fillMaxWidth(),
                color = OnPhaseBackground,
                trackColor = OnPhaseBackground.copy(alpha = 0.2f),
            )
        }

        Spacer(Modifier.height(8.dp))

        // Controls
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(12.dp),
        ) {
            Button(
                onClick = onPauseResume,
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
                onClick = onStop,
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

@Composable
private fun ExitConfirmDialog(onConfirm: () -> Unit, onDismiss: () -> Unit) {
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Exit workout?") },
        text = { Text("Your progress in this session will be lost.") },
        confirmButton = {
            TextButton(onClick = onConfirm) {
                Text("Exit", color = MaterialTheme.colorScheme.error)
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) { Text("Cancel") }
        },
    )
}
