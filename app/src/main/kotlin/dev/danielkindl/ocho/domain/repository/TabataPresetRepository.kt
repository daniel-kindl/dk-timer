package dev.danielkindl.ocho.domain.repository

import dev.danielkindl.ocho.domain.model.TabataPreset
import kotlinx.coroutines.flow.Flow

/**
 * Stores saved Tabata configurations, kept separate from [PresetRepository] so
 * neither setup screen can offer the other mode's presets.
 */
interface TabataPresetRepository {

    /** Emits the full preset list, and again on every save or delete. */
    fun getPresets(): Flow<List<TabataPreset>>

    /** Adds [preset], replacing any existing preset with the same id. */
    suspend fun savePreset(preset: TabataPreset)

    /** Removes the preset with [id]. A missing id is not an error. */
    suspend fun deletePreset(id: String)
}
