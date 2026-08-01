package dev.danielkindl.ocho.data.feedback

import dev.danielkindl.ocho.data.vibration.VibrationManager
import dev.danielkindl.ocho.domain.repository.SettingsRepository
import kotlinx.coroutines.flow.first
import javax.inject.Inject

/**
 * Plays a sound (via the caller-supplied [playSound]) and/or vibrates for an
 * interval/completion event, gated by the user's sound/vibration settings.
 */
class FeedbackTrigger @Inject constructor(
    private val settingsRepository: SettingsRepository,
    private val vibrationManager: VibrationManager,
) {
    /**
     * Fires feedback for one timer event, honouring the current settings.
     *
     * Settings are read per event rather than cached, so toggling sound mid-workout
     * takes effect on the very next beep.
     *
     * @param isCompletion true for the end of a workout, which gets the distinct
     *   double-pulse vibration instead of the single interval pulse.
     * @param playSound invoked only when sound is enabled; the caller picks which
     *   tone suits the event.
     */
    suspend fun trigger(isCompletion: Boolean, playSound: () -> Unit) {
        val settings = settingsRepository.getSettings().first()
        if (settings.soundEnabled) playSound()
        if (settings.vibrationEnabled) {
            if (isCompletion) vibrationManager.vibrateCompletion() else vibrationManager.vibrateInterval()
        }
    }

    /**
     * Fires one tick of the 3-2-1 lead-in.
     *
     * Sound only, never vibration. A lead-in fires three times before every boundary,
     * and buzzing that often would drown the single pulse that marks the boundary
     * itself, which is the one the user actually needs to feel.
     *
     * Gated by both the global sound toggle and the countdown toggle, so turning
     * sound off silences these too.
     */
    suspend fun triggerCountdown(playSound: () -> Unit) {
        val settings = settingsRepository.getSettings().first()
        if (settings.soundEnabled && settings.countdownBeepsEnabled) playSound()
    }
}
