package com.g2s.trading.strategy.simple

import com.g2s.trading.indicator.MarkPriceUseCase
import com.g2s.trading.event.PositionEvent
import com.g2s.trading.event.StrategyEvent
import com.g2s.trading.event.TradingEvent
import com.g2s.trading.account.AccountUseCase
import com.g2s.trading.history.CloseCondition
import com.g2s.trading.history.ConditionUseCase
import com.g2s.trading.lock.LockUsage
import com.g2s.trading.lock.LockUseCase
import com.g2s.trading.order.OrderSide
import com.g2s.trading.position.Position
import com.g2s.trading.position.PositionUseCase
import com.g2s.trading.strategy.StrategySpec
import com.g2s.trading.strategy.StrategySpecRepository
import org.slf4j.LoggerFactory
import org.springframework.context.event.EventListener
import org.springframework.stereotype.Component
import java.math.BigDecimal
import java.util.concurrent.ConcurrentHashMap

@Component
class SimpleCloseMan(
    private val lockUseCase: LockUseCase,
    private val positionUseCase: PositionUseCase,
    private val markPriceUseCase: MarkPriceUseCase,
    private val conditionUseCase: ConditionUseCase,
    private val accountUseCase: AccountUseCase,
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
    }

    @EventListener
    fun handleMarkPriceEvent(event: TradingEvent.MarkPriceRefreshEvent) {
        // find matching position
        val position = symbolPositionMap.asSequence()
            .map { it.value }
            .find { position -> position.symbol == event.source.symbol } ?: return
        // position must be synced
        if (!position.synced) {
            logger.debug("position not synced : ${position.symbol.value}")
            return
        }
        // if you find position, close it
        val acquired = lockUseCase.acquire(position.strategyKey, LockUsage.CLOSE)
        if (!acquired) {
            logger.debug("lock is not acquired. strategyKey : ${position.strategyKey}, symbol : ${position.symbol}")
            return
        }
        // check should close
        val availableBalance = accountUseCase.getAvailableBalance(position.asset)
        val entryPrice = BigDecimal(position.entryPrice)
        val lastPrice = BigDecimal(markPriceUseCase.getMarkPrice(position.symbol).price)
        val priceChange = (entryPrice - lastPrice).abs()
        val tailLength = BigDecimal(position.referenceData["tailLength"].asDouble())
        val spec = specs[position.strategyKey]!!
        val stopLossFactor = BigDecimal(spec.op["stopLossFactor"].asDouble())
        val takeProfitFactor = BigDecimal(spec.op["takeProfitFactor"].asDouble())
        var shouldClose = false
        when (position.orderSide) {
            OrderSide.LONG -> {
                // 손절
                if (lastPrice < entryPrice && priceChange > tailLength.multiply(stopLossFactor)) {
                    logger.debug(
                        "[롱 손절] entryPrice: $entryPrice | lastPrice: $lastPrice " +
                                "| 오픈 시 꼬리길이(tailLength): $tailLength" +
                                "| StopLossFactor: $stopLossFactor " +
                                "| StopLossFactor 적용한 꼬리길이 : ${tailLength.multiply(stopLossFactor)}" +
                                "| priceChange: $priceChange" +
                                "| specKey: ${spec.strategyKey}"
                    )
                    shouldClose = true
                    conditionUseCase.setCloseCondition(
                        position, CloseCondition.SimpleCondition(
                            tailLength = tailLength.toString(),
                            tailLengthWithStopLossFactor = tailLength.multiply(stopLossFactor).toString(),
                            entryPrice = entryPrice.toString(),
                            lastPrice = lastPrice.toString(),
                            beforeBalance = availableBalance.toDouble()
                        )
                    )
                    cntLoss++
                }
                // 익절
                else if (lastPrice > entryPrice && priceChange > tailLength.multiply(stopLossFactor)) {
                    logger.debug(
                        "[롱 익절] entryPrice: $entryPrice | lastPrice: $lastPrice" +
                                "| 오픈 시 꼬리길이(tailLength): $tailLength" +
                                "| 오픈 시 takeProfitFactor 반영된 꼬리길이: ${tailLength.multiply(takeProfitFactor)}" +
                                "| takeProfitFactor: $takeProfitFactor" +
                                "| specKey: ${spec.strategyKey}"
                    )
                    shouldClose = true
                    conditionUseCase.setCloseCondition(
                        position, CloseCondition.SimpleCondition(
                            tailLength = tailLength.toString(),
                            tailLengthWithStopLossFactor = tailLength.multiply(stopLossFactor).toString(),
                            entryPrice = entryPrice.toString(),
                            lastPrice = lastPrice.toString(),
                            beforeBalance = availableBalance.toDouble()
                        )
                    )
                    cntProfit++
                } else {
                    logger.debug(
                        "[롱 청산 X] entryPrice: $entryPrice | lastPrice: $lastPrice | 오픈 시 꼬리길이: $tailLength" +
                                "| takeProfitFactor: $takeProfitFactor" +
                                "| 오픈 시 takeProfitFactor 반영된 꼬리길이: ${tailLength.multiply(takeProfitFactor)}" +
                                "| StopLossFactor: $stopLossFactor" +
                                "| StopLossFactor 적용한 꼬리길이: ${tailLength.multiply(stopLossFactor)}"
                    )
                }
            }

            OrderSide.SHORT -> {
                // 손절
                if (lastPrice > entryPrice && priceChange > tailLength.multiply(stopLossFactor)) {
                    logger.debug(
                        "[숏 손절] entryPrice: $entryPrice | lastPrice: $lastPrice " +
                                "| 오픈 시 꼬리길이(tailLength): $tailLength" +
                                "| StopLossFactor: $stopLossFactor " +
                                "| StopLossFactor 적용한 꼬리길이 : ${tailLength.multiply(stopLossFactor)}" +
                                "| priceChange: $priceChange" +
                                "| specKey : ${spec.strategyKey}"
                    )
                    shouldClose = true
                    conditionUseCase.setCloseCondition(
                        position, CloseCondition.SimpleCondition(
                            tailLength = tailLength.toString(),
                            tailLengthWithStopLossFactor = tailLength.multiply(stopLossFactor).toString(),
                            entryPrice = entryPrice.toString(),
                            lastPrice = lastPrice.toString(),
                            beforeBalance = availableBalance.toDouble()
                        )
                    )
                    cntLoss++
                }
                // 익절
                else if (lastPrice < entryPrice && priceChange > tailLength.multiply(takeProfitFactor)) {
                    logger.debug(
                        "[숏 익절] entryPrice: $entryPrice | lastPrice: $lastPrice " +
                                "| 오픈 시 꼬리길이(tailLength): $tailLength" +
                                "| 오픈 시 takeProfitFactor 반영된 꼬리길이: ${tailLength.multiply(takeProfitFactor)}" +
                                "| takeProfitFactor: $takeProfitFactor" +
                                "| specKey: ${spec.strategyKey}"
                    )
                    shouldClose = true
                    conditionUseCase.setCloseCondition(
                        position, CloseCondition.SimpleCondition(
                            tailLength = tailLength.toString(),
                            tailLengthWithStopLossFactor = tailLength.multiply(stopLossFactor).toString(),
                            entryPrice = entryPrice.toString(),
                            lastPrice = lastPrice.toString(),
                            beforeBalance = availableBalance.toDouble()
                        )
                    )
                    cntProfit++
                } else {
                    logger.debug(
                        "[숏 청산 X] entryPrice: $entryPrice | lastPrice: $lastPrice | 오픈 시 꼬리길이: $tailLength" +
                                "| takeProfitFactor: $takeProfitFactor" +
                                "| 오픈 시 takeProfitFactor 반영된 꼬리길이: ${tailLength.multiply(takeProfitFactor)}" +
                                "| StopLossFactor: $stopLossFactor" +
                                "| StopLossFactor 적용한 꼬리길이: ${tailLength.multiply(stopLossFactor)}"
                    )
                }
            }
        }
        // close position
        if (shouldClose) {
            logger.debug("익절 count: $cntProfit, 손절 count: $cntLoss")
            symbolPositionMap.remove(position.positionKey)
            positionUseCase.closePosition(position, spec)
        }
        // 릴리즈
        lockUseCase.release(position.strategyKey, LockUsage.CLOSE)
    }
}
