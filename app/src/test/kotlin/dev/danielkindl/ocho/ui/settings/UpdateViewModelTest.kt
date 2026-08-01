package dev.danielkindl.ocho.ui.settings

import android.content.Context
import android.content.pm.PackageInfo
import android.content.pm.PackageManager
import dev.danielkindl.ocho.data.update.ApkInstaller
import dev.danielkindl.ocho.data.update.DownloadStatus
import dev.danielkindl.ocho.data.update.UpdateCheckCache
import dev.danielkindl.ocho.data.update.UpdateDownloader
import dev.danielkindl.ocho.domain.model.AppUpdate
import dev.danielkindl.ocho.domain.model.SemVer
import dev.danielkindl.ocho.domain.repository.UpdateRepository
import io.mockk.coEvery
import io.mockk.coVerify
import io.mockk.every
import io.mockk.mockk
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.test.UnconfinedTestDispatcher
import kotlinx.coroutines.test.advanceUntilIdle
import kotlinx.coroutines.test.resetMain
import kotlinx.coroutines.test.runTest
import kotlinx.coroutines.test.setMain
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import java.io.File

@OptIn(ExperimentalCoroutinesApi::class)
class UpdateViewModelTest {

    private val dispatcher = UnconfinedTestDispatcher()

    private val context = mockk<Context>()
    private val updateRepository = mockk<UpdateRepository>()
    private val updateDownloader = mockk<UpdateDownloader>()
    private val apkInstaller = mockk<ApkInstaller>()
    private val updateCheckCache = UpdateCheckCache()

    @Before
    fun setUp() {
        Dispatchers.setMain(dispatcher)

        val packageManager = mockk<PackageManager>()
        val packageInfo = mockk<PackageInfo>()
        packageInfo.versionName = "2.2.0"
        every { context.packageManager } returns packageManager
        every { context.packageName } returns "dev.danielkindl.ocho"
        every { packageManager.getPackageInfo("dev.danielkindl.ocho", 0) } returns packageInfo
    }

    @After
    fun tearDown() {
        Dispatchers.resetMain()
    }

    private fun viewModel() =
        UpdateViewModel(context, updateRepository, updateDownloader, apkInstaller, updateCheckCache)

    private fun update(version: String) = AppUpdate(
        version = checkNotNull(SemVer.parse(version)),
        tagName = "v$version",
        downloadUrl = "https://example.com/dk-timer-$version.apk",
        releaseNotes = "notes",
    )

    @Test
    fun `checkForUpdates surfaces Available when the release is newer`() = runTest(dispatcher) {
        coEvery { updateRepository.fetchLatestRelease() } returns Result.success(update("3.0.0"))
        val viewModel = viewModel()

        viewModel.checkForUpdates()

        assertEquals(UpdateUiState.Available(update("3.0.0")), viewModel.uiState.value)
    }

    @Test
    fun `checkForUpdates surfaces UpToDate when the release is not newer`() = runTest(dispatcher) {
        coEvery { updateRepository.fetchLatestRelease() } returns Result.success(update("2.2.0"))
        val viewModel = viewModel()

        viewModel.checkForUpdates()

        assertEquals(UpdateUiState.UpToDate, viewModel.uiState.value)
    }

    @Test
    fun `checkForUpdates surfaces Error on failure`() = runTest(dispatcher) {
        coEvery { updateRepository.fetchLatestRelease() } returns Result.failure(IllegalStateException("boom"))
        val viewModel = viewModel()

        viewModel.checkForUpdates()

        assertEquals(UpdateUiState.Error("boom"), viewModel.uiState.value)
    }

    @Test
    fun `startDownload polls until success and lands on ReadyToInstall`() = runTest(dispatcher) {
        coEvery { updateRepository.fetchLatestRelease() } returns Result.success(update("3.0.0"))
        val viewModel = viewModel()
        viewModel.checkForUpdates()

        every { updateDownloader.enqueue(any()) } returns 42L
        val apkFile = File("dk-timer-3.0.0.apk")
        every { updateDownloader.queryStatus(42L) } returnsMany listOf(
            DownloadStatus.InProgress(50),
            DownloadStatus.Successful(apkFile),
        )

        viewModel.startDownload()
        advanceUntilIdle() // flush the delay() between the InProgress and Successful polls

        val state = viewModel.uiState.value
        assertTrue("Expected ReadyToInstall but was $state", state is UpdateUiState.ReadyToInstall)
        assertEquals(apkFile, (state as UpdateUiState.ReadyToInstall).apkFile)
    }

    @Test
    fun `startDownload surfaces Error when the download fails`() = runTest(dispatcher) {
        coEvery { updateRepository.fetchLatestRelease() } returns Result.success(update("3.0.0"))
        val viewModel = viewModel()
        viewModel.checkForUpdates()

        every { updateDownloader.enqueue(any()) } returns 42L
        every { updateDownloader.queryStatus(42L) } returns DownloadStatus.Failed("network error")

        viewModel.startDownload()

        assertEquals(UpdateUiState.Error("network error"), viewModel.uiState.value)
    }

    @Test
    fun `startInstall delegates to ApkInstaller once ready`() = runTest(dispatcher) {
        coEvery { updateRepository.fetchLatestRelease() } returns Result.success(update("3.0.0"))
        val viewModel = viewModel()
        viewModel.checkForUpdates()

        val apkFile = File("dk-timer-3.0.0.apk")
        every { updateDownloader.enqueue(any()) } returns 42L
        every { updateDownloader.queryStatus(42L) } returns DownloadStatus.Successful(apkFile)
        every { apkInstaller.install(apkFile) } returns Unit
        viewModel.startDownload()

        viewModel.startInstall()

        coVerify(exactly = 1) { apkInstaller.install(apkFile) }
    }
}
