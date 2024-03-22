package com.g2s.trading.exchange

import com.binance.connector.futures.client.impl.UMFuturesClientImpl
import com.binance.connector.futures.client.impl.UMWebsocketClientImpl
import org.springframework.beans.factory.annotation.Value
import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.Configuration

@Configuration
class BinanceConfig(
    @Value("\${binance.apiKey}")
    private val apiKey: String,
    @Value("\${binance.apiSecret}")
    private val apiSecret: String,
    @Value("\${binance.rest.url}")
    private val restUrl: String,
    @Value("\${binance.wss.url}")
    private val webSocketUrl: String
) {

    @Bean
    fun binanceClient(): UMFuturesClientImpl {
        return UMFuturesClientImpl(apiKey, apiSecret, restUrl)
    }

    @Bean
    fun binanceWebSocketStreamClient(): UMWebsocketClientImpl {
        return UMWebsocketClientImpl(webSocketUrl)
    }
}
