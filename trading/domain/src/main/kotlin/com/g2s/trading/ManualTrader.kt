package com.g2s.trading

import com.g2s.trading.account.AccountUseCase
import com.g2s.trading.history.CloseCondition
import com.g2s.trading.history.ConditionUseCase
import com.g2s.trading.history.OpenCondition
import com.g2s.trading.lock.LockUsage
import com.g2s.trading.lock.LockUseCase
import com.g2s.trading.position.Position
import com.g2s.trading.position.PositionUseCase
import com.g2s.trading.strategy.StrategySpec
import com.g2s.trading.symbol.Symbol
import org.springframework.stereotype.Component

@Component
class ManualTrader(
    val lockUseCase: LockUseCase,
    val accountUseCase: AccountUseCase,
    val positionUseCase: PositionUseCase,
    val conditionUseCase: ConditionUseCase,
) {

    fun manuallyOpenPosition(position: Position, spec: StrategySpec) {
        // lock 획득
        val acquired = lockUseCase.acquire(position.strategyKey, LockUsage.OPEN)
        if (!acquired) return

        // TODO: 강제로 열 때 이미 열린 포지션 덮어 쓰면 close 후 새로 open으로 처리됨
        val usedSymbols = positionUseCase.getAllUsedSymbols()
        if (usedSymbols.contains(position.symbol)) {
            lockUseCase.release(position.strategyKey, LockUsage.OPEN)
            return
        }

        // account lock
        val accountAcquired = accountUseCase.acquire()
        if (!accountAcquired) {
            lockUseCase.release(position.strategyKey, LockUsage.OPEN)
            return
        }

        // account sync check
        val accountSynced = accountUseCase.isSynced()
        if (!accountSynced) {
            accountUseCase.release()
            lockUseCase.release(position.strategyKey, LockUsage.OPEN)
            return
        }

        try {
            val allocatedBalance =
                accountUseCase.getAllocatedBalancePerStrategy(spec.asset, spec.allocatedRatio)
            val availableBalance = accountUseCase.getAvailableBalance(spec.asset)

            if (allocatedBalance > availableBalance) {
                accountUseCase.release()
                lockUseCase.release(spec.strategyKey, LockUsage.OPEN)
                throw RuntimeException("Insufficient account ")
            }
        } catch (e: RuntimeException) {
            println(e.message)
        }
        conditionUseCase.setOpenCondition(position, OpenCondition.ManualCondition)
        positionUseCase.openPosition(position, spec)
        accountUseCase.release()
        lockUseCase.release(position.strategyKey, LockUsage.OPEN)
    }

    fun manuallyClosePosition(symbol: Symbol, spec: StrategySpec) {
        val position = positionUseCase.getAllPositions().find { it.symbol == symbol }
        if (position == null) {
            return
        }
        val acquired = lockUseCase.acquire(position.strategyKey, LockUsage.CLOSE)
        if (!acquired) return

        if (!position.synced) {
            lockUseCase.release(position.strategyKey, LockUsage.CLOSE)
            return
        }
        conditionUseCase.setCloseCondition(position, CloseCondition.ManualCondition)
        positionUseCase.closePosition(position, spec)
        lockUseCase.release(position.strategyKey, LockUsage.CLOSE)
    }

}
