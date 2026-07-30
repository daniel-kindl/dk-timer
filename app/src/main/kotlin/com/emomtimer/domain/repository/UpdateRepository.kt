package com.emomtimer.domain.repository

import com.emomtimer.domain.model.AppUpdate

interface UpdateRepository {
    suspend fun fetchLatestRelease(): Result<AppUpdate>
}
