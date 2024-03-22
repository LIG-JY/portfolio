package com.g2s.trading.event

import com.g2s.trading.strategy.StrategySpec

interface TradingEventListener {
    fun type(): String

    fun addListener(spec: StrategySpec)

    fun removeListener(key: String)

    fun updateListener(spec: StrategySpec)
}
