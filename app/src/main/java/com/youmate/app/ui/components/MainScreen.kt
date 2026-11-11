package com.youmate.app.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Clear
import androidx.compose.material.icons.filled.ContentPaste
import androidx.compose.material.icons.filled.Download
import androidx.compose.material.icons.filled.Folder
import androidx.compose.material.icons.filled.Link
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material.icons.filled.Share
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.youmate.app.settings.SettingsHelper
import com.youmate.app.ui.MainUiState
import com.youmate.app.ui.MainViewModel
import com.youmate.app.ui.theme.DarkSurfaceVariant
import com.youmate.app.ui.theme.RedPrimary

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun MainScreen(
    viewModel: MainViewModel,
    uiState: MainUiState,
    snackbarHostState: SnackbarHostState
) {
    val context = LocalContext.current
    val scrollState = rememberScrollState()

    Scaffold(
        snackbarHost = { SnackbarHost(snackbarHostState) },
        topBar = {
            TopAppBar(
                title = {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Surface(
                            shape = RoundedCornerShape(8.dp),
                            color = RedPrimary,
                            modifier = Modifier.size(36.dp)
                        ) {
                            Box(contentAlignment = Alignment.Center) {
                                Icon(
                                    Icons.Default.Download,
                                    contentDescription = null,
                                    tint = Color.White,
                                    modifier = Modifier.size(20.dp)
                                )
                            }
                        }
                        Spacer(modifier = Modifier.width(12.dp))
                        Text(
                            text = "YouMate",
                            style = MaterialTheme.typography.titleLarge,
                            fontWeight = FontWeight.ExtraBold
                        )
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = MaterialTheme.colorScheme.background,
                    titleContentColor = MaterialTheme.colorScheme.onBackground
                ),
                actions = {
                    IconButton(onClick = { viewModel.openSettings(context) }) {
                        Icon(
                            imageVector = Icons.Default.Settings,
                            contentDescription = "Ajustes de almacenamiento",
                            tint = MaterialTheme.colorScheme.onBackground
                        )
                    }
                }
            )
        }
    ) { paddingValues ->
        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues)
        ) {
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .verticalScroll(scrollState)
                    .padding(20.dp),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                Spacer(modifier = Modifier.height(12.dp))

                // Hero Header
                Text(
                    text = "Descargas Rápidas de YouTube",
                    style = MaterialTheme.typography.titleLarge,
                    fontWeight = FontWeight.Bold,
                    textAlign = TextAlign.Center
                )
                Spacer(modifier = Modifier.height(6.dp))
                Text(
                    text = "Sin límites, sin anuncios, 100% nativo y ligero.",
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    textAlign = TextAlign.Center
                )

                Spacer(modifier = Modifier.height(28.dp))

                // URL Input Field
                OutlinedTextField(
                    value = uiState.urlInput,
                    onValueChange = { viewModel.onUrlChange(it) },
                    modifier = Modifier.fillMaxWidth(),
                    label = { Text("Enlace del video") },
                    placeholder = { Text("https://www.youtube.com/watch?v=...") },
                    leadingIcon = {
                        Icon(Icons.Default.Link, contentDescription = null, tint = RedPrimary)
                    },
                    trailingIcon = {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            if (uiState.urlInput.isNotBlank()) {
                                IconButton(onClick = { viewModel.onUrlChange("") }) {
                                    Icon(Icons.Default.Clear, contentDescription = "Limpiar")
                                }
                            }
                            IconButton(onClick = { viewModel.pasteFromClipboard(context) }) {
                                Icon(Icons.Default.ContentPaste, contentDescription = "Pegar", tint = RedPrimary)
                            }
                        }
                    },
                    singleLine = true,
                    shape = RoundedCornerShape(14.dp),
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedBorderColor = RedPrimary,
                        unfocusedBorderColor = MaterialTheme.colorScheme.surfaceVariant,
                        focusedLabelColor = RedPrimary
                    ),
                    enabled = !uiState.isLoading
                )

                Spacer(modifier = Modifier.height(16.dp))

                // Extract Button
                Button(
                    onClick = { viewModel.processUrl(uiState.urlInput) },
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(52.dp),
                    shape = RoundedCornerShape(14.dp),
                    colors = ButtonDefaults.buttonColors(
                        containerColor = RedPrimary,
                        contentColor = Color.White
                    ),
                    enabled = !uiState.isLoading && uiState.urlInput.isNotBlank()
                ) {
                    if (uiState.isLoading) {
                        CircularProgressIndicator(
                            modifier = Modifier.size(24.dp),
                            color = Color.White,
                            strokeWidth = 2.5.dp
                        )
                        Spacer(modifier = Modifier.width(12.dp))
                        Text("Extrayendo medios...", style = MaterialTheme.typography.labelLarge)
                    } else {
                        Icon(Icons.Default.Download, contentDescription = null)
                        Spacer(modifier = Modifier.width(8.dp))
                        Text("Obtener Medios", style = MaterialTheme.typography.labelLarge, fontSize = 16.sp)
                    }
                }

                // Error Message Card
                if (uiState.errorMessage != null) {
                    Spacer(modifier = Modifier.height(18.dp))
                    Surface(
                        modifier = Modifier.fillMaxWidth(),
                        shape = RoundedCornerShape(12.dp),
                        color = MaterialTheme.colorScheme.errorContainer.copy(alpha = 0.8f)
                    ) {
                        Text(
                            text = uiState.errorMessage,
                            color = MaterialTheme.colorScheme.onErrorContainer,
                            style = MaterialTheme.typography.bodyMedium,
                            modifier = Modifier.padding(14.dp)
                        )
                    }
                }

                Spacer(modifier = Modifier.height(20.dp))

                // Storage Destination Quick-Access Card
                Surface(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clip(RoundedCornerShape(14.dp))
                        .clickable { viewModel.openSettings(context) },
                    color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f)
                ) {
                    Row(
                        modifier = Modifier.padding(horizontal = 14.dp, vertical = 12.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Surface(
                            shape = RoundedCornerShape(8.dp),
                            color = RedPrimary.copy(alpha = 0.15f),
                            modifier = Modifier.size(36.dp)
                        ) {
                            Box(contentAlignment = Alignment.Center) {
                                Icon(
                                    Icons.Default.Folder,
                                    contentDescription = null,
                                    tint = RedPrimary,
                                    modifier = Modifier.size(20.dp)
                                )
                            }
                        }
                        Spacer(modifier = Modifier.width(12.dp))
                        Column(modifier = Modifier.weight(1f)) {
                            Text(
                                text = "Guardar en:",
                                style = MaterialTheme.typography.labelSmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                            Text(
                                text = SettingsHelper.getPreviewDisplayPath(context),
                                style = MaterialTheme.typography.bodySmall,
                                fontWeight = FontWeight.SemiBold
                            )
                        }
                        Icon(
                            Icons.Default.Settings,
                            contentDescription = "Configurar",
                            tint = MaterialTheme.colorScheme.onSurfaceVariant,
                            modifier = Modifier.size(20.dp)
                        )
                    }
                }

                Spacer(modifier = Modifier.height(24.dp))

                // Info Banner for YouTube Share Feature
                Surface(
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(16.dp),
                    color = DarkSurfaceVariant.copy(alpha = 0.6f)
                ) {
                    Column(
                        modifier = Modifier.padding(18.dp)
                    ) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Icon(
                                Icons.Default.Share,
                                contentDescription = null,
                                tint = RedPrimary,
                                modifier = Modifier.size(24.dp)
                            )
                            Spacer(modifier = Modifier.width(10.dp))
                            Text(
                                text = "¿Sabías que no necesitas abrir la app?",
                                style = MaterialTheme.typography.titleMedium,
                                fontWeight = FontWeight.Bold
                            )
                        }
                        Spacer(modifier = Modifier.height(10.dp))
                        Text(
                            text = "1. Abre la app oficial de YouTube.\n" +
                                   "2. Presiona 'Compartir' en cualquier video o Short.\n" +
                                   "3. Selecciona 'YouMate' en la lista de aplicaciones.\n" +
                                   "4. Elige formato y calidad, ¡y listo!",
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                            lineHeight = 22.sp
                        )
                    }
                }
            }

            // Settings Dialog
            if (uiState.showSettingsDialog) {
                SettingsDialog(
                    viewModel = viewModel,
                    uiState = uiState,
                    onDismissRequest = { viewModel.dismissSettings() }
                )
            }

            // BottomSheet for Stream & Format Selection
            if (uiState.showBottomSheet && uiState.extractionResult != null) {
                StreamSelectionBottomSheet(
                    uiState = uiState,
                    onDismissRequest = { viewModel.dismissBottomSheet() },
                    onSelectTab = { viewModel.selectTab(it) },
                    onSelectVideoFormat = { viewModel.selectVideoFormat(it) },
                    onSelectAudioFormat = { viewModel.selectAudioFormat(it) },
                    onSelectVideoStream = { viewModel.selectVideoStream(it) },
                    onSelectAudioStream = { viewModel.selectAudioStream(it) },
                    onConfirmDownload = { viewModel.startDownload(context) }
                )
            }
        }
    }
}
