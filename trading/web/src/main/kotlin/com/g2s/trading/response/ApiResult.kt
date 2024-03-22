package com.g2s.trading.response

/**
 * API 응답 바디로 나갈 API 결과
 */
sealed class ApiResult {
    abstract val status: Status // 응답 상태를 구분 하기 위한 값

    enum class Status { SUCCESS, FAILURE }

    /**
     * 단건 성공 API 결과
     *
     * @property data 단건 데이터
     */
    data class Single<T>(
        val data: T,
    ) : ApiResult() {
        override val status = Status.SUCCESS
    }

    // TODO: pagination 개념 녹이기
    /**
     * 다건 성공 API 결과
     *
     * @property list 다건 데이터
     */
    data class List<T>(
        val list: T,
    ) : ApiResult() {
        override val status = Status.SUCCESS
    }

    /**
     * 실패 API 결과
     *
     * @property reason 실패 이유
     * @property message 에러 메시지
     * @property path 요청 Path
     * @property extra 추가 정보
     * @property trace 스택 트레이스
     */
    data class Failure(
        val reason: String,
        val message: String,
        val path: String,
        val extra: String?,
        val trace: String?
    ) : ApiResult() {
        override val status = Status.FAILURE
    }
}
