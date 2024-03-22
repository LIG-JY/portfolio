package com.g2s.trading.response

import org.springframework.http.HttpStatus
import org.springframework.http.MediaType
import org.springframework.http.ResponseEntity

/**
 * 공통 API 응답
 *
 * 배경 :
 * 다양한 응답부에서 HTTP 응답(200, 201, 204, 4xx~5xx 등)을 만들 때 사용하는 Spring 패키지의 클래스가 다름
 * - 컨트롤러 메서드 : HttpEntity
 * - 글로벌 에러 핸들러 : ServerResponse
 * - SpringSecurity 예외 처리 : ServerHttpResponse
 * 성공 또는 실패 여부, 다양한 응답부, 스프링과 상관 없이 HTTP 응답을 유연하게 처리하기 위해 필요한 데이터를 담을 클래스가 필요해져서 이 클래스를 정의함.
 *
 * @property status HTTP 상태
 * @property contentType HTTP 컨텐츠 타입
 * @property body HTTP Body
 */
data class ApiResponse(
    val status: HttpStatus,
    val contentType: MediaType? = null,
    val body: ApiResult? = null
) {
    fun toResponseEntity(): ResponseEntity<ApiResult> {
        val responseEntity = ResponseEntity.status(status)
        if (contentType != null) {
            responseEntity.contentType(contentType)
        }
        return when (body != null) {
            true -> responseEntity.body(body)
            false -> responseEntity.build()
        }
    }
}
