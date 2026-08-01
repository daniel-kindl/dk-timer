package dev.danielkindl.ocho.domain.model

/**
 * Where this build looks for updates, and which releases it will accept.
 *
 * Exists so the update code depends on plain values rather than on generated
 * `BuildConfig` fields, which would drag an Android dependency into the domain
 * layer. `AppModule` is the single place that reads `BuildConfig` and turns it
 * into one of these.
 *
 * @property repoSlug the GitHub `owner/name` pair to query, e.g. `daniel-kindl/ocho`.
 * @property channel which releases from that repository this build is eligible for.
 * @property installedVersion this build's own version, or null if its `versionName`
 *   is not valid SemVer. Null disables update prompts rather than guessing, since
 *   there is nothing meaningful to compare a fetched release against.
 */
data class UpdateConfig(
    val repoSlug: String,
    val channel: UpdateChannel,
    val installedVersion: SemVer?,
)
