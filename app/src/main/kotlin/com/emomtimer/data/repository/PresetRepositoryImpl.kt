package com.emomtimer.data.repository

import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import com.emomtimer.domain.model.Preset
import com.emomtimer.domain.repository.PresetRepository
import kotlinx.coroutines.flow.Flow
import javax.inject.Inject

class PresetRepositoryImpl @Inject constructor(
    dataStore: DataStore<Preferences>,
) : PresetRepository {

    private val store = JsonListDataStore(
        dataStore = dataStore,
        key = "presets",
        parseItem = { obj ->
            Preset(
                id = obj.getString("id"),
                name = obj.getString("name"),
                totalMinutes = obj.getInt("totalMinutes"),
                totalSeconds = obj.getInt("totalSeconds"),
                intervalMinutes = obj.getInt("intervalMinutes"),
                intervalSeconds = obj.getInt("intervalSeconds"),
            )
        },
        serializeItem = { preset, obj ->
            obj.put("id", preset.id)
            obj.put("name", preset.name)
            obj.put("totalMinutes", preset.totalMinutes)
            obj.put("totalSeconds", preset.totalSeconds)
            obj.put("intervalMinutes", preset.intervalMinutes)
            obj.put("intervalSeconds", preset.intervalSeconds)
        },
        idOf = { it.id },
    )

    override fun getPresets(): Flow<List<Preset>> = store.observe()

    override suspend fun savePreset(preset: Preset) = store.upsert(preset)

    override suspend fun deletePreset(id: String) = store.delete(id)
}
