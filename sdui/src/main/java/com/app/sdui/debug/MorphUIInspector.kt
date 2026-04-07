package com.app.sdui.debug

import com.app.sdui.components.*
import com.app.sdui.core.UIComponent

/**
 * Debug inspector that generates ASCII tree representations of MorphUI component trees.
 *
 * Usage:
 * ```kotlin
 * val tree = MorphUIInspector.inspect(component)
 * Log.d("MorphUI", tree)
 * ```
 *
 * Output:
 * ```
 * Column (id=root)
 * ├── Card
 * │   ├── Row
 * │   │   ├── Image (url=https://example.com/img.jpg)
 * │   │   └── Text (value="Hello World")
 * │   └── Spacer (h=16.0)
 * └── Button (label="Click Me")
 *
 * Components: 7 | Max Depth: 4
 * ```
 */
object MorphUIInspector {

    /**
     * Generates a full ASCII tree representation with stats.
     */
    fun inspect(component: UIComponent): String {
        val sb = StringBuilder()
        buildTree(component, sb, prefix = "", isLast = true, isRoot = true)
        sb.appendLine()
        sb.appendLine("Components: ${componentCount(component)} | Max Depth: ${maxDepth(component)}")
        return sb.toString()
    }

    /**
     * Returns the total number of components in the tree.
     */
    fun componentCount(component: UIComponent): Int {
        return 1 + getChildren(component).sumOf { componentCount(it) }
    }

    /**
     * Returns the maximum nesting depth.
     */
    fun maxDepth(component: UIComponent): Int {
        val children = getChildren(component)
        return if (children.isEmpty()) 1
        else 1 + children.maxOf { maxDepth(it) }
    }

    /**
     * Logs the tree via [android.util.Log] if debug mode is on.
     */
    fun logTree(component: UIComponent, tag: String = "MorphUI") {
        val tree = inspect(component)
        tree.lines().forEach { line ->
            android.util.Log.d(tag, line)
        }
    }

    // ──────────────────────────────────────────────
    // Tree builder
    // ──────────────────────────────────────────────

    private fun buildTree(
        component: UIComponent,
        sb: StringBuilder,
        prefix: String,
        isLast: Boolean,
        isRoot: Boolean,
    ) {
        val connector = when {
            isRoot -> ""
            isLast -> "└── "
            else -> "├── "
        }

        val childPrefix = when {
            isRoot -> ""
            isLast -> "$prefix    "
            else -> "$prefix│   "
        }

        sb.appendLine("$prefix$connector${describe(component)}")

        val children = getChildren(component)
        children.forEachIndexed { index, child ->
            buildTree(
                component = child,
                sb = sb,
                prefix = childPrefix,
                isLast = index == children.lastIndex,
                isRoot = false,
            )
        }
    }

    // ──────────────────────────────────────────────
    // Component description
    // ──────────────────────────────────────────────

    private fun describe(component: UIComponent): String {
        val idSuffix = component.id?.let { " (id=$it)" } ?: ""

        val detail = when (component) {
            is TextComponent -> "Text (value=\"${component.value.take(30)}\")"
            is ImageComponent -> "Image (url=${component.url.take(50)})"
            is ButtonComponent -> "Button (label=\"${component.label}\")"
            is ColumnComponent -> "Column"
            is RowComponent -> "Row"
            is BoxComponent -> "Box (${component.children.size} children)"
            is NavBarItemComponent -> "NavBarItem (label=\"${component.label}\")"
            is SpacerComponent -> {
                val dims = listOfNotNull(
                    component.height?.let { "h=$it" },
                    component.width?.let { "w=$it" },
                ).joinToString(", ")
                "Spacer${if (dims.isNotEmpty()) " ($dims)" else ""}"
            }
            is CardComponent -> "Card"
            is DividerComponent -> "Divider (thickness=${component.thickness})"
            is TextInputComponent -> "TextInput (placeholder=\"${component.placeholder ?: ""}\")"
            is IconButtonComponent -> "IconButton (icon=\"${component.icon}\")"
            is ListComponent -> "List (${component.children.size} items)"
            is CarouselComponent -> "Carousel (${component.children.size} items)"
            is GridComponent -> "Grid (${component.children.size} items)"
            is BottomNavComponent -> "BottomNav (${component.children.size} items)"
            is PageComponent -> "Page (${component.children.size} children)"
            is HeroComponent -> "Hero"
            is UnknownComponent -> "Unknown (type=\"${component.type}\")"
            else -> {}
        }

        return "$detail$idSuffix"
    }

    // ──────────────────────────────────────────────
    // Children extraction
    // ──────────────────────────────────────────────

    private fun getChildren(component: UIComponent): List<UIComponent> {
        return when (component) {
            is ColumnComponent -> component.children
            is RowComponent -> component.children
            is BoxComponent -> component.children
            is CardComponent -> listOf(component.child)
            is ListComponent -> component.children
            is CarouselComponent -> component.children
            is GridComponent -> component.children
            is BottomNavComponent -> component.children
            is PageComponent -> component.children
            is HeroComponent -> emptyList()
            is NavBarItemComponent -> emptyList()
            is TextComponent -> emptyList()
            is ImageComponent -> emptyList()
            is ButtonComponent -> emptyList()
            is SpacerComponent -> emptyList()
            is DividerComponent -> emptyList()
            is TextInputComponent -> emptyList()
            is IconButtonComponent -> emptyList()
            is UnknownComponent -> emptyList()
            else -> {
                android.util.Log.w("MorphUIInspector", "Unknown component type: ${component::class.java}")
                emptyList()
            }}
        }
    }
