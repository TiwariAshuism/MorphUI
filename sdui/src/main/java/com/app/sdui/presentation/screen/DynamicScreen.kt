package com.app.sdui.presentation.screen

import androidx.compose.foundation.layout.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.app.sdui.core.UIAction
import com.app.sdui.debug.MorphUIInspector
import com.app.sdui.renderer.ComposeRenderer
import com.app.sdui.presentation.viewmodel.ScreenViewModel
import kotlinx.coroutines.flow.collectLatest

@Composable
fun DynamicScreen(
    screenId: String,
    viewModel: ScreenViewModel,
    onNavigate: (String, Map<String, String>?) -> Unit,
    onBack: () -> Unit
) {
    val uiState by viewModel.uiState.collectAsState()
    val formState by viewModel.formState.collectAsState()
    val navigate by rememberUpdatedState(onNavigate)
    val back by rememberUpdatedState(onBack)

    LaunchedEffect(screenId) {
        viewModel.loadScreen(screenId)
    }

    LaunchedEffect(Unit) {
        viewModel.uiEvents.collectLatest { action ->
            when (action) {
                is UIAction.Navigate -> navigate(action.route, action.params)
                is UIAction.OpenUrl -> viewModel.handleOpenUrl(action.url)
                is UIAction.ShowToast -> viewModel.handleShowToast(action.message)
                is UIAction.Back -> back()
                is UIAction.ApiCall -> viewModel.handleApiCall(action)
                is UIAction.Custom -> viewModel.handleCustomAction(action)
                UIAction.None -> {}
            }
        }
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
            // Debug tree logging
            if (viewModel.isDebugMode) {
                LaunchedEffect(state.component) {
                    MorphUIInspector.logTree(state.component)
                }
            }

            ComposeRenderer.RenderComponent(
                component = state.component,
                formState = formState,
                onStateChange = viewModel::updateFormState,
                onAction = { action ->
                    when (action) {
                        is UIAction.Navigate -> onNavigate(action.route, action.params)
                        is UIAction.OpenUrl -> viewModel.handleOpenUrl(action.url)
                        is UIAction.ShowToast -> viewModel.handleShowToast(action.message)
                        is UIAction.Back -> onBack()
                        is UIAction.ApiCall -> viewModel.handleApiCall(action)
                        is UIAction.Custom -> viewModel.handleCustomAction(action)
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
