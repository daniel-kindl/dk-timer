package dev.danielkindl.ocho.domain.repository

import dev.danielkindl.ocho.domain.model.AppUpdate

/** Looks up the newest release this build is eligible to install. */
interface UpdateRepository {

    /**
     * Fetches the newest release on this build's update channel.
     *
     * Returns a failed [Result] rather than throwing, and does **not** compare
     * against the installed version — callers decide whether the result is newer.
     */
    suspend fun fetchLatestRelease(): Result<AppUpdate>
}
