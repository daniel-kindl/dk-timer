package dev.danielkindl.ocho.domain.model

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
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
    fun `a newer major version is greater regardless of minor and patch`() {
        // Pins the exact comparison UpdateViewModel.toUiState() relies on to decide
        // whether a fetched release is newer than the installed version.
        assertTrue(SemVer(3, 0, 0) > SemVer(2, 9, 9))
    }

    @Test
    fun `equal versions are not strictly newer`() {
        assertFalse(SemVer(2, 2, 0) > SemVer(2, 2, 0))
    }

    @Test
    fun `malformed input returns null instead of throwing`() {
        assertNull(SemVer.parse("abc"))
        assertNull(SemVer.parse("1.2"))
        assertNull(SemVer.parse("1.2.3.4"))
        assertNull(SemVer.parse(""))
    }

    @Test
    fun `parses a dev channel prerelease version`() {
        // The exact shape assembleDev produces: versionName + "-dev.<run number>".
        assertEquals(
            SemVer(3, 0, 0, listOf("dev", "7")),
            SemVer.parse("v3.0.0-dev.7"),
        )
    }

    @Test
    fun `prerelease identifiers compare numerically, not lexically`() {
        // The bug this guards: as strings, "12" < "7", so a lexical comparison would
        // rank build 12 as older than build 7 and the dev channel would stop
        // offering updates after the ninth build.
        val newer = checkNotNull(SemVer.parse("3.0.0-dev.12"))
        val older = checkNotNull(SemVer.parse("3.0.0-dev.7"))
        assertTrue(newer > older)
    }

    @Test
    fun `a prerelease ranks below the release it precedes`() {
        val release = checkNotNull(SemVer.parse("3.0.0"))
        val preRelease = checkNotNull(SemVer.parse("3.0.0-dev.7"))
        assertTrue(preRelease < release)
    }

    @Test
    fun `a prerelease of a later patch outranks the current release`() {
        val next = checkNotNull(SemVer.parse("3.0.1-dev.1"))
        val current = checkNotNull(SemVer.parse("3.0.0"))
        assertTrue(next > current)
    }

    @Test
    fun `numeric identifiers rank below alphanumeric ones`() {
        val alphanumeric = checkNotNull(SemVer.parse("3.0.0-alpha"))
        val numeric = checkNotNull(SemVer.parse("3.0.0-1"))
        assertTrue(numeric < alphanumeric)
    }

    @Test
    fun `a longer identifier list outranks a shorter prefix of itself`() {
        val longer = checkNotNull(SemVer.parse("3.0.0-dev.1"))
        val shorter = checkNotNull(SemVer.parse("3.0.0-dev"))
        assertTrue(longer > shorter)
    }

    @Test
    fun `build metadata is ignored for precedence`() {
        assertEquals(SemVer(3, 0, 0), SemVer.parse("3.0.0+build.5"))
        assertEquals(
            SemVer(3, 0, 0, listOf("dev", "7")),
            SemVer.parse("3.0.0-dev.7+abc123"),
        )
    }

    @Test
    fun `malformed prerelease identifiers return null`() {
        assertNull(SemVer.parse("3.0.0-"))
        assertNull(SemVer.parse("3.0.0-dev..1"))
        assertNull(SemVer.parse("3.0.0-dev.01"))
    }
}
