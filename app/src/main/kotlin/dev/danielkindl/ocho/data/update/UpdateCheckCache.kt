package dev.danielkindl.ocho.data.update

import dev.danielkindl.ocho.domain.model.AppUpdate
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class UpdateCheckCache @Inject constructor() {
    private val _latestUpdate = MutableStateFlow<AppUpdate?>(null)
    val latestUpdate: StateFlow<AppUpdate?> = _latestUpdate.asStateFlow()

    fun set(update: AppUpdate?) {
        _latestUpdate.value = update
    }
}
