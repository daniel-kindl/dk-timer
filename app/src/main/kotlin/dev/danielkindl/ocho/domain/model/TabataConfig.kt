package dev.danielkindl.ocho.domain.model

data class TabataConfig(
    val workMillis: Long,
    val restMillis: Long,
    val totalDurationMillis: Long,
)
