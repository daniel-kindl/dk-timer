package dev.danielkindl.ocho.ui.tabata.setup

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

    // ──────────────────────────────────────────────────────────────────────
    // Characterization table
    //
    // Ports verbatim onto the unified setup state. Only the constructor call
    // should change. **A changed expected value means a regression.**
    // ──────────────────────────────────────────────────────────────────────

    @Test
    fun `round count over a table of configurations`() {
        // total, work, rest, expected rounds. A round is one work phase, and the
        // engine never cuts a phase short, so the count rounds up.
        val cases = listOf(
            // 20min of 45s/15s cycles: 60s per cycle, so 20 rounds
            listOf(20, 0, 0, 45, 0, 15) to 20,
            // classic Tabata: 4min of 20/10, 30s per cycle, 8 rounds
            listOf(4, 0, 0, 20, 0, 10) to 8,
            // uneven: 1min of 40/20 is exactly 1 cycle
            listOf(1, 0, 0, 40, 0, 20) to 1,
            // partial final cycle still counts as a round
            listOf(1, 30, 0, 40, 0, 20) to 2,
        )

        cases.forEach { (input, expected) ->
            val state = TabataSetupUiState(
                totalMinutes = input[0], totalSeconds = input[1],
                workMinutes = input[2], workSeconds = input[3],
                restMinutes = input[4], restSeconds = input[5],
            )
            assertEquals("input=$input", expected, state.roundCount)
        }
    }

    @Test
    fun `pattern label over a table of configurations`() {
        val state = TabataSetupUiState(
            totalMinutes = 4, totalSeconds = 0,
            workMinutes = 0, workSeconds = 20,
            restMinutes = 0, restSeconds = 10,
        )
        assertEquals("8 × (20s work / 10s rest)", state.patternLabel)
    }

    @Test
    fun `millisecond conversion over a table of configurations`() {
        val state = TabataSetupUiState(
            totalMinutes = 20, totalSeconds = 30,
            workMinutes = 1, workSeconds = 5,
            restMinutes = 0, restSeconds = 15,
        )
        assertEquals(1_230_000L, state.totalDurationMillis)
        assertEquals(65_000L, state.workMillis)
        assertEquals(15_000L, state.restMillis)
    }
}
