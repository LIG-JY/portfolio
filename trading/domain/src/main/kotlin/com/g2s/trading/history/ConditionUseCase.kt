package com.g2s.trading.history

import com.g2s.trading.position.Position
import org.springframework.stereotype.Service
import java.util.concurrent.ConcurrentHashMap

@Service
class ConditionUseCase {

    private val positionOpenConditionMap = ConcurrentHashMap<Position.PositionKey, OpenCondition>()
    private val positionCloseConditionMap = ConcurrentHashMap<Position.PositionKey, CloseCondition>()

    fun setOpenCondition(position: Position, condition: OpenCondition) {
        positionOpenConditionMap.computeIfAbsent(position.positionKey) { _ ->
            condition
        }
    }

    fun setCloseCondition(position: Position, condition: CloseCondition) {
        positionCloseConditionMap.computeIfAbsent(position.positionKey) { _ ->
            condition
        }
    }

    fun getOpenCondition(position: Position): OpenCondition {
        return positionOpenConditionMap[position.positionKey]!!
    }

    fun getCloseCondition(position: Position): CloseCondition {
        return positionCloseConditionMap[position.positionKey]!!
    }

    fun removeOpenCondition(position: Position) {
        positionOpenConditionMap.remove(position.positionKey)
    }

    fun removeCloseCondition(position: Position) {
        positionCloseConditionMap.remove(position.positionKey)
    }
}
