package com.g2s.trading.lock

import org.springframework.stereotype.Service
import java.util.concurrent.ConcurrentHashMap

@Service
class LockUseCase {
    private val locks = ConcurrentHashMap<LockKey, Unit>()

    // strategyLock
    fun acquire(strategyKey: String, usage: LockUsage): Boolean {
        val key = LockKey(strategyKey, usage)
        var result = false
        locks.computeIfAbsent(key) {
            result = true
        }

        return result
    }

    fun release(strategyKey: String, usage: LockUsage) {
        val key = LockKey(strategyKey, usage)
        locks.remove(key)
    }

    internal data class LockKey(
        val strategyKey: String,
        val usage: LockUsage
    )
}
