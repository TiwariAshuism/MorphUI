package com.app.sdui.renderer

import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
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
        formState: Map<String, Any>,
        onStateChange: (String, Any) -> Unit,
        onAction: (UIAction) -> Unit,
    ) {
        when (component) {
            is PageComponent -> RenderPage(component, formState, onStateChange, onAction)
            is TextComponent -> RenderText(component)
            is ImageComponent -> RenderImage(component)
            is HeroComponent -> RenderHero(component, onAction)
            is ButtonComponent -> RenderButton(component, formState, onAction)
            is ColumnComponent -> RenderColumn(component, formState, onStateChange, onAction)
            is RowComponent -> RenderRow(component, formState, onStateChange, onAction)
            is SpacerComponent -> RenderSpacer(component)
            is CardComponent -> RenderCard(component, formState, onStateChange, onAction)
            is DividerComponent -> RenderDivider(component)
            is TextInputComponent -> RenderTextInput(component, formState, onStateChange, onAction)
            is IconButtonComponent -> RenderIconButton(component, onAction)
            is ListComponent -> RenderList(component, formState, onStateChange, onAction)
            is CarouselComponent -> RenderCarousel(component, formState, onStateChange, onAction)
            is GridComponent -> RenderGrid(component, formState, onStateChange, onAction)
            is BottomNavComponent -> RenderBottomNav(component, formState, onStateChange, onAction)
            is UnknownComponent -> RenderUnknown(component)
            else -> RenderUnknown(UnknownComponent(type = component.javaClass.simpleName, id = component.id, style = component.style))
        }
    }

    // ──────────────────────────────────────────────
    // Individual render functions
    // ──────────────────────────────────────────────

    @Composable
    private fun RenderPage(
        component: PageComponent,
        formState: Map<String, Any>,
        onStateChange: (String, Any) -> Unit,
        onAction: (UIAction) -> Unit,
    ) {
        Column(
            modifier = component.style.toContainerModifier().fillMaxSize(),
        ) {
            component.title?.let { title ->
                Text(
                    text = title,
                    modifier = Modifier.padding(16.dp),
                    style = MaterialTheme.typography.titleLarge,
                )
            }
            component.children.forEach { child ->
                RenderComponent(child, formState, onStateChange, onAction)
            }
        }
    }

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
    private fun RenderHero(component: HeroComponent, onAction: (UIAction) -> Unit) {
        val style = component.style
        Card(
            modifier = style.toModifier().padding(12.dp),
            shape = RoundedCornerShape(style?.cornerRadius?.dp ?: 16.dp),
        ) {
            Column(modifier = Modifier.fillMaxWidth()) {
                AsyncImage(
                    model = component.imageUrl,
                    contentDescription = component.title,
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(220.dp),
                    contentScale = ContentScale.Crop,
                )
                Column(modifier = Modifier.padding(12.dp)) {
                    component.title?.let {
                        Text(it, style = MaterialTheme.typography.titleLarge)
                    }
                    component.subtitle?.let {
                        Spacer(modifier = Modifier.height(6.dp))
                        Text(it, style = MaterialTheme.typography.bodyMedium)
                    }
                    Spacer(modifier = Modifier.height(12.dp))
                    Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                        component.primaryAction?.let { action ->
                            Button(onClick = { onAction(action) }) {
                                Text("Play")
                            }
                        }
                        component.secondaryAction?.let { action ->
                            OutlinedButton(onClick = { onAction(action) }) {
                                Text("My List")
                            }
                        }
                    }
                }
            }
        }
    }

    @Composable
    private fun RenderButton(
        component: ButtonComponent,
        formState: Map<String, Any>,
        onAction: (UIAction) -> Unit,
    ) {
        val style = component.style

        val isLoading = component.loadingKey
            ?.let { (formState[it] as? Boolean) == true }
            ?: false
        val isEnabled = component.enabledKey
            ?.let { (formState[it] as? Boolean) != false }
            ?: true

        Button(
            onClick = { onAction(component.action) },
            enabled = isEnabled && !isLoading,
            modifier = style.toModifier(),
            colors = ButtonDefaults.buttonColors(
                containerColor = StyleResolver.parseColor(style?.backgroundColor) ?: Color(0xFF6200EE),
            ),
            shape = RoundedCornerShape(style?.cornerRadius?.dp ?: 8.dp),
        ) {
            if (isLoading) {
                CircularProgressIndicator(
                    modifier = Modifier.size(18.dp),
                    strokeWidth = 2.dp,
                    color = style.textColor().let { if (it == Color.Black) Color.White else it },
                )
                Spacer(modifier = Modifier.width(10.dp))
            }
            val label = if (isEnabled) component.label else (component.disabledLabel ?: component.label)
            Text(
                text = label,
                color = style.textColor().let { if (it == Color.Black) Color.White else it },
                fontSize = style.fontSize(),
            )
        }
    }

    @Composable
    private fun RenderColumn(component: ColumnComponent, formState: Map<String, Any>, onStateChange: (String, Any) -> Unit, onAction: (UIAction) -> Unit) {
        Column(
            modifier = component.style.toContainerModifier(),
        ) {
            component.children.forEach { child ->
                RenderComponent(child, formState, onStateChange, onAction)
            }
        }
    }

    @Composable
    private fun RenderRow(component: RowComponent, formState: Map<String, Any>, onStateChange: (String, Any) -> Unit, onAction: (UIAction) -> Unit) {
        Row(
            modifier = component.style.toContainerModifier(),
        ) {
            component.children.forEach { child ->
                RenderComponent(child, formState, onStateChange, onAction)
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
    private fun RenderCard(component: CardComponent, formState: Map<String, Any>, onStateChange: (String, Any) -> Unit, onAction: (UIAction) -> Unit) {
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
            RenderComponent(component.child, formState, onStateChange, onAction)
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
    private fun RenderTextInput(
        component: TextInputComponent,
        formState: Map<String, Any>,
        onStateChange: (String, Any) -> Unit,
        onAction: (UIAction) -> Unit
    ) {
        val style = component.style
        val internalState = remember(component.id ?: "") { mutableStateOf(component.value) }
        val textValue = if (component.id != null) {
            formState[component.id] as? String ?: component.value
        } else {
            internalState.value
        }

        OutlinedTextField(
            value = textValue,
            onValueChange = { newValue ->
                internalState.value = newValue
                if (component.id != null) {
                    onStateChange(component.id, newValue)
                }
            },
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
    private fun RenderList(component: ListComponent, formState: Map<String, Any>, onStateChange: (String, Any) -> Unit, onAction: (UIAction) -> Unit) {
        LazyColumn(
            modifier = component.style.toContainerModifier()
                .then(Modifier.fillMaxSize()),
        ) {
            items(component.children) { child ->
                RenderComponent(child, formState, onStateChange, onAction)
            }
        }
    }

    @Composable
    private fun RenderCarousel(
        component: CarouselComponent,
        formState: Map<String, Any>,
        onStateChange: (String, Any) -> Unit,
        onAction: (UIAction) -> Unit,
    ) {
        Column(modifier = component.style.toContainerModifier()) {
            component.title?.let { title ->
                Text(
                    text = title,
                    modifier = Modifier.padding(horizontal = 16.dp, vertical = 8.dp),
                    style = MaterialTheme.typography.titleMedium,
                )
            }
            val itemSpacing = (component.itemSpacingDp ?: 12f).dp
            val padH = (component.contentPaddingHorizontalDp ?: 16f).dp
            LazyRow(
                contentPadding = PaddingValues(horizontal = padH),
                horizontalArrangement = Arrangement.spacedBy(itemSpacing),
            ) {
                items(component.children) { child ->
                    RenderComponent(child, formState, onStateChange, onAction)
                }
            }
        }
    }

    @OptIn(ExperimentalFoundationApi::class)
    @Composable
    private fun RenderGrid(
        component: GridComponent,
        formState: Map<String, Any>,
        onStateChange: (String, Any) -> Unit,
        onAction: (UIAction) -> Unit,
    ) {
        val columns = component.columns.coerceIn(1, 6)
        val h = (component.horizontalSpacingDp ?: 8f).dp
        val v = (component.verticalSpacingDp ?: 8f).dp
        LazyVerticalGrid(
            columns = GridCells.Fixed(columns),
            modifier = component.style.toContainerModifier().fillMaxSize(),
            contentPadding = PaddingValues(12.dp),
            horizontalArrangement = Arrangement.spacedBy(h),
            verticalArrangement = Arrangement.spacedBy(v),
        ) {
            items(component.children) { child ->
                RenderComponent(child, formState, onStateChange, onAction)
            }
        }
    }

    @Composable
    private fun RenderBottomNav(component: BottomNavComponent, formState: Map<String, Any>, onStateChange: (String, Any) -> Unit, onAction: (UIAction) -> Unit) {
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
                    RenderComponent(child, formState, onStateChange, onAction)
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
