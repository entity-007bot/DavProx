package com.example.davprox.ui

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.davprox.data.TrafficRepository
import com.example.davprox.engine.LiveNetworkTester
import com.example.davprox.engine.ProxyServerEngine
import com.example.davprox.model.TrafficRecord
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

enum class StatusFilter(val label: String) {
    ALL("All"),
    SUCCESS("2xx"),
    REDIRECT("3xx"),
    CLIENT_ERROR("4xx"),
    SERVER_ERROR("5xx"),
    FAILED("Errors")
}

class TrafficViewModel : ViewModel() {

    val isProxyRunning: StateFlow<Boolean> = ProxyServerEngine.isRunning
    val proxyPort: StateFlow<Int> = ProxyServerEngine.currentPort
    val activeConnections: StateFlow<Int> = ProxyServerEngine.activeConnections
    val proxyError: StateFlow<String?> = ProxyServerEngine.lastError

    private val _searchQuery = MutableStateFlow("")
    val searchQuery: StateFlow<String> = _searchQuery.asStateFlow()

    private val _selectedStatusFilter = MutableStateFlow(StatusFilter.ALL)
    val selectedStatusFilter: StateFlow<StatusFilter> = _selectedStatusFilter.asStateFlow()

    private val _selectedMethodFilter = MutableStateFlow("ALL")
    val selectedMethodFilter: StateFlow<String> = _selectedMethodFilter.asStateFlow()

    private val _selectedRecord = MutableStateFlow<TrafficRecord?>(null)
    val selectedRecord: StateFlow<TrafficRecord?> = _selectedRecord.asStateFlow()

    private val _isSendingLiveRequest = MutableStateFlow(false)
    val isSendingLiveRequest: StateFlow<Boolean> = _isSendingLiveRequest.asStateFlow()

    private val _liveRequestMessage = MutableStateFlow<String?>(null)
    val liveRequestMessage: StateFlow<String?> = _liveRequestMessage.asStateFlow()

    // Automatically filter records based on query, status filter, and method
    val filteredRecords: StateFlow<List<TrafficRecord>> = combine(
        TrafficRepository.records,
        _searchQuery,
        _selectedStatusFilter,
        _selectedMethodFilter
    ) { records, query, statusFilter, methodFilter ->
        records.filter { record ->
            // Filter by Method
            val matchesMethod = if (methodFilter == "ALL") true else record.method.equals(methodFilter, ignoreCase = true)
            if (!matchesMethod) return@filter false

            // Filter by Status
            val matchesStatus = when (statusFilter) {
                StatusFilter.ALL -> true
                StatusFilter.SUCCESS -> record.responseCode in 200..299
                StatusFilter.REDIRECT -> record.responseCode in 300..399
                StatusFilter.CLIENT_ERROR -> record.responseCode in 400..499
                StatusFilter.SERVER_ERROR -> record.responseCode in 500..599
                StatusFilter.FAILED -> record.responseCode == 0 || record.error != null
            }
            if (!matchesStatus) return@filter false

            // Filter by Query
            if (query.isBlank()) return@filter true
            val q = query.trim().lowercase()
            record.url.lowercase().contains(q) ||
                record.host.lowercase().contains(q) ||
                record.method.lowercase().contains(q) ||
                (record.responseMessage.lowercase().contains(q)) ||
                (record.requestBody?.lowercase()?.contains(q) == true) ||
                (record.responseBody?.lowercase()?.contains(q) == true)
        }
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    val totalCount: StateFlow<Int> = combine(TrafficRepository.records) { it.first().size }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), 0)

    fun onSearchQueryChanged(query: String) {
        _searchQuery.value = query
    }

    fun onStatusFilterChanged(filter: StatusFilter) {
        _selectedStatusFilter.value = filter
    }

    fun onMethodFilterChanged(method: String) {
        _selectedMethodFilter.value = method
    }

    fun selectRecord(record: TrafficRecord?) {
        _selectedRecord.value = record
    }

    fun toggleProxy() {
        if (ProxyServerEngine.isRunning.value) {
            ProxyServerEngine.stopProxy()
        } else {
            ProxyServerEngine.startProxy(ProxyServerEngine.currentPort.value)
        }
    }

    fun updateProxyPort(port: Int) {
        val wasRunning = ProxyServerEngine.isRunning.value
        ProxyServerEngine.stopProxy()
        if (wasRunning) {
            ProxyServerEngine.startProxy(port)
        } else {
            // Just update port
            ProxyServerEngine.startProxy(port)
            ProxyServerEngine.stopProxy()
        }
    }

    fun clearHistory() {
        TrafficRepository.clearAll()
        _selectedRecord.value = null
    }

    fun getLocalIpAddress(): String {
        return ProxyServerEngine.getLocalIpAddress()
    }

    fun sendTestRequest(
        url: String,
        method: String = "GET",
        headers: Map<String, String> = emptyMap(),
        body: String? = null,
        routeViaLocalProxy: Boolean = false
    ) {
        _isSendingLiveRequest.value = true
        _liveRequestMessage.value = null
        viewModelScope.launch {
            val res = LiveNetworkTester.executeLiveRequest(
                urlStr = url,
                method = method,
                headers = headers,
                bodyContent = body,
                routeViaLocalProxy = routeViaLocalProxy,
                proxyPort = ProxyServerEngine.currentPort.value
            )
            _isSendingLiveRequest.value = false
            res.fold(
                onSuccess = { record ->
                    _liveRequestMessage.value = "Captured ${record.method} -> HTTP ${record.responseCode} (${record.durationMs}ms)"
                },
                onFailure = { err ->
                    _liveRequestMessage.value = "Request failed: ${err.localizedMessage}"
                }
            )
        }
    }

    fun clearLiveRequestMessage() {
        _liveRequestMessage.value = null
    }

    fun getExportJson(): String {
        return TrafficRepository.exportAsJson()
    }

    fun getCurl(record: TrafficRecord): String {
        return TrafficRepository.generateCurl(record)
    }

    override fun onCleared() {
        super.onCleared()
        // Do not necessarily kill server on background unless needed
    }
}
