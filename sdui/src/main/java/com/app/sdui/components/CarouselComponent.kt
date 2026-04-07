package com.app.sdui.components

import com.app.sdui.core.UIComponent
import com.app.sdui.core.UIStyle

/**
 * Horizontal rail of items (Netflix-style carousel).
 *
 * The items are represented as [children] (usually cards/images/buttons).
 */
data class CarouselComponent(
    val title: String? = null,
    val children: List<UIComponent> = emptyList(),
    val itemSpacingDp: Float? = null,
    val contentPaddingHorizontalDp: Float? = null,
    override val id: String? = null,
    override val style: UIStyle? = null,
) : UIComponent

