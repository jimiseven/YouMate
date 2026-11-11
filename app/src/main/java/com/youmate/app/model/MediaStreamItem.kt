package com.youmate.app.model

enum class MediaType {
    VIDEO, AUDIO
}

data class VideoStreamItem(
    val resolution: String,      // e.g., "1080p", "720p", "480p", "360p"
    val format: String,          // e.g., "mp4", "webm"
    val url: String,
    val isVideoOnly: Boolean,    // true if separate video stream
    val quality: String
)

data class AudioStreamItem(
    val format: String,          // e.g., "m4a", "opus", "webm"
    val bitrate: Int,            // e.g., 160, 128, 70 kbps
    val url: String,
    val quality: String
)

data class MediaExtractionResult(
    val title: String,
    val uploader: String,
    val thumbnailUrl: String?,
    val durationSeconds: Long,
    val videoStreams: List<VideoStreamItem>,
    val audioStreams: List<AudioStreamItem>
)
