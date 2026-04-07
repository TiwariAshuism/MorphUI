package com.app.sdui.data.remote.bff

import kotlinx.serialization.json.JsonArray
import kotlinx.serialization.json.JsonElement
import kotlinx.serialization.json.JsonNull
import kotlinx.serialization.json.JsonObject
import kotlinx.serialization.json.JsonPrimitive
import kotlinx.serialization.json.*

/**
 * Converts kotlinx.serialization JSON structures into Map/List primitives
 * compatible with the existing MorphUIEngine Map-based pipeline.
 *
 * This is a Phase 4 bridge. Later we can parse DTOs directly into UIComponent
 * for stronger type safety end-to-end.
 */
internal object JsonElementMapper {

    fun toAny(element: JsonElement): Any? {
        return when (element) {
            is JsonNull -> null
            is JsonObject -> toMap(element)
            is JsonArray -> element.map { toAny(it) }
            is JsonPrimitive -> toPrimitive(element)
            else -> null
        }
    }

    fun toMap(obj: JsonObject): Map<String, Any> {
        val out = LinkedHashMap<String, Any>(obj.size)
        for ((k, v) in obj) {
            val mapped = toAny(v) ?: continue
            @Suppress("UNCHECKED_CAST")
            out[k] = mapped as Any
        }
        return out
    }

    private fun toPrimitive(p: JsonPrimitive): Any? {
        if (p.isString) return p.content

        // Keep numbers as Double/Long where possible. Existing ComponentRegistry
        // uses Number casts, so either works.
        p.longOrNull?.let { return it }
        p.doubleOrNull?.let { return it }
        p.booleanOrNull?.let { return it }
        return p.contentOrNull
    }
}

