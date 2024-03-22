package com.g2s.trading.indicator.MarkPrice

import com.g2s.trading.symbol.Symbol

data class MarkPrice(
    val symbol: Symbol,
    val price: Double
)
