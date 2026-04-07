package com.app.sdui.data.remote.bff

import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import kotlinx.serialization.json.Json
import okhttp3.HttpUrl
import okhttp3.OkHttpClient
import okhttp3.Request

/**
 * Minimal BFF HTTP client (Phase 4).
 *
 * - Uses OkHttp + kotlinx.serialization
 * - Decodes typed envelopes, then converts screen JsonObject to Map<String, Any>
 *   for the existing MorphUIEngine parsing pipeline.
 */
class BffClient(
    private val baseUrl: HttpUrl,
    private val http: OkHttpClient,
    private val json: Json,
) {

    suspend fun fetchHome(userId: String?, acceptLanguage: String?): Result<Map<String, Any>> {
        val url = baseUrl.newBuilder()
            .addPathSegment("home")
            .build()

        return fetchEnvelope(url, userId, acceptLanguage)
    }

    suspend fun fetchSection(
        sectionId: String,
        cursor: String?,
        userId: String?,
        acceptLanguage: String?,
    ): Result<SectionResponseDto> {
        val b = baseUrl.newBuilder()
            .addPathSegment("section")
            .addPathSegment(sectionId)
        if (!cursor.isNullOrBlank()) {
            b.addQueryParameter("cursor", cursor)
        }
        val url = b.build()

        return withContext(Dispatchers.IO) {
            runCatching {
                val req = Request.Builder()
                    .url(url)
                    .apply {
                        if (!userId.isNullOrBlank()) header("X-User-Id", userId)
                        if (!acceptLanguage.isNullOrBlank()) header("Accept-Language", acceptLanguage)
                    }
                    .get()
                    .build()

                http.newCall(req).execute().use { resp ->
                    if (!resp.isSuccessful) {
                        throw IllegalStateException("BFF /section failed: ${resp.code}")
                    }
                    val body = resp.body?.string() ?: throw IllegalStateException("Empty body")
                    json.decodeFromString(SectionResponseDto.serializer(), body)
                }
            }
        }
    }

    private suspend fun fetchEnvelope(
        url: HttpUrl,
        userId: String?,
        acceptLanguage: String?,
    ): Result<Map<String, Any>> {
        return withContext(Dispatchers.IO) {
            runCatching {
                val req = Request.Builder()
                    .url(url)
                    .apply {
                        if (!userId.isNullOrBlank()) header("X-User-Id", userId)
                        if (!acceptLanguage.isNullOrBlank()) header("Accept-Language", acceptLanguage)
                    }
                    .get()
                    .build()

                http.newCall(req).execute().use { resp ->
                    if (!resp.isSuccessful) {
                        throw IllegalStateException("BFF /home failed: ${resp.code}")
                    }
                    val body = resp.body?.string() ?: throw IllegalStateException("Empty body")
                    val dto = json.decodeFromString(SduiEnvelopeDto.serializer(), body)
                    val screen = dto.screen ?: dto.page
                        ?: throw IllegalStateException("Missing 'screen' (or 'page') in envelope")

                    // Re-wrap into the exact Map shape MorphUIEngine expects:
                    // { "ui_version": <int>, "screen": <component-map>, ... }
                    val root = LinkedHashMap<String, Any>()
                    root["ui_version"] = dto.uiVersion
                    root["page_id"] = dto.pageId ?: "home"
                    dto.ttlMs?.let { root["ttl_ms"] = it }
                    dto.traceId?.let { root["trace_id"] = it }
                    dto.serverTimeMs?.let { root["server_time_ms"] = it }
                    dto.experiments?.let { root["experiments"] = it }
                    dto.featureFlags?.let { root["feature_flags"] = it }
                    dto.errors?.let { root["errors"] = it }
                    root["screen"] = JsonElementMapper.toMap(screen)
                    root
                }
            }
        }
    }
}

