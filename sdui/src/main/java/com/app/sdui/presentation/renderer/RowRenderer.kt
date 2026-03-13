package com.app.sdui.presentation.renderer

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import com.app.sdui.domain.model.UIAction
import com.app.sdui.domain.model.UIElement
import com.app.sdui.presentation.screen.DynamicRenderer

object RowRenderer : ComponentRenderer<UIElement.Row> {
    @Composable
    override fun Render(element: UIElement.Row, onAction: (UIAction) -> Unit) {
        val style = element.style
        
        Row(
            modifier = Modifier
                .then(style?.margin?.let { Modifier.padding(it) } ?: Modifier)
                .then(style?.backgroundColor?.let { Modifier.background(it) } ?: Modifier)
                .then(style?.padding?.let { Modifier.padding(it) } ?: Modifier)
                .then(style?.width?.let { Modifier.width(it) } ?: Modifier.fillMaxWidth())
        ) {
            element.children.forEach { child ->
                DynamicRenderer(element = child, onAction = onAction)
            }
        }
    }
}
