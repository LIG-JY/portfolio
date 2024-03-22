package com.g2s.trading.exchange

import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.Configuration
import org.springframework.scheduling.annotation.EnableScheduling
import org.springframework.scheduling.concurrent.ThreadPoolTaskScheduler

@Configuration
@EnableScheduling
class BinanceStreamExchangeSchedulingConfig {

    @Bean
    fun taskScheduler(): ThreadPoolTaskScheduler {
        return ThreadPoolTaskScheduler().apply {
            poolSize = 2
            setThreadNamePrefix("ExchangeStream-taskScheduler-")
        }
    }
}
