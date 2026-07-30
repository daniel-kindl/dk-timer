package com.emomtimer

import android.app.Application
import com.emomtimer.data.update.UpdateCheckCache
import com.emomtimer.domain.model.SemVer
import com.emomtimer.domain.repository.UpdateRepository
import dagger.hilt.android.HiltAndroidApp
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltAndroidApp
class EmomTimerApp : Application() {

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
