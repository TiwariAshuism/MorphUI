package com.example.sdui

import com.app.sdui.binding.DataBindingEngine
import org.junit.Test

import org.junit.Assert.*

/**
 * Example local unit test, which will execute on the development machine (host).
 *
 * See [testing documentation](http://d.android.com/tools/testing).
 */
class ExampleUnitTest {
    @Test
    fun addition_isCorrect() {
        assertEquals(4, 2 + 2)
    }

    @Test
    fun dataBindingEngine_resolvesTemplates() {
        val engine = DataBindingEngine()
        val data = mapOf(
            "user" to mapOf(
                "name" to "Ashu",
                "meta" to mapOf("tier" to "premium"),
            )
        )

        val out = engine.resolve("Hi {{user.name}} ({{user.meta.tier}})", data)
        assertEquals("Hi Ashu (premium)", out)
    }
}