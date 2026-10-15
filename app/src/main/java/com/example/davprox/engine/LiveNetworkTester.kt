package com.example.davprox.engine

import com.example.davprox.data.TrafficRepository
import com.example.davprox.model.TrafficClassification
import com.example.davprox.model.TrafficRecord
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import okhttp3.MediaType.Companion.toMediaTypeOrNull
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.RequestBody.Companion.toRequestBody
import org.json.JSONObject
import java.net.InetSocketAddress
import java.net.Proxy
import java.net.URL
import java.util.UUID
import java.util.concurrent.TimeUnit

object LiveNetworkTester {

    private val directClient: OkHttpClient by lazy {
        OkHttpClient.Builder()
            .connectTimeout(15, TimeUnit.SECONDS)
            .readTimeout(20, TimeUnit.SECONDS)
            .writeTimeout(15, TimeUnit.SECONDS)
            .followRedirects(true)
            .build()
    }

    private fun createProxyClient(proxyPort: Int): OkHttpClient {
        val proxy = Proxy(Proxy.Type.HTTP, InetSocketAddress("127.0.0.1", proxyPort))
        return OkHttpClient.Builder()
            .proxy(proxy)
            .connectTimeout(15, TimeUnit.SECONDS)
            .readTimeout(20, TimeUnit.SECONDS)
            .writeTimeout(15, TimeUnit.SECONDS)
            .followRedirects(true)
            .build()
    }

    suspend fun executeLiveRequest(
        urlStr: String,
        method: String = "GET",
        headers: Map<String, String> = emptyMap(),
        bodyContent: String? = null,
        routeViaLocalProxy: Boolean = false,
        proxyPort: Int = 8080,
        isReplay: Boolean = false
    ): Result<TrafficRecord> = withContext(Dispatchers.IO) {
        val startTime = System.currentTimeMillis()
        val recordId = UUID.randomUUID().toString()

        val parsedUrl = try {
            URL(urlStr)
        } catch (e: Exception) {
            return@withContext Result.failure(Exception("Invalid URL format: ${e.message}"))
        }

        val client = if (routeViaLocalProxy && ProxyServerEngine.isRunning.value) {
            createProxyClient(proxyPort)
        } else {
            directClient
        }

        val requestBuilder = Request.Builder().url(urlStr)

        headers.forEach { (k, v) ->
            requestBuilder.addHeader(k, v)
        }
        if (!headers.containsKey("User-Agent")) {
            requestBuilder.addHeader("User-Agent", "DavProx/1.0 (Android; Network Inspector)")
        }

        val mediaType = (headers["Content-Type"] ?: "application/json; charset=utf-8").toMediaTypeOrNull()
        val okRequestBody = when (method.uppercase()) {
            "POST", "PUT", "PATCH" -> (bodyContent ?: "").toRequestBody(mediaType)
            "DELETE" -> bodyContent?.toRequestBody(mediaType)
            else -> null
        }
        requestBuilder.method(method.uppercase(), okRequestBody)

        val request = requestBuilder.build()

        // Extract request headers
        val reqHeadersMap = mutableMapOf<String, String>()
        for (i in 0 until request.headers.size) {
            reqHeadersMap[request.headers.name(i)] = request.headers.value(i)
        }

        val queryParams = mutableMapOf<String, String>()
        parsedUrl.query?.let { q ->
            queryParams.putAll(TrafficRecord.parseUrlEncoded(q))
        }

        val reqContentType = headers["Content-Type"]
        val formParams = mutableMapOf<String, String>()
        if (reqContentType?.contains("application/x-www-form-urlencoded", ignoreCase = true) == true) {
            formParams.putAll(TrafficRecord.parseUrlEncoded(bodyContent))
        } else if (reqContentType?.contains("application/json", ignoreCase = true) == true && bodyContent != null) {
            try {
                val json = JSONObject(bodyContent)
                json.keys().forEach { k -> formParams[k] = json.optString(k, "") }
            } catch (_: Exception) {}
        }

        val initialRecord = TrafficRecord(
            id = recordId,
            timestamp = startTime,
            method = method.uppercase(),
            scheme = parsedUrl.protocol,
            url = urlStr,
            host = parsedUrl.host,
            port = if (parsedUrl.port != -1) parsedUrl.port else parsedUrl.defaultPort,
            path = parsedUrl.path.ifEmpty { "/" },
            queryParams = queryParams,
            protocol = "HTTP/1.1",
            requestHeaders = reqHeadersMap,
            requestContentType = reqContentType,
            requestBody = bodyContent,
            formParameters = formParams,
            requestSize = (bodyContent?.toByteArray(Charsets.UTF_8)?.size ?: 0).toLong(),
            clientIp = "127.0.0.1",
            classification = TrafficClassification.PARSED_HTTP,
            isReplay = isReplay
        )

        // Only log directly if not routed through the local proxy socket,
        // because the proxy socket server already captures proxy-routed requests!
        if (!routeViaLocalProxy || !ProxyServerEngine.isRunning.value) {
            TrafficRepository.addRecord(initialRecord)
        }

        try {
            val response = client.newCall(request).execute()
            val duration = System.currentTimeMillis() - startTime

            val respHeadersMap = mutableMapOf<String, String>()
            for (i in 0 until response.headers.size) {
                respHeadersMap[response.headers.name(i)] = response.headers.value(i)
            }

            val responseBodyString = response.body?.string() ?: ""
            val responseSize = responseBodyString.toByteArray(Charsets.UTF_8).size.toLong()

            val completedRecord = initialRecord.copy(
                responseCode = response.code,
                responseMessage = response.message,
                responseHeaders = respHeadersMap,
                responseContentType = response.header("Content-Type"),
                responseBody = responseBodyString,
                durationMs = duration,
                responseSize = responseSize
            )

            if (!routeViaLocalProxy || !ProxyServerEngine.isRunning.value) {
                TrafficRepository.updateRecord(completedRecord)
            }

            Result.success(completedRecord)
        } catch (e: Exception) {
            val duration = System.currentTimeMillis() - startTime
            val failedRecord = initialRecord.copy(
                responseCode = 0,
                responseMessage = "Connection Failed",
                error = e.localizedMessage ?: e.javaClass.simpleName,
                durationMs = duration
            )

            if (!routeViaLocalProxy || !ProxyServerEngine.isRunning.value) {
                TrafficRepository.updateRecord(failedRecord)
            }

            Result.failure(e)
        }
    }
}
