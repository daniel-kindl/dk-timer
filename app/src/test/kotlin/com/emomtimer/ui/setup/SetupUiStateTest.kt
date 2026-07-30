package com.emomtimer.ui.setup

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
}
