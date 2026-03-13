package com.app.sdui.components

import com.app.sdui.core.UIComponent
import com.app.sdui.core.UIStyle

data class ImageComponent(
    val url: String,
    val contentDescription: String? = null,
    override val id: String? = null,
    override val style: UIStyle? = null,
) : UIComponent
