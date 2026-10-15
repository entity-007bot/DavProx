package com.example.davprox.model

import java.net.URLDecoder
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale
import java.util.UUID

enum class TrafficClassification(val label: String) {
    PARSED_HTTP("Parsed HTTP Flow"),
    NETWORK_TUNNEL("Encrypted TLS Tunnel"),
    DECRYPTED_TLS("Decrypted TLS Flow")
}

data class TrafficRecord(
    val id: String = UUID.randomUUID().toString(),
    val timestamp: Long = System.currentTimeMillis(),
    val method: String, // GET, POST, CONNECT, etc.
    val scheme: String = "http", // http, https
    val url: String,
    val host: String,
    val port: Int,
    val destIp: String? = null,
    val path: String = "/",
    val queryParams: Map<String, String> = emptyMap(),
    val protocol: String = "HTTP/1.1",
    val requestHeaders: Map<String, String> = emptyMap(),
    val requestContentType: String? = null,
    val requestBody: String? = null,
    val formParameters: Map<String, String> = emptyMap(),
    val responseCode: Int = 0, // 0 for pending/tunnel/error
    val responseMessage: String = "",
    val responseHeaders: Map<String, String> = emptyMap(),
    val responseContentType: String? = null,
    val responseBody: String? = null,
    val durationMs: Long = 0,
    val requestSize: Long = 0,
    val responseSize: Long = 0,
    val isTlsTunnel: Boolean = false,
    val classification: TrafficClassification = if (isTlsTunnel) TrafficClassification.NETWORK_TUNNEL else TrafficClassification.PARSED_HTTP,
    val error: String? = null,
    val clientIp: String = "127.0.0.1",
    val isReplay: Boolean = false
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

    fun getSanitized(redactSensitive: Boolean): TrafficRecord {
        if (!redactSensitive) return this

        val sensitiveKeys = listOf("password", "passwd", "token", "auth", "secret", "cookie", "key", "authorization", "session", "access_token", "credit_card")

        val sanitizedHeaders = requestHeaders.mapValues { (k, v) ->
            if (sensitiveKeys.any { k.contains(it, ignoreCase = true) }) "[REDACTED]" else v
        }

        val sanitizedFormParams = formParameters.mapValues { (k, v) ->
            if (sensitiveKeys.any { k.contains(it, ignoreCase = true) }) "[REDACTED]" else v
        }

        val sanitizedQueryParams = queryParams.mapValues { (k, v) ->
            if (sensitiveKeys.any { k.contains(it, ignoreCase = true) }) "[REDACTED]" else v
        }

        var sanitizedBody = requestBody
        if (sanitizedBody != null) {
            sensitiveKeys.forEach { key ->
                // Simple regex to redact key values in json/form
                sanitizedBody = sanitizedBody?.replace(Regex("(?i)(\"$key\"\\s*:\\s*\")[^\"]+(\")"), "$1[REDACTED]$2")
                sanitizedBody = sanitizedBody?.replace(Regex("(?i)($key=)[^&]+"), "$1[REDACTED]")
            }
        }

        return copy(
            requestHeaders = sanitizedHeaders,
            formParameters = sanitizedFormParams,
            queryParams = sanitizedQueryParams,
            requestBody = sanitizedBody
        )
    }

    companion object {
        fun parseUrlEncoded(raw: String?): Map<String, String> {
            if (raw.isNullOrBlank()) return emptyMap()
            val result = mutableMapOf<String, String>()
            try {
                val pairs = raw.split("&")
                for (pair in pairs) {
                    val idx = pair.indexOf("=")
                    if (idx > 0) {
                        val key = URLDecoder.decode(pair.substring(0, idx), "UTF-8")
                        val value = if (pair.length > idx + 1) URLDecoder.decode(pair.substring(idx + 1), "UTF-8") else ""
                        result[key] = value
                    } else if (pair.isNotBlank()) {
                        result[URLDecoder.decode(pair, "UTF-8")] = ""
                    }
                }
            } catch (_: Exception) {}
            return result
        }
    }
}
