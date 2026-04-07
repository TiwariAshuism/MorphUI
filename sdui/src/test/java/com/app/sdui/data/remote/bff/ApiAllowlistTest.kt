package com.app.sdui.data.remote.bff

import okhttp3.HttpUrl.Companion.toHttpUrl
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class ApiAllowlistTest {

    private val base = "http://10.0.2.2:8080/".toHttpUrl()

    @Test
    fun sectionGet_allowed() {
        val u = base.resolve("/section/trending")!!
        assertTrue(ApiAllowlist.isAllowed(base, u, "GET"))
    }

    @Test
    fun sectionPost_blocked() {
        val u = base.resolve("/section/trending")!!
        assertFalse(ApiAllowlist.isAllowed(base, u, "POST"))
    }

    @Test
    fun eventsPost_allowed() {
        val u = base.resolve("/api/events")!!
        assertTrue(ApiAllowlist.isAllowed(base, u, "POST"))
    }

    @Test
    fun foreignHost_blocked() {
        val other = "https://evil.example/api/events".toHttpUrl()
        assertFalse(ApiAllowlist.isAllowed(base, other, "POST"))
    }
}
