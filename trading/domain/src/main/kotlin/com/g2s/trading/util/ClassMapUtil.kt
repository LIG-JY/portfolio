package com.g2s.trading.util

object ClassMapUtil {
    fun <T : Any> dataClassToLinkedHashMap(dataClassInstance: T): LinkedHashMap<String, Any?> {
        val linkedHashMap = LinkedHashMap<String, Any?>()

        val properties = dataClassInstance::class.java.declaredFields
        properties.forEach { field ->
            field.isAccessible = true
            linkedHashMap[field.name] = field.get(dataClassInstance)
        }
        return linkedHashMap
    }
}