package com.g2s.trading.strategy

import com.g2s.trading.indicator.MarkPrice.MarkPriceUseCase
import com.g2s.trading.event.PositionEvent
import com.g2s.trading.event.StrategyEvent
import com.g2s.trading.event.TradingEvent
import com.g2s.trading.lock.LockUsage
import com.g2s.trading.lock.LockUseCase
import com.g2s.trading.order.OrderSide
import com.g2s.trading.position.Position
import com.g2s.trading.position.PositionUseCase
import org.slf4j.LoggerFactory
import org.springframework.context.event.EventListener
import org.springframework.stereotype.Component
import java.math.BigDecimal
import java.util.concurrent.ConcurrentHashMap

@Component
class NewSimpleCloseMan(
    private val lockUseCase: LockUseCase,
    private val positionUseCase: PositionUseCase,
    private val markPriceUseCase: MarkPriceUseCase,
    private val strategySpecRepository: StrategySpecRepository
) {
    private val logger = LoggerFactory.getLogger(this.javaClass)

    companion object {
        private const val TYPE = "simple"
    }

    var cntProfit = 0
    var cntLoss = 0

    // simple TYPE의 strategySpec들을 관리
    private val specs: ConcurrentHashMap<String, StrategySpec> =
        strategySpecRepository.findAllServiceStrategySpecByType(TYPE)
            .associateBy { it.strategyKey }
            .let { ConcurrentHashMap(it) }

    // 열린 포지션을 관리
    private val symbolPositionMap: ConcurrentHashMap<Position.PositionKey, Position> =
        positionUseCase.getAllPositions()
            .filter { position -> specs.keys.contains(position.strategyKey) }
            .associateBy { it.positionKey }
            .let { ConcurrentHashMap(it) }

    @EventListener
    fun handleStartStrategyEvent(event: StrategyEvent.StartStrategyEvent) {
        val spec = event.source
        if (spec.strategyType != TYPE) return
        specs.putIfAbsent(spec.strategyKey, spec)
    }

    @EventListener
    fun handleUpdateStrategyEvent(event: StrategyEvent.UpdateStrategyEvent) {
        val spec = event.source
        if (spec.strategyType != TYPE) return
        specs.replace(spec.strategyKey, spec)
    }

    @EventListener
    fun handleStopStrategyEvent(event: StrategyEvent.StopStrategyEvent) {
        val spec = event.source
        if (spec.strategyType != TYPE) return
        specs.remove(spec.strategyKey)
    }

    @EventListener
    fun handlePositionSyncedEvent(event: PositionEvent.PositionSyncedEvent) {
        val newPosition = event.source
        if (!specs.keys.contains(newPosition.strategyKey)) return
        symbolPositionMap.compute(newPosition.positionKey) { _, _ ->
            newPosition
        }
        logger.debug("handlePositionSyncedEvent : position strategy key = ${newPosition.strategyKey}")
    }

    @EventListener
    fun handleMarkPriceEvent(event: TradingEvent.MarkPriceRefreshEvent) {
//        logger.debug("handleMarkPriceEvent: ${event.source.symbol}")
        // find matching position
        val position = symbolPositionMap.asSequence()
            .map { it.value }
            .find { position -> position.symbol == event.source.symbol } ?: return
        // position must be synced
        if (!position.synced) {
            return
        }
        // if you find position, close it
        val acquired = lockUseCase.acquire(position.strategyKey, LockUsage.CLOSE)
        if (!acquired) return
        // check should close
        val entryPrice = BigDecimal(position.entryPrice)
        val lastPrice = BigDecimal(markPriceUseCase.getMarkPrice(position.symbol).price)
        val spec = specs[position.strategyKey]!!
        val stopLossFactor = BigDecimal(spec.op["stopLossFactor"].asDouble())
        var shouldClose = false
        when (position.orderSide) {
            OrderSide.LONG -> {
                // 손절
                val stickLength = BigDecimal(position.referenceData["high"].asDouble()).minus(BigDecimal(position.referenceData["low"].asDouble()))
                if (stickLength.multiply(stopLossFactor) > entryPrice.minus(lastPrice)) {
                    logger.debug(
                        "롱 손절: lastPrice: $lastPrice, 오픈시 고가 - 저가: $stickLength" +
                                ", StopLossFactor 반영 후 고가 - 저가: ${stickLength.multiply(stopLossFactor)}"
                    )
                    shouldClose = true
                    cntLoss++
                }
                // 익절
                if (lastPrice > entryPrice.plus(BigDecimal(position.referenceData["tailLength"].asDouble()))) {
                    logger.debug(
                        "롱 익절: lastPrice: $lastPrice, entryPrice: $entryPrice, 오픈시 꼬리 길이: ${position.referenceData["tailLength"].asDouble()}"
                    )
                    shouldClose = true
                    cntProfit++
                }
            }

            OrderSide.SHORT -> {
                // 손절
                val stickLength = BigDecimal(position.referenceData["high"].asDouble()).minus(BigDecimal(position.referenceData["low"].asDouble()))
                if (stickLength.multiply(stopLossFactor) < lastPrice.minus(entryPrice)) {
                    logger.debug(
                        "숏 손절: lastPrice: $lastPrice, 오픈시 고가 - 저가: $stickLength" +
                                ", StopLossFactor 반영 후 고가 - 저가: ${stickLength.multiply(stopLossFactor)}"
                    )
                    shouldClose = true
                    cntLoss++
                }
                // 익절
                if (lastPrice < entryPrice.minus(BigDecimal(position.referenceData["tailLength"].asDouble()))) {
                    logger.debug(
                        "숏 익절: lastPrice: $lastPrice, entryPrice: $entryPrice, 오픈시 꼬리 길이: ${position.referenceData["tailLength"].asDouble()}"
                    )
                    shouldClose = true
                    cntProfit++
                }
            }
        }
        // close position
        if (shouldClose) {
            logger.debug("포지션 청산: $position")
            logger.debug("익절: $cntProfit, 손절: $cntLoss")
            symbolPositionMap.remove(position.positionKey)
            logger.debug("position deleted from symbolPositionMap: ${position.strategyKey}")
            positionUseCase.closePosition(position, spec)
        }
        logger.debug("${position.symbol} shouldClose: $shouldClose")
        logger.debug("symbolPositionMap의 크기 : " + symbolPositionMap.size.toString())
        // 릴리즈
        lockUseCase.release(position.strategyKey, LockUsage.CLOSE)
    }
}
