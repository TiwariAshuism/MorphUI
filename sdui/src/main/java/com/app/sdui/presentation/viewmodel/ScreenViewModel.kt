package com.app.sdui.presentation.viewmodel

import android.content.Context
import android.content.Intent
import android.net.Uri
import android.widget.Toast
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.app.sdui.analytics.ActionAnalytics
import com.app.sdui.core.UIAction
import com.app.sdui.core.UIComponent
import com.app.sdui.data.remote.bff.ApiActionExecutor
import com.app.sdui.data.repository.ScreenRepository
import com.app.sdui.engine.MorphUIEngine
import com.app.sdui.presentation.tree.appendIntoRail
import com.app.sdui.presentation.tree.removeLoadMoreFromRail
import kotlinx.coroutines.channels.BufferOverflow
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharedFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asSharedFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.launch

class ScreenViewModel(
    private val engine: MorphUIEngine,
    private val screens: ScreenRepository,
    private val context: Context,
    private val apiExecutor: ApiActionExecutor,
    private val analytics: ActionAnalytics,
    private val userIdProvider: () -> String?,
    private val acceptLanguageProvider: () -> String?,
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

    /** Emits actions that must run after async work (e.g. [UIAction.Navigate] from [UIAction.ApiCall.onSuccess]). */
    private val _uiEvents = MutableSharedFlow<UIAction>(
        extraBufferCapacity = 32,
        onBufferOverflow = BufferOverflow.DROP_OLDEST,
    )
    val uiEvents: SharedFlow<UIAction> = _uiEvents.asSharedFlow()

    /** Per-section pagination cursor for `/section/{id}` (empty / absent = first page). */
    private val sectionCursors = mutableMapOf<String, String>()

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
            sectionCursors.clear()

            // 1. Try cached content first for instant render
            engine.loadCached(screenId, bindingData)?.let { cached ->
                if (cached.isSuccess && cached.component != null) {
                    android.util.Log.d("ScreenViewModel", "Rendering cached screen: $screenId")
                    _uiState.value = UiState.Success(cached.component)
                    emitImpressions(screenId, cached.component)
                }
            }

            // 2. Observe live updates from configured source (BFF or Firebase)
            screens.observeScreen(screenId)
                .catch { e ->
                    android.util.Log.e("ScreenViewModel", "Error loading screen: ${e.message}", e)
                    if (_uiState.value !is UiState.Success) {
                        _uiState.value = UiState.Error(e.message ?: "Unknown error")
                    }
                }
                .collect { result ->
                    result.fold(
                        onSuccess = { json ->
                            try {
                                val screenResult = engine.parseScreen(
                                    json = json,
                                    data = bindingData,
                                    screenId = screenId,
                                )

                                if (screenResult.isSuccess && screenResult.component != null) {
                                    android.util.Log.d(
                                        "ScreenViewModel",
                                        "Parsed screen: $screenId (v${screenResult.version})",
                                    )
                                    _uiState.value = UiState.Success(screenResult.component)
                                    emitImpressions(screenId, screenResult.component)
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
                            android.util.Log.e(
                                "ScreenViewModel",
                                "Screen source error: ${error.message}",
                                error,
                            )
                            _uiState.value = UiState.Error(error.message ?: "Unknown error")
                        },
                    )
                }
        }
    }

    fun handleOpenUrl(url: String) {
        if (!isAllowedExternalUrl(url)) {
            Toast.makeText(context, "URL not allowed", Toast.LENGTH_SHORT).show()
            return
        }
        try {
            val intent = Intent(Intent.ACTION_VIEW, Uri.parse(url))
            intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
            context.startActivity(intent)
        } catch (e: Exception) {
            Toast.makeText(context, "Cannot open URL", Toast.LENGTH_SHORT).show()
        }
    }

    private fun isAllowedExternalUrl(url: String): Boolean {
        val uri = Uri.parse(url)
        val scheme = uri.scheme?.lowercase() ?: return false
        return scheme == "http" || scheme == "https"
    }

    fun handleShowToast(message: String) {
        Toast.makeText(context, message, Toast.LENGTH_SHORT).show()
    }

    fun handleApiCall(action: UIAction.ApiCall) {
        viewModelScope.launch {
            analytics.onActionTap(action)
            executeApiCall(action)
        }
    }

    private suspend fun executeApiCall(action: UIAction.ApiCall) {
        val form = _formState.value
        val sectionHint = sectionIdFromEndpoint(action.endpoint)
        val cursor = sectionHint?.let { sectionCursors[it] }
        sectionHint?.let { setSectionLoading(it, isLoading = true) }

        analytics.onApiCallStart(action.method, action.endpoint)
        val result = apiExecutor.execute(
            action = action,
            formState = form,
            userId = userIdProvider(),
            acceptLanguage = acceptLanguageProvider(),
            sectionCursor = cursor,
        )

        result.fold(
            onSuccess = { exec ->
                when (exec) {
                    is ApiActionExecutor.ExecutionResult.Section -> {
                        analytics.onApiCallSuccess(action.method, action.endpoint, 200)
                        if (!exec.nextCursor.isNullOrBlank()) {
                            sectionCursors[exec.sectionId] = exec.nextCursor
                        } else {
                            sectionCursors.remove(exec.sectionId)
                        }
                        mergeSectionItems(exec.sectionId, exec.itemMaps)
                        if (exec.nextCursor.isNullOrBlank()) {
                            removeLoadMore(exec.sectionId)
                            setSectionEnabled(exec.sectionId, isEnabled = false)
                        }
                        emitUiAction(action.onSuccess)
                    }
                    is ApiActionExecutor.ExecutionResult.HttpSuccess -> {
                        analytics.onApiCallSuccess(action.method, action.endpoint, exec.code)
                        emitUiAction(action.onSuccess)
                    }
                    is ApiActionExecutor.ExecutionResult.HttpFailure -> {
                        android.util.Log.w(
                            "ScreenViewModel",
                            "API error ${exec.code}: ${exec.message}",
                        )
                        analytics.onApiCallFailure(
                            action.method,
                            action.endpoint,
                            exec.code,
                            exec.message,
                        )
                        if (action.onError == null) {
                            handleShowToast("Request failed (${exec.code})")
                        }
                        emitUiAction(action.onError)
                        if (action.onRetry != null) {
                            // Optional: server can provide an explicit retry action.
                            // The UI can surface this in the future; for now we just keep it available.
                        }
                    }
                }
            },
            onFailure = { e ->
                android.util.Log.e("ScreenViewModel", "API call failed", e)
                analytics.onApiCallFailure(action.method, action.endpoint, null, e.message)
                if (action.onError == null) {
                    handleShowToast(e.message ?: "Request failed")
                }
                emitUiAction(action.onError)
            },
        )

        sectionHint?.let { setSectionLoading(it, isLoading = false) }
    }

    private suspend fun emitUiAction(action: UIAction?) {
        when (action) {
            null -> {}
            is UIAction.ApiCall -> executeApiCall(action)
            else -> _uiEvents.emit(action)
        }
    }

    private fun mergeSectionItems(sectionId: String, itemMaps: List<Map<String, Any>>) {
        if (itemMaps.isEmpty()) return
        val current = (_uiState.value as? UiState.Success)?.component ?: return
        val railId = "rail_$sectionId"
        val newItems = itemMaps.mapNotNull { map ->
            try {
                engine.parseComponent(map, bindingData)
            } catch (e: Exception) {
                android.util.Log.e("ScreenViewModel", "Failed to parse section item", e)
                null
            }
        }
        if (newItems.isEmpty()) return
        val merged = appendIntoRail(current, railId, newItems)
        _uiState.value = UiState.Success(merged)
        emitImpressions("home", merged)
    }

    private fun removeLoadMore(sectionId: String) {
        val current = (_uiState.value as? UiState.Success)?.component ?: return
        val railId = "rail_$sectionId"
        val merged = removeLoadMoreFromRail(current, railId)
        _uiState.value = UiState.Success(merged)
        emitImpressions("home", merged)
    }

    private fun emitImpressions(screenId: String, root: UIComponent) {
        // Best-effort: emit impressions for components that have ids.
        // Dedupe is handled client-side per screen render to keep event volume sane.
        val seen = HashSet<String>()
        fun walk(c: UIComponent) {
            val id = c.id
            if (!id.isNullOrBlank() && seen.add(id)) {
                analytics.onImpression(screenId, id, c.javaClass.simpleName)
            }
            when (c) {
                is com.app.sdui.components.PageComponent -> c.children.forEach(::walk)
                is com.app.sdui.components.ListComponent -> c.children.forEach(::walk)
                is com.app.sdui.components.ColumnComponent -> c.children.forEach(::walk)
                is com.app.sdui.components.RowComponent -> c.children.forEach(::walk)
                is com.app.sdui.components.CarouselComponent -> c.children.forEach(::walk)
                is com.app.sdui.components.GridComponent -> c.children.forEach(::walk)
                is com.app.sdui.components.CardComponent -> walk(c.child)
                else -> {}
            }
        }
        walk(root)
    }

    private fun setSectionLoading(sectionId: String, isLoading: Boolean) {
        val k = "loading:section:$sectionId"
        _formState.value = _formState.value + (k to isLoading)
        if (isLoading) {
            setSectionEnabled(sectionId, isEnabled = false)
        }
    }

    private fun setSectionEnabled(sectionId: String, isEnabled: Boolean) {
        val k = "enabled:section:$sectionId"
        _formState.value = _formState.value + (k to isEnabled)
    }

    private fun sectionIdFromEndpoint(endpoint: String): String? {
        val e = endpoint.trim()
        val path = when {
            e.startsWith("http://", true) || e.startsWith("https://", true) ->
                Uri.parse(e).path ?: return null
            else -> if (e.startsWith("/")) e else "/$e"
        }
        if (!path.startsWith("/section/")) return null
        return path.removePrefix("/section/").substringBefore("/").takeIf { it.isNotBlank() }
    }

    fun handleCustomAction(action: UIAction.Custom) {
        analytics.onActionTap(action)
        android.util.Log.d("ScreenViewModel", "Custom action: ${action.name} params=${action.params}")
    }
}
