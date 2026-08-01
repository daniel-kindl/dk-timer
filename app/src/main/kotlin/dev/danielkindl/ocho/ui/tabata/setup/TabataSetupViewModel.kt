package dev.danielkindl.ocho.ui.tabata.setup

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import dev.danielkindl.ocho.domain.model.TabataPreset
import dev.danielkindl.ocho.domain.model.formatDuration
import dev.danielkindl.ocho.domain.model.minutesSecondsToMillis
import dev.danielkindl.ocho.domain.repository.TabataPresetRepository
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
 * Picker state for the Tabata setup screen. The Tabata counterpart to
 * `SetupUiState`, holding a work/rest pair instead of a single interval.
 *
 * Defaults to 45s work / 15s rest over 20 minutes — a common HIIT ratio rather than
 * the strict 20/10 protocol, which is punishing as an out-of-the-box default.
 *
 * @property totalMinutes minutes component of the total duration.
 * @property totalSeconds seconds component of the total duration.
 * @property workMinutes minutes component of the work phase.
 * @property workSeconds seconds component of the work phase.
 * @property restMinutes minutes component of the rest phase.
 * @property restSeconds seconds component of the rest phase.
 */
data class TabataSetupUiState(
    val totalMinutes: Int = 20,
    val totalSeconds: Int = 0,
    val workMinutes: Int = 0,
    val workSeconds: Int = 45,
    val restMinutes: Int = 0,
    val restSeconds: Int = 15,
) {
    /** Total duration in milliseconds, as the engine wants it. */
    val totalDurationMillis: Long
        get() = minutesSecondsToMillis(totalMinutes, totalSeconds)

    /** Work phase length in milliseconds. */
    val workMillis: Long
        get() = minutesSecondsToMillis(workMinutes, workSeconds)

    /** Rest phase length in milliseconds. */
    val restMillis: Long
        get() = minutesSecondsToMillis(restMinutes, restSeconds)

    /**
     * Whether START may be enabled. All three durations must be non-zero — zero work
     * and zero rest together would leave the engine's phase loop unable to advance.
     */
    val isValid: Boolean
        get() = totalDurationMillis > 0 && workMillis > 0 && restMillis > 0

    /** Suggested preset name, e.g. `20min / 45s work / 15s rest`, used when the field is blank. */
    fun defaultPresetName(): String {
        val total = formatDuration(totalMinutes, totalSeconds)
        val work = formatDuration(workMinutes, workSeconds)
        val rest = formatDuration(restMinutes, restSeconds)
        return "$total / $work work / $rest rest"
    }
}

/** Drives the Tabata setup screen: picker state and saved presets. */
@HiltViewModel
class TabataSetupViewModel @Inject constructor(
    private val presetRepository: TabataPresetRepository,
) : ViewModel() {

    private val _uiState = MutableStateFlow(TabataSetupUiState())

    /** Current picker values. */
    val uiState: StateFlow<TabataSetupUiState> = _uiState.asStateFlow()

    /** Saved Tabata presets, refreshed automatically after a save or delete. */
    val presets: StateFlow<List<TabataPreset>> = presetRepository.getPresets()
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), emptyList())

    /** Sets total minutes, clamped to the picker's 0..99 range. */
    fun setTotalMinutes(value: Int) = _uiState.update { it.copy(totalMinutes = value.coerceIn(0, 99)) }

    /** Sets total seconds, clamped to 0..59. */
    fun setTotalSeconds(value: Int) = _uiState.update { it.copy(totalSeconds = value.coerceIn(0, 59)) }

    /** Sets work-phase minutes, clamped to the picker's 0..99 range. */
    fun setWorkMinutes(value: Int) = _uiState.update { it.copy(workMinutes = value.coerceIn(0, 99)) }

    /** Sets work-phase seconds, clamped to 0..59. */
    fun setWorkSeconds(value: Int) = _uiState.update { it.copy(workSeconds = value.coerceIn(0, 59)) }

    /** Sets rest-phase minutes, clamped to the picker's 0..99 range. */
    fun setRestMinutes(value: Int) = _uiState.update { it.copy(restMinutes = value.coerceIn(0, 99)) }

    /** Sets rest-phase seconds, clamped to 0..59. */
    fun setRestSeconds(value: Int) = _uiState.update { it.copy(restSeconds = value.coerceIn(0, 59)) }

    /** Replaces the current picker values with [preset]'s. */
    fun loadPreset(preset: TabataPreset) {
        _uiState.update {
            it.copy(
                totalMinutes = preset.totalMinutes,
                totalSeconds = preset.totalSeconds,
                workMinutes = preset.workMinutes,
                workSeconds = preset.workSeconds,
                restMinutes = preset.restMinutes,
                restSeconds = preset.restSeconds,
            )
        }
    }

    /** Saves the current values under [name], falling back to [TabataSetupUiState.defaultPresetName] if blank. */
    fun savePreset(name: String) {
        val state = _uiState.value
        val preset = TabataPreset(
            id = UUID.randomUUID().toString(),
            name = name.trim().ifEmpty { state.defaultPresetName() },
            totalMinutes = state.totalMinutes,
            totalSeconds = state.totalSeconds,
            workMinutes = state.workMinutes,
            workSeconds = state.workSeconds,
            restMinutes = state.restMinutes,
            restSeconds = state.restSeconds,
        )
        viewModelScope.launch { presetRepository.savePreset(preset) }
    }

    /** Deletes the preset with [id]; [presets] updates on its own. */
    fun deletePreset(id: String) {
        viewModelScope.launch { presetRepository.deletePreset(id) }
    }
}
