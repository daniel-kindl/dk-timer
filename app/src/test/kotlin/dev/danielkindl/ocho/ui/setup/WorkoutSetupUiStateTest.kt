package dev.danielkindl.ocho.ui.setup

import dev.danielkindl.ocho.domain.model.SessionRequest
import dev.danielkindl.ocho.domain.model.WorkoutMode
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

/**
 * Ported from `SetupUiStateTest` and `TabataSetupUiStateTest`.
 *
 * Every expected value below is carried over unchanged from those two suites. Only
 * the constructor call moved, which is the whole point: a number that had to change
 * during the port would have been a regression rather than a test needing an update.
 */
class WorkoutSetupUiStateTest {

    private fun emom(
        totalMinutes: Int = 20,
        totalSeconds: Int = 0,
        intervalMinutes: Int = 1,
        intervalSeconds: Int = 0,
    ) = WorkoutSetupUiState(
        mode = WorkoutMode.EMOM,
        totalMinutes = totalMinutes,
        totalSeconds = totalSeconds,
        intervalMinutes = intervalMinutes,
        intervalSeconds = intervalSeconds,
    )

    private fun tabata(
        totalMinutes: Int = 20,
        totalSeconds: Int = 0,
        workMinutes: Int = 0,
        workSeconds: Int = 45,
        restMinutes: Int = 0,
        restSeconds: Int = 15,
    ) = WorkoutSetupUiState(
        mode = WorkoutMode.TABATA,
        totalMinutes = totalMinutes,
        totalSeconds = totalSeconds,
        workMinutes = workMinutes,
        workSeconds = workSeconds,
        restMinutes = restMinutes,
        restSeconds = restSeconds,
    )

    private fun amrap(totalMinutes: Int = 20, totalSeconds: Int = 0) = WorkoutSetupUiState(
        mode = WorkoutMode.AMRAP,
        totalMinutes = totalMinutes,
        totalSeconds = totalSeconds,
    )

    // EMOM validation

    @Test
    fun `isValid is true for the default EMOM state`() {
        assertTrue(emom().isValid)
    }

    @Test
    fun `isValid is false when total duration is zero`() {
        assertFalse(emom(totalMinutes = 0, totalSeconds = 0).isValid)
    }

    @Test
    fun `isValid is false when interval is zero`() {
        assertFalse(emom(intervalMinutes = 0, intervalSeconds = 0).isValid)
    }

    @Test
    fun `intervalExceedsTotal is false when interval fits within total`() {
        assertFalse(emom(totalMinutes = 20, intervalMinutes = 1).intervalExceedsTotal)
    }

    @Test
    fun `intervalExceedsTotal is true when interval is longer than total`() {
        val state = emom(totalMinutes = 0, totalSeconds = 30, intervalMinutes = 1)
        assertTrue(state.intervalExceedsTotal)
    }

    @Test
    fun `intervalExceedsTotal is false when the state is otherwise invalid`() {
        // Zero interval makes isValid false, so intervalExceedsTotal must short-circuit to false
        // rather than reporting an interval/total comparison on a nonsensical config.
        val state = emom(totalMinutes = 20, intervalMinutes = 0, intervalSeconds = 0)
        assertFalse(state.intervalExceedsTotal)
    }

    @Test
    fun `intervalExceedsTotal is false outside EMOM`() {
        // Other modes have no interval, so the warning must never appear for them
        // even though the field still exists on the shared state.
        assertFalse(tabata().intervalExceedsTotal)
        assertFalse(amrap().intervalExceedsTotal)
    }

    // Tabata validation

    @Test
    fun `isValid is true for the default Tabata state`() {
        assertTrue(tabata().isValid)
    }

    @Test
    fun `isValid is false when Tabata total duration is zero`() {
        assertFalse(tabata(totalMinutes = 0, totalSeconds = 0).isValid)
    }

    @Test
    fun `isValid is false when work is zero`() {
        assertFalse(tabata(workMinutes = 0, workSeconds = 0).isValid)
    }

    @Test
    fun `isValid is false when rest is zero`() {
        assertFalse(tabata(restMinutes = 0, restSeconds = 0).isValid)
    }

    // AMRAP validation

    @Test
    fun `AMRAP validates only its total duration`() {
        // The interval, work and rest fields still hold their defaults here. AMRAP
        // reads none of them, so they must not affect whether START is enabled.
        assertTrue(amrap().isValid)
        assertFalse(amrap(totalMinutes = 0, totalSeconds = 0).isValid)
    }

    @Test
    fun `AMRAP reports no round count`() {
        // Its rounds are whatever the athlete manages, which the app cannot know.
        assertEquals(0, amrap().roundCount)
    }

    // Preset naming

    @Test
    fun `defaultPresetName formats both minutes and seconds when present`() {
        val state = emom(totalMinutes = 20, totalSeconds = 30, intervalMinutes = 1, intervalSeconds = 5)
        assertEquals("20min 30s / 1min 5s", state.defaultPresetName())
    }

    @Test
    fun `defaultPresetName omits zero components`() {
        val state = emom(totalMinutes = 20, totalSeconds = 0, intervalMinutes = 0, intervalSeconds = 45)
        assertEquals("20min / 45s", state.defaultPresetName())
    }

    @Test
    fun `defaultPresetName formats total, work, and rest segments`() {
        val state = tabata(totalMinutes = 20, totalSeconds = 0, workSeconds = 45, restSeconds = 15)
        assertEquals("20min / 45s work / 15s rest", state.defaultPresetName())
    }

    @Test
    fun `defaultPresetName for AMRAP is just the total`() {
        assertEquals("20min", amrap().defaultPresetName())
    }

    // Characterization tables, ported verbatim

    @Test
    fun `EMOM round count over a table of configurations`() {
        // total, interval, expected rounds
        val cases = listOf(
            Triple(20 to 0, 1 to 0, 20),   // the canonical EMOM
            Triple(10 to 0, 0 to 30, 20),  // sub-minute intervals
            Triple(5 to 0, 2 to 0, 3),     // rounds up: a partial final interval still beeps
            Triple(1 to 0, 1 to 0, 1),     // interval equal to total
            Triple(0 to 45, 0 to 15, 3),   // seconds only
        )

        cases.forEach { (total, interval, expected) ->
            val state = emom(
                totalMinutes = total.first,
                totalSeconds = total.second,
                intervalMinutes = interval.first,
                intervalSeconds = interval.second,
            )
            assertEquals("total=$total interval=$interval", expected, state.roundCount)
        }
    }

    @Test
    fun `EMOM round count is zero when the configuration is invalid`() {
        assertEquals(0, emom(totalMinutes = 0, totalSeconds = 0).roundCount)
        assertEquals(0, emom(intervalMinutes = 0, intervalSeconds = 0).roundCount)
    }

    @Test
    fun `Tabata round count over a table of configurations`() {
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
            val state = tabata(
                totalMinutes = input[0], totalSeconds = input[1],
                workMinutes = input[2], workSeconds = input[3],
                restMinutes = input[4], restSeconds = input[5],
            )
            assertEquals("input=$input", expected, state.roundCount)
        }
    }

    @Test
    fun `millisecond conversion over a table of configurations`() {
        val state = WorkoutSetupUiState(
            mode = WorkoutMode.TABATA,
            totalMinutes = 20, totalSeconds = 30,
            intervalMinutes = 1, intervalSeconds = 5,
            workMinutes = 1, workSeconds = 5,
            restMinutes = 0, restSeconds = 15,
        )
        assertEquals(1_230_000L, state.totalDurationMillis)
        assertEquals(65_000L, state.intervalMillis)
        assertEquals(65_000L, state.workMillis)
        assertEquals(15_000L, state.restMillis)
    }

    @Test
    fun `Tabata pattern label over a table of configurations`() {
        val state = tabata(totalMinutes = 4, workSeconds = 20, restSeconds = 10)
        assertEquals("8 × (20s work / 10s rest)", state.patternLabel)
    }

    @Test
    fun `toRequest reaches the values SessionRequestCharacterizationTest pins`() {
        // Closes the loop on the Part 0 seam test, which pins the boundary type but
        // constructs it directly. These are the same expected numbers, arrived at
        // through the unified setup state instead.
        val emomRequest = emom(
            totalMinutes = 20, totalSeconds = 0,
            intervalMinutes = 1, intervalSeconds = 30,
        ).toRequest() as SessionRequest.Emom
        assertEquals(1_200_000L, emomRequest.config.totalDurationMillis)
        assertEquals(90_000L, emomRequest.config.intervalMillis)

        val tabataRequest = tabata(
            totalMinutes = 20, totalSeconds = 0,
            workSeconds = 45, restSeconds = 15,
        ).toRequest() as SessionRequest.Tabata
        assertEquals(1_200_000L, tabataRequest.config.totalDurationMillis)
        assertEquals(45_000L, tabataRequest.config.workMillis)
        assertEquals(15_000L, tabataRequest.config.restMillis)

        val amrapRequest = amrap(totalMinutes = 20).toRequest() as SessionRequest.Amrap
        assertEquals(1_200_000L, amrapRequest.config.totalDurationMillis)
    }

    @Test
    fun `a preset round-trips through the setup state`() {
        val original = tabata(totalMinutes = 4, workSeconds = 20, restSeconds = 10)
        val preset = original.toPreset(id = "1", name = "")

        // A blank name falls back to the generated one rather than saving an unlabelled preset.
        assertEquals("4min / 20s work / 10s rest", preset.name)
        assertEquals(original, tabata().withPreset(preset))
    }

    @Test
    fun `EMOM pattern label over a table of configurations`() {
        // The old state exposed the bare interval as `intervalLabel`. It now appears
        // inside `patternLabel`, so the formatted durations are asserted there.
        assertEquals("20 × 1min", emom(intervalMinutes = 1, intervalSeconds = 0).patternLabel)
        assertEquals("27 × 45s", emom(intervalMinutes = 0, intervalSeconds = 45).patternLabel)
        assertEquals("14 × 1min 30s", emom(intervalMinutes = 1, intervalSeconds = 30).patternLabel)
    }
}
