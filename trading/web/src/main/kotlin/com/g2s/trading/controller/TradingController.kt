package com.g2s.trading.controller

import com.g2s.trading.dreamandhope.DreamAndHope
import com.g2s.trading.dreamandhope.StartStrategyRequest
import com.g2s.trading.dreamandhope.StopStrategyRequest
import com.g2s.trading.dreamandhope.UpdateStrategyRequest
import com.g2s.trading.response.ApiResponseService
import com.g2s.trading.response.ApiResult
import org.springframework.http.HttpEntity
import org.springframework.web.bind.annotation.PostMapping
import org.springframework.web.bind.annotation.RequestBody
import org.springframework.web.bind.annotation.RestController

@RestController
class TradingController(
    private val dreamAndHope: DreamAndHope,
    private val apiResponseService: ApiResponseService
) {

    @PostMapping("/api/start")
    fun start(
        @RequestBody startStrategyRequest: StartStrategyRequest
    ): HttpEntity<ApiResult> {
        dreamAndHope.start(startStrategyRequest.strategyKey)

        return apiResponseService.noContent().toResponseEntity()
    }

    @PostMapping("/api/stop")
    fun stop(
        @RequestBody stopStrategyRequest: StopStrategyRequest
    ): HttpEntity<ApiResult> {
        dreamAndHope.stop(stopStrategyRequest.strategyKey)

        return apiResponseService.noContent().toResponseEntity()
    }

    @PostMapping("api/update")
    fun update(
        @RequestBody updateStrategyRequest: UpdateStrategyRequest
    ): HttpEntity<ApiResult> {
        dreamAndHope.update(updateStrategyRequest.strategySpec)

        return apiResponseService.noContent().toResponseEntity()
    }
}
