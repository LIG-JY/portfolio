package com.g2s.trading.history

/*
 각 청산 조건 클래스의 이름은 해당 전략의 이름과 'Condition'을 결합하여 명명한다.
 예를 들어, 'Simple' 전략의 경우 조건 클래스의 이름은 'SimpleCondition'이 된다.

 - 전략 이름: 전략을 나타내는 명확한 이름으로 StrategyType enum의 value (예: Simple)
 - Condition: 'Condition' 고정 문자열을 접미사로 사용하여 클래스가 특정 전략의 조건을 정의함을 나타냄
*/
sealed class CloseCondition {
    /*
    BigDecimal DB에 Mapping할 때 에러 발생해서 String으로 변환 후 저장
    Exception in thread "taskExecutor-10" java.lang.reflect.InaccessibleObjectException: Unable to make field private final java.math.BigInteger java.math.BigDecimal.intVal accessible: module java.base does not "opens java.math" to unnamed module @644baf4a
     */
    data class SimpleCondition(
        val tailLength: String,
        val tailLengthWithStopLossFactor: String,
        val entryPrice: String,
        val lastPrice: String,
        val beforeBalance: Double
    ) : CloseCondition() {
        override fun toString(): String {
            return "SimpleCondition(tailLength=$tailLength, tailLengthWithStopLossFactor=$tailLengthWithStopLossFactor, entryPrice=$entryPrice, lastPrice=$lastPrice, beforeBalance=$beforeBalance)"
        }
    }

    data object ManualCondition : CloseCondition()
}
