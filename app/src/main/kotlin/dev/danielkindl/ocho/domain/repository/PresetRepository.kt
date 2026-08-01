package dev.danielkindl.ocho.domain.repository

import dev.danielkindl.ocho.domain.model.Preset
import kotlinx.coroutines.flow.Flow

/** Stores saved EMOM configurations. */
interface PresetRepository {

    /** Emits the full preset list, and again on every save or delete. */
    fun getPresets(): Flow<List<Preset>>

    /** Adds [preset], replacing any existing preset with the same id. */
    suspend fun savePreset(preset: Preset)

    /** Removes the preset with [id]. A missing id is not an error. */
    suspend fun deletePreset(id: String)
}
