package com.app.sdui.presentation.renderer

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.layout.ContentScale
import coil.compose.AsyncImage
import com.app.sdui.domain.model.UIAction
import com.app.sdui.domain.model.UIElement

object ImageRenderer : ComponentRenderer<UIElement.Image> {
    @Composable
    override fun Render(element: UIElement.Image, onAction: (UIAction) -> Unit) {
        val style = element.style
        
        Box(
            modifier = Modifier
                .then(style?.margin?.let { Modifier.padding(it) } ?: Modifier)
        ) {
            AsyncImage(
                model = element.url,
                contentDescription = element.contentDescription,
                modifier = Modifier
                    .then(style?.width?.let { Modifier.width(it) } ?: Modifier)
                    .then(style?.height?.let { Modifier.height(it) } ?: Modifier)
                    .then(style?.cornerRadius?.let { Modifier.clip(RoundedCornerShape(it)) } ?: Modifier)
                    .then(style?.backgroundColor?.let { Modifier.background(it) } ?: Modifier)
                    .then(style?.padding?.let { Modifier.padding(it) } ?: Modifier),
                contentScale = ContentScale.Crop
            )
        }
    }
}
