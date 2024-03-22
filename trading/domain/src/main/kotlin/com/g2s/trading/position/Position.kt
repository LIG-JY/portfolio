package com.g2s.trading.position

import com.fasterxml.jackson.databind.JsonNode
import com.g2s.trading.order.OrderSide
import com.g2s.trading.order.OrderType
import com.g2s.trading.symbol.Symbol

data class Position(
    val positionKey: PositionKey,
    val strategyKey: String,
    val symbol: Symbol,
    val entryPrice: Double,
    val positionAmt: Double,
    val orderSide: OrderSide,
    val orderType: OrderType,
    val referenceData: JsonNode,
    val synced: Boolean = false // close시 확인
) {

    data class PositionKey(val symbol: Symbol, val orderSide: OrderSide)
    companion object {
        fun update(old: Position, refreshData: PositionRefreshData): Position {
            return old.copy(
                entryPrice = refreshData.entryPrice,
                positionAmt = refreshData.positionAmt,
            )
        }

        fun sync(old: Position): Position {
            return old.copy(synced = true)
        }
    }
}

