package dev.danielkindl.ocho.domain.engine

import dev.danielkindl.ocho.domain.model.TimerConfig
import dev.danielkindl.ocho.domain.model.TimerEvent
import kotlinx.coroutines.flow.SharedFlow

interface TimerEngine {
    val events: SharedFlow<TimerEvent>
    fun start(config: TimerConfig)
    fun pause()
    fun resume()
    fun stop()
}
