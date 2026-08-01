package dev.danielkindl.ocho.data.session

import android.Manifest
import android.app.Service
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.os.Build
import android.os.IBinder
import android.os.PowerManager
import androidx.core.app.NotificationManagerCompat
import androidx.core.content.ContextCompat
import dagger.hilt.android.AndroidEntryPoint
import dev.danielkindl.ocho.domain.model.SessionSnapshot
import dev.danielkindl.ocho.domain.model.SessionStatus
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.cancel
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.launch
import javax.inject.Inject

/**
 * Keeps a workout running when the app is not on screen.
 *
 * Two distinct jobs, and the release needs both:
 *
 * 1. **Foreground status** stops Android freezing or killing the process, which
 *    would take the engine coroutine with it.
 * 2. **A partial wake lock** stops the CPU sleeping. This is the part people miss.
 *    The engines are built on `delay()`, which does not fire while the device
 *    dozes, so without the lock a locked screen means the clock silently falls
 *    behind and interval beeps go missing. Foreground status alone does not
 *    prevent that.
 *
 * The service owns neither the session nor its timing. [SessionController] does, and
 * this observes it.
 */
@AndroidEntryPoint
class SessionService : Service() {

    /** The running session. Injected as a field because Android constructs services. */
    @Inject
    lateinit var sessionController: SessionController

    /** Builds the ongoing notification this service posts. */
    @Inject
    lateinit var notifications: SessionNotifications

    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.Main.immediate)
    private var wakeLock: PowerManager.WakeLock? = null

    override fun onCreate() {
        super.onCreate()
        notifications.ensureChannel()
        acquireWakeLock()
        observeSession()
    }

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        // Must post the notification immediately. Android 14 kills a foreground
        // service that has not called startForeground within five seconds, and the
        // first snapshot may not have arrived yet.
        startForeground(SESSION_NOTIFICATION_ID, notifications.build(sessionController.snapshot.value))

        when (intent?.action) {
            ACTION_PAUSE -> sessionController.pause()
            ACTION_RESUME -> sessionController.resume()
            ACTION_STOP -> sessionController.stop()
        }

        // Not sticky: a restarted service with no session would show an empty
        // notification and hold a wake lock for nothing.
        return START_NOT_STICKY
    }

    /**
     * Mirrors session state into the notification, at most once a second.
     *
     * The engine ticks ten times a second. Rebuilding the notification that often
     * costs real battery for updates nobody can read, so this samples the fields
     * that actually change the rendering and ignores sub-second jitter.
     */
    private fun observeSession() {
        scope.launch {
            sessionController.snapshot
                .map { snapshot ->
                    snapshot?.copy(
                        remainingInPhaseMillis = snapshot.remainingInPhaseMillis / MILLIS_PER_SECOND,
                        elapsedMillis = 0,
                    )
                }
                .distinctUntilChanged()
                .collect { _ ->
                    val current = sessionController.snapshot.value
                    if (current != null && isFinished(current.status)) {
                        stopSelf()
                        return@collect
                    }
                    postNotification(current)
                }
        }
    }

    private fun isFinished(status: SessionStatus): Boolean =
        status == SessionStatus.Completed || status == SessionStatus.Stopped

    /**
     * Updates the ongoing notification, or does nothing if the user denied
     * notifications.
     *
     * Denial must not affect the workout. The service still runs, the wake lock is
     * still held and timing is still exact; the user simply does not see it. Letting
     * a `SecurityException` escape here would kill the session over a cosmetic
     * permission.
     */
    private fun postNotification(snapshot: SessionSnapshot?) {
        // The permission check is inlined rather than extracted to a helper because
        // lint's analysis does not cross method boundaries and would flag the notify
        // call as unguarded. Below Android 13 the permission is granted at install
        // time, so the condition is a no-op there.
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU &&
            ContextCompat.checkSelfPermission(this, Manifest.permission.POST_NOTIFICATIONS) !=
            PackageManager.PERMISSION_GRANTED
        ) {
            return
        }
        NotificationManagerCompat.from(this)
            .notify(SESSION_NOTIFICATION_ID, notifications.build(snapshot))
    }

    /**
     * Holds the CPU awake for the session.
     *
     * The timeout is a safety net, not the intended lifetime: if this service were
     * ever killed without [onDestroy] running, an untimed lock would drain the
     * battery until reboot. Four hours comfortably exceeds any real workout.
     */
    private fun acquireWakeLock() {
        val powerManager = getSystemService(Context.POWER_SERVICE) as PowerManager
        wakeLock = powerManager.newWakeLock(PowerManager.PARTIAL_WAKE_LOCK, WAKE_LOCK_TAG).apply {
            setReferenceCounted(false)
            acquire(WAKE_LOCK_TIMEOUT_MS)
        }
    }

    override fun onDestroy() {
        wakeLock?.let { if (it.isHeld) it.release() }
        wakeLock = null
        scope.cancel()
        super.onDestroy()
    }

    /** Not a bound service; everything goes through [SessionController]. */
    override fun onBind(intent: Intent?): IBinder? = null

    companion object {
        /** Notification action: freeze the running session. */
        const val ACTION_PAUSE = "dev.danielkindl.ocho.action.PAUSE"

        /** Notification action: resume a paused session. */
        const val ACTION_RESUME = "dev.danielkindl.ocho.action.RESUME"

        /** Notification action: end the session early. */
        const val ACTION_STOP = "dev.danielkindl.ocho.action.STOP"

        private const val WAKE_LOCK_TAG = "ocho:session"
        private const val MILLIS_PER_SECOND = 1_000L
        private const val SECONDS_PER_HOUR = 3_600L

        /** Comfortably longer than any real workout; see [acquireWakeLock]. */
        private const val WAKE_LOCK_TIMEOUT_HOURS = 4L
        private const val WAKE_LOCK_TIMEOUT_MS =
            WAKE_LOCK_TIMEOUT_HOURS * SECONDS_PER_HOUR * MILLIS_PER_SECOND
    }
}
