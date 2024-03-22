package com.g2s.trading

import org.springframework.boot.autoconfigure.SpringBootApplication
import org.springframework.boot.runApplication

@SpringBootApplication(scanBasePackages = ["com.g2s.trading"])
class TradingApplication

fun main(args: Array<String>) {
    runApplication<TradingApplication>(*args)
}
