package com.g2s.trading.config

import com.fasterxml.jackson.databind.JsonNode
import com.fasterxml.jackson.databind.node.ArrayNode
import com.fasterxml.jackson.databind.node.NullNode
import com.g2s.trading.common.ObjectMapperProvider
import org.bson.Document
import org.springframework.context.annotation.Configuration
import org.springframework.core.convert.converter.Converter
import org.springframework.data.convert.ReadingConverter
import org.springframework.data.convert.WritingConverter
import org.springframework.data.mongodb.core.convert.MongoCustomConversions
import java.io.IOException

@Configuration
class MongoConverters {
    fun mongoCustomConversions(): MongoCustomConversions {
        val converters: MutableList<Converter<*, *>> = mutableListOf()
        converters.add(ArrayNodeToDocumentListConverter())
        converters.add(JsonNodeToDocumentConverter())
        converters.add(DocumentToJsonNodeConverter())
        return MongoCustomConversions(converters)
    }

    @WritingConverter
    class ArrayNodeToDocumentListConverter : Converter<ArrayNode, List<Document>> {
        override fun convert(source: ArrayNode): List<Document> {
            val om = ObjectMapperProvider.get()
            return source.map { Document.parse(om.writeValueAsString(source)) }
        }
    }

    @WritingConverter
    class JsonNodeToDocumentConverter : Converter<JsonNode, Document> {
        override fun convert(source: JsonNode): Document? {
            val om = ObjectMapperProvider.get()
            if (source is NullNode) return null
            return Document.parse(om.writeValueAsString(source))
        }
    }

    @ReadingConverter
    class DocumentToJsonNodeConverter : Converter<Document, JsonNode> {
        override fun convert(source: Document): JsonNode {
            val mapper = ObjectMapperProvider.get()
            return try {
                mapper.readTree(source.toJson())
            } catch (e: IOException) {
                throw RuntimeException("Unable to parse DbObject to JsonNode", e)
            }
        }
    }
}