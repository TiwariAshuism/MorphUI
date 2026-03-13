package com.app.sdui.presentation.renderer

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import com.app.sdui.domain.model.UIAction
import com.app.sdui.domain.model.UIElement
import com.app.sdui.presentation.screen.DynamicRenderer

object ListRenderer : ComponentRenderer<UIElement.ListContainer> {
    @Composable
    override fun Render(element: UIElement.ListContainer, onAction: (UIAction) -> Unit) {
        val style = element.style

        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .then(style?.backgroundColor?.let { Modifier.background(it) } ?: Modifier)
        ) {
            items(element.children) { child ->
                DynamicRenderer(element = child, onAction = onAction)
            }
        }
    }
}

