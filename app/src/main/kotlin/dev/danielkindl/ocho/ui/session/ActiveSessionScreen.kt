package dev.danielkindl.ocho.ui.session

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
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import dev.danielkindl.ocho.R
import dev.danielkindl.ocho.core.format.formatCountdown
import dev.danielkindl.ocho.core.format.formatElapsed
import dev.danielkindl.ocho.domain.model.SessionStatus
import dev.danielkindl.ocho.ui.components.PhaseClock
import dev.danielkindl.ocho.ui.components.PhaseLabel
import dev.danielkindl.ocho.ui.components.PhaseScaffold
import dev.danielkindl.ocho.ui.components.PrimarySessionControl
import dev.danielkindl.ocho.ui.components.SUBDUED_ON_PLATE
import dev.danielkindl.ocho.ui.components.SecondarySessionControl
import dev.danielkindl.ocho.ui.components.SessionColumn
import dev.danielkindl.ocho.ui.components.SessionLifecycleScaffold
import dev.danielkindl.ocho.ui.theme.Phase

/**
 * A running EMOM session.
 *
 * Shares the phase colour system with Tabata, but EMOM has no rest interval — the
 * whole session is one continuous work phase, so the plate stays red between the
 * amber prepare countdown and the violet completion screen. The clock counts down to
 * the next interval beep rather than to a phase change.
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
        PhaseScaffold(phase = state.phaseColour()) { theme ->
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

/**
 * Maps session lifecycle onto the phase colour model.
 *
 * EMOM never reports [Phase.REST]: every interval is work. Pausing keeps the work
 * plate, because the user is still inside the work interval — the stopped clock and
 * the changed control label carry that state instead.
 */
private fun SessionUiState.phaseColour(): Phase = when (status) {
    SessionStatus.CountingDown -> Phase.PREPARE
    SessionStatus.Completed -> Phase.COMPLETE
    else -> Phase.WORK
}

@Composable
private fun PrepareContent(secondsRemaining: Int, onPlate: Color, onStop: () -> Unit) {
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
    onPlate: Color,
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
    state: SessionUiState,
    onPlate: Color,
    onPauseResume: () -> Unit,
    onStop: () -> Unit,
) {
    val isPaused = state.status == SessionStatus.Paused

    SessionColumn {
        PhaseLabel(if (isPaused) "Work · paused" else "Work", onPlate)

        Column(horizontalAlignment = Alignment.CenterHorizontally) {
            PhaseClock(state.remainingInIntervalMillis.formatCountdown(), onPlate)
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
