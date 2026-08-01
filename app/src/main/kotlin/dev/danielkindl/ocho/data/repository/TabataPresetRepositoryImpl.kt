package dev.danielkindl.ocho.data.repository

import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import dev.danielkindl.ocho.domain.model.TabataPreset
import dev.danielkindl.ocho.domain.repository.TabataPresetRepository
import kotlinx.coroutines.flow.Flow
import javax.inject.Inject

/** DataStore-backed [TabataPresetRepository]; persistence details live in [JsonListDataStore]. */
class TabataPresetRepositoryImpl @Inject constructor(
    dataStore: DataStore<Preferences>,
) : TabataPresetRepository {

    private val store = JsonListDataStore(
        dataStore = dataStore,
        key = "tabata_presets",
        parseItem = { obj ->
            TabataPreset(
                id = obj.getString("id"),
                name = obj.getString("name"),
                totalMinutes = obj.getInt("totalMinutes"),
                totalSeconds = obj.getInt("totalSeconds"),
                workMinutes = obj.getInt("workMinutes"),
                workSeconds = obj.getInt("workSeconds"),
                restMinutes = obj.getInt("restMinutes"),
                restSeconds = obj.getInt("restSeconds"),
            )
        },
        serializeItem = { preset, obj ->
            obj.put("id", preset.id)
            obj.put("name", preset.name)
            obj.put("totalMinutes", preset.totalMinutes)
            obj.put("totalSeconds", preset.totalSeconds)
            obj.put("workMinutes", preset.workMinutes)
            obj.put("workSeconds", preset.workSeconds)
            obj.put("restMinutes", preset.restMinutes)
            obj.put("restSeconds", preset.restSeconds)
        },
        idOf = { it.id },
    )

    override fun getPresets(): Flow<List<TabataPreset>> = store.observe()

    override suspend fun savePreset(preset: TabataPreset) = store.upsert(preset)

    override suspend fun deletePreset(id: String) = store.delete(id)
}
