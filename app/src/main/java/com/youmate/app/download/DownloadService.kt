package com.youmate.app.download

import android.app.NotificationManager
import android.app.PendingIntent
import android.app.Service
import android.content.ContentValues
import android.content.Context
import android.content.Intent
import android.media.MediaScannerConnection
import android.net.Uri
import android.os.Build
import android.os.Environment
import android.os.IBinder
import android.provider.MediaStore
import android.util.Log
import androidx.core.app.NotificationCompat
import com.youmate.app.YouMateApp
import com.youmate.app.extractor.ExtractorHelper
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.cancel
import kotlinx.coroutines.launch
import okhttp3.OkHttpClient
import okhttp3.Request
import java.io.File
import java.io.FileOutputStream
import java.io.IOException
import java.io.OutputStream
import java.util.concurrent.TimeUnit

class DownloadService : Service() {

    private val serviceScope = CoroutineScope(Dispatchers.IO + SupervisorJob())
    private lateinit var notificationManager: NotificationManager

    private val httpClient = OkHttpClient.Builder()
        .connectTimeout(30, TimeUnit.SECONDS)
        .readTimeout(60, TimeUnit.SECONDS)
        .followRedirects(true)
        .build()

    override fun onCreate() {
        super.onCreate()
        notificationManager = getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
    }

    override fun onBind(intent: Intent?): IBinder? = null

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        if (intent == null) return START_NOT_STICKY

        val streamUrl = intent.getStringExtra(EXTRA_URL).orEmpty()
        val title = intent.getStringExtra(EXTRA_TITLE).orEmpty().ifBlank { "YouTube_Media" }
        val format = intent.getStringExtra(EXTRA_FORMAT).orEmpty().ifBlank { "mp4" }
        val isAudio = intent.getBooleanExtra(EXTRA_IS_AUDIO, false)

        if (streamUrl.isBlank()) {
            stopSelf()
            return START_NOT_STICKY
        }

        val notificationId = (System.currentTimeMillis() % 100000).toInt() + 1000

        // Start Foreground Service with initial notification
        val initialNotification = buildProgressNotification(notificationId, title, 0, "Conectando al servidor...")
        startForeground(notificationId, initialNotification)

        DownloadProgressTracker.start(title)

        serviceScope.launch {
            try {
                executeDownload(notificationId, streamUrl, title, format, isAudio)
            } catch (e: Exception) {
                Log.e("DownloadService", "Error durante la descarga: ${e.localizedMessage}", e)
                showErrorNotification(notificationId, title, e.localizedMessage ?: "Error de descarga")
                DownloadProgressTracker.error(e.localizedMessage ?: "Error de descarga")
            } finally {
                stopForeground(STOP_FOREGROUND_DETACH)
                stopSelf()
            }
        }

        return START_NOT_STICKY
    }

    private fun executeDownload(
        notificationId: Int,
        streamUrl: String,
        title: String,
        format: String,
        isAudio: Boolean
    ) {
        val safeTitle = DownloadHelper.sanitizeFileName(title)
        val safeExtension = format.lowercase().removePrefix(".")
        val fileName = "$safeTitle.$safeExtension"
        val mimeType = DownloadHelper.getMimeType(safeExtension)

        Log.i("DownloadService", "Iniciando descarga: $fileName desde $streamUrl")

        // Browser User-Agent matching NewPipeExtractor to bypass YouTube CDN 403 blocks
        val request = Request.Builder()
            .url(streamUrl)
            .header(
                "User-Agent",
                "Mozilla/5.0 (Windows NT 10.0; Win64; x64) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/120.0.0.0 Safari/537.36"
            )
            .build()

        val response = httpClient.newCall(request).execute()
        if (!response.isSuccessful) {
            throw IOException("El servidor respondió con código HTTP ${response.code}")
        }

        val body = response.body ?: throw IOException("Respuesta del servidor vacía")
        val totalBytes = body.contentLength()
        val inputStream = body.byteStream()

        var outputStream: OutputStream? = null
        var targetUri: Uri? = null
        var localFile: File? = null

        try {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
                // Android 10+ (API 29+): Scoped Storage via MediaStore.Downloads
                val values = ContentValues().apply {
                    put(MediaStore.MediaColumns.DISPLAY_NAME, fileName)
                    put(MediaStore.MediaColumns.MIME_TYPE, mimeType)
                    put(MediaStore.MediaColumns.RELATIVE_PATH, Environment.DIRECTORY_DOWNLOADS)
                    put(MediaStore.MediaColumns.IS_PENDING, 1)
                }
                targetUri = contentResolver.insert(MediaStore.Downloads.EXTERNAL_CONTENT_URI, values)
                    ?: throw IOException("No se pudo registrar el archivo en Descargas")
                outputStream = contentResolver.openOutputStream(targetUri)
                    ?: throw IOException("No se pudo abrir el flujo de escritura")
            } else {
                // Android 9 and lower: Direct file write in DIRECTORY_DOWNLOADS
                val dir = Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_DOWNLOADS)
                if (!dir.exists()) dir.mkdirs()
                localFile = File(dir, fileName)
                targetUri = Uri.fromFile(localFile)
                outputStream = FileOutputStream(localFile)
            }

            val buffer = ByteArray(64 * 1024)
            var bytesRead: Int
            var totalRead = 0L
            var lastUpdateMillis = System.currentTimeMillis()

            outputStream.use { out ->
                inputStream.use { input ->
                    while (input.read(buffer).also { bytesRead = it } != -1) {
                        out.write(buffer, 0, bytesRead)
                        totalRead += bytesRead

                        val now = System.currentTimeMillis()
                        if (now - lastUpdateMillis > 500) {
                            lastUpdateMillis = now
                            val percent = if (totalBytes > 0L) ((totalRead * 100) / totalBytes).toInt() else -1
                            val downloadedStr = ExtractorHelper.formatFileSize(totalRead)
                            val totalStr = if (totalBytes > 0L) ExtractorHelper.formatFileSize(totalBytes) else "..."
                            val infoText = "$downloadedStr / $totalStr (${if (percent >= 0) "$percent%" else ""})"

                            val notification = buildProgressNotification(notificationId, title, percent, infoText)
                            notificationManager.notify(notificationId, notification)

                            DownloadProgressTracker.updateProgress(title, percent, downloadedStr, totalStr)
                        }
                    }
                    out.flush()
                }
            }

            // Mark download as complete in MediaStore (Android 10+)
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q && targetUri != null) {
                val completeValues = ContentValues().apply {
                    put(MediaStore.MediaColumns.IS_PENDING, 0)
                }
                contentResolver.update(targetUri, completeValues, null, null)
            } else if (localFile != null) {
                // Scan file so it appears immediately in media apps
                MediaScannerConnection.scanFile(
                    applicationContext,
                    arrayOf(localFile.absolutePath),
                    arrayOf(mimeType),
                    null
                )
            }

            Log.i("DownloadService", "Descarga completada con éxito: $fileName ($totalRead bytes)")
            showSuccessNotification(notificationId, title, fileName, targetUri, mimeType)
            DownloadProgressTracker.complete(title)

        } catch (e: Exception) {
            // Clean up incomplete entry if needed
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q && targetUri != null) {
                runCatching { contentResolver.delete(targetUri, null, null) }
            } else if (localFile?.exists() == true) {
                runCatching { localFile.delete() }
            }
            throw e
        }
    }

    private fun buildProgressNotification(
        notificationId: Int,
        title: String,
        progress: Int,
        subText: String
    ) = NotificationCompat.Builder(this, YouMateApp.DOWNLOAD_CHANNEL_ID)
        .setSmallIcon(android.R.drawable.stat_sys_download)
        .setContentTitle(title)
        .setContentText(subText)
        .setProgress(100, progress.coerceAtLeast(0), progress < 0)
        .setOngoing(true)
        .setOnlyAlertOnce(true)
        .build()

    private fun showSuccessNotification(
        notificationId: Int,
        title: String,
        fileName: String,
        uri: Uri?,
        mimeType: String
    ) {
        val clickIntent = if (uri != null) {
            Intent(Intent.ACTION_VIEW).apply {
                setDataAndType(uri, mimeType)
                flags = Intent.FLAG_GRANT_READ_URI_PERMISSION or Intent.FLAG_ACTIVITY_NEW_TASK
            }
        } else {
            Intent(Intent.ACTION_GET_CONTENT).apply {
                type = mimeType
                flags = Intent.FLAG_ACTIVITY_NEW_TASK
            }
        }

        val pendingIntent = PendingIntent.getActivity(
            this,
            notificationId,
            clickIntent,
            PendingIntent.FLAG_IMMUTABLE or PendingIntent.FLAG_UPDATE_CURRENT
        )

        val notification = NotificationCompat.Builder(this, YouMateApp.DOWNLOAD_CHANNEL_ID)
            .setSmallIcon(android.R.drawable.stat_sys_download_done)
            .setContentTitle("✅ ¡Descarga completada!")
            .setContentText("$fileName guardado en Descargas")
            .setContentIntent(pendingIntent)
            .setAutoCancel(true)
            .setOngoing(false)
            .build()

        notificationManager.notify(notificationId, notification)
    }

    private fun showErrorNotification(notificationId: Int, title: String, errorMsg: String) {
        val notification = NotificationCompat.Builder(this, YouMateApp.DOWNLOAD_CHANNEL_ID)
            .setSmallIcon(android.R.drawable.stat_notify_error)
            .setContentTitle("❌ Error al descargar: $title")
            .setContentText(errorMsg)
            .setAutoCancel(true)
            .setOngoing(false)
            .build()

        notificationManager.notify(notificationId, notification)
    }

    override fun onDestroy() {
        super.onDestroy()
        serviceScope.cancel()
    }

    companion object {
        const val EXTRA_URL = "extra_stream_url"
        const val EXTRA_TITLE = "extra_media_title"
        const val EXTRA_FORMAT = "extra_media_format"
        const val EXTRA_IS_AUDIO = "extra_is_audio"

        fun start(context: Context, url: String, title: String, format: String, isAudio: Boolean) {
            val intent = Intent(context, DownloadService::class.java).apply {
                putExtra(EXTRA_URL, url)
                putExtra(EXTRA_TITLE, title)
                putExtra(EXTRA_FORMAT, format)
                putExtra(EXTRA_IS_AUDIO, isAudio)
            }
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                context.startForegroundService(intent)
            } else {
                context.startService(intent)
            }
        }
    }
}
