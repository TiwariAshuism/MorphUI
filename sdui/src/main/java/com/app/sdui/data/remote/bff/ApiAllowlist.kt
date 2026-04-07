package com.app.sdui.data.remote.bff

import okhttp3.HttpUrl

/**
 * Centralized BFF allowlist rules (testable without executing HTTP).
 */
object ApiAllowlist {

    fun isAllowed(baseUrl: HttpUrl, resolved: HttpUrl, method: String): Boolean {
        val sameOrigin =
            resolved.scheme == baseUrl.scheme &&
                resolved.host == baseUrl.host &&
                resolved.port == baseUrl.port
        if (!sameOrigin) return false

        val m = method.uppercase()
        val p = resolved.encodedPath

        return when {
            p == "/healthz" -> m == "GET"
            p == "/home" -> m == "GET"
            p.startsWith("/section/") -> m == "GET"
            p.startsWith("/api/") -> m in setOf("GET", "POST", "PUT", "PATCH", "DELETE")
            else -> false
        }
    }
}
