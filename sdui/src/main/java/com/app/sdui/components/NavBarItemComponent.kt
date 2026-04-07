package com.app.sdui.components

import com.app.sdui.core.UIAction
import com.app.sdui.core.UIComponent
import com.app.sdui.core.UIStyle

data class NavBarItemComponent(
    val label: String,
    val icon: String,
    val selected: Boolean = false,
    val action: UIAction,
    override val id: String? = null,
    override val style: UIStyle? = null,
) : UIComponent
