package com.app.sdui.components

import com.app.sdui.core.UIAction
import com.app.sdui.core.UIComponent
import com.app.sdui.core.UIStyle

data class ButtonComponent(
    val label: String,
    val action: UIAction = UIAction.None,
    /**
     * Optional form-state key that, when true, indicates this button is loading.
     * Used for pagination and async actions without introducing new component types.
     */
    val loadingKey: String? = null,
    /**
     * Optional form-state key that, when false, disables the button.
     * If null, button is enabled.
     */
    val enabledKey: String? = null,
    /** Optional label to render when disabled. */
    val disabledLabel: String? = null,
    override val id: String? = null,
    override val style: UIStyle? = null,
) : UIComponent
