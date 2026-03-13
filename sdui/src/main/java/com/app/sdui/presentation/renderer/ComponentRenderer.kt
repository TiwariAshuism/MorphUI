package com.app.sdui.presentation.renderer

import androidx.compose.runtime.Composable
import com.app.sdui.domain.model.UIElement
import com.app.sdui.domain.model.UIAction

interface ComponentRenderer<T : UIElement> {
    @Composable
    fun Render(element: T, onAction: (UIAction) -> Unit)
}
