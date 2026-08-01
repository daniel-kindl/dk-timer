package dev.danielkindl.ocho.domain.engine

import dev.danielkindl.ocho.core.Clock
import kotlinx.coroutines.CoroutineScope

/**
 * Creates a [TabataEngine] bound to a caller-supplied scope. The Tabata counterpart
 * to [TimerEngineFactory], and exists for the same reason: the engine's coroutine
 * must die with the view model that owns it.
 */
fun interface TabataEngineFactory {
    /** Builds an engine whose timing loop runs in, and is cancelled with, [scope]. */
    fun create(scope: CoroutineScope): TabataEngine
}

/** Production factory; supplies the real [Clock] to every engine it creates. */
class DefaultTabataEngineFactory(private val clock: Clock) : TabataEngineFactory {
    override fun create(scope: CoroutineScope): TabataEngine = TabataEngineImpl(clock, scope)
}
