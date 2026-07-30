package com.emomtimer.domain.model

data class SemVer(val major: Int, val minor: Int, val patch: Int) : Comparable<SemVer> {

    override fun compareTo(other: SemVer): Int =
        compareValuesBy(this, other, SemVer::major, SemVer::minor, SemVer::patch)

    companion object {
        private const val PART_COUNT = 3

        fun parse(raw: String): SemVer? {
            val parts = raw.removePrefix("v").split(".")
            if (parts.size != PART_COUNT) return null
            val numbers = parts.map { it.toIntOrNull() ?: return null }
            return SemVer(numbers[0], numbers[1], numbers[2])
        }
    }
}
