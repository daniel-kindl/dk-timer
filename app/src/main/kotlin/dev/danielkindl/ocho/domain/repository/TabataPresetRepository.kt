package dev.danielkindl.ocho.domain.repository

import dev.danielkindl.ocho.domain.model.TabataPreset
import kotlinx.coroutines.flow.Flow

interface TabataPresetRepository {
    fun getPresets(): Flow<List<TabataPreset>>
    suspend fun savePreset(preset: TabataPreset)
    suspend fun deletePreset(id: String)
}
