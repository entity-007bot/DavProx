package com.example.davprox.model

import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

data class EndpointEntry(
    val domain: String,
    val method: String,
    val path: String,
    val queryParams: List<String> = emptyList(),
    val contentType: String = "N/A",
    val lastStatusCode: Int = 0,
    val observationCount: Int = 1,
    val lastSeenTimestamp: Long = System.currentTimeMillis(),
    val sampleRecordId: String = ""
) {
    val endpointKey: String
        get() = "$domain|$method|$path"

    val formattedLastSeen: String
        get() = SimpleDateFormat("HH:mm:ss", Locale.getDefault()).format(Date(lastSeenTimestamp))
}
