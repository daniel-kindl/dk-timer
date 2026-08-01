package dev.danielkindl.ocho.data.repository

import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.stringPreferencesKey
import dev.danielkindl.ocho.domain.model.WorkoutMode
import dev.danielkindl.ocho.domain.model.WorkoutPreset
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

/**
 * Ported from `PresetRepositoryImplTest` and `TabataPresetRepositoryImplTest`.
 *
 * The invariant that survived the merge of the two stores is "save, then read back
 * identical durations", so that is what these assert. The mode filter and the
 * round trip across all three modes are new behaviour and tested separately below.
 */
class WorkoutPresetRepositoryImplTest {

    private fun emomPreset(id: String, name: String = "test") = WorkoutPreset(
        id = id,
        name = name,
        mode = WorkoutMode.EMOM,
        totalMinutes = 20,
        totalSeconds = 0,
        intervalMinutes = 1,
        intervalSeconds = 0,
    )

    @Test
    fun `getPresets returns empty list when nothing has been saved`() = runTest {
        val repository = WorkoutPresetRepositoryImpl(FakeDataStore())
        assertTrue(repository.getPresets(WorkoutMode.EMOM).first().isEmpty())
    }

    @Test
    fun `savePreset then getPresets round-trips a single preset`() = runTest {
        val repository = WorkoutPresetRepositoryImpl(FakeDataStore())
        val saved = emomPreset("1", "My Preset")

        repository.savePreset(saved)

        assertEquals(listOf(saved), repository.getPresets(WorkoutMode.EMOM).first())
    }

    @Test
    fun `savePreset with an existing id replaces rather than duplicates`() = runTest {
        val repository = WorkoutPresetRepositoryImpl(FakeDataStore())
        repository.savePreset(emomPreset("1", "Original"))
        repository.savePreset(emomPreset("1", "Updated"))

        val presets = repository.getPresets(WorkoutMode.EMOM).first()

        assertEquals(1, presets.size)
        assertEquals("Updated", presets.first().name)
    }

    @Test
    fun `deletePreset removes only the matching id`() = runTest {
        val repository = WorkoutPresetRepositoryImpl(FakeDataStore())
        repository.savePreset(emomPreset("1"))
        repository.savePreset(emomPreset("2"))

        repository.deletePreset("1")

        assertEquals(listOf("2"), repository.getPresets(WorkoutMode.EMOM).first().map { it.id })
    }

    @Test
    fun `multiple presets round-trip in save order`() = runTest {
        val repository = WorkoutPresetRepositoryImpl(FakeDataStore())
        repository.savePreset(emomPreset("1"))
        repository.savePreset(emomPreset("2"))
        repository.savePreset(emomPreset("3"))

        assertEquals(
            listOf("1", "2", "3"),
            repository.getPresets(WorkoutMode.EMOM).first().map { it.id },
        )
    }

    @Test
    fun `corrupt stored JSON falls back to an empty list`() = runTest {
        val dataStore = FakeDataStore()
        dataStore.edit { it[stringPreferencesKey("workout_presets")] = "not valid json" }
        val repository = WorkoutPresetRepositoryImpl(dataStore)

        assertTrue(repository.getPresets(WorkoutMode.EMOM).first().isEmpty())
    }

    @Test
    fun `every mode round-trips its own duration fields`() = runTest {
        val repository = WorkoutPresetRepositoryImpl(FakeDataStore())
        val presets = listOf(
            WorkoutPreset(
                id = "emom", name = "EMOM", mode = WorkoutMode.EMOM,
                totalMinutes = 20, totalSeconds = 30,
                intervalMinutes = 1, intervalSeconds = 5,
            ),
            WorkoutPreset(
                id = "tabata", name = "Tabata", mode = WorkoutMode.TABATA,
                totalMinutes = 4, totalSeconds = 0,
                workMinutes = 0, workSeconds = 20,
                restMinutes = 0, restSeconds = 10,
            ),
            WorkoutPreset(
                id = "amrap", name = "AMRAP", mode = WorkoutMode.AMRAP,
                totalMinutes = 12, totalSeconds = 0,
            ),
        )

        presets.forEach { repository.savePreset(it) }

        presets.forEach { saved ->
            assertEquals(listOf(saved), repository.getPresets(saved.mode).first())
        }
    }

    @Test
    fun `getPresets never leaks another mode's configuration`() = runTest {
        // The reason filtering lives in the repository rather than the caller: a setup
        // screen must not be able to offer a preset it has no pickers for.
        val repository = WorkoutPresetRepositoryImpl(FakeDataStore())
        repository.savePreset(emomPreset("1"))

        assertTrue(repository.getPresets(WorkoutMode.TABATA).first().isEmpty())
        assertTrue(repository.getPresets(WorkoutMode.AMRAP).first().isEmpty())
    }

    @Test
    fun `a preset with an unrecognised mode is dropped without losing the others`() = runTest {
        // A future mode's preset read back by an older build. Dropping just that one
        // entry is why JsonListDataStore parses items individually.
        val dataStore = FakeDataStore()
        val repository = WorkoutPresetRepositoryImpl(dataStore)
        repository.savePreset(emomPreset("1"))

        val stored = dataStore.data.first()[stringPreferencesKey("workout_presets")]!!
        val unknown = """{"id":"2","name":"future","mode":"HYROX","totalMinutes":1,"totalSeconds":0}"""
        dataStore.edit {
            it[stringPreferencesKey("workout_presets")] = stored.dropLast(1) + ",$unknown]"
        }

        assertEquals(listOf("1"), repository.getPresets(WorkoutMode.EMOM).first().map { it.id })
    }
}
