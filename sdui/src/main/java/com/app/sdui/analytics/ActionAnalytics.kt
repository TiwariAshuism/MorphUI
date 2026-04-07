package com.app.sdui.analytics

import com.app.sdui.core.UIAction

/**
 * Minimal analytics hook for Phase 6 actions.
 *
 * This is intentionally lightweight (no SDK dependency) and can be wired
 * to Firebase Analytics, OpenTelemetry, or a custom pipeline in Phase 8.
 */
interface ActionAnalytics {

    fun onActionTap(action: UIAction)

    fun onApiCallStart(method: String, endpoint: String)

    fun onApiCallSuccess(method: String, endpoint: String, httpCode: Int?)

    fun onApiCallFailure(method: String, endpoint: String, httpCode: Int?, message: String?)

    /**
     * Best-effort impression event.
     *
     * Phase 7 uses component ids (when present) for dedupe on the backend.
     */
    fun onImpression(screenId: String, componentId: String, componentType: String)

    object Noop : ActionAnalytics {
        override fun onActionTap(action: UIAction) = Unit

        override fun onApiCallStart(method: String, endpoint: String) = Unit

        override fun onApiCallSuccess(method: String, endpoint: String, httpCode: Int?) = Unit

        override fun onApiCallFailure(
            method: String,
            endpoint: String,
            httpCode: Int?,
            message: String?,
        ) = Unit

        override fun onImpression(screenId: String, componentId: String, componentType: String) = Unit
    }
}

