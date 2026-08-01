package dev.danielkindl.ocho

import android.app.Application
import dev.danielkindl.ocho.data.update.UpdateCheckCache
import dev.danielkindl.ocho.domain.model.UpdateConfig
import dev.danielkindl.ocho.domain.repository.UpdateRepository
import dagger.hilt.android.HiltAndroidApp
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.launch
import javax.inject.Inject

/**
 * Application entry point and Hilt root.
 *
 * Also fires the one update check per launch. Doing it here rather than from the
 * settings screen means the answer is already waiting by the time the user goes
 * looking for it.
 */
@HiltAndroidApp
class OchoApp : Application() {

    /** Injected field rather than a constructor parameter: Android owns this instance. */
    @Inject
    lateinit var updateRepository: UpdateRepository

    /** Carries the startup check's result through to the settings screen. */
    @Inject
    lateinit var updateCheckCache: UpdateCheckCache

    /** This build's update channel and its own version. */
    @Inject
    lateinit var updateConfig: UpdateConfig

    private val appScope = CoroutineScope(SupervisorJob() + Dispatchers.IO)

    override fun onCreate() {
        super.onCreate()
        checkForUpdateOnStart()
    }

    /**
     * Looks for a newer release in this build's channel, caching it if one exists.
     *
     * Every failure path is silent by design — no network, a rate-limited API, an
     * unparseable version — because a launch-time update check is not something the
     * user asked for and must never interrupt starting a workout.
     */
    private fun checkForUpdateOnStart() {
        val installedVersion = updateConfig.installedVersion ?: return
        appScope.launch {
            updateRepository.fetchLatestRelease().getOrNull()
                ?.takeIf { it.version > installedVersion }
                ?.let(updateCheckCache::set)
        }
    }
}
