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

import java.util.Locale

object ExtractorHelper {

    private var isInitialized = false

    fun formatFileSize(bytes: Long): String {
        if (bytes <= 0L) return ""
        return when {
            bytes >= 1024L * 1024L * 1024L -> String.format(Locale.getDefault(), "%.1f GB", bytes / (1024.0 * 1024.0 * 1024.0))
            bytes >= 1024L * 1024L -> String.format(Locale.getDefault(), "%.1f MB", bytes / (1024.0 * 1024.0))
            bytes >= 1024L -> String.format(Locale.getDefault(), "%.0f KB", bytes / 1024.0)
            else -> "$bytes B"
        }
    }

    fun resolveStreamSizeBytes(url: String, bitrate: Int, durationSeconds: Long): Long {
        val clen = Regex("""[?&]clen=(\d+)""").find(url)?.groupValues?.get(1)?.toLongOrNull()
        if (clen != null && clen > 0L) {
            return clen
        }
        if (bitrate > 0 && durationSeconds > 0L) {
            return (bitrate.toLong() * durationSeconds) / 8L
        }
        return 0L
    }

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

    private fun parseResolutionHeight(resolution: String): Int {
        return Regex("""(\d+)""").find(resolution)?.groupValues?.get(1)?.toIntOrNull() ?: 0
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

            // Fetch stream info using NewPipeExtractor with iOS client enabled
            // to obtain all video resolutions (1080p, 720p, 480p, 360p, etc.) and audio streams
            val service = ServiceList.YouTube
            val extractor = service.getStreamExtractor(cleanUrl)

            runCatching {
                val field = extractor.javaClass.getDeclaredField("fetchIosClient")
                field.isAccessible = true
                field.setBoolean(extractor, true)
            }
            runCatching {
                val method = extractor.javaClass.getDeclaredMethod("setFetchIosClient", Boolean::class.javaPrimitiveType)
                method.isAccessible = true
                method.invoke(extractor, true)
            }

            val streamInfo = StreamInfo.getInfo(extractor)

            val title = streamInfo.name.orEmpty().ifBlank { "YouTube_Video" }
            val uploader = streamInfo.uploaderName.orEmpty().ifBlank { "Desconocido" }
            val thumbnailUrl = streamInfo.thumbnails?.lastOrNull()?.url
                ?: streamInfo.thumbnails?.firstOrNull()?.url
            val duration = streamInfo.duration

            // Collect video streams (both progressive video+audio and video-only DASH)
            val rawVideoList = mutableListOf<VideoStreamItem>()

            // 1. Progressive streams (contains both video and audio)
            streamInfo.videoStreams?.forEach { stream ->
                val formatSuffix = runCatching { stream.format?.suffix }.getOrNull()
                    ?: runCatching { stream.format?.name?.lowercase() }.getOrNull()
                    ?: "mp4"
                val url = stream.url.orEmpty()
                val bitrate = runCatching { stream.bitrate }.getOrDefault(0)
                val sizeBytes = resolveStreamSizeBytes(url, bitrate, duration)

                rawVideoList.add(
                    VideoStreamItem(
                        resolution = stream.resolution.orEmpty().ifBlank { stream.quality.orEmpty() },
                        format = formatSuffix,
                        url = url,
                        isVideoOnly = false,
                        quality = stream.quality.orEmpty(),
                        sizeBytes = sizeBytes,
                        formattedSize = formatFileSize(sizeBytes)
                    )
                )
            }

            // 2. Video-only DASH streams (e.g., 2160p, 1440p, 1080p, 720p, 480p)
            streamInfo.videoOnlyStreams?.forEach { stream ->
                val formatSuffix = runCatching { stream.format?.suffix }.getOrNull()
                    ?: runCatching { stream.format?.name?.lowercase() }.getOrNull()
                    ?: "mp4"
                val url = stream.url.orEmpty()
                val bitrate = runCatching { stream.bitrate }.getOrDefault(0)
                val sizeBytes = resolveStreamSizeBytes(url, bitrate, duration)

                rawVideoList.add(
                    VideoStreamItem(
                        resolution = stream.resolution.orEmpty().ifBlank { stream.quality.orEmpty() },
                        format = formatSuffix,
                        url = url,
                        isVideoOnly = true,
                        quality = stream.quality.orEmpty(),
                        sizeBytes = sizeBytes,
                        formattedSize = formatFileSize(sizeBytes)
                    )
                )
            }

            // Sort by resolution height descending (1080p > 720p > 480p > 360p),
            // prioritizing streams with integrated audio (!isVideoOnly), then deduplicate by (resolution, format)
            val sortedVideoList = rawVideoList
                .filter { it.url.isNotBlank() }
                .sortedWith(
                    compareByDescending<VideoStreamItem> { parseResolutionHeight(it.resolution) }
                        .thenBy { it.isVideoOnly }
                )
                .distinctBy { "${it.resolution.lowercase()}_${it.format.lowercase()}" }

            // Collect audio streams
            val rawAudioList = mutableListOf<AudioStreamItem>()
            streamInfo.audioStreams?.forEach { stream ->
                val formatSuffix = runCatching { stream.format?.suffix }.getOrNull()
                    ?: runCatching { stream.format?.name?.lowercase() }.getOrNull()
                    ?: "m4a"
                val url = stream.url.orEmpty()
                val bitrate = stream.averageBitrate
                val rawBitrate = runCatching { stream.bitrate }.getOrDefault(bitrate * 1000)
                val sizeBytes = resolveStreamSizeBytes(url, rawBitrate, duration)

                rawAudioList.add(
                    AudioStreamItem(
                        format = formatSuffix,
                        bitrate = bitrate,
                        url = url,
                        quality = stream.quality.orEmpty(),
                        sizeBytes = sizeBytes,
                        formattedSize = formatFileSize(sizeBytes)
                    )
                )
            }

            // Fallback: If YouTube only provided progressive video streams (video+audio),
            // offer the progressive stream as an M4A audio option so audio download is always available.
            if (rawAudioList.isEmpty()) {
                streamInfo.videoStreams?.firstOrNull { !it.url.isNullOrBlank() }?.let { progressive ->
                    val url = progressive.url.orEmpty()
                    val sizeBytes = resolveStreamSizeBytes(url, 128_000, duration)
                    rawAudioList.add(
                        AudioStreamItem(
                            format = "m4a",
                            bitrate = 128,
                            url = url,
                            quality = "128 kbps (Audio AAC)",
                            sizeBytes = sizeBytes,
                            formattedSize = formatFileSize(sizeBytes)
                        )
                    )
                }
            }

            val sortedAudioList = rawAudioList
                .filter { it.url.isNotBlank() }
                .sortedByDescending { it.bitrate }
                .distinctBy { "${it.bitrate}_${it.format.lowercase()}" }

            MediaExtractionResult(
                title = title,
                uploader = uploader,
                thumbnailUrl = thumbnailUrl,
                durationSeconds = duration,
                videoStreams = sortedVideoList,
                audioStreams = sortedAudioList
            )
        }
    }
}
