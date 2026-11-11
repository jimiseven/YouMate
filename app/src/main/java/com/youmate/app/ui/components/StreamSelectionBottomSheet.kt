package com.youmate.app.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Audiotrack
import androidx.compose.material.icons.filled.Download
import androidx.compose.material.icons.filled.Videocam
import androidx.compose.material3.*
import androidx.compose.material3.TabRowDefaults.tabIndicatorOffset
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import coil.compose.AsyncImage
import com.youmate.app.model.AudioStreamItem
import com.youmate.app.model.MediaType
import com.youmate.app.model.VideoStreamItem
import com.youmate.app.ui.MainUiState
import com.youmate.app.ui.theme.RedPrimary
import java.util.Locale

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun StreamSelectionBottomSheet(
    uiState: MainUiState,
    onDismissRequest: () -> Unit,
    onSelectTab: (MediaType) -> Unit,
    onSelectVideoFormat: (String) -> Unit,
    onSelectAudioFormat: (String) -> Unit,
    onSelectVideoStream: (VideoStreamItem) -> Unit,
    onSelectAudioStream: (AudioStreamItem) -> Unit,
    onConfirmDownload: () -> Unit
) {
    val extractionResult = uiState.extractionResult ?: return
    val sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)

    ModalBottomSheet(
        onDismissRequest = onDismissRequest,
        sheetState = sheetState,
        containerColor = MaterialTheme.colorScheme.surface,
        dragHandle = { BottomSheetDefaults.DragHandle() }
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 20.dp, vertical = 8.dp)
        ) {
            // Video Header (Thumbnail + Title + Details)
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(bottom = 16.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                if (!extractionResult.thumbnailUrl.isNullOrBlank()) {
                    AsyncImage(
                        model = extractionResult.thumbnailUrl,
                        contentDescription = "Miniatura",
                        modifier = Modifier
                            .width(110.dp)
                            .height(65.dp)
                            .clip(RoundedCornerShape(8.dp)),
                        contentScale = ContentScale.Crop
                    )
                    Spacer(modifier = Modifier.width(12.dp))
                }

                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = extractionResult.title,
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold,
                        maxLines = 2,
                        overflow = TextOverflow.Ellipsis
                    )
                    Spacer(modifier = Modifier.height(4.dp))
                    Text(
                        text = "${extractionResult.uploader} • ${formatDuration(extractionResult.durationSeconds)}",
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }

            // Tabs: VIDEO vs AUDIO
            TabRow(
                selectedTabIndex = if (uiState.selectedTab == MediaType.VIDEO) 0 else 1,
                containerColor = MaterialTheme.colorScheme.surfaceVariant,
                contentColor = RedPrimary,
                indicator = { tabPositions ->
                    TabRowDefaults.SecondaryIndicator(
                        Modifier.tabIndicatorOffset(tabPositions[if (uiState.selectedTab == MediaType.VIDEO) 0 else 1]),
                        color = RedPrimary
                    )
                }
            ) {
                Tab(
                    selected = uiState.selectedTab == MediaType.VIDEO,
                    onClick = { onSelectTab(MediaType.VIDEO) },
                    text = { Text("VIDEO", fontWeight = FontWeight.Bold) },
                    icon = { Icon(Icons.Default.Videocam, contentDescription = null) }
                )
                Tab(
                    selected = uiState.selectedTab == MediaType.AUDIO,
                    onClick = { onSelectTab(MediaType.AUDIO) },
                    text = { Text("AUDIO", fontWeight = FontWeight.Bold) },
                    icon = { Icon(Icons.Default.Audiotrack, contentDescription = null) }
                )
            }

            Spacer(modifier = Modifier.height(16.dp))

            when (uiState.selectedTab) {
                MediaType.VIDEO -> {
                    VideoSelectionSection(
                        videoStreams = extractionResult.videoStreams,
                        selectedFormat = uiState.selectedVideoFormat,
                        selectedStream = uiState.selectedVideoStream,
                        onFormatSelected = onSelectVideoFormat,
                        onStreamSelected = onSelectVideoStream
                    )
                }
                MediaType.AUDIO -> {
                    AudioSelectionSection(
                        audioStreams = extractionResult.audioStreams,
                        selectedFormat = uiState.selectedAudioFormat,
                        selectedStream = uiState.selectedAudioStream,
                        onFormatSelected = onSelectAudioFormat,
                        onStreamSelected = onSelectAudioStream
                    )
                }
            }

            Spacer(modifier = Modifier.height(20.dp))

            // Confirm Download Button
            Button(
                onClick = onConfirmDownload,
                modifier = Modifier
                    .fillMaxWidth()
                    .height(52.dp),
                shape = RoundedCornerShape(12.dp),
                colors = ButtonDefaults.buttonColors(
                    containerColor = RedPrimary,
                    contentColor = Color.White
                )
            ) {
                Icon(Icons.Default.Download, contentDescription = null)
                Spacer(modifier = Modifier.width(8.dp))
                Text(
                    text = when (uiState.selectedTab) {
                        MediaType.VIDEO -> "Descargar Video (${uiState.selectedVideoStream?.resolution.orEmpty()} ${uiState.selectedVideoFormat.uppercase()})"
                        MediaType.AUDIO -> "Descargar Audio (${uiState.selectedAudioStream?.bitrate ?: 0} kbps ${uiState.selectedAudioFormat.uppercase()})"
                    },
                    style = MaterialTheme.typography.labelLarge,
                    fontSize = 15.sp
                )
            }

            Spacer(modifier = Modifier.height(16.dp))
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun VideoSelectionSection(
    videoStreams: List<VideoStreamItem>,
    selectedFormat: String,
    selectedStream: VideoStreamItem?,
    onFormatSelected: (String) -> Unit,
    onStreamSelected: (VideoStreamItem) -> Unit
) {
    val formats = remember(videoStreams) {
        videoStreams.map { it.format.lowercase() }.distinct()
    }

    // Filter Chips for Video Formats (MP4, WEBM)
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        Text(
            text = "Formato:",
            style = MaterialTheme.typography.bodyMedium,
            fontWeight = FontWeight.Medium,
            modifier = Modifier.align(Alignment.CenterVertically)
        )
        formats.forEach { format ->
            val isSelected = format.equals(selectedFormat, ignoreCase = true)
            FilterChip(
                selected = isSelected,
                onClick = { onFormatSelected(format) },
                label = { Text(format.uppercase()) },
                colors = FilterChipDefaults.filterChipColors(
                    selectedContainerColor = RedPrimary.copy(alpha = 0.2f),
                    selectedLabelColor = RedPrimary
                )
            )
        }
    }

    Spacer(modifier = Modifier.height(10.dp))

    // Filter streams matching selected format
    val filteredStreams = remember(videoStreams, selectedFormat) {
        videoStreams
            .filter { it.format.equals(selectedFormat, ignoreCase = true) }
            .distinctBy { it.resolution }
    }

    Text(
        text = "Resolución:",
        style = MaterialTheme.typography.bodyMedium,
        fontWeight = FontWeight.Medium
    )

    Spacer(modifier = Modifier.height(8.dp))

    if (filteredStreams.isEmpty()) {
        Text(
            text = "No hay streams disponibles para este formato.",
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            modifier = Modifier.padding(vertical = 12.dp)
        )
    } else {
        LazyColumn(
            modifier = Modifier
                .fillMaxWidth()
                .heightIn(max = 240.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            items(filteredStreams) { stream ->
                val isSelected = stream == selectedStream
                val borderColor = if (isSelected) RedPrimary else MaterialTheme.colorScheme.surfaceVariant
                val bgColor = if (isSelected) RedPrimary.copy(alpha = 0.12f) else MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f)

                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clip(RoundedCornerShape(10.dp))
                        .background(bgColor)
                        .border(1.dp, borderColor, RoundedCornerShape(10.dp))
                        .clickable { onStreamSelected(stream) }
                        .padding(horizontal = 14.dp, vertical = 10.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        RadioButton(
                            selected = isSelected,
                            onClick = { onStreamSelected(stream) },
                            colors = RadioButtonDefaults.colors(selectedColor = RedPrimary)
                        )
                        Spacer(modifier = Modifier.width(8.dp))
                        Column {
                            Text(
                                text = stream.resolution,
                                style = MaterialTheme.typography.bodyLarge,
                                fontWeight = FontWeight.Bold
                            )
                            Text(
                                text = if (stream.isVideoOnly) "Video HD (DASH)" else "Video + Audio integrado",
                                style = MaterialTheme.typography.bodySmall,
                                color = if (stream.isVideoOnly) MaterialTheme.colorScheme.onSurfaceVariant else RedPrimary
                            )
                        }
                    }

                    Surface(
                        shape = RoundedCornerShape(6.dp),
                        color = if (stream.isVideoOnly) MaterialTheme.colorScheme.surfaceVariant else RedPrimary.copy(alpha = 0.15f)
                    ) {
                        Text(
                            text = if (stream.isVideoOnly) "${stream.format.uppercase()} (DASH)" else "${stream.format.uppercase()} ★",
                            modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp),
                            style = MaterialTheme.typography.labelSmall,
                            fontWeight = FontWeight.Bold,
                            color = if (stream.isVideoOnly) MaterialTheme.colorScheme.onSurfaceVariant else RedPrimary
                        )
                    }
                }
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AudioSelectionSection(
    audioStreams: List<AudioStreamItem>,
    selectedFormat: String,
    selectedStream: AudioStreamItem?,
    onFormatSelected: (String) -> Unit,
    onStreamSelected: (AudioStreamItem) -> Unit
) {
    val formats = remember(audioStreams) {
        audioStreams.map { it.format.lowercase() }.distinct()
    }

    // Filter Chips for Audio Formats (M4A, OPUS, WEBM)
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        Text(
            text = "Formato:",
            style = MaterialTheme.typography.bodyMedium,
            fontWeight = FontWeight.Medium,
            modifier = Modifier.align(Alignment.CenterVertically)
        )
        formats.forEach { format ->
            val isSelected = format.equals(selectedFormat, ignoreCase = true)
            FilterChip(
                selected = isSelected,
                onClick = { onFormatSelected(format) },
                label = { Text(format.uppercase()) },
                colors = FilterChipDefaults.filterChipColors(
                    selectedContainerColor = RedPrimary.copy(alpha = 0.2f),
                    selectedLabelColor = RedPrimary
                )
            )
        }
    }

    Spacer(modifier = Modifier.height(10.dp))

    val filteredStreams = remember(audioStreams, selectedFormat) {
        audioStreams
            .filter { it.format.equals(selectedFormat, ignoreCase = true) }
            .sortedByDescending { it.bitrate }
    }

    Text(
        text = "Calidad de Audio:",
        style = MaterialTheme.typography.bodyMedium,
        fontWeight = FontWeight.Medium
    )

    Spacer(modifier = Modifier.height(8.dp))

    if (filteredStreams.isEmpty()) {
        Text(
            text = "No hay streams disponibles para este formato.",
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            modifier = Modifier.padding(vertical = 12.dp)
        )
    } else {
        LazyColumn(
            modifier = Modifier
                .fillMaxWidth()
                .heightIn(max = 240.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            items(filteredStreams) { stream ->
                val isSelected = stream == selectedStream
                val borderColor = if (isSelected) RedPrimary else MaterialTheme.colorScheme.surfaceVariant
                val bgColor = if (isSelected) RedPrimary.copy(alpha = 0.12f) else MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f)

                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clip(RoundedCornerShape(10.dp))
                        .background(bgColor)
                        .border(1.dp, borderColor, RoundedCornerShape(10.dp))
                        .clickable { onStreamSelected(stream) }
                        .padding(horizontal = 14.dp, vertical = 10.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        RadioButton(
                            selected = isSelected,
                            onClick = { onStreamSelected(stream) },
                            colors = RadioButtonDefaults.colors(selectedColor = RedPrimary)
                        )
                        Spacer(modifier = Modifier.width(8.dp))
                        Column {
                            Text(
                                text = if (stream.bitrate > 0) "${stream.bitrate} kbps" else "Calidad Estándar",
                                style = MaterialTheme.typography.bodyLarge,
                                fontWeight = FontWeight.Bold
                            )
                            Text(
                                text = "Audio estéreo (${stream.quality.ifBlank { "Original" }})",
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                    }

                    Surface(
                        shape = RoundedCornerShape(6.dp),
                        color = RedPrimary.copy(alpha = 0.15f)
                    ) {
                        Text(
                            text = stream.format.uppercase(),
                            modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp),
                            style = MaterialTheme.typography.labelSmall,
                            fontWeight = FontWeight.Bold,
                            color = RedPrimary
                        )
                    }
                }
            }
        }
    }
}

private fun formatDuration(durationSeconds: Long): String {
    if (durationSeconds <= 0) return "0:00"
    val hours = durationSeconds / 3600
    val minutes = (durationSeconds % 3600) / 60
    val seconds = durationSeconds % 60
    return if (hours > 0) {
        String.format(Locale.getDefault(), "%d:%02d:%02d", hours, minutes, seconds)
    } else {
        String.format(Locale.getDefault(), "%d:%02d", minutes, seconds)
    }
}
