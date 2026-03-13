package com.app.sdui.binding

/**
 * Resolves `{{path.to.variable}}` templates against a runtime data context.
 *
 * Usage:
 * ```kotlin
 * val engine = DataBindingEngine()
 * val data = mapOf("user" to mapOf("name" to "Ashu", "age" to 25))
 *
 * engine.resolve("Hello, {{user.name}}!", data)
 * // → "Hello, Ashu!"
 * ```
 *
 * Binding is applied at the **JSON string level**, before parsing.
 * This means any value — props, styles, actions — can use templates.
 */
class DataBindingEngine {

    companion object {
        private val BINDING_PATTERN = Regex("""\{\{(.+?)}}""")
    }

    /**
     * Resolves all `{{key}}` / `{{key.sub}}` templates in [input]
     * against the provided [data] context.
     *
     * Unresolved bindings remain as-is (no crash, just a log warning).
     */
    fun resolve(input: String, data: Map<String, Any>): String {
        if (!input.contains("{{")) return input

        return BINDING_PATTERN.replace(input) { match ->
            val path = match.groupValues[1].trim()
            val value = resolveValue(path, data)
            value?.toString() ?: run {
                android.util.Log.w("DataBindingEngine", "Unresolved binding: {{$path}}")
                match.value // leave as-is
            }
        }
    }

    /**
     * Resolves bindings in all string values within a raw JSON map.
     * Recursively walks the map and resolves any string values containing `{{...}}`.
     */
    @Suppress("UNCHECKED_CAST")
    fun resolveMap(json: Map<String, Any>, data: Map<String, Any>): Map<String, Any> {
        if (data.isEmpty()) return json

        return json.mapValues { (_, value) ->
            resolveAny(value, data)
        }
    }

    @Suppress("UNCHECKED_CAST")
    private fun resolveAny(value: Any, data: Map<String, Any>): Any {
        return when (value) {
            is String -> resolve(value, data)
            is Map<*, *> -> (value as Map<String, Any>).mapValues { (_, v) ->
                resolveAny(v, data)
            }
            is List<*> -> value.map { item ->
                if (item != null) resolveAny(item, data) else item
            }
            else -> value
        }
    }

    /**
     * Resolves a dot-separated path against the data context.
     *
     * Example: `resolveValue("user.address.city", data)` walks
     * `data["user"]["address"]["city"]`.
     */
    @Suppress("UNCHECKED_CAST")
    private fun resolveValue(path: String, data: Map<String, Any>): Any? {
        val segments = path.split(".")
        var current: Any? = data

        for (segment in segments) {
            current = when (current) {
                is Map<*, *> -> (current as Map<String, Any>)[segment]
                else -> return null
            }
        }

        return current
    }
}
