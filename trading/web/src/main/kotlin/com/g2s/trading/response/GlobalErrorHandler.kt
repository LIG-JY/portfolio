package com.g2s.trading.response

import jakarta.servlet.http.HttpServletRequest
import org.springframework.http.ResponseEntity
import org.springframework.web.bind.annotation.ExceptionHandler
import org.springframework.web.bind.annotation.RestControllerAdvice

@RestControllerAdvice
class GlobalErrorHandler(
    private val apiResponseService: ApiResponseService
) {
    @ExceptionHandler(value = [Exception::class])
    fun handleException(
        exception: Exception,
        request: HttpServletRequest
    ): ResponseEntity<ApiResult> {
        val apiResponse = apiResponseService.failure(request, exception)

        return apiResponse.toResponseEntity()
    }
}
