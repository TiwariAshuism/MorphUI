package com.app.sdui.components

import com.app.sdui.core.UIComponent
import com.app.sdui.core.UIStyle

data class TextComponent(
    val value: String,
    override val id: String? = null,
    override val style: UIStyle? = null,
) : UIComponent
