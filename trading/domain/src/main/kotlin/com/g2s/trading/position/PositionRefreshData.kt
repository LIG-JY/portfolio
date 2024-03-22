package com.g2s.trading.position

import com.g2s.trading.symbol.Symbol

data class PositionRefreshData(
    val symbol: Symbol,
    val entryPrice: Double,
    val positionAmt: Double,
    val positionSide: PositionSide,
)
