package dev.danielkindl.ocho.domain.model

data class Preset(
    val id: String,
    val name: String,
    val totalMinutes: Int,
    val totalSeconds: Int,
    val intervalMinutes: Int,
    val intervalSeconds: Int,
)
