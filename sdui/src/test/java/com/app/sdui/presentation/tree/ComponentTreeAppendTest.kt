package com.app.sdui.presentation.tree

import com.app.sdui.components.ButtonComponent
import com.app.sdui.components.ColumnComponent
import com.app.sdui.components.TextComponent
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class ComponentTreeAppendTest {

    @Test
    fun append_insertsBeforeLoadMore() {
        val rail = ColumnComponent(
            id = "rail_trending",
            children = listOf(
                TextComponent(value = "t1", id = "a"),
                ButtonComponent(label = "Load more", id = "lm"),
            ),
        )
        val newCard = TextComponent(value = "new", id = "n")
        val out = appendIntoRail(rail, "rail_trending", listOf(newCard)) as ColumnComponent
        assertEquals(3, out.children.size)
        assertEquals("n", (out.children[1] as TextComponent).id)
        assertEquals("lm", (out.children[2] as ButtonComponent).id)
    }

    @Test
    fun removeLoadMore_dropsTrailingButton() {
        val rail = ColumnComponent(
            id = "rail_x",
            children = listOf(
                TextComponent(value = "x", id = "t"),
                ButtonComponent(label = "Load more", id = "lm"),
            ),
        )
        val out = removeLoadMoreFromRail(rail, "rail_x") as ColumnComponent
        assertEquals(1, out.children.size)
        assertTrue(out.children[0] is TextComponent)
    }
}
