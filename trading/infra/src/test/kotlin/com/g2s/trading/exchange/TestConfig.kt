package com.g2s.trading.exchange

import com.binance.connector.futures.client.impl.UMFuturesClientImpl
import org.junit.jupiter.api.Disabled
import org.springframework.beans.factory.annotation.Value
import org.springframework.boot.test.context.TestConfiguration
import org.springframework.context.annotation.Bean

@Disabled
@TestConfiguration
class TestConfig(
    @Value("\${binance.apiKey}") private val apiKey: String,
    @Value("\${binance.apiSecret}") private val apiSecret: String,
    @Value("\${binance.rest.url}") private val restUrl: String,
    @Value("\${binance.wss.url}") private val webSocketUrl: String
) {

    @Bean
    fun umFuturesClientImpl(): UMFuturesClientImpl {
        return UMFuturesClientImpl(apiKey, apiSecret, restUrl)
    }
}
