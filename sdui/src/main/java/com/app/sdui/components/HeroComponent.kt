package com.app.sdui.components

import com.app.sdui.core.UIAction
import com.app.sdui.core.UIComponent
import com.app.sdui.core.UIStyle

/**
 * First-class hero/banner component.
 *
 * Backend can either emit this, or compose the hero using primitives.
 * Supporting it here enables compact payloads and consistent rendering.
 */
data class HeroComponent(
    val imageUrl: String,
    val title: String? = null,
    val subtitle: String? = null,
    val primaryAction: UIAction? = null,
    val secondaryAction: UIAction? = null,
    override val id: String? = null,
    override val style: UIStyle? = null,
) : UIComponent

