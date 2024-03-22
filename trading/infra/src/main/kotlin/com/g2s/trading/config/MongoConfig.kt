package com.g2s.trading.config

import com.mongodb.ConnectionString
import com.mongodb.MongoClientSettings
import com.mongodb.client.MongoClient
import com.mongodb.client.MongoClients
import org.bson.UuidRepresentation
import org.slf4j.LoggerFactory
import org.springframework.beans.factory.annotation.Value
import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.Configuration
import org.springframework.data.mongodb.MongoDatabaseFactory
import org.springframework.data.mongodb.core.MongoTemplate
import org.springframework.data.mongodb.core.SimpleMongoClientDatabaseFactory
import org.springframework.data.mongodb.core.convert.DefaultDbRefResolver
import org.springframework.data.mongodb.core.convert.DefaultMongoTypeMapper
import org.springframework.data.mongodb.core.convert.MappingMongoConverter
import org.springframework.data.mongodb.core.mapping.MongoMappingContext
import java.util.concurrent.TimeUnit

@Configuration
class MongoConfig(
    @Value("\${trading.mongodb.uri}")
    val mongoConnectionString: String,
    @Value("\${trading.mongodb.connectionPool.size}")
    val connectionPoolSize: Int,
    @Value("\${trading.mongodb.connection.timeoutMs}")
    val connectionTimeout: Int
) {
    companion object {
        private const val DATABASE_NAME = "dev"
    }

    private val logger = LoggerFactory.getLogger(MongoConfig::class.java)

    @Bean
    fun mongoClientSettings(): MongoClientSettings {
        return MongoClientSettings.builder()
            .applyToConnectionPoolSettings { builder ->
                builder.maxConnectionLifeTime(1, TimeUnit.HOURS)
                builder.minSize(connectionPoolSize)
                builder.maxSize(connectionPoolSize)
            }.applyToSocketSettings { builder ->
                builder.connectTimeout(connectionTimeout, TimeUnit.MILLISECONDS)
            }
            .applyConnectionString(ConnectionString(mongoConnectionString))
            .uuidRepresentation(UuidRepresentation.JAVA_LEGACY)
            .build()
    }

    @Bean
    fun mongoClient(): MongoClient {
        logger.debug("Connecting to MongoDB at URI: {}", mongoConnectionString);
        return MongoClients.create(mongoClientSettings())
    }

    @Bean
    fun mongoDatabaseFactory(): MongoDatabaseFactory {
        return SimpleMongoClientDatabaseFactory(mongoClient(), DATABASE_NAME)
    }

    @Bean
    fun mappingMongoConverter(): MappingMongoConverter {
        val converter = MappingMongoConverter(DefaultDbRefResolver(mongoDatabaseFactory()), MongoMappingContext())
        converter.customConversions = MongoConverters().mongoCustomConversions()
        converter.setTypeMapper(DefaultMongoTypeMapper(null))
        converter.afterPropertiesSet()

        return converter
    }

    @Bean
    fun mongoTemplate(): MongoTemplate {
        return MongoTemplate(mongoDatabaseFactory(), mappingMongoConverter())
    }
}
