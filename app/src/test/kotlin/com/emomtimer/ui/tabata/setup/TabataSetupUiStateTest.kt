package com.emomtimer.ui.tabata.setup

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class TabataSetupUiStateTest {

    @Test
    fun `isValid is true for the default state`() {
        assertTrue(TabataSetupUiState().isValid)
    }

    @Test
    fun `isValid is false when total duration is zero`() {
        val state = TabataSetupUiState(totalMinutes = 0, totalSeconds = 0)
        assertFalse(state.isValid)
    }

    @Test
    fun `isValid is false when work is zero`() {
        val state = TabataSetupUiState(workMinutes = 0, workSeconds = 0)
        assertFalse(state.isValid)
    }

    @Test
    fun `isValid is false when rest is zero`() {
        val state = TabataSetupUiState(restMinutes = 0, restSeconds = 0)
        assertFalse(state.isValid)
    }

    @Test
    fun `defaultPresetName formats total, work, and rest segments`() {
        val state = TabataSetupUiState(
            totalMinutes = 20, totalSeconds = 0,
            workMinutes = 0, workSeconds = 45,
            restMinutes = 0, restSeconds = 15,
        )
        assertEquals("20min / 45s work / 15s rest", state.defaultPresetName())
    }
}
