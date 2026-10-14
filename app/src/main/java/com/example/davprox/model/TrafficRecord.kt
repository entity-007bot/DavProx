package com.example.davprox.model

import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale
import java.util.UUID

data class TrafficRecord(
    val id: String = UUID.randomUUID().toString(),
    val timestamp: Long = System.currentTimeMillis(),
    val method: String, // GET, POST, CONNECT, etc.
    val url: String,
    val host: String,
    val port: Int,
    val protocol: String = "HTTP/1.1",
    val requestHeaders: Map<String, String> = emptyMap(),
    val requestBody: String? = null,
    val responseCode: Int = 0, // 0 for pending/tunnel/error
    val responseMessage: String = "",
    val responseHeaders: Map<String, String> = emptyMap(),
    val responseBody: String? = null,
    val durationMs: Long = 0,
    val requestSize: Long = 0,
    val responseSize: Long = 0,
    val isTlsTunnel: Boolean = false,
    val error: String? = null,
    val clientIp: String = "127.0.0.1"
) {
    val formattedTime: String
        get() = SimpleDateFormat("HH:mm:ss.SSS", Locale.getDefault()).format(Date(timestamp))

    val isSuccess: Boolean
        get() = responseCode in 200..299

    val isClientError: Boolean
        get() = responseCode in 400..499

    val isServerError: Boolean
        get() = responseCode >= 500

    val isPending: Boolean
        get() = responseCode == 0 && error == null
}
