package com.youmate.app.extractor

import com.youmate.app.model.AudioStreamItem
import com.youmate.app.model.MediaExtractionResult
import com.youmate.app.model.VideoStreamItem
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import org.schabi.newpipe.extractor.NewPipe
import org.schabi.newpipe.extractor.ServiceList
import org.schabi.newpipe.extractor.localization.ContentCountry
import org.schabi.newpipe.extractor.localization.Localization
import org.schabi.newpipe.extractor.stream.StreamInfo

object ExtractorHelper {

    private var isInitialized = false

    /**
     * Initializes NewPipeExtractor with OkHttp Downloader and default localization.
     */
    @Synchronized
    fun initialize() {
        if (!isInitialized) {
            val downloader = OkHttpDownloader()
            NewPipe.init(downloader, Localization.DEFAULT, ContentCountry.DEFAULT)
            isInitialized = true
        }
    }

    /**
     * Extracts a clean YouTube URL from shared text or input string.
     */
    fun extractCleanUrl(text: String): String? {
        val trimmed = text.trim()
        val regex = Regex("""https?://(?:www\.|m\.)?(?:youtube\.com/(?:watch\?v=|shorts/|live/)|youtu\.be/)[a-zA-Z0-9_\-]+[^\s]*""")
        val match = regex.find(trimmed)
        if (match != null) {
            return match.value
        }
        if (trimmed.startsWith("http://") || trimmed.startsWith("https://")) {
            return trimmed
        }
        return null
    }

    /**
     * Extracts video metadata, progressive streams, video-only streams, and audio streams.
     * Runs asynchronously on Dispatchers.IO.
     */
    suspend fun extractMedia(rawUrl: String): Result<MediaExtractionResult> = withContext(Dispatchers.IO) {
        runCatching {
            initialize()

            val cleanUrl = extractCleanUrl(rawUrl)
                ?: throw IllegalArgumentException("No se encontró una URL de YouTube válida.")

            // Fetch stream info using NewPipeExtractor
            val streamInfo = StreamInfo.getInfo(ServiceList.YouTube, cleanUrl)

            val title = streamInfo.name.orEmpty().ifBlank { "YouTube_Video" }
            val uploader = streamInfo.uploaderName.orEmpty().ifBlank { "Desconocido" }
            val thumbnailUrl = streamInfo.thumbnails?.lastOrNull()?.url
                ?: streamInfo.thumbnails?.firstOrNull()?.url
            val duration = streamInfo.duration

            // Collect video streams (both progressive video+audio and video-only)
            val videoList = mutableListOf<VideoStreamItem>()

            // 1. Progressive streams (contains both video and audio)
            streamInfo.videoStreams?.forEach { stream ->
                val formatSuffix = runCatching { stream.format?.suffix }.getOrNull()
                    ?: runCatching { stream.format?.name?.lowercase() }.getOrNull()
                    ?: "mp4"

                videoList.add(
                    VideoStreamItem(
                        resolution = stream.resolution.orEmpty().ifBlank { stream.quality.orEmpty() },
                        format = formatSuffix,
                        url = stream.url.orEmpty(),
                        isVideoOnly = false,
                        quality = stream.quality.orEmpty()
                    )
                )
            }

            // 2. Video-only DASH streams (e.g., 1080p, 1440p, 4K)
            streamInfo.videoOnlyStreams?.forEach { stream ->
                val formatSuffix = runCatching { stream.format?.suffix }.getOrNull()
                    ?: runCatching { stream.format?.name?.lowercase() }.getOrNull()
                    ?: "mp4"

                videoList.add(
                    VideoStreamItem(
                        resolution = stream.resolution.orEmpty().ifBlank { stream.quality.orEmpty() },
                        format = formatSuffix,
                        url = stream.url.orEmpty(),
                        isVideoOnly = true,
                        quality = stream.quality.orEmpty()
                    )
                )
            }

            // Collect audio streams
            val audioList = mutableListOf<AudioStreamItem>()
            streamInfo.audioStreams?.forEach { stream ->
                val formatSuffix = runCatching { stream.format?.suffix }.getOrNull()
                    ?: runCatching { stream.format?.name?.lowercase() }.getOrNull()
                    ?: "m4a"

                audioList.add(
                    AudioStreamItem(
                        format = formatSuffix,
                        bitrate = stream.averageBitrate,
                        url = stream.url.orEmpty(),
                        quality = stream.quality.orEmpty()
                    )
                )
            }

            // Fallback: If YouTube only provided progressive video streams (video+audio),
            // offer the progressive stream as an M4A audio option so audio download is always available.
            if (audioList.isEmpty()) {
                streamInfo.videoStreams?.firstOrNull { !it.url.isNullOrBlank() }?.let { progressive ->
                    audioList.add(
                        AudioStreamItem(
                            format = "m4a",
                            bitrate = 128,
                            url = progressive.url.orEmpty(),
                            quality = "128 kbps (Audio AAC)"
                        )
                    )
                }
            }

            MediaExtractionResult(
                title = title,
                uploader = uploader,
                thumbnailUrl = thumbnailUrl,
                durationSeconds = duration,
                videoStreams = videoList.filter { it.url.isNotBlank() },
                audioStreams = audioList.filter { it.url.isNotBlank() }
            )
        }
    }
}
