package com.emomtimer.data.audio

import android.media.AudioManager
import android.media.ToneGenerator
import javax.inject.Inject

/**
 * Plays workout sounds using [ToneGenerator] on [AudioManager.STREAM_ALARM]
 * so audio is never silenced by system silent/do-not-disturb mode.
 *
 * Audio cannot overlap: each call starts a tone that auto-stops after [durationMs].
 */
class ToneAudioPlayer @Inject constructor() : AudioPlayer {

    @Volatile
    private var toneGenerator: ToneGenerator? = createGenerator()

    override fun playIntervalBeep() {
        ensureGenerator()?.startTone(ToneGenerator.TONE_CDMA_ALERT_CALL_GUARD, BEEP_DURATION_MS)
    }

    override fun playCompletionSound() {
        ensureGenerator()?.startTone(ToneGenerator.TONE_CDMA_ALERT_NETWORK_LITE, COMPLETION_DURATION_MS)
    }

    override fun playWorkStartBeep() {
        ensureGenerator()?.startTone(ToneGenerator.TONE_CDMA_HIGH_SS, BEEP_DURATION_MS)
    }

    override fun playRestStartBeep() {
        ensureGenerator()?.startTone(ToneGenerator.TONE_CDMA_LOW_SS, BEEP_DURATION_MS)
    }

    @Synchronized
    override fun release() {
        toneGenerator?.release()
        toneGenerator = null
    }

    @Synchronized
    private fun ensureGenerator(): ToneGenerator? {
        if (toneGenerator == null) toneGenerator = createGenerator()
        return toneGenerator
    }

    private fun createGenerator(): ToneGenerator? = runCatching {
        ToneGenerator(AudioManager.STREAM_ALARM, ToneGenerator.MAX_VOLUME)
    }.getOrNull()

    private companion object {
        const val BEEP_DURATION_MS = 250
        const val COMPLETION_DURATION_MS = 600
    }
}
