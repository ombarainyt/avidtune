package com.cgens67.innertube.models.response

import com.cgens67.innertube.models.Continuation
import com.cgens67.innertube.models.ItemSectionRenderer
import com.cgens67.innertube.models.MusicResponsiveListItemRenderer
import com.cgens67.innertube.models.SectionListRenderer
import com.cgens67.innertube.models.Tabs
import kotlinx.serialization.Serializable

@Serializable
data class SearchResponse(
    val contents: Contents?,
    val continuationContents: ContinuationContents?,
    val onResponseReceivedActions: List<BrowseResponse.ResponseAction>? = null,
) {
    @Serializable
    data class Contents(
        val tabbedSearchResultsRenderer: Tabs?,
    )

    @Serializable
    data class ContinuationContents(
        val musicShelfContinuation: MusicShelfContinuation? = null,
        val sectionListContinuation: SectionListContinuation? = null,
        val itemSectionContinuation: ItemSectionContinuation? = null,
    ) {
        @Serializable
        data class MusicShelfContinuation(
            val contents: List<Content>?,
            val continuations: List<Continuation>?,
        ) {
            @Serializable
            data class Content(
                val musicResponsiveListItemRenderer: MusicResponsiveListItemRenderer?,
            )
        }

        @Serializable
        data class SectionListContinuation(
            val contents: List<SectionListRenderer.Content>?,
            val continuations: List<Continuation>?,
        )
        
        @Serializable
        data class ItemSectionContinuation(
            val contents: List<ItemSectionRenderer.Content>?,
            val continuations: List<Continuation>?,
        )
    }
}