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

data class TabataSetupUiState(
    val totalMinutes: Int = 20,
    val totalSeconds: Int = 0,
    val workMinutes: Int = 0,
    val workSeconds: Int = 45,
    val restMinutes: Int = 0,
    val restSeconds: Int = 15,
) {
    val totalDurationMillis: Long
        get() = minutesSecondsToMillis(totalMinutes, totalSeconds)

    val workMillis: Long
        get() = minutesSecondsToMillis(workMinutes, workSeconds)

    val restMillis: Long
        get() = minutesSecondsToMillis(restMinutes, restSeconds)

    val isValid: Boolean
        get() = totalDurationMillis > 0 && workMillis > 0 && restMillis > 0

    fun defaultPresetName(): String {
        val total = formatDuration(totalMinutes, totalSeconds)
        val work = formatDuration(workMinutes, workSeconds)
        val rest = formatDuration(restMinutes, restSeconds)
        return "$total / $work work / $rest rest"
    }
}

@HiltViewModel
class TabataSetupViewModel @Inject constructor(
    private val presetRepository: TabataPresetRepository,
) : ViewModel() {

    private val _uiState = MutableStateFlow(TabataSetupUiState())
    val uiState: StateFlow<TabataSetupUiState> = _uiState.asStateFlow()

    val presets: StateFlow<List<TabataPreset>> = presetRepository.getPresets()
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), emptyList())

    fun setTotalMinutes(value: Int) = _uiState.update { it.copy(totalMinutes = value.coerceIn(0, 99)) }
    fun setTotalSeconds(value: Int) = _uiState.update { it.copy(totalSeconds = value.coerceIn(0, 59)) }
    fun setWorkMinutes(value: Int) = _uiState.update { it.copy(workMinutes = value.coerceIn(0, 99)) }
    fun setWorkSeconds(value: Int) = _uiState.update { it.copy(workSeconds = value.coerceIn(0, 59)) }
    fun setRestMinutes(value: Int) = _uiState.update { it.copy(restMinutes = value.coerceIn(0, 99)) }
    fun setRestSeconds(value: Int) = _uiState.update { it.copy(restSeconds = value.coerceIn(0, 59)) }

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

    fun deletePreset(id: String) {
        viewModelScope.launch { presetRepository.deletePreset(id) }
    }
}
