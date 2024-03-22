package com.g2s.trading.event

import com.g2s.trading.strategy.StrategySpec
import org.springframework.context.ApplicationEvent

sealed class StrategyEvent (
    source: Any
): ApplicationEvent(source){
    data class StartStrategyEvent(
        val source: StrategySpec
    ): StrategyEvent(source)

    data class StopStrategyEvent(
        val source: StrategySpec
    ): StrategyEvent(source)

    data class UpdateStrategyEvent(
        val source: StrategySpec
    ): StrategyEvent(source)
}
