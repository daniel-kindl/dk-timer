package dev.danielkindl.ocho.domain.model

/**
 * Which stream of GitHub releases a build follows when checking for updates.
 *
 * The two channels are mutually invisible by construction: stable builds read the
 * `releases/latest` endpoint, which GitHub defines as excluding pre-releases, while
 * dev builds read the full release list and consider only pre-releases. A stable
 * install can therefore never be offered a dev build, or the reverse.
 *
 * @property id the value stored in `BuildConfig.UPDATE_CHANNEL`, resolved by [fromId].
 */
enum class UpdateChannel(val id: String) {

    /** Tagged releases published from `main`, e.g. `v3.0.0`. */
    Stable("stable"),

    /** Pre-releases published on every push to `dev`, e.g. `v3.0.0-dev.12`. */
    Dev("dev"),

    ;

    companion object {
        /**
         * Resolves the build-time channel id, falling back to [Stable] if it is
         * unrecognised — an unknown value must never silently move an install onto
         * the dev channel.
         */
        fun fromId(id: String): UpdateChannel = entries.firstOrNull { it.id == id } ?: Stable
    }
}
