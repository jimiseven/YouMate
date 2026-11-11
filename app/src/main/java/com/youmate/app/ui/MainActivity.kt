package com.youmate.app.ui

import android.Manifest
import android.app.DownloadManager
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.content.pm.PackageManager
import android.os.Build
import android.os.Bundle
import android.util.Log
import android.widget.Toast
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
import com.youmate.app.ui.components.MainScreen
import com.youmate.app.ui.theme.YouMateTheme

class MainActivity : ComponentActivity() {

    private val viewModel: MainViewModel by viewModels()

    private val downloadReceiver = object : BroadcastReceiver() {
        override fun onReceive(context: Context?, intent: Intent?) {
            if (intent?.action == DownloadManager.ACTION_DOWNLOAD_COMPLETE) {
                val downloadId = intent.getLongExtra(DownloadManager.EXTRA_DOWNLOAD_ID, -1L)
                if (downloadId != -1L && context != null) {
                    val dm = context.getSystemService(Context.DOWNLOAD_SERVICE) as? DownloadManager ?: return
                    val query = DownloadManager.Query().setFilterById(downloadId)
                    runCatching {
                        dm.query(query)?.use { cursor ->
                            if (cursor.moveToFirst()) {
                                val statusIndex = cursor.getColumnIndex(DownloadManager.COLUMN_STATUS)
                                val reasonIndex = cursor.getColumnIndex(DownloadManager.COLUMN_REASON)
                                val titleIndex = cursor.getColumnIndex(DownloadManager.COLUMN_TITLE)

                                val status = if (statusIndex != -1) cursor.getInt(statusIndex) else -1
                                val reason = if (reasonIndex != -1) cursor.getInt(reasonIndex) else -1
                                val title = if (titleIndex != -1) cursor.getString(titleIndex) else "Archivo"

                                if (status == DownloadManager.STATUS_SUCCESSFUL) {
                                    val msg = "✅ ¡Descarga completada con éxito!\nGuardado en Descargas: $title"
                                    Log.i("YouMateDownload", msg)
                                    Toast.makeText(context, msg, Toast.LENGTH_LONG).show()
                                    viewModel.showDownloadStatus(msg, isError = false)
                                } else if (status == DownloadManager.STATUS_FAILED) {
                                    val reasonDesc = when (reason) {
                                        DownloadManager.ERROR_CANNOT_RESUME -> "No se puede reanudar la descarga (1008)"
                                        DownloadManager.ERROR_DEVICE_NOT_FOUND -> "Almacenamiento no encontrado (1007)"
                                        DownloadManager.ERROR_FILE_ALREADY_EXISTS -> "El archivo ya existe en Descargas (1009)"
                                        DownloadManager.ERROR_FILE_ERROR -> "Error de archivo en almacenamiento (1001)"
                                        DownloadManager.ERROR_HTTP_DATA_ERROR -> "Error de transmisión de datos HTTP (1004)"
                                        DownloadManager.ERROR_INSUFFICIENT_SPACE -> "Espacio en disco insuficiente (1006)"
                                        DownloadManager.ERROR_TOO_MANY_REDIRECTS -> "Demasiadas redirecciones (1005)"
                                        DownloadManager.ERROR_UNHANDLED_HTTP_CODE -> "Código HTTP no soportado (1002)"
                                        DownloadManager.ERROR_UNKNOWN -> "Error desconocido (1000)"
                                        else -> if (reason in 100..599) "Error HTTP $reason del servidor" else "Código de error: $reason"
                                    }
                                    val errMsg = "❌ Descarga fallida: $reasonDesc"
                                    Log.e("YouMateDownload", "$errMsg ($title)")
                                    Toast.makeText(context, errMsg, Toast.LENGTH_LONG).show()
                                    viewModel.showDownloadStatus(errMsg, isError = true)
                                }
                            }
                        }
                    }
                }
            }
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()

        // Register download complete receiver
        val filter = IntentFilter(DownloadManager.ACTION_DOWNLOAD_COMPLETE)
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            ContextCompat.registerReceiver(this, downloadReceiver, filter, ContextCompat.RECEIVER_EXPORTED)
        } else {
            @Suppress("UnspecifiedRegisterReceiverFlag")
            registerReceiver(downloadReceiver, filter)
        }

        // Handle initial intent (e.g. shared from YouTube)
        handleIncomingIntent(intent)

        setContent {
            YouMateTheme {
                val uiState by viewModel.uiState.collectAsStateWithLifecycle()
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

    override fun onDestroy() {
        super.onDestroy()
        runCatching { unregisterReceiver(downloadReceiver) }
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
