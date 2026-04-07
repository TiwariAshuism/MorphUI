package com.app.sdui.registry

import com.app.sdui.components.*
import com.app.sdui.core.UIComponent
import com.app.sdui.core.UIStyle
import com.app.sdui.parser.ActionParser
import com.app.sdui.parser.ComponentParser
import com.app.sdui.parser.StyleParser

/**
 * Central registry that maps component type names to their parsers.
 *
 * All 12 built-in component parsers are pre-registered. Third-party
 * developers can dynamically register custom components via [register].
 *
 * Usage:
 * ```kotlin
 * val registry = ComponentRegistry()
 * registry.register("my_custom_widget") { props, children, style, id ->
 *     MyCustomWidget(props["title"] as String, id = id, style = style)
 * }
 * ```
 */
class ComponentRegistry {

    private val parsers = mutableMapOf<String, ComponentParser>()

    init {
        registerBuiltInParsers()
    }

    // ──────────────────────────────────────────────
    // Public API
    // ──────────────────────────────────────────────

    fun register(type: String, parser: ComponentParser) {
        parsers[type.lowercase()] = parser
    }

    fun getParser(type: String): ComponentParser? {
        return parsers[type.lowercase()]
    }

    fun hasParser(type: String): Boolean {
        return parsers.containsKey(type.lowercase())
    }

    fun registeredTypes(): Set<String> = parsers.keys.toSet()

    /**
     * Parses a full component tree from a raw JSON map.
     *
     * This is the primary entry point for converting JSON → [UIComponent].
     */
    @Suppress("UNCHECKED_CAST")
    fun parseComponent(json: Map<String, Any>): UIComponent {
        val type = json["type"] as? String
            ?: throw IllegalArgumentException("Component JSON must contain a 'type' field: $json")

        val props = json["props"] as? Map<String, Any> ?: emptyMap()
        val styleMap = json["style"] as? Map<String, Any>
        val childrenJson = json["children"] as? List<Map<String, Any>>
        val id = json["id"] as? String

        val style: UIStyle? = StyleParser.parse(styleMap)
        val children: List<UIComponent> = childrenJson?.map { parseComponent(it) } ?: emptyList()

        val parser = getParser(type)
            ?: return UnknownComponent(type = type, id = id, style = style)

        return parser.parse(props, children, style, id)
    }

    // ──────────────────────────────────────────────
    // Built-in parsers
    // ──────────────────────────────────────────────

    private fun registerBuiltInParsers() {
        register("page") { props, children, style, id ->
            PageComponent(
                title = props["title"] as? String,
                children = children,
                id = id,
                style = style,
            )
        }

        register("text") { props, _, style, id ->
            TextComponent(
                value = props["value"] as? String ?: "",
                id = id,
                style = style,
            )
        }

        register("image") { props, _, style, id ->
            ImageComponent(
                url = props["url"] as? String ?: "",
                contentDescription = props["contentDescription"] as? String,
                id = id,
                style = style,
            )
        }

        register("hero") { props, _, style, id ->
            HeroComponent(
                imageUrl = props["imageUrl"] as? String
                    ?: props["url"] as? String
                    ?: "",
                title = props["title"] as? String,
                subtitle = props["subtitle"] as? String,
                primaryAction = ActionParser.parse(props["primaryAction"]),
                secondaryAction = ActionParser.parse(props["secondaryAction"]),
                id = id,
                style = style,
            )
        }

        register("button") { props, _, style, id ->
            ButtonComponent(
                label = props["label"] as? String ?: "",
                action = ActionParser.parse(props["action"]),
                loadingKey = props["loadingKey"] as? String,
                enabledKey = props["enabledKey"] as? String,
                disabledLabel = props["disabledLabel"] as? String,
                id = id,
                style = style,
            )
        }

        register("column") { _, children, style, id ->
            ColumnComponent(
                children = children,
                id = id,
                style = style,
            )
        }

        register("row") { _, children, style, id ->
            RowComponent(
                children = children,
                id = id,
                style = style,
            )
        }

        register("box") { _, children, style, id ->
            BoxComponent(
                children = children,
                id = id,
                style = style,
            )
        }

        register("spacer") { props, _, style, id ->
            SpacerComponent(
                height = (props["height"] as? Number)?.toFloat(),
                width = (props["width"] as? Number)?.toFloat(),
                id = id,
                style = style,
            )
        }

        register("card") { _, children, style, id ->
            CardComponent(
                child = children.firstOrNull()
                    ?: UnknownComponent(type = "empty_card"),
                id = id,
                style = style,
            )
        }

        register("divider") { props, _, style, id ->
            DividerComponent(
                thickness = (props["thickness"] as? Number)?.toFloat() ?: 1f,
                color = props["color"] as? String,
                id = id,
                style = style,
            )
        }

        register("text_input") { props, _, style, id ->
            TextInputComponent(
                value = props["value"] as? String ?: "",
                placeholder = props["placeholder"] as? String,
                action = ActionParser.parse(props["action"]),
                id = id,
                style = style,
            )
        }

        register("icon_button") { props, _, style, id ->
            IconButtonComponent(
                icon = props["icon"] as? String ?: "",
                action = ActionParser.parse(props["action"]),
                id = id,
                style = style,
            )
        }

        register("list") { props, children, style, id ->
            ListComponent(
                children = children,
                contentPaddingBottomDp = (props["contentPaddingBottomDp"] as? Number)?.toFloat(),
                id = id,
                style = style,
            )
        }

        register("nav_item") { props, _, style, id ->
            NavBarItemComponent(
                label = props["label"] as? String ?: "",
                icon = props["icon"] as? String ?: "",
                selected = props["selected"] as? Boolean ?: false,
                action = ActionParser.parse(props["action"]),
                id = id,
                style = style,
            )
        }

        register("carousel") { props, children, style, id ->
            CarouselComponent(
                title = props["title"] as? String,
                itemSpacingDp = (props["itemSpacingDp"] as? Number)?.toFloat(),
                contentPaddingHorizontalDp = (props["contentPaddingHorizontalDp"] as? Number)?.toFloat(),
                children = children,
                id = id,
                style = style,
            )
        }

        register("grid") { props, children, style, id ->
            GridComponent(
                columns = (props["columns"] as? Number)?.toInt() ?: 3,
                horizontalSpacingDp = (props["horizontalSpacingDp"] as? Number)?.toFloat(),
                verticalSpacingDp = (props["verticalSpacingDp"] as? Number)?.toFloat(),
                children = children,
                id = id,
                style = style,
            )
        }

        register("bottom_nav") { _, children, style, id ->
            BottomNavComponent(
                children = children,
                id = id,
                style = style,
            )
        }
    }
}
