package dev.danielkindl.ocho.domain.model

/**
 * User preferences for workout feedback.
 *
 * Both default to enabled: a timer whose feedback is silent by default would look
 * broken, and this is the state a first launch sees before anything is persisted.
 *
 * @property soundEnabled whether to beep at timer events. Beeps use the alarm
 *   stream, so they play through silent mode.
 * @property vibrationEnabled whether to vibrate at timer events and on completion.
 * @property countdownBeepsEnabled whether to tick down the last three seconds before
 *   each boundary. Separate from [soundEnabled] because opinion divides sharply:
 *   some people need the warning, others find three extra beeps a minute maddening.
 *   Subordinate to [soundEnabled], which silences everything.
 */
data class UserSettings(
    val soundEnabled: Boolean = true,
    val vibrationEnabled: Boolean = true,
    val countdownBeepsEnabled: Boolean = true,
)
