package com.app.sdui.components

import com.app.sdui.core.UIComponent
import com.app.sdui.core.UIStyle

data class SpacerComponent(
    val height: Float? = null,
    val width: Float? = null,
    override val id: String? = null,
    override val style: UIStyle? = null,
) : UIComponent
