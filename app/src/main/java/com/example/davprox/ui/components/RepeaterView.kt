package com.example.davprox.ui.components

import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.CompareArrows
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.Replay
import androidx.compose.material.icons.filled.Security
import androidx.compose.material.icons.filled.Warning
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Divider
import androidx.compose.material3.FilterChip
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.davprox.model.TrafficRecord

@Composable
fun RepeaterView(
    originalRecord: TrafficRecord?,
    replayedRecord: TrafficRecord?,
    isLoading: Boolean,
    onExecuteReplay: (url: String, method: String, headers: Map<String, String>, body: String?) -> Unit,
    modifier: Modifier = Modifier
) {
    if (originalRecord == null) {
        Box(
            modifier = modifier
                .fillMaxSize()
                .padding(32.dp),
            contentAlignment = Alignment.Center
        ) {
            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                Icon(
                    imageVector = Icons.Default.Replay,
                    contentDescription = null,
                    modifier = Modifier.size(56.dp),
                    tint = MaterialTheme.colorScheme.outline
                )
                Spacer(modifier = Modifier.height(12.dp))
                Text(
                    text = "No Request Selected for Repeater",
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold
                )
                Spacer(modifier = Modifier.height(6.dp))
                Text(
                    text = "Select any captured request from Live Traffic or Endpoints and tap 'Open in Repeater' to modify and replay it.",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.outline
                )
            }
        }
        return
    }

    var method by remember(originalRecord.id) { mutableStateOf(originalRecord.method) }
    var url by remember(originalRecord.id) { mutableStateOf(originalRecord.url) }
    var headersText by remember(originalRecord.id) {
        val lines = originalRecord.requestHeaders.map { "${it.key}: ${it.value}" }.joinToString("\n")
        mutableStateOf(lines)
    }
    var bodyText by remember(originalRecord.id) { mutableStateOf(originalRecord.requestBody ?: "") }

    val methods = listOf("GET", "POST", "PUT", "DELETE", "PATCH")

    Column(
        modifier = modifier
            .fillMaxSize()
            .padding(horizontal = 16.dp)
            .verticalScroll(rememberScrollState())
    ) {
        // Warning Banner
        Surface(
            color = MaterialTheme.colorScheme.tertiaryContainer.copy(alpha = 0.4f),
            shape = RoundedCornerShape(8.dp),
            modifier = Modifier.fillMaxWidth()
        ) {
            Row(
                modifier = Modifier.padding(10.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Icon(
                    imageVector = Icons.Default.Security,
                    contentDescription = null,
                    tint = MaterialTheme.colorScheme.tertiary,
                    modifier = Modifier.size(18.dp)
                )
                Spacer(modifier = Modifier.width(8.dp))
                Text(
                    text = "Repeater Mode: Deliberately replay requests against authorized test systems. Respect scope and do not replay unauthorized session tokens.",
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.onTertiaryContainer
                )
            }
        }

        Spacer(modifier = Modifier.height(12.dp))

        // Method Selector
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .horizontalScroll(rememberScrollState()),
            horizontalArrangement = Arrangement.spacedBy(6.dp)
        ) {
            methods.forEach { m ->
                FilterChip(
                    selected = method == m,
                    onClick = { method = m },
                    label = { Text(m, fontWeight = FontWeight.Bold, fontSize = 11.sp) }
                )
            }
        }

        Spacer(modifier = Modifier.height(8.dp))

        // Target URL Input
        OutlinedTextField(
            value = url,
            onValueChange = { url = it },
            label = { Text("Request URL") },
            modifier = Modifier
                .fillMaxWidth()
                .testTag("repeater_url_input"),
            textStyle = MaterialTheme.typography.bodySmall.copy(fontFamily = FontFamily.Monospace),
            singleLine = true
        )

        Spacer(modifier = Modifier.height(8.dp))

        // Headers Input
        OutlinedTextField(
            value = headersText,
            onValueChange = { headersText = it },
            label = { Text("Request Headers (Name: Value)") },
            modifier = Modifier
                .fillMaxWidth()
                .height(120.dp)
                .testTag("repeater_headers_input"),
            textStyle = MaterialTheme.typography.bodySmall.copy(fontFamily = FontFamily.Monospace, fontSize = 11.sp)
        )

        Spacer(modifier = Modifier.height(8.dp))

        // Body Input
        OutlinedTextField(
            value = bodyText,
            onValueChange = { bodyText = it },
            label = { Text("Request Body / Payload") },
            modifier = Modifier
                .fillMaxWidth()
                .height(130.dp)
                .testTag("repeater_body_input"),
            textStyle = MaterialTheme.typography.bodySmall.copy(fontFamily = FontFamily.Monospace, fontSize = 11.sp)
        )

        Spacer(modifier = Modifier.height(12.dp))

        Button(
            onClick = {
                val parsedHeaders = mutableMapOf<String, String>()
                headersText.split("\n").forEach { line ->
                    val idx = line.indexOf(":")
                    if (idx > 0) {
                        parsedHeaders[line.substring(0, idx).trim()] = line.substring(idx + 1).trim()
                    }
                }
                onExecuteReplay(url.trim(), method, parsedHeaders, if (bodyText.isNotBlank()) bodyText else null)
            },
            enabled = !isLoading && url.isNotBlank(),
            modifier = Modifier
                .fillMaxWidth()
                .testTag("execute_replay_btn")
        ) {
            if (isLoading) {
                CircularProgressIndicator(modifier = Modifier.size(18.dp), color = MaterialTheme.colorScheme.onPrimary)
                Spacer(modifier = Modifier.width(8.dp))
                Text("Replaying...")
            } else {
                Icon(imageVector = Icons.Default.PlayArrow, contentDescription = null, modifier = Modifier.size(18.dp))
                Spacer(modifier = Modifier.width(8.dp))
                Text("Send Replayed Request")
            }
        }

        // Comparison View if replayed
        if (replayedRecord != null) {
            Spacer(modifier = Modifier.height(18.dp))
            Row(verticalAlignment = Alignment.CenterVertically) {
                Icon(imageVector = Icons.Default.CompareArrows, contentDescription = null, tint = MaterialTheme.colorScheme.primary)
                Spacer(modifier = Modifier.width(6.dp))
                Text(
                    text = "Original vs Replay Comparison",
                    style = MaterialTheme.typography.titleSmall,
                    fontWeight = FontWeight.Bold
                )
            }
            Spacer(modifier = Modifier.height(8.dp))

            ComparisonSummaryCard(original = originalRecord, replayed = replayedRecord)
        }

        Spacer(modifier = Modifier.height(32.dp))
    }
}

@Composable
fun ComparisonSummaryCard(
    original: TrafficRecord,
    replayed: TrafficRecord
) {
    Card(
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f)),
        shape = RoundedCornerShape(12.dp),
        modifier = Modifier.fillMaxWidth()
    ) {
        Column(modifier = Modifier.padding(12.dp)) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Column(modifier = Modifier.weight(1f)) {
                    Text("ORIGINAL REQUEST", style = MaterialTheme.typography.labelSmall, fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.outline)
                    Text("HTTP ${original.responseCode} ${original.responseMessage}", style = MaterialTheme.typography.bodySmall, fontWeight = FontWeight.SemiBold)
                    Text("${original.durationMs}ms • ${formatBytes(original.responseSize)}", style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                }

                Column(modifier = Modifier.weight(1f)) {
                    Text("REPLAYED REQUEST", style = MaterialTheme.typography.labelSmall, fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.primary)
                    Text("HTTP ${replayed.responseCode} ${replayed.responseMessage}", style = MaterialTheme.typography.bodySmall, fontWeight = FontWeight.SemiBold)
                    Text("${replayed.durationMs}ms • ${formatBytes(replayed.responseSize)}", style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                }
            }

            Spacer(modifier = Modifier.height(10.dp))
            Divider()
            Spacer(modifier = Modifier.height(8.dp))

            Text("Replayed Response Preview:", style = MaterialTheme.typography.labelSmall, fontWeight = FontWeight.Bold)
            Spacer(modifier = Modifier.height(4.dp))
            Surface(
                color = MaterialTheme.colorScheme.surfaceVariant,
                shape = RoundedCornerShape(6.dp),
                modifier = Modifier.fillMaxWidth()
            ) {
                Text(
                    text = replayed.responseBody?.take(500) ?: "(Empty response)",
                    style = MaterialTheme.typography.bodySmall.copy(fontFamily = FontFamily.Monospace, fontSize = 10.sp),
                    modifier = Modifier.padding(8.dp)
                )
            }
        }
    }
}
