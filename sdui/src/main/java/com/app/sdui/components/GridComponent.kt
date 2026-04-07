package com.app.sdui.components

import com.app.sdui.core.UIComponent
import com.app.sdui.core.UIStyle

/**
 * Grid layout for posters/cards (Netflix-style rows of tiles).
 */
data class GridComponent(
    val columns: Int = 3,
    val children: List<UIComponent> = emptyList(),
    val horizontalSpacingDp: Float? = null,
    val verticalSpacingDp: Float? = null,
    override val id: String? = null,
    override val style: UIStyle? = null,
) : UIComponent

