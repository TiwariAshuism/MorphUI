package com.app.sdui.core

/**
 * Framework-agnostic style model for MorphUI components.
 *
 * All values are stored as primitives — no Compose/Android imports.
 * The renderer layer converts these into platform-specific styles.
 *
 * - Colors: stored as hex strings (e.g. "#FF6200EE")
 * - Dimensions: stored as Float in dp
 * - Font weight: stored as String (e.g. "bold", "normal", "light")
 * - Text alignment: stored as String (e.g. "center", "start", "end")
 */
data class UIStyle(
    val padding: Float? = null,
    val paddingHorizontal: Float? = null,
    val paddingVertical: Float? = null,
    val margin: Float? = null,
    val backgroundColor: String? = null,
    val cornerRadius: Float? = null,
    val textColor: String? = null,
    val fontSize: Float? = null,
    val fontWeight: String? = null,
    val textAlign: String? = null,
    val width: Float? = null,
    val height: Float? = null,
    val maxWidth: Float? = null,
    val maxHeight: Float? = null,
    val opacity: Float? = null,
    val borderColor: String? = null,
    val borderWidth: Float? = null,
    val elevation: Float? = null,
    // Phase 9-ish styling extensions (used to match richer web-like designs).
    // All are optional and safely ignored by older renderers.
    val zIndex: Float? = null,
    val blurDp: Float? = null,
    // Simple vertical gradient background for overlays (e.g. hero scrim).
    val gradientFromColor: String? = null,
    val gradientToColor: String? = null,
    val gradientFromAlpha: Float? = null,
    val gradientToAlpha: Float? = null,
    /** Optional middle stop for 3-color gradients (50% position). */
    val gradientViaColor: String? = null,
    val gradientViaAlpha: Float? = null,
    /** `vertical` (default) or `horizontal` for gradient brush direction. */
    val gradientDirection: String? = null,
    /** Letter spacing in sp (e.g. 1.2f for wide tracking). */
    val letterSpacing: Float? = null,
    // Layout (not applied in generic [toModifier]; used by box / row / column renderers).
    /** Box child placement: `fill`, `bottomStart`, `center`, `topStart`, etc. */
    val layoutAlign: String? = null,
    /** Row/Column flex child weight (RowScope/ColumnScope). */
    val weight: Float? = null,
    /** Row: `start`, `center`, `end`, `spaceBetween`, `spaceEvenly`, `spaceAround`. */
    val horizontalArrangement: String? = null,
    /** Column content horizontal alignment: `start`, `center`, `end`. */
    val columnHorizontalAlignment: String? = null,
    /** Column: `start`, `center`, `end`, `spaceBetween`. */
    val verticalArrangement: String? = null,
)
