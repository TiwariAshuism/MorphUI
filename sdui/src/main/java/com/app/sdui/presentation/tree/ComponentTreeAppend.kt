package com.app.sdui.presentation.tree

import com.app.sdui.components.ButtonComponent
import com.app.sdui.components.CardComponent
import com.app.sdui.components.CarouselComponent
import com.app.sdui.components.ColumnComponent
import com.app.sdui.components.GridComponent
import com.app.sdui.components.ListComponent
import com.app.sdui.components.PageComponent
import com.app.sdui.components.RowComponent
import com.app.sdui.core.UIComponent

/**
 * Appends [newItems] into a rail identified by [railId] (e.g. `rail_trending`).
 *
 * If the rail ends with a **Load more** button, new items are inserted **before** that button.
 * Otherwise items are appended to the end.
 */
fun appendIntoRail(
    root: UIComponent,
    railId: String,
    newItems: List<UIComponent>,
): UIComponent {
    if (newItems.isEmpty()) return root
    return when (root) {
        is PageComponent -> root.copy(children = root.children.map { appendIntoRail(it, railId, newItems) })
        is ListComponent -> root.copy(children = root.children.map { appendIntoRail(it, railId, newItems) })
        is ColumnComponent -> {
            if (root.id == railId) mergeColumnOrAppend(root, newItems)
            else root.copy(children = root.children.map { appendIntoRail(it, railId, newItems) })
        }
        is RowComponent -> root.copy(children = root.children.map { appendIntoRail(it, railId, newItems) })
        is CarouselComponent -> {
            if (root.id == railId) root.copy(children = root.children + newItems)
            else root.copy(children = root.children.map { appendIntoRail(it, railId, newItems) })
        }
        is GridComponent -> {
            if (root.id == railId) root.copy(children = root.children + newItems)
            else root.copy(children = root.children.map { appendIntoRail(it, railId, newItems) })
        }
        is CardComponent -> root.copy(child = appendIntoRail(root.child, railId, newItems))
        else -> root
    }
}

private fun mergeColumnOrAppend(col: ColumnComponent, newItems: List<UIComponent>): ColumnComponent {
    val children = col.children.toMutableList()
    if (children.isEmpty()) return col.copy(children = newItems)
    val last = children.last()
    val loadMore = last is ButtonComponent &&
        last.label.trim().equals("Load more", ignoreCase = true)
    return if (loadMore) {
        val insertAt = children.lastIndex
        children.addAll(insertAt, newItems)
        col.copy(children = children)
    } else {
        col.copy(children = children + newItems)
    }
}
