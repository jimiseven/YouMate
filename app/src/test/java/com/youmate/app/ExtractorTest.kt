package com.youmate.app

import com.youmate.app.extractor.ExtractorHelper
import kotlinx.coroutines.runBlocking
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class ExtractorTest {

    @Test
    fun testExtraction() = runBlocking {
        ExtractorHelper.initialize()
        val testUrl = "https://www.youtube.com/watch?v=dQw4w9WgXcQ"
        val result = ExtractorHelper.extractMedia(testUrl)

        assertTrue("Media extraction should succeed", result.isSuccess)
        val data = result.getOrThrow()
        assertTrue("Title should not be blank", data.title.isNotBlank())
        assertFalse("Video streams should not be empty", data.videoStreams.isEmpty())
        assertFalse("Audio streams should not be empty", data.audioStreams.isEmpty())

        println("Verified extraction for: ${data.title}")
        println("Video streams count: ${data.videoStreams.size}")
        println("Audio streams count: ${data.audioStreams.size}")
    }
}
