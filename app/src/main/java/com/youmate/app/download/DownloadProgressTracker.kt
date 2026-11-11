package com.youmate.app.download

import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow

data class ActiveDownloadState(
    val isDownloading: Boolean = false,
    val progressPercent: Int = 0,
    val downloadedFormatted: String = "",
    val totalFormatted: String = "",
    val title: String = "",
    val isSuccess: Boolean = false,
    val errorMessage: String? = null
)

object DownloadProgressTracker {

    private val _state = MutableStateFlow(ActiveDownloadState())
    val state: StateFlow<ActiveDownloadState> = _state.asStateFlow()

    fun start(title: String) {
        _state.value = ActiveDownloadState(
            isDownloading = true,
            progressPercent = 0,
            title = title
        )
    }

    fun updateProgress(title: String, percent: Int, downloaded: String, total: String) {
        _state.value = ActiveDownloadState(
            isDownloading = true,
            progressPercent = percent,
            downloadedFormatted = downloaded,
            totalFormatted = total,
            title = title
        )
    }

    fun complete(title: String) {
        _state.value = ActiveDownloadState(
            isDownloading = false,
            progressPercent = 100,
            title = title,
            isSuccess = true
        )
    }

    fun error(message: String) {
        _state.value = ActiveDownloadState(
            isDownloading = false,
            errorMessage = message
        )
    }

    fun reset() {
        _state.value = ActiveDownloadState()
    }
}
