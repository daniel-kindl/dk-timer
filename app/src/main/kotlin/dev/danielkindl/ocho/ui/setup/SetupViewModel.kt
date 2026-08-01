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

data class SetupUiState(
    val totalMinutes: Int = 20,
    val totalSeconds: Int = 0,
    val intervalMinutes: Int = 1,
    val intervalSeconds: Int = 0,
) {
    val totalDurationMillis: Long
        get() = minutesSecondsToMillis(totalMinutes, totalSeconds)

    val intervalMillis: Long
        get() = minutesSecondsToMillis(intervalMinutes, intervalSeconds)

    val isValid: Boolean
        get() = totalDurationMillis > 0 && intervalMillis > 0

    /** True when interval exceeds total — no interval events will fire. */
    val intervalExceedsTotal: Boolean
        get() = isValid && intervalMillis > totalDurationMillis

    fun defaultPresetName(): String =
        "${formatDuration(totalMinutes, totalSeconds)} / ${formatDuration(intervalMinutes, intervalSeconds)}"
}

@HiltViewModel
class SetupViewModel @Inject constructor(
    private val presetRepository: PresetRepository,
) : ViewModel() {

    private val _uiState = MutableStateFlow(SetupUiState())
    val uiState: StateFlow<SetupUiState> = _uiState.asStateFlow()

    val presets: StateFlow<List<Preset>> = presetRepository.getPresets()
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), emptyList())

    fun setTotalMinutes(value: Int) = _uiState.update { it.copy(totalMinutes = value.coerceIn(0, 99)) }
    fun setTotalSeconds(value: Int) = _uiState.update { it.copy(totalSeconds = value.coerceIn(0, 59)) }
    fun setIntervalMinutes(value: Int) = _uiState.update { it.copy(intervalMinutes = value.coerceIn(0, 99)) }
    fun setIntervalSeconds(value: Int) = _uiState.update { it.copy(intervalSeconds = value.coerceIn(0, 59)) }

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

    fun deletePreset(id: String) {
        viewModelScope.launch { presetRepository.deletePreset(id) }
    }
}
