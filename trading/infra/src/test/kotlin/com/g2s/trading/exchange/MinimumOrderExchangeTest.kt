package com.g2s.trading.exchange

import com.binance.connector.futures.client.impl.UMFuturesClientImpl
import com.fasterxml.jackson.databind.JsonNode
import com.fasterxml.jackson.databind.node.DoubleNode
import com.fasterxml.jackson.databind.node.ObjectNode
import com.g2s.trading.common.ObjectMapperProvider
import com.g2s.trading.indicator.CandleStick.CandleStick
import com.g2s.trading.indicator.CandleStick.Interval
import com.g2s.trading.openman.AnalyzeReport
import com.g2s.trading.order.OrderSide
import com.g2s.trading.symbol.Symbol
import org.junit.jupiter.api.Disabled
import org.junit.jupiter.api.Test
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.boot.test.context.SpringBootTest
import java.math.BigDecimal
import java.text.SimpleDateFormat
import java.util.*
import kotlin.math.max
import kotlin.math.min

@Disabled
@SpringBootTest(classes = [TestConfig::class])
class MinimumOrderExchangeTest {

    @Autowired
    private lateinit var binanceClient: UMFuturesClientImpl
    // ObjectMapper
    private val om = ObjectMapperProvider.get()

    private val pretty = om.writerWithDefaultPrettyPrinter()
    // const
    val MAXIMUM_HAMMER_RATIO = BigDecimal(9999)

    val TAKER_FEE_RATE = 0.00045  // taker fee : 0.045%

    @Test
    fun testExchangeInfo() {
        val jsonExchangeInfo = om.readTree(binanceClient.market().exchangeInfo())
        println(pretty.writeValueAsString(jsonExchangeInfo))
    }

    // Binance App Trading View 차트에서 LastPrice
    @Test
    fun testGetContinuousContractCandleStickData() {
        val parameters = LinkedHashMap<String, Any>()
        parameters["pair"] = "BTCUSDT"
        parameters["contractType"] = "PERPETUAL"
        parameters["interval"] = "1m"
        parameters["startTime"] = convertMilliseconds("2024-02-28-0623")
        parameters["limit"] = 1
        val jsonCandleStick = om.readTree(binanceClient.market().continuousKlines(parameters))
        println(pretty.writeValueAsString(jsonCandleStick))
    }

    // Binance App Trading View 차트에서 IndexPrice
    @Test
    fun testGetIndexPriceCandleStickData() {
        val parameters = LinkedHashMap<String, Any>()
        parameters["pair"] = "BTCUSDT"
        parameters["interval"] = "1m"
        parameters["startTime"] = convertMilliseconds("2024-02-28-0623")
        parameters["limit"] = 1
        val jsonCandleStick = om.readTree(binanceClient.market().indexPriceKlines(parameters))
        println(pretty.writeValueAsString(jsonCandleStick))
    }

    // Binance App Trading View 차트에서 MarkPrice
    @Test
    fun testGetMarkPriceCandleStickData() {
        val parameters = LinkedHashMap<String, Any>()
        parameters["symbol"] = "BTCUSDT"
        parameters["interval"] = "1m"
        parameters["startTime"] = convertMilliseconds("2024-02-28-0623")
        parameters["limit"] = 1
        val jsonCandleStick = om.readTree(binanceClient.market().markPriceKlines(parameters))
        println(pretty.writeValueAsString(jsonCandleStick))
    }

    // Binance App Trading View 차트에서 LastPrice
    @Test
    fun testGetCandleStickData() {
        val parameters = LinkedHashMap<String, Any>()
        parameters["symbol"] = "BTCUSDT"
        parameters["interval"] = "1m"
        parameters["startTime"] = convertMilliseconds("2024-02-28-0623")
        parameters["limit"] = 1
        val jsonCandleStick = om.readTree(binanceClient.market().klines(parameters))
        println(pretty.writeValueAsString(jsonCandleStick))
    }

    @Test
    fun testGetMinQtyByMarketOrder() {
        val symbol = Symbol.valueOf("BTCUSDT")
        getMinQtyByMarketOrder(symbol)
    }


    private fun getMinQtyByMarketOrder(symbol: Symbol) {
        val jsonSymbols = om.readTree(binanceClient.market().exchangeInfo()).get("symbols")
        if (jsonSymbols.isArray) {
            jsonSymbols.forEach { symbolNode ->
                if (symbolNode.get("symbol").asText() == symbol.value) {
                    val filterNode = symbolNode.get(
                        "filters"
                    )
                    filterNode.forEach { node ->
                        if (node.get("filterType").asText() == "MARKET_LOT_SIZE") {
                            val minQty = node.get("minQty").asText()
                            print("${symbol.value} : ")
                            println(minQty)
                        }
                    }
                }
            }
        }
    }

    private fun convertMilliseconds(dateString: String): Long {
        // Specify the format of the date string
        val format = SimpleDateFormat("yyyy-MM-dd-HHmm")

        // Set the SimpleDateFormat timezone to UTC
        format.timeZone = TimeZone.getTimeZone("UTC")

        // Parse the date string into a Date object
        val date = format.parse(dateString)

        // Convert the Date object to milliseconds
        val dateInMilliseconds = date.time

        return dateInMilliseconds
    }

    // 전략의 analyze 메서드를 테스트
    @Test
    fun testAnalyze() {
        val candleStick = getCandleStickData(Symbol.valueOf("BTCUSDT"), Interval.ONE_MINUTE, "2024-02-27-1037")
        // check report
        when (val report = analyze(candleStick, 1.5, 1.5)) {
            is AnalyzeReport.MatchingReport -> {
                println(report)
            }
            is AnalyzeReport.NonMatchingReport -> {
                println("non matching")
            }
        }
    }

    private fun getCandleStickData(symbol: Symbol, interval: Interval, startTime: String): CandleStick {
        val parameters = LinkedHashMap<String, Any>()
        parameters["symbol"] = symbol.value
        parameters["interval"] = interval.value
        parameters["startTime"] = convertMilliseconds(startTime)
        parameters["limit"] = 1
        val jsonCandleStick = om.readTree(binanceClient.market().klines(parameters)).get(0)
        // json to candlestikc mapping
        val candleStick =
            CandleStick(
                symbol = symbol,
                interval = interval,
                key = jsonCandleStick.get(0).asLong(),
                open = jsonCandleStick.get(1).asDouble(),
                high = jsonCandleStick.get(2).asDouble(),
                low = jsonCandleStick.get(3).asDouble(),
                close = jsonCandleStick.get(4).asDouble(),
                volume = jsonCandleStick.get(5).asDouble(),
                numberOfTrades = jsonCandleStick.get(8).asInt()
            )
        return candleStick
    }

    private fun analyze(
        candleStick: CandleStick,
        hammerRatio: Double,
        scale: Double
    ): AnalyzeReport {
        println("analyze... candleStick: $candleStick")
        val tailTop = BigDecimal(candleStick.high)
        val tailBottom = BigDecimal(candleStick.low)
        val bodyTop = BigDecimal(max(candleStick.open, candleStick.close))
        val bodyBottom = BigDecimal(min(candleStick.open, candleStick.close))
        val bodyLength = bodyTop - bodyBottom
        val decimalHammerRatio = BigDecimal(hammerRatio)
        val decimalScale = BigDecimal(scale)

        if (bodyLength.compareTo(BigDecimal.ZERO) == 0) {
            println("star candle")
            val topTailLength = tailTop - bodyTop
            val bottomTailLength = bodyTop - tailBottom
            if (topTailLength > bottomTailLength) {
                println("star top tail")
                val candleHammerRatio =
                    if (bottomTailLength.compareTo(BigDecimal.ZERO) != 0) topTailLength / bottomTailLength else MAXIMUM_HAMMER_RATIO
                println("calculated candleHammerRatio: $candleHammerRatio, topTailLength: $topTailLength, bottomTailLength: $bottomTailLength")
                if (candleHammerRatio > decimalHammerRatio && isPositivePnl(
                        bodyTop,
                        bodyTop.minus(topTailLength.multiply(decimalScale))
                    )
                ) {
                    println("candleHammer: ${candleHammerRatio.toDouble()}, decimalHammer: ${decimalHammerRatio.toDouble()}")
                    val referenceData = ObjectMapperProvider.get().convertValue(candleStick, JsonNode::class.java)
                    (referenceData as ObjectNode).set<DoubleNode>(
                        "tailLength",
                        DoubleNode(topTailLength.multiply(decimalScale).toDouble())
                    )
                    println("scaled tailLength: ${topTailLength.multiply(decimalScale)}")
                    return AnalyzeReport.MatchingReport(candleStick.symbol, OrderSide.SHORT, referenceData)
                }
            } else {
                println("star bottom tail")
                val candleHammerRatio =
                    if (topTailLength.compareTo(BigDecimal.ZERO) != 0) bottomTailLength / topTailLength else MAXIMUM_HAMMER_RATIO
                println("calculated candleHammerRatio: $candleHammerRatio, topTailLength: $topTailLength, bottomTailLength: $bottomTailLength")
                if (candleHammerRatio > decimalHammerRatio && isPositivePnl(
                        bodyTop,
                        bodyTop.plus(bottomTailLength.multiply(decimalScale))
                    )
                ) {
                    println("candleHammer: ${candleHammerRatio.toDouble()}, decimalHammer: ${decimalHammerRatio.toDouble()}")
                    val referenceData = ObjectMapperProvider.get().convertValue(candleStick, JsonNode::class.java)
                    (referenceData as ObjectNode).set<DoubleNode>(
                        "tailLength",
                        DoubleNode(bottomTailLength.multiply(decimalScale).toDouble())
                    )
                    println("scaled tailLength: ${bottomTailLength.multiply(decimalScale)}")
                    return AnalyzeReport.MatchingReport(candleStick.symbol, OrderSide.LONG, referenceData)
                }
            }
        } else if (tailTop > bodyTop && tailBottom == bodyBottom) {
            println("top tail")
            val tailLength = tailTop - bodyTop
            val candleHammerRatio = tailLength / bodyLength
            println("calculated candleHammerRatio: $candleHammerRatio, tailLength: $tailLength, bodyLength: $bodyLength")
            if (candleHammerRatio > decimalHammerRatio && isPositivePnl(
                    bodyTop,
                    bodyTop.minus(tailLength.multiply(decimalScale))
                )
            ) {
                println("candleHammer: ${candleHammerRatio.toDouble()}, decimalHammer: ${decimalHammerRatio.toDouble()}")
                val referenceData = ObjectMapperProvider.get().convertValue(candleStick, JsonNode::class.java)
                (referenceData as ObjectNode).set<DoubleNode>(
                    "tailLength",
                    DoubleNode(tailLength.multiply(decimalScale).toDouble())
                )
                println("scaled tailLength: ${tailLength.multiply(decimalScale)}")
                return AnalyzeReport.MatchingReport(candleStick.symbol, OrderSide.SHORT, referenceData)
            }
        } else if (tailBottom < bodyBottom && tailTop == bodyTop) {
            println("bottom tail")
            val tailLength = bodyBottom - tailBottom
            val candleHammerRatio = tailLength / bodyLength
            println("calculated candleHammerRatio: $candleHammerRatio, tailLength: $tailLength, bodyLength: $bodyLength")
            if (candleHammerRatio > decimalHammerRatio && isPositivePnl(
                    bodyBottom,
                    bodyBottom.plus(tailLength.multiply(decimalScale))
                )
            ) {
                println("candleHammer: ${candleHammerRatio.toDouble()}, decimalHammer: ${decimalHammerRatio.toDouble()}")
                val referenceData = ObjectMapperProvider.get().convertValue(candleStick, JsonNode::class.java)
                (referenceData as ObjectNode).set<DoubleNode>(
                    "tailLength",
                    DoubleNode(tailLength.multiply(decimalScale).toDouble())
                )
                println("scaled tailLength: ${tailLength.multiply(decimalScale)}")
                return AnalyzeReport.MatchingReport(candleStick.symbol, OrderSide.LONG, referenceData)
            }
        } else {
            println("middle tail")
            val highTailLength = tailTop - bodyTop
            val lowTailLength = bodyBottom - tailBottom

            if (highTailLength > lowTailLength) {
                println("middle high tail, highTail: $highTailLength, lowTail: $lowTailLength, bodyTail: $bodyLength")
                val candleHammerRatio = highTailLength / bodyLength
                println("calculated candleHammerRatio: $candleHammerRatio")
                if (candleHammerRatio > decimalHammerRatio && isPositivePnl(
                        bodyTop,
                        bodyTop.minus(highTailLength.multiply(decimalScale))
                    )
                ) {
                    println("candleHammer: ${candleHammerRatio.toDouble()}, decimalHammer: ${decimalHammerRatio.toDouble()}")
                    val referenceData = ObjectMapperProvider.get().convertValue(candleStick, JsonNode::class.java)
                    (referenceData as ObjectNode).set<DoubleNode>(
                        "tailLength",
                        DoubleNode(highTailLength.multiply(decimalScale).toDouble())
                    )
                    println("scaled tailLength: ${highTailLength.multiply(decimalScale)}")
                    return AnalyzeReport.MatchingReport(candleStick.symbol, OrderSide.SHORT, referenceData)
                }
            } else {
                println("middle low tail, highTail: $highTailLength, lowTail: $lowTailLength, bodyTail: $bodyLength")
                val candleHammerRatio = highTailLength / bodyLength
                println("calculated candleHammerRatio: $candleHammerRatio")
                if (candleHammerRatio > decimalHammerRatio && isPositivePnl(
                        bodyBottom,
                        bodyBottom.plus(lowTailLength.multiply(decimalScale))
                    )
                ) {
                    println("candleHammer: ${candleHammerRatio.toDouble()}, decimalHammer: ${decimalHammerRatio.toDouble()}")
                    val referenceData = ObjectMapperProvider.get().convertValue(candleStick, JsonNode::class.java)
                    (referenceData as ObjectNode).set<DoubleNode>(
                        "tailLength",
                        DoubleNode(lowTailLength.multiply(decimalScale).toDouble())
                    )
                    println("scaled tailLength: ${lowTailLength.multiply(decimalScale)}")
                    return AnalyzeReport.MatchingReport(candleStick.symbol, OrderSide.LONG, referenceData)
                }
            }
        }

        return AnalyzeReport.NonMatchingReport
    }

    private fun isPositivePnl(open: BigDecimal, close: BigDecimal): Boolean {
        val pnl = (open - close).abs()
        val fee = (open + close).multiply(BigDecimal(TAKER_FEE_RATE))
        println("fee :${fee}, pnl :${pnl}")
        return pnl > fee
    }
}
