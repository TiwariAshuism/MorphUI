package com.app.sdui.components

import com.app.sdui.core.UIComponent
import com.app.sdui.core.UIStyle

data class DividerComponent(
    val thickness: Float = 1f,
    val color: String? = null,
    override val id: String? = null,
    override val style: UIStyle? = null,
) : UIComponent
