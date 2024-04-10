package com.g2s.trading.indicator

import java.lang.RuntimeException

enum class Interval(val value: String) {
    ONE_MINUTE("1m");
//    FIVE_MINUTE("5m"),
//    TEN_MINUTE("10m");

    companion object {
        fun from(value: String): Interval {
            return entries.find { it.value == value } ?: throw RuntimeException("no interval enum")
        }
    }
}
