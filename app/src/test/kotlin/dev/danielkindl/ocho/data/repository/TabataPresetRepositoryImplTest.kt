package dev.danielkindl.ocho.data.repository

import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.stringPreferencesKey
import dev.danielkindl.ocho.domain.model.TabataPreset
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class TabataPresetRepositoryImplTest {

    private fun preset(id: String, name: String = "test") = TabataPreset(
        id = id, name = name,
        totalMinutes = 20, totalSeconds = 0,
        workMinutes = 0, workSeconds = 45,
        restMinutes = 0, restSeconds = 15,
    )

    @Test
    fun `getPresets returns empty list when nothing has been saved`() = runTest {
        val repository = TabataPresetRepositoryImpl(FakeDataStore())
        assertTrue(repository.getPresets().first().isEmpty())
    }

    @Test
    fun `savePreset then getPresets round-trips a single preset`() = runTest {
        val repository = TabataPresetRepositoryImpl(FakeDataStore())
        val saved = preset("1", "My Preset")

        repository.savePreset(saved)

        assertEquals(listOf(saved), repository.getPresets().first())
    }

    @Test
    fun `savePreset with an existing id replaces rather than duplicates`() = runTest {
        val repository = TabataPresetRepositoryImpl(FakeDataStore())
        repository.savePreset(preset("1", "Original"))
        repository.savePreset(preset("1", "Updated"))

        val presets = repository.getPresets().first()

        assertEquals(1, presets.size)
        assertEquals("Updated", presets.first().name)
    }

    @Test
    fun `deletePreset removes only the matching id`() = runTest {
        val repository = TabataPresetRepositoryImpl(FakeDataStore())
        repository.savePreset(preset("1"))
        repository.savePreset(preset("2"))

        repository.deletePreset("1")

        assertEquals(listOf("2"), repository.getPresets().first().map { it.id })
    }

    @Test
    fun `multiple presets round-trip in save order`() = runTest {
        val repository = TabataPresetRepositoryImpl(FakeDataStore())
        repository.savePreset(preset("1"))
        repository.savePreset(preset("2"))
        repository.savePreset(preset("3"))

        assertEquals(listOf("1", "2", "3"), repository.getPresets().first().map { it.id })
    }

    @Test
    fun `corrupt stored JSON falls back to an empty list`() = runTest {
        val dataStore = FakeDataStore()
        dataStore.edit { it[stringPreferencesKey("tabata_presets")] = "not valid json" }
        val repository = TabataPresetRepositoryImpl(dataStore)

        assertTrue(repository.getPresets().first().isEmpty())
    }
}
