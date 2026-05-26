package com.cgens67.avidtune.lyrics

import android.content.Context
import com.cgens67.music.betterlyrics.BetterLyrics
import com.cgens67.avidtune.constants.EnableBetterLyricsKey
import com.cgens67.avidtune.utils.dataStore
import com.cgens67.avidtune.utils.get

object BetterLyricsProvider : LyricsProvider {
    override val name = "BetterLyrics"

    override fun isEnabled(context: Context): Boolean = context.dataStore[EnableBetterLyricsKey] ?: true

    override suspend fun getLyrics(
        id: String,
        title: String,
        artist: String,
        duration: Int,
    ): Result<String> = BetterLyrics.getLyrics(title, artist, duration, null)
}