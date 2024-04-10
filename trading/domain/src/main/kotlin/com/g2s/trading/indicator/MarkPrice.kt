package com.g2s.trading.indicator

import com.g2s.trading.symbol.Symbol

data class MarkPrice(
    val symbol: Symbol,
    val price: Double
)
