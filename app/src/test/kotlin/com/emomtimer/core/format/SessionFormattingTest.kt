package com.emomtimer.core.format

import org.junit.Assert.assertEquals
import org.junit.Test

class SessionFormattingTest {

    @Test
    fun `sessionProgress computes a fraction between 0 and 1`() {
        assertEquals(0.5f, sessionProgress(5_000L, 10_000L))
    }

    @Test
    fun `sessionProgress guards against a zero total duration`() {
        assertEquals(0f, sessionProgress(5_000L, 0L))
    }

    @Test
    fun `sessionProgress guards against a negative total duration`() {
        assertEquals(0f, sessionProgress(5_000L, -1_000L))
    }

    @Test
    fun `sessionProgress clamps to 1 when elapsed exceeds total`() {
        assertEquals(1f, sessionProgress(15_000L, 10_000L))
    }

    @Test
    fun `sessionProgress clamps to 0 when elapsed is negative`() {
        assertEquals(0f, sessionProgress(-5_000L, 10_000L))
    }

    @Test
    fun `formatCountdown renders minutes and seconds`() {
        assertEquals("1:05", 65_000L.formatCountdown())
    }

    @Test
    fun `formatCountdown floors partial seconds`() {
        assertEquals("0:01", 1_900L.formatCountdown())
    }

    @Test
    fun `formatCountdown clamps negative values to zero`() {
        assertEquals("0:00", (-500L).formatCountdown())
    }

    @Test
    fun `formatElapsed pads minutes with a leading zero`() {
        assertEquals("01:05", 65_000L.formatElapsed())
    }

    @Test
    fun `formatElapsed handles zero`() {
        assertEquals("00:00", 0L.formatElapsed())
    }
}
