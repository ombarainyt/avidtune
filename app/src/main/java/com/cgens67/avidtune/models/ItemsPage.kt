package com.cgens67.avidtune.models

import com.cgens67.innertube.models.YTItem

data class ItemsPage(
    val items: List<YTItem>,
    val continuation: String?,
)
