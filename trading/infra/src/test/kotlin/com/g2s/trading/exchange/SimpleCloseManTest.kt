package com.g2s.trading.exchange

import com.g2s.trading.indicator.MarkPriceUseCase
import com.g2s.trading.common.ObjectMapperProvider
import com.g2s.trading.order.OrderSide
import com.g2s.trading.order.OrderType
import com.g2s.trading.position.Position
import com.g2s.trading.symbol.Symbol
import org.junit.jupiter.api.DisplayName
import org.junit.jupiter.api.Test
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.boot.test.context.SpringBootTest
import org.springframework.test.context.ActiveProfiles
import java.math.BigDecimal

@ActiveProfiles("test")
@SpringBootTest(classes = [TestConfig::class])
class SimpleCloseManTest(
    @Autowired private val markPriceUseCase: MarkPriceUseCase
) {

    private val om = ObjectMapperProvider.get()

    @Test
    fun closeTest() {
        val position = Position(
            positionKey = Position.PositionKey(Symbol.valueOf("BTCUSDT"), OrderSide.SHORT),
            strategyKey = "test",
            symbol = Symbol.valueOf("BTCUSDT"),
            orderSide = OrderSide.SHORT,
            orderType = OrderType.MARKET,
            entryPrice = 69762.3,
            positionAmt = -0.002,
            // 직접 입력 필요
            referenceData = om.readTree(
                "{\n" +
                        "  \"symbol\": \"BTCUSDT\",\n" +
                        "  \"interval\": \"ONE_MINUTE\",\n" +
                        "  \"key\": \"1710247680000\",\n" +
                        "  \"open\": 71999,\n" +
                        "  \"high\": 72380,\n" +
                        "  \"low\": 71885,\n" +
                        "  \"close\": 71961,\n" +
                        "  \"volume\": 67.816,\n" +
                        "  \"numberOfTrades\": 61,\n" +
                        "  \"tailLength\": 571.5\n" +
                        "}"
            )
        )

        println(shouldClose(position))
    }

    @Test
    @DisplayName("REST API 호출해서 정상적으로 MarkPrice 받는지 테스트")
    fun getLastPriceTest() {
        println(markPriceUseCase.getMarkPrice(Symbol.valueOf("BTCUSDT")))
    }

    private fun shouldClose(position: Position): Boolean {
        val stopLossFactor = BigDecimal(1.2)
        val entryPrice = BigDecimal(position.entryPrice)
        val lastPrice = BigDecimal(markPriceUseCase.getMarkPrice(position.symbol).price)
        when (position.orderSide) {
            OrderSide.LONG -> {
                // 손절
                val stickLength =
                    BigDecimal(position.referenceData["high"].asDouble()).minus(BigDecimal(position.referenceData["low"].asDouble()))
                if (stickLength.multiply(stopLossFactor) > entryPrice.minus(lastPrice)) {
                    println(
                        "롱 손절: lastPrice: $lastPrice, 오픈시 고가 - 저가: $stickLength" +
                                ", StopLossFactor 반영 후 고가 - 저가: ${stickLength.multiply(stopLossFactor)}"
                    )
                    return true
                }
                // 익절
                if (lastPrice > entryPrice.plus(BigDecimal(position.referenceData["tailLength"].asDouble()))) {
                    println(
                        "롱 익절: lastPrice: $lastPrice, entryPrice: $entryPrice, 오픈시 꼬리 길이: ${position.referenceData["tailLength"].asDouble()}"
                    )
                    return true
                }
            }

            OrderSide.SHORT -> {
                // 손절
                val stickLength =
                    BigDecimal(position.referenceData["high"].asDouble()).minus(BigDecimal(position.referenceData["low"].asDouble()))
                if (stickLength.multiply(stopLossFactor) < lastPrice.minus(entryPrice)) {
                    println(
                        "숏 손절: lastPrice: $lastPrice, 오픈시 고가 - 저가: $stickLength" +
                                ", StopLossFactor 반영 후 고가 - 저가: ${stickLength.multiply(stopLossFactor)}"
                    )
                    return true
                }
                // 익절
                if (lastPrice < entryPrice.minus(BigDecimal(position.referenceData["tailLength"].asDouble()))) {
                    println(
                        "숏 익절: lastPrice: $lastPrice, entryPrice: $entryPrice, 오픈시 꼬리 길이: ${position.referenceData["tailLength"].asDouble()}"
                    )
                    return true;
                }
            }
        }
        return false
    }
}
