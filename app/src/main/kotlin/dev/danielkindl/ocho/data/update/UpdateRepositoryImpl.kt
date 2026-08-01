package dev.danielkindl.ocho.data.update

import dev.danielkindl.ocho.domain.model.AppUpdate
import dev.danielkindl.ocho.domain.model.SemVer
import dev.danielkindl.ocho.domain.model.UpdateChannel
import dev.danielkindl.ocho.domain.model.UpdateConfig
import dev.danielkindl.ocho.domain.repository.UpdateRepository
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import org.json.JSONArray
import org.json.JSONObject
import java.io.IOException
import java.net.HttpURLConnection
import java.net.URL
import javax.inject.Inject

/**
 * Reads the newest release this build is eligible for from the GitHub Releases API.
 *
 * The endpoint depends on [UpdateConfig.channel], and the two channels cannot see
 * each other's builds: `releases/latest` omits pre-releases by GitHub's own
 * definition, while the dev channel reads the full list and keeps only pre-releases.
 *
 * Network failures, HTTP errors and malformed payloads all surface as a failed
 * [Result] rather than an exception, because a failed update check must never take
 * down the settings screen.
 */
class UpdateRepositoryImpl @Inject constructor(
    private val config: UpdateConfig,
) : UpdateRepository {

    override suspend fun fetchLatestRelease(): Result<AppUpdate> = withContext(Dispatchers.IO) {
        runCatching {
            val connection = URL(endpoint()).openConnection() as HttpURLConnection
            connection.requestMethod = "GET"
            connection.setRequestProperty("Accept", "application/vnd.github+json")
            connection.setRequestProperty("User-Agent", USER_AGENT)
            connection.connectTimeout = TIMEOUT_MS
            connection.readTimeout = TIMEOUT_MS
            try {
                if (connection.responseCode != HttpURLConnection.HTTP_OK) {
                    throw IOException("GitHub API returned ${connection.responseCode}")
                }
                val body = connection.inputStream.bufferedReader().use { it.readText() }
                parseResponse(body)
            } finally {
                connection.disconnect()
            }
        }
    }

    private fun endpoint(): String = when (config.channel) {
        UpdateChannel.Stable -> "$API_BASE/${config.repoSlug}/releases/latest"
        UpdateChannel.Dev -> "$API_BASE/${config.repoSlug}/releases?per_page=$DEV_PAGE_SIZE"
    }

    /**
     * Turns a response body into an [AppUpdate]. Split from the network call so the
     * channel-selection logic — the part with real branching — is unit-testable.
     */
    internal fun parseResponse(body: String): AppUpdate = when (config.channel) {
        UpdateChannel.Stable -> parseRelease(JSONObject(body))
        UpdateChannel.Dev -> parseNewestPreRelease(body)
    }

    /**
     * Picks the highest-versioned pre-release from a `/releases` array.
     *
     * Selects by parsed version rather than by the array's order. GitHub returns
     * releases newest-created first, which usually matches version order but need not:
     * a re-run or a manually created tag could invert them, and offering an older dev
     * build would fail to install anyway because its versionCode would be lower.
     * Entries whose tags are not valid SemVer are skipped rather than throwing.
     */
    private fun parseNewestPreRelease(body: String): AppUpdate {
        val releases = JSONArray(body)
        val newest = (0 until releases.length())
            .map { releases.getJSONObject(it) }
            .filter { it.optBoolean("prerelease", false) }
            .mapNotNull { release ->
                SemVer.parse(release.optString("tag_name"))?.let { version -> version to release }
            }
            .maxByOrNull { (version, _) -> version }
            ?: error("No pre-release with a valid version tag was found")

        return parseRelease(newest.second)
    }

    internal fun parseRelease(json: JSONObject): AppUpdate {
        val tagName = json.getString("tag_name")
        val version = SemVer.parse(tagName) ?: error("Malformed release tag: $tagName")
        val assets = json.getJSONArray("assets")
        val downloadUrl = (0 until assets.length())
            .map { assets.getJSONObject(it) }
            .firstOrNull { it.getString("name").endsWith(".apk") }
            ?.getString("browser_download_url")
            ?: error("No APK asset found in release $tagName")

        return AppUpdate(
            version = version,
            tagName = tagName,
            downloadUrl = downloadUrl,
            releaseNotes = json.optString("body", ""),
        )
    }

    private companion object {
        const val API_BASE = "https://api.github.com/repos"
        const val USER_AGENT = "ocho-android"
        const val TIMEOUT_MS = 10_000

        /** Enough to look past the newest few stable releases and still find a pre-release. */
        const val DEV_PAGE_SIZE = 20
    }
}
