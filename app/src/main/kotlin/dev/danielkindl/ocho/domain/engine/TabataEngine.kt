package dev.danielkindl.ocho.domain.engine

import dev.danielkindl.ocho.domain.model.TabataConfig
import dev.danielkindl.ocho.domain.model.TabataEvent
import kotlinx.coroutines.flow.SharedFlow

interface TabataEngine {
    val events: SharedFlow<TabataEvent>
    fun start(config: TabataConfig)
    fun pause()
    fun resume()
    fun stop()
}
