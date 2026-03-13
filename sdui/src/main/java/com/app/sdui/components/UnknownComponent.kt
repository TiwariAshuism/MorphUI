package com.app.sdui.components

import com.app.sdui.core.UIComponent
import com.app.sdui.core.UIStyle

/**
 * Fallback component for unrecognized component types.
 * Rendered as an error placeholder in debug mode, invisible in release.
 */
data class UnknownComponent(
    val type: String,
    override val id: String? = null,
    override val style: UIStyle? = null,
) : UIComponent
