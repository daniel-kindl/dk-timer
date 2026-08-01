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
    suspend fun trigger(isCompletion: Boolean, playSound: () -> Unit) {
        val settings = settingsRepository.getSettings().first()
        if (settings.soundEnabled) playSound()
        if (settings.vibrationEnabled) {
            if (isCompletion) vibrationManager.vibrateCompletion() else vibrationManager.vibrateInterval()
        }
    }
}
