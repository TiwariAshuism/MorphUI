package com.app.sdui.analytics

import com.app.sdui.core.UIAction
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import kotlinx.serialization.json.Json
import okhttp3.HttpUrl
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.RequestBody.Companion.toRequestBody

/**
 * Posts client events to the BFF (`POST /api/events`).
 *
 * Phase 8:
 * - persists pending batches across process death
 * - retries failed posts with exponential backoff
 * - dedupes impressions per (screenId, componentId) for the app session
 */
class BffActionAnalytics(
    private val baseUrl: HttpUrl,
    private val http: OkHttpClient,
    private val json: Json,
    private val userIdProvider: () -> String?,
    private val queue: AnalyticsEventQueue,
) : ActionAnalytics {

    private val scope = CoroutineScope(Dispatchers.IO)
    private val mutex = Mutex()
    private val buffer = ArrayList<ClientEvent>(64)
    private val impressionKeys = mutableSetOf<String>()
    private var flushJob: Job? = null

    init {
        scope.launch {
            mutex.withLock {
                buffer.addAll(queue.load())
                if (buffer.isNotEmpty()) {
                    scheduleFlushLocked()
                }
            }
        }
    }

    override fun onActionTap(action: UIAction) {
        val (name, attrs) = when (action) {
            is UIAction.Navigate -> "action_tap" to mapOf("action" to "navigate", "route" to action.route)
            is UIAction.ApiCall -> "action_tap" to mapOf("action" to "api_call", "endpoint" to action.endpoint)
            is UIAction.OpenUrl -> "action_tap" to mapOf("action" to "open_url", "url" to action.url)
            is UIAction.ShowToast -> "action_tap" to mapOf("action" to "show_toast")
            is UIAction.Custom -> "action_tap" to mapOf("action" to "custom", "name" to action.name)
            UIAction.Back -> "action_tap" to mapOf("action" to "back")
            UIAction.None -> return
        }
        enqueue(
            ClientEvent(
                eventName = name,
                actionType = action.javaClass.simpleName,
                attrs = attrs,
            ),
        )
    }

    override fun onApiCallStart(method: String, endpoint: String) {
        enqueue(
            ClientEvent(
                eventName = "api_call_start",
                actionType = "ApiCall",
                attrs = mapOf("method" to method, "endpoint" to endpoint),
            ),
        )
    }

    override fun onApiCallSuccess(method: String, endpoint: String, httpCode: Int?) {
        enqueue(
            ClientEvent(
                eventName = "api_call_success",
                actionType = "ApiCall",
                attrs = mapOf(
                    "method" to method,
                    "endpoint" to endpoint,
                    "code" to (httpCode?.toString() ?: ""),
                ),
            ),
        )
    }

    override fun onApiCallFailure(method: String, endpoint: String, httpCode: Int?, message: String?) {
        enqueue(
            ClientEvent(
                eventName = "api_call_failure",
                actionType = "ApiCall",
                attrs = mapOf(
                    "method" to method,
                    "endpoint" to endpoint,
                    "code" to (httpCode?.toString() ?: ""),
                    "message" to (message ?: ""),
                ),
            ),
        )
    }

    override fun onImpression(screenId: String, componentId: String, componentType: String) {
        val key = "$screenId|$componentId"
        synchronized(impressionKeys) {
            if (!impressionKeys.add(key)) return
        }
        enqueue(
            ClientEvent(
                eventName = "impression",
                screenId = screenId,
                componentId = componentId,
                actionType = componentType,
            ),
        )
    }

    private fun enqueue(ev: ClientEvent) {
        scope.launch {
            mutex.withLock {
                buffer.add(ev.copy(tsMs = System.currentTimeMillis()))
                persistLocked()
                if (buffer.size >= 25) {
                    flushLocked()
                    return@withLock
                }
                scheduleFlushLocked()
            }
        }
    }

    private fun persistLocked() {
        queue.save(buffer.toList())
    }

    private fun scheduleFlushLocked() {
        if (flushJob?.isActive == true) return
        flushJob = scope.launch {
            delay(1500)
            mutex.withLock { flushLocked() }
        }
    }

    private fun flushLocked() {
        if (buffer.isEmpty()) return
        val toSend = buffer.toList()
        buffer.clear()
        persistLocked()

        scope.launch {
            val ok = postWithRetry(toSend)
            if (!ok) {
                mutex.withLock {
                    buffer.addAll(0, toSend)
                    persistLocked()
                }
            }
        }
    }

    private suspend fun postWithRetry(events: List<ClientEvent>): Boolean {
        val url = baseUrl.newBuilder()
            .addPathSegments("api/events")
            .build()
        val payload = json.encodeToString(
            EventsIngestRequest.serializer(),
            EventsIngestRequest(events = events),
        )
        repeat(3) { attempt ->
            try {
                val req = Request.Builder()
                    .url(url)
                    .apply {
                        val uid = userIdProvider()
                        if (!uid.isNullOrBlank()) header("X-User-Id", uid)
                    }
                    .post(payload.toRequestBody(JSON_MEDIA))
                    .build()
                val success = http.newCall(req).execute().use { it.isSuccessful }
                if (success) return true
            } catch (_: Exception) {
                // retry
            }
            delay(200L shl attempt)
        }
        return false
    }

    private companion object {
        private val JSON_MEDIA = "application/json; charset=utf-8".toMediaType()
    }
}
