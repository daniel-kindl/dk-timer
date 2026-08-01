package dev.danielkindl.ocho.domain.model

import org.junit.Assert.assertEquals
import org.junit.Test

/**
 * Pins what the setup layer hands to the engine layer, so a refactor of the layers
 * above cannot silently change it.
 *
 * [SessionRequest] sits exactly on the boundary between the code being restructured
 * (setup screens, view models, presets) and the code that is not (engines, session
 * controller, service). Because that boundary does not move, these assertions
 * survive the refactor: only the way a request is constructed changes, never the
 * values it should end up carrying.
 *
 * That is the whole point. Tests rewritten alongside the code they cover are part of
 * the change rather than a check on it, and would let a behaviour change through
 * with both sides agreeing. **Do not update the expected numbers below when the
 * setup layer is unified.** A number that has to change is the signal that something
 * broke.
 */
class SessionRequestCharacterizationTest {

    // Durations chosen so every field is distinguishable in a failure message:
    // no two of total, interval, work and rest share a value.
    private val totalMillis = 20 * 60 * 1_000L // 20min
    private val intervalMillis = 90 * 1_000L // 1min 30s
    private val workMillis = 45 * 1_000L
    private val restMillis = 15 * 1_000L

    @Test
    fun `an EMOM request carries total and interval unchanged`() {
        val request = SessionRequest.Emom(
            TimerConfig(intervalMillis = intervalMillis, totalDurationMillis = totalMillis)
        )

        assertEquals(1_200_000L, request.config.totalDurationMillis)
        assertEquals(90_000L, request.config.intervalMillis)
    }

    @Test
    fun `a Tabata request carries total, work and rest unchanged`() {
        val request = SessionRequest.Tabata(
            TabataConfig(
                workMillis = workMillis,
                restMillis = restMillis,
                totalDurationMillis = totalMillis,
            )
        )

        assertEquals(1_200_000L, request.config.totalDurationMillis)
        assertEquals(45_000L, request.config.workMillis)
        assertEquals(15_000L, request.config.restMillis)
    }

    @Test
    fun `picker minutes and seconds convert to the millisecond values the engines expect`() {
        // The conversion the setup layer performs. Pinned here rather than only in
        // DurationFormatTest because this is the value that reaches an engine.
        assertEquals(1_200_000L, minutesSecondsToMillis(20, 0))
        assertEquals(90_000L, minutesSecondsToMillis(1, 30))
        assertEquals(45_000L, minutesSecondsToMillis(0, 45))
        assertEquals(15_000L, minutesSecondsToMillis(0, 15))
        assertEquals(0L, minutesSecondsToMillis(0, 0))
    }

    @Test
    fun `TimerConfig rejects a zero interval`() {
        // The setup layer's isValid check exists to keep this from ever throwing.
        // Pinned so a unified validator cannot quietly become more permissive.
        assertThrows { TimerConfig(intervalMillis = 0, totalDurationMillis = totalMillis) }
    }

    @Test
    fun `TimerConfig rejects a zero total`() {
        assertThrows { TimerConfig(intervalMillis = intervalMillis, totalDurationMillis = 0) }
    }

    private fun assertThrows(block: () -> Unit) {
        val threw = runCatching(block).isFailure
        assertEquals("Expected the constructor to reject this input", true, threw)
    }
}
