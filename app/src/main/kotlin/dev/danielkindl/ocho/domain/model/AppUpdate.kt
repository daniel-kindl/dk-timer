package dev.danielkindl.ocho.domain.model

data class AppUpdate(
    val version: SemVer,
    val tagName: String,
    val downloadUrl: String,
    val releaseNotes: String,
)
