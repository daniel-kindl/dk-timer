package dev.danielkindl.ocho.domain.repository

import dev.danielkindl.ocho.domain.model.UserSettings
import kotlinx.coroutines.flow.Flow

/** Persists the sound and vibration preferences. */
interface SettingsRepository {

    /** Emits current settings, and again whenever either toggle changes. */
    fun getSettings(): Flow<UserSettings>

    /** Persists the sound toggle; takes effect on the next timer event. */
    suspend fun setSoundEnabled(enabled: Boolean)

    /** Persists the vibration toggle; takes effect on the next timer event. */
    suspend fun setVibrationEnabled(enabled: Boolean)
}
