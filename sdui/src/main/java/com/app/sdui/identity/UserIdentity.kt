package com.app.sdui.identity

import android.content.Context
import java.util.UUID

/**
 * Phase 7 identity provider.
 *
 * Until real auth is wired, we use a device-scoped anonymous id persisted in SharedPreferences.
 */
class UserIdentity(private val context: Context) {

    fun userId(): String {
        val prefs = context.getSharedPreferences(PREFS, Context.MODE_PRIVATE)
        val existing = prefs.getString(KEY_USER_ID, null)
        if (!existing.isNullOrBlank()) return existing

        val created = "anon_" + UUID.randomUUID().toString().replace("-", "")
        prefs.edit().putString(KEY_USER_ID, created).apply()
        return created
    }

    companion object {
        private const val PREFS = "morphui.identity"
        private const val KEY_USER_ID = "user_id"
    }
}

