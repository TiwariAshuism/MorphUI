package com.app.sdui.core

/**
 * Base sealed interface for all MorphUI components.
 *
 * Every UI component in the SDUI system extends this interface,
 * enabling exhaustive pattern matching in the renderer and inspector.
 */
interface UIComponent {
    val id: String?
    val style: UIStyle?
}
