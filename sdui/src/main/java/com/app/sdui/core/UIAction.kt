package com.app.sdui.core

/**
 * Sealed interface for all MorphUI actions.
 *
 * Actions represent user interactions that the host app must handle.
 * The sealed hierarchy enables exhaustive handling in action dispatchers.
 */
sealed interface UIAction {

    data class Navigate(
        val route: String,
        val params: Map<String, String>? = null,
    ) : UIAction

    data class OpenUrl(
        val url: String,
    ) : UIAction

    data class ShowToast(
        val message: String,
    ) : UIAction

    data class ApiCall(
        val endpoint: String,
        val method: String = "GET",
        val body: Map<String, Any>? = null,
        val onSuccess: UIAction? = null,
        val onError: UIAction? = null,
    ) : UIAction

    data class Custom(
        val name: String,
        val params: Map<String, Any>? = null,
    ) : UIAction

    data object Back : UIAction

    data object None : UIAction
}
