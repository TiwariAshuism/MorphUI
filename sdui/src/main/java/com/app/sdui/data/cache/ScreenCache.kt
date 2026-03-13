package com.app.sdui.data.cache

import com.app.sdui.domain.model.UIElement
import java.util.concurrent.ConcurrentHashMap

class ScreenCache {
    private val cache = ConcurrentHashMap<String, UIElement>()

    fun get(screenId: String): UIElement? = cache[screenId]

    fun save(screenId: String, element: UIElement) {
        cache[screenId] = element
    }

    fun clear(screenId: String) {
        cache.remove(screenId)
    }

    fun clearAll() {
        cache.clear()
    }
}
