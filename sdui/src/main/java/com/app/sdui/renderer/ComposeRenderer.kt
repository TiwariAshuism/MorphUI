package com.app.sdui.renderer

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import coil.compose.AsyncImage
import com.app.sdui.components.*
import com.app.sdui.core.UIAction
import com.app.sdui.core.UIComponent
import com.app.sdui.renderer.StyleResolver.fontSize
import com.app.sdui.renderer.StyleResolver.fontWeight
import com.app.sdui.renderer.StyleResolver.textAlign
import com.app.sdui.renderer.StyleResolver.textColor
import com.app.sdui.renderer.StyleResolver.toContainerModifier
import com.app.sdui.renderer.StyleResolver.toModifier

/**
 * Unified Jetpack Compose renderer for MorphUI components.
 *
 * Uses exhaustive `when` on the sealed [UIComponent] hierarchy —
 * the compiler enforces handling every component type. No more
 * large registry lookups or scattered renderer objects.
 */
object ComposeRenderer {

    @Composable
    fun RenderComponent(
        component: UIComponent,
        onAction: (UIAction) -> Unit,
    ) {
        when (component) {
            is TextComponent -> RenderText(component)
            is ImageComponent -> RenderImage(component)
            is ButtonComponent -> RenderButton(component, onAction)
            is ColumnComponent -> RenderColumn(component, onAction)
            is RowComponent -> RenderRow(component, onAction)
            is SpacerComponent -> RenderSpacer(component)
            is CardComponent -> RenderCard(component, onAction)
            is DividerComponent -> RenderDivider(component)
            is TextInputComponent -> RenderTextInput(component, onAction)
            is IconButtonComponent -> RenderIconButton(component, onAction)
            is ListComponent -> RenderList(component, onAction)
            is BottomNavComponent -> RenderBottomNav(component, onAction)
            is UnknownComponent -> RenderUnknown(component)
        }
    }

    // ──────────────────────────────────────────────
    // Individual render functions
    // ──────────────────────────────────────────────

    @Composable
    private fun RenderText(component: TextComponent) {
        Text(
            text = component.value,
            modifier = component.style.toModifier(),
            color = component.style.textColor(),
            fontSize = component.style.fontSize(),
            fontWeight = component.style.fontWeight(),
            textAlign = component.style.textAlign(),
        )
    }

    @Composable
    private fun RenderImage(component: ImageComponent) {
        AsyncImage(
            model = component.url,
            contentDescription = component.contentDescription,
            modifier = component.style.toModifier(),
            contentScale = ContentScale.Crop,
        )
    }

    @Composable
    private fun RenderButton(component: ButtonComponent, onAction: (UIAction) -> Unit) {
        val style = component.style

        Button(
            onClick = { onAction(component.action) },
            modifier = style.toModifier(),
            colors = ButtonDefaults.buttonColors(
                containerColor = StyleResolver.parseColor(style?.backgroundColor) ?: Color(0xFF6200EE),
            ),
            shape = RoundedCornerShape(style?.cornerRadius?.dp ?: 8.dp),
        ) {
            Text(
                text = component.label,
                color = style.textColor().let { if (it == Color.Black) Color.White else it },
                fontSize = style.fontSize(),
            )
        }
    }

    @Composable
    private fun RenderColumn(component: ColumnComponent, onAction: (UIAction) -> Unit) {
        Column(
            modifier = component.style.toContainerModifier(),
        ) {
            component.children.forEach { child ->
                RenderComponent(child, onAction)
            }
        }
    }

    @Composable
    private fun RenderRow(component: RowComponent, onAction: (UIAction) -> Unit) {
        Row(
            modifier = component.style.toContainerModifier(),
        ) {
            component.children.forEach { child ->
                RenderComponent(child, onAction)
            }
        }
    }

    @Composable
    private fun RenderSpacer(component: SpacerComponent) {
        Spacer(
            modifier = Modifier
                .then(component.height?.let { Modifier.height(it.dp) } ?: Modifier)
                .then(component.width?.let { Modifier.width(it.dp) } ?: Modifier),
        )
    }

    @Composable
    private fun RenderCard(component: CardComponent, onAction: (UIAction) -> Unit) {
        val style = component.style

        Card(
            modifier = style.toModifier(),
            shape = RoundedCornerShape(style?.cornerRadius?.dp ?: 8.dp),
            colors = CardDefaults.cardColors(
                containerColor = StyleResolver.parseColor(style?.backgroundColor) ?: Color.White,
            ),
            elevation = CardDefaults.cardElevation(
                defaultElevation = style?.elevation?.dp ?: 4.dp,
            ),
        ) {
            RenderComponent(component.child, onAction)
        }
    }

    @Composable
    private fun RenderDivider(component: DividerComponent) {
        val color = StyleResolver.parseColor(component.color) ?: Color.LightGray

        HorizontalDivider(
            modifier = component.style.toModifier(),
            thickness = component.thickness.dp,
            color = color,
        )
    }

    @Composable
    private fun RenderTextInput(component: TextInputComponent, onAction: (UIAction) -> Unit) {
        val style = component.style
        val textState = remember(component.id ?: "") { mutableStateOf(component.value) }

        OutlinedTextField(
            value = textState.value,
            onValueChange = { textState.value = it },
            modifier = style.toContainerModifier(),
            placeholder = {
                component.placeholder?.let {
                    Text(text = it, color = style.textColor().let { c ->
                        if (c == Color.Black) Color.Gray else c
                    })
                }
            },
            singleLine = false,
            maxLines = 4,
        )
    }

    @Composable
    private fun RenderIconButton(component: IconButtonComponent, onAction: (UIAction) -> Unit) {
        val style = component.style

        IconButton(
            onClick = { onAction(component.action) },
            modifier = style.toModifier(),
        ) {
            Text(
                text = component.icon,
                color = style.textColor().let {
                    if (it == Color.Black) MaterialTheme.colorScheme.primary else it
                },
                textAlign = TextAlign.Center,
            )
        }
    }

    @Composable
    private fun RenderList(component: ListComponent, onAction: (UIAction) -> Unit) {
        LazyColumn(
            modifier = component.style.toContainerModifier()
                .then(Modifier.fillMaxSize()),
        ) {
            items(component.children) { child ->
                RenderComponent(child, onAction)
            }
        }
    }

    @Composable
    private fun RenderBottomNav(component: BottomNavComponent, onAction: (UIAction) -> Unit) {
        val style = component.style

        Surface(
            color = StyleResolver.parseColor(style?.backgroundColor) ?: Color.White,
            shadowElevation = style?.elevation?.dp ?: 8.dp,
        ) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .then(style.toModifier()),
                horizontalArrangement = Arrangement.SpaceEvenly,
            ) {
                component.children.forEach { child ->
                    RenderComponent(child, onAction)
                }
            }
        }
    }

    @Composable
    private fun RenderUnknown(component: UnknownComponent) {
        Text(
            text = "Unknown component: ${component.type}",
            modifier = Modifier.padding(8.dp),
            color = Color.Red,
        )
    }
}
