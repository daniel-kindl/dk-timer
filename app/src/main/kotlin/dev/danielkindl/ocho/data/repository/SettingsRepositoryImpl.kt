package dev.danielkindl.ocho.data.repository

import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.booleanPreferencesKey
import androidx.datastore.preferences.core.edit
import dev.danielkindl.ocho.domain.model.UserSettings
import dev.danielkindl.ocho.domain.repository.SettingsRepository
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import javax.inject.Inject

/**
 * DataStore-backed [SettingsRepository].
 *
 * A missing key reads as the [UserSettings] default rather than an error, so a
 * first launch behaves like one where both toggles were deliberately left on.
 */
class SettingsRepositoryImpl @Inject constructor(
    private val dataStore: DataStore<Preferences>,
) : SettingsRepository {

    override fun getSettings(): Flow<UserSettings> =
        dataStore.data.map { prefs ->
            UserSettings(
                soundEnabled = prefs[KEY_SOUND] ?: true,
                vibrationEnabled = prefs[KEY_VIBRATION] ?: true,
            )
        }

    override suspend fun setSoundEnabled(enabled: Boolean) {
        dataStore.edit { it[KEY_SOUND] = enabled }
    }

    override suspend fun setVibrationEnabled(enabled: Boolean) {
        dataStore.edit { it[KEY_VIBRATION] = enabled }
    }

    private companion object {
        val KEY_SOUND = booleanPreferencesKey("sound_enabled")
        val KEY_VIBRATION = booleanPreferencesKey("vibration_enabled")
    }
}
