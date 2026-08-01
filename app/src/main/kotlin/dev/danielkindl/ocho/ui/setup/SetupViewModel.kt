package dev.danielkindl.ocho.ui.setup

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import dev.danielkindl.ocho.domain.model.Preset
import dev.danielkindl.ocho.domain.model.formatDuration
import dev.danielkindl.ocho.domain.model.minutesSecondsToMillis
import dev.danielkindl.ocho.domain.repository.PresetRepository
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
 * Picker state for the EMOM setup screen.
 *
 * Holds minutes and seconds as the pickers show them and derives milliseconds on
 * demand, so nothing has to be kept in sync as the user scrolls.
 *
 * Defaults to a 20-minute workout on 1-minute intervals: the canonical EMOM, and
 * startable without touching a picker.
 *
 * @property totalMinutes minutes component of the total duration.
 * @property totalSeconds seconds component of the total duration.
 * @property intervalMinutes minutes component of the interval.
 * @property intervalSeconds seconds component of the interval.
 */
data class SetupUiState(
    val totalMinutes: Int = 20,
    val totalSeconds: Int = 0,
    val intervalMinutes: Int = 1,
    val intervalSeconds: Int = 0,
) {
    /** Total duration in milliseconds, as the engine wants it. */
    val totalDurationMillis: Long
        get() = minutesSecondsToMillis(totalMinutes, totalSeconds)

    /** Interval length in milliseconds, as the engine wants it. */
    val intervalMillis: Long
        get() = minutesSecondsToMillis(intervalMinutes, intervalSeconds)

    /** Whether START may be enabled. Both durations must be non-zero. */
    val isValid: Boolean
        get() = totalDurationMillis > 0 && intervalMillis > 0

    /** True when interval exceeds total — no interval events will fire. */
    val intervalExceedsTotal: Boolean
        get() = isValid && intervalMillis > totalDurationMillis

    /** Suggested preset name, e.g. `20min / 1min`, used when the user leaves the field blank. */
    fun defaultPresetName(): String =
        "${formatDuration(totalMinutes, totalSeconds)} / ${formatDuration(intervalMinutes, intervalSeconds)}"
}

/** Drives the EMOM setup screen: picker state and saved presets. */
@HiltViewModel
class SetupViewModel @Inject constructor(
    private val presetRepository: PresetRepository,
) : ViewModel() {

    private val _uiState = MutableStateFlow(SetupUiState())

    /** Current picker values. */
    val uiState: StateFlow<SetupUiState> = _uiState.asStateFlow()

    /** Saved EMOM presets, refreshed automatically after a save or delete. */
    val presets: StateFlow<List<Preset>> = presetRepository.getPresets()
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), emptyList())

    /** Sets total minutes, clamped to the picker's 0..99 range. */
    fun setTotalMinutes(value: Int) = _uiState.update { it.copy(totalMinutes = value.coerceIn(0, 99)) }

    /** Sets total seconds, clamped to 0..59. */
    fun setTotalSeconds(value: Int) = _uiState.update { it.copy(totalSeconds = value.coerceIn(0, 59)) }

    /** Sets interval minutes, clamped to the picker's 0..99 range. */
    fun setIntervalMinutes(value: Int) = _uiState.update { it.copy(intervalMinutes = value.coerceIn(0, 99)) }

    /** Sets interval seconds, clamped to 0..59. */
    fun setIntervalSeconds(value: Int) = _uiState.update { it.copy(intervalSeconds = value.coerceIn(0, 59)) }

    /** Replaces the current picker values with [preset]'s. */
    fun loadPreset(preset: Preset) {
        _uiState.update {
            it.copy(
                totalMinutes = preset.totalMinutes,
                totalSeconds = preset.totalSeconds,
                intervalMinutes = preset.intervalMinutes,
                intervalSeconds = preset.intervalSeconds,
            )
        }
    }

    /** Saves the current values under [name], falling back to [SetupUiState.defaultPresetName] if blank. */
    fun savePreset(name: String) {
        val state = _uiState.value
        val preset = Preset(
            id = UUID.randomUUID().toString(),
            name = name.trim().ifEmpty { state.defaultPresetName() },
            totalMinutes = state.totalMinutes,
            totalSeconds = state.totalSeconds,
            intervalMinutes = state.intervalMinutes,
            intervalSeconds = state.intervalSeconds,
        )
        viewModelScope.launch { presetRepository.savePreset(preset) }
    }

    /** Deletes the preset with [id]; [presets] updates on its own. */
    fun deletePreset(id: String) {
        viewModelScope.launch { presetRepository.deletePreset(id) }
    }
}
