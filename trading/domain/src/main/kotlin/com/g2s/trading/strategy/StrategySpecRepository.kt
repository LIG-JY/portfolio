package com.g2s.trading.strategy

import org.springframework.stereotype.Repository

@Repository
interface StrategySpecRepository {
    fun findStrategySpecByKey(strategyKey: String): StrategySpec?
    fun findAllServiceStrategySpec(): List<StrategySpec>
    fun findAllServiceStrategySpecByType(type: String): List<StrategySpec>
    fun updateSpec(strategySpec: StrategySpec): StrategySpec
}
