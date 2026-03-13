package com.app.sdui.data.dto

data class ActionDto(
    val type: String = "None",
    val route: String? = null,
    val url: String? = null,
    val params: Map<String, String>? = null
)
