package com.youmate.app

import android.app.Application
import android.app.NotificationChannel
import android.app.NotificationManager
import android.os.Build
import com.youmate.app.extractor.ExtractorHelper

class YouMateApp : Application() {

    companion object {
        const val DOWNLOAD_CHANNEL_ID = "youmate_downloads_channel"
        const val DOWNLOAD_CHANNEL_NAME = "Descargas YouMate"
    }

    override fun onCreate() {
        super.onCreate()
        // Initialize NewPipeExtractor engine with OkHttp downloader
        ExtractorHelper.initialize()
        createNotificationChannel()
    }

    private fun createNotificationChannel() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val channel = NotificationChannel(
                DOWNLOAD_CHANNEL_ID,
                DOWNLOAD_CHANNEL_NAME,
                NotificationManager.IMPORTANCE_LOW
            ).apply {
                description = "Progreso y estado de descargas de YouMate"
                setShowBadge(false)
            }
            val manager = getSystemService(NotificationManager::class.java)
            manager?.createNotificationChannel(channel)
        }
    }
}
