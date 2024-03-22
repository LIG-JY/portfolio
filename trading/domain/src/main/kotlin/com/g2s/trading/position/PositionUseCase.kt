package com.g2s.trading.position

import com.g2s.trading.event.EventUseCase
import com.g2s.trading.event.PositionEvent
import com.g2s.trading.account.AccountUseCase
import com.g2s.trading.exceptions.OrderFailException
import com.g2s.trading.exchange.Exchange
import com.g2s.trading.strategy.StrategySpec
import com.g2s.trading.symbol.Symbol
import org.slf4j.LoggerFactory
import org.springframework.stereotype.Service
import java.util.concurrent.ConcurrentHashMap

@Service
class PositionUseCase(
    private val exchangeImpl: Exchange,
    private val accountUseCase: AccountUseCase,
    private val eventUseCase: EventUseCase,
    private val positionRepository: PositionRepository
) {
    private val logger = LoggerFactory.getLogger(this.javaClass)
    private val positionMap = ConcurrentHashMap<Position.PositionKey, Position>()

    init {
        loadPositions()
    }

    private fun loadPositions() {
        // db 조회
        val positions = positionRepository.findAllPositions()
        logger.debug("load positions: ${positions.size}")
        // map 저장
        positions.forEach { position ->
            positionMap[position.positionKey] = position
        }
    }

    fun openPosition(position: Position, spec: StrategySpec) {
        logger.debug("포지션 오픈 - symbol:${position.symbol}")
        try {

            val currentValue = positionMap.computeIfAbsent(position.positionKey) { _ ->
                logger.debug("openPostion 실행 전 map size = ${positionMap.size}\n")
                accountUseCase.setUnSynced()
                positionRepository.savePosition(position)
                position
            }

            if (currentValue == position) {
                exchangeImpl.openPosition(position)
            }
        } catch (e: OrderFailException) {
            logger.debug(e.message)
            positionMap.remove(position.positionKey)
            positionRepository.deletePosition(position)
            accountUseCase.syncAccount()
        }
        logger.debug("openPostion 실행 후 map size = ${positionMap.size}\n")
    }

    fun refreshPosition(positionRefreshData: PositionRefreshData) {
        logger.debug("refreshPosition")
        positionMap.values.find {
            // TOOD :PostionRefreshData와 postion key와 매핑
            it.symbol == positionRefreshData.symbol
        }?.let { old ->
            val updated = Position.update(old, positionRefreshData)
            if (updated.positionAmt != 0.0) {
                logger.debug("position is opened because positionAmt is  ${updated.positionAmt}")
                positionRepository.updatePosition(updated)
                positionMap.replace(updated.positionKey, updated)
            }
        }
    }

    fun syncPosition(symbol: Symbol) {
        logger.debug("syncPosition")
        positionMap.values.find {
            it.symbol == symbol
        }?.let { old ->
            val synced = Position.sync(old)
            positionRepository.updatePosition(synced)
            logger.debug("position synced in DB\n")
            positionMap.replace(synced.positionKey, synced)
            logger.debug("position synced in map\n")
            eventUseCase.publishEvent(PositionEvent.PositionSyncedEvent(synced))
        }
    }

    fun closePosition(position: Position, spec: StrategySpec) {
        val originalPosition = position.copy()

        try {
            accountUseCase.setUnSynced()
            positionRepository.deletePosition(position)
            positionMap.remove(position.positionKey)
            exchangeImpl.closePosition(position)
        } catch (e: OrderFailException) {
            logger.debug(e.message)
            positionMap[originalPosition.positionKey] = originalPosition
            positionRepository.savePosition(originalPosition)
            accountUseCase.syncAccount()
            return
        }
        logger.debug("$position closed\n")
    }

    fun getAllUsedSymbols(): Set<Symbol> {
        return positionMap.values.map { position -> position.symbol }.toSet()
    }

    fun getAllPositions(): List<Position> {
        return positionMap.values.toList()
    }
}
