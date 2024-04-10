package com.g2s.trading.exchange

import com.g2s.trading.history.Commission
import com.g2s.trading.event.EventUseCase
import com.g2s.trading.history.RealizedProfit
import com.g2s.trading.event.TradingEvent
import org.springframework.stereotype.Component
import java.util.concurrent.ConcurrentHashMap

// 수수료와 실현 손익을 추적하기 위함
@Component
class BinanceCommissionAndRealizedProfitTracker(
    val eventUseCase: EventUseCase
) {

    private val clientIdRealizedProfitMap = ConcurrentHashMap<String, Double>()
    private val clientIdCommissionMap = ConcurrentHashMap<String, Double>()

    fun updateCommission(clientId: String, commission: Double) {
        clientIdCommissionMap.compute(clientId) { _, currentCommission ->
            if (currentCommission == null) {
                commission
            } else {
                currentCommission + commission
            }
        }
    }

    fun publishAccumulatedCommission(clientId: String) {
        val accumulatedCommission = clientIdCommissionMap[clientId]!!
        eventUseCase.publishEvent(
            TradingEvent.CommissionEvent(
                Commission(accumulatedCommission, clientId)
            )
        )
    }

    fun updateRealizedProfit(clientId: String, realizedProfit: Double) {
        clientIdRealizedProfitMap.compute(clientId) { _: String, rp: Double? ->
            if (rp == null) {
                realizedProfit
            } else {
                rp + realizedProfit
            }
        }
    }

    fun publishRealizedProfitAndCommissionEvent(clientId: String) {
        val realizedProfit = clientIdRealizedProfitMap[clientId]!!
        val accumulatedCommission = clientIdCommissionMap[clientId]!!
        eventUseCase.publishEvent(
            TradingEvent.RealizedProfitAndCommissionEvent(
                Pair(Commission(accumulatedCommission, clientId), RealizedProfit(realizedProfit, clientId))
            )
        )
        clientIdRealizedProfitMap.remove(clientId)
        clientIdCommissionMap.remove(clientId)
    }
}
