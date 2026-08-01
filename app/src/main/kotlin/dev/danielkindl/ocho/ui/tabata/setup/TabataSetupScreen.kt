package dev.danielkindl.ocho.ui.tabata.setup

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
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
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import dev.danielkindl.ocho.domain.model.TabataPreset
import dev.danielkindl.ocho.ui.components.DeletePresetDialog
import dev.danielkindl.ocho.ui.components.DurationPicker
import dev.danielkindl.ocho.ui.components.PresetsSection
import dev.danielkindl.ocho.ui.components.SavePresetDialog

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun TabataSetupScreen(
    onStartSession: (totalDurationMillis: Long, workMillis: Long, restMillis: Long) -> Unit,
    onNavigateUp: () -> Unit,
    viewModel: TabataSetupViewModel = hiltViewModel(),
) {
    val state by viewModel.uiState.collectAsStateWithLifecycle()
    val presets by viewModel.presets.collectAsStateWithLifecycle()
    var showSaveDialog by rememberSaveable { mutableStateOf(false) }
    var dialogPresetName by rememberSaveable { mutableStateOf("") }
    var presetToDelete by remember { mutableStateOf<TabataPreset?>(null) }

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
                title = { Text("Tabata") },
                navigationIcon = {
                    IconButton(onClick = onNavigateUp) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back")
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
                label = "Total Duration",
                minutes = state.totalMinutes,
                seconds = state.totalSeconds,
                onMinutesChange = viewModel::setTotalMinutes,
                onSecondsChange = viewModel::setTotalSeconds,
            )

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
                onClick = {
                    onStartSession(state.totalDurationMillis, state.workMillis, state.restMillis)
                },
                enabled = state.isValid,
                modifier = Modifier
                    .fillMaxWidth()
                    .height(52.dp),
            ) {
                Text("START", style = MaterialTheme.typography.titleLarge)
            }
        }
    }
}
