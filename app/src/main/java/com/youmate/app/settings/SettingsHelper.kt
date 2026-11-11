package com.youmate.app.settings

import android.content.Context
import android.os.Environment

enum class StorageLocation(val key: String, val displayName: String, val dirType: String) {
    DOWNLOADS("DOWNLOADS", "Descargas (Downloads)", Environment.DIRECTORY_DOWNLOADS),
    MOVIES("MOVIES", "Películas y Videos (Movies)", Environment.DIRECTORY_MOVIES),
    MUSIC("MUSIC", "Música (Music)", Environment.DIRECTORY_MUSIC),
    DCIM("DCIM", "Cámara / Galería (DCIM)", Environment.DIRECTORY_DCIM);

    companion object {
        fun fromKey(key: String): StorageLocation {
            return entries.firstOrNull { it.key.equals(key, ignoreCase = true) } ?: DOWNLOADS
        }
    }
}

object SettingsHelper {

    private const val PREFS_NAME = "youmate_settings"
    private const val KEY_STORAGE_LOC = "storage_location"
    private const val KEY_SUBFOLDER = "subfolder"
    private const val KEY_SEPARATE_AUDIO = "separate_audio"

    fun getStorageLocation(context: Context): StorageLocation {
        val prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
        val key = prefs.getString(KEY_STORAGE_LOC, StorageLocation.DOWNLOADS.key) ?: StorageLocation.DOWNLOADS.key
        return StorageLocation.fromKey(key)
    }

    fun setStorageLocation(context: Context, location: StorageLocation) {
        context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE).edit()
            .putString(KEY_STORAGE_LOC, location.key)
            .apply()
    }

    fun getSubfolder(context: Context): String {
        val prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
        return prefs.getString(KEY_SUBFOLDER, "")?.trim().orEmpty()
    }

    fun setSubfolder(context: Context, subfolder: String) {
        context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE).edit()
            .putString(KEY_SUBFOLDER, subfolder.trim())
            .apply()
    }

    fun isSeparateAudioEnabled(context: Context): Boolean {
        val prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
        return prefs.getBoolean(KEY_SEPARATE_AUDIO, true)
    }

    fun setSeparateAudioEnabled(context: Context, enabled: Boolean) {
        context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE).edit()
            .putBoolean(KEY_SEPARATE_AUDIO, enabled)
            .apply()
    }

    /**
     * Resolves the target directory type for a given media download (Video vs Audio).
     */
    fun resolveDirectoryType(context: Context, isAudio: Boolean): String {
        if (isAudio && isSeparateAudioEnabled(context)) {
            return Environment.DIRECTORY_MUSIC
        }
        return getStorageLocation(context).dirType
    }

    /**
     * Resolves the safe relative path for DownloadManager.
     * If the subfolder exists or can be created, saves inside it.
     * Otherwise, safely defaults to the root of the directory to prevent ENOENT crashes.
     */
    fun resolveSubPath(context: Context, targetDirType: String, fileName: String): String {
        val subfolder = getSubfolder(context)
        if (subfolder.isBlank()) {
            return fileName
        }
        return runCatching {
            val publicDir = Environment.getExternalStoragePublicDirectory(targetDirType)
            val subDir = java.io.File(publicDir, subfolder)
            val exists = subDir.exists() || subDir.mkdirs()
            if (exists && subDir.exists()) {
                "$subfolder/$fileName"
            } else {
                fileName
            }
        }.getOrDefault(fileName)
    }

    /**
     * Returns a human-readable display path for preview in UI.
     */
    fun getPreviewDisplayPath(context: Context, isAudio: Boolean = false): String {
        return "Almacenamiento > Descargas (Download)/"
    }
}
