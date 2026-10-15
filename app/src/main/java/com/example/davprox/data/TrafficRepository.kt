package com.example.davprox.data

import com.example.davprox.model.EndpointEntry
import com.example.davprox.model.TrafficClassification
import com.example.davprox.model.TrafficRecord
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import org.json.JSONArray
import org.json.JSONObject

object TrafficRepository {
    private val _records = MutableStateFlow<List<TrafficRecord>>(emptyList())
    val records: StateFlow<List<TrafficRecord>> = _records.asStateFlow()

    private val _isPaused = MutableStateFlow(false)
    val isPaused: StateFlow<Boolean> = _isPaused.asStateFlow()

    private val _isRedactionEnabled = MutableStateFlow(true)
    val isRedactionEnabled: StateFlow<Boolean> = _isRedactionEnabled.asStateFlow()

    // Diagnostic counters
    private val _totalObserved = MutableStateFlow(0)
    val totalObserved: StateFlow<Int> = _totalObserved.asStateFlow()

    private val _httpsTunnelsCount = MutableStateFlow(0)
    val httpsTunnelsCount: StateFlow<Int> = _httpsTunnelsCount.asStateFlow()

    private val _parsedHttpCount = MutableStateFlow(0)
    val parsedHttpCount: StateFlow<Int> = _parsedHttpCount.asStateFlow()

    private val _failuresCount = MutableStateFlow(0)
    val failuresCount: StateFlow<Int> = _failuresCount.asStateFlow()

    // Active Replay / Comparison
    private val _replayedComparison = MutableStateFlow<Pair<TrafficRecord, TrafficRecord>?>(null)
    val replayedComparison: StateFlow<Pair<TrafficRecord, TrafficRecord>?> = _replayedComparison.asStateFlow()

    private val lock = Any()

    fun togglePause() {
        _isPaused.value = !_isPaused.value
    }

    fun toggleRedaction() {
        _isRedactionEnabled.value = !_isRedactionEnabled.value
    }

    fun setReplayComparison(original: TrafficRecord, replayed: TrafficRecord) {
        _replayedComparison.value = original to replayed
    }

    fun clearReplayComparison() {
        _replayedComparison.value = null
    }

    fun addRecord(record: TrafficRecord) {
        if (_isPaused.value) return

        synchronized(lock) {
            _totalObserved.value += 1
            if (record.isTlsTunnel) {
                _httpsTunnelsCount.value += 1
            } else {
                _parsedHttpCount.value += 1
            }
            if (record.error != null || record.responseCode >= 500) {
                _failuresCount.value += 1
            }

            val current = _records.value.toMutableList()
            current.add(0, record) // newest first
            // Cap at 500 items to avoid excessive memory on mobile devices
            if (current.size > 500) {
                _records.value = current.take(500)
            } else {
                _records.value = current
            }
        }
    }

    fun updateRecord(record: TrafficRecord) {
        synchronized(lock) {
            val current = _records.value.toMutableList()
            val index = current.indexOfFirst { it.id == record.id }
            if (index != -1) {
                current[index] = record
                _records.value = current
            } else {
                current.add(0, record)
                _records.value = current
            }

            if (record.error != null && _records.value.none { it.id == record.id && it.error != null }) {
                _failuresCount.value += 1
            }
        }
    }

    fun clearAll() {
        synchronized(lock) {
            _records.value = emptyList()
            _totalObserved.value = 0
            _httpsTunnelsCount.value = 0
            _parsedHttpCount.value = 0
            _failuresCount.value = 0
            _replayedComparison.value = null
        }
    }

    fun getDiscoveredEndpoints(): List<EndpointEntry> {
        val list = _records.value
        val map = mutableMapOf<String, EndpointEntry>()

        for (item in list) {
            if (item.method == "CONNECT") continue // skip raw connect tunnels for endpoint discovery

            val key = "${item.host}|${item.method}|${item.path}"
            val existing = map[key]
            if (existing == null) {
                map[key] = EndpointEntry(
                    domain = item.host,
                    method = item.method,
                    path = item.path,
                    queryParams = item.queryParams.keys.toList(),
                    contentType = item.requestContentType ?: item.responseContentType ?: "text/html",
                    lastStatusCode = item.responseCode,
                    observationCount = 1,
                    lastSeenTimestamp = item.timestamp,
                    sampleRecordId = item.id
                )
            } else {
                val combinedQuery = (existing.queryParams + item.queryParams.keys).distinct()
                map[key] = existing.copy(
                    observationCount = existing.observationCount + 1,
                    lastStatusCode = if (item.responseCode > 0) item.responseCode else existing.lastStatusCode,
                    lastSeenTimestamp = maxOf(existing.lastSeenTimestamp, item.timestamp),
                    queryParams = combinedQuery
                )
            }
        }

        return map.values.sortedByDescending { it.lastSeenTimestamp }
    }

    fun exportAsJson(): String {
        val redact = _isRedactionEnabled.value
        val list = _records.value.map { it.getSanitized(redact) }
        val root = JSONObject()
        root.put("app", "DavProx by ENTITY_007 (Chukwuyem David)")
        root.put("version", "1.0")
        root.put("exportedAt", System.currentTimeMillis())
        root.put("redacted", redact)
        root.put("totalTransactions", list.size)

        val transactions = JSONArray()
        for (item in list) {
            val obj = JSONObject()
            obj.put("id", item.id)
            obj.put("timestamp", item.timestamp)
            obj.put("timeFormatted", item.formattedTime)
            obj.put("classification", item.classification.label)
            obj.put("method", item.method)
            obj.put("scheme", item.scheme)
            obj.put("url", item.url)
            obj.put("host", item.host)
            obj.put("port", item.port)
            obj.put("destIp", item.destIp ?: "")
            obj.put("path", item.path)
            obj.put("protocol", item.protocol)
            obj.put("requestContentType", item.requestContentType ?: "")
            obj.put("responseContentType", item.responseContentType ?: "")
            obj.put("responseCode", item.responseCode)
            obj.put("responseMessage", item.responseMessage)
            obj.put("durationMs", item.durationMs)
            obj.put("requestSize", item.requestSize)
            obj.put("responseSize", item.responseSize)
            obj.put("isTlsTunnel", item.isTlsTunnel)
            obj.put("error", item.error ?: "")

            val queryParamsObj = JSONObject()
            item.queryParams.forEach { (k, v) -> queryParamsObj.put(k, v) }
            obj.put("queryParams", queryParamsObj)

            val formParamsObj = JSONObject()
            item.formParameters.forEach { (k, v) -> formParamsObj.put(k, v) }
            obj.put("formParameters", formParamsObj)

            val reqHeaders = JSONObject()
            item.requestHeaders.forEach { (k, v) -> reqHeaders.put(k, v) }
            obj.put("requestHeaders", reqHeaders)
            obj.put("requestBody", item.requestBody ?: "")

            val resHeaders = JSONObject()
            item.responseHeaders.forEach { (k, v) -> resHeaders.put(k, v) }
            obj.put("responseHeaders", resHeaders)
            obj.put("responseBody", item.responseBody ?: "")

            transactions.put(obj)
        }
        root.put("transactions", transactions)
        return root.toString(2)
    }

    fun exportEndpointsAsJson(): String {
        val endpoints = getDiscoveredEndpoints()
        val root = JSONObject()
        root.put("app", "DavProx Endpoint Discovery Inventory")
        root.put("totalEndpoints", endpoints.size)
        root.put("exportedAt", System.currentTimeMillis())

        val arr = JSONArray()
        for (ep in endpoints) {
            val item = JSONObject()
            item.put("domain", ep.domain)
            item.put("method", ep.method)
            item.put("path", ep.path)
            item.put("contentType", ep.contentType)
            item.put("lastStatusCode", ep.lastStatusCode)
            item.put("observationCount", ep.observationCount)
            item.put("lastSeen", ep.formattedLastSeen)
            val queryArr = JSONArray()
            ep.queryParams.forEach { queryArr.put(it) }
            item.put("queryParameterNames", queryArr)
            arr.put(item)
        }
        root.put("endpoints", arr)
        return root.toString(2)
    }

    fun generateCurl(record: TrafficRecord): String {
        val sanitized = record.getSanitized(_isRedactionEnabled.value)
        val sb = StringBuilder("curl -X ${sanitized.method} \"${sanitized.url}\"")
        for ((key, value) in sanitized.requestHeaders) {
            if (!key.equals("Host", ignoreCase = true) && !key.equals("Content-Length", ignoreCase = true)) {
                sb.append(" \\\n  -H \"$key: $value\"")
            }
        }
        if (!sanitized.requestBody.isNullOrEmpty()) {
            val escapedBody = sanitized.requestBody.replace("\"", "\\\"")
            sb.append(" \\\n  --data \"$escapedBody\"")
        }
        return sb.toString()
    }
}
