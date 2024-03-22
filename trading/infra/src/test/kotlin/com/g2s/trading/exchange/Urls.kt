package com.g2s.trading.exchange

enum class Urls(val value: String) {
    TESTNET_URL("https://testnet.binancefuture.com"),
    TESTNET_WSS_URL("wss://stream.binancefuture.com"),
    USDM_PROD_URL("https://fapi.binance.com"),
    USDM_WS_URL("wss://fstream.binance.com"),
    COINM_PROD_URL("https://dapi.binance.com"),
    COINM_WS_URL("wss://dstream.binance.com")
}
