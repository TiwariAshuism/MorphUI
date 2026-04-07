package com.app.sdui.data.remote.bff

import com.app.sdui.core.UIAction
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.JsonArray
import kotlinx.serialization.json.JsonElement
import kotlinx.serialization.json.JsonNull
import kotlinx.serialization.json.JsonObject
import kotlinx.serialization.json.JsonPrimitive
import okhttp3.HttpUrl
import okhttp3.HttpUrl.Companion.toHttpUrl
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.RequestBody.Companion.toRequestBody

/**
 * Executes [UIAction.ApiCall] against the BFF with an allowlist (Phase 6).
 *
 * - `/section/{id}` uses typed [SectionResponseDto] decoding (pagination).
 * - Other allowlisted paths use raw HTTP and return the response body as text.
 */
class ApiActionExecutor(
    private val baseUrl: HttpUrl,
    private val http: OkHttpClient,
    private val json: Json,
    private val bff: BffClient,
) {

    sealed class ExecutionResult {
        data class Section(
            val sectionId: String,
            val nextCursor: String?,
            val itemMaps: List<Map<String, Any>>,
        ) : ExecutionResult()

        data class HttpSuccess(val code: Int, val body: String) : ExecutionResult()
        data class HttpFailure(val code: Int, val message: String) : ExecutionResult()
    }

    suspend fun execute(
        action: UIAction.ApiCall,
        formState: Map<String, Any>,
        userId: String?,
        acceptLanguage: String?,
        /** Current cursor for this section (may be empty). */
        sectionCursor: String?,
    ): Result<ExecutionResult> = withContext(Dispatchers.IO) {
        runCatching {
            val mergedBody = (action.body.orEmpty() + formState).toMutableMap()
            val resolved = resolveUrl(action.endpoint)

            require(ApiAllowlist.isAllowed(baseUrl, resolved, action.method)) {
                "Blocked by allowlist: ${action.method} ${resolved.scheme}://${resolved.host}${resolved.encodedPath}"
            }

            val path = resolved.encodedPath

            if (path.startsWith("/section/") && action.method.equals("GET", ignoreCase = true)) {
                val sectionId = path.removePrefix("/section/").substringBefore("/")
                val cursorFromUrl = resolved.queryParameter("cursor")
                val cursor = cursorFromUrl ?: sectionCursor.orEmpty()
                val dto = bff.fetchSection(sectionId, cursor, userId, acceptLanguage)
                    .getOrElse { throw it }
                val maps = dto.items.map { item -> JsonElementMapper.toMap(item) }
                return@runCatching ExecutionResult.Section(
                    sectionId = dto.sectionId,
                    nextCursor = dto.nextCursor,
                    itemMaps = maps,
                )
            }

            val reqBuilder = Request.Builder().url(resolved).apply {
                if (!userId.isNullOrBlank()) header("X-User-Id", userId)
                if (!acceptLanguage.isNullOrBlank()) header("Accept-Language", acceptLanguage)
            }

            when (action.method.uppercase()) {
                "GET" -> reqBuilder.get()
                "POST", "PUT", "PATCH" -> {
                    val jsonStr = json.encodeToString(
                        JsonElement.serializer(),
                        mergedBody.toJsonElement(),
                    )
                    val body = jsonStr.toRequestBody(JSON_MEDIA)
                    reqBuilder.method(action.method.uppercase(), body)
                }
                "DELETE" -> reqBuilder.delete()
                else -> throw IllegalArgumentException("Unsupported method: ${action.method}")
            }

            http.newCall(reqBuilder.build()).execute().use { resp ->
                val body = resp.body?.string().orEmpty()
                if (!resp.isSuccessful) {
                    return@runCatching ExecutionResult.HttpFailure(
                        resp.code,
                        body.ifBlank { resp.message },
                    )
                }
                ExecutionResult.HttpSuccess(resp.code, body)
            }
        }
    }

    private fun resolveUrl(endpoint: String): HttpUrl {
        val e = endpoint.trim()
        return if (e.startsWith("http://", ignoreCase = true) || e.startsWith("https://", ignoreCase = true)) {
            e.toHttpUrl()
        } else {
            val relative = if (e.startsWith("/")) e else "/$e"
            baseUrl.resolve(relative) ?: throw IllegalArgumentException("Cannot resolve $relative against $baseUrl")
        }
    }

    private fun Map<String, Any>.toJsonElement(): JsonObject {
        return JsonObject(mapValues { (_, v) -> anyToJsonElement(v) })
    }

    private fun anyToJsonElement(v: Any?): JsonElement {
        return when (v) {
            null -> JsonNull
            is Map<*, *> -> {
                @Suppress("UNCHECKED_CAST")
                (v as Map<String, Any>).toJsonElement()
            }
            is List<*> -> JsonArray(v.map { anyToJsonElement(it) })
            is Boolean -> JsonPrimitive(v)
            is Number -> JsonPrimitive(v)
            is String -> JsonPrimitive(v)
            else -> JsonPrimitive(v.toString())
        }
    }

    companion object {
        private val JSON_MEDIA = "application/json; charset=utf-8".toMediaType()
    }
}
