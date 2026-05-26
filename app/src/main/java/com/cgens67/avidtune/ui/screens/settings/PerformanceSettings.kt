package com.cgens67.avidtune.ui.screens.settings

import androidx.compose.foundation.layout.Column
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBarScrollBehavior
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.navigation.NavController
import com.cgens67.avidtune.R
import com.cgens67.avidtune.constants.AnimateLyricsKey
import com.cgens67.avidtune.constants.AutoLoadMoreKey
import com.cgens67.avidtune.constants.DisableBlurKey
import com.cgens67.avidtune.constants.MinimalPlayerDesignKey
import com.cgens67.avidtune.constants.SimilarContent
import com.cgens67.avidtune.ui.component.SettingsGeneralCategory
import com.cgens67.avidtune.ui.component.SettingsPage
import com.cgens67.avidtune.ui.component.SwitchPreference
import com.cgens67.avidtune.utils.rememberPreference

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun PerformanceSettings(
    navController: NavController,
    scrollBehavior: TopAppBarScrollBehavior,
) {
    val (minimalPlayerDesign, onMinimalPlayerDesignChange) = rememberPreference(
        key = MinimalPlayerDesignKey,
        defaultValue = false
    )
    val (disableBlur, onDisableBlurChange) = rememberPreference(
        key = DisableBlurKey,
        defaultValue = false
    )
    val (animateLyrics, onAnimateLyricsChange) = rememberPreference(
        AnimateLyricsKey,
        defaultValue = true
    )
    val (autoLoadMore, onAutoLoadMoreChange) = rememberPreference(
        AutoLoadMoreKey,
        defaultValue = true
    )
    val (similarContentEnabled, similarContentEnabledChange) = rememberPreference(
        key = SimilarContent,
        defaultValue = true
    )

    SettingsPage(
        title = stringResource(R.string.performance),
        navController = navController,
        scrollBehavior = scrollBehavior
    ) {
        SettingsGeneralCategory(
            title = stringResource(R.string.player),
            items = listOf(
                {
                    SwitchPreference(
                        title = { Text(stringResource(R.string.minimal_player_design)) },
                        icon = { Icon(painterResource(R.drawable.play), null) },
                        checked = minimalPlayerDesign,
                        onCheckedChange = onMinimalPlayerDesignChange
                    )
                }
            )
        )

        SettingsGeneralCategory(
            title = stringResource(R.string.visual_effects),
            items = listOf(
                {
                    SwitchPreference(
                        title = { Text(stringResource(R.string.disable_blur_effects)) },
                        icon = { Icon(painterResource(R.drawable.image), null) },
                        description = stringResource(R.string.disable_blur_effects_desc),
                        checked = disableBlur,
                        onCheckedChange = onDisableBlurChange
                    )
                },
                {
                    SwitchPreference(
                        title = { Text(stringResource(R.string.animate_lyrics)) },
                        icon = { Icon(painterResource(R.drawable.lyrics), null) },
                        description = stringResource(R.string.animate_lyrics_desc),
                        checked = animateLyrics,
                        onCheckedChange = onAnimateLyricsChange
                    )
                }
            )
        )

        SettingsGeneralCategory(
            title = stringResource(R.string.network),
            items = listOf(
                {
                    SwitchPreference(
                        title = { Text(stringResource(R.string.auto_load_more)) },
                        description = stringResource(R.string.auto_load_more_desc),
                        icon = { Icon(painterResource(R.drawable.playlist_add), null) },
                        checked = autoLoadMore,
                        onCheckedChange = onAutoLoadMoreChange
                    )
                },
                {
                    SwitchPreference(
                        title = { Text(stringResource(R.string.enable_similar_content)) },
                        description = stringResource(R.string.similar_content_desc),
                        icon = { Icon(painterResource(R.drawable.similar), null) },
                        checked = similarContentEnabled,
                        onCheckedChange = similarContentEnabledChange
                    )
                }
            )
        )
    }
}