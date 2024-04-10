package com.g2s.trading.repository

import com.g2s.trading.history.History
import com.g2s.trading.history.HistoryRepository
import org.springframework.data.mongodb.core.MongoTemplate
import org.springframework.stereotype.Component

@Component
class MongoHistoryRepository(
    private val mongoTemplate: MongoTemplate
) : HistoryRepository {
    companion object {
        private const val HISTORY_COLLECTION_NAME = "history"
    }

    override fun saveHistory(history: History) {
        mongoTemplate.save(history, HISTORY_COLLECTION_NAME)
    }
}
