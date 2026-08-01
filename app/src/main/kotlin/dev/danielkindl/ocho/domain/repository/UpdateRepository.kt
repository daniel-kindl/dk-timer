package dev.danielkindl.ocho.domain.repository

import dev.danielkindl.ocho.domain.model.AppUpdate

interface UpdateRepository {
    suspend fun fetchLatestRelease(): Result<AppUpdate>
}
