package com.g2s.trading.controller

import com.g2s.trading.ManualTrader
import com.g2s.trading.indicator.MarkPrice.MarkPriceUseCase
import com.g2s.trading.account.Asset
import com.g2s.trading.common.ObjectMapperProvider
import com.g2s.trading.order.OrderSide
import com.g2s.trading.order.OrderType
import com.g2s.trading.position.Position
import com.g2s.trading.strategy.StrategySpec
import com.g2s.trading.strategy.StrategySpecServiceStatus
import com.g2s.trading.symbol.Symbol
import com.g2s.trading.symbol.SymbolUseCase
import org.springframework.web.bind.annotation.PostMapping
import org.springframework.web.bind.annotation.RequestMapping
import org.springframework.web.bind.annotation.RequestParam
import org.springframework.web.bind.annotation.RestController
import java.math.BigDecimal
import java.math.RoundingMode

@RestController
@RequestMapping("/manual")
class ManualController(
    val markPriceUseCase: MarkPriceUseCase,
    val symbolUseCase: SymbolUseCase,
    val manualTrader: ManualTrader
) {
    // 최소 가치(MIN_NOTIONAL) 주문
    @PostMapping("min/open")
    fun openMinNotionalValuePosition(@RequestParam(name = "symbol") symbolValue: String) {
        // testSpec
        val emptyJsonNode = ObjectMapperProvider.get().createArrayNode()
        val tempSpec = StrategySpec(
            strategyKey = "manual",
            strategyType = "manual",
            symbols = listOf(Symbol.valueOf(symbolValue)),
            asset = Asset.USDT,
            allocatedRatio = 0.0,
            op = emptyJsonNode,
            trigger = "",
            status = StrategySpecServiceStatus.SERVICE
        )

        val testNode = ObjectMapperProvider.get().createObjectNode()
        val symbol = Symbol.valueOf(symbolValue)
        val markPrice = markPriceUseCase.getMarkPrice(symbol).price
        val position = Position(
            positionKey = Position.PositionKey(symbol, OrderSide.SHORT),
            strategyKey = tempSpec.strategyKey,
            symbol = symbol,
            orderSide = OrderSide.SHORT,
            orderType = OrderType.MARKET,
            entryPrice = markPrice,
            positionAmt = BigDecimal(symbolUseCase.getMinNotionalValue(symbol)).divide(
                BigDecimal(markPrice),
                symbolUseCase.getQuantityPrecision(symbol),
                RoundingMode.CEILING
            ).toDouble(),
            referenceData = testNode
        )
        manualTrader.manuallyOpenPosition(position, tempSpec)
    }

    @PostMapping("close")
    fun close(@RequestParam(name = "symbol") symbolValue: String) {
        // manualSpec
        val emptyJsonNode = ObjectMapperProvider.get().createArrayNode()
        val tempSpec = StrategySpec(
            strategyKey = "manual",
            strategyType = "manual",
            symbols = listOf(Symbol.valueOf(symbolValue)),
            asset = Asset.USDT,
            allocatedRatio = 0.0,
            op = emptyJsonNode,
            trigger = "",
            status = StrategySpecServiceStatus.SERVICE
        )
        manualTrader.manuallyClosePosition(Symbol.valueOf(symbolValue), tempSpec)
    }
}
