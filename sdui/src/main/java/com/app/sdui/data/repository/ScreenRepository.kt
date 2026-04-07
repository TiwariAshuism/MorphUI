package com.app.sdui.data.repository

import kotlinx.coroutines.flow.Flow

/**
 * Source of SDUI screen payloads.
 *
 * The engine consumes Map<String, Any> (Phase 4 bridge).
 */
interface ScreenRepository {
    fun observeScreen(screenId: String): Flow<Result<Map<String, Any>>>
}

