package com.app.sdui.presentation.renderer

import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Divider
import androidx.compose.material3.HorizontalDivider
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import com.app.sdui.domain.model.UIAction
import com.app.sdui.domain.model.UIElement
import androidx.core.graphics.toColorInt

object DividerRenderer : ComponentRenderer<UIElement.Divider> {
    @Composable
    override fun Render(element: UIElement.Divider, onAction: (UIAction) -> Unit) {
        val color = element.color?.let { 
             try {
                Color(it.toColorInt())
             } catch (e: Exception) {
                Color.LightGray
             }
        } ?: Color.LightGray

        HorizontalDivider(
            modifier = Modifier.padding(element.style?.padding ?: 0.dp),
            thickness = element.thickness.dp,
            color = color
        )
    }
}
