package dev.danielkindl.ocho.data.session

import android.content.Context
import android.content.Intent
import androidx.core.content.ContextCompat
import dagger.hilt.android.qualifiers.ApplicationContext
import javax.inject.Inject
import javax.inject.Singleton

/**
 * Starts and stops [SessionService].
 *
 * The only class that knows a service exists, which is what keeps `Context` out of
 * [SessionController] and leaves it unit-testable.
 */
@Singleton
class AndroidSessionServiceLauncher @Inject constructor(
    @ApplicationContext private val context: Context,
) : SessionServiceLauncher {

    override fun start() {
        // startForegroundService, not startService: from Android 8 a background
        // start throws unless the service promotes itself to the foreground, which
        // SessionService does immediately in onStartCommand.
        ContextCompat.startForegroundService(context, Intent(context, SessionService::class.java))
    }

    override fun stop() {
        context.stopService(Intent(context, SessionService::class.java))
    }
}
