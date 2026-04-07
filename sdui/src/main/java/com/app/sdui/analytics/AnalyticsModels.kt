package com.app.sdui.analytics

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

@Serializable
data class EventsIngestRequest(
    @SerialName("events") val events: List<ClientEvent>,
)

@Serializable
data class ClientEvent(
    @SerialName("event_name") val eventName: String,
    @SerialName("screen_id") val screenId: String? = null,
    @SerialName("component_id") val componentId: String? = null,
    @SerialName("action_type") val actionType: String? = null,
    @SerialName("attrs") val attrs: Map<String, String>? = null,
    @SerialName("ts_ms") val tsMs: Long? = null,
)
