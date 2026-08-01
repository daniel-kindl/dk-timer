package dev.danielkindl.ocho.domain.repository

import dev.danielkindl.ocho.domain.model.WorkoutMode
import dev.danielkindl.ocho.domain.model.WorkoutPreset
import kotlinx.coroutines.flow.Flow

/**
 * Stores saved workout configurations for every mode.
 *
 * One store rather than one per mode. Presets differ only in which duration fields
 * they use, so separate repositories meant duplicating persistence for each new
 * mode. Callers filter with [getPresets] instead.
 */
interface WorkoutPresetRepository {

    /**
     * Emits the presets for [mode], and again on every save or delete.
     *
     * Filtered here rather than by the caller so a setup screen cannot accidentally
     * offer another mode's configuration.
     */
    fun getPresets(mode: WorkoutMode): Flow<List<WorkoutPreset>>

    /** Adds [preset], replacing any existing preset with the same id. */
    suspend fun savePreset(preset: WorkoutPreset)

    /** Removes the preset with [id]. A missing id is not an error. */
    suspend fun deletePreset(id: String)
}
