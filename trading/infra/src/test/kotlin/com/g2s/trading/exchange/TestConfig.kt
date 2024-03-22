package com.g2s.trading.exchange

import com.binance.connector.futures.client.impl.UMFuturesClientImpl
import com.g2s.trading.indicator.MarkPriceUseCase
import org.springframework.beans.factory.annotation.Value
import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.Configuration

@Configuration
class TestConfig {

    @Value("\${binance.apiKey}")
    private lateinit var apiKey: String

    @Value("\${binance.apiSecret}")
    private lateinit var apiSecret: String

    @Value("\${binance.rest.url}")
    private lateinit var restUrl: String

    @Value("\${binance.wss.url}")
    private lateinit var webSocketUrl: String

    @Bean
    fun umFuturesClientImpl(): UMFuturesClientImpl {
        return UMFuturesClientImpl(apiKey, apiSecret, restUrl)
    }

    @Bean
    fun restApiExchange(umFuturesClientImpl: UMFuturesClientImpl): Exchange {
        return RestApiExchangeImpl(umFuturesClientImpl)
    }

    @Bean
    fun markPriceUseCase(restApiExchange: Exchange): MarkPriceUseCase {
        return MarkPriceUseCase(restApiExchange)
    }
}
