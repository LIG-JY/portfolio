package com.g2s.trading.history

import com.g2s.trading.order.OrderSide
import com.g2s.trading.order.OrderType
import com.g2s.trading.position.Position

sealed class History() {
    companion object {
        fun generateHistoryKey(position: Position): String {
            return "${position.positionKey}-${position.strategyKey}-${position.openTransactionTime}"
        }
    }

    // Open과 Close를 pairing 할 수 있도록 HistoryKey를 활용
    data class Open(
        val historyKey: String,
        val position: Position,
        val strategyKey: String,
        val openCondition: OpenCondition,
        val orderSide: OrderSide,
        val orderType: OrderType,
        val transactionTime: Long,
        val commission: Double,
        val afterBalance: Double,
    ) : History()

    data class Close(
        val historyKey: String,
        val position: Position,
        val strategyKey: String,
        val closeCondition: CloseCondition,
        val orderSide: OrderSide,
        val orderType: OrderType,
        val transactionTime: Long,
        val realizedPnL: Double,
        val commission: Double,
        val afterBalance: Double,
    ) : History()
}
