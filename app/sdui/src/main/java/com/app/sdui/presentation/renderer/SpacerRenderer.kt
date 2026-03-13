package com.app.sdui.presentation.renderer

import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.width
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.app.sdui.domain.model.UIAction
import com.app.sdui.domain.model.UIElement

object SpacerRenderer : ComponentRenderer<UIElement.Spacer> {
    @Composable
    override fun Render(element: UIElement.Spacer, onAction: (UIAction) -> Unit) {
        Spacer(
            modifier = Modifier
                .then(element.height?.let { Modifier.height(it.dp) } ?: Modifier)
                .then(element.width?.let { Modifier.width(it.dp) } ?: Modifier)
        )
    }
}
