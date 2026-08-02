package dev.danielkindl.ocho.data.repository

import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import dev.danielkindl.ocho.domain.model.WorkoutMode
import dev.danielkindl.ocho.domain.model.WorkoutPreset
import dev.danielkindl.ocho.domain.repository.WorkoutPresetRepository
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import javax.inject.Inject

/**
 * DataStore-backed [WorkoutPresetRepository]; persistence details live in
 * [JsonListDataStore], which needed no changes to serve every mode.
 *
 * A preset whose stored mode is unrecognised is dropped rather than defaulted. A
 * default would put it on the wrong setup screen, which is more confusing than it
 * simply not being there.
 */
class WorkoutPresetRepositoryImpl @Inject constructor(
    dataStore: DataStore<Preferences>,
) : WorkoutPresetRepository {

    private val store = JsonListDataStore(
        dataStore = dataStore,
        key = "workout_presets",
        parseItem = { obj ->
            WorkoutPreset(
                id = obj.getString("id"),
                name = obj.getString("name"),
                mode = WorkoutMode.valueOf(obj.getString("mode")),
                totalMinutes = obj.getInt("totalMinutes"),
                totalSeconds = obj.getInt("totalSeconds"),
                intervalMinutes = obj.optInt("intervalMinutes"),
                intervalSeconds = obj.optInt("intervalSeconds"),
                workMinutes = obj.optInt("workMinutes"),
                workSeconds = obj.optInt("workSeconds"),
                restMinutes = obj.optInt("restMinutes"),
                restSeconds = obj.optInt("restSeconds"),
            )
        },
        serializeItem = { preset, obj ->
            obj.put("id", preset.id)
            obj.put("name", preset.name)
            obj.put("mode", preset.mode.name)
            obj.put("totalMinutes", preset.totalMinutes)
            obj.put("totalSeconds", preset.totalSeconds)
            obj.put("intervalMinutes", preset.intervalMinutes)
            obj.put("intervalSeconds", preset.intervalSeconds)
            obj.put("workMinutes", preset.workMinutes)
            obj.put("workSeconds", preset.workSeconds)
            obj.put("restMinutes", preset.restMinutes)
            obj.put("restSeconds", preset.restSeconds)
        },
        idOf = { it.id },
    )

    override fun getPresets(mode: WorkoutMode): Flow<List<WorkoutPreset>> =
        store.observe().map { presets -> presets.filter { it.mode == mode } }

    override suspend fun savePreset(preset: WorkoutPreset) = store.upsert(preset)

    override suspend fun deletePreset(id: String) = store.delete(id)
}
