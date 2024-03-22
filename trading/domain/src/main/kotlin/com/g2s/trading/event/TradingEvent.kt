package com.g2s.trading.event

import com.g2s.trading.indicator.MarkPrice
import com.g2s.trading.indicator.indicator.CandleStick
import org.springframework.context.ApplicationEvent

sealed class TradingEvent(
    source: Any
) : ApplicationEvent(source) {
    data class CandleStickEvent(
        val source: CandleStick
    ) : TradingEvent(source)

    data class MarkPriceRefreshEvent(
        val source: MarkPrice
    ) : TradingEvent(source)
}
