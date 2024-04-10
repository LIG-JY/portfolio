package com.g2s.trading.history

import com.g2s.trading.strategy.simple.SimplePattern

/*
 각 오픈 조건 클래스의 이름은 해당 전략의 이름과 'Condition'을 결합하여 명명한다.
 예를 들어, 'Simple' 전략의 경우 조건 클래스의 이름은 'SimpleCondition'이 된다.

 - 전략 이름: 전략을 나타내는 명확한 이름으로 StrategyType enum의 value (예: Simple)
 - Condition: 'Condition' 고정 문자열을 접미사로 사용하여 클래스가 특정 전략의 조건을 정의함을 나타냄
*/
sealed class OpenCondition {
    /*
    BigDecimal DB에 Mapping할 때 에러 발생해서 String으로 변환 후 저장
    Exception in thread "taskExecutor-10" java.lang.reflect.InaccessibleObjectException: Unable to make field private final java.math.BigInteger java.math.BigDecimal.intVal accessible: module java.base does not "opens java.math" to unnamed module @644baf4a
     */
    data class SimpleCondition(
        val patten: SimplePattern,
        val candleHammerRatio: String,
        val operationalCandleHammerRatio: String,
        val beforeBalance: Double
    ) : OpenCondition() {
        override fun toString(): String {
            return "SimpleCondition(patten=$patten, candleHammerRatio=$candleHammerRatio, operationalCandleHammerRatio=$operationalCandleHammerRatio, beforeBalance=$beforeBalance)"
        }
    }

    data object ManualCondition : OpenCondition()
}
