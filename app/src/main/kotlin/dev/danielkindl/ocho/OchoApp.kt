package dev.danielkindl.ocho

import android.app.Application
import dev.danielkindl.ocho.data.update.UpdateCheckCache
import dev.danielkindl.ocho.domain.model.SemVer
import dev.danielkindl.ocho.domain.repository.UpdateRepository
import dagger.hilt.android.HiltAndroidApp
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltAndroidApp
class OchoApp : Application() {

    @Inject
    lateinit var updateRepository: UpdateRepository

    @Inject
    lateinit var updateCheckCache: UpdateCheckCache

    private val appScope = CoroutineScope(SupervisorJob() + Dispatchers.IO)

    override fun onCreate() {
        super.onCreate()
        checkForUpdateOnStart()
    }

    private fun checkForUpdateOnStart() {
        appScope.launch {
            val installedVersionName = packageManager.getPackageInfo(packageName, 0).versionName ?: return@launch
            val installedVersion = SemVer.parse(installedVersionName) ?: return@launch
            updateRepository.fetchLatestRelease().getOrNull()
                ?.takeIf { it.version > installedVersion }
                ?.let(updateCheckCache::set)
        }
    }
}
