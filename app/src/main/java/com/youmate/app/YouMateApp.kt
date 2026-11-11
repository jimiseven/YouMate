package com.youmate.app

import android.app.Application
import com.youmate.app.extractor.ExtractorHelper

class YouMateApp : Application() {
    override fun onCreate() {
        super.onCreate()
        // Initialize NewPipeExtractor engine with OkHttp downloader
        ExtractorHelper.initialize()
    }
}
