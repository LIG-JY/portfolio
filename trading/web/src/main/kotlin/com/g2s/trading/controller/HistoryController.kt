package com.g2s.trading.controller

import com.g2s.trading.history.HistoryUseCase
import org.springframework.web.bind.annotation.PostMapping
import org.springframework.web.bind.annotation.RequestMapping
import org.springframework.web.bind.annotation.RequestParam
import org.springframework.web.bind.annotation.RestController

@RestController
@RequestMapping("/api/history")
class HistoryController(
    private val historyUseCase: HistoryUseCase
) {
    @PostMapping("/on")
    fun turnOnHistoryFeature(@RequestParam(name = "strategyKey") strategyKey: String) {
        historyUseCase.turnOnHistoryFeature(strategyKey)
    }

    @PostMapping("/on/all")
    fun turnOnAllHistoryFeature() {
        historyUseCase.turnOnAllHistoryFeature()
    }

    @PostMapping("/off")
    fun turnOffHistoryFeature(@RequestParam(name = "strategyKey") strategyKey: String) {
        historyUseCase.turnOffHistoryFeature(strategyKey)
    }

    @PostMapping("/off/all")
    fun turnOffAllHistoryFeature() {
        historyUseCase.turnOffAllHistoryFeature()
    }
}
