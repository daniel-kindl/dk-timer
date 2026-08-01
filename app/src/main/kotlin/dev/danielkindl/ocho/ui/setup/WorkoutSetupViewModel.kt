package dev.danielkindl.ocho.ui.setup

import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import dev.danielkindl.ocho.domain.model.WorkoutMode
import dev.danielkindl.ocho.domain.model.WorkoutPreset
import dev.danielkindl.ocho.domain.repository.WorkoutPresetRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import java.util.UUID
import javax.inject.Inject

/**
 * Drives the setup screen for whichever mode it was opened with.
 *
 * One view model for all modes, replacing the near-identical EMOM and Tabata pair.
 * The mode arrives as a navigation argument and never changes for the lifetime of
 * the screen, so it is read once rather than held as mutable state.
 */
@HiltViewModel
class WorkoutSetupViewModel @Inject constructor(
    private val presetRepository: WorkoutPresetRepository,
    savedStateHandle: SavedStateHandle,
) : ViewModel() {

    private val mode: WorkoutMode =
        WorkoutMode.valueOf(checkNotNull(savedStateHandle["mode"]))

    private val _uiState = MutableStateFlow(WorkoutSetupUiState(mode = mode))

    /** Current picker values. */
    val uiState: StateFlow<WorkoutSetupUiState> = _uiState.asStateFlow()

    /** Saved presets for this mode only, refreshed automatically after a save or delete. */
    val presets: StateFlow<List<WorkoutPreset>> = presetRepository.getPresets(mode)
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(SUBSCRIBE_TIMEOUT_MS), emptyList())

    /** Sets total minutes, clamped to the picker's range. */
    fun setTotalMinutes(value: Int) =
        _uiState.update { it.copy(totalMinutes = value.coerceIn(0, MAX_MINUTES)) }

    /** Sets total seconds, clamped to the picker's range. */
    fun setTotalSeconds(value: Int) =
        _uiState.update { it.copy(totalSeconds = value.coerceIn(0, MAX_SECONDS)) }

    /** Sets EMOM interval minutes, clamped to the picker's range. */
    fun setIntervalMinutes(value: Int) =
        _uiState.update { it.copy(intervalMinutes = value.coerceIn(0, MAX_MINUTES)) }

    /** Sets EMOM interval seconds, clamped to the picker's range. */
    fun setIntervalSeconds(value: Int) =
        _uiState.update { it.copy(intervalSeconds = value.coerceIn(0, MAX_SECONDS)) }

    /** Sets Tabata work minutes, clamped to the picker's range. */
    fun setWorkMinutes(value: Int) =
        _uiState.update { it.copy(workMinutes = value.coerceIn(0, MAX_MINUTES)) }

    /** Sets Tabata work seconds, clamped to the picker's range. */
    fun setWorkSeconds(value: Int) =
        _uiState.update { it.copy(workSeconds = value.coerceIn(0, MAX_SECONDS)) }

    /** Sets Tabata rest minutes, clamped to the picker's range. */
    fun setRestMinutes(value: Int) =
        _uiState.update { it.copy(restMinutes = value.coerceIn(0, MAX_MINUTES)) }

    /** Sets Tabata rest seconds, clamped to the picker's range. */
    fun setRestSeconds(value: Int) =
        _uiState.update { it.copy(restSeconds = value.coerceIn(0, MAX_SECONDS)) }

    /** Replaces the current picker values with [preset]'s. */
    fun loadPreset(preset: WorkoutPreset) = _uiState.update { it.withPreset(preset) }

    /** Saves the current values under [name], falling back to the generated name if blank. */
    fun savePreset(name: String) {
        val preset = _uiState.value.toPreset(id = UUID.randomUUID().toString(), name = name)
        viewModelScope.launch { presetRepository.savePreset(preset) }
    }

    /** Deletes the preset with [id]; [presets] updates on its own. */
    fun deletePreset(id: String) {
        viewModelScope.launch { presetRepository.deletePreset(id) }
    }

    private companion object {
        const val MAX_MINUTES = 99
        const val MAX_SECONDS = 59
        const val SUBSCRIBE_TIMEOUT_MS = 5_000L
    }
}
