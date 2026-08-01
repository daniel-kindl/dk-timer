package dev.danielkindl.ocho.ui.session

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
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import dev.danielkindl.ocho.core.format.formatCountdown
import dev.danielkindl.ocho.core.format.formatElapsed
import dev.danielkindl.ocho.domain.model.SessionStatus
import dev.danielkindl.ocho.ui.components.SessionLifecycleScaffold
import dev.danielkindl.ocho.ui.components.SessionProgressBar
import dev.danielkindl.ocho.ui.theme.JetBrainsMonoFamily
import dev.danielkindl.ocho.ui.theme.SpaceGroteskFamily

/**
 * A running EMOM session: countdown, round counter, progress, and transport controls.
 *
 * Built for glancing at from across a room mid-effort — the remaining-time numeral
 * dominates and everything else is secondary.
 *
 * @param onSessionFinished invoked on an explicit stop, not on completion, which
 *   shows its own summary first.
 */
@Composable
fun ActiveSessionScreen(
    onSessionFinished: () -> Unit,
    viewModel: SessionViewModel = hiltViewModel(),
) {
    val state by viewModel.uiState.collectAsStateWithLifecycle()

    SessionLifecycleScaffold(
        status = state.status,
        onSessionFinished = onSessionFinished,
        onStopSession = viewModel::stopSession,
    ) { onRequestExit ->
        Scaffold { padding ->
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(padding),
                contentAlignment = Alignment.Center,
            ) {
                when (state.status) {
                    SessionStatus.CountingDown -> CountdownContent(
                        secondsRemaining = state.countdownSecondsRemaining,
                        onStop = onRequestExit,
                    )

                    SessionStatus.Completed -> CompletionContent(
                        totalElapsedMillis = state.elapsedMillis,
                        onDone = onSessionFinished,
                    )

                    else -> RunningContent(
                        state = state,
                        onPauseResume = {
                            if (state.status == SessionStatus.Paused) {
                                viewModel.resumeSession()
                            } else {
                                viewModel.pauseSession()
                            }
                        },
                        onStop = onRequestExit,
                    )
                }
            }
        }
    }
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
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
        Text(
            text = "$secondsRemaining",
            style = MaterialTheme.typography.displayLarge.copy(fontFamily = SpaceGroteskFamily),
            color = MaterialTheme.colorScheme.primary,
        )
        Button(
            onClick = onStop,
            modifier = Modifier.fillMaxWidth().height(64.dp),
            colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.error),
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
            tint = MaterialTheme.colorScheme.primary,
        )
        Text(
            text = "Workout Complete!",
            style = MaterialTheme.typography.headlineMedium,
            color = MaterialTheme.colorScheme.onSurface,
            textAlign = TextAlign.Center,
        )
        Column(horizontalAlignment = Alignment.CenterHorizontally) {
            Text(
                text = "TOTAL TIME",
                style = MaterialTheme.typography.titleLarge,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
            Text(
                text = totalElapsedMillis.formatElapsed(),
                style = MaterialTheme.typography.headlineLarge.copy(fontFamily = JetBrainsMonoFamily),
                color = MaterialTheme.colorScheme.onSurface,
            )
        }
        Button(
            onClick = onDone,
            modifier = Modifier.fillMaxWidth().height(64.dp),
        ) {
            Text("DONE", style = MaterialTheme.typography.titleLarge)
        }
    }
}

@Composable
private fun RunningContent(
    state: SessionUiState,
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
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
            Text(
                text = "${state.currentRound} / ${state.totalRounds}",
                style = MaterialTheme.typography.displayLarge.copy(fontFamily = JetBrainsMonoFamily),
                color = MaterialTheme.colorScheme.primary,
                textAlign = TextAlign.Center,
            )
        }

        // Countdown to next beep
        Column(horizontalAlignment = Alignment.CenterHorizontally) {
            Text(
                text = if (isPaused) "⏸  PAUSED" else "NEXT BEEP IN",
                style = MaterialTheme.typography.headlineMedium,
                color = if (isPaused)
                    MaterialTheme.colorScheme.primary
                else
                    MaterialTheme.colorScheme.onSurfaceVariant,
            )
            Text(
                text = state.remainingInIntervalMillis.formatCountdown(),
                style = MaterialTheme.typography.displayMedium.copy(fontFamily = SpaceGroteskFamily),
                color = MaterialTheme.colorScheme.onSurface,
                textAlign = TextAlign.Center,
            )
        }

        // Elapsed time + overall progress
        Column(horizontalAlignment = Alignment.CenterHorizontally) {
            Text(
                text = "ELAPSED",
                style = MaterialTheme.typography.titleLarge,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
            Text(
                text = state.elapsedMillis.formatElapsed(),
                style = MaterialTheme.typography.headlineLarge.copy(fontFamily = JetBrainsMonoFamily),
                color = MaterialTheme.colorScheme.onSurface,
            )
            Spacer(Modifier.height(8.dp))
            SessionProgressBar(progress = state.progressFraction)
        }

        Spacer(Modifier.height(8.dp))

        // Pause / Resume + Stop buttons
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
                    containerColor = if (isPaused)
                        MaterialTheme.colorScheme.primary
                    else
                        MaterialTheme.colorScheme.secondary,
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
                    containerColor = MaterialTheme.colorScheme.error,
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
