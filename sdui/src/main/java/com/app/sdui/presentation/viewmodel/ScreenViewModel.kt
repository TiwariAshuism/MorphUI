package com.app.sdui.presentation.viewmodel

import android.content.Context
import android.content.Intent
import android.net.Uri
import android.widget.Toast
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.app.sdui.core.UIAction
import com.app.sdui.core.UIComponent
import com.app.sdui.data.remote.FirebaseService
import com.app.sdui.debug.MorphUIInspector
import com.app.sdui.engine.MorphUIEngine
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch

class ScreenViewModel(
    private val engine: MorphUIEngine,
    private val firebaseService: FirebaseService,
    private val context: Context
) : ViewModel() {

    sealed class UiState {
        data object Loading : UiState()
        data class Success(val component: UIComponent) : UiState()
        data class Error(val message: String) : UiState()
    }

    private val _uiState = MutableStateFlow<UiState>(UiState.Loading)
    val uiState: StateFlow<UiState> = _uiState.asStateFlow()

    private val _formState = MutableStateFlow<Map<String, Any>>(emptyMap())
    val formState: StateFlow<Map<String, Any>> = _formState.asStateFlow()

    fun updateFormState(key: String, value: Any) {
        _formState.value = _formState.value + (key to value)
    }

    /** Runtime data context for data binding (e.g. user info). */
    private var bindingData: Map<String, Any> = emptyMap()

    val isDebugMode: Boolean get() = engine.config.debugMode

    fun setBindingData(data: Map<String, Any>) {
        bindingData = data
    }

    fun loadScreen(screenId: String) {
        android.util.Log.d("ScreenViewModel", "Loading screen: $screenId")
        viewModelScope.launch {
            _uiState.value = UiState.Loading
            _formState.value = emptyMap()

            // 1. Try cached content first for instant render
            engine.loadCached(screenId, bindingData)?.let { cached ->
                if (cached.isSuccess && cached.component != null) {
                    android.util.Log.d("ScreenViewModel", "Rendering cached screen: $screenId")
                    _uiState.value = UiState.Success(cached.component)
                }
            }

            // 2. Observe live updates from Firebase
            firebaseService.observeScreen(screenId)
                .catch { e ->
                    android.util.Log.e("ScreenViewModel", "Error loading screen: ${e.message}", e)
                    // Only show error if we don't already have cached content showing
                    if (_uiState.value !is UiState.Success) {
                        _uiState.value = UiState.Error(e.message ?: "Unknown error")
                    }
                }
                .collect { result ->
                    result.fold(
                        onSuccess = { dto ->
                            try {
                                // Convert DTO to raw map for the engine pipeline
                                val json = dtoToMap(dto)
                                val screenResult = engine.parseScreen(
                                    json = json,
                                    data = bindingData,
                                    screenId = screenId,
                                )

                                if (screenResult.isSuccess && screenResult.component != null) {
                                    android.util.Log.d(
                                        "ScreenViewModel",
                                        "Parsed screen: $screenId (v${screenResult.version})"
                                    )
                                    _uiState.value = UiState.Success(screenResult.component)
                                } else {
                                    val errMsg = screenResult.errors.joinToString("; ")
                                    android.util.Log.e("ScreenViewModel", "Parse errors: $errMsg")
                                    _uiState.value = UiState.Error(errMsg)
                                }
                            } catch (e: Exception) {
                                android.util.Log.e("ScreenViewModel", "Parse failed", e)
                                _uiState.value = UiState.Error(e.message ?: "Parse error")
                            }
                        },
                        onFailure = { error ->
                            android.util.Log.e("ScreenViewModel", "Firebase error: ${error.message}", error)
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

    fun handleApiCall(action: UIAction.ApiCall) {
        // TODO: Implement API call handling
        val finalBody = action.body.orEmpty() + _formState.value
        android.util.Log.d("ScreenViewModel", "API Call: ${action.method} ${action.endpoint} with body $finalBody")
        Toast.makeText(context, "API: ${action.method} ${action.endpoint}", Toast.LENGTH_SHORT).show()
    }

    fun handleCustomAction(action: UIAction.Custom) {
        android.util.Log.d("ScreenViewModel", "Custom action: ${action.name} params=${action.params}")
    }

    fun saveScreenData(screenId: String, data: Any) {
        firebaseService.setScreenData(screenId, data)
        Toast.makeText(context, "Data saved for $screenId", Toast.LENGTH_SHORT).show()
    }

    // ──────────────────────────────────────────────
    // DTO → Map conversion (bridges old Firebase DTOs to new engine)
    // ──────────────────────────────────────────────

    @Suppress("UNCHECKED_CAST")
    private fun dtoToMap(dto: com.app.sdui.data.dto.ComponentDto): Map<String, Any> {
        val map = mutableMapOf<String, Any>()
        map["type"] = dto.type
        if (dto.props.isNotEmpty()) map["props"] = dto.props
        dto.style?.let { map["style"] = it }
        dto.children?.let { children ->
            map["children"] = children.map { dtoToMap(it) }
        }
        dto.id?.let { map["id"] = it }
        return map
    }
}
