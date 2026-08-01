package dev.danielkindl.ocho.domain.repository

import dev.danielkindl.ocho.domain.model.Preset
import kotlinx.coroutines.flow.Flow

interface PresetRepository {
    fun getPresets(): Flow<List<Preset>>
    suspend fun savePreset(preset: Preset)
    suspend fun deletePreset(id: String)
}
