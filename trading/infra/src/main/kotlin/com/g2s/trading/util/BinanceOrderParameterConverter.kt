package com.g2s.trading.util

import com.g2s.trading.order.OrderSide
import com.g2s.trading.position.Position
import com.g2s.trading.position.PositionMode
import com.g2s.trading.position.PositionSide
import kotlin.math.absoluteValue

object BinanceOrderParameterConverter {
    fun toBinanceOpenPositionParam(
        position: Position,
        positionMode: PositionMode,
        positionSide: PositionSide
    ): LinkedHashMap<String, Any> {
        val parameters = LinkedHashMap<String, Any>()
        parameters["symbol"] = position.symbol.value
        parameters["side"] = when (position.orderSide) {
            OrderSide.LONG -> "BUY"
            OrderSide.SHORT -> "SELL"
        }
        parameters["type"] = position.orderType.toString()
        parameters["quantity"] = position.positionAmt.toString()
        parameters["timeStamp"] = System.currentTimeMillis()
        parameters["positionMode"] = positionMode.toString()
        parameters["positionSide"] = positionSide.toString()
        return parameters
    }

    fun toBinanceClosePositionParam(
        position: Position,
        positionMode: PositionMode,
        positionSide: PositionSide
    ): LinkedHashMap<String, Any> {
        val parameters = LinkedHashMap<String, Any>()
        parameters["symbol"] = position.symbol.value
        parameters["side"] = when (position.orderSide) {
            OrderSide.LONG -> "SELL"
            OrderSide.SHORT -> "BUY"
        }
        parameters["type"] = position.orderType.toString()
        parameters["quantity"] = position.positionAmt.absoluteValue.toString()
        parameters["timeStamp"] = System.currentTimeMillis()
        parameters["positionMode"] = positionMode.toString()
        parameters["positionSide"] = positionSide.toString()
        return parameters
    }
}
