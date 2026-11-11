package com.youmate.app.ui

import android.Manifest
import android.content.Intent
import android.content.pm.PackageManager
import android.os.Build
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.activity.result.contract.ActivityResultContracts
import androidx.activity.viewModels
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Surface
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.core.content.ContextCompat
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.youmate.app.download.DownloadProgressTracker
import com.youmate.app.ui.components.MainScreen
import com.youmate.app.ui.theme.YouMateTheme

class MainActivity : ComponentActivity() {

    private val viewModel: MainViewModel by viewModels()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()

        // Handle initial intent (e.g. shared from YouTube)
        handleIncomingIntent(intent)

        setContent {
            YouMateTheme {
                val uiState by viewModel.uiState.collectAsStateWithLifecycle()
                val downloadState by DownloadProgressTracker.state.collectAsStateWithLifecycle()
                val snackbarHostState = remember { SnackbarHostState() }

                // Android 13+ (API 33) Notification Permission Request
                RequestNotificationPermission()

                // Display success snackbar
                LaunchedEffect(uiState.successMessage) {
                    uiState.successMessage?.let { msg ->
                        snackbarHostState.showSnackbar(msg)
                        viewModel.dismissMessages()
                    }
                }

                LaunchedEffect(downloadState.isSuccess) {
                    if (downloadState.isSuccess) {
                        snackbarHostState.showSnackbar("✅ ¡Descarga completada! Guardado en Descargas.")
                    }
                }

                LaunchedEffect(downloadState.errorMessage) {
                    downloadState.errorMessage?.let { err ->
                        snackbarHostState.showSnackbar("❌ Error al descargar: $err")
                    }
                }

                Surface(
                    modifier = Modifier.fillMaxSize(),
                    color = MaterialTheme.colorScheme.background
                ) {
                    MainScreen(
                        viewModel = viewModel,
                        uiState = uiState,
                        snackbarHostState = snackbarHostState
                    )
                }
            }
        }
    }

    override fun onNewIntent(intent: Intent) {
        super.onNewIntent(intent)
        setIntent(intent)
        handleIncomingIntent(intent)
    }

    /**
     * Intercepts ACTION_SEND (text/plain) from YouTube Share menu or ACTION_VIEW URLs.
     */
    private fun handleIncomingIntent(intent: Intent?) {
        if (intent == null) return

        when (intent.action) {
            Intent.ACTION_SEND -> {
                if (intent.type?.startsWith("text/") == true) {
                    val sharedText = intent.getStringExtra(Intent.EXTRA_TEXT)
                    if (!sharedText.isNullOrBlank()) {
                        viewModel.processUrl(sharedText)
                    }
                }
            }
            Intent.ACTION_VIEW -> {
                val dataUri = intent.dataString
                if (!dataUri.isNullOrBlank()) {
                    viewModel.processUrl(dataUri)
                }
            }
        }
    }
}

/**
 * Composable that requests POST_NOTIFICATIONS permission on Android 13+ (Tiramisu).
 */
@Composable
private fun RequestNotificationPermission() {
    val context = androidx.compose.ui.platform.LocalContext.current

    val permissionLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.RequestPermission()
    ) { _ ->
        // Permission granted or denied: DownloadManager still downloads either way,
        // but granted allows system to display download completion notification.
    }

    LaunchedEffect(Unit) {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            val isGranted = ContextCompat.checkSelfPermission(
                context,
                Manifest.permission.POST_NOTIFICATIONS
            ) == PackageManager.PERMISSION_GRANTED

            if (!isGranted) {
                permissionLauncher.launch(Manifest.permission.POST_NOTIFICATIONS)
            }
        }
    }
}
