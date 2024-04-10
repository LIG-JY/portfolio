package com.g2s.trading.history

interface HistoryRepository {
    fun saveHistory(history: History)
}
