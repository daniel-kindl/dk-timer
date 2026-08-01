package dev.danielkindl.ocho.domain.engine

import dev.danielkindl.ocho.core.Clock
import kotlinx.coroutines.CoroutineScope

/**
 * Creates a [TimerEngine] bound to a caller-supplied scope.
 *
 * A factory rather than a directly injected engine because the engine's lifetime
 * must match the view model's: injecting a singleton engine would leave its
 * coroutine running after the session screen is gone.
 */
fun interface TimerEngineFactory {
    /** Builds an engine whose timing loop runs in, and is cancelled with, [scope]. */
    fun create(scope: CoroutineScope): TimerEngine
}

/** Production factory; supplies the real [Clock] to every engine it creates. */
class DefaultTimerEngineFactory(private val clock: Clock) : TimerEngineFactory {
    override fun create(scope: CoroutineScope): TimerEngine = TimerEngineImpl(clock, scope)
}
