package com.app.sdui.renderer

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.core.graphics.toColorInt
import com.app.sdui.core.UIStyle

/**
 * Converts framework-agnostic [UIStyle] into Compose [Modifier] and [TextStyle].
 *
 * This is the single place where raw style values get converted to
 * platform-specific Compose types, keeping the rest of the engine clean.
 */
object StyleResolver {

    /**
     * Creates a Compose [Modifier] from a [UIStyle].
     * Applies: margin → background → border → shadow → size → padding → opacity.
     */
    fun UIStyle?.toModifier(): Modifier {
        if (this == null) return Modifier

        var modifier = Modifier as Modifier

        // Margin (outer spacing)
        margin?.let { modifier = modifier.padding(it.dp) }

        // Size constraints
        width?.let { modifier = modifier.width(it.dp) }
        height?.let { modifier = modifier.height(it.dp) }
        maxWidth?.let { modifier = modifier.widthIn(max = it.dp) }
        maxHeight?.let { modifier = modifier.heightIn(max = it.dp) }

        // Shadow / elevation
        elevation?.let {
            modifier = modifier.shadow(
                elevation = it.dp,
                shape = RoundedCornerShape(cornerRadius?.dp ?: 0.dp),
            )
        }

        // Background + corner radius
        val bgColor = parseColor(backgroundColor)
        val radius = cornerRadius?.dp ?: 0.dp
        if (bgColor != null) {
            modifier = modifier.background(bgColor, RoundedCornerShape(radius))
        }

        // Border
        val bColor = parseColor(borderColor)
        if (bColor != null && borderWidth != null) {
            modifier = modifier.border(borderWidth.dp, bColor, RoundedCornerShape(radius))
        }

        // Padding (inner spacing)
        when {
            paddingHorizontal != null || paddingVertical != null -> {
                modifier = modifier.padding(
                    horizontal = (paddingHorizontal ?: 0f).dp,
                    vertical = (paddingVertical ?: 0f).dp,
                )
            }

            padding != null -> {
                modifier = modifier.padding(padding.dp)
            }
        }

        // Opacity
        opacity?.let { modifier = modifier.alpha(it) }

        return modifier
    }

    /**
     * Creates a Compose [Modifier] that fills max width by default,
     * unless an explicit width is set in the style.
     */
    fun UIStyle?.toContainerModifier(): Modifier {
        val base = toModifier()
        return if (this?.width != null) base else base.fillMaxWidth()
    }

    /**
     * Creates a Compose [TextStyle] from a [UIStyle].
     */
    fun UIStyle?.toTextStyle(): TextStyle {
        if (this == null) return TextStyle.Default

        return TextStyle(
            color = parseColor(textColor) ?: Color.Unspecified,
            fontSize = fontSize?.sp ?: TextStyle.Default.fontSize,
            fontWeight = parseFontWeight(fontWeight),
            textAlign = parseTextAlign(textAlign),
        )
    }

    fun UIStyle?.textColor(): Color {
        return parseColor(this?.textColor) ?: Color.Black
    }

    fun UIStyle?.fontSize(): androidx.compose.ui.unit.TextUnit {
        return this?.fontSize?.sp ?: androidx.compose.ui.unit.TextUnit.Unspecified
    }

    fun UIStyle?.fontWeight(): FontWeight? = parseFontWeight(this?.fontWeight)

    fun UIStyle?.textAlign(): TextAlign? = parseTextAlign(this?.textAlign)

    // ──────────────────────────────────────────────
    // Internal helpers
    // ──────────────────────────────────────────────

    fun parseColor(hex: String?): Color? {
        if (hex.isNullOrBlank()) return null
        return try {
            Color(hex.toColorInt())
        } catch (_: Exception) {
            null
        }
    }

    private fun parseFontWeight(weight: String?): FontWeight? {
        return when (weight?.lowercase()) {
            "bold" -> FontWeight.Bold
            "normal" -> FontWeight.Normal
            "light" -> FontWeight.Light
            "medium" -> FontWeight.Medium
            "semibold", "semi_bold" -> FontWeight.SemiBold
            "thin" -> FontWeight.Thin
            "extra_bold", "extrabold" -> FontWeight.ExtraBold
            "black" -> FontWeight.Black
            else -> null
        }
    }

    private fun parseTextAlign(align: String?): TextAlign? {
        return when (align?.lowercase()) {
            "center" -> TextAlign.Center
            "left" -> TextAlign.Left
            "right" -> TextAlign.Right
            "start" -> TextAlign.Start
            "end" -> TextAlign.End
            "justify" -> TextAlign.Justify
            else -> null
        }
    }
}
