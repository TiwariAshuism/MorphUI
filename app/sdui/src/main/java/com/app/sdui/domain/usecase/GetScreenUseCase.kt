package com.app.sdui.domain.usecase

import com.app.sdui.data.repository.ScreenRepository
import com.app.sdui.domain.model.UIElement
import kotlinx.coroutines.flow.Flow

class GetScreenUseCase(private val repository: ScreenRepository) {
    
    operator fun invoke(screenId: String): Flow<Result<UIElement>> {
        return repository.observeScreen(screenId)
    }

    fun getCached(screenId: String): UIElement? {
        return repository.getCachedScreen(screenId)
    }
}
