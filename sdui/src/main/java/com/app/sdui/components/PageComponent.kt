package com.app.sdui.components

import com.app.sdui.core.UIComponent
import com.app.sdui.core.UIStyle

/**
 * Top-level container for a screen/page.
 *
 * Phase 5 introduces this component, but the renderer stays compatible with
 * older payloads that start with `list`/`column`.
 */
data class PageComponent(
    val title: String? = null,
    val children: List<UIComponent> = emptyList(),
    override val id: String? = null,
    override val style: UIStyle? = null,
) : UIComponent

