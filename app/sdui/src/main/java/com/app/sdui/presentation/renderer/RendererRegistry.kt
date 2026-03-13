package com.app.sdui.presentation.renderer

import com.app.sdui.domain.model.UIElement

object RendererRegistry {
    private val renderers = mutableMapOf<String, ComponentRenderer<out UIElement>>()

    fun <T : UIElement> register(typeName: String, renderer: ComponentRenderer<T>) {
        renderers[typeName] = renderer
    }

    @Suppress("UNCHECKED_CAST")
    fun <T : UIElement> get(typeName: String): ComponentRenderer<T>? {
        return renderers[typeName] as? ComponentRenderer<T>
    }

    fun init() {
        register("text", TextRenderer)
        register("image", ImageRenderer)
        register("button", ButtonRenderer)
        register("column", ColumnRenderer)
        register("row", RowRenderer)
        register("spacer", SpacerRenderer)
        register("card", CardRenderer)
        register("divider", DividerRenderer)
    }
}
