package com.example.davprox.ui.components

import android.widget.Toast
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.CloudDownload
import androidx.compose.material.icons.filled.CloudUpload
import androidx.compose.material.icons.filled.ContentCopy
import androidx.compose.material.icons.filled.DataObject
import androidx.compose.material.icons.filled.Info
import androidx.compose.material.icons.filled.Lock
import androidx.compose.material.icons.filled.Replay
import androidx.compose.material.icons.filled.Tune
import androidx.compose.material3.Divider
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilledTonalButton
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.PrimaryTabRow
import androidx.compose.material3.ScrollableTabRow
import androidx.compose.material3.Surface
import androidx.compose.material3.Tab
import androidx.compose.material3.Text
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
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
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.davprox.model.TrafficRecord
import org.json.JSONArray
import org.json.JSONObject

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun TrafficDetailSheet(
    record: TrafficRecord,
    onDismiss: () -> Unit,
    onCopyCurl: () -> Unit,
    onOpenInRepeater: (TrafficRecord) -> Unit,
    modifier: Modifier = Modifier
) {
    val sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)
    var selectedTabIndex by remember { mutableIntStateOf(0) }
    val tabs = listOf("Overview", "Headers", "Parameters", "Body", "Response")

    ModalBottomSheet(
        onDismissRequest = onDismiss,
        sheetState = sheetState,
        modifier = modifier.testTag("traffic_detail_sheet")
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .fillMaxHeight(0.9f)
                .padding(horizontal = 16.dp)
        ) {
            // Header Row
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    MethodBadge(method = record.method)
                    Spacer(modifier = Modifier.width(8.dp))
                    StatusBadge(record = record)
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(
                        text = record.host,
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.onSurface
                    )
                }

                IconButton(
                    onClick = onDismiss,
                    modifier = Modifier.testTag("close_detail_sheet")
                ) {
                    Icon(imageVector = Icons.Default.Close, contentDescription = "Close")
                }
            }

            Spacer(modifier = Modifier.height(6.dp))

            // Navigation Tabs (Scrollable)
            ScrollableTabRow(
                selectedTabIndex = selectedTabIndex,
                modifier = Modifier.fillMaxWidth(),
                edgePadding = 0.dp
            ) {
                tabs.forEachIndexed { index, title ->
                    val tabIcon = when (index) {
                        0 -> Icons.Default.Info
                        1 -> Icons.Default.DataObject
                        2 -> Icons.Default.Tune
                        3 -> Icons.Default.CloudUpload
                        else -> Icons.Default.CloudDownload
                    }
                    Tab(
                        selected = selectedTabIndex == index,
                        onClick = { selectedTabIndex = index },
                        text = { Text(title, fontSize = 12.sp) },
                        icon = { Icon(imageVector = tabIcon, contentDescription = title, modifier = Modifier.size(16.dp)) },
                        modifier = Modifier.testTag("tab_$title")
                    )
                }
            }

            Spacer(modifier = Modifier.height(10.dp))

            // Tab Content
            Box(modifier = Modifier.weight(1f)) {
                when (selectedTabIndex) {
                    0 -> DetailOverviewTab(record = record, onCopyCurl = onCopyCurl, onOpenInRepeater = onOpenInRepeater)
                    1 -> DetailHeadersTab(record = record)
                    2 -> DetailParametersTab(record = record)
                    3 -> DetailBodyTab(record = record)
                    4 -> DetailResponseTab(record = record)
                }
            }
        }
    }
}

@Composable
fun DetailOverviewTab(
    record: TrafficRecord,
    onCopyCurl: () -> Unit,
    onOpenInRepeater: (TrafficRecord) -> Unit
) {
    val context = LocalContext.current
    val clipboardManager = LocalClipboardManager.current
    val scrollState = rememberScrollState()

    Column(
        modifier = Modifier
            .fillMaxWidth()
            .verticalScroll(scrollState)
            .padding(bottom = 24.dp)
    ) {
        // Quick Action Bar
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            FilledTonalButton(
                onClick = { onOpenInRepeater(record) },
                modifier = Modifier.weight(1f).testTag("open_in_repeater_btn")
            ) {
                Icon(imageVector = Icons.Default.Replay, contentDescription = null, modifier = Modifier.size(16.dp))
                Spacer(modifier = Modifier.width(4.dp))
                Text("Repeater", fontSize = 12.sp)
            }

            OutlinedButton(
                onClick = onCopyCurl,
                modifier = Modifier.weight(1f).testTag("copy_curl_button")
            ) {
                Icon(imageVector = Icons.Default.ContentCopy, contentDescription = null, modifier = Modifier.size(16.dp))
                Spacer(modifier = Modifier.width(4.dp))
                Text("Copy cURL", fontSize = 12.sp)
            }

            OutlinedButton(
                onClick = {
                    clipboardManager.setText(AnnotatedString(record.url))
                    Toast.makeText(context, "URL copied", Toast.LENGTH_SHORT).show()
                },
                modifier = Modifier.weight(1f)
            ) {
                Icon(imageVector = Icons.Default.ContentCopy, contentDescription = null, modifier = Modifier.size(16.dp))
                Spacer(modifier = Modifier.width(4.dp))
                Text("Copy URL", fontSize = 12.sp)
            }
        }

        Spacer(modifier = Modifier.height(14.dp))

        // Target URL
        InfoSectionTitle("Target Endpoint")
        Surface(
            color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f),
            shape = RoundedCornerShape(8.dp),
            modifier = Modifier.fillMaxWidth()
        ) {
            Text(
                text = record.url,
                style = MaterialTheme.typography.bodySmall.copy(fontFamily = FontFamily.Monospace),
                color = MaterialTheme.colorScheme.onSurface,
                modifier = Modifier.padding(10.dp)
            )
        }

        Spacer(modifier = Modifier.height(14.dp))
        InfoSectionTitle("Traffic Classification & Telemetry")

        MetricRow("Classification", record.classification.label)
        MetricRow("HTTP Status", "${record.responseCode} ${record.responseMessage}".trim())
        MetricRow("Duration", if (record.durationMs > 0) "${record.durationMs} ms" else "Pending")
        MetricRow("Domain & Port", "${record.host}:${record.port}")
        MetricRow("Destination IP", record.destIp ?: "Pending DNS resolution")
        MetricRow("Client Source IP", record.clientIp)
        MetricRow("Protocol & Scheme", "${record.protocol} (${record.scheme.uppercase()})")
        MetricRow("Endpoint Path", record.path)
        MetricRow("Query Parameters", "${record.queryParams.size} parameter(s)")
        MetricRow("Form Parameters", "${record.formParameters.size} parameter(s)")
        MetricRow("Captured At", record.formattedTime)
        MetricRow("Request Size", formatBytes(record.requestSize))
        MetricRow("Response Size", formatBytes(record.responseSize))

        if (record.error != null) {
            Spacer(modifier = Modifier.height(14.dp))
            InfoSectionTitle("Connection Error")
            Surface(
                color = MaterialTheme.colorScheme.errorContainer,
                shape = RoundedCornerShape(8.dp),
                modifier = Modifier.fillMaxWidth()
            ) {
                Text(
                    text = record.error,
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onErrorContainer,
                    modifier = Modifier.padding(10.dp)
                )
            }
        }
    }
}

@Composable
fun DetailHeadersTab(record: TrafficRecord) {
    val scrollState = rememberScrollState()

    Column(
        modifier = Modifier
            .fillMaxWidth()
            .verticalScroll(scrollState)
            .padding(bottom = 24.dp)
    ) {
        InfoSectionTitle("Request Headers (${record.requestHeaders.size})")
        record.requestContentType?.let {
            Text(text = "Content-Type: $it", style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.primary)
        }
        Spacer(modifier = Modifier.height(6.dp))

        if (record.requestHeaders.isEmpty()) {
            Text(text = "No request headers captured", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.outline)
        } else {
            HeadersTable(headers = record.requestHeaders)
        }

        Spacer(modifier = Modifier.height(16.dp))

        InfoSectionTitle("Response Headers (${record.responseHeaders.size})")
        record.responseContentType?.let {
            Text(text = "Content-Type: $it", style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.primary)
        }
        Spacer(modifier = Modifier.height(6.dp))

        if (record.responseHeaders.isEmpty()) {
            Text(text = "No response headers available", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.outline)
        } else {
            HeadersTable(headers = record.responseHeaders)
        }
    }
}

@Composable
fun DetailParametersTab(record: TrafficRecord) {
    val scrollState = rememberScrollState()

    Column(
        modifier = Modifier
            .fillMaxWidth()
            .verticalScroll(scrollState)
            .padding(bottom = 24.dp)
    ) {
        InfoSectionTitle("URL Query Parameters (${record.queryParams.size})")
        Spacer(modifier = Modifier.height(6.dp))
        if (record.queryParams.isEmpty()) {
            Text(text = "No URL query parameters detected", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.outline)
        } else {
            HeadersTable(headers = record.queryParams)
        }

        Spacer(modifier = Modifier.height(16.dp))

        InfoSectionTitle("Form Submission Parameters (${record.formParameters.size})")
        Spacer(modifier = Modifier.height(2.dp))
        Text(
            text = "Extracted from POST payload (${record.requestContentType ?: "N/A"})",
            style = MaterialTheme.typography.labelSmall,
            color = MaterialTheme.colorScheme.outline
        )
        Spacer(modifier = Modifier.height(6.dp))

        if (record.formParameters.isEmpty()) {
            Surface(
                color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.3f),
                shape = RoundedCornerShape(8.dp),
                modifier = Modifier.fillMaxWidth()
            ) {
                Text(
                    text = "No structured form fields detected in this request",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.outline,
                    modifier = Modifier.padding(10.dp)
                )
            }
        } else {
            HeadersTable(headers = record.formParameters)
        }
    }
}

@Composable
fun DetailBodyTab(record: TrafficRecord) {
    val context = LocalContext.current
    val clipboardManager = LocalClipboardManager.current
    val scrollState = rememberScrollState()

    Column(
        modifier = Modifier
            .fillMaxWidth()
            .verticalScroll(scrollState)
            .padding(bottom = 24.dp)
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            InfoSectionTitle("Request Body (${formatBytes(record.requestSize)})")
            if (!record.requestBody.isNullOrBlank()) {
                IconButton(
                    onClick = {
                        clipboardManager.setText(AnnotatedString(record.requestBody))
                        Toast.makeText(context, "Request body copied", Toast.LENGTH_SHORT).show()
                    },
                    modifier = Modifier.size(32.dp)
                ) {
                    Icon(imageVector = Icons.Default.ContentCopy, contentDescription = "Copy body", modifier = Modifier.size(16.dp))
                }
            }
        }

        Spacer(modifier = Modifier.height(6.dp))

        if (record.isTlsTunnel) {
            Surface(
                color = MaterialTheme.colorScheme.surfaceVariant,
                shape = RoundedCornerShape(8.dp),
                modifier = Modifier.fillMaxWidth()
            ) {
                Row(modifier = Modifier.padding(12.dp), verticalAlignment = Alignment.CenterVertically) {
                    Icon(imageVector = Icons.Default.Lock, contentDescription = null, tint = MaterialTheme.colorScheme.tertiary, modifier = Modifier.size(20.dp))
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(
                        text = "Encrypted TLS Tunnel Payload: End-to-end encrypted directly with remote server. Packet metadata and handshake recorded.",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }
        } else if (record.requestBody.isNullOrBlank()) {
            Surface(
                color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.3f),
                shape = RoundedCornerShape(8.dp),
                modifier = Modifier.fillMaxWidth()
            ) {
                Text(
                    text = "(Empty request body)",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.outline,
                    modifier = Modifier.padding(10.dp)
                )
            }
        } else {
            CodeViewer(content = record.requestBody)
        }
    }
}

@Composable
fun DetailResponseTab(record: TrafficRecord) {
    val context = LocalContext.current
    val clipboardManager = LocalClipboardManager.current
    val scrollState = rememberScrollState()

    Column(
        modifier = Modifier
            .fillMaxWidth()
            .verticalScroll(scrollState)
            .padding(bottom = 24.dp)
    ) {
        InfoSectionTitle("Status & Protocol")
        Surface(
            color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f),
            shape = RoundedCornerShape(8.dp),
            modifier = Modifier.fillMaxWidth()
        ) {
            Text(
                text = "${record.protocol} ${record.responseCode} ${record.responseMessage}".trim(),
                style = MaterialTheme.typography.bodyMedium.copy(
                    fontFamily = FontFamily.Monospace,
                    fontWeight = FontWeight.SemiBold
                ),
                color = MaterialTheme.colorScheme.onSurface,
                modifier = Modifier.padding(10.dp)
            )
        }

        Spacer(modifier = Modifier.height(14.dp))

        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            InfoSectionTitle("Response Body (${formatBytes(record.responseSize)})")
            if (!record.responseBody.isNullOrBlank()) {
                IconButton(
                    onClick = {
                        clipboardManager.setText(AnnotatedString(record.responseBody))
                        Toast.makeText(context, "Response body copied", Toast.LENGTH_SHORT).show()
                    },
                    modifier = Modifier.size(32.dp)
                ) {
                    Icon(imageVector = Icons.Default.ContentCopy, contentDescription = "Copy body", modifier = Modifier.size(16.dp))
                }
            }
        }

        Spacer(modifier = Modifier.height(6.dp))

        if (record.responseBody.isNullOrBlank()) {
            Surface(
                color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.3f),
                shape = RoundedCornerShape(8.dp),
                modifier = Modifier.fillMaxWidth()
            ) {
                Text(
                    text = "(Empty response body or pending)",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.outline,
                    modifier = Modifier.padding(10.dp)
                )
            }
        } else {
            CodeViewer(content = record.responseBody)
        }
    }
}

@Composable
fun HeadersTable(headers: Map<String, String>) {
    Surface(
        color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.35f),
        shape = RoundedCornerShape(8.dp),
        modifier = Modifier.fillMaxWidth()
    ) {
        Column(modifier = Modifier.padding(8.dp)) {
            headers.entries.forEachIndexed { index, (k, v) ->
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(vertical = 4.dp),
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Text(
                        text = k,
                        style = MaterialTheme.typography.labelMedium.copy(fontWeight = FontWeight.SemiBold),
                        color = MaterialTheme.colorScheme.primary,
                        modifier = Modifier.weight(0.4f)
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(
                        text = v,
                        style = MaterialTheme.typography.bodySmall.copy(fontFamily = FontFamily.Monospace),
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        modifier = Modifier.weight(0.6f)
                    )
                }
                if (index < headers.size - 1) {
                    Divider(color = MaterialTheme.colorScheme.outline.copy(alpha = 0.15f))
                }
            }
        }
    }
}

@Composable
fun CodeViewer(content: String) {
    val formattedContent = remember(content) {
        formatIfJson(content)
    }

    val horizontalScroll = rememberScrollState()

    Surface(
        color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.6f),
        shape = RoundedCornerShape(8.dp),
        modifier = Modifier.fillMaxWidth()
    ) {
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .horizontalScroll(horizontalScroll)
                .padding(10.dp)
        ) {
            Text(
                text = formattedContent,
                style = MaterialTheme.typography.bodySmall.copy(
                    fontFamily = FontFamily.Monospace,
                    fontSize = 11.sp,
                    lineHeight = 16.sp
                ),
                color = MaterialTheme.colorScheme.onSurface
            )
        }
    }
}

fun formatIfJson(str: String): String {
    val trimmed = str.trim()
    return try {
        if (trimmed.startsWith("{") && trimmed.endsWith("}")) {
            JSONObject(trimmed).toString(2)
        } else if (trimmed.startsWith("[") && trimmed.endsWith("]")) {
            JSONArray(trimmed).toString(2)
        } else {
            str
        }
    } catch (_: Exception) {
        str
    }
}

@Composable
fun MetricRow(label: String, value: String) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 3.dp),
        horizontalArrangement = Arrangement.SpaceBetween
    ) {
        Text(
            text = label,
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
        Text(
            text = value,
            style = MaterialTheme.typography.bodySmall.copy(
                fontWeight = FontWeight.Medium,
                fontFamily = FontFamily.Monospace
            ),
            color = MaterialTheme.colorScheme.onSurface
        )
    }
    Divider(color = MaterialTheme.colorScheme.outline.copy(alpha = 0.15f))
}

@Composable
fun InfoSectionTitle(title: String) {
    Text(
        text = title,
        style = MaterialTheme.typography.labelLarge,
        fontWeight = FontWeight.Bold,
        color = MaterialTheme.colorScheme.primary
    )
}
