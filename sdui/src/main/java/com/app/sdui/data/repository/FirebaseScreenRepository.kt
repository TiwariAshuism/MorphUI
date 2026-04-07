package com.app.sdui.data.repository

import com.app.sdui.data.remote.FirebaseService
import com.app.sdui.data.dto.ComponentDto
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map

/**
 * Backwards compatible screen source using Firebase Realtime Database.
 *
 * Phase 4 keeps this implementation so existing screens can continue to work
 * while the app migrates to BFF.
 */
class FirebaseScreenRepository(
    private val firebase: FirebaseService,
) : ScreenRepository {

    override fun observeScreen(screenId: String): Flow<Result<Map<String, Any>>> {
        return firebase.observeScreen(screenId).map { result ->
            result.map { dto -> dtoToMap(dto) }
        }
    }

    @Suppress("UNCHECKED_CAST")
    private fun dtoToMap(dto: ComponentDto): Map<String, Any> {
        val map = mutableMapOf<String, Any>()
        map["type"] = dto.type
        if (dto.props.isNotEmpty()) map["props"] = dto.props
        dto.style?.let { map["style"] = it }
        dto.children?.let { children ->
            map["children"] = children.map { dtoToMap(it) }
        }
        dto.id?.let { map["id"] = it }
        return map
    }
}

