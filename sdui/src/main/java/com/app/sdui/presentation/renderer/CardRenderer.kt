package com.app.sdui.presentation.renderer

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import com.app.sdui.domain.model.UIAction
import com.app.sdui.domain.model.UIElement
import com.app.sdui.presentation.screen.DynamicRenderer

object CardRenderer : ComponentRenderer<UIElement.Card> {
    @Composable
    override fun Render(element: UIElement.Card, onAction: (UIAction) -> Unit) {
        val style = element.style
        
        Card(
            modifier = Modifier
                .then(style?.margin?.let { Modifier.padding(it) } ?: Modifier)
                .then(style?.padding?.let { Modifier.padding(it) } ?: Modifier),
            shape = RoundedCornerShape(style?.cornerRadius ?: 8.dp),
            colors = CardDefaults.cardColors(
                containerColor = style?.backgroundColor ?: Color.White
            ),
            elevation = CardDefaults.cardElevation(defaultElevation = 4.dp)
        ) {
            DynamicRenderer(element = element.child, onAction = onAction)
        }
    }
}
