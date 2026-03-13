package com.app.sdui.parser

import com.app.sdui.core.UIAction

/**
 * Parses JSON action maps into strongly typed [UIAction] instances.
 */
object ActionParser {

    @Suppress("UNCHECKED_CAST")
    fun parse(actionData: Any?): UIAction {
        if (actionData == null) return UIAction.None

        val map = actionData as? Map<String, Any> ?: return UIAction.None

        return when (val type = map["type"] as? String) {
            "Navigate" -> UIAction.Navigate(
                route = map["route"] as? String ?: "",
                params = map["params"] as? Map<String, String>,
            )

            "OpenUrl" -> UIAction.OpenUrl(
                url = map["url"] as? String ?: "",
            )

            "ShowToast" -> UIAction.ShowToast(
                message = map["message"] as? String ?: "",
            )

            "ApiCall" -> UIAction.ApiCall(
                endpoint = map["endpoint"] as? String ?: "",
                method = map["method"] as? String ?: "GET",
                body = map["body"] as? Map<String, Any>,
                onSuccess = parse(map["onSuccess"]),
                onError = parse(map["onError"]),
            )

            "Custom" -> UIAction.Custom(
                name = map["name"] as? String ?: "",
                params = map["params"] as? Map<String, Any>,
            )

            "Back" -> UIAction.Back

            else -> {
                android.util.Log.w("ActionParser", "Unknown action type: $type")
                UIAction.None
            }
        }
    }
}
