package com.cgens67.avidtune.ui.screens

import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.tween
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.background
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.systemBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.pager.HorizontalPager
import androidx.compose.foundation.pager.rememberPagerState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.runtime.withFrameMillis
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.navigation.NavController
import coil.compose.AsyncImage
import com.cgens67.avidtune.LocalPlayerConnection
import com.cgens67.avidtune.R
import com.cgens67.avidtune.extensions.toMediaItem
import com.cgens67.avidtune.playback.queues.ListQueue
import com.cgens67.avidtune.viewmodels.InsightViewModel
import kotlinx.coroutines.launch

@OptIn(ExperimentalFoundationApi::class)
@Composable
fun InsightScreen(
    navController: NavController,
    viewModel: InsightViewModel = hiltViewModel()
) {
    val isLoading by viewModel.isLoading.collectAsState()
    val topSongs by viewModel.topSongs.collectAsState()
    val topSongStats by viewModel.topSongStats.collectAsState()
    val topArtists by viewModel.topArtists.collectAsState()
    val totalMinutes by viewModel.totalMinutes.collectAsState()

    val playerConnection = LocalPlayerConnection.current

    if (isLoading) {
        Box(modifier = Modifier.fillMaxSize().background(Color.Black), contentAlignment = Alignment.Center) {
            CircularProgressIndicator(color = Color.White)
        }
        return
    }

    if (topSongs.isEmpty()) {
        Box(modifier = Modifier.fillMaxSize().background(Color.Black), contentAlignment = Alignment.Center) {
            Text(
                text = stringResource(R.string.insight_no_data),
                color = Color.White,
                style = MaterialTheme.typography.titleLarge
            )
            Button(onClick = { navController.navigateUp() }, modifier = Modifier.padding(top = 64.dp)) {
                Text(stringResource(R.string.insight_close))
            }
        }
        return
    }

    val pageCount = 5
    val pagerState = rememberPagerState(pageCount = { pageCount })
    var isPaused by remember { mutableStateOf(false) }
    val coroutineScope = rememberCoroutineScope()

    // Control auto-advance
    var currentProgress by remember { mutableFloatStateOf(0f) }

    LaunchedEffect(pagerState.currentPage, isPaused) {
        if (!isPaused) {
            var lastTime = 0L
            while (currentProgress < 1f) {
                withFrameMillis { time ->
                    if (lastTime == 0L) lastTime = time
                    val dt = time - lastTime
                    lastTime = time
                    currentProgress += dt / 5000f // 5 seconds per page
                }
            }
            if (currentProgress >= 1f && pagerState.currentPage < pageCount - 1) {
                currentProgress = 0f
                pagerState.animateScrollToPage(pagerState.currentPage + 1)
            }
        }
    }

    Box(modifier = Modifier.fillMaxSize().background(Color.Black)) {
        HorizontalPager(
            state = pagerState,
            userScrollEnabled = false,
            modifier = Modifier
                .fillMaxSize()
                .pointerInput(Unit) {
                    detectTapGestures(
                        onPress = {
                            isPaused = true
                            tryAwaitRelease()
                            isPaused = false
                        },
                        onTap = { offset ->
                            val isRight = offset.x > size.width / 2
                            coroutineScope.launch {
                                if (isRight) {
                                    if (pagerState.currentPage < pageCount - 1) {
                                        currentProgress = 0f
                                        pagerState.animateScrollToPage(pagerState.currentPage + 1)
                                    } else {
                                        currentProgress = 1f // Stay on last page
                                    }
                                } else {
                                    if (pagerState.currentPage > 0) {
                                        currentProgress = 0f
                                        pagerState.animateScrollToPage(pagerState.currentPage - 1)
                                    } else {
                                        currentProgress = 0f
                                    }
                                }
                            }
                        }
                    )
                }
        ) { page ->
            when (page) {
                0 -> IntroPage()
                1 -> TotalTimePage(totalMinutes)
                2 -> TopSongPage(topSongs.firstOrNull(), topSongStats?.songCountListened ?: 0)
                3 -> TopArtistsPage(topArtists)
                4 -> SummaryPage(topSongs, totalMinutes, topArtists.firstOrNull()?.artist?.name) {
                    navController.navigateUp()
                }
            }
        }

        // Top Progress Bars
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .systemBarsPadding()
                .padding(16.dp),
            horizontalArrangement = Arrangement.spacedBy(4.dp)
        ) {
            for (i in 0 until pageCount) {
                val progress = when {
                    i < pagerState.currentPage -> 1f
                    i == pagerState.currentPage -> currentProgress
                    else -> 0f
                }
                Box(
                    modifier = Modifier
                        .weight(1f)
                        .height(4.dp)
                        .clip(CircleShape)
                        .background(Color.White.copy(alpha = 0.3f))
                ) {
                    Box(
                        modifier = Modifier
                            .fillMaxWidth(progress)
                            .height(4.dp)
                            .background(Color.White)
                    )
                }
            }
        }

        // Close button top right
        IconButton(
            onClick = { navController.navigateUp() },
            modifier = Modifier
                .align(Alignment.TopEnd)
                .systemBarsPadding()
                .padding(top = 28.dp, end = 8.dp)
        ) {
            Icon(
                painter = painterResource(R.drawable.close),
                contentDescription = stringResource(R.string.insight_close),
                tint = Color.White
            )
        }
        
        // Play button bottom for music integration
        if (pagerState.currentPage > 1) {
            Button(
                onClick = { 
                    playerConnection?.playQueue(
                        ListQueue(
                            title = "AvidTune Insight",
                            items = topSongs.map { it.toMediaItem() }
                        )
                    )
                },
                modifier = Modifier
                    .align(Alignment.BottomCenter)
                    .padding(bottom = 32.dp),
                colors = ButtonDefaults.buttonColors(containerColor = Color.White, contentColor = Color.Black)
            ) {
                Icon(painter = painterResource(R.drawable.play), contentDescription = null, modifier = Modifier.size(20.dp))
                Spacer(Modifier.width(8.dp))
                Text(stringResource(R.string.insight_play), fontWeight = FontWeight.Bold)
            }
        }
    }
}

@Composable
fun IntroPage() {
    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(Brush.verticalGradient(listOf(Color(0xFF8E2DE2), Color(0xFF4A00E0)))),
        contentAlignment = Alignment.Center
    ) {
        Column(horizontalAlignment = Alignment.CenterHorizontally) {
            Text(
                text = stringResource(R.string.insight_intro_ready),
                fontSize = 32.sp,
                color = Color.White,
                fontWeight = FontWeight.Bold
            )
            Text(
                text = stringResource(R.string.insight_intro_ready_2),
                fontSize = 32.sp,
                color = Color.White,
                fontWeight = FontWeight.Bold
            )
        }
    }
}

@Composable
fun TotalTimePage(minutes: Long) {
    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(Brush.verticalGradient(listOf(Color(0xFF00b09b), Color(0xFF96c93d)))),
        contentAlignment = Alignment.Center
    ) {
        Column(horizontalAlignment = Alignment.CenterHorizontally, modifier = Modifier.padding(32.dp)) {
            Text(
                text = stringResource(R.string.insight_minutes_spent),
                fontSize = 24.sp,
                color = Color.White
            )
            Text(
                text = "$minutes",
                fontSize = 80.sp,
                color = Color.White,
                fontWeight = FontWeight.Black
            )
            Text(
                text = stringResource(R.string.insight_minutes),
                fontSize = 24.sp,
                color = Color.White,
                fontWeight = FontWeight.Bold
            )
            Spacer(Modifier.height(16.dp))
            Text(
                text = stringResource(R.string.insight_minutes_listening),
                fontSize = 20.sp,
                color = Color.White,
                textAlign = TextAlign.Center
            )
        }
    }
}

@Composable
fun TopSongPage(song: com.cgens67.avidtune.db.entities.Song?, playCount: Int) {
    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(Brush.verticalGradient(listOf(Color(0xFFFF512F), Color(0xFFDD2476)))),
        contentAlignment = Alignment.Center
    ) {
        Column(horizontalAlignment = Alignment.CenterHorizontally, modifier = Modifier.padding(32.dp)) {
            Text(
                text = stringResource(R.string.insight_top_song_was),
                fontSize = 24.sp,
                color = Color.White,
                fontWeight = FontWeight.Bold
            )
            Spacer(Modifier.height(32.dp))
            AsyncImage(
                model = song?.song?.thumbnailUrl,
                contentDescription = null,
                modifier = Modifier
                    .size(250.dp)
                    .shadow(16.dp, RoundedCornerShape(12.dp))
                    .clip(RoundedCornerShape(12.dp)),
                contentScale = ContentScale.Crop
            )
            Spacer(Modifier.height(24.dp))
            Text(
                text = song?.song?.title ?: "",
                fontSize = 32.sp,
                color = Color.White,
                fontWeight = FontWeight.Black,
                textAlign = TextAlign.Center,
                maxLines = 2,
                overflow = TextOverflow.Ellipsis
            )
            Text(
                text = song?.artists?.joinToString { it.name } ?: "",
                fontSize = 20.sp,
                color = Color.White.copy(alpha = 0.8f),
                textAlign = TextAlign.Center
            )
            Spacer(Modifier.height(32.dp))
            Text(
                text = "${stringResource(R.string.insight_with)} $playCount ${stringResource(R.string.insight_plays)}",
                fontSize = 18.sp,
                color = Color.White,
                fontWeight = FontWeight.Bold,
                modifier = Modifier
                    .background(Color.Black.copy(alpha = 0.3f), RoundedCornerShape(16.dp))
                    .padding(horizontal = 16.dp, vertical = 8.dp)
            )
        }
    }
}

@Composable
fun TopArtistsPage(artists: List<com.cgens67.avidtune.db.entities.Artist>) {
    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(Brush.verticalGradient(listOf(Color(0xFFcb2d3e), Color(0xFFef473a)))),
        contentAlignment = Alignment.Center
    ) {
        Column(horizontalAlignment = Alignment.Start, modifier = Modifier.padding(32.dp)) {
            Text(
                text = stringResource(R.string.insight_artists_title),
                fontSize = 32.sp,
                color = Color.White,
                fontWeight = FontWeight.Black,
                modifier = Modifier.padding(bottom = 32.dp)
            )
            
            artists.take(5).forEachIndexed { index, artist ->
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    modifier = Modifier.padding(vertical = 12.dp)
                ) {
                    Text(
                        text = "#${index + 1}",
                        fontSize = 24.sp,
                        color = Color.White.copy(alpha = 0.7f),
                        fontWeight = FontWeight.Bold,
                        modifier = Modifier.width(40.dp)
                    )
                    AsyncImage(
                        model = artist.artist.thumbnailUrl,
                        contentDescription = null,
                        modifier = Modifier
                            .size(56.dp)
                            .clip(CircleShape),
                        contentScale = ContentScale.Crop
                    )
                    Spacer(Modifier.width(16.dp))
                    Text(
                        text = artist.artist.name,
                        fontSize = 24.sp,
                        color = Color.White,
                        fontWeight = FontWeight.Bold,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                }
            }
        }
    }
}

@Composable
fun SummaryPage(topSongs: List<com.cgens67.avidtune.db.entities.Song>, totalMinutes: Long, topArtistName: String?, onClose: () -> Unit) {
    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(Brush.verticalGradient(listOf(Color(0xFF141E30), Color(0xFF243B55)))),
        contentAlignment = Alignment.Center
    ) {
        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            modifier = Modifier.padding(24.dp)
        ) {
            Text(
                text = stringResource(R.string.insight_summary_title),
                fontSize = 32.sp,
                color = Color.White,
                fontWeight = FontWeight.Black,
                textAlign = TextAlign.Center
            )
            
            Spacer(Modifier.height(32.dp))
            
            Box(
                modifier = Modifier
                    .background(Color.White.copy(alpha = 0.1f), RoundedCornerShape(24.dp))
                    .padding(24.dp)
                    .fillMaxWidth()
            ) {
                Column {
                    Text(stringResource(R.string.insight_minutes_spent), color = Color.White.copy(alpha = 0.7f), fontSize = 14.sp)
                    Text("$totalMinutes", color = Color.White, fontSize = 28.sp, fontWeight = FontWeight.Bold)
                    
                    Spacer(Modifier.height(16.dp))
                    
                    Text(stringResource(R.string.insight_artists_title).replace("Your", "Top"), color = Color.White.copy(alpha = 0.7f), fontSize = 14.sp)
                    Text(topArtistName ?: "N/A", color = Color.White, fontSize = 24.sp, fontWeight = FontWeight.Bold)
                    
                    Spacer(Modifier.height(16.dp))
                    
                    Text("Top 5 ${stringResource(R.string.insight_top_songs).replace("\n", " ")}", color = Color.White.copy(alpha = 0.7f), fontSize = 14.sp)
                    topSongs.take(5).forEachIndexed { index, song ->
                        Text(
                            text = "${index + 1}. ${song.song.title}",
                            color = Color.White,
                            fontSize = 16.sp,
                            fontWeight = FontWeight.Medium,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis,
                            modifier = Modifier.padding(vertical = 4.dp)
                        )
                    }
                }
            }
            
            Spacer(Modifier.height(80.dp)) // Space for the play button
            
            TextButton(onClick = onClose) {
                Text(stringResource(R.string.insight_close), color = Color.White.copy(alpha = 0.5f))
            }
        }
    }
}