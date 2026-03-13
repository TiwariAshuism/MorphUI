package com.app.sdui.presentation.renderer

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import com.app.sdui.domain.model.UIAction
import com.app.sdui.domain.model.UIElement

object IconButtonRenderer : ComponentRenderer<UIElement.IconButton> {
    @Composable
    override fun Render(element: UIElement.IconButton, onAction: (UIAction) -> Unit) {
        val style = element.style

        Box(
            modifier = Modifier
                .then(style?.margin?.let { Modifier.padding(it) } ?: Modifier)
        ) {
            IconButton(
                onClick = { onAction(element.action) },
                modifier = Modifier.then(style?.padding?.let { Modifier.padding(it) } ?: Modifier)
            ) {
                // For simplicity, render the icon string as text (e.g., "♥", "↻")
                Text(
                    text = element.icon,
                    color = style?.textColor ?: MaterialTheme.colorScheme.primary,
                    textAlign = TextAlign.Center
                )
            }
        }
    }
}

