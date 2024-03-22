package com.g2s.trading.exchange

import com.binance.connector.futures.client.impl.UMFuturesClientImpl
import com.g2s.trading.indicator.MarkPriceUseCase
import com.g2s.trading.common.ObjectMapperProvider
import com.g2s.trading.order.OrderSide
import com.g2s.trading.order.OrderType
import com.g2s.trading.position.Position
import com.g2s.trading.position.PositionMode
import com.g2s.trading.position.PositionSide
import com.g2s.trading.symbol.Symbol
import com.g2s.trading.symbol.SymbolUseCase
import com.g2s.trading.util.BinanceOrderParameterConverter
import org.junit.jupiter.api.Disabled
import org.junit.jupiter.api.Test
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.boot.test.context.SpringBootTest
import java.math.BigDecimal
import java.math.RoundingMode

@Disabled
@SpringBootTest(classes = [TestConfig::class])
class OrderTest {
    private val om = ObjectMapperProvider.get()
    private val pretty = om.writerWithDefaultPrettyPrinter()
    private var positionMode = PositionMode.ONE_WAY_MODE
    private var positionSide = PositionSide.BOTH

    @Autowired
    private lateinit var clientImpl: UMFuturesClientImpl

    @Autowired
    private lateinit var markPriceUseCase: MarkPriceUseCase

    @Autowired
    private lateinit var symbolUseCase: SymbolUseCase

    @Test
    fun testOpenOrder() {
        val testNode = ObjectMapperProvider.get().createObjectNode()
        val symbol = Symbol.valueOf("ETHUSDT")
        val markPrice = markPriceUseCase.getMarkPrice(symbol).price
        val position = Position(
            positionKey = Position.PositionKey(symbol, OrderSide.SHORT),
            strategyKey = "test",
            symbol = symbol,
            orderSide = OrderSide.SHORT,
            orderType = OrderType.MARKET,
            entryPrice = markPrice,
            positionAmt = quantity(
                BigDecimal(symbolUseCase.getMinNotionalValue(symbol)),
                BigDecimal(markPrice),
                symbolUseCase.getQuantityPrecision(symbol)
            ),
            referenceData = testNode
        )
        val responseJson = om.readTree(
            clientImpl.account().newOrder(
                BinanceOrderParameterConverter.toBinanceOpenPositionParam(
                    position,
                    positionMode,
                    positionSide
                )
            )
        )
        print(pretty.writeValueAsString(responseJson))
    }

    @Test
    fun testCloseOrder() {
        val testNode = ObjectMapperProvider.get().createObjectNode()
        val symbol = Symbol.valueOf("ETHUSDT")
        val markPrice = markPriceUseCase.getMarkPrice(symbol).price
        val position = Position(
            positionKey = Position.PositionKey(symbol, OrderSide.SHORT),
            strategyKey = "test",
            symbol = symbol,
            orderSide = OrderSide.SHORT,
            orderType = OrderType.MARKET,
            entryPrice = markPrice,
            positionAmt = quantity(
                BigDecimal(symbolUseCase.getMinNotionalValue(symbol)),
                BigDecimal(markPrice),
                symbolUseCase.getQuantityPrecision(symbol)
            ),
            referenceData = testNode
        )

        val params = BinanceOrderParameterConverter.toBinanceClosePositionParam(
            position,
            positionMode,
            positionSide
        )

        params["newOrderRespType"] = "RESULT"
        val responseJson = om.readTree(
            clientImpl.account().newOrder(
                params
            )
        )
        print(pretty.writeValueAsString(responseJson))
    }

    @Test
    @Disabled
    fun changeLeverageTest() {
        val parameters = LinkedHashMap<String, Any>()
        parameters["symbol"] = Symbol.valueOf("BCHUSDT")
        parameters["leverage"] = 10
        println(pretty.writeValueAsString(om.readTree(clientImpl.account().changeInitialLeverage(parameters))))
    }

    private fun quantity(minNotional: BigDecimal, markPrice: BigDecimal, quantityPrecision: Int): Double {
        return minNotional.divide(markPrice, quantityPrecision, RoundingMode.CEILING).toDouble()
    }
}
