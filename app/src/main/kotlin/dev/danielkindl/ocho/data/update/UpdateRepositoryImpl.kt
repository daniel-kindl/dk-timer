package dev.danielkindl.ocho.data.update

import dev.danielkindl.ocho.domain.model.AppUpdate
import dev.danielkindl.ocho.domain.model.SemVer
import dev.danielkindl.ocho.domain.repository.UpdateRepository
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import org.json.JSONObject
import java.io.IOException
import java.net.HttpURLConnection
import java.net.URL
import javax.inject.Inject

class UpdateRepositoryImpl @Inject constructor() : UpdateRepository {

    override suspend fun fetchLatestRelease(): Result<AppUpdate> = withContext(Dispatchers.IO) {
        runCatching {
            val connection = URL(LATEST_RELEASE_URL).openConnection() as HttpURLConnection
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
                parseRelease(body)
            } finally {
                connection.disconnect()
            }
        }
    }

    internal fun parseRelease(body: String): AppUpdate {
        val json = JSONObject(body)
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
        const val LATEST_RELEASE_URL = "https://api.github.com/repos/daniel-kindl/dk-timer/releases/latest"
        const val USER_AGENT = "dk-timer-android"
        const val TIMEOUT_MS = 10_000
    }
}
