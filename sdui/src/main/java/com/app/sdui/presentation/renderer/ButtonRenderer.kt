package com.app.sdui.presentation.renderer

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import com.app.sdui.domain.model.UIAction
import com.app.sdui.domain.model.UIElement

object ButtonRenderer : ComponentRenderer<UIElement.Button> {
    @Composable
    override fun Render(element: UIElement.Button, onAction: (UIAction) -> Unit) {
        val style = element.style
        
        Box(
            modifier = Modifier
                .then(style?.margin?.let { Modifier.padding(it) } ?: Modifier)
        ) {
            Button(
                onClick = { onAction(element.action) },
                modifier = Modifier
                    .then(style?.width?.let { Modifier.width(it) } ?: Modifier)
                    .then(style?.padding?.let { Modifier.padding(it) } ?: Modifier),
                colors = ButtonDefaults.buttonColors(
                    containerColor = style?.backgroundColor ?: Color(0xFF6200EE)
                ),
                shape = RoundedCornerShape(style?.cornerRadius ?: 8.dp)
            ) {
                Text(
                    text = element.label,
                    color = style?.textColor ?: Color.White,
                    fontSize = style?.fontSize ?: androidx.compose.ui.unit.TextUnit.Unspecified
                )
            }
        }
    }
}
