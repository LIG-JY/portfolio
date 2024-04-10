package com.g2s.trading.exchange

import com.binance.connector.futures.client.impl.UMFuturesClientImpl
import com.g2s.trading.common.ObjectMapperProvider
import com.g2s.trading.indicator.CandleStick
import com.g2s.trading.indicator.Interval
import com.g2s.trading.symbol.Symbol
import org.junit.jupiter.api.Disabled
import org.junit.jupiter.api.Test
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.boot.test.context.SpringBootTest
import java.math.BigDecimal
import java.text.SimpleDateFormat
import java.util.*

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
}
