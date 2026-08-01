package dev.danielkindl.ocho.ui.settings

import android.content.Intent
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import dev.danielkindl.ocho.data.update.ApkInstaller
import dev.danielkindl.ocho.data.update.DownloadStatus
import dev.danielkindl.ocho.data.update.UpdateCheckCache
import dev.danielkindl.ocho.data.update.UpdateDownloader
import dev.danielkindl.ocho.domain.model.AppUpdate
import dev.danielkindl.ocho.domain.model.SemVer
import dev.danielkindl.ocho.domain.model.UpdateConfig
import dev.danielkindl.ocho.domain.repository.UpdateRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import java.io.File
import javax.inject.Inject

/**
 * Steps of the in-app update flow, in the order they normally occur:
 * check, offer, download, install.
 *
 * Modelled as a sealed hierarchy rather than a flag-covered data class so states
 * that cannot coexist — downloading and up-to-date, say — are unrepresentable.
 */
sealed interface UpdateUiState {

    /** No check has run yet this session. */
    data object Idle : UpdateUiState

    /** A check is in flight. */
    data object Checking : UpdateUiState

    /** The newest release on this channel is not newer than what is installed. */
    data object UpToDate : UpdateUiState

    /** A newer release exists. @property update the release on offer. */
    data class Available(val update: AppUpdate) : UpdateUiState

    /**
     * The APK is downloading.
     * @property update the release being fetched.
     * @property progressPercent completion from 0 to 100.
     */
    data class Downloading(val update: AppUpdate, val progressPercent: Int) : UpdateUiState

    /**
     * The APK is on disk and ready to install.
     * @property update the release that was fetched.
     * @property apkFile the downloaded file.
     */
    data class ReadyToInstall(val update: AppUpdate, val apkFile: File) : UpdateUiState

    /** A step failed. @property message shown to the user verbatim. */
    data class Error(val message: String) : UpdateUiState
}

/**
 * Drives the in-app update flow in Settings: check, download, install.
 *
 * Seeds itself from [UpdateCheckCache] so a result found during the launch-time
 * check is already on screen when Settings opens, rather than requiring a
 * redundant second check.
 */
@HiltViewModel
class UpdateViewModel @Inject constructor(
    updateConfig: UpdateConfig,
    private val updateRepository: UpdateRepository,
    private val updateDownloader: UpdateDownloader,
    private val apkInstaller: ApkInstaller,
    updateCheckCache: UpdateCheckCache,
) : ViewModel() {

    private val installedVersion: SemVer? = updateConfig.installedVersion

    private val _uiState = MutableStateFlow<UpdateUiState>(
        updateCheckCache.latestUpdate.value?.let { UpdateUiState.Available(it) } ?: UpdateUiState.Idle
    )
    /** Current step of the update flow. */
    val uiState: StateFlow<UpdateUiState> = _uiState.asStateFlow()

    /**
     * Queries this build's channel for a newer release.
     *
     * Reports [UpdateUiState.UpToDate] when the installed version is unknown —
     * offering an update we cannot compare against risks a downgrade.
     */
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

    /** Downloads the offered APK. No-op unless an update is currently on offer. */
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

    /**
     * Installs the downloaded APK. Check [canInstallPackages] first — without that
     * permission the install is refused with no visible explanation.
     */
    fun startInstall() {
        val state = _uiState.value as? UpdateUiState.ReadyToInstall ?: return
        apkInstaller.install(state.apkFile)
    }

    /** Whether the user has granted permission to install packages. */
    fun canInstallPackages(): Boolean = apkInstaller.canInstallPackages()

    /** Settings screen where that permission is granted; launch when [canInstallPackages] is false. */
    fun unknownSourcesSettingsIntent(): Intent = apkInstaller.unknownSourcesSettingsIntent()

    private companion object {
        const val POLL_INTERVAL_MS = 500L
    }
}
