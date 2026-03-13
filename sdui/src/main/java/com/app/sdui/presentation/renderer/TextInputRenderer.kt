package com.app.sdui.presentation.renderer

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import com.app.sdui.domain.model.UIAction
import com.app.sdui.domain.model.UIElement

object TextInputRenderer : ComponentRenderer<UIElement.TextInput> {
    @Composable
    override fun Render(element: UIElement.TextInput, onAction: (UIAction) -> Unit) {
        val style = element.style
        val textState = remember(element.id ?: "") { mutableStateOf(element.value) }

        Box(
            modifier = Modifier
                .then(style?.margin?.let { Modifier.padding(it) } ?: Modifier)
                .then(style?.backgroundColor?.let { Modifier.background(it) } ?: Modifier)
        ) {
            OutlinedTextField(
                value = textState.value,
                onValueChange = { textState.value = it },
                modifier = Modifier
                    .fillMaxWidth()
                    .then(style?.padding?.let { Modifier.padding(it) } ?: Modifier),
                placeholder = {
                    element.placeholder?.let {
                        Text(text = it, color = style?.textColor ?: Color.Gray)
                    }
                },
                singleLine = false,
                maxLines = 4
            )
        }
    }
}

