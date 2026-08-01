package dev.danielkindl.ocho.ui.tabata.session

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import dev.danielkindl.ocho.R
import dev.danielkindl.ocho.core.format.formatCountdown
import dev.danielkindl.ocho.core.format.formatElapsed
import dev.danielkindl.ocho.domain.model.SessionStatus
import dev.danielkindl.ocho.domain.model.Phase
import dev.danielkindl.ocho.ui.components.PhaseClock
import dev.danielkindl.ocho.ui.components.PhaseLabel
import dev.danielkindl.ocho.ui.components.PhaseScaffold
import dev.danielkindl.ocho.ui.components.PrimarySessionControl
import dev.danielkindl.ocho.ui.components.SUBDUED_ON_PLATE
import dev.danielkindl.ocho.ui.components.SecondarySessionControl
import dev.danielkindl.ocho.ui.components.SessionColumn
import dev.danielkindl.ocho.ui.components.SessionLifecycleScaffold
import dev.danielkindl.ocho.domain.model.SessionSnapshot

/**
 * A running Tabata session.
 *
 * The full-bleed phase colour is the primary information channel — it answers "what
 * am I doing right now" from three metres away, before any text is read. Work and
 * rest differ by lightness as well as hue, so the distinction survives with no
 * colour vision at all, and the uppercase phase label carries the same information
 * redundantly.
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
        PhaseScaffold(phase = state.phase) { theme ->
            when (state.status) {
                SessionStatus.CountingDown -> PrepareContent(
                    secondsRemaining = state.countdownSecondsRemaining,
                    onPlate = theme.onPlate,
                    onStop = onRequestExit,
                )

                SessionStatus.Completed -> CompleteContent(
                    totalElapsedMillis = state.elapsedMillis,
                    rounds = state.totalRounds,
                    onPlate = theme.onPlate,
                    onDone = onSessionFinished,
                )

                else -> RunningContent(
                    state = state,
                    onPlate = theme.onPlate,
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


@Composable
private fun PrepareContent(
    secondsRemaining: Int,
    onPlate: androidx.compose.ui.graphics.Color,
    onStop: () -> Unit,
) {
    SessionColumn {
        PhaseLabel("Prepare", onPlate)
        PhaseClock(secondsRemaining.toString(), onPlate)
        SecondarySessionControl(
            label = "Stop",
            onPlate = onPlate,
            onClick = onStop,
            icon = painterResource(R.drawable.ic_square),
        )
    }
}

@Composable
private fun CompleteContent(
    totalElapsedMillis: Long,
    rounds: Int,
    onPlate: androidx.compose.ui.graphics.Color,
    onDone: () -> Unit,
) {
    SessionColumn {
        PhaseLabel("Complete", onPlate)
        Column(horizontalAlignment = Alignment.CenterHorizontally) {
            PhaseClock(totalElapsedMillis.formatElapsed(), onPlate)
            Spacer(Modifier.height(8.dp))
            Text(
                text = "$rounds rounds",
                style = MaterialTheme.typography.titleMedium,
                color = onPlate.copy(alpha = SUBDUED_ON_PLATE),
                textAlign = TextAlign.Center,
            )
        }
        SecondarySessionControl(label = "Done", onPlate = onPlate, onClick = onDone)
    }
}

@Composable
private fun RunningContent(
    state: SessionSnapshot,
    onPlate: androidx.compose.ui.graphics.Color,
    onPauseResume: () -> Unit,
    onStop: () -> Unit,
) {
    val isPaused = state.status == SessionStatus.Paused
    val phaseName = if (state.phase == Phase.WORK) "Work" else "Rest"

    SessionColumn {
        PhaseLabel(if (isPaused) "$phaseName · paused" else phaseName, onPlate)

        Column(horizontalAlignment = Alignment.CenterHorizontally) {
            PhaseClock(state.remainingInPhaseMillis.formatCountdown(), onPlate)
            Spacer(Modifier.height(12.dp))
            Text(
                text = "round ${state.currentRound}/${state.totalRounds}",
                style = MaterialTheme.typography.titleMedium,
                color = onPlate.copy(alpha = SUBDUED_ON_PLATE),
                textAlign = TextAlign.Center,
            )
            Spacer(Modifier.height(4.dp))
            Text(
                text = state.elapsedMillis.formatElapsed(),
                style = MaterialTheme.typography.labelSmall,
                color = onPlate.copy(alpha = SUBDUED_ON_PLATE),
                textAlign = TextAlign.Center,
            )
        }

        Column(
            modifier = Modifier.fillMaxWidth(),
            verticalArrangement = Arrangement.spacedBy(12.dp),
        ) {
            PrimarySessionControl(
                label = if (isPaused) "Resume" else "Pause",
                icon = painterResource(
                    if (isPaused) R.drawable.ic_play else R.drawable.ic_pause
                ),
                onPlate = onPlate,
                onClick = onPauseResume,
            )
            SecondarySessionControl(label = "Stop", onPlate = onPlate, onClick = onStop)
        }
    }
}
