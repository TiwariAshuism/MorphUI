package com.app.sdui.presentation.viewmodel

import android.content.Context
import android.content.Intent
import android.net.Uri
import android.widget.Toast
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.app.sdui.domain.model.UIElement
import com.app.sdui.domain.usecase.GetScreenUseCase
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch

class ScreenViewModel(
    private val getScreenUseCase: GetScreenUseCase,
    private val context: Context
) : ViewModel() {

    sealed class UiState {
        object Loading : UiState()
        data class Success(val element: UIElement) : UiState()
        data class Error(val message: String) : UiState()
    }

    private val _uiState = MutableStateFlow<UiState>(UiState.Loading)
    val uiState: StateFlow<UiState> = _uiState.asStateFlow()

    fun loadScreen(screenId: String) {
        android.util.Log.d("ScreenViewModel", "Loading screen: $screenId")
        viewModelScope.launch {
            _uiState.value = UiState.Loading
            
            // Try to show cached content immediately
            getScreenUseCase.getCached(screenId)?.let { cached ->
                android.util.Log.d("ScreenViewModel", "Found cached content for: $screenId")
                _uiState.value = UiState.Success(cached)
            }

            // Observe live updates
            getScreenUseCase(screenId)
                .catch { e ->
                    android.util.Log.e("ScreenViewModel", "Error loading screen: ${e.message}", e)
                    _uiState.value = UiState.Error(e.message ?: "Unknown error")
                }
                .collect { result ->
                    result.fold(
                        onSuccess = { element ->
                            android.util.Log.d("ScreenViewModel", "Successfully loaded screen: $screenId")
                            _uiState.value = UiState.Success(element)
                        },
                        onFailure = { error ->
                            android.util.Log.e("ScreenViewModel", "Failed to load screen: ${error.message}", error)
                            _uiState.value = UiState.Error(error.message ?: "Unknown error")
                        }
                    )
                }
        }
    }

    fun handleOpenUrl(url: String) {
        try {
            val intent = Intent(Intent.ACTION_VIEW, Uri.parse(url))
            intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
            context.startActivity(intent)
        } catch (e: Exception) {
            Toast.makeText(context, "Cannot open URL", Toast.LENGTH_SHORT).show()
        }
    }

    fun handleShowToast(message: String) {
        Toast.makeText(context, message, Toast.LENGTH_SHORT).show()
    }
}
