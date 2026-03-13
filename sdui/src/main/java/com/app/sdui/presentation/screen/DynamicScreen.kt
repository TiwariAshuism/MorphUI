package com.app.sdui.presentation.screen

import androidx.compose.foundation.layout.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.app.sdui.domain.model.UIAction
import com.app.sdui.domain.model.UIElement
import com.app.sdui.presentation.renderer.RendererRegistry
import com.app.sdui.presentation.viewmodel.ScreenViewModel

@Composable
fun DynamicScreen(
    screenId: String,
    viewModel: ScreenViewModel,
    onNavigate: (String, Map<String, String>?) -> Unit,
    onBack: () -> Unit
) {
    val uiState by viewModel.uiState.collectAsState()

    LaunchedEffect(screenId) {
        viewModel.loadScreen(screenId)
    }

    when (val state = uiState) {
        is ScreenViewModel.UiState.Loading -> {
            Box(
                modifier = Modifier.fillMaxSize(),
                contentAlignment = Alignment.Center
            ) {
                CircularProgressIndicator()
            }
        }
        
        is ScreenViewModel.UiState.Success -> {
            DynamicRenderer(
                element = state.element,
                onAction = { action ->
                    when (action) {
                        is UIAction.Navigate -> onNavigate(action.route, action.params)
                        is UIAction.OpenUrl -> viewModel.handleOpenUrl(action.url)
                        is UIAction.ShowToast -> viewModel.handleShowToast(action.message)
                        is UIAction.Back -> onBack()
                        UIAction.None -> {}
                    }
                }
            )
        }
        
        is ScreenViewModel.UiState.Error -> {
            Box(
                modifier = Modifier.fillMaxSize(),
                contentAlignment = Alignment.Center
            ) {
                Column(
                    horizontalAlignment = Alignment.CenterHorizontally,
                    modifier = Modifier.padding(16.dp)
                ) {
                    Text(
                        text = "Error loading screen",
                        style = MaterialTheme.typography.titleMedium,
                        color = MaterialTheme.colorScheme.error
                    )
                    Spacer(modifier = Modifier.height(8.dp))
                    Text(
                        text = state.message,
                        style = MaterialTheme.typography.bodyMedium
                    )
                    Spacer(modifier = Modifier.height(16.dp))
                    Button(onClick = { viewModel.loadScreen(screenId) }) {
                        Text("Retry")
                    }
                }
            }
        }
    }
}

@Composable
fun DynamicRenderer(
    element: UIElement,
    onAction: (UIAction) -> Unit
) {
    when (element) {
        is UIElement.Text -> RendererRegistry.get<UIElement.Text>("text")
            ?.Render(element, onAction)
        is UIElement.Image -> RendererRegistry.get<UIElement.Image>("image")
            ?.Render(element, onAction)
        is UIElement.Button -> RendererRegistry.get<UIElement.Button>("button")
            ?.Render(element, onAction)
        is UIElement.Column -> RendererRegistry.get<UIElement.Column>("column")
            ?.Render(element, onAction)
        is UIElement.Row -> RendererRegistry.get<UIElement.Row>("row")
            ?.Render(element, onAction)
        is UIElement.Spacer -> RendererRegistry.get<UIElement.Spacer>("spacer")
            ?.Render(element, onAction)
        is UIElement.Card -> RendererRegistry.get<UIElement.Card>("card")
            ?.Render(element, onAction)
        is UIElement.Divider -> RendererRegistry.get<UIElement.Divider>("divider")
            ?.Render(element, onAction)
        is UIElement.TextInput -> RendererRegistry.get<UIElement.TextInput>("text_input")
            ?.Render(element, onAction)
        is UIElement.IconButton -> RendererRegistry.get<UIElement.IconButton>("icon_button")
            ?.Render(element, onAction)
        is UIElement.ListContainer -> RendererRegistry.get<UIElement.ListContainer>("list")
            ?.Render(element, onAction)
        is UIElement.BottomNav -> RendererRegistry.get<UIElement.BottomNav>("bottom_nav")
            ?.Render(element, onAction)
        is UIElement.Unknown -> {
            Text(
                text = "Unknown component: ${element.type}",
                modifier = Modifier.padding(8.dp),
                color = androidx.compose.ui.graphics.Color.Red
            )
        }
    }
}
