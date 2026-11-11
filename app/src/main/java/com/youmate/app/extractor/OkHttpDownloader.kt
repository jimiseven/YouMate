package com.youmate.app.extractor

import okhttp3.MediaType.Companion.toMediaTypeOrNull
import okhttp3.OkHttpClient
import okhttp3.RequestBody.Companion.toRequestBody
import org.schabi.newpipe.extractor.downloader.Downloader
import org.schabi.newpipe.extractor.downloader.Request
import org.schabi.newpipe.extractor.downloader.Response
import org.schabi.newpipe.extractor.exceptions.ReCaptchaException
import java.io.IOException
import java.util.concurrent.TimeUnit

class OkHttpDownloader(
    private val client: OkHttpClient = OkHttpClient.Builder()
        .connectTimeout(30, TimeUnit.SECONDS)
        .readTimeout(30, TimeUnit.SECONDS)
        .followRedirects(true)
        .build()
) : Downloader() {

    @Throws(IOException::class, ReCaptchaException::class)
    override fun execute(request: Request): Response {
        val httpMethod = request.httpMethod()
        val url = request.url()
        val headers = request.headers()
        val dataToSend = request.dataToSend()

        val requestBody = if (dataToSend != null) {
            val contentType = headers["Content-Type"]?.firstOrNull() ?: "application/octet-stream"
            dataToSend.toRequestBody(contentType.toMediaTypeOrNull())
        } else if (httpMethod.equals("POST", ignoreCase = true)) {
            ByteArray(0).toRequestBody(null)
        } else {
            null
        }

        val okRequestBuilder = okhttp3.Request.Builder()
            .url(url)
            .method(httpMethod, requestBody)

        var hasUserAgent = false
        headers.forEach { (key, values) ->
            if (key.equals("User-Agent", ignoreCase = true)) {
                hasUserAgent = true
            }
            values.forEach { value ->
                okRequestBuilder.addHeader(key, value)
            }
        }

        if (!hasUserAgent) {
            okRequestBuilder.addHeader(
                "User-Agent",
                "Mozilla/5.0 (Windows NT 10.0; Win64; x64; rv:140.0) Gecko/20100101 Firefox/140.0"
            )
        }

        client.newCall(okRequestBuilder.build()).execute().use { response ->
            val responseBody = response.body?.string() ?: ""
            val responseHeaders = mutableMapOf<String, List<String>>()
            for (headerName in response.headers.names()) {
                responseHeaders[headerName] = response.headers.values(headerName)
            }
            val responseCode = response.code
            val responseMessage = response.message
            val latestUrl = response.request.url.toString()

            return Response(responseCode, responseMessage, responseHeaders, responseBody, latestUrl)
        }
    }
}
