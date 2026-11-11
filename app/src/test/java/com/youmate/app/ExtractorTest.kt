package com.youmate.app

import com.youmate.app.extractor.ExtractorHelper
import kotlinx.coroutines.runBlocking
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class ExtractorTest {

    @Test
    fun testExtraction() {
        runBlocking {
            ExtractorHelper.initialize()
            val testUrl = "https://www.youtube.com/watch?v=dQw4w9WgXcQ"
            val result = ExtractorHelper.extractMedia(testUrl)

            assertTrue("Extraction should be successful", result.isSuccess)
            val data = result.getOrThrow()

            println("Video title: ${data.title}")
            println("Total video streams extracted: ${data.videoStreams.size}")
            data.videoStreams.forEach {
                println("  Video: ${it.resolution} [${it.format}] isVideoOnly=${it.isVideoOnly}")
            }
            println("Total audio streams extracted: ${data.audioStreams.size}")
            data.audioStreams.forEach {
                println("  Audio: ${it.bitrate} kbps [${it.format}]")
            }

            assertTrue("Should contain 1080p", data.videoStreams.any { it.resolution.contains("1080") })
            assertTrue("Should contain 720p", data.videoStreams.any { it.resolution.contains("720") })
            assertTrue("Should contain 480p", data.videoStreams.any { it.resolution.contains("480") })
            assertTrue("Should contain 360p", data.videoStreams.any { it.resolution.contains("360") })
            assertFalse("Audio streams should not be empty", data.audioStreams.isEmpty())
        }
    }
}
