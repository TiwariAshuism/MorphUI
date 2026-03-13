package com.app.sdui.domain.model

import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp

data class UIStyle(
    val padding: Dp? = null,
    val margin: Dp? = null,
    val backgroundColor: Color? = null,
    val cornerRadius: Dp? = null,
    val textColor: Color? = null,
    val fontSize: androidx.compose.ui.unit.TextUnit? = null,
    val fontWeight: androidx.compose.ui.text.font.FontWeight? = null,
    val textAlign: androidx.compose.ui.text.style.TextAlign? = null,
    val width: Dp? = null,
    val height: Dp? = null
)
