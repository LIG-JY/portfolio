package com.g2s.trading.dreamandhope

import com.g2s.trading.event.EventUseCase
import com.g2s.trading.event.StrategyEvent
import com.g2s.trading.strategy.StrategySpec
import com.g2s.trading.strategy.StrategySpecRepository
import com.g2s.trading.strategy.StrategySpecServiceStatus
import org.slf4j.LoggerFactory
import org.springframework.stereotype.Service

@Service
class DreamAndHope(
    private val mongoStrategySpecRepository: StrategySpecRepository,
    private val eventUseCase: EventUseCase
) {

    private val logger = LoggerFactory.getLogger(this.javaClass)

    fun start(strategyKey: String) {
        try {
            logger.debug("start: $strategyKey")
            // strategyKey로 DB에서 스펙조회
            mongoStrategySpecRepository.findStrategySpecByKey(strategyKey)
                ?.let { spec ->
                    if (spec.status == StrategySpecServiceStatus.SERVICE) {
                        eventUseCase.publishEvent(StrategyEvent.StartStrategyEvent(spec))
                    }
                }
        } catch (e: Exception) {
            throw DreamAndHopeErrors.START_STRATEGY_ERROR.error("Failed to start strategy: $strategyKey", e)
        }
    }

    fun stop(strategyKey: String) {
        try {
            logger.debug("stop: $strategyKey")
            mongoStrategySpecRepository.findStrategySpecByKey(strategyKey)
                ?.let { spec ->
                    if (spec.status == StrategySpecServiceStatus.STOP) {
                        eventUseCase.publishEvent(StrategyEvent.StartStrategyEvent(spec))
                    }
                }
        } catch (e: Exception) {
            throw DreamAndHopeErrors.STOP_STRATEGY_ERROR.error("Failed to stop strategy: $strategyKey", e)
        }
    }

    fun update(strategySpec: StrategySpec) {
        try {
            logger.debug("update: ${strategySpec.strategyKey}")
            mongoStrategySpecRepository.updateSpec(strategySpec)
                .let { spec -> eventUseCase.publishEvent(StrategyEvent.UpdateStrategyEvent(spec)) }
        } catch (e: Exception) {
            throw DreamAndHopeErrors.UPDATE_STRATEGY_ERROR.error(
                "Failed to update strategy: ${strategySpec.strategyKey}",
                e
            )
        }
    }
}
