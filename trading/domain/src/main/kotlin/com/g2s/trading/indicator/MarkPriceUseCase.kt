package com.g2s.trading.indicator

import com.g2s.trading.event.TradingEvent
import com.g2s.trading.exchange.Exchange
import com.g2s.trading.symbol.Symbol
import org.springframework.context.event.EventListener
import org.springframework.stereotype.Service

@Service
class MarkPriceUseCase(
    private val exchangeImpl: Exchange
) {
    private val markPrices = mutableMapOf<Symbol, MarkPrice>()

    @EventListener
    fun handleRefreshMarkPriceEvent(event: TradingEvent.MarkPriceRefreshEvent) {
        markPrices[event.source.symbol] = event.source
    }

    fun getMarkPrice(symbol: Symbol): MarkPrice {
        return markPrices[symbol] ?: exchangeImpl.getMarkPrice(symbol)
    }
}
