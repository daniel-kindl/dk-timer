package dev.danielkindl.ocho.domain.model

/**
 * A release available to install, as read from the GitHub Releases API.
 *
 * @property version parsed from [tagName]; compared against the installed version
 *   to decide whether to offer the update at all.
 * @property tagName the raw git tag, e.g. `v3.0.0` or `v3.0.0-dev.12`. Displayed
 *   verbatim so what the user sees matches the release page exactly.
 * @property downloadUrl direct link to the release's APK asset.
 * @property releaseNotes the release body, empty if the release has none.
 */
data class AppUpdate(
    val version: SemVer,
    val tagName: String,
    val downloadUrl: String,
    val releaseNotes: String,
)
