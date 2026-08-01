package dev.danielkindl.ocho.ui.setup

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class SetupUiStateTest {

    @Test
    fun `isValid is true for the default state`() {
        assertTrue(SetupUiState().isValid)
    }

    @Test
    fun `isValid is false when total duration is zero`() {
        val state = SetupUiState(totalMinutes = 0, totalSeconds = 0, intervalMinutes = 1, intervalSeconds = 0)
        assertFalse(state.isValid)
    }

    @Test
    fun `isValid is false when interval is zero`() {
        val state = SetupUiState(totalMinutes = 20, totalSeconds = 0, intervalMinutes = 0, intervalSeconds = 0)
        assertFalse(state.isValid)
    }

    @Test
    fun `intervalExceedsTotal is false when interval fits within total`() {
        val state = SetupUiState(totalMinutes = 20, totalSeconds = 0, intervalMinutes = 1, intervalSeconds = 0)
        assertFalse(state.intervalExceedsTotal)
    }

    @Test
    fun `intervalExceedsTotal is true when interval is longer than total`() {
        val state = SetupUiState(totalMinutes = 0, totalSeconds = 30, intervalMinutes = 1, intervalSeconds = 0)
        assertTrue(state.intervalExceedsTotal)
    }

    @Test
    fun `intervalExceedsTotal is false when the state is otherwise invalid`() {
        // Zero interval makes isValid false, so intervalExceedsTotal must short-circuit to false
        // rather than reporting an interval/total comparison on a nonsensical config.
        val state = SetupUiState(totalMinutes = 20, totalSeconds = 0, intervalMinutes = 0, intervalSeconds = 0)
        assertFalse(state.intervalExceedsTotal)
    }

    @Test
    fun `defaultPresetName formats both minutes and seconds when present`() {
        val state = SetupUiState(totalMinutes = 20, totalSeconds = 30, intervalMinutes = 1, intervalSeconds = 5)
        assertEquals("20min 30s / 1min 5s", state.defaultPresetName())
    }

    @Test
    fun `defaultPresetName omits zero components`() {
        val state = SetupUiState(totalMinutes = 20, totalSeconds = 0, intervalMinutes = 0, intervalSeconds = 45)
        assertEquals("20min / 45s", state.defaultPresetName())
    }

    // ──────────────────────────────────────────────────────────────────────
    // Characterization table
    //
    // Pins the derived values across a spread of inputs so they can be ported
    // verbatim onto the unified setup state. Only the constructor call should
    // change when that happens. **A changed expected value means a regression,
    // not a test that needs updating.**
    // ──────────────────────────────────────────────────────────────────────

    @Test
    fun `round count over a table of configurations`() {
        // total, interval, expected rounds
        val cases = listOf(
            Triple(20 to 0, 1 to 0, 20),   // the canonical EMOM
            Triple(10 to 0, 0 to 30, 20),  // sub-minute intervals
            Triple(5 to 0, 2 to 0, 3),     // rounds up: a partial final interval still beeps
            Triple(1 to 0, 1 to 0, 1),     // interval equal to total
            Triple(0 to 45, 0 to 15, 3),   // seconds only
        )

        cases.forEach { (total, interval, expected) ->
            val state = SetupUiState(
                totalMinutes = total.first,
                totalSeconds = total.second,
                intervalMinutes = interval.first,
                intervalSeconds = interval.second,
            )
            assertEquals(
                "total=$total interval=$interval",
                expected,
                state.roundCount,
            )
        }
    }

    @Test
    fun `round count is zero when the configuration is invalid`() {
        assertEquals(0, SetupUiState(totalMinutes = 0, totalSeconds = 0).roundCount)
        assertEquals(0, SetupUiState(intervalMinutes = 0, intervalSeconds = 0).roundCount)
    }

    @Test
    fun `millisecond conversion over a table of configurations`() {
        val state = SetupUiState(
            totalMinutes = 20,
            totalSeconds = 30,
            intervalMinutes = 1,
            intervalSeconds = 5,
        )
        assertEquals(1_230_000L, state.totalDurationMillis)
        assertEquals(65_000L, state.intervalMillis)
    }

    @Test
    fun `interval label over a table of configurations`() {
        assertEquals("1min", SetupUiState(intervalMinutes = 1, intervalSeconds = 0).intervalLabel)
        assertEquals("45s", SetupUiState(intervalMinutes = 0, intervalSeconds = 45).intervalLabel)
        assertEquals(
            "1min 30s",
            SetupUiState(intervalMinutes = 1, intervalSeconds = 30).intervalLabel,
        )
    }
}
