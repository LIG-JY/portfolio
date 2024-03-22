package com.g2s.trading.response

import com.g2s.trading.common.ApiError
import jakarta.servlet.http.HttpServletRequest
import org.slf4j.event.Level
import org.springframework.core.env.Environment
import org.springframework.http.HttpStatus
import org.springframework.http.MediaType
import org.springframework.stereotype.Service
import org.springframework.web.bind.MethodArgumentNotValidException
import org.springframework.web.server.ServerWebInputException
import org.springframework.web.servlet.resource.NoResourceFoundException

/**
 * 표준 응답 결과를 생성하는 서비스
 */
@Service
class ApiResponseService(
    private val environment: Environment,
) {
    private val isLocalProfile: Boolean by lazy {
        environment.activeProfiles.any { profile -> profile.equals("local", ignoreCase = true) }
    }

    fun <T> ok(data: T): ApiResponse {
        return ApiResponse(HttpStatus.OK, MediaType.APPLICATION_JSON, ApiResult.Single(data))
    }

    fun <T> ok(data: List<T>): ApiResponse {
        return ApiResponse(HttpStatus.OK, MediaType.APPLICATION_JSON, ApiResult.List(data))
    }

    fun created() = ApiResponse(HttpStatus.CREATED)

    fun noContent() = ApiResponse(HttpStatus.NO_CONTENT)

    fun failure(
        request: HttpServletRequest,
        throwable: Throwable,
    ): ApiResponse {
        val (httpStatus, apiError) = determineHttpStatusAndWrapApiError(throwable)

        apiError.print(
            requestMethod = request.method,
            requestPath = request.requestURI.toString(),
            requestStatus = httpStatus.name
        )

        val isTraceEnabled = isLocalProfile
        return ApiResponse(
            httpStatus,
            MediaType.APPLICATION_JSON,
            ApiResult.Failure(
                reason = apiError.code,
                message = apiError.message,
                path = request.requestURI.toString(),
                extra = if (isTraceEnabled) apiError.extra else null,
                trace = if (isTraceEnabled) apiError.cause?.toString() else null
            )
        )
    }

    private fun determineHttpStatusAndWrapApiError(throwable: Throwable): Pair<HttpStatus, ApiError> {
        return when (throwable) {
            is ApiError -> {
                Pair(HttpStatus.INTERNAL_SERVER_ERROR, throwable)
            }
            is NoResourceFoundException -> {
                Pair(
                    HttpStatus.NOT_FOUND, ApiError(
                        code = "NOT_FOUND_ERROR",
                        message = "not found",
                        cause = throwable,
                        logLevel = Level.ERROR
                    )
                )
            }

            // 권한이 없는 사용자가 요청한 경우
            is AccessDeniedException -> {
                Pair(
                    HttpStatus.UNAUTHORIZED, ApiError(
                        code = "ACCESS_DENIED_ERROR",
                        message = "접근할 수 있는 권한이 없습니다.",
                        cause = throwable,
                        logLevel = Level.INFO
                    )
                )
            }
            // @RequestBody의 파라미터가 유효하지 않은 경우
            is MethodArgumentNotValidException -> {
                Pair(
                    HttpStatus.BAD_REQUEST, ApiError(
                        code = "BAD_REQUEST_ERROR",
                        message = throwable.bindingResult.fieldErrors
                            .map { fieldError ->
                                fieldError.defaultMessage
                            }.toSet()
                            .joinToString("\n"),
                        cause = throwable,
                        logLevel = Level.INFO
                    )
                )
            }
            // 요청 바인딩 에러, 요청 바디 처리 에러, 유효성 검사 오류 등의 경우
            is ServerWebInputException -> {
                Pair(
                    HttpStatus.BAD_REQUEST, ApiError(
                        code = "BAD_REQUEST_ERROR",
                        message = throwable.reason ?: "Bad request",
                        cause = throwable,
                        logLevel = Level.INFO
                    )
                )
            }

            else -> {
                Pair(
                    HttpStatus.INTERNAL_SERVER_ERROR, ApiError(
                        code = "UNKNOWN_ERROR",
                        message = throwable.message ?: "Unknown error",
                        cause = throwable,
                        logLevel = Level.ERROR
                    )
                )
            }
        }
    }
}
