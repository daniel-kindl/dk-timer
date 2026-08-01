package dev.danielkindl.ocho.data.update

import dev.danielkindl.ocho.domain.model.SemVer
import dev.danielkindl.ocho.domain.model.UpdateChannel
import dev.danielkindl.ocho.domain.model.UpdateConfig
import org.junit.Assert.assertEquals
import org.junit.Assert.assertThrows
import org.junit.Test

class UpdateRepositoryImplTest {

    private fun repositoryFor(channel: UpdateChannel) = UpdateRepositoryImpl(
        UpdateConfig(
            repoSlug = "daniel-kindl/ocho",
            channel = channel,
            installedVersion = SemVer.parse("3.0.0"),
        )
    )

    private val repository = repositoryFor(UpdateChannel.Stable)

    private fun releaseJson(
        tagName: String = "v2.3.0",
        assets: String = """[{"name":"ocho-2.3.0.apk","browser_download_url":"https://example.com/app.apk"}]""",
        body: String? = "Release notes",
        preRelease: Boolean? = null,
    ): String {
        val bodyField = body?.let { """"body":"$it",""" }.orEmpty()
        val preReleaseField = preRelease?.let { """"prerelease":$it,""" }.orEmpty()
        return """{"tag_name":"$tagName",$preReleaseField$bodyField"assets":$assets}"""
    }

    @Test
    fun `parses a valid release`() {
        val update = repository.parseResponse(releaseJson())

        assertEquals(SemVer(2, 3, 0), update.version)
        assertEquals("v2.3.0", update.tagName)
        assertEquals("https://example.com/app.apk", update.downloadUrl)
        assertEquals("Release notes", update.releaseNotes)
    }

    @Test
    fun `defaults releaseNotes to empty string when body is missing`() {
        val update = repository.parseResponse(releaseJson(body = null))

        assertEquals("", update.releaseNotes)
    }

    @Test
    fun `throws when the release tag is malformed semver`() {
        assertThrows(IllegalStateException::class.java) {
            repository.parseResponse(releaseJson(tagName = "not-a-version"))
        }
    }

    @Test
    fun `throws when no apk asset is present`() {
        val nonApkAssets = """[{"name":"README.md","browser_download_url":"https://example.com/readme"}]"""
        assertThrows(IllegalStateException::class.java) {
            repository.parseResponse(releaseJson(assets = nonApkAssets))
        }
    }

    @Test
    fun `throws when tag_name is missing`() {
        assertThrows(org.json.JSONException::class.java) {
            repository.parseResponse("""{"assets":[]}""")
        }
    }

    @Test
    fun `dev channel picks the highest versioned prerelease`() {
        val releases = """[
            ${releaseJson(tagName = "v3.0.0", preRelease = false)},
            ${releaseJson(tagName = "v3.1.0-dev.7", preRelease = true)},
            ${releaseJson(tagName = "v3.1.0-dev.12", preRelease = true)}
        ]"""

        val update = repositoryFor(UpdateChannel.Dev).parseResponse(releases)

        // Newest by version, not by position: GitHub orders by creation date, which a
        // re-run or a hand-made tag can put out of step with version order.
        assertEquals("v3.1.0-dev.12", update.tagName)
    }

    @Test
    fun `dev channel ignores stable releases entirely`() {
        val releases = """[
            ${releaseJson(tagName = "v9.9.9", preRelease = false)},
            ${releaseJson(tagName = "v3.1.0-dev.1", preRelease = true)}
        ]"""

        val update = repositoryFor(UpdateChannel.Dev).parseResponse(releases)

        assertEquals("v3.1.0-dev.1", update.tagName)
    }

    @Test
    fun `dev channel throws when the list holds no prereleases`() {
        val releases = """[${releaseJson(tagName = "v3.0.0", preRelease = false)}]"""

        assertThrows(IllegalStateException::class.java) {
            repositoryFor(UpdateChannel.Dev).parseResponse(releases)
        }
    }

    @Test
    fun `dev channel skips prereleases whose tag is not valid semver`() {
        val releases = """[
            ${releaseJson(tagName = "nightly", preRelease = true)},
            ${releaseJson(tagName = "v3.1.0-dev.4", preRelease = true)}
        ]"""

        val update = repositoryFor(UpdateChannel.Dev).parseResponse(releases)

        assertEquals("v3.1.0-dev.4", update.tagName)
    }

    @Test
    fun `a release with no prerelease flag is treated as stable`() {
        // GitHub always sends the field, but a missing one must not be read as a
        // prerelease - that would leak stable builds onto the dev channel.
        val releases = """[${releaseJson(tagName = "v3.0.0", preRelease = null)}]"""

        assertThrows(IllegalStateException::class.java) {
            repositoryFor(UpdateChannel.Dev).parseResponse(releases)
        }
    }
}
