package com.g2s.trading.event

import org.springframework.context.ApplicationEvent
import org.springframework.context.event.ApplicationEventMulticaster
import org.springframework.stereotype.Component

@Component
class EventUseCase(
    private val applicationEventMulticaster: ApplicationEventMulticaster
) {
    fun publishEvent(vararg event: ApplicationEvent) {
        event.forEach {
            applicationEventMulticaster.multicastEvent(it)
        }
    }
}
