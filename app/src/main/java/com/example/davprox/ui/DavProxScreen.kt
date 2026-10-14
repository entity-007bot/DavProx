package com.example.davprox.ui

import android.content.Intent
import android.widget.Toast
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Clear
import androidx.compose.material.icons.filled.DeleteSweep
import androidx.compose.material.icons.filled.FilterList
import androidx.compose.material.icons.filled.Lan
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.Router
import androidx.compose.material.icons.filled.Search
import androidx.compose.material.icons.filled.Security
import androidx.compose.material.icons.filled.Send
import androidx.compose.material.icons.filled.Sensors
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material.icons.filled.Share
import androidx.compose.material.icons.filled.Speed
import androidx.compose.material.icons.filled.Stop
import androidx.compose.material.icons.filled.SwapVert
import androidx.compose.material.icons.filled.Tune
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CenterAlignedTopAppBar
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilterChip
import androidx.compose.material3.FloatingActionButton
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalClipboardManager
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import com.example.davprox.ui.components.LiveTestPanel
import com.example.davprox.ui.components.ProxyConfigDialog
import com.example.davprox.ui.components.TrafficDetailSheet
import com.example.davprox.ui.components.TrafficItemRow

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun DavProxScreen(
    viewModel: TrafficViewModel = viewModel()
) {
    val context = LocalContext.current
    val clipboardManager = LocalClipboardManager.current

    val records by viewModel.filteredRecords.collectAsStateWithLifecycle()
    val totalCount by viewModel.totalCount.collectAsStateWithLifecycle()
    val isProxyRunning by viewModel.isProxyRunning.collectAsStateWithLifecycle()
    val proxyPort by viewModel.proxyPort.collectAsStateWithLifecycle()
    val activeConnections by viewModel.activeConnections.collectAsStateWithLifecycle()
    val proxyError by viewModel.proxyError.collectAsStateWithLifecycle()

    val searchQuery by viewModel.searchQuery.collectAsStateWithLifecycle()
    val selectedStatusFilter by viewModel.selectedStatusFilter.collectAsStateWithLifecycle()
    val selectedMethodFilter by viewModel.selectedMethodFilter.collectAsStateWithLifecycle()
    val selectedRecord by viewModel.selectedRecord.collectAsStateWithLifecycle()

    val isSendingLiveRequest by viewModel.isSendingLiveRequest.collectAsStateWithLifecycle()
    val liveRequestMessage by viewModel.liveRequestMessage.collectAsStateWithLifecycle()

    var showConfigDialog by remember { mutableStateOf(false) }
    var showLiveTestPanel by remember { mutableStateOf(false) }
    var showExportDialog by remember { mutableStateOf(false) }
    var showClearConfirmDialog by remember { mutableStateOf(false) }

    // Start proxy engine automatically on launch
    LaunchedEffect(Unit) {
        if (!isProxyRunning) {
            viewModel.toggleProxy()
        }
    }

    Scaffold(
        topBar = {
            CenterAlignedTopAppBar(
                title = {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Surface(
                            shape = RoundedCornerShape(8.dp),
                            color = MaterialTheme.colorScheme.primaryContainer,
                            modifier = Modifier.size(36.dp)
                        ) {
                            Box(contentAlignment = Alignment.Center) {
                                Icon(
                                    imageVector = Icons.Default.Security,
                                    contentDescription = null,
                                    tint = MaterialTheme.colorScheme.primary,
                                    modifier = Modifier.size(22.dp)
                                )
                            }
                        }
                        Spacer(modifier = Modifier.width(10.dp))
                        Column {
                            Text(
                                text = "DavProx",
                                style = MaterialTheme.typography.titleLarge,
                                fontWeight = FontWeight.Bold,
                                color = MaterialTheme.colorScheme.onSurface
                            )
                            Text(
                                text = "by ENTITY_007 (Chukwuyem David)",
                                style = MaterialTheme.typography.labelSmall,
                                color = MaterialTheme.colorScheme.primary
                            )
                        }
                    }
                },
                actions = {
                    IconButton(
                        onClick = { showLiveTestPanel = true },
                        modifier = Modifier.testTag("action_live_test")
                    ) {
                        Icon(imageVector = Icons.Default.Send, contentDescription = "Test Request")
                    }
                    IconButton(
                        onClick = { showConfigDialog = true },
                        modifier = Modifier.testTag("action_proxy_config")
                    ) {
                        Icon(imageVector = Icons.Default.Settings, contentDescription = "Proxy Settings")
                    }
                },
                colors = TopAppBarDefaults.centerAlignedTopAppBarColors(
                    containerColor = MaterialTheme.colorScheme.surface
                )
            )
        },
        floatingActionButton = {
            FloatingActionButton(
                onClick = { showLiveTestPanel = true },
                containerColor = MaterialTheme.colorScheme.primary,
                contentColor = MaterialTheme.colorScheme.onPrimary,
                modifier = Modifier.testTag("fab_live_request")
            ) {
                Icon(imageVector = Icons.Default.Send, contentDescription = "Dispatch Request")
            }
        }
    ) { innerPadding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
        ) {
            // Server Status Banner Card
            ServerStatusBar(
                isRunning = isProxyRunning,
                port = proxyPort,
                localIp = viewModel.getLocalIpAddress(),
                activeConnections = activeConnections,
                onToggle = { viewModel.toggleProxy() },
                onOpenConfig = { showConfigDialog = true }
            )

            // Search Bar
            OutlinedTextField(
                value = searchQuery,
                onValueChange = { viewModel.onSearchQueryChanged(it) },
                placeholder = { Text("Filter by URL, host, body...") },
                leadingIcon = {
                    Icon(imageVector = Icons.Default.Search, contentDescription = "Search")
                },
                trailingIcon = {
                    if (searchQuery.isNotEmpty()) {
                        IconButton(onClick = { viewModel.onSearchQueryChanged("") }) {
                            Icon(imageVector = Icons.Default.Clear, contentDescription = "Clear search")
                        }
                    }
                },
                singleLine = true,
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp, vertical = 6.dp)
                    .testTag("search_traffic_input")
            )

            // Filter Chips (Status and Methods)
            FilterBar(
                selectedStatus = selectedStatusFilter,
                selectedMethod = selectedMethodFilter,
                onStatusSelected = { viewModel.onStatusFilterChanged(it) },
                onMethodSelected = { viewModel.onMethodFilterChanged(it) }
            )

            // Sub-header toolbar: item count, export, clear
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp, vertical = 4.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Text(
                    text = "${records.size} of $totalCount packets",
                    style = MaterialTheme.typography.labelMedium,
                    color = MaterialTheme.colorScheme.outline
                )

                Row(horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                    IconButton(
                        onClick = { showExportDialog = true },
                        enabled = totalCount > 0,
                        modifier = Modifier.size(32.dp).testTag("export_btn")
                    ) {
                        Icon(
                            imageVector = Icons.Default.Share,
                            contentDescription = "Export History",
                            modifier = Modifier.size(18.dp)
                        )
                    }

                    IconButton(
                        onClick = { showClearConfirmDialog = true },
                        enabled = totalCount > 0,
                        modifier = Modifier.size(32.dp).testTag("clear_all_btn")
                    ) {
                        Icon(
                            imageVector = Icons.Default.DeleteSweep,
                            contentDescription = "Clear History",
                            modifier = Modifier.size(18.dp)
                        )
                    }
                }
            }

            // Main Traffic List
            if (records.isEmpty()) {
                EmptyTrafficState(
                    isFilterActive = searchQuery.isNotEmpty() || selectedStatusFilter != StatusFilter.ALL || selectedMethodFilter != "ALL",
                    isProxyRunning = isProxyRunning,
                    onOpenTest = { showLiveTestPanel = true },
                    onStartEngine = { viewModel.toggleProxy() }
                )
            } else {
                LazyColumn(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(horizontal = 16.dp),
                    verticalArrangement = Arrangement.spacedBy(8.dp),
                    contentPadding = PaddingValues(top = 4.dp, bottom = 80.dp)
                ) {
                    items(records, key = { it.id }) { record ->
                        TrafficItemRow(
                            record = record,
                            onClick = { viewModel.selectRecord(record) }
                        )
                    }
                }
            }
        }
    }

    // Modal Bottom Sheets & Dialogs
    selectedRecord?.let { record ->
        TrafficDetailSheet(
            record = record,
            onDismiss = { viewModel.selectRecord(null) },
            onCopyCurl = {
                val curl = viewModel.getCurl(record)
                clipboardManager.setText(AnnotatedString(curl))
                Toast.makeText(context, "cURL command copied", Toast.LENGTH_SHORT).show()
            }
        )
    }

    if (showConfigDialog) {
        ProxyConfigDialog(
            isRunning = isProxyRunning,
            currentPort = proxyPort,
            localIp = viewModel.getLocalIpAddress(),
            activeConnections = activeConnections,
            errorMessage = proxyError,
            onToggleProxy = { viewModel.toggleProxy() },
            onPortChanged = { viewModel.updateProxyPort(it) },
            onDismiss = { showConfigDialog = false }
        )
    }

    if (showLiveTestPanel) {
        LiveTestPanel(
            isLoading = isSendingLiveRequest,
            statusMessage = liveRequestMessage,
            isProxyRunning = isProxyRunning,
            onSendRequest = { url, method, headers, body, routeViaProxy ->
                viewModel.sendTestRequest(url, method, headers, body, routeViaProxy)
            },
            onDismiss = {
                showLiveTestPanel = false
                viewModel.clearLiveRequestMessage()
            }
        )
    }

    if (showExportDialog) {
        ExportSessionDialog(
            jsonExport = viewModel.getExportJson(),
            onDismiss = { showExportDialog = false }
        )
    }

    if (showClearConfirmDialog) {
        AlertDialog(
            onDismissRequest = { showClearConfirmDialog = false },
            title = { Text("Clear Session History?") },
            text = { Text("This will remove all captured requests from the current session.") },
            confirmButton = {
                TextButton(
                    onClick = {
                        viewModel.clearHistory()
                        showClearConfirmDialog = false
                    }
                ) {
                    Text("Clear All", color = MaterialTheme.colorScheme.error)
                }
            },
            dismissButton = {
                TextButton(onClick = { showClearConfirmDialog = false }) {
                    Text("Cancel")
                }
            }
        )
    }
}

@Composable
fun ServerStatusBar(
    isRunning: Boolean,
    port: Int,
    localIp: String,
    activeConnections: Int,
    onToggle: () -> Unit,
    onOpenConfig: () -> Unit
) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp, vertical = 6.dp)
            .clickable(onClick = onOpenConfig),
        colors = CardDefaults.cardColors(
            containerColor = if (isRunning)
                Color(0xFF2E7D32).copy(alpha = 0.12f)
            else
                MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f)
        ),
        shape = RoundedCornerShape(12.dp)
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 12.dp, vertical = 8.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                modifier = Modifier.weight(1f)
            ) {
                Surface(
                    shape = RoundedCornerShape(8.dp),
                    color = if (isRunning) Color(0xFF2E7D32).copy(alpha = 0.2f) else MaterialTheme.colorScheme.surfaceVariant,
                    modifier = Modifier.size(36.dp)
                ) {
                    Box(contentAlignment = Alignment.Center) {
                        Icon(
                            imageVector = if (isRunning) Icons.Default.Router else Icons.Default.Lan,
                            contentDescription = null,
                            tint = if (isRunning) Color(0xFF2E7D32) else MaterialTheme.colorScheme.outline,
                            modifier = Modifier.size(20.dp)
                        )
                    }
                }
                Spacer(modifier = Modifier.width(10.dp))
                Column {
                    Text(
                        text = if (isRunning) "PROXY ACTIVE : $localIp:$port" else "PROXY ENGINE STOPPED",
                        style = MaterialTheme.typography.labelMedium.copy(
                            fontWeight = FontWeight.Bold,
                            fontFamily = FontFamily.Monospace
                        ),
                        color = if (isRunning) Color(0xFF2E7D32) else MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    Text(
                        text = if (isRunning) "$activeConnections active connection(s) • Tap to configure" else "Tap to start or view Wi-Fi settings",
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.outline
                    )
                }
            }

            IconButton(
                onClick = onToggle,
                modifier = Modifier.size(36.dp).testTag("status_toggle_btn")
            ) {
                Icon(
                    imageVector = if (isRunning) Icons.Default.Stop else Icons.Default.PlayArrow,
                    contentDescription = if (isRunning) "Stop Proxy" else "Start Proxy",
                    tint = if (isRunning) MaterialTheme.colorScheme.error else MaterialTheme.colorScheme.primary
                )
            }
        }
    }
}

@Composable
fun FilterBar(
    selectedStatus: StatusFilter,
    selectedMethod: String,
    onStatusSelected: (StatusFilter) -> Unit,
    onMethodSelected: (String) -> Unit
) {
    val methods = listOf("ALL", "GET", "POST", "CONNECT", "PUT", "DELETE")

    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp, vertical = 2.dp)
    ) {
        // Status row
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .horizontalScroll(rememberScrollState()),
            horizontalArrangement = Arrangement.spacedBy(6.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Icon(
                imageVector = Icons.Default.Tune,
                contentDescription = "Filters",
                modifier = Modifier.size(16.dp),
                tint = MaterialTheme.colorScheme.primary
            )
            StatusFilter.values().forEach { filter ->
                FilterChip(
                    selected = selectedStatus == filter,
                    onClick = { onStatusSelected(filter) },
                    label = { Text(filter.label, fontSize = 11.sp) },
                    modifier = Modifier.height(30.dp)
                )
            }
        }

        Spacer(modifier = Modifier.height(4.dp))

        // Methods row
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .horizontalScroll(rememberScrollState()),
            horizontalArrangement = Arrangement.spacedBy(6.dp)
        ) {
            methods.forEach { method ->
                FilterChip(
                    selected = selectedMethod == method,
                    onClick = { onMethodSelected(method) },
                    label = { Text(method, fontSize = 11.sp, fontWeight = FontWeight.SemiBold) },
                    modifier = Modifier.height(30.dp)
                )
            }
        }
    }
}

@Composable
fun EmptyTrafficState(
    isFilterActive: Boolean,
    isProxyRunning: Boolean,
    onOpenTest: () -> Unit,
    onStartEngine: () -> Unit
) {
    Box(
        modifier = Modifier
            .fillMaxSize()
            .padding(32.dp),
        contentAlignment = Alignment.Center
    ) {
        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center
        ) {
            Surface(
                shape = RoundedCornerShape(20.dp),
                color = MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.45f),
                modifier = Modifier.size(80.dp)
            ) {
                Box(contentAlignment = Alignment.Center) {
                    Icon(
                        imageVector = if (isFilterActive) Icons.Default.FilterList else Icons.Default.Sensors,
                        contentDescription = null,
                        modifier = Modifier.size(42.dp),
                        tint = MaterialTheme.colorScheme.primary
                    )
                }
            }

            Spacer(modifier = Modifier.height(18.dp))

            Text(
                text = if (isFilterActive) "No Matching Packets" else "Network Monitor Standby",
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.Bold
            )

            Spacer(modifier = Modifier.height(6.dp))

            Text(
                text = if (isFilterActive)
                    "Try adjusting your search query or filter chips above."
                else
                    "Start testing by dispatching a live request or routing your mobile device Wi-Fi proxy to DavProx.",
                style = MaterialTheme.typography.bodyMedium,
                textAlign = TextAlign.Center,
                color = MaterialTheme.colorScheme.outline
            )

            Spacer(modifier = Modifier.height(20.dp))

            Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                Button(
                    onClick = onOpenTest,
                    modifier = Modifier.testTag("empty_state_test_btn")
                ) {
                    Icon(imageVector = Icons.Default.Send, contentDescription = null, modifier = Modifier.size(16.dp))
                    Spacer(modifier = Modifier.width(6.dp))
                    Text("Send Test Request")
                }

                if (!isProxyRunning) {
                    Button(onClick = onStartEngine) {
                        Text("Start Engine")
                    }
                }
            }
        }
    }
}

@Composable
fun ExportSessionDialog(
    jsonExport: String,
    onDismiss: () -> Unit
) {
    val context = LocalContext.current
    val clipboardManager = LocalClipboardManager.current

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Export Session Data") },
        text = {
            Column {
                Text(
                    text = "Export captured session transactions in standard JSON format for debugging, reporting, or archiving.",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                Spacer(modifier = Modifier.height(12.dp))
                Surface(
                    color = MaterialTheme.colorScheme.surfaceVariant,
                    shape = RoundedCornerShape(8.dp),
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(140.dp)
                ) {
                    Box(modifier = Modifier.padding(8.dp)) {
                        Text(
                            text = jsonExport.take(600) + if (jsonExport.length > 600) "\n..." else "",
                            style = MaterialTheme.typography.bodySmall.copy(
                                fontFamily = FontFamily.Monospace,
                                fontSize = 10.sp
                            )
                        )
                    }
                }
            }
        },
        confirmButton = {
            Button(
                onClick = {
                    val sendIntent = Intent().apply {
                        action = Intent.ACTION_SEND
                        putExtra(Intent.EXTRA_TEXT, jsonExport)
                        type = "application/json"
                    }
                    val shareIntent = Intent.createChooser(sendIntent, "Share DavProx Session Log")
                    context.startActivity(shareIntent)
                    onDismiss()
                }
            ) {
                Text("Share JSON")
            }
        },
        dismissButton = {
            TextButton(
                onClick = {
                    clipboardManager.setText(AnnotatedString(jsonExport))
                    Toast.makeText(context, "JSON log copied to clipboard", Toast.LENGTH_SHORT).show()
                    onDismiss()
                }
            ) {
                Text("Copy to Clipboard")
            }
        }
    )
}
