package com.g2s.trading.event

import com.g2s.trading.position.Position
import org.springframework.context.ApplicationEvent

sealed class PositionEvent(
    source: Any
) : ApplicationEvent(source) {
    data class PositionSyncedEvent(
        val source: Position
    ) : PositionEvent(source)
}
