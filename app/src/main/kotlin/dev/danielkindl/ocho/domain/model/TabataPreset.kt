package dev.danielkindl.ocho.domain.model

data class TabataPreset(
    val id: String,
    val name: String,
    val totalMinutes: Int,
    val totalSeconds: Int,
    val workMinutes: Int,
    val workSeconds: Int,
    val restMinutes: Int,
    val restSeconds: Int,
)
