package com.youmate.app.download

import android.content.Context

object DownloadHelper {

    /**
     * Sanitizes file names by removing characters invalid on Android / Linux / FAT filesystems.
     */
    fun sanitizeFileName(rawName: String): String {
        return rawName
            .replace(Regex("[\r\n\t\u0000-\u001F]"), " ")
            .replace(Regex("[\\\\/:*?\"<>|]"), "_")
            .replace(Regex("\\s+"), " ")
            .trim()
            .take(100)
    }

    /**
     * Returns the appropriate MIME type based on file extension.
     */
    fun getMimeType(extension: String): String {
        return when (extension.lowercase()) {
            "mp4" -> "video/mp4"
            "webm" -> "video/webm"
            "m4a" -> "audio/mp4"
            "opus" -> "audio/opus"
            "ogg" -> "audio/ogg"
            "mp3" -> "audio/mpeg"
            else -> "*/*"
        }
    }

    /**
     * Starts a high-speed background media download using OkHttp and Foreground Service.
     * Bypasses Android DownloadManager's HTTP 403 Forbidden restrictions on YouTube CDN.
     * Shows system progress notification and saves directly to public Downloads folder.
     *
     * @param context Android context
     * @param streamUrl Direct media stream URL
     * @param title Title of the YouTube video
     * @param format File extension (e.g. "mp4", "m4a", "webm")
     * @param isAudio Whether this download is an audio stream
     */
    fun startDownload(
        context: Context,
        streamUrl: String,
        title: String,
        format: String,
        isAudio: Boolean = false
    ): Result<Unit> {
        return runCatching {
            DownloadService.start(
                context = context,
                url = streamUrl,
                title = title,
                format = format,
                isAudio = isAudio
            )
        }
    }
}
