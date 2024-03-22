package com.g2s.trading.indicator.CandleStick

import com.g2s.trading.symbol.Symbol

data class CandleStick(
    val symbol: Symbol,
    val interval: Interval,
    val key: Long,  // 캔들스틱 시작 시간
    val open: Double,
    val high: Double,
    val low: Double,
    val close: Double,
    val volume: Double,
    val numberOfTrades: Int,    // 거래 횟수
)
