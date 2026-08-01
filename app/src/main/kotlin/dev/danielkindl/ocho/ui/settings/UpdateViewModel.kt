package dev.danielkindl.ocho.ui.settings

import android.content.Context
import android.content.Intent
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import dev.danielkindl.ocho.data.update.ApkInstaller
import dev.danielkindl.ocho.data.update.DownloadStatus
import dev.danielkindl.ocho.data.update.UpdateCheckCache
import dev.danielkindl.ocho.data.update.UpdateDownloader
import dev.danielkindl.ocho.domain.model.AppUpdate
import dev.danielkindl.ocho.domain.model.SemVer
import dev.danielkindl.ocho.domain.repository.UpdateRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import java.io.File
import javax.inject.Inject

sealed interface UpdateUiState {
    data object Idle : UpdateUiState
    data object Checking : UpdateUiState
    data object UpToDate : UpdateUiState
    data class Available(val update: AppUpdate) : UpdateUiState
    data class Downloading(val update: AppUpdate, val progressPercent: Int) : UpdateUiState
    data class ReadyToInstall(val update: AppUpdate, val apkFile: File) : UpdateUiState
    data class Error(val message: String) : UpdateUiState
}

@HiltViewModel
class UpdateViewModel @Inject constructor(
    @ApplicationContext private val context: Context,
    private val updateRepository: UpdateRepository,
    private val updateDownloader: UpdateDownloader,
    private val apkInstaller: ApkInstaller,
    updateCheckCache: UpdateCheckCache,
) : ViewModel() {

    private val installedVersion: SemVer? =
        SemVer.parse(context.packageManager.getPackageInfo(context.packageName, 0).versionName.orEmpty())

    private val _uiState = MutableStateFlow<UpdateUiState>(
        updateCheckCache.latestUpdate.value?.let { UpdateUiState.Available(it) } ?: UpdateUiState.Idle
    )
    val uiState: StateFlow<UpdateUiState> = _uiState.asStateFlow()

    fun checkForUpdates() {
        _uiState.value = UpdateUiState.Checking
        viewModelScope.launch {
            _uiState.value = updateRepository.fetchLatestRelease().fold(
                onSuccess = { update -> toUiState(update) },
                onFailure = { UpdateUiState.Error(it.message ?: "Update check failed") },
            )
        }
    }

    private fun toUiState(update: AppUpdate): UpdateUiState =
        if (installedVersion != null && update.version > installedVersion) {
            UpdateUiState.Available(update)
        } else {
            UpdateUiState.UpToDate
        }

    fun startDownload() {
        val update = (_uiState.value as? UpdateUiState.Available)?.update ?: return
        _uiState.value = UpdateUiState.Downloading(update, 0)
        viewModelScope.launch {
            val downloadId = updateDownloader.enqueue(update)
            pollDownload(update, downloadId)
        }
    }

    private suspend fun pollDownload(update: AppUpdate, downloadId: Long) {
        while (true) {
            when (val status = updateDownloader.queryStatus(downloadId)) {
                is DownloadStatus.InProgress -> {
                    _uiState.value = UpdateUiState.Downloading(update, status.percent)
                    delay(POLL_INTERVAL_MS)
                }
                is DownloadStatus.Successful -> {
                    _uiState.value = UpdateUiState.ReadyToInstall(update, status.file)
                    return
                }
                is DownloadStatus.Failed -> {
                    _uiState.value = UpdateUiState.Error(status.reason)
                    return
                }
            }
        }
    }

    fun startInstall() {
        val state = _uiState.value as? UpdateUiState.ReadyToInstall ?: return
        apkInstaller.install(state.apkFile)
    }

    fun canInstallPackages(): Boolean = apkInstaller.canInstallPackages()

    fun unknownSourcesSettingsIntent(): Intent = apkInstaller.unknownSourcesSettingsIntent()

    private companion object {
        const val POLL_INTERVAL_MS = 500L
    }
}
