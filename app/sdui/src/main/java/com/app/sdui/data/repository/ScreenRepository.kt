package com.app.sdui.data.repository

import com.app.sdui.data.cache.ScreenCache
import com.app.sdui.data.remote.FirebaseService
import com.app.sdui.domain.mapper.ComponentMapper
import com.app.sdui.domain.model.UIElement
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.onEach

class ScreenRepository(
    private val firebaseService: FirebaseService,
    private val cache: ScreenCache,
    private val mapper: ComponentMapper
) {
    
    fun observeScreen(screenId: String): Flow<Result<UIElement>> {
        return firebaseService.observeScreen(screenId)
            .map { result ->
                result.map { dto ->
                    mapper.mapToUIElement(dto)
                }
            }
            .onEach { result ->
                result.onSuccess { uiElement ->
                    cache.save(screenId, uiElement)
                }
            }
    }

    fun getCachedScreen(screenId: String): UIElement? {
        return cache.get(screenId)
    }
}
