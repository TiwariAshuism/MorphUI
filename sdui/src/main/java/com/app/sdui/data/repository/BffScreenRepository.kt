package com.app.sdui.data.repository

import com.app.sdui.data.remote.bff.BffClient
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flow

/**
 * Screen source backed by the Go BFF (Phase 4).
 *
 * For now we support:
 * - screenId == "home" → GET /home
 *
 * Later phases can add:
 * - screenId patterns → /details, /section, etc.
 */
class BffScreenRepository(
    private val bff: BffClient,
    private val userIdProvider: () -> String?,
    private val acceptLanguageProvider: () -> String?,
) : ScreenRepository {

    override fun observeScreen(screenId: String): Flow<Result<Map<String, Any>>> = flow {
        val userId = userIdProvider()
        val lang = acceptLanguageProvider()

        val result = when (screenId) {
            "home" -> bff.fetchHome(userId = userId, acceptLanguage = lang)
            else -> Result.failure(IllegalArgumentException("Unsupported BFF screenId: $screenId"))
        }
        emit(result)
    }
}

