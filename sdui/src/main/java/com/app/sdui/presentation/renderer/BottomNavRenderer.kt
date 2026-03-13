package com.app.sdui.presentation.renderer

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Surface
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import com.app.sdui.domain.model.UIAction
import com.app.sdui.domain.model.UIElement
import com.app.sdui.presentation.screen.DynamicRenderer

object BottomNavRenderer : ComponentRenderer<UIElement.BottomNav> {
    @Composable
    override fun Render(element: UIElement.BottomNav, onAction: (UIAction) -> Unit) {
        val style = element.style

        Surface(
            color = style?.backgroundColor ?: Color.White,
            shadowElevation = 8.dp
        ) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .then(style?.padding?.let { Modifier.padding(it) } ?: Modifier),
                horizontalArrangement = Arrangement.SpaceEvenly
            ) {
                element.children.forEach { child ->
                    DynamicRenderer(element = child, onAction = onAction)
                }
            }
        }
    }
}

