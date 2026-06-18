package com.cgens67.avidtune.utils

import android.net.ConnectivityManager
import androidx.media3.common.PlaybackException
import com.cgens67.innertube.models.response.PlayerResponse
import com.cgens67.innertube.pages.NewPipeUtils
import com.cgens67.avidtune.constants.AudioQuality
import com.cgens67.avidtune.constants.VideoQuality
import com.cgens67.innertube.YouTube
import com.cgens67.innertube.models.YouTubeClient
import com.cgens67.innertube.models.YouTubeClient.Companion.ANDROID_VR_NO_AUTH
import com.cgens67.innertube.models.YouTubeClient.Companion.IOS
import com.cgens67.innertube.models.YouTubeClient.Companion.MOBILE
import com.cgens67.innertube.models.YouTubeClient.Companion.TVHTML5_SIMPLY_EMBEDDED_PLAYER
import com.cgens67.innertube.models.YouTubeClient.Companion.WEB
import com.cgens67.innertube.models.YouTubeClient.Companion.WEB_CREATOR
import com.cgens67.innertube.models.YouTubeClient.Companion.WEB_REMIX
import okhttp3.OkHttpClient
import okhttp3.Request
import timber.log.Timber

object YTPlayerUtils {
    private const val logTag = "YTPlayerUtils"

    private val httpClient = OkHttpClient.Builder()
        .proxy(YouTube.proxy)
        .build()

    private val MAIN_CLIENT: YouTubeClient = WEB_REMIX

    private val STREAM_FALLBACK_CLIENTS: Array<YouTubeClient> = arrayOf(
        ANDROID_VR_NO_AUTH,
        MOBILE,
        TVHTML5_SIMPLY_EMBEDDED_PLAYER,
        IOS,
        WEB,
        WEB_CREATOR
    )
    
    data class PlaybackData(
        val audioConfig: PlayerResponse.PlayerConfig.AudioConfig?,
        val videoDetails: PlayerResponse.VideoDetails?,
        val playbackTracking: PlayerResponse.PlaybackTracking?,
        val format: PlayerResponse.StreamingData.Format,
        val streamUrl: String,
        val streamExpiresInSeconds: Int,
    )

    suspend fun playerResponseForPlayback(
        videoId: String,
        playlistId: String? = null,
        audioQuality: AudioQuality,
        videoQuality: VideoQuality = VideoQuality.P1080,
        enableVideo: Boolean = true,
        connectivityManager: ConnectivityManager,
    ): Result<PlaybackData> = runCatching {
        Timber.tag(logTag).d("Fetching player response for videoId: $videoId, playlistId: $playlistId")

        val signatureTimestamp = getSignatureTimestampOrNull(videoId)
        Timber.tag(logTag).d("Signature timestamp: $signatureTimestamp")

        val isLoggedIn = YouTube.cookie != null
        val sessionId =
            if (isLoggedIn) {
                YouTube.dataSyncId
            } else {
                YouTube.visitorData
            }
        Timber.tag(logTag).d("Session authentication status: ${if (isLoggedIn) "Logged in" else "Not logged in"}")

        val mainPlayerResponse =
            YouTube.player(videoId, playlistId, MAIN_CLIENT, signatureTimestamp).getOrThrow()
        
        var bestFormat: PlayerResponse.StreamingData.Format? = null
        var bestStreamUrl: String? = null
        var bestStreamExpiresInSeconds: Int? = null
        var bestAudioConfig: PlayerResponse.PlayerConfig.AudioConfig? = null
        var bestVideoDetails: PlayerResponse.VideoDetails? = null
        var bestPlaybackTracking: PlayerResponse.PlaybackTracking? = null

        for (clientIndex in (-1 until STREAM_FALLBACK_CLIENTS.size)) {
            val client = if (clientIndex == -1) MAIN_CLIENT else STREAM_FALLBACK_CLIENTS[clientIndex]

            if (clientIndex != -1 && client.loginRequired && !isLoggedIn && YouTube.cookie == null) {
                Timber.tag(logTag).d("Skipping client ${client.clientName} - requires login but user is not logged in")
                continue
            }

            val streamPlayerResponse = if (clientIndex == -1) mainPlayerResponse else YouTube.player(videoId, playlistId, client, signatureTimestamp).getOrNull()

            if (streamPlayerResponse?.playabilityStatus?.status == "OK") {
                val format = findFormat(streamPlayerResponse, audioQuality, videoQuality, enableVideo, connectivityManager)

                if (format != null) {
                    val streamUrl = findUrlOrNull(format, videoId)
                    if (streamUrl != null && validateStatus(streamUrl)) {
                        if (enableVideo) {
                            val currentBestHeight = bestFormat?.height ?: 0
                            val newHeight = format.height ?: 0
                            if (newHeight >= currentBestHeight) {
                                bestFormat = format
                                bestStreamUrl = streamUrl
                                bestStreamExpiresInSeconds = streamPlayerResponse.streamingData?.expiresInSeconds
                                bestAudioConfig = streamPlayerResponse.playerConfig?.audioConfig
                                bestVideoDetails = streamPlayerResponse.videoDetails
                                bestPlaybackTracking = streamPlayerResponse.playbackTracking
                            }

                            if (newHeight >= videoQuality.height) {
                                break
                            }
                        } else {
                            bestFormat = format
                            bestStreamUrl = streamUrl
                            bestStreamExpiresInSeconds = streamPlayerResponse.streamingData?.expiresInSeconds
                            bestAudioConfig = streamPlayerResponse.playerConfig?.audioConfig
                            bestVideoDetails = streamPlayerResponse.videoDetails
                            bestPlaybackTracking = streamPlayerResponse.playbackTracking
                            break
                        }
                    }
                }
            }
        }

        if (bestFormat == null || bestStreamUrl == null || bestStreamExpiresInSeconds == null) {
            if (mainPlayerResponse.playabilityStatus.status != "OK") {
                val errorReason = mainPlayerResponse.playabilityStatus.reason
                Timber.tag(logTag).e("Playability status not OK: $errorReason")
                throw PlaybackException(
                    errorReason,
                    null,
                    PlaybackException.ERROR_CODE_REMOTE_ERROR
                )
            }
            throw Exception("Could not find suitable format or stream URL")
        }

        PlaybackData(
            bestAudioConfig,
            bestVideoDetails,
            bestPlaybackTracking,
            bestFormat,
            bestStreamUrl,
            bestStreamExpiresInSeconds,
        )
    }

    suspend fun playerResponseForMetadata(
        videoId: String,
        playlistId: String? = null,
    ): Result<PlayerResponse> {
        Timber.tag(logTag).d("Fetching metadata-only player response for videoId: $videoId using MAIN_CLIENT: ${MAIN_CLIENT.clientName}")
        return YouTube.player(videoId, playlistId, client = WEB_REMIX)
            .onSuccess { Timber.tag(logTag).d("Successfully fetched metadata") }
            .onFailure { Timber.tag(logTag).e(it, "Failed to fetch metadata") }
    }

    private fun findFormat(
        playerResponse: PlayerResponse,
        audioQuality: AudioQuality,
        videoQuality: VideoQuality,
        enableVideo: Boolean,
        connectivityManager: ConnectivityManager,
    ): PlayerResponse.StreamingData.Format? {
        Timber.tag(logTag).d("Finding format with audioQuality: $audioQuality, enableVideo: $enableVideo, videoQuality: $videoQuality")

        if (enableVideo) {
            val videoFormat = playerResponse.streamingData?.formats
                ?.filter { it.width != null && (it.height ?: 0) <= videoQuality.height }
                ?.maxByOrNull { it.height ?: 0 }

            if (videoFormat != null) {
                Timber.tag(logTag).d("Selected video format: ${videoFormat.mimeType}, height: ${videoFormat.height}, bitrate: ${videoFormat.bitrate}")
                return videoFormat
            }
        }

        val format = playerResponse.streamingData?.adaptiveFormats
            ?.filter { it.isAudio }
            ?.maxByOrNull {
                it.bitrate * when (audioQuality) {
                    AudioQuality.AUTO -> if (connectivityManager.isActiveNetworkMetered) -1 else 1
                    AudioQuality.HIGH -> 1
                    AudioQuality.LOW -> -1
                } + (if (it.mimeType.startsWith("audio/webm")) 10240 else 0) // prefer opus stream
            }

        if (format != null) {
            Timber.tag(logTag).d("Selected audio format: ${format.mimeType}, bitrate: ${format.bitrate}")
        } else {
            Timber.tag(logTag).d("No suitable audio format found")
        }

        return format
    }

    private fun validateStatus(url: String): Boolean {
        Timber.tag(logTag).d("Validating stream URL status")
        try {
            val requestBuilder = Request.Builder()
                .head()
                .url(url)
            val response = httpClient.newCall(requestBuilder.build()).execute()
            val isSuccessful = response.isSuccessful
            Timber.tag(logTag).d("Stream URL validation result: ${if (isSuccessful) "Success" else "Failed"} (${response.code})")
            return isSuccessful
        } catch (e: Exception) {
            Timber.tag(logTag).e(e, "Stream URL validation failed with exception")
            reportException(e)
        }
        return false
    }

    private fun getSignatureTimestampOrNull(
        videoId: String
    ): Int? {
        Timber.tag(logTag).d("Getting signature timestamp for videoId: $videoId")
        return NewPipeUtils.getSignatureTimestamp(videoId)
            .onSuccess { Timber.tag(logTag).d("Signature timestamp obtained: $it") }
            .onFailure {
                Timber.tag(logTag).e(it, "Failed to get signature timestamp")
                reportException(it)
            }
            .getOrNull()
    }

    private fun findUrlOrNull(
        format: PlayerResponse.StreamingData.Format,
        videoId: String
    ): String? {
        Timber.tag(logTag).d("Finding stream URL for format: ${format.mimeType}, videoId: $videoId")
        return NewPipeUtils.getStreamUrl(format, videoId)
            .onSuccess { Timber.tag(logTag).d("Stream URL obtained successfully") }
            .onFailure {
                Timber.tag(logTag).e(it, "Failed to get stream URL")
                reportException(it)
            }
            .getOrNull()
    }
}
