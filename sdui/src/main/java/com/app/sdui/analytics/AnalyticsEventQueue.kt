package com.app.sdui.analytics

import android.content.Context
import kotlinx.serialization.builtins.ListSerializer
import kotlinx.serialization.json.Json

/**
 * Persists undelivered analytics events across process death (Phase 8).
 */
class AnalyticsEventQueue(
    private val context: Context,
    private val json: Json,
) {

    private val prefs = context.getSharedPreferences(PREFS, Context.MODE_PRIVATE)

    fun load(): List<ClientEvent> {
        val raw = prefs.getString(KEY_PENDING, null) ?: return emptyList()
        return runCatching {
            json.decodeFromString(ListSerializer(ClientEvent.serializer()), raw)
        }.getOrElse { emptyList() }
    }

    fun save(events: List<ClientEvent>) {
        if (events.isEmpty()) {
            prefs.edit().remove(KEY_PENDING).apply()
            return
        }
        val raw = json.encodeToString(ListSerializer(ClientEvent.serializer()), events)
        prefs.edit().putString(KEY_PENDING, raw).apply()
    }

    companion object {
        private const val PREFS = "morphui.analytics.queue"
        private const val KEY_PENDING = "pending_events_json"
    }
}
