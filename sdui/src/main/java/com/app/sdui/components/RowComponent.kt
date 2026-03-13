package com.app.sdui.components

import com.app.sdui.core.UIComponent
import com.app.sdui.core.UIStyle

data class RowComponent(
    val children: List<UIComponent> = emptyList(),
    override val id: String? = null,
    override val style: UIStyle? = null,
) : UIComponent
