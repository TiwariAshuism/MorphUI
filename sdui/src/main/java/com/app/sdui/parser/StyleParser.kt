package com.app.sdui.parser

import com.app.sdui.core.UIStyle

/**
 * Parses JSON style maps into framework-agnostic [UIStyle] instances.
 *
 * All values are stored as primitives (Float, String) — no Compose imports.
 */
object StyleParser {

    fun parse(styleMap: Map<String, Any>?): UIStyle? {
        if (styleMap.isNullOrEmpty()) return null

        return UIStyle(
            padding = styleMap.floatOrNull("padding"),
            paddingHorizontal = styleMap.floatOrNull("paddingHorizontal"),
            paddingVertical = styleMap.floatOrNull("paddingVertical"),
            margin = styleMap.floatOrNull("margin"),
            backgroundColor = styleMap["background"] as? String
                ?: styleMap["backgroundColor"] as? String,
            cornerRadius = styleMap.floatOrNull("cornerRadius"),
            textColor = styleMap["textColor"] as? String,
            fontSize = styleMap.floatOrNull("fontSize"),
            fontWeight = styleMap["fontWeight"] as? String,
            textAlign = styleMap["alignment"] as? String
                ?: styleMap["textAlign"] as? String,
            width = styleMap.floatOrNull("width"),
            height = styleMap.floatOrNull("height"),
            maxWidth = styleMap.floatOrNull("maxWidth"),
            maxHeight = styleMap.floatOrNull("maxHeight"),
            opacity = styleMap.floatOrNull("opacity"),
            borderColor = styleMap["borderColor"] as? String,
            borderWidth = styleMap.floatOrNull("borderWidth"),
            elevation = styleMap.floatOrNull("elevation"),
        )
    }

    private fun Map<String, Any>.floatOrNull(key: String): Float? {
        return (this[key] as? Number)?.toFloat()
    }
}
