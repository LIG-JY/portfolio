package com.g2s.trading.matchingReport

import com.fasterxml.jackson.databind.JsonNode
import com.g2s.trading.history.OpenCondition
import com.g2s.trading.order.OrderSide
import com.g2s.trading.symbol.Symbol

sealed class AnalyzeReport {
    data class MatchingReport(
        val symbol: Symbol,
        val orderSide: OrderSide,
        val openCondition: OpenCondition,
        val referenceData : JsonNode
    ) : AnalyzeReport()

    object NonMatchingReport : AnalyzeReport()
}
