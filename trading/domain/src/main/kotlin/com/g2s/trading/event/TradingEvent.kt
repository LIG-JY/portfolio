package com.g2s.trading.event

import com.g2s.trading.history.Commission
import com.g2s.trading.history.RealizedProfit
import com.g2s.trading.indicator.CandleStick
import com.g2s.trading.indicator.MarkPrice
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

    data class CommissionEvent(
        val source: Commission
    ) : TradingEvent(source)

    data class RealizedProfitAndCommissionEvent(
        val source: Pair<Commission, RealizedProfit>
    ) : TradingEvent(source)
}
