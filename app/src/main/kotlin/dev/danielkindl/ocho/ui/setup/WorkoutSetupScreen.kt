package dev.danielkindl.ocho.ui.setup

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.material3.Button
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.unit.dp
import androidx.hilt.lifecycle.viewmodel.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import dev.danielkindl.ocho.R
import dev.danielkindl.ocho.domain.model.PREPARE_COUNTDOWN_MILLIS
import dev.danielkindl.ocho.domain.model.SessionRequest
import dev.danielkindl.ocho.domain.model.WorkoutMode
import dev.danielkindl.ocho.domain.model.WorkoutPreset
import dev.danielkindl.ocho.ui.components.DeletePresetDialog
import dev.danielkindl.ocho.ui.components.DurationPicker
import dev.danielkindl.ocho.ui.components.ErrorPlate
import dev.danielkindl.ocho.ui.components.PresetsSection
import dev.danielkindl.ocho.ui.components.RunTimeline
import dev.danielkindl.ocho.ui.components.SavePresetDialog
import dev.danielkindl.ocho.ui.components.amrapSegments
import dev.danielkindl.ocho.ui.components.emomSegments
import dev.danielkindl.ocho.ui.components.tabataSegments

/**
 * Configures a workout of any mode.
 *
 * Replaces the separate EMOM and Tabata setup screens, which differed only in which
 * duration pickers they showed. The mode arrives as a navigation argument and
 * selects the pickers, the labels and the run timeline shape.
 *
 * @param onStartSession receives the assembled request; navigation is the caller's
 *   concern, so this screen stays independent of the nav graph.
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun WorkoutSetupScreen(
    onStartSession: (SessionRequest) -> Unit,
    onNavigateUp: () -> Unit,
    viewModel: WorkoutSetupViewModel = hiltViewModel(),
) {
    val state by viewModel.uiState.collectAsStateWithLifecycle()
    val presets by viewModel.presets.collectAsStateWithLifecycle()
    var showSaveDialog by rememberSaveable { mutableStateOf(false) }
    var dialogPresetName by rememberSaveable { mutableStateOf("") }
    var presetToDelete by remember { mutableStateOf<WorkoutPreset?>(null) }

    if (showSaveDialog) {
        SavePresetDialog(
            name = dialogPresetName,
            onNameChange = { dialogPresetName = it },
            onSave = {
                viewModel.savePreset(dialogPresetName)
                showSaveDialog = false
            },
            onDismiss = { showSaveDialog = false },
        )
    }

    presetToDelete?.let { preset ->
        DeletePresetDialog(
            presetName = preset.name,
            onConfirm = {
                viewModel.deletePreset(preset.id)
                presetToDelete = null
            },
            onDismiss = { presetToDelete = null },
        )
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text(state.mode.title()) },
                navigationIcon = {
                    IconButton(onClick = onNavigateUp) {
                        Icon(painterResource(R.drawable.ic_arrow_left), contentDescription = "Back")
                    }
                },
            )
        },
    ) { padding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
                .padding(horizontal = 24.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.SpaceBetween,
        ) {
            DurationPicker(
                label = "Total duration",
                minutes = state.totalMinutes,
                seconds = state.totalSeconds,
                onMinutesChange = viewModel::setTotalMinutes,
                onSecondsChange = viewModel::setTotalSeconds,
            )

            if (state.mode == WorkoutMode.EMOM) {
                HorizontalDivider()
                DurationPicker(
                    label = "Interval",
                    minutes = state.intervalMinutes,
                    seconds = state.intervalSeconds,
                    onMinutesChange = viewModel::setIntervalMinutes,
                    onSecondsChange = viewModel::setIntervalSeconds,
                )
            }

            if (state.mode == WorkoutMode.TABATA) {
                HorizontalDivider()
                DurationPicker(
                    label = "Work",
                    minutes = state.workMinutes,
                    seconds = state.workSeconds,
                    onMinutesChange = viewModel::setWorkMinutes,
                    onSecondsChange = viewModel::setWorkSeconds,
                )
                HorizontalDivider()
                DurationPicker(
                    label = "Rest",
                    minutes = state.restMinutes,
                    seconds = state.restSeconds,
                    onMinutesChange = viewModel::setRestMinutes,
                    onSecondsChange = viewModel::setRestSeconds,
                )
            }

            if (state.isValid) {
                RunTimeline(
                    segments = state.timelineSegments(),
                    patternLabel = state.patternLabel,
                    totalMillis = state.totalDurationMillis,
                )
            }

            if (state.intervalExceedsTotal) {
                ErrorPlate(
                    message = "The interval is longer than the total duration, " +
                        "so no interval beeps will fire. Shorten the interval or " +
                        "lengthen the workout.",
                )
            }

            PresetsSection(
                presets = presets,
                getKey = { it.id },
                getLabel = { it.name },
                onPresetClick = viewModel::loadPreset,
                onDeleteClick = { presetToDelete = it },
                onSavePreset = {
                    dialogPresetName = state.defaultPresetName()
                    showSaveDialog = true
                },
                saveEnabled = state.isValid,
            )

            Button(
                onClick = { onStartSession(state.toRequest()) },
                enabled = state.isValid,
                modifier = Modifier
                    .fillMaxWidth()
                    .height(52.dp),
            ) {
                Icon(
                    painterResource(R.drawable.ic_play),
                    contentDescription = null,
                    modifier = Modifier.size(20.dp),
                )
                Spacer(Modifier.width(10.dp))
                Text("Start", style = MaterialTheme.typography.titleLarge)
            }
        }
    }
}

/** Screen title for a mode. EMOM and AMRAP are acronyms and stay capitalised. */
private fun WorkoutMode.title(): String = when (this) {
    WorkoutMode.EMOM -> "EMOM"
    WorkoutMode.TABATA -> "Tabata"
    WorkoutMode.AMRAP -> "AMRAP"
}

/** Builds the run timeline preview for whichever mode is configured. */
private fun WorkoutSetupUiState.timelineSegments() = when (mode) {
    WorkoutMode.EMOM -> emomSegments(
        prepareMillis = PREPARE_COUNTDOWN_MILLIS,
        totalMillis = totalDurationMillis,
    )

    WorkoutMode.TABATA -> tabataSegments(
        prepareMillis = PREPARE_COUNTDOWN_MILLIS,
        workMillis = workMillis,
        restMillis = restMillis,
        totalMillis = totalDurationMillis,
    )

    WorkoutMode.AMRAP -> amrapSegments(
        prepareMillis = PREPARE_COUNTDOWN_MILLIS,
        totalMillis = totalDurationMillis,
    )
}
