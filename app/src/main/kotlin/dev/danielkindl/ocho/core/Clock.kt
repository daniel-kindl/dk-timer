package dev.danielkindl.ocho.core

/**
 * Source of wall-clock time, injected rather than called statically.
 *
 * Exists so the timer engines stay deterministic under test: a fake [Clock] lets a
 * test advance an hour instantly and assert on exact interval boundaries, which is
 * the only practical way to verify drift-free timing. It also keeps `domain/` free
 * of `android.*`, since `System.currentTimeMillis` lives behind this interface.
 */
fun interface Clock {
    /** Milliseconds since the Unix epoch. */
    fun currentTimeMillis(): Long
}

/** The real clock. Bound in `AppModule`; tests substitute their own [Clock]. */
class SystemClock : Clock {
    override fun currentTimeMillis(): Long = System.currentTimeMillis()
}
