package dev.danielkindl.ocho.data.audio

import android.content.Context
import android.media.AudioAttributes
import android.media.AudioFocusRequest
import android.media.AudioManager
import android.media.ToneGenerator
import dagger.hilt.android.qualifiers.ApplicationContext
import javax.inject.Inject

/**
 * Plays workout sounds using [ToneGenerator] on [AudioManager.STREAM_ALARM]
 * so audio is never silenced by system silent or do-not-disturb mode.
 *
 * Audio cannot overlap: each call starts a tone that auto-stops after its duration.
 */
class ToneAudioPlayer @Inject constructor(
    @ApplicationContext private val context: Context,
) : AudioPlayer {

    @Volatile
    private var toneGenerator: ToneGenerator? = createGenerator()

    private val audioManager: AudioManager? =
        context.getSystemService(Context.AUDIO_SERVICE) as? AudioManager

    /**
     * Held between [requestAudioFocus] and [abandonAudioFocus].
     *
     * Kept as a field because the system requires the *same* request object to
     * abandon focus that was used to gain it; a freshly built equivalent is not
     * accepted and focus would leak, leaving other apps permanently ducked.
     */
    private var focusRequest: AudioFocusRequest? = null

    override fun playIntervalBeep() {
        ensureGenerator()?.startTone(ToneGenerator.TONE_CDMA_ALERT_CALL_GUARD, BEEP_DURATION_MS)
    }

    override fun playCountdownBeep() {
        ensureGenerator()?.startTone(ToneGenerator.TONE_PROP_BEEP, COUNTDOWN_DURATION_MS)
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
    override fun requestAudioFocus() {
        if (focusRequest != null) return

        val attributes = AudioAttributes.Builder()
            // Sonification rather than media: these are functional cues, and the
            // usage is what tells the system to duck music rather than pause it.
            .setUsage(AudioAttributes.USAGE_ASSISTANCE_SONIFICATION)
            .setContentType(AudioAttributes.CONTENT_TYPE_SONIFICATION)
            .build()

        val request = AudioFocusRequest.Builder(AudioManager.AUDIOFOCUS_GAIN_TRANSIENT_MAY_DUCK)
            .setAudioAttributes(attributes)
            // The app deliberately ignores focus loss. A workout timer that went
            // silent because a notification chimed would be worse than useless, and
            // the cues are short enough not to obstruct whatever took focus.
            .setWillPauseWhenDucked(false)
            .setOnAudioFocusChangeListener { }
            .build()

        audioManager?.requestAudioFocus(request)
        focusRequest = request
    }

    @Synchronized
    override fun abandonAudioFocus() {
        focusRequest?.let { audioManager?.abandonAudioFocusRequest(it) }
        focusRequest = null
    }

    @Synchronized
    override fun release() {
        abandonAudioFocus()
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

        /** Deliberately shorter than a boundary beep, so the two are never confused. */
        const val COUNTDOWN_DURATION_MS = 90
    }
}
