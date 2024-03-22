package com.g2s.trading.repository

import com.g2s.trading.position.Position
import com.g2s.trading.position.PositionRepository
import org.slf4j.LoggerFactory
import org.springframework.data.mongodb.core.FindAndReplaceOptions
import org.springframework.data.mongodb.core.MongoTemplate
import org.springframework.data.mongodb.core.query.Criteria.where
import org.springframework.data.mongodb.core.query.Query
import org.springframework.stereotype.Component

@Component
class MongoPositionRepository(val mongoTemplate: MongoTemplate) : PositionRepository {

    private val logger = LoggerFactory.getLogger(MongoStrategySpecRepository::class.java)

    companion object {
        private const val POSITION_COLLECTION_NAME = "positions"
    }

    override fun findAllPositions(): List<Position> {
        return mongoTemplate.findAll(Position::class.java, POSITION_COLLECTION_NAME)
    }

    override fun savePosition(position: Position) {
        mongoTemplate.save(position, POSITION_COLLECTION_NAME)
    }

    override fun updatePosition(position: Position) {
        val query = Query.query(where("positionKey").`is`(position.positionKey))

        val options = FindAndReplaceOptions.options().upsert().returnNew()
        val result = mongoTemplate.findAndReplace(query, position, options, POSITION_COLLECTION_NAME)

        if (result != null) {
            logger.debug("A document was upserted or replaced.")
        } else {
            logger.debug("No operation was performed.")
        }
    }

    override fun deletePosition(position: Position) {
        mongoTemplate.remove(Query.query(where("positionKey").`is`(position.positionKey)), POSITION_COLLECTION_NAME)
    }
}
