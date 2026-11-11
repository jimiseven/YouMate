package com.youmate.app.ui

import android.content.ClipboardManager
import android.content.Context
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.youmate.app.download.DownloadHelper
import com.youmate.app.extractor.ExtractorHelper
import com.youmate.app.model.AudioStreamItem
import com.youmate.app.model.MediaExtractionResult
import com.youmate.app.model.MediaType
import com.youmate.app.model.VideoStreamItem
import com.youmate.app.settings.SettingsHelper
import com.youmate.app.settings.StorageLocation
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

data class MainUiState(
    val urlInput: String = "",
    val isLoading: Boolean = false,
    val errorMessage: String? = null,
    val successMessage: String? = null,
    val showBottomSheet: Boolean = false,
    val showSettingsDialog: Boolean = false,
    val storageLocation: StorageLocation = StorageLocation.DOWNLOADS,
    val subfolder: String = "YouMate",
    val separateAudio: Boolean = true,
    val extractionResult: MediaExtractionResult? = null,
    val selectedTab: MediaType = MediaType.VIDEO,
    val selectedVideoFormat: String = "mp4",
    val selectedAudioFormat: String = "m4a",
    val selectedVideoStream: VideoStreamItem? = null,
    val selectedAudioStream: AudioStreamItem? = null
)

class MainViewModel : ViewModel() {

    private val _uiState = MutableStateFlow(MainUiState())
    val uiState: StateFlow<MainUiState> = _uiState.asStateFlow()

    fun onUrlChange(newUrl: String) {
        _uiState.update { it.copy(urlInput = newUrl, errorMessage = null) }
    }

    fun pasteFromClipboard(context: Context) {
        val clipboard = context.getSystemService(Context.CLIPBOARD_SERVICE) as? ClipboardManager
        val clipData = clipboard?.primaryClip
        if (clipData != null && clipData.itemCount > 0) {
            val text = clipData.getItemAt(0)?.text?.toString()?.trim()
            if (!text.isNullOrBlank()) {
                onUrlChange(text)
                // If it's a valid link, automatically trigger extraction
                if (ExtractorHelper.extractCleanUrl(text) != null) {
                    processUrl(text)
                }
            }
        }
    }

    fun processUrl(rawText: String) {
        if (rawText.isBlank()) {
            _uiState.update { it.copy(errorMessage = "Por favor, introduce o pega un enlace de YouTube.") }
            return
        }

        val cleanUrl = ExtractorHelper.extractCleanUrl(rawText)
        if (cleanUrl == null) {
            _uiState.update {
                it.copy(
                    errorMessage = "El enlace no parece ser una URL válida de YouTube.",
                    isLoading = false
                )
            }
            return
        }

        _uiState.update {
            it.copy(
                urlInput = cleanUrl,
                isLoading = true,
                errorMessage = null,
                successMessage = null
            )
        }

        viewModelScope.launch {
            val result = ExtractorHelper.extractMedia(cleanUrl)

            result.onSuccess { data ->
                val initialVideo = data.videoStreams.firstOrNull { !it.isVideoOnly && it.format.equals("mp4", ignoreCase = true) }
                    ?: data.videoStreams.firstOrNull { !it.isVideoOnly }
                    ?: data.videoStreams.firstOrNull { it.format.equals("mp4", ignoreCase = true) }
                    ?: data.videoStreams.firstOrNull()
                val initialAudio = data.audioStreams.firstOrNull { it.format.equals("m4a", ignoreCase = true) }
                    ?: data.audioStreams.firstOrNull()

                val initialVideoFormat = initialVideo?.format ?: "mp4"
                val initialAudioFormat = initialAudio?.format ?: "m4a"

                _uiState.update {
                    it.copy(
                        isLoading = false,
                        extractionResult = data,
                        showBottomSheet = true,
                        selectedTab = MediaType.VIDEO,
                        selectedVideoFormat = initialVideoFormat,
                        selectedAudioFormat = initialAudioFormat,
                        selectedVideoStream = initialVideo,
                        selectedAudioStream = initialAudio
                    )
                }
            }.onFailure { error ->
                _uiState.update {
                    it.copy(
                        isLoading = false,
                        errorMessage = error.localizedMessage ?: "Error al extraer datos del video."
                    )
                }
            }
        }
    }

    fun selectTab(tab: MediaType) {
        _uiState.update { it.copy(selectedTab = tab) }
    }

    fun selectVideoFormat(format: String) {
        _uiState.update { current ->
            val matchingStream = current.extractionResult?.videoStreams
                ?.filter { it.format.equals(format, ignoreCase = true) }
                ?.firstOrNull()

            current.copy(
                selectedVideoFormat = format,
                selectedVideoStream = matchingStream ?: current.selectedVideoStream
            )
        }
    }

    fun selectAudioFormat(format: String) {
        _uiState.update { current ->
            val matchingStream = current.extractionResult?.audioStreams
                ?.filter { it.format.equals(format, ignoreCase = true) }
                ?.firstOrNull()

            current.copy(
                selectedAudioFormat = format,
                selectedAudioStream = matchingStream ?: current.selectedAudioStream
            )
        }
    }

    fun selectVideoStream(stream: VideoStreamItem) {
        _uiState.update { it.copy(selectedVideoStream = stream, selectedVideoFormat = stream.format) }
    }

    fun selectAudioStream(stream: AudioStreamItem) {
        _uiState.update { it.copy(selectedAudioStream = stream, selectedAudioFormat = stream.format) }
    }

    fun dismissBottomSheet() {
        _uiState.update { it.copy(showBottomSheet = false) }
    }

    fun dismissMessages() {
        _uiState.update { it.copy(errorMessage = null, successMessage = null) }
    }

    fun openSettings(context: Context) {
        val loc = SettingsHelper.getStorageLocation(context)
        val sub = SettingsHelper.getSubfolder(context)
        val sep = SettingsHelper.isSeparateAudioEnabled(context)
        _uiState.update {
            it.copy(
                showSettingsDialog = true,
                storageLocation = loc,
                subfolder = sub,
                separateAudio = sep
            )
        }
    }

    fun dismissSettings() {
        _uiState.update { it.copy(showSettingsDialog = false) }
    }

    fun updateStorageLocation(context: Context, location: StorageLocation) {
        SettingsHelper.setStorageLocation(context, location)
        _uiState.update { it.copy(storageLocation = location) }
    }

    fun updateSubfolder(context: Context, subfolder: String) {
        SettingsHelper.setSubfolder(context, subfolder)
        _uiState.update { it.copy(subfolder = subfolder) }
    }

    fun updateSeparateAudio(context: Context, enabled: Boolean) {
        SettingsHelper.setSeparateAudioEnabled(context, enabled)
        _uiState.update { it.copy(separateAudio = enabled) }
    }

    fun showDownloadStatus(message: String, isError: Boolean = false) {
        _uiState.update {
            if (isError) {
                it.copy(errorMessage = message)
            } else {
                it.copy(successMessage = message)
            }
        }
    }

    fun startDownload(context: Context) {
        val state = _uiState.value
        val result = state.extractionResult ?: return

        val isAudio = state.selectedTab == MediaType.AUDIO
        val (streamUrl, format) = when (state.selectedTab) {
            MediaType.VIDEO -> {
                val stream = state.selectedVideoStream
                if (stream == null) {
                    _uiState.update { it.copy(errorMessage = "Selecciona una resolución de video.") }
                    return
                }
                Pair(stream.url, stream.format)
            }
            MediaType.AUDIO -> {
                val stream = state.selectedAudioStream
                if (stream == null) {
                    _uiState.update { it.copy(errorMessage = "Selecciona una calidad de audio.") }
                    return
                }
                Pair(stream.url, stream.format)
            }
        }

        // Close BottomSheet immediately
        _uiState.update { it.copy(showBottomSheet = false) }

        // Start download service with OkHttp
        DownloadHelper.startDownload(
            context = context,
            streamUrl = streamUrl,
            title = result.title,
            format = format,
            isAudio = isAudio
        ).onSuccess {
            _uiState.update {
                it.copy(
                    successMessage = "Iniciando descarga en segundo plano...",
                    urlInput = ""
                )
            }
        }.onFailure { err ->
            _uiState.update {
                it.copy(
                    errorMessage = "Error al iniciar la descarga: ${err.localizedMessage}"
                )
            }
        }
    }
}
