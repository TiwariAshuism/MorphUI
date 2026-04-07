package com.app.sdui.parser

/**
 * Pre-parse JSON schema validation for MorphUI component trees.
 *
 * Validates:
 * - Every component has a "type" field
 * - Props are present when required by the component type
 * - Children are valid component arrays
 * - No circular or deeply nested structures
 *
 * Supports both **strict** mode (fails on first error) and
 * **lenient** mode (collects all errors).
 */
object SchemaValidator {

    /** Maximum nesting depth to prevent stack overflows. */
    private const val MAX_DEPTH = 50

    /** Map of component type → list of required props keys. */
    private val requiredProps: Map<String, List<String>> = mapOf(
        "text" to listOf("value"),
        "image" to listOf("url"),
        "button" to listOf("label"),
        "icon_button" to listOf("icon"),
        "hero" to listOf("imageUrl"),
    )

    /** Component types that require at least one child. */
    private val requiresChildren: Set<String> = setOf("card")

    /** Component types that support children. */
    private val supportsChildren: Set<String> = setOf(
        "page",
        "column", "row", "list",
        "carousel",
        "grid",
        "bottom_nav",
        "card",
    )

    // ──────────────────────────────────────────────

    sealed interface ValidationResult {
        data object Valid : ValidationResult
        data class Invalid(val errors: List<ValidationError>) : ValidationResult
    }

    data class ValidationError(
        val path: String,
        val message: String,
    )

    // ──────────────────────────────────────────────

    /**
     * Validates a full screen JSON payload.
     *
     * @param json The root JSON map, expected to contain "screen" or be a component itself.
     * @param strict If true, stop at the first error.
     */
    @Suppress("UNCHECKED_CAST")
    fun validate(
        json: Map<String, Any>,
        strict: Boolean = false,
    ): ValidationResult {
        val errors = mutableListOf<ValidationError>()

        // Support both root-level component and screen-wrapped payload
        val component = json["screen"] as? Map<String, Any> ?: json

        validateComponent(component, "root", 0, errors, strict)

        return if (errors.isEmpty()) {
            ValidationResult.Valid
        } else {
            ValidationResult.Invalid(errors)
        }
    }

    @Suppress("UNCHECKED_CAST")
    private fun validateComponent(
        json: Map<String, Any>,
        path: String,
        depth: Int,
        errors: MutableList<ValidationError>,
        strict: Boolean,
    ) {
        // Depth guard
        if (depth > MAX_DEPTH) {
            errors += ValidationError(path, "Maximum nesting depth ($MAX_DEPTH) exceeded")
            return
        }

        // 1. Type must be present
        val type = json["type"] as? String
        if (type.isNullOrBlank()) {
            errors += ValidationError(path, "Missing or empty 'type' field")
            if (strict) return
        }

        // 2. Required props check
        if (type != null) {
            val required = requiredProps[type.lowercase()]
            if (required != null) {
                val props = json["props"] as? Map<String, Any> ?: emptyMap()
                for (key in required) {
                    if (!props.containsKey(key)) {
                        errors += ValidationError(
                            "$path.$type",
                            "Missing required prop '$key' for component '$type'"
                        )
                        if (strict) return
                    }
                }
            }
        }

        // 3. Children validation
        val children = json["children"] as? List<Map<String, Any>>
        if (type != null) {
            val typeLower = type.lowercase()

            // Check if children are required but missing
            if (typeLower in requiresChildren && children.isNullOrEmpty()) {
                errors += ValidationError(
                    "$path.$type",
                    "Component '$type' requires at least one child"
                )
                if (strict) return
            }

            // Check if children are present but not supported
            if (!children.isNullOrEmpty() && typeLower !in supportsChildren) {
                errors += ValidationError(
                    "$path.$type",
                    "Component '$type' does not support children"
                )
                if (strict) return
            }
        }

        // 4. Recurse into children
        children?.forEachIndexed { index, child ->
            validateComponent(
                json = child,
                path = "$path.children[$index]",
                depth = depth + 1,
                errors = errors,
                strict = strict,
            )
        }
    }
}
