package com.youmate.app

import com.youmate.app.extractor.ExtractorHelper
import kotlinx.coroutines.runBlocking
import okhttp3.OkHttpClient
import okhttp3.Request
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test
import java.util.concurrent.TimeUnit

class ExtractorTest {

    @Test
    fun testExtraction() = runBlocking {
        ExtractorHelper.initialize()
        val testUrl = "https://www.youtube.com/watch?v=dQw4w9WgXcQ"
        val result = ExtractorHelper.extractMedia(testUrl)

        assertTrue("Media extraction should succeed: ${result.exceptionOrNull()?.message}", result.isSuccess)
        val data = result.getOrThrow()
        assertTrue("Title should not be blank", data.title.isNotBlank())
        assertFalse("Video streams should not be empty", data.videoStreams.isEmpty())
        assertFalse("Audio streams should not be empty", data.audioStreams.isEmpty())

        println("Verified extraction for: ${data.title}")
        println("Video streams count: ${data.videoStreams.size}")
        println("Audio streams count: ${data.audioStreams.size}")
    }

    @Test
    fun testChunkedStreamDownload() = runBlocking {
        ExtractorHelper.initialize()
        val testUrl = "https://www.youtube.com/watch?v=kJQP7kiw5Fk" // Despacito
        val result = ExtractorHelper.extractMedia(testUrl)
        assertTrue("Extraction should succeed", result.isSuccess)
        val data = result.getOrThrow()

        val client = OkHttpClient.Builder()
            .connectTimeout(15, TimeUnit.SECONDS)
            .readTimeout(15, TimeUnit.SECONDS)
            .build()

        val p1080 = data.videoStreams.firstOrNull { it.resolution.contains("1080") && it.format.equals("mp4", ignoreCase = true) }
        val m4aAudio = data.audioStreams.firstOrNull { it.format.equals("m4a", ignoreCase = true) }

        listOfNotNull(p1080?.url, m4aAudio?.url).forEach { streamUrl ->
            val req = Request.Builder()
                .url(streamUrl)
                .header("Range", "bytes=0-1048575")
                .build()
            val resp = client.newCall(req).execute()
            println("Chunked range test for ${streamUrl.take(60)}... -> HTTP ${resp.code}")
            assertTrue("Expected HTTP 206 for bounded range: ${resp.code}", resp.code == 206)
            resp.close()
        }
    }
}
