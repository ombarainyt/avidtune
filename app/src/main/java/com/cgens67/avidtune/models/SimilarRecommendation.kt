package com.cgens67.avidtune.models

import com.cgens67.innertube.models.YTItem
import com.cgens67.avidtune.db.entities.LocalItem

data class SimilarRecommendation(
    val title: LocalItem,
    val items: List<YTItem>,
)
