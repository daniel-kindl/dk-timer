package com.emomtimer.data.update

import com.emomtimer.domain.model.SemVer
import org.junit.Assert.assertEquals
import org.junit.Assert.assertThrows
import org.junit.Test

class UpdateRepositoryImplTest {

    private val repository = UpdateRepositoryImpl()

    private fun releaseJson(
        tagName: String = "v2.3.0",
        assets: String = """[{"name":"dk-timer-2.3.0.apk","browser_download_url":"https://example.com/app.apk"}]""",
        body: String? = "Release notes",
    ): String {
        val bodyField = body?.let { """"body":"$it",""" }.orEmpty()
        return """{"tag_name":"$tagName",$bodyField"assets":$assets}"""
    }

    @Test
    fun `parses a valid release`() {
        val update = repository.parseRelease(releaseJson())

        assertEquals(SemVer(2, 3, 0), update.version)
        assertEquals("v2.3.0", update.tagName)
        assertEquals("https://example.com/app.apk", update.downloadUrl)
        assertEquals("Release notes", update.releaseNotes)
    }

    @Test
    fun `defaults releaseNotes to empty string when body is missing`() {
        val update = repository.parseRelease(releaseJson(body = null))

        assertEquals("", update.releaseNotes)
    }

    @Test
    fun `throws when the release tag is malformed semver`() {
        assertThrows(IllegalStateException::class.java) {
            repository.parseRelease(releaseJson(tagName = "not-a-version"))
        }
    }

    @Test
    fun `throws when no apk asset is present`() {
        val nonApkAssets = """[{"name":"README.md","browser_download_url":"https://example.com/readme"}]"""
        assertThrows(IllegalStateException::class.java) {
            repository.parseRelease(releaseJson(assets = nonApkAssets))
        }
    }

    @Test
    fun `throws when tag_name is missing`() {
        assertThrows(org.json.JSONException::class.java) {
            repository.parseRelease("""{"assets":[]}""")
        }
    }
}
