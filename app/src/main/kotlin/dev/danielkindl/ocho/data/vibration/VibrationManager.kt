package dev.danielkindl.ocho.data.vibration

import android.content.Context
import android.os.Build
import android.os.VibrationEffect
import android.os.Vibrator
import android.os.VibratorManager
import dagger.hilt.android.qualifiers.ApplicationContext
import javax.inject.Inject

class VibrationManager @Inject constructor(
    @ApplicationContext private val context: Context,
) {
    private val vibrator: Vibrator = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
        val manager = context.getSystemService(Context.VIBRATOR_MANAGER_SERVICE) as VibratorManager
        manager.defaultVibrator
    } else {
        @Suppress("DEPRECATION")
        context.getSystemService(Context.VIBRATOR_SERVICE) as Vibrator
    }

    /** Single short pulse marking an interval or phase boundary. */
    fun vibrateInterval() {
        vibrator.vibrate(
            VibrationEffect.createOneShot(INTERVAL_DURATION_MS, VibrationEffect.DEFAULT_AMPLITUDE)
        )
    }

    /** Double pulse marking the end of a workout, so it is distinguishable from an interval. */
    fun vibrateCompletion() {
        val pattern = longArrayOf(0, COMPLETION_PULSE_MS, COMPLETION_PAUSE_MS, COMPLETION_PULSE_MS)
        vibrator.vibrate(VibrationEffect.createWaveform(pattern, NO_REPEAT))
    }

    private companion object {
        const val INTERVAL_DURATION_MS = 300L
        const val COMPLETION_PULSE_MS = 300L
        const val COMPLETION_PAUSE_MS = 150L

        /** `repeat` index meaning "play the waveform once and stop". */
        const val NO_REPEAT = -1
    }
}
