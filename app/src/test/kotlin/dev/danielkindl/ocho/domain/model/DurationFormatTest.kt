package dev.danielkindl.ocho.domain.model

import org.junit.Assert.assertEquals
import org.junit.Test

class DurationFormatTest {

    @Test
    fun `minutesSecondsToMillis converts minutes and seconds to milliseconds`() {
        assertEquals(65_000L, minutesSecondsToMillis(1, 5))
    }

    @Test
    fun `minutesSecondsToMillis handles zero`() {
        assertEquals(0L, minutesSecondsToMillis(0, 0))
    }

    @Test
    fun `minutesSecondsToMillis handles minutes only`() {
        assertEquals(120_000L, minutesSecondsToMillis(2, 0))
    }

    @Test
    fun `formatDuration shows minutes and seconds when both are present`() {
        assertEquals("1min 5s", formatDuration(1, 5))
    }

    @Test
    fun `formatDuration omits seconds when zero`() {
        assertEquals("2min", formatDuration(2, 0))
    }

    @Test
    fun `formatDuration falls back to seconds when minutes are zero`() {
        assertEquals("45s", formatDuration(0, 45))
    }

    @Test
    fun `formatDuration shows zero seconds when both are zero`() {
        assertEquals("0s", formatDuration(0, 0))
    }
}
