package com.app.sdui.components

import com.app.sdui.core.UIAction
import com.app.sdui.core.UIComponent
import com.app.sdui.core.UIStyle

data class ButtonComponent(
    val label: String,
    val action: UIAction = UIAction.None,
    override val id: String? = null,
    override val style: UIStyle? = null,
) : UIComponent
