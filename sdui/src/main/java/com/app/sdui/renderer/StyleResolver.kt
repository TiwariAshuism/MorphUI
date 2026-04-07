package com.app.sdui.renderer

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.blur
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.zIndex
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

        // z-index (useful for overlays)
        zIndex?.let { modifier = modifier.then(Modifier.zIndex(it)) }

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
        val gf = parseColor(gradientFromColor)
        val gt = parseColor(gradientToColor)
        val gv = parseColor(gradientViaColor)
        if (gf != null && gt != null) {
            val from = gf.copy(alpha = gradientFromAlpha ?: gf.alpha)
            val to = gt.copy(alpha = gradientToAlpha ?: gt.alpha)
            val via = gv?.copy(alpha = gradientViaAlpha ?: gv.alpha)
            val isHorizontal = gradientDirection?.equals("horizontal", ignoreCase = true) == true
            val brush = when {
                via != null -> {
                    if (isHorizontal) {
                        Brush.horizontalGradient(
                            0f to from,
                            0.5f to via,
                            1f to to,
                        )
                    } else {
                        Brush.verticalGradient(
                            0f to from,
                            0.5f to via,
                            1f to to,
                        )
                    }
                }
                isHorizontal -> Brush.horizontalGradient(colors = listOf(from, to))
                else -> Brush.verticalGradient(colors = listOf(from, to))
            }
            modifier = modifier.background(
                brush = brush,
                shape = RoundedCornerShape(radius),
            )
        }
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

        // Blur (useful for frosted glass surfaces)
        blurDp?.let { modifier = modifier.blur(it.dp) }

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

        return TextStyle.Default.copy(
            color = parseColor(textColor) ?: Color.Black,
            fontSize = fontSize?.sp ?: TextStyle.Default.fontSize,
            fontWeight = parseFontWeight(fontWeight),
            textAlign = parseTextAlign(textAlign) ?: TextStyle.Default.textAlign,
            letterSpacing = letterSpacing?.sp ?: TextStyle.Default.letterSpacing,
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

    fun parseHorizontalArrangement(s: String?): Arrangement.Horizontal {
        return when (s?.lowercase()) {
            "center" -> Arrangement.Center
            "end" -> Arrangement.End
            "spacebetween" -> Arrangement.SpaceBetween
            "spaceevenly" -> Arrangement.SpaceEvenly
            "spacearound" -> Arrangement.SpaceAround
            "spacedby" -> Arrangement.spacedBy(8.dp)
            else -> Arrangement.Start
        }
    }

    fun parseColumnHorizontalAlignment(s: String?): Alignment.Horizontal {
        return when (s?.lowercase()) {
            "center" -> Alignment.CenterHorizontally
            "end" -> Alignment.End
            else -> Alignment.Start
        }
    }

    fun parseVerticalArrangement(s: String?): Arrangement.Vertical {
        return when (s?.lowercase()) {
            "center" -> Arrangement.Center
            "end", "bottom" -> Arrangement.Bottom
            "spacebetween" -> Arrangement.SpaceBetween
            "spaceevenly" -> Arrangement.SpaceEvenly
            "spacearound" -> Arrangement.SpaceAround
            else -> Arrangement.Top
        }
    }

    /**
     * BoxScope alignment for a child (omit for [layoutAlign] `fill`).
     */
    fun parseBoxLayoutAlign(layoutAlign: String?): Alignment? {
        return when (layoutAlign?.lowercase()) {
            "fill" -> null
            "bottomstart" -> Alignment.BottomStart
            "bottomcenter" -> Alignment.BottomCenter
            "bottomend" -> Alignment.BottomEnd
            "topstart" -> Alignment.TopStart
            "topcenter" -> Alignment.TopCenter
            "topend" -> Alignment.TopEnd
            "center" -> Alignment.Center
            "centerstart" -> Alignment.CenterStart
            "centerend" -> Alignment.CenterEnd
            else -> null
        }
    }

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
