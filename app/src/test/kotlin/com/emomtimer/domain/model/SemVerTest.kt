package com.emomtimer.domain.model

import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

class SemVerTest {

    @Test
    fun `parses a plain semver string`() {
        assertEquals(SemVer(2, 0, 1), SemVer.parse("2.0.1"))
    }

    @Test
    fun `strips a leading v prefix`() {
        assertEquals(SemVer(2, 0, 1), SemVer.parse("v2.0.1"))
    }

    @Test
    fun `compares numerically, not lexically`() {
        val newer = SemVer.parse("2.10.0")
        val older = SemVer.parse("2.9.0")
        checkNotNull(newer)
        checkNotNull(older)
        assertTrue(newer > older)
    }

    @Test
    fun `equal versions compare as equal`() {
        assertEquals(0, SemVer(1, 2, 3).compareTo(SemVer(1, 2, 3)))
    }

    @Test
    fun `malformed input returns null instead of throwing`() {
        assertNull(SemVer.parse("abc"))
        assertNull(SemVer.parse("1.2"))
        assertNull(SemVer.parse("1.2.3.4"))
        assertNull(SemVer.parse(""))
    }
}
