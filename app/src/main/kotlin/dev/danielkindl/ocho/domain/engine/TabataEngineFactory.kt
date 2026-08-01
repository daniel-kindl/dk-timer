package dev.danielkindl.ocho.domain.engine

import dev.danielkindl.ocho.core.Clock
import kotlinx.coroutines.CoroutineScope

fun interface TabataEngineFactory {
    fun create(scope: CoroutineScope): TabataEngine
}

class DefaultTabataEngineFactory(private val clock: Clock) : TabataEngineFactory {
    override fun create(scope: CoroutineScope): TabataEngine = TabataEngineImpl(clock, scope)
}
