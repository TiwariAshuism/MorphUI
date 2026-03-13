package com.app.sdui.engine

import com.app.sdui.binding.DataBindingEngine
import com.app.sdui.cache.UICache
import com.app.sdui.core.UIComponent
import com.app.sdui.parser.SchemaValidator
import com.app.sdui.registry.ComponentRegistry

/**
 * Central orchestrator for the MorphUI SDUI engine.
 *
 * Pipeline:
 * ```
 * JSON → Version Check → Schema Validation → Data Binding → Parsing → Caching
 * ```
 *
 * Usage:
 * ```kotlin
 * val engine = MorphUIEngine(registry, cache, bindingEngine, validator, config)
 *
 * val result = engine.parseScreen(jsonMap, runtimeData)
 * when {
 *     !result.isCompatible -> showUpdatePrompt()
 *     result.component != null -> renderUI(result.component)
 *     else -> showError(result.errors)
 * }
 * ```
 */
class MorphUIEngine(
    private val registry: ComponentRegistry,
    private val cache: UICache,
    private val bindingEngine: DataBindingEngine,
    private val validator: SchemaValidator = SchemaValidator,
    val config: MorphUIConfig = MorphUIConfig(),
) {

    /**
     * Parses a single component (no version/screen wrapper).
     * Useful for parsing individual components or fragments.
     */
    fun parseComponent(
        json: Map<String, Any>,
        data: Map<String, Any> = emptyMap(),
    ): UIComponent {
        val resolved = bindingEngine.resolveMap(json, data)
        return registry.parseComponent(resolved)
    }

    /**
     * Parses a full screen payload with version check, validation, and caching.
     *
     * Expected JSON structure:
     * ```json
     * {
     *   "ui_version": 2,
     *   "screen": { "type": "column", "children": [...] }
     * }
     * ```
     *
     * If the JSON has no "screen" wrapper, it's treated as a raw component.
     */
    @Suppress("UNCHECKED_CAST")
    fun parseScreen(
        json: Map<String, Any>,
        data: Map<String, Any> = emptyMap(),
        screenId: String? = null,
    ): ScreenResult {
        // 1. Version check
        val version = (json["ui_version"] as? Number)?.toInt() ?: 1
        val isCompatible = version in config.minSupportedVersion..config.maxSupportedVersion

        if (!isCompatible && config.strictVersioning) {
            return ScreenResult(
                component = null,
                version = version,
                isCompatible = false,
                errors = listOf(
                    "Incompatible UI version: $version " +
                            "(supported: ${config.minSupportedVersion}–${config.maxSupportedVersion})"
                ),
            )
        }

        // 2. Extract screen component
        val screenJson = json["screen"] as? Map<String, Any> ?: json

        // 3. Schema validation
        if (config.validateSchema) {
            val validationResult = validator.validate(screenJson, strict = config.strictValidation)
            if (validationResult is SchemaValidator.ValidationResult.Invalid) {
                val errorMessages = validationResult.errors.map { "${it.path}: ${it.message}" }
                if (config.strictValidation) {
                    return ScreenResult(
                        component = null,
                        version = version,
                        isCompatible = isCompatible,
                        errors = errorMessages,
                    )
                } else {
                    // Log warnings but continue parsing
                    errorMessages.forEach {
                        android.util.Log.w("MorphUIEngine", "Validation warning: $it")
                    }
                }
            }
        }

        // 4. Data binding
        val resolved = bindingEngine.resolveMap(screenJson, data)

        // 5. Parse
        return try {
            val component = registry.parseComponent(resolved)

            // 6. Cache if screenId provided
            if (screenId != null) {
                try {
                    // Serialize the resolved JSON back for caching
                    val jsonString = org.json.JSONObject(resolved).toString()
                    cache.put(screenId, jsonString, version)
                } catch (e: Exception) {
                    android.util.Log.w("MorphUIEngine", "Failed to cache screen: $screenId", e)
                }
            }

            ScreenResult(
                component = component,
                version = version,
                isCompatible = isCompatible,
                errors = emptyList(),
            )
        } catch (e: Exception) {
            android.util.Log.e("MorphUIEngine", "Failed to parse screen", e)
            ScreenResult(
                component = null,
                version = version,
                isCompatible = isCompatible,
                errors = listOf(e.message ?: "Unknown parse error"),
            )
        }
    }

    /**
     * Attempts to load a screen from cache, parse it, and return.
     */
    @Suppress("UNCHECKED_CAST")
    fun loadCached(
        screenId: String,
        data: Map<String, Any> = emptyMap(),
    ): ScreenResult? {
        val cached = cache.get(screenId) ?: return null

        return try {
            val json = org.json.JSONObject(cached.json)
            val map = jsonObjectToMap(json)
            val resolved = bindingEngine.resolveMap(map, data)
            val component = registry.parseComponent(resolved)

            ScreenResult(
                component = component,
                version = cached.version,
                isCompatible = true,
                errors = emptyList(),
            )
        } catch (e: Exception) {
            android.util.Log.w("MorphUIEngine", "Failed to parse cached screen: $screenId", e)
            null
        }
    }

    // ──────────────────────────────────────────────
    // Helpers
    // ──────────────────────────────────────────────

    @Suppress("UNCHECKED_CAST")
    private fun jsonObjectToMap(json: org.json.JSONObject): Map<String, Any> {
        val map = mutableMapOf<String, Any>()
        json.keys().forEach { key ->
            val value = json.get(key)
            map[key] = when (value) {
                is org.json.JSONObject -> jsonObjectToMap(value)
                is org.json.JSONArray -> jsonArrayToList(value)
                else -> value
            }
        }
        return map
    }

    @Suppress("UNCHECKED_CAST")
    private fun jsonArrayToList(json: org.json.JSONArray): List<Any> {
        val list = mutableListOf<Any>()
        for (i in 0 until json.length()) {
            val value = json.get(i)
            list += when (value) {
                is org.json.JSONObject -> jsonObjectToMap(value)
                is org.json.JSONArray -> jsonArrayToList(value)
                else -> value
            }
        }
        return list
    }
}

/**
 * Result of parsing a full screen payload.
 */
data class ScreenResult(
    val component: UIComponent?,
    val version: Int,
    val isCompatible: Boolean,
    val errors: List<String> = emptyList(),
) {
    val isSuccess: Boolean get() = component != null && errors.isEmpty()
}

/**
 * Configuration for [MorphUIEngine].
 */
data class MorphUIConfig(
    val minSupportedVersion: Int = 1,
    val maxSupportedVersion: Int = 10,
    val strictVersioning: Boolean = false,
    val validateSchema: Boolean = true,
    val strictValidation: Boolean = false,
    val debugMode: Boolean = false,
)
