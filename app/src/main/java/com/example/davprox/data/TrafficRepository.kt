package com.example.davprox.data

import com.example.davprox.model.TrafficRecord
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import org.json.JSONArray
import org.json.JSONObject

object TrafficRepository {
    private val _records = MutableStateFlow<List<TrafficRecord>>(emptyList())
    val records: StateFlow<List<TrafficRecord>> = _records.asStateFlow()

    private val lock = Any()

    fun addRecord(record: TrafficRecord) {
        synchronized(lock) {
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
        }
    }

    fun clearAll() {
        synchronized(lock) {
            _records.value = emptyList()
        }
    }

    fun exportAsJson(): String {
        val list = _records.value
        val root = JSONObject()
        root.put("app", "DavProx by ENTITY_007 (Chukwuyem David)")
        root.put("version", "1.0")
        root.put("exportedAt", System.currentTimeMillis())
        root.put("totalTransactions", list.size)

        val transactions = JSONArray()
        for (item in list) {
            val obj = JSONObject()
            obj.put("id", item.id)
            obj.put("timestamp", item.timestamp)
            obj.put("timeFormatted", item.formattedTime)
            obj.put("method", item.method)
            obj.put("url", item.url)
            obj.put("host", item.host)
            obj.put("port", item.port)
            obj.put("protocol", item.protocol)
            obj.put("responseCode", item.responseCode)
            obj.put("responseMessage", item.responseMessage)
            obj.put("durationMs", item.durationMs)
            obj.put("requestSize", item.requestSize)
            obj.put("responseSize", item.responseSize)
            obj.put("isTlsTunnel", item.isTlsTunnel)
            obj.put("error", item.error ?: "")

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

    fun generateCurl(record: TrafficRecord): String {
        val sb = StringBuilder("curl -X ${record.method} \"${record.url}\"")
        for ((key, value) in record.requestHeaders) {
            if (!key.equals("Host", ignoreCase = true) && !key.equals("Content-Length", ignoreCase = true)) {
                sb.append(" \\\n  -H \"$key: $value\"")
            }
        }
        if (!record.requestBody.isNullOrEmpty()) {
            val escapedBody = record.requestBody.replace("\"", "\\\"")
            sb.append(" \\\n  --data \"$escapedBody\"")
        }
        return sb.toString()
    }
}
