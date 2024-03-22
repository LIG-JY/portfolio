package com.g2s.trading.repository

import com.g2s.trading.common.ObjectMapperProvider
import com.g2s.trading.strategy.StrategySpec
import com.g2s.trading.strategy.StrategySpecRepository
import com.g2s.trading.strategy.StrategySpecServiceStatus
import com.mongodb.client.model.Filters
import com.mongodb.client.model.FindOneAndReplaceOptions
import com.mongodb.client.model.ReturnDocument
import org.bson.Document
import org.springframework.data.mongodb.core.MongoTemplate
import org.springframework.data.mongodb.core.query.Criteria
import org.springframework.data.mongodb.core.query.Query
import org.springframework.stereotype.Component

@Component
class MongoStrategySpecRepository(
    private val mongoTemplate: MongoTemplate
) : StrategySpecRepository {

    companion object {
        private const val SPEC_COLLECTION_NAME = "strategy_spec"
    }

    override fun findStrategySpecByKey(strategyKey: String): StrategySpec? {
        val query = Query.query(Criteria.where("strategyKey").`is`(strategyKey))
        val result = mongoTemplate.findOne(query, StrategySpec::class.java, SPEC_COLLECTION_NAME)

        return result
    }

    override fun findAllServiceStrategySpec(): List<StrategySpec> {
        val query = Query.query(
            Criteria.where(StrategySpec::status.name).`is`(StrategySpecServiceStatus.SERVICE.name)
        )
        return mongoTemplate.find(
            query, StrategySpec::class.java, SPEC_COLLECTION_NAME
        )
    }

    override fun findAllServiceStrategySpecByType(type: String): List<StrategySpec> {
        val query = Query.query(
            Criteria.where(StrategySpec::status.name).`is`(StrategySpecServiceStatus.SERVICE.name)
                .andOperator(
                    Criteria.where(StrategySpec::strategyType.name).`is`(type)
                )
        )
        return mongoTemplate.find(
            query, StrategySpec::class.java, SPEC_COLLECTION_NAME
        )
    }

    override fun updateSpec(strategySpec: StrategySpec): StrategySpec {
        val om = ObjectMapperProvider.get()
        val resultDocument = mongoTemplate.db.getCollection(SPEC_COLLECTION_NAME).findOneAndReplace(
            Filters.eq(strategySpec::strategyKey.name, strategySpec.strategyKey),
            Document.parse(om.writeValueAsString(strategySpec)),
            FindOneAndReplaceOptions().returnDocument(ReturnDocument.AFTER).upsert(true)
        )

        return om.readValue(resultDocument!!.toJson(), StrategySpec::class.java)
    }
}
