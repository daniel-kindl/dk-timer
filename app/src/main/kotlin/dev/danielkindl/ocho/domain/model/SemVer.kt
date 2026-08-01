package dev.danielkindl.ocho.domain.model

/**
 * A version number as defined by [Semantic Versioning 2.0.0](https://semver.org/).
 *
 * Models the version core plus pre-release identifiers. Build metadata (`+sha.abc`)
 * is accepted by [parse] but discarded, because SemVer §10 excludes it from
 * precedence — two versions differing only in metadata are the same version.
 *
 * Pre-release support is what makes the dev update channel work. Dev builds are
 * versioned `3.0.0-dev.12`, so [compareTo] must rank them below the `3.0.0` they
 * precede and above `3.0.0-dev.7`. Without it, [parse] returns null for every dev
 * build, the installed version reads as unknown, and the update check silently
 * reports "up to date" forever.
 */
data class SemVer(
    val major: Int,
    val minor: Int,
    val patch: Int,
    val preRelease: List<String> = emptyList(),
) : Comparable<SemVer> {

    /**
     * Orders versions by SemVer §11: version core numerically, then pre-release
     * identifiers, with a pre-release always ranking below the release it precedes.
     */
    override fun compareTo(other: SemVer): Int {
        val core = compareValuesBy(this, other, SemVer::major, SemVer::minor, SemVer::patch)
        return if (core != 0) core else comparePreRelease(preRelease, other.preRelease)
    }

    companion object {
        private const val CORE_PART_COUNT = 3

        /**
         * Parses [raw], tolerating a leading `v`, or returns null if it is not a valid
         * SemVer string. Callers treat null as "version unknown" rather than failing,
         * so malformed input can never crash an update check.
         */
        fun parse(raw: String): SemVer? {
            val withoutMetadata = raw.removePrefix("v").substringBefore('+')

            // Split on the *position* of the hyphen rather than with substringBefore/After,
            // which cannot distinguish "3.0.0" (no pre-release) from the invalid "3.0.0-"
            // (a pre-release with no identifiers) — both yield an empty remainder.
            val hyphen = withoutMetadata.indexOf('-')
            val hasPreRelease = hyphen >= 0

            val coreRaw = if (hasPreRelease) withoutMetadata.substring(0, hyphen) else withoutMetadata
            val core = parseCore(coreRaw) ?: return null

            val identifiers =
                if (hasPreRelease) withoutMetadata.substring(hyphen + 1).split(".") else emptyList()
            if (identifiers.any { !isValidIdentifier(it) }) return null

            return SemVer(core[0], core[1], core[2], identifiers)
        }

        /** Parses the `MAJOR.MINOR.PATCH` triple, rejecting negatives and leading zeros. */
        private fun parseCore(core: String): List<Int>? {
            val parts = core.split(".")
            if (parts.size != CORE_PART_COUNT) return null
            return parts.map { part ->
                val value = part.toIntOrNull() ?: return null
                if (value < 0 || hasLeadingZero(part)) return null
                value
            }
        }

        /**
         * Compares pre-release identifier lists per SemVer §11.
         *
         * An empty list means "not a pre-release", which ranks *above* any pre-release —
         * the rule that keeps `3.0.0` ahead of `3.0.0-dev.12`.
         */
        private fun comparePreRelease(left: List<String>, right: List<String>): Int {
            if (left.isEmpty() || right.isEmpty()) {
                return when {
                    left.isEmpty() && right.isEmpty() -> 0
                    left.isEmpty() -> 1
                    else -> -1
                }
            }
            for (index in 0 until minOf(left.size, right.size)) {
                val result = compareIdentifier(left[index], right[index])
                if (result != 0) return result
            }
            // "A larger set of pre-release fields has a higher precedence than a
            // smaller set, if all of the preceding identifiers are equal."
            return left.size.compareTo(right.size)
        }

        /**
         * Compares one identifier pair. All-digit identifiers compare numerically —
         * lexically, `dev.12` would sort below `dev.7`, which is precisely the bug this
         * avoids — and rank below any identifier containing letters.
         */
        private fun compareIdentifier(left: String, right: String): Int {
            val leftNumeric = left.toLongOrNull()
            val rightNumeric = right.toLongOrNull()
            return when {
                leftNumeric != null && rightNumeric != null -> leftNumeric.compareTo(rightNumeric)
                leftNumeric != null -> -1
                rightNumeric != null -> 1
                else -> left.compareTo(right)
            }
        }

        /** Accepts ASCII alphanumerics and hyphens; numeric identifiers may not have leading zeros. */
        private fun isValidIdentifier(identifier: String): Boolean {
            if (identifier.isEmpty()) return false
            val allowed =
                identifier.all { it in '0'..'9' || it in 'a'..'z' || it in 'A'..'Z' || it == '-' }
            if (!allowed) return false
            return !(identifier.all { it in '0'..'9' } && hasLeadingZero(identifier))
        }

        private fun hasLeadingZero(part: String): Boolean = part.length > 1 && part.startsWith('0')
    }
}
