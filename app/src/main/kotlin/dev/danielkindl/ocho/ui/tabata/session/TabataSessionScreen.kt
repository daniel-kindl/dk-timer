package dev.danielkindl.ocho.ui.tabata.session

import android.provider.Settings
import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.EaseInOut
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
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import dev.danielkindl.ocho.core.format.formatCountdown
import dev.danielkindl.ocho.core.format.formatElapsed
import dev.danielkindl.ocho.domain.model.SessionStatus
import dev.danielkindl.ocho.domain.model.TabataPhase
import dev.danielkindl.ocho.ui.components.SessionLifecycleScaffold
import dev.danielkindl.ocho.ui.components.SessionProgressBar
import dev.danielkindl.ocho.ui.theme.Green500
import dev.danielkindl.ocho.ui.theme.Green700
import dev.danielkindl.ocho.ui.theme.JetBrainsMonoFamily
import dev.danielkindl.ocho.ui.theme.N0
import dev.danielkindl.ocho.ui.theme.N950
import dev.danielkindl.ocho.ui.theme.Red500
import dev.danielkindl.ocho.ui.theme.Red700
import dev.danielkindl.ocho.ui.theme.SpaceGroteskFamily

private const val PHASE_TRANSITION_MILLIS = 340

private val WorkBackground = Red500
private val RestBackground = Green500
private val WorkBackgroundPaused = Red700
private val RestBackgroundPaused = Green700
private val NeutralBackground = N950
private val OnPhaseBackground = N0

@Composable
private fun rememberReducedMotionEnabled(): Boolean {
    val contentResolver = LocalContext.current.contentResolver
    return remember {
        Settings.Global.getFloat(contentResolver, Settings.Global.ANIMATOR_DURATION_SCALE, 1f) == 0f
    }
}

/**
 * A running Tabata session. Like the EMOM screen, but the entire background carries
 * the phase colour so work and rest are readable from across the room without
 * focusing on any text.
 *
 * @param onSessionFinished invoked on an explicit stop, not on completion.
 */
@Composable
fun TabataSessionScreen(
    onSessionFinished: () -> Unit,
    viewModel: TabataSessionViewModel = hiltViewModel(),
) {
    val state by viewModel.uiState.collectAsStateWithLifecycle()

    SessionLifecycleScaffold(
        status = state.status,
        onSessionFinished = onSessionFinished,
        onStopSession = viewModel::stopSession,
    ) { onRequestExit ->
        val isPaused = state.status == SessionStatus.Paused
        val reducedMotion = rememberReducedMotionEnabled()
        val background by animateColorAsState(
            targetValue = tabataBackgroundColor(status = state.status, phase = state.phase, isPaused = isPaused),
            animationSpec = if (reducedMotion) {
                tween(durationMillis = 0)
            } else {
                tween(durationMillis = PHASE_TRANSITION_MILLIS, easing = EaseInOut)
            },
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
                    onStop = onRequestExit,
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
                    onStop = onRequestExit,
                )
            }
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
            style = MaterialTheme.typography.displayLarge.copy(fontFamily = SpaceGroteskFamily),
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
                style = MaterialTheme.typography.headlineLarge.copy(fontFamily = JetBrainsMonoFamily),
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
                style = MaterialTheme.typography.displaySmall.copy(fontFamily = JetBrainsMonoFamily),
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
            style = MaterialTheme.typography.displayLarge.copy(fontFamily = SpaceGroteskFamily),
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
                style = MaterialTheme.typography.headlineLarge.copy(fontFamily = JetBrainsMonoFamily),
                color = OnPhaseBackground,
            )
            Spacer(Modifier.height(8.dp))
            SessionProgressBar(
                progress = state.progressFraction,
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
