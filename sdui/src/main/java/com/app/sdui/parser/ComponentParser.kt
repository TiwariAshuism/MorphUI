package com.app.sdui.parser

import com.app.sdui.core.UIComponent
import com.app.sdui.core.UIStyle

/**
 * Functional interface for parsing a single component type from JSON props.
 *
 * Each component type registers its own [ComponentParser] in the [ComponentRegistry].
 * The parser receives:
 *  - [props]: the "props" map from the JSON
 *  - [children]: already-parsed child [UIComponent]s
 *  - [style]: already-parsed [UIStyle] (or null)
 *  - [id]: the optional component id
 */
fun interface ComponentParser {

    fun parse(
        props: Map<String, Any>,
        children: List<UIComponent>,
        style: UIStyle?,
        id: String?,
    ): UIComponent
}
