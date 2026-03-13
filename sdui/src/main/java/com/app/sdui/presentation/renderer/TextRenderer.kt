package com.app.sdui.presentation.renderer

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import com.app.sdui.domain.model.UIAction
import com.app.sdui.domain.model.UIElement

object TextRenderer : ComponentRenderer<UIElement.Text> {
    @Composable
    override fun Render(element: UIElement.Text, onAction: (UIAction) -> Unit) {
        val style = element.style
        
        Box(
            modifier = Modifier
                .then(style?.margin?.let { Modifier.padding(it) } ?: Modifier)
                .then(style?.backgroundColor?.let { Modifier.background(it) } ?: Modifier)
        ) {
            Text(
                text = element.value,
                modifier = Modifier
                    .then(style?.padding?.let { Modifier.padding(it) } ?: Modifier)
                    .then(style?.width?.let { Modifier.width(it) } ?: Modifier),
                color = style?.textColor ?: Color.Black,
                fontSize = style?.fontSize ?: androidx.compose.ui.unit.TextUnit.Unspecified,
                fontWeight = style?.fontWeight,
                textAlign = style?.textAlign
            )
        }
    }
}
