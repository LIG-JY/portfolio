package com.g2s.trading.exchange

import com.g2s.trading.common.ObjectMapperProvider
import com.g2s.trading.position.Position
import org.springframework.stereotype.Component
import java.util.concurrent.ConcurrentHashMap

@Component
class BinanceOrderInfoTracker {

    private val om = ObjectMapperProvider.get()
    private val openedPositionClientIdMap = ConcurrentHashMap<Position.PositionKey, String>()
    private val closedPositionClientIdMap = ConcurrentHashMap<Position.PositionKey, String>()
    private val openedPositionTradeTimeMap = ConcurrentHashMap<Position.PositionKey, Long>()
    private val closedPositionTradeTimeMap = ConcurrentHashMap<Position.PositionKey, Long>()

    // 주문을 식별할 수 있는 프로퍼티 : clientOrderId, orderId, tradeTime
    fun setOpenOrderInfo(position: Position, orderData: String) {
        val jsonNode = om.readTree(orderData)
        openedPositionClientIdMap.computeIfAbsent(position.positionKey) { _ ->
            jsonNode.get("clientOrderId").asText()
        }
        openedPositionTradeTimeMap.computeIfAbsent(position.positionKey) {_ ->
            jsonNode.get("updateTime").asLong()
        }
    }

    fun setCloseOrderInfo(position: Position, orderData: String) {
        val jsonNode = om.readTree(orderData)
        closedPositionClientIdMap.computeIfAbsent(position.positionKey) { _ ->
            jsonNode.get("clientOrderId").asText()
        }
        closedPositionTradeTimeMap.computeIfAbsent(position.positionKey) {_ ->
            jsonNode.get("updateTime").asLong()
        }
    }

    fun getOpenClientId(position: Position): String {
        return openedPositionClientIdMap[position.positionKey]!!
    }

    fun getCloseClientId(position: Position): String {
        return closedPositionClientIdMap[position.positionKey]!!
    }
    fun getOpenedPositionTransactionTime(position: Position): Long {
        return openedPositionTradeTimeMap[position.positionKey]!!
    }

    fun getClosedPositionTransactionTime(position: Position): Long {
        return closedPositionTradeTimeMap[position.positionKey]!!
    }

    fun removeOpenClientId(position: Position) {
        openedPositionClientIdMap.remove(position.positionKey)
    }

    fun removeCloseClientId(position: Position) {
        closedPositionClientIdMap.remove(position.positionKey)
    }
    fun removeOpenedPositionTransactionTime(position: Position) {
        openedPositionTradeTimeMap.remove(position.positionKey)
    }
    fun removeClosedPositionTransactionTime(position: Position) {
        closedPositionTradeTimeMap.remove(position.positionKey)
    }
}
