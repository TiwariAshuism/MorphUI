package com.app.sdui.domain.model

sealed class UIAction {
    data class Navigate(val route: String, val params: Map<String, String>? = null) : UIAction()
    data class OpenUrl(val url: String) : UIAction()
    data class ShowToast(val message: String) : UIAction()
    object Back : UIAction()
    object None : UIAction()
}
