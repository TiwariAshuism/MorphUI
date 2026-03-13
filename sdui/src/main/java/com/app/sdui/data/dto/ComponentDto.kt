package com.app.sdui.data.dto

import com.google.firebase.database.PropertyName

data class ComponentDto(
    @PropertyName("type") val type: String = "",
    @PropertyName("props") val props: Map<String, Any> = emptyMap(),
    @PropertyName("style") val style: Map<String, Any>? = null,
    @PropertyName("children") val children: List<ComponentDto>? = null,
    @PropertyName("id") val id: String? = null
)
