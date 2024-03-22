package com.g2s.trading.common

import org.slf4j.LoggerFactory
import org.slf4j.event.Level

/**
 * API 에러를 통일된 형식으로 다루기 위한 API 에러 클래스
 *
 * @property code 에러 코드
 * @property message 사용자 메시지
 * @property cause 원인이 되는 throwable
 * @property logLevel 로그 레벨
 * @property logMessage 로깅 메시지
 * @property extra 부가적인 참조 데이터
 */
class ApiError(
    val code: String,
    override val message: String,
    override val cause: Throwable? = null,
    val logLevel: Level,
    val logMessage: String? = null,
    val extra: String? = null
) : RuntimeException() {
    private val logger = LoggerFactory.getLogger(this::class.java)

    fun print() {
        val message = "[$code], logMessage=$logMessage, message=$message, cause=$cause, extra=$extra"
        print(message)
    }

    fun print(requestMethod: String, requestPath: String, requestStatus: String) {
        val message =
            "[$code] $requestMethod $requestPath, status=$requestStatus, logMessage=$logMessage, message=$message, extra=$extra"
        print(message)
    }

    private fun print(message: String) {
        when (logLevel) {
            Level.ERROR -> logger.error(message, cause)
            Level.WARN -> logger.warn(message, cause)
            Level.INFO -> logger.info(message, cause)
            Level.DEBUG -> logger.debug(message, cause)
            Level.TRACE -> logger.trace(message, cause)
        }
    }
}
