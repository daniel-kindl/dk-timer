package dev.danielkindl.ocho.data.update

import dev.danielkindl.ocho.domain.model.AppUpdate
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import javax.inject.Inject
import javax.inject.Singleton

/**
 * Holds the result of the startup update check until the settings screen is opened.
 *
 * The check runs once at launch, long before anything can display it. Without this
 * the result would be discarded and the user would have to press "Check for
 * updates" to learn what the app already knew.
 *
 * Process-lifetime only; nothing is persisted, so each launch re-checks.
 */
@Singleton
class UpdateCheckCache @Inject constructor() {
    private val _latestUpdate = MutableStateFlow<AppUpdate?>(null)

    /** The most recent check's result, or null if none has succeeded this launch. */
    val latestUpdate: StateFlow<AppUpdate?> = _latestUpdate.asStateFlow()

    /** Records a check result; null clears it. */
    fun set(update: AppUpdate?) {
        _latestUpdate.value = update
    }
}
