package dev.danielkindl.ocho.domain.model

import dev.danielkindl.ocho.ui.setup.WorkoutSetupUiState
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

/**
 * Invariants of the presets a testing build ships with.
 *
 * These are a convenience, so it would be easy to leave them unchecked. They are also
 * the first thing a device check touches, and every failure mode here is silent:
 * duplicate ids crash the chip row rather than the build, an unmarked preset offers a
 * delete control that cannot work, and a preset that fails validation leaves START
 * greyed out with no explanation.
 */
class DeviceCheckPresetsTest {

    private val leadInMillis = 3_000L

    @Test
    fun `every preset is marked built-in`() {
        // Anything unmarked would show a delete control, and deleting it would do
        // nothing, since built-ins never reach the store.
        assertTrue(DEVICE_CHECK_PRESETS.all { it.builtIn })
    }

    @Test
    fun `ids are unique`() {
        // The chip row keys on the id. Duplicates throw during composition.
        val ids = DEVICE_CHECK_PRESETS.map { it.id }
        assertEquals(ids.size, ids.distinct().size)
    }

    @Test
    fun `every mode is covered`() {
        assertEquals(
            WorkoutMode.entries.toSet(),
            DEVICE_CHECK_PRESETS.map { it.mode }.toSet(),
        )
    }

    @Test
    fun `every preset starts a workout without further input`() {
        // The point of them: tap the chip, tap Start. A preset that failed validation
        // would leave START disabled and the tester wondering why.
        DEVICE_CHECK_PRESETS.forEach { preset ->
            val state = WorkoutSetupUiState(mode = preset.mode).withPreset(preset)
            assertTrue("${preset.name} must enable START", state.isValid)
            state.toRequest()
        }
    }

    @Test
    fun `the EMOM presets cover all four lead-in cases`() {
        // The four shapes where the lead-in and the final numeral are decided. If one
        // is missing, a regression in it survives a full device pass unheard.
        val shapes = DEVICE_CHECK_PRESETS
            .filter { it.mode == WorkoutMode.EMOM }
            .map { preset ->
                val state = WorkoutSetupUiState(mode = preset.mode).withPreset(preset)
                val total = state.totalDurationMillis
                val interval = state.intervalMillis
                when {
                    interval > total -> "interval outlasts the workout"
                    total % interval == 0L -> "exact multiple"
                    total % interval > leadInMillis -> "remainder longer than the lead-in"
                    else -> "remainder at or below the lead-in"
                }
            }

        assertEquals(
            setOf(
                "exact multiple",
                "remainder longer than the lead-in",
                "remainder at or below the lead-in",
                "interval outlasts the workout",
            ),
            shapes.toSet(),
        )
    }
}
