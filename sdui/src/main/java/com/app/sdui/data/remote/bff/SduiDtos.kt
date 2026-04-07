package com.app.sdui.data.remote.bff

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable
import kotlinx.serialization.json.JsonObject

/**
 * Typed SDUI envelope matching the Go BFF contract (Phase 2/3).
 *
 * The Android engine currently consumes a Map-based representation.
 * We decode the envelope with kotlinx.serialization (typed), then convert
 * the [screen] JsonObject into Map<String, Any> for MorphUIEngine.parseScreen.
 */
@Serializable
data class SduiEnvelopeDto(
    @SerialName("schema_version") val schemaVersion: String? = null,
    @SerialName("ui_version") val uiVersion: Int = 1,
    @SerialName("page_id") val pageId: String? = null,
    @SerialName("ttl_ms") val ttlMs: Long? = null,
    @SerialName("trace_id") val traceId: String? = null,
    @SerialName("server_time_ms") val serverTimeMs: Long? = null,
    @SerialName("experiments") val experiments: Map<String, String>? = null,
    @SerialName("feature_flags") val featureFlags: Map<String, Boolean>? = null,
    @SerialName("errors") val errors: List<String>? = null,
    // MorphUIEngine expects a "screen" component tree.
    @SerialName("screen") val screen: JsonObject? = null,
    // Optional alias used by schema docs.
    @SerialName("page") val page: JsonObject? = null,
    @SerialName("fallback_page") val fallbackPage: JsonObject? = null,
)

@Serializable
data class SectionResponseDto(
    @SerialName("schema_version") val schemaVersion: String? = null,
    @SerialName("ui_version") val uiVersion: Int = 1,
    @SerialName("section_id") val sectionId: String,
    @SerialName("next_cursor") val nextCursor: String? = null,
    @SerialName("ttl_ms") val ttlMs: Long? = null,
    @SerialName("items") val items: List<JsonObject> = emptyList(),
    @SerialName("trace_id") val traceId: String? = null,
    @SerialName("server_time_ms") val serverTimeMs: Long? = null,
    @SerialName("errors") val errors: List<String>? = null,
)

