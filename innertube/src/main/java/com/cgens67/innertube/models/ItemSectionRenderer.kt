package com.cgens67.innertube.models

import kotlinx.serialization.Serializable

@Serializable
data class ItemSectionRenderer(
    val contents: List<Content>? = null,
) {
    @Serializable
    data class Content(
        val musicResponsiveListItemRenderer: MusicResponsiveListItemRenderer? = null,
        val musicShelfRenderer: MusicShelfRenderer? = null,
    )
}