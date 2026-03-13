package com.app.sdui.components

import com.app.sdui.core.UIAction
import com.app.sdui.core.UIComponent
import com.app.sdui.core.UIStyle

data class TextInputComponent(
    val value: String = "",
    val placeholder: String? = null,
    val action: UIAction? = null,
    override val id: String? = null,
    override val style: UIStyle? = null,
) : UIComponent
