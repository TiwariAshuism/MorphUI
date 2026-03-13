package com.app.sdui.domain.model

sealed class UIElement {
    abstract val id: String?
    abstract val style: UIStyle?

    data class Text(
        val value: String,
        override val id: String? = null,
        override val style: UIStyle? = null
    ) : UIElement()

    data class Image(
        val url: String,
        val contentDescription: String? = null,
        override val id: String? = null,
        override val style: UIStyle? = null
    ) : UIElement()

    data class Button(
        val label: String,
        val action: UIAction,
        override val id: String? = null,
        override val style: UIStyle? = null
    ) : UIElement()

    data class Column(
        val children: List<UIElement>,
        override val id: String? = null,
        override val style: UIStyle? = null
    ) : UIElement()

    data class Row(
        val children: List<UIElement>,
        override val id: String? = null,
        override val style: UIStyle? = null
    ) : UIElement()

    data class Spacer(
        val height: Float? = null,
        val width: Float? = null,
        override val id: String? = null,
        override val style: UIStyle? = null
    ) : UIElement()

    data class Card(
        val child: UIElement,
        override val id: String? = null,
        override val style: UIStyle? = null
    ) : UIElement()

    data class Divider(
        val thickness: Float = 1f,
        val color: String? = null,
        override val id: String? = null,
        override val style: UIStyle? = null
    ) : UIElement()

    data class TextInput(
        val value: String,
        val placeholder: String? = null,
        val action: UIAction? = null,
        override val id: String? = null,
        override val style: UIStyle? = null
    ) : UIElement()

    data class IconButton(
        val icon: String,
        val action: UIAction,
        override val id: String? = null,
        override val style: UIStyle? = null
    ) : UIElement()

    data class ListContainer(
        val children: List<UIElement>,
        override val id: String? = null,
        override val style: UIStyle? = null
    ) : UIElement()

    data class BottomNav(
        val children: List<UIElement>,
        override val id: String? = null,
        override val style: UIStyle? = null
    ) : UIElement()

    data class Unknown(
        val type: String,
        override val id: String? = null,
        override val style: UIStyle? = null
    ) : UIElement()
}
