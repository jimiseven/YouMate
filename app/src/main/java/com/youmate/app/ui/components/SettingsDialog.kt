package com.youmate.app.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Folder
import androidx.compose.material.icons.filled.MusicNote
import androidx.compose.material.icons.filled.Videocam
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import com.youmate.app.settings.SettingsHelper
import com.youmate.app.settings.StorageLocation
import com.youmate.app.ui.MainUiState
import com.youmate.app.ui.MainViewModel
import com.youmate.app.ui.theme.RedPrimary

@Composable
fun SettingsDialog(
    viewModel: MainViewModel,
    uiState: MainUiState,
    onDismissRequest: () -> Unit
) {
    val context = LocalContext.current
    var selectedLocation by remember(uiState.storageLocation) { mutableStateOf(uiState.storageLocation) }
    var subfolderText by remember(uiState.subfolder) { mutableStateOf(uiState.subfolder) }
    var separateAudio by remember(uiState.separateAudio) { mutableStateOf(uiState.separateAudio) }

    Dialog(onDismissRequest = onDismissRequest) {
        Card(
            modifier = Modifier
                .fillMaxWidth()
                .wrapContentHeight(),
            shape = RoundedCornerShape(20.dp),
            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)
        ) {
            Column(
                modifier = Modifier
                    .padding(24.dp)
                    .verticalScroll(rememberScrollState())
            ) {
                // Header
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    modifier = Modifier.padding(bottom = 16.dp)
                ) {
                    Icon(
                        imageVector = Icons.Default.Folder,
                        contentDescription = null,
                        tint = RedPrimary,
                        modifier = Modifier.size(28.dp)
                    )
                    Spacer(modifier = Modifier.width(10.dp))
                    Text(
                        text = "Ubicación de Descargas",
                        style = MaterialTheme.typography.titleLarge,
                        fontWeight = FontWeight.Bold
                    )
                }

                Text(
                    text = "Carpeta principal de videos:",
                    style = MaterialTheme.typography.bodyMedium,
                    fontWeight = FontWeight.SemiBold,
                    color = MaterialTheme.colorScheme.onSurface
                )

                Spacer(modifier = Modifier.height(8.dp))

                // Location Options
                StorageLocation.entries.forEach { loc ->
                    val isSelected = loc == selectedLocation
                    val borderColor = if (isSelected) RedPrimary else MaterialTheme.colorScheme.surfaceVariant
                    val bgColor = if (isSelected) RedPrimary.copy(alpha = 0.1f) else MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.4f)

                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(vertical = 4.dp)
                            .clip(RoundedCornerShape(10.dp))
                            .background(bgColor)
                            .border(1.dp, borderColor, RoundedCornerShape(10.dp))
                            .clickable { selectedLocation = loc }
                            .padding(horizontal = 12.dp, vertical = 8.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        RadioButton(
                            selected = isSelected,
                            onClick = { selectedLocation = loc },
                            colors = RadioButtonDefaults.colors(selectedColor = RedPrimary)
                        )
                        Spacer(modifier = Modifier.width(8.dp))
                        Text(
                            text = loc.displayName,
                            style = MaterialTheme.typography.bodyMedium,
                            fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Normal
                        )
                    }
                }

                Spacer(modifier = Modifier.height(16.dp))

                // Subfolder TextField
                Text(
                    text = "Subcarpeta (Opcional):",
                    style = MaterialTheme.typography.bodyMedium,
                    fontWeight = FontWeight.SemiBold
                )
                Spacer(modifier = Modifier.height(4.dp))
                OutlinedTextField(
                    value = subfolderText,
                    onValueChange = { subfolderText = it },
                    placeholder = { Text("Opcional (ej. YouMate)") },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(10.dp)
                )
                Text(
                    text = "Deja en blanco para guardar directo en la carpeta seleccionada.",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier.padding(top = 2.dp)
                )

                Spacer(modifier = Modifier.height(16.dp))

                // Separate Audio Switch
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clip(RoundedCornerShape(10.dp))
                        .background(MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.4f))
                        .padding(horizontal = 12.dp, vertical = 8.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Column(modifier = Modifier.weight(1f)) {
                        Text(
                            text = "Guardar audios en Música",
                            style = MaterialTheme.typography.bodyMedium,
                            fontWeight = FontWeight.SemiBold
                        )
                        Text(
                            text = "Envía los archivos de audio a la carpeta pública 'Music'.",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                    Switch(
                        checked = separateAudio,
                        onCheckedChange = { separateAudio = it },
                        colors = SwitchDefaults.colors(
                            checkedThumbColor = RedPrimary,
                            checkedTrackColor = RedPrimary.copy(alpha = 0.3f)
                        )
                    )
                }

                Spacer(modifier = Modifier.height(16.dp))

                // Live Preview Card
                Surface(
                    shape = RoundedCornerShape(10.dp),
                    color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.6f),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Column(modifier = Modifier.padding(12.dp)) {
                        Text(
                            text = "Rutas de guardado:",
                            style = MaterialTheme.typography.labelMedium,
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                        Spacer(modifier = Modifier.height(4.dp))
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Icon(Icons.Default.Videocam, contentDescription = null, modifier = Modifier.size(16.dp), tint = RedPrimary)
                            Spacer(modifier = Modifier.width(6.dp))
                            val dirName = when (selectedLocation) {
                                StorageLocation.DOWNLOADS -> "Download"
                                StorageLocation.MOVIES -> "Movies"
                                StorageLocation.MUSIC -> "Music"
                                StorageLocation.DCIM -> "DCIM"
                            }
                            val sub = subfolderText.trim()
                            val videoPath = if (sub.isNotBlank()) "Almacenamiento > $dirName > $sub/" else "Almacenamiento > $dirName/"
                            Text(text = videoPath, style = MaterialTheme.typography.bodySmall, fontSize = 12.sp)
                        }
                        if (separateAudio) {
                            Spacer(modifier = Modifier.height(2.dp))
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Icon(Icons.Default.MusicNote, contentDescription = null, modifier = Modifier.size(16.dp), tint = RedPrimary)
                                Spacer(modifier = Modifier.width(6.dp))
                                val sub = subfolderText.trim()
                                val audioPath = if (sub.isNotBlank()) "Almacenamiento > Music > $sub/" else "Almacenamiento > Music/"
                                Text(text = audioPath, style = MaterialTheme.typography.bodySmall, fontSize = 12.sp)
                            }
                        }
                    }
                }

                Spacer(modifier = Modifier.height(20.dp))

                // Action Buttons
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.End
                ) {
                    TextButton(onClick = onDismissRequest) {
                        Text("Cancelar", color = MaterialTheme.colorScheme.onSurfaceVariant)
                    }
                    Spacer(modifier = Modifier.width(8.dp))
                    Button(
                        onClick = {
                            viewModel.updateStorageLocation(context, selectedLocation)
                            viewModel.updateSubfolder(context, subfolderText)
                            viewModel.updateSeparateAudio(context, separateAudio)
                            onDismissRequest()
                        },
                        colors = ButtonDefaults.buttonColors(
                            containerColor = RedPrimary,
                            contentColor = Color.White
                        ),
                        shape = RoundedCornerShape(10.dp)
                    ) {
                        Text("Guardar")
                    }
                }
            }
        }
    }
}
