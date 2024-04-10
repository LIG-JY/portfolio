package com.g2s.trading.exchange

import com.binance.connector.futures.client.impl.UMFuturesClientImpl
import com.binance.connector.futures.client.exceptions.BinanceClientException
import com.fasterxml.jackson.databind.JsonNode
import com.fasterxml.jackson.databind.node.ArrayNode
import com.fasterxml.jackson.module.kotlin.readValue
import com.g2s.trading.indicator.MarkPrice
import com.g2s.trading.account.Account
import com.g2s.trading.account.Asset
import com.g2s.trading.account.AssetWallet
import com.g2s.trading.common.ObjectMapperProvider
import com.g2s.trading.exceptions.OrderFailException
import com.g2s.trading.position.Position
import com.g2s.trading.position.PositionMode
import com.g2s.trading.position.PositionSide
import com.g2s.trading.symbol.Symbol
import com.g2s.trading.util.BinanceOrderParameterConverter
import org.slf4j.LoggerFactory
import org.springframework.stereotype.Component


@Component
class RestApiExchangeImpl(
    private val binanceClient: UMFuturesClientImpl,
    private val binanceOrderInfoTracker: BinanceOrderInfoTracker,
) : Exchange {
    private val logger = LoggerFactory.getLogger(this.javaClass)
    private val om = ObjectMapperProvider.get()

    private var positionMode = PositionMode.ONE_WAY_MODE
    private var positionSide = PositionSide.BOTH

    // Exchange MetaData
    private var exchangeInfo: JsonNode = om.readTree(binanceClient.market().exchangeInfo())

    override fun setPositionMode(positionMode: PositionMode) {
        this.positionMode = positionMode
        if (positionMode == PositionMode.ONE_WAY_MODE) {
            this.positionSide = PositionSide.BOTH
        }
    }

    override fun getAccount(): Account {
        val parameters: LinkedHashMap<String, Any> = linkedMapOf(
            "timestamp" to System.currentTimeMillis().toString()
        )
        val bodyString = binanceClient.account().accountInformation(parameters)
        val bodyJson = om.readTree(bodyString)

        val assetWallets = (bodyJson.get("assets") as ArrayNode)
            .filter { jsonNode ->
                Asset.entries.map { it.name }.contains(jsonNode.get("asset").textValue())
            }.map {
                om.convertValue(it, AssetWallet::class.java)
            }

        return Account(assetWallets)
    }

    override fun getAccount(timeStamp: Long): Account {
        val parameters: LinkedHashMap<String, Any> = linkedMapOf(
            "timestamp" to timeStamp.toString()
        )
        val bodyString = binanceClient.account().accountInformation(parameters)
        val bodyJson = om.readTree(bodyString)

        val assetWallets = (bodyJson.get("assets") as ArrayNode)
            .filter { jsonNode ->
                Asset.entries.map { it.name }.contains(jsonNode.get("asset").textValue())
            }.map {
                om.convertValue(it, AssetWallet::class.java)
            }

        return Account(assetWallets)
    }

    /**
    orderResult :
    {
    "clientOrderId": "testOrder",
    "cumQty": "0",
    "cumQuote": "0",
    "executedQty": "0",
    "orderId": 22542179,
    "avgPrice": "0.00000",
    "origQty": "10",
    "price": "0",
    "reduceOnly": false,
    "side": "BUY",
    "positionSide": "SHORT",
    "status": "NEW",
    "stopPrice": "9300",        // please ignore when order type is TRAILING_STOP_MARKET
    "closePosition": false,   // if Close-All
    "symbol": "BTCUSDT",
    "timeInForce": "GTD",
    "type": "TRAILING_STOP_MARKET",
    "origType": "TRAILING_STOP_MARKET",
    "activatePrice": "9020",    // activation price, only return with TRAILING_STOP_MARKET order
    "priceRate": "0.3",         // callback rate, only return with TRAILING_STOP_MARKET order
    "updateTime": 1566818724722,
    "workingType": "CONTRACT_PRICE",
    "priceProtect": false,      // if conditional order trigger is protected
    "priceMatch": "NONE",              //price match mode
    "selfTradePreventionMode": "NONE", //self trading preventation mode
    "goodTillDate": 1693207680000      //order pre-set auot cancel time for TIF GTD order
    }
     */
    override fun closePosition(position: Position) {
        val params = BinanceOrderParameterConverter.toBinanceClosePositionParam(position, positionMode, positionSide)
        try {
            val orderResult = sendOrder(params)
            binanceOrderInfoTracker.setCloseOrderInfo(position, orderResult)
        } catch (e: OrderFailException) {
            throw e
        }
    }

    override fun openPosition(position: Position) {
        val params = BinanceOrderParameterConverter.toBinanceOpenPositionParam(position, positionMode, positionSide)
        try {
            val orderResult = sendOrder(params)
            binanceOrderInfoTracker.setOpenOrderInfo(position, orderResult)
        } catch (e: OrderFailException) {
            throw e
        }
    }

    override fun getPosition(symbol: Symbol): Position {
        val parameters: LinkedHashMap<String, Any> = linkedMapOf(
            "symbol" to symbol.value,
            "timestamp" to System.currentTimeMillis().toString()
        )
        val jsonString = binanceClient.account().positionInformation(parameters)

        val position = om.readValue<List<Position>>(jsonString).first()

        return position
    }

    private fun sendOrder(params: LinkedHashMap<String, Any>): String {
        try {
            val response: String = binanceClient.account().newOrder(params)
            logger.debug(response)
            return response
        } catch (e: BinanceClientException) {
            logger.warn("$params\n" + e.errMsg)
            throw OrderFailException("선물 주문 실패")
        }
    }

    override fun getMarkPrice(symbol: Symbol): MarkPrice {
        val parameters = LinkedHashMap<String, Any>()
        parameters["symbol"] = symbol.value
        val jsonNode = om.readTree(binanceClient.market().tickerSymbol(parameters))

        return MarkPrice(
            symbol = symbol,
            price = jsonNode.get("price").asDouble()
        )
    }

    override fun getQuantityPrecision(symbol: Symbol): Int {
        return exchangeInfo.get("symbols")
            .find { node -> node.get("symbol").asText() == symbol.value }!!.get("quantityPrecision").asInt()
    }

    // 시장가 주문일 때만 적용
    // 시장가 주문이 아닐 때 filterType : LOT_SIZE
    override fun getMinQty(symbol: Symbol): Double {
        return exchangeInfo.get("symbols")
            .find { node -> node.get("symbol").asText() == symbol.value }!!.get("filters")
            .find { node -> node.get("filterType").asText() == "MARKET_LOT_SIZE" }!!.get("minQty").asDouble()
    }

    override fun getMinNotionalValue(symbol: Symbol): Double {
        return exchangeInfo.get("symbols")
            .find { node -> node.get("symbol").asText() == symbol.value }!!.get("filters")
            .find { node -> node.get("filterType").asText() == "MIN_NOTIONAL" }!!.get("notional").asDouble()
    }

    override fun getPositionOpeningTime(position: Position): Long {
        val positionOpeningTime = binanceOrderInfoTracker.getOpenedPositionTransactionTime(position)
        binanceOrderInfoTracker.removeOpenedPositionTransactionTime(position)

        return positionOpeningTime
    }

    override fun getPositionClosingTime(position: Position): Long {
        val positionClosingTime = binanceOrderInfoTracker.getClosedPositionTransactionTime(position)
        binanceOrderInfoTracker.removeClosedPositionTransactionTime(position)

        return positionClosingTime
    }

    override fun getClientIdAtOpen(position: Position): String {
        val clientId = binanceOrderInfoTracker.getOpenClientId(position)
        binanceOrderInfoTracker.removeOpenClientId(position)

        return clientId
    }

    override fun getClientIdAtClose(position: Position): String {
        val clientId = binanceOrderInfoTracker.getCloseClientId(position)
        binanceOrderInfoTracker.removeCloseClientId(position)

        return clientId
    }
}
