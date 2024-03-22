package com.g2s.trading.strategy

import com.fasterxml.jackson.databind.JsonNode
import com.g2s.trading.symbol.Symbol
import com.g2s.trading.account.Asset

data class StrategySpec (
    val strategyKey: String,
    val strategyType: String,
    val symbols: List<Symbol>,
    val asset: Asset,
    val allocatedRatio: Double,
    val op: JsonNode,
    val trigger: String,
    val status: StrategySpecServiceStatus
)
