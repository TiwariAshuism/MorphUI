package com.app.sdui.domain.mapper

import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.app.sdui.data.dto.ComponentDto
import com.app.sdui.domain.model.UIAction
import com.app.sdui.domain.model.UIElement
import com.app.sdui.domain.model.UIStyle

class ComponentMapper {

    fun mapToUIElement(dto: ComponentDto): UIElement {
        val style = parseStyle(dto.style)
        
        return when (dto.type.lowercase()) {
            "text" -> UIElement.Text(
                value = dto.props["value"] as? String ?: "",
                id = dto.id,
                style = style
            )
            
            "image" -> UIElement.Image(
                url = dto.props["url"] as? String ?: "",
                contentDescription = dto.props["contentDescription"] as? String,
                id = dto.id,
                style = style
            )
            
            "button" -> UIElement.Button(
                label = dto.props["label"] as? String ?: "",
                action = parseAction(dto.props["action"]),
                id = dto.id,
                style = style
            )
            
            "column" -> UIElement.Column(
                children = dto.children?.map { mapToUIElement(it) } ?: emptyList(),
                id = dto.id,
                style = style
            )
            
            "row" -> UIElement.Row(
                children = dto.children?.map { mapToUIElement(it) } ?: emptyList(),
                id = dto.id,
                style = style
            )
            
            "spacer" -> UIElement.Spacer(
                height = (dto.props["height"] as? Number)?.toFloat(),
                width = (dto.props["width"] as? Number)?.toFloat(),
                id = dto.id,
                style = style
            )
            
            "card" -> UIElement.Card(
                child = dto.children?.firstOrNull()?.let { mapToUIElement(it) }
                    ?: UIElement.Unknown("empty_card"),
                id = dto.id,
                style = style
            )

            "divider" -> UIElement.Divider(
                thickness = (dto.props["thickness"] as? Number)?.toFloat() ?: 1f,
                color = dto.props["color"] as? String,
                id = dto.id,
                style = style
            )

            "text_input" -> UIElement.TextInput(
                value = dto.props["value"] as? String ?: "",
                placeholder = dto.props["placeholder"] as? String,
                action = parseAction(dto.props["action"]),
                id = dto.id,
                style = style
            )

            "icon_button" -> UIElement.IconButton(
                icon = dto.props["icon"] as? String ?: "",
                action = parseAction(dto.props["action"]),
                id = dto.id,
                style = style
            )

            "list" -> UIElement.ListContainer(
                children = dto.children?.map { mapToUIElement(it) } ?: emptyList(),
                id = dto.id,
                style = style
            )

            "bottom_nav" -> UIElement.BottomNav(
                children = dto.children?.map { mapToUIElement(it) } ?: emptyList(),
                id = dto.id,
                style = style
            )

            else -> UIElement.Unknown(dto.type, dto.id, style)
        }
    }

    private fun parseStyle(styleMap: Map<String, Any>?): UIStyle? {
        if (styleMap == null) return null

        return UIStyle(
            padding = (styleMap["padding"] as? Number)?.toFloat()?.dp,
            margin = (styleMap["margin"] as? Number)?.toFloat()?.dp,
            backgroundColor = parseColor(styleMap["background"] as? String),
            cornerRadius = (styleMap["cornerRadius"] as? Number)?.toFloat()?.dp,
            textColor = parseColor(styleMap["textColor"] as? String),
            fontSize = (styleMap["fontSize"] as? Number)?.toFloat()?.sp,
            fontWeight = parseFontWeight(styleMap["fontWeight"] as? String),
            textAlign = parseTextAlign(styleMap["alignment"] as? String),
            width = (styleMap["width"] as? Number)?.toFloat()?.dp,
            height = (styleMap["height"] as? Number)?.toFloat()?.dp
        )
    }

    @Suppress("UNCHECKED_CAST")
    private fun parseAction(actionData: Any?): UIAction {
        if (actionData == null) return UIAction.None
        
        val actionMap = actionData as? Map<String, Any> ?: return UIAction.None
        
        return when (actionMap["type"] as? String) {
            "Navigate" -> UIAction.Navigate(
                route = actionMap["route"] as? String ?: "",
                params = actionMap["params"] as? Map<String, String>
            )
            "OpenUrl" -> UIAction.OpenUrl(actionMap["url"] as? String ?: "")
            "ShowToast" -> UIAction.ShowToast(actionMap["message"] as? String ?: "")
            "Back" -> UIAction.Back
            else -> UIAction.None
        }
    }

    private fun parseColor(colorString: String?): Color? {
        if (colorString == null) return null
        return try {
            Color(android.graphics.Color.parseColor(colorString))
        } catch (e: Exception) {
            null
        }
    }

    private fun parseFontWeight(weight: String?): FontWeight? {
        return when (weight?.lowercase()) {
            "bold" -> FontWeight.Bold
            "normal" -> FontWeight.Normal
            "light" -> FontWeight.Light
            "medium" -> FontWeight.Medium
            "semibold" -> FontWeight.SemiBold
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
            else -> null
        }
    }
}
