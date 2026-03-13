package com.app.sdui.cache

import android.content.Context
import android.content.SharedPreferences
import java.util.concurrent.ConcurrentHashMap

/**
 * Two-tier UI cache for MorphUI screens.
 *
 * **Tier 1**: In-memory [ConcurrentHashMap] for instant access.
 * **Tier 2**: Disk-based [SharedPreferences] for persistence across app restarts.
 *
 * Flow:
 * ```
 * App Launch → load cached UI → render instantly → fetch latest → update cache
 * ```
 */
class UICache(context: Context) {

    private val memoryCache = ConcurrentHashMap<String, CachedScreen>()
    private val prefs: SharedPreferences =
        context.getSharedPreferences("morphui_cache", Context.MODE_PRIVATE)

    /**
     * Retrieves a cached screen, checking memory first, then disk.
     *
     * @param screenId Unique screen identifier
     * @param maxAgeMs Maximum age in milliseconds (default: 24 hours). Pass 0 for no expiry.
     */
    fun get(screenId: String, maxAgeMs: Long = DEFAULT_TTL): CachedScreen? {
        // Try memory first
        memoryCache[screenId]?.let { cached ->
            if (maxAgeMs == 0L || !cached.isExpired(maxAgeMs)) return cached
        }

        // Try disk
        val diskJson = prefs.getString("json_$screenId", null) ?: return null
        val diskVersion = prefs.getInt("version_$screenId", 1)
        val diskTimestamp = prefs.getLong("timestamp_$screenId", 0L)

        val cached = CachedScreen(
            json = diskJson,
            version = diskVersion,
            timestamp = diskTimestamp,
        )

        if (maxAgeMs != 0L && cached.isExpired(maxAgeMs)) {
            // Expired on disk too — clean up
            remove(screenId)
            return null
        }

        // Promote to memory
        memoryCache[screenId] = cached
        return cached
    }

    /**
     * Stores a screen in both memory and disk.
     */
    fun put(screenId: String, json: String, version: Int) {
        val cached = CachedScreen(
            json = json,
            version = version,
            timestamp = System.currentTimeMillis(),
        )

        // Memory
        memoryCache[screenId] = cached

        // Disk
        prefs.edit()
            .putString("json_$screenId", json)
            .putInt("version_$screenId", version)
            .putLong("timestamp_$screenId", cached.timestamp)
            .apply()
    }

    fun remove(screenId: String) {
        memoryCache.remove(screenId)
        prefs.edit()
            .remove("json_$screenId")
            .remove("version_$screenId")
            .remove("timestamp_$screenId")
            .apply()
    }

    fun clearAll() {
        memoryCache.clear()
        prefs.edit().clear().apply()
    }

    companion object {
        /** Default TTL: 24 hours */
        const val DEFAULT_TTL: Long = 24 * 60 * 60 * 1000L
    }
}

/**
 * Represents a cached screen payload with metadata.
 */
data class CachedScreen(
    val json: String,
    val version: Int,
    val timestamp: Long,
) {
    fun isExpired(maxAgeMs: Long): Boolean {
        return System.currentTimeMillis() - timestamp > maxAgeMs
    }
}
