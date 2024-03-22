package com.g2s.trading.common

import org.slf4j.event.Level

/**
 * 도메인별로 API 에러를 다루기 위한 API 에러 모음 인터페이스
 */
interface ApiErrors {
    fun error(): ApiError = TODO()
    fun error(message: String): ApiError = TODO()
    fun error(cause: Throwable): ApiError = TODO()
    fun error(message: String, cause: Throwable): ApiError = TODO()
    fun error(message: String, logLevel: Level, logMessage: String? = null): ApiError = TODO()
    fun error(message: String, cause: Throwable, logLevel: Level, logMessage: String? = null): ApiError = TODO()
}
