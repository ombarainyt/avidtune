package com.cgens67.avidtune.ui.screens

import android.content.Context
import android.content.Intent
import android.graphics.Bitmap
import android.graphics.Paint
import android.graphics.Shader
import android.graphics.LinearGradient
import android.graphics.Typeface
import android.os.Handler
import android.os.Looper
import android.text.TextPaint
import android.widget.Toast
import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.spring
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.scaleIn
import androidx.compose.animation.slideInHorizontally
import androidx.compose.animation.slideInVertically
import androidx.compose.animation.slideOutHorizontally
import androidx.compose.animation.togetherWith
import androidx.compose.foundation.Canvas
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
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.runtime.withFrameMillis
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.scale
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.toArgb
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.drawText
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.rememberTextMeasurer
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.util.lerp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.navigation.NavController
import coil.compose.AsyncImage
import com.cgens67.avidtune.LocalPlayerConnection
import com.cgens67.avidtune.R
import com.cgens67.avidtune.extensions.toMediaItem
import com.cgens67.avidtune.playback.PlayerConnection
import com.cgens67.avidtune.playback.queues.ListQueue
import com.cgens67.avidtune.utils.ComposeToImage
import com.cgens67.avidtune.viewmodels.InsightViewModel
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch

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
    var currentPage by remember { mutableIntStateOf(0) }
    var isPaused by remember { mutableStateOf(false) }
    val coroutineScope = rememberCoroutineScope()

    // Control auto-advance
    var currentProgress by remember { mutableFloatStateOf(0f) }

    LaunchedEffect(currentPage, isPaused) {
        currentProgress = 0f
        if (!isPaused && currentPage < pageCount - 1) {
            val duration = 5000f
            val startTime = withFrameMillis { it }
            while (currentProgress < 1f) {
                withFrameMillis { time ->
                    currentProgress = (time - startTime) / duration
                }
            }
            currentPage++
        } else if (currentPage == pageCount - 1) {
            currentProgress = 1f // Stay filled on last page
        }
    }

    // Dynamic gradient transitions
    val pageColors = listOf(
        Color(0xFF8E2DE2) to Color(0xFF4A00E0), // Intro: Purple
        Color(0xFF00b09b) to Color(0xFF96c93d), // Time: Green
        Color(0xFFFF512F) to Color(0xFFDD2476), // Top Song: Pink/Red
        Color(0xFFcb2d3e) to Color(0xFFef473a), // Artists: Red
        Color(0xFF141E30) to Color(0xFF243B55)  // Summary: Dark Blue
    )

    val currentColors = pageColors[currentPage.coerceIn(0, pageColors.lastIndex)]

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(Brush.verticalGradient(listOf(currentColors.first, currentColors.second)))
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
                                if (currentPage < pageCount - 1) {
                                    currentPage++
                                }
                            } else {
                                if (currentPage > 0) {
                                    currentPage--
                                }
                            }
                        }
                    }
                )
            }
    ) {
        AnimatedContent(
            targetState = currentPage,
            transitionSpec = {
                if (targetState > initialState) {
                    slideInHorizontally(tween(400, easing = FastOutSlowInEasing)) { width -> width } + fadeIn(tween(400)) togetherWith
                            slideOutHorizontally(tween(400, easing = FastOutSlowInEasing)) { width -> -width } + fadeOut(tween(400))
                } else {
                    slideInHorizontally(tween(400, easing = FastOutSlowInEasing)) { width -> -width } + fadeIn(tween(400)) togetherWith
                            slideOutHorizontally(tween(400, easing = FastOutSlowInEasing)) { width -> width } + fadeOut(tween(400))
                }
            },
            label = "InsightPages",
            modifier = Modifier.fillMaxSize()
        ) { page ->
            when (page) {
                0 -> IntroPage(topArtists, isActive = true)
                1 -> TotalTimePage(totalMinutes, isActive = true)
                2 -> TopSongPage(topSongs.firstOrNull(), topSongStats?.songCountListened ?: 0, isActive = true)
                3 -> TopArtistsPage(topArtists, isActive = true)
                4 -> SummaryPage(topSongs, totalMinutes, topArtists.firstOrNull()?.artist?.name)
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
                    i < currentPage -> 1f
                    i == currentPage -> currentProgress
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
        if (currentPage > 1) {
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
fun IntroPage(topArtists: List<com.cgens67.avidtune.db.entities.Artist>, isActive: Boolean) {
    var phase by remember { mutableIntStateOf(0) }
    
    LaunchedEffect(isActive) {
        if (isActive) {
            phase = 0
            kotlinx.coroutines.delay(1000)
            phase = 1
            kotlinx.coroutines.delay(2000)
            phase = 2
        } else {
            phase = 0
        }
    }

    // Meteor race animations
    val raceProgress by animateFloatAsState(
        targetValue = if (phase >= 1) 1f else 0f,
        animationSpec = tween(2000, easing = FastOutSlowInEasing),
        label = "race"
    )
    
    val textMeasurer = rememberTextMeasurer()

    Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
        AnimatedVisibility(visible = phase == 0, enter = fadeIn(), exit = fadeOut()) {
            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                Text(stringResource(R.string.insight_intro_ready), fontSize = 32.sp, color = Color.White, fontWeight = FontWeight.Bold)
                Text(stringResource(R.string.insight_intro_ready_2), fontSize = 32.sp, color = Color.White, fontWeight = FontWeight.Bold)
            }
        }
        
        if (phase >= 1 && phase < 2) {
            Canvas(modifier = Modifier.fillMaxSize()) {
                val width = size.width
                val height = size.height
                
                topArtists.take(5).forEachIndexed { index, artist ->
                    // Top artist goes fastest
                    val speed = 1f - (index * 0.15f) 
                    val startX = -300f
                    val endX = width + 300f
                    
                    val currentX = lerp(startX, endX, raceProgress * speed)
                    val currentY = (height / 6f) * (index + 1)
                    
                    drawLine(
                        color = Color.White.copy(alpha = 0.5f),
                        start = Offset(currentX - 150f, currentY),
                        end = Offset(currentX, currentY),
                        strokeWidth = 8f,
                        cap = StrokeCap.Round
                    )
                    drawCircle(
                        color = Color.Yellow,
                        radius = 15f,
                        center = Offset(currentX, currentY)
                    )
                    
                    drawText(
                        textMeasurer = textMeasurer,
                        text = artist.artist.name,
                        topLeft = Offset(currentX + 25f, currentY - 20f),
                        style = androidx.compose.ui.text.TextStyle(
                            color = Color.White,
                            fontSize = 18.sp,
                            fontWeight = FontWeight.Bold
                        )
                    )
                }
            }
        }
        
        AnimatedVisibility(
            visible = phase == 2, 
            enter = fadeIn(tween(500)) + scaleIn(initialScale = 0.5f, animationSpec = spring(dampingRatio = 0.6f)), 
            exit = fadeOut()
        ) {
            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                Text(stringResource(R.string.insight_top_artist_reveal), fontSize = 24.sp, color = Color.White.copy(alpha = 0.8f))
                Spacer(Modifier.height(16.dp))
                Text(topArtists.firstOrNull()?.artist?.name ?: "N/A", fontSize = 48.sp, color = Color.White, fontWeight = FontWeight.Black, textAlign = TextAlign.Center)
            }
        }
    }
}

@Composable
fun TotalTimePage(minutes: Long, isActive: Boolean) {
    val scale by animateFloatAsState(
        targetValue = if (isActive) 1f else 0.8f,
        animationSpec = spring(dampingRatio = 0.5f, stiffness = 100f),
        label = "scale"
    )

    val comparison = when {
        minutes == 0L -> stringResource(R.string.insight_time_0)
        minutes in 1L..10L -> stringResource(R.string.insight_time_1_10)
        minutes in 11L..20L -> stringResource(R.string.insight_time_11_20)
        minutes in 21L..50L -> stringResource(R.string.insight_time_21_50)
        minutes in 51L..80L -> stringResource(R.string.insight_time_51_80)
        minutes in 81L..200L -> stringResource(R.string.insight_time_90_200)
        minutes in 201L..500L -> stringResource(R.string.insight_time_300_500)
        else -> stringResource(R.string.insight_time_600_plus)
    }

    Box(
        modifier = Modifier.fillMaxSize(),
        contentAlignment = Alignment.Center
    ) {
        AnimatedVisibility(visible = isActive, enter = fadeIn() + slideInVertically(initialOffsetY = { 100 })) {
            Column(
                horizontalAlignment = Alignment.CenterHorizontally, 
                modifier = Modifier.padding(32.dp).scale(scale)
            ) {
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
                Spacer(Modifier.height(32.dp))
                Text(
                    text = comparison,
                    fontSize = 16.sp,
                    color = Color.White.copy(alpha = 0.8f),
                    textAlign = TextAlign.Center,
                    fontWeight = FontWeight.Medium,
                    modifier = Modifier
                        .background(Color.Black.copy(alpha = 0.2f), RoundedCornerShape(16.dp))
                        .padding(16.dp)
                )
            }
        }
    }
}

@Composable
fun TopSongPage(song: com.cgens67.avidtune.db.entities.Song?, playCount: Int, isActive: Boolean) {
    val scale by animateFloatAsState(
        targetValue = if (isActive) 1f else 0.5f,
        animationSpec = spring(dampingRatio = 0.5f, stiffness = 100f),
        label = "scale"
    )

    Box(
        modifier = Modifier.fillMaxSize(),
        contentAlignment = Alignment.Center
    ) {
        AnimatedVisibility(visible = isActive, enter = fadeIn() + slideInVertically(initialOffsetY = { 100 })) {
            Column(
                horizontalAlignment = Alignment.CenterHorizontally, 
                modifier = Modifier.padding(32.dp)
            ) {
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
                        .scale(scale)
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
}

@Composable
fun TopArtistsPage(artists: List<com.cgens67.avidtune.db.entities.Artist>, isActive: Boolean) {
    Box(
        modifier = Modifier.fillMaxSize(),
        contentAlignment = Alignment.Center
    ) {
        AnimatedVisibility(visible = isActive, enter = fadeIn() + slideInVertically(initialOffsetY = { 100 })) {
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
}

@Composable
fun SummaryPage(topSongs: List<com.cgens67.avidtune.db.entities.Song>, totalMinutes: Long, topArtistName: String?) {
    val context = LocalContext.current
    val coroutineScope = rememberCoroutineScope()

    Box(
        modifier = Modifier.fillMaxSize(),
        contentAlignment = Alignment.Center
    ) {
        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            modifier = Modifier.padding(24.dp)
        ) {
            // "Receipt" Style Card
            Box(
                modifier = Modifier
                    .fillMaxWidth(0.9f)
                    .shadow(16.dp, RoundedCornerShape(16.dp))
                    .background(Color(0xFFFDFDFD), RoundedCornerShape(16.dp))
                    .padding(24.dp)
            ) {
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    Text(
                        text = stringResource(R.string.insight_summary_title),
                        fontSize = 22.sp,
                        color = Color.Black,
                        fontWeight = FontWeight.Black,
                        textAlign = TextAlign.Center,
                        letterSpacing = 1.sp
                    )

                    Spacer(Modifier.height(16.dp))

                    Canvas(modifier = Modifier.fillMaxWidth().height(2.dp)) {
                        drawLine(
                            color = Color.LightGray,
                            start = Offset(0f, size.height / 2),
                            end = Offset(size.width, size.height / 2),
                            strokeWidth = 2.dp.toPx(),
                            pathEffect = androidx.compose.ui.graphics.PathEffect.dashPathEffect(floatArrayOf(10f, 10f), 0f)
                        )
                    }

                    Spacer(Modifier.height(16.dp))

                    Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                        Column {
                            Text(stringResource(R.string.insight_minutes_spent).uppercase(), color = Color.Gray, fontSize = 10.sp, fontWeight = FontWeight.Bold)
                            Text("$totalMinutes", color = Color.Black, fontSize = 24.sp, fontWeight = FontWeight.Bold)
                        }
                        Column(horizontalAlignment = Alignment.End) {
                            Text(stringResource(R.string.insight_artists_title).replace("Your", "Top").uppercase(), color = Color.Gray, fontSize = 10.sp, fontWeight = FontWeight.Bold)
                            Text(topArtistName ?: "N/A", color = Color.Black, fontSize = 20.sp, fontWeight = FontWeight.Bold, maxLines = 1, overflow = TextOverflow.Ellipsis, modifier = Modifier.width(120.dp), textAlign = TextAlign.End)
                        }
                    }

                    Spacer(Modifier.height(16.dp))

                    Canvas(modifier = Modifier.fillMaxWidth().height(2.dp)) {
                        drawLine(
                            color = Color.LightGray,
                            start = Offset(0f, size.height / 2),
                            end = Offset(size.width, size.height / 2),
                            strokeWidth = 2.dp.toPx(),
                            pathEffect = androidx.compose.ui.graphics.PathEffect.dashPathEffect(floatArrayOf(10f, 10f), 0f)
                        )
                    }

                    Spacer(Modifier.height(16.dp))

                    Text("TOP 5", color = Color.Gray, fontSize = 12.sp, fontWeight = FontWeight.Bold, modifier = Modifier.align(Alignment.Start))
                    Spacer(Modifier.height(8.dp))
                    
                    topSongs.take(5).forEachIndexed { index, song ->
                        Row(modifier = Modifier.fillMaxWidth().padding(vertical = 4.dp), verticalAlignment = Alignment.CenterVertically) {
                            Text("${index + 1}", color = Color.Black, fontSize = 14.sp, fontWeight = FontWeight.Bold, modifier = Modifier.width(20.dp))
                            Text(
                                text = song.song.title,
                                color = Color.Black,
                                fontSize = 14.sp,
                                fontWeight = FontWeight.Medium,
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis
                            )
                        }
                    }
                    
                    Spacer(Modifier.height(24.dp))
                    
                    // Fake barcode
                    Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.Center) {
                        val bars = listOf(2, 4, 1, 3, 2, 5, 1, 2, 4, 2, 1, 3, 2, 4)
                        bars.forEach { width ->
                            Box(modifier = Modifier.padding(horizontal = 1.dp).width(width.dp).height(30.dp).background(Color.Black))
                        }
                    }
                }
            }
            
            Spacer(Modifier.height(32.dp))

            Row(horizontalArrangement = Arrangement.spacedBy(16.dp)) {
                Button(
                    onClick = {
                        val shareIntent = Intent().apply {
                            action = Intent.ACTION_SEND
                            type = "text/plain"
                            val topSongText = topSongs.firstOrNull()?.song?.title ?: "N/A"
                            putExtra(Intent.EXTRA_TEXT, context.getString(R.string.insight_share_text, topArtistName ?: "N/A", totalMinutes, topSongText))
                        }
                        context.startActivity(Intent.createChooser(shareIntent, context.getString(R.string.insight_share)))
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = Color.White, contentColor = Color.Black)
                ) {
                    Icon(painter = painterResource(R.drawable.share), contentDescription = null, modifier = Modifier.size(18.dp))
                    Spacer(Modifier.width(8.dp))
                    Text(stringResource(R.string.insight_share), fontWeight = FontWeight.Bold)
                }

                Button(
                    onClick = {
                        coroutineScope.launch(Dispatchers.IO) {
                            downloadInsightReport(context, topSongs, totalMinutes, topArtistName)
                        }
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = Color.White, contentColor = Color.Black)
                ) {
                    Icon(painter = painterResource(R.drawable.download), contentDescription = null, modifier = Modifier.size(18.dp))
                    Spacer(Modifier.width(8.dp))
                    Text(stringResource(R.string.insight_download), fontWeight = FontWeight.Bold)
                }
            }
        }
    }
}

private fun downloadInsightReport(context: android.content.Context, topSongs: List<com.cgens67.avidtune.db.entities.Song>, totalMinutes: Long, topArtist: String?) {
    try {
        val width = 1080
        val height = 1920
        val bitmap = Bitmap.createBitmap(width, height, Bitmap.Config.ARGB_8888)
        val canvas = android.graphics.Canvas(bitmap)
        
        val bgPaint = Paint().apply {
            shader = LinearGradient(0f, 0f, 0f, height.toFloat(), Color(0xFF141E30).toArgb(), Color(0xFF243B55).toArgb(), Shader.TileMode.CLAMP)
        }
        canvas.drawRect(0f, 0f, width.toFloat(), height.toFloat(), bgPaint)
        
        val textPaint = TextPaint().apply {
            color = android.graphics.Color.WHITE
            textSize = 80f
            typeface = Typeface.create(Typeface.DEFAULT, Typeface.BOLD)
            textAlign = Paint.Align.CENTER
            isAntiAlias = true
        }
        
        canvas.drawText("AvidTune Insight 2026", width / 2f, 250f, textPaint)
        
        textPaint.textSize = 45f
        textPaint.typeface = Typeface.create(Typeface.DEFAULT, Typeface.NORMAL)
        textPaint.color = android.graphics.Color.LTGRAY
        canvas.drawText("Total Listening Time", width / 2f, 450f, textPaint)
        
        textPaint.textSize = 100f
        textPaint.typeface = Typeface.create(Typeface.DEFAULT, Typeface.BOLD)
        textPaint.color = android.graphics.Color.WHITE
        canvas.drawText("$totalMinutes Minutes", width / 2f, 570f, textPaint)
        
        textPaint.textSize = 45f
        textPaint.typeface = Typeface.create(Typeface.DEFAULT, Typeface.NORMAL)
        textPaint.color = android.graphics.Color.LTGRAY
        canvas.drawText("Top Artist", width / 2f, 750f, textPaint)
        
        textPaint.textSize = 90f
        textPaint.typeface = Typeface.create(Typeface.DEFAULT, Typeface.BOLD)
        textPaint.color = android.graphics.Color.WHITE
        canvas.drawText(topArtist ?: "N/A", width / 2f, 870f, textPaint)
        
        textPaint.textSize = 45f
        textPaint.typeface = Typeface.create(Typeface.DEFAULT, Typeface.NORMAL)
        textPaint.color = android.graphics.Color.LTGRAY
        canvas.drawText("Top Songs", width / 2f, 1050f, textPaint)
        
        textPaint.textAlign = Paint.Align.LEFT
        textPaint.textSize = 55f
        textPaint.typeface = Typeface.create(Typeface.DEFAULT, Typeface.BOLD)
        textPaint.color = android.graphics.Color.WHITE
        var y = 1180f
        topSongs.take(5).forEachIndexed { index, song ->
            canvas.drawText("${index + 1}. ${song.song.title}", 150f, y, textPaint)
            y += 100f
        }
        
        ComposeToImage.saveBitmapAsFile(context, bitmap, "AvidTune_Insight_${System.currentTimeMillis()}")
        
        Handler(Looper.getMainLooper()).post {
            Toast.makeText(context, context.getString(R.string.insight_downloaded_toast), Toast.LENGTH_SHORT).show()
        }
    } catch (e: Exception) {
        e.printStackTrace()
    }
}
--- START OF FILE avidtune-master/app/src/main/java/com/cgens67/avidtune/ui/screens/StatsScreen.kt ---
package com.cgens67.avidtune.ui.screens

import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.background
import androidx.compose.foundation.combinedClickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.WindowInsetsSides
import androidx.compose.foundation.layout.asPaddingValues
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.only
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.windowInsetsPadding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.pluralStringResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.navigation.NavController
import com.cgens67.innertube.models.WatchEndpoint
import com.cgens67.avidtune.LocalPlayerAwareWindowInsets
import com.cgens67.avidtune.LocalPlayerConnection
import com.cgens67.avidtune.R
import com.cgens67.avidtune.constants.StatPeriod
import com.cgens67.avidtune.extensions.toMediaItem
import com.cgens67.avidtune.extensions.togglePlayPause
import com.cgens67.avidtune.models.toMediaMetadata
import com.cgens67.avidtune.playback.queues.ListQueue
import com.cgens67.avidtune.playback.queues.YouTubeQueue
import com.cgens67.avidtune.ui.component.ChoiceChipsRow
import com.cgens67.avidtune.ui.component.HideOnScrollFAB
import com.cgens67.avidtune.ui.component.IconButton
import com.cgens67.avidtune.ui.component.LocalAlbumsGrid
import com.cgens67.avidtune.ui.component.LocalArtistsGrid
import com.cgens67.avidtune.ui.component.LocalMenuState
import com.cgens67.avidtune.ui.component.LocalSongsGrid
import com.cgens67.avidtune.ui.component.NavigationTitle
import com.cgens67.avidtune.ui.menu.AlbumMenu
import com.cgens67.avidtune.ui.menu.ArtistMenu
import com.cgens67.avidtune.ui.menu.SongMenu
import com.cgens67.avidtune.ui.utils.backToMain
import com.cgens67.avidtune.utils.joinByBullet
import com.cgens67.avidtune.utils.makeTimeString
import com.cgens67.avidtune.viewmodels.StatsViewModel
import kotlinx.coroutines.launch
import java.time.LocalDateTime
import java.time.format.DateTimeFormatter

@OptIn(ExperimentalMaterial3Api::class, ExperimentalFoundationApi::class)
@Composable
fun StatsScreen(
    navController: NavController,
    viewModel: StatsViewModel = hiltViewModel(),
) {
    val menuState = LocalMenuState.current
    val haptic = LocalHapticFeedback.current
    val playerConnection = LocalPlayerConnection.current ?: return
    val isPlaying by playerConnection.isPlaying.collectAsState()
    val mediaMetadata by playerConnection.mediaMetadata.collectAsState()
    val context = LocalContext.current

    val indexChips by viewModel.indexChips.collectAsState()
    val mostPlayedSongs by viewModel.mostPlayedSongs.collectAsState()
    val mostPlayedSongsStats by viewModel.mostPlayedSongsStats.collectAsState()
    val mostPlayedArtists by viewModel.mostPlayedArtists.collectAsState()
    val mostPlayedAlbums by viewModel.mostPlayedAlbums.collectAsState()
    val firstEvent by viewModel.firstEvent.collectAsState()
    val currentDate = LocalDateTime.now()

    val coroutineScope = rememberCoroutineScope()
    val lazyListState = rememberLazyListState()
    val selectedOption by viewModel.selectedOption.collectAsState()

    // BottomSheet para Insight
    var showInsightBottomSheet by remember { mutableStateOf(false) }
    val sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)

    val weeklyDates =
        if (currentDate != null && firstEvent != null) {
            generateSequence(currentDate) { it.minusWeeks(1) }
                .takeWhile { it.isAfter(firstEvent?.event?.timestamp?.minusWeeks(1)) }
                .mapIndexed { index, date ->
                    val endDate = date.plusWeeks(1).minusDays(1).coerceAtMost(currentDate)
                    val formatter = DateTimeFormatter.ofPattern("dd MMM")

                    val startDateFormatted = formatter.format(date)
                    val endDateFormatted = formatter.format(endDate)

                    val startMonth = date.month
                    val endMonth = endDate.month
                    val startYear = date.year
                    val endYear = endDate.year

                    val text =
                        when {
                            startYear != currentDate.year -> "$startDateFormatted, $startYear - $endDateFormatted, $endYear"
                            startMonth != endMonth -> "$startDateFormatted - $endDateFormatted"
                            else -> "${date.dayOfMonth} - $endDateFormatted"
                        }
                    Pair(index, text)
                }.toList()
        } else {
            emptyList()
        }

    val monthlyDates =
        if (currentDate != null && firstEvent != null) {
            generateSequence(
                currentDate.plusMonths(1).withDayOfMonth(1).minusDays(1)
            ) { it.minusMonths(1) }
                .takeWhile {
                    it.isAfter(
                        firstEvent
                            ?.event
                            ?.timestamp
                            ?.withDayOfMonth(1),
                    )
                }.mapIndexed { index, date ->
                    val formatter = DateTimeFormatter.ofPattern("MMM")
                    val formattedDate = formatter.format(date)
                    val text =
                        if (date.year != currentDate.year) {
                            "$formattedDate ${date.year}"
                        } else {
                            formattedDate
                        }
                    Pair(index, text)
                }.toList()
        } else {
            emptyList()
        }

    val yearlyDates =
        if (currentDate != null && firstEvent != null) {
            generateSequence(
                currentDate
                    .plusYears(1)
                    .withDayOfYear(1)
                    .minusDays(1),
            ) { it.minusYears(1) }
                .takeWhile {
                    it.isAfter(
                        firstEvent
                            ?.event
                            ?.timestamp,
                    )
                }.mapIndexed { index, date ->
                    Pair(index, "${date.year}")
                }.toList()
        } else {
            emptyList()
        }

    Box(modifier = Modifier.fillMaxSize()) {
        LazyColumn(
            state = lazyListState,
            contentPadding = LocalPlayerAwareWindowInsets.current
                .only(WindowInsetsSides.Horizontal + WindowInsetsSides.Bottom)
                .asPaddingValues(),
            modifier = Modifier.windowInsetsPadding(
                LocalPlayerAwareWindowInsets.current.only(WindowInsetsSides.Top)
            )
        ) {
            item {
                ChoiceChipsRow(
                    chips =
                        when (selectedOption) {
                            OptionStats.WEEKS -> weeklyDates
                            OptionStats.MONTHS -> monthlyDates
                            OptionStats.YEARS -> yearlyDates
                            OptionStats.CONTINUOUS -> {
                                listOf(
                                    StatPeriod.WEEK_1.ordinal to pluralStringResource(
                                        R.plurals.n_week,
                                        1,
                                        1
                                    ),
                                    StatPeriod.MONTH_1.ordinal to pluralStringResource(
                                        R.plurals.n_month,
                                        1,
                                        1
                                    ),
                                    StatPeriod.MONTH_3.ordinal to pluralStringResource(
                                        R.plurals.n_month,
                                        3,
                                        3
                                    ),
                                    StatPeriod.MONTH_6.ordinal to pluralStringResource(
                                        R.plurals.n_month,
                                        6,
                                        6
                                    ),
                                    StatPeriod.YEAR_1.ordinal to pluralStringResource(
                                        R.plurals.n_year,
                                        1,
                                        1
                                    ),
                                    StatPeriod.ALL.ordinal to stringResource(R.string.filter_all),
                                )
                            }
                        },
                    options =
                        listOf(
                            OptionStats.CONTINUOUS to stringResource(id = R.string.continuous),
                            OptionStats.WEEKS to stringResource(R.string.weeks),
                            OptionStats.MONTHS to stringResource(R.string.months),
                            OptionStats.YEARS to stringResource(R.string.years),
                        ),
                    selectedOption = selectedOption,
                    onSelectionChange = {
                        viewModel.selectedOption.value = it
                        viewModel.indexChips.value = 0
                    },
                    currentValue = indexChips,
                    onValueUpdate = { viewModel.indexChips.value = it },
                )
            }

            item(key = "mostPlayedSongs") {
                NavigationTitle(
                    title = "${mostPlayedSongsStats.size} ${stringResource(id = R.string.songs)}",
                    modifier = Modifier.animateItem(),
                )

                LazyRow(
                    modifier = Modifier.animateItem(),
                ) {
                    itemsIndexed(
                        items = mostPlayedSongsStats,
                        key = { _, song -> song.id },
                    ) { index, song ->
                        LocalSongsGrid(
                            title = "${index + 1}. ${song.title}",
                            subtitle =
                                joinByBullet(
                                    pluralStringResource(
                                        R.plurals.n_time,
                                        song.songCountListened,
                                        song.songCountListened,
                                    ),
                                    makeTimeString(song.timeListened),
                                ),
                            thumbnailUrl = song.thumbnailUrl,
                            isActive = song.id == mediaMetadata?.id,
                            isPlaying = isPlaying,
                            modifier =
                                Modifier
                                    .fillMaxWidth()
                                    .combinedClickable(
                                        onClick = {
                                            if (song.id == mediaMetadata?.id) {
                                                playerConnection.player.togglePlayPause()
                                            } else {
                                                playerConnection.playQueue(
                                                    YouTubeQueue(
                                                        endpoint = WatchEndpoint(song.id),
                                                        preloadItem = mostPlayedSongs[index].toMediaMetadata(),
                                                    ),
                                                )
                                            }
                                        },
                                        onLongClick = {
                                            haptic.performHapticFeedback(HapticFeedbackType.LongPress)
                                            menuState.show {
                                                SongMenu(
                                                    originalSong = mostPlayedSongs[index],
                                                    navController = navController,
                                                    onDismiss = menuState::dismiss,
                                                )
                                            }
                                        },
                                    )
                                    .animateItem(),
                        )
                    }
                }
            }

            item(key = "mostPlayedArtists") {
                NavigationTitle(
                    title = "${mostPlayedArtists.size} ${stringResource(id = R.string.artists)}",
                    modifier = Modifier.animateItem(),
                )

                LazyRow(
                    modifier = Modifier.animateItem(),
                ) {
                    itemsIndexed(
                        items = mostPlayedArtists,
                        key = { _, artist -> artist.id },
                    ) { index, artist ->
                        LocalArtistsGrid(
                            title = "${index + 1}. ${artist.artist.name}",
                            subtitle =
                                joinByBullet(
                                    pluralStringResource(
                                        R.plurals.n_time,
                                        artist.songCount,
                                        artist.songCount
                                    ),
                                    makeTimeString(artist.timeListened?.toLong()),
                                ),
                            thumbnailUrl = artist.artist.thumbnailUrl,
                            modifier =
                                Modifier
                                    .combinedClickable(
                                        onClick = {
                                            navController.navigate("artist/${artist.id}")
                                        },
                                        onLongClick = {
                                            haptic.performHapticFeedback(HapticFeedbackType.LongPress)
                                            menuState.show {
                                                ArtistMenu(
                                                    originalArtist = artist,
                                                    coroutineScope = coroutineScope,
                                                    onDismiss = menuState::dismiss,
                                                )
                                            }
                                        },
                                    )
                                    .animateItem(),
                        )
                    }
                }
            }

            item(key = "mostPlayedAlbums") {
                NavigationTitle(
                    title = "${mostPlayedAlbums.size} ${stringResource(id = R.string.albums)}",
                    modifier = Modifier.animateItem(),
                )

                if (mostPlayedAlbums.isNotEmpty()) {
                    LazyRow(
                        modifier = Modifier.animateItem(),
                    ) {
                        itemsIndexed(
                            items = mostPlayedAlbums,
                            key = { _, album -> album.id },
                        ) { index, album ->
                            LocalAlbumsGrid(
                                title = "${index + 1}. ${album.album.title}",
                                subtitle =
                                    joinByBullet(
                                        pluralStringResource(
                                            R.plurals.n_time,
                                            album.songCountListened!!,
                                            album.songCountListened
                                        ),
                                        makeTimeString(album.timeListened?.toLong()),
                                    ),
                                thumbnailUrl = album.album.thumbnailUrl,
                                isActive = album.id == mediaMetadata?.album?.id,
                                isPlaying = isPlaying,
                                modifier =
                                    Modifier
                                        .fillMaxWidth()
                                        .combinedClickable(
                                            onClick = {
                                                navController.navigate("album/${album.id}")
                                            },
                                            onLongClick = {
                                                haptic.performHapticFeedback(HapticFeedbackType.LongPress)
                                                menuState.show {
                                                    AlbumMenu(
                                                        originalAlbum = album,
                                                        navController = navController,
                                                        onDismiss = menuState::dismiss,
                                                    )
                                                }
                                            },
                                        )
                                        .animateItem(),
                            )
                        }
                    }
                }
            }
        }

        // FAB to shuffle most played songs
        if (mostPlayedSongs.isNotEmpty()) {
            HideOnScrollFAB(
                visible = true,
                lazyListState = lazyListState,
                icon = R.drawable.shuffle,
                onClick = {
                    playerConnection.playQueue(
                        ListQueue(
                            title = context.getString(R.string.most_played_songs),
                            items = mostPlayedSongs.map { it.toMediaMetadata().toMediaItem() }
                                .shuffled()
                        )
                    )
                }
            )
        }

        TopAppBar(
            title = { Text(stringResource(R.string.stats)) },
            navigationIcon = {
                IconButton(
                    onClick = navController::navigateUp,
                    onLongClick = navController::backToMain,
                ) {
                    Icon(
                        painterResource(R.drawable.arrow_back),
                        contentDescription = null,
                    )
                }
            },
            actions = {
                IconButton(
                    onClick = { showInsightBottomSheet = true },
                    modifier = Modifier.size(48.dp),
                    enabled = true,
                    onLongClick = {}
                ) {
                    Icon(
                        painter = painterResource(R.drawable.auto_awesome),
                        contentDescription = stringResource(R.string.avidtune_insight),
                        tint = Color(0xFF1DB954)
                    )
                }
            }

        )
    }

    // BottomSheet de Insight
    if (showInsightBottomSheet) {
        val totalMinutes = mostPlayedSongsStats.sumOf { it.timeListened ?: 0L } / 60000L

        ModalBottomSheet(
            onDismissRequest = { showInsightBottomSheet = false },
            sheetState = sheetState
        ) {
            InsightBottomSheetContent(
                totalMinutes = totalMinutes,
                onNavigateToFullInsight = {
                    coroutineScope.launch {
                        sheetState.hide()
                        showInsightBottomSheet = false
                    }
                    navController.navigate("insight")
                },
                onDismiss = {
                    coroutineScope.launch {
                        sheetState.hide()
                        showInsightBottomSheet = false
                    }
                }
            )
        }
    }
}

@Composable
fun InsightBottomSheetContent(
    totalMinutes: Long,
    onNavigateToFullInsight: () -> Unit,
    onDismiss: () -> Unit
) {
    val currentYear = LocalDateTime.now().year
    val gradientColors = listOf(
        Color(0xFF8E2DE2),
        Color(0xFF4A00E0)
    )

    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(bottom = 32.dp, start = 16.dp, end = 16.dp, top = 8.dp)
    ) {
        // Header
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(bottom = 16.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Icon(
                painter = painterResource(R.drawable.auto_awesome),
                contentDescription = null,
                modifier = Modifier.size(32.dp),
                tint = Color(0xFF8E2DE2)
            )
            Spacer(modifier = Modifier.width(12.dp))
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = stringResource(R.string.avidtune_insight),
                    style = MaterialTheme.typography.headlineSmall,
                    fontWeight = FontWeight.Bold
                )
                Text(
                    text = stringResource(R.string.insight_discover_year),
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.7f)
                )
            }
        }

        // Card principal
        Card(
            modifier = Modifier
                .fillMaxWidth()
                .height(220.dp),
            shape = RoundedCornerShape(20.dp),
            colors = CardDefaults.cardColors(
                containerColor = Color.Transparent
            )
        ) {
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .background(
                        Brush.verticalGradient(gradientColors)
                    )
                    .padding(24.dp)
            ) {
                Column(
                    modifier = Modifier.fillMaxSize(),
                    verticalArrangement = Arrangement.SpaceBetween
                ) {
                    Text(
                        text = stringResource(R.string.insight_your_musical_year) + " $currentYear",
                        fontSize = 24.sp,
                        fontWeight = FontWeight.Bold,
                        color = Color.White
                    )

                    Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.Center) {
                        Column {
                            Text(
                                text = "$totalMinutes",
                                fontSize = 42.sp,
                                fontWeight = FontWeight.Black,
                                color = Color.White
                            )
                            Text(
                                text = stringResource(R.string.insight_minutes),
                                fontSize = 14.sp,
                                color = Color.White.copy(alpha = 0.9f)
                            )
                        }
                    }
                }
            }
        }

        Spacer(modifier = Modifier.height(24.dp))

        // Características
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceEvenly
        ) {
            InsightFeatureItem(
                iconRes = R.drawable.music_note,
                label = stringResource(R.string.insight_top_songs)
            )
            InsightFeatureItem(
                iconRes = R.drawable.artist,
                label = stringResource(R.string.insight_top_artists)
            )
            InsightFeatureItem(
                iconRes = R.drawable.equalizer,
                label = stringResource(R.string.insight_full_stats)
            )
            InsightFeatureItem(
                iconRes = R.drawable.download,
                label = stringResource(R.string.insight_download_report)
            )
        }

        Spacer(modifier = Modifier.height(24.dp))

        // Botón para ver completo
        Button(
            onClick = onNavigateToFullInsight,
            modifier = Modifier.fillMaxWidth(),
            shape = RoundedCornerShape(12.dp)
        ) {
            Text(
                text = stringResource(R.string.insight_view_full),
                modifier = Modifier.padding(8.dp),
                fontSize = 16.sp,
                fontWeight = FontWeight.SemiBold
            )
        }
    }
}

@Composable
fun InsightFeatureItem(
    iconRes: Int,
    label: String
) {
    Column(
        horizontalAlignment = Alignment.CenterHorizontally,
        modifier = Modifier.width(80.dp)
    ) {
        Icon(
            painter = painterResource(id = iconRes),
            contentDescription = null,
            modifier = Modifier.size(32.dp),
            tint = MaterialTheme.colorScheme.primary
        )
        Spacer(modifier = Modifier.height(8.dp))
        Text(
            text = label,
            style = MaterialTheme.typography.labelSmall,
            textAlign = TextAlign.Center,
            fontSize = 11.sp,
            lineHeight = 14.sp,
            color = MaterialTheme.colorScheme.onSurface
        )
    }
}

enum class OptionStats { WEEKS, MONTHS, YEARS, CONTINUOUS }