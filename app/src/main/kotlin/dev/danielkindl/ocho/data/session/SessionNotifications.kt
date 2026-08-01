package dev.danielkindl.ocho.data.session

import android.app.Notification
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import androidx.core.app.NotificationCompat
import dagger.hilt.android.qualifiers.ApplicationContext
import dev.danielkindl.ocho.MainActivity
import dev.danielkindl.ocho.R
import dev.danielkindl.ocho.core.format.formatCountdown
import dev.danielkindl.ocho.domain.model.Phase
import dev.danielkindl.ocho.domain.model.SessionSnapshot
import dev.danielkindl.ocho.domain.model.SessionStatus
import javax.inject.Inject
import javax.inject.Singleton

/** Notification channel carrying the ongoing session. */
const val SESSION_CHANNEL_ID = "session"

/** Notification id for the ongoing session. Constant: only one session runs at a time. */
const val SESSION_NOTIFICATION_ID = 1

/**
 * Builds the ongoing session notification.
 *
 * This is the workout's face when the app is not on screen, so it has to answer the
 * same question the session screen does: what am I doing, and for how much longer.
 * It carries transport controls too, because reaching them should not require
 * unlocking and reopening the app mid-set.
 */
@Singleton
class SessionNotifications @Inject constructor(
    @ApplicationContext private val context: Context,
) {

    private val manager =
        context.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager

    /**
     * Registers the channel. Safe to call repeatedly.
     *
     * Importance is LOW deliberately: the app already owns audio and haptics, and a
     * channel that made its own sound would double every cue.
     */
    fun ensureChannel() {
        val channel = NotificationChannel(
            SESSION_CHANNEL_ID,
            "Workout session",
            NotificationManager.IMPORTANCE_LOW,
        ).apply {
            description = "Shows the running workout and its controls"
            setSound(null, null)
            enableVibration(false)
        }
        manager.createNotificationChannel(channel)
    }

    /** Builds the notification for [snapshot], or a neutral placeholder if none is running. */
    fun build(snapshot: SessionSnapshot?): Notification {
        val builder = NotificationCompat.Builder(context, SESSION_CHANNEL_ID)
            .setSmallIcon(R.drawable.ic_notification)
            .setContentIntent(openAppIntent())
            .setOngoing(true)
            .setSilent(true)
            .setCategory(NotificationCompat.CATEGORY_STOPWATCH)
            // Visible on the lock screen: mid-workout the phone is usually locked,
            // and that is exactly when the remaining time is worth reading.
            .setVisibility(NotificationCompat.VISIBILITY_PUBLIC)
            .setShowWhen(false)

        if (snapshot == null) {
            return builder.setContentTitle("Ocho").build()
        }

        val paused = snapshot.status == SessionStatus.Paused
        builder.setContentTitle(title(snapshot, paused))
        builder.setContentText(snapshot.remainingInPhaseMillis.formatCountdown())

        if (snapshot.isActive) {
            builder.addAction(
                if (paused) R.drawable.ic_play else R.drawable.ic_pause,
                if (paused) "Resume" else "Pause",
                servicePendingIntent(
                    if (paused) SessionService.ACTION_RESUME else SessionService.ACTION_PAUSE
                ),
            )
            builder.addAction(
                R.drawable.ic_square,
                "Stop",
                servicePendingIntent(SessionService.ACTION_STOP),
            )
        }

        return builder.build()
    }

    private fun title(snapshot: SessionSnapshot, paused: Boolean): String {
        val phase = when (snapshot.phase) {
            Phase.PREPARE -> "Prepare"
            Phase.WORK -> "Work"
            Phase.REST -> "Rest"
            Phase.COMPLETE -> "Complete"
        }
        val rounds = if (snapshot.totalRounds > 0) {
            " · round ${snapshot.currentRound}/${snapshot.totalRounds}"
        } else {
            ""
        }
        return if (paused) "$phase · paused$rounds" else "$phase$rounds"
    }

    private fun openAppIntent(): PendingIntent {
        val intent = Intent(context, MainActivity::class.java)
            .setFlags(Intent.FLAG_ACTIVITY_SINGLE_TOP or Intent.FLAG_ACTIVITY_CLEAR_TOP)
        return PendingIntent.getActivity(context, 0, intent, IMMUTABLE_UPDATE)
    }

    private fun servicePendingIntent(action: String): PendingIntent {
        val intent = Intent(context, SessionService::class.java).setAction(action)
        return PendingIntent.getService(context, action.hashCode(), intent, IMMUTABLE_UPDATE)
    }

    private companion object {
        const val IMMUTABLE_UPDATE =
            PendingIntent.FLAG_IMMUTABLE or PendingIntent.FLAG_UPDATE_CURRENT
    }
}
