package com.g2s.trading.history

import com.g2s.trading.event.TradingEvent
import com.g2s.trading.account.AccountUseCase
import com.g2s.trading.exchange.Exchange
import com.g2s.trading.position.Position
import com.g2s.trading.strategy.StrategySpecRepository
import org.slf4j.LoggerFactory
import org.springframework.context.event.EventListener
import org.springframework.stereotype.Service
import java.util.concurrent.ConcurrentHashMap

@Service
class HistoryUseCase(
    private val conditionUseCase: ConditionUseCase,
    private val accountUseCase: AccountUseCase,
    private val strategySpecRepository: StrategySpecRepository,
    private val historyRepository: HistoryRepository,
    private val exchangeImpl: Exchange
) {
    private val strategyHistoryToggleMap = ConcurrentHashMap<String, Boolean>()
    private val clientIdSyncedPositionMap = ConcurrentHashMap<String, Position>()
    private val clientIdClosedPositions = ConcurrentHashMap<String, Position>()
    private val logger = LoggerFactory.getLogger(this.javaClass)

    init {
        loadStrategySpecKey()
        strategyHistoryToggleMap.compute("manual") { _, _ ->
            true
        }
    }

    fun setSyncedPosition(position: Position) {
        val clientId = exchangeImpl.getClientIdAtOpen(position)
        clientIdSyncedPositionMap.compute(clientId) { _, _ ->
            position
        }
    }

    fun setClosedPosition(position: Position) {
        val clientId = exchangeImpl.getClientIdAtClose(position)
        clientIdClosedPositions.compute(clientId) { _, _ ->
            position
        }
    }

    fun removeClosedPosition(position: Position) {
        clientIdClosedPositions.remove(position.strategyKey)
    }

    @EventListener
    fun recordOpenHistory(event: TradingEvent.CommissionEvent) {
        val position = clientIdSyncedPositionMap[event.source.clientId] ?: return

        if (strategyHistoryToggleMap[position.strategyKey]!!) {
            val openTransactionTime = exchangeImpl.getPositionOpeningTime(position)

            val openHistory = History.Open(
                historyKey = History.generateHistoryKey(position),
                position = position,
                strategyKey = position.strategyKey,
                openCondition = conditionUseCase.getOpenCondition(position),
                orderSide = position.orderSide,
                orderType = position.orderType,
                transactionTime = openTransactionTime,
                commission = event.source.commission,
                afterBalance = accountUseCase.getBalance(position.asset, openTransactionTime),
            )
            conditionUseCase.removeOpenCondition(position)
            clientIdSyncedPositionMap.remove(position.strategyKey)
            logger.debug(openHistory.toString())
            historyRepository.saveHistory(openHistory)
        }
    }

    @EventListener
    fun recordCloseHistory(event: TradingEvent.RealizedProfitAndCommissionEvent) {
        val position = clientIdClosedPositions[event.source.first.clientId] ?: return

        if (strategyHistoryToggleMap[position.strategyKey]!!) {
            val closeTransactionTime = exchangeImpl.getPositionClosingTime(position)

            val closeHistory = History.Close(
                historyKey = History.generateHistoryKey(position),
                position = position,
                strategyKey = position.strategyKey,
                closeCondition = conditionUseCase.getCloseCondition(position),
                orderSide = position.orderSide,
                orderType = position.orderType,
                transactionTime = closeTransactionTime,
                realizedPnL = event.source.second.realizedProfit,
                commission = event.source.first.commission,
                afterBalance = accountUseCase.getBalance(position.asset, closeTransactionTime),
            )
            conditionUseCase.removeCloseCondition(position)
            clientIdClosedPositions.remove(position.strategyKey)
            logger.debug(closeHistory.toString())
            historyRepository.saveHistory(closeHistory)
        }
    }

    fun turnOnHistoryFeature(strategyKey: String) {
        strategyHistoryToggleMap.compute(strategyKey) { _, _ ->
            true
        }
    }

    fun turnOnAllHistoryFeature() {
        for (entry in strategyHistoryToggleMap) {
            entry.setValue(true)
        }
    }

    fun turnOffHistoryFeature(strategyKey: String) {
        strategyHistoryToggleMap.compute(strategyKey) { _, _ ->
            false
        }
    }

    fun turnOffAllHistoryFeature() {
        for (entry in strategyHistoryToggleMap) {
            entry.setValue(false)
        }
    }

    private fun loadStrategySpecKey() {
        strategySpecRepository.findAllServiceStrategySpec().forEach {
            strategyHistoryToggleMap.computeIfAbsent(it.strategyKey) {
                true
            }
        }
    }
}
