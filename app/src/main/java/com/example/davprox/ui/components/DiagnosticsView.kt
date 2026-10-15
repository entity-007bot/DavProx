package com.example.davprox.ui.components

import androidx.compose.foundation.background
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
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.Info
import androidx.compose.material.icons.filled.Lan
import androidx.compose.material.icons.filled.Lock
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.Router
import androidx.compose.material.icons.filled.Security
import androidx.compose.material.icons.filled.Sensors
import androidx.compose.material.icons.filled.Stop
import androidx.compose.material.icons.filled.Warning
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Divider
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp

@Composable
fun DiagnosticsView(
    isRunning: Boolean,
    port: Int,
    localIp: String,
    activeConnections: Int,
    totalObserved: Int,
    httpsCount: Int,
    parsedHttpCount: Int,
    failuresCount: Int,
    lastError: String?,
    onToggleProxy: () -> Unit,
    onRunDiagnosticTest: () -> Unit,
    modifier: Modifier = Modifier
) {
    Column(
        modifier = modifier
            .fillMaxSize()
            .padding(horizontal = 16.dp)
            .verticalScroll(rememberScrollState())
    ) {
        // Engine Status Card
        Card(
            colors = CardDefaults.cardColors(
                containerColor = if (isRunning) Color(0xFF2E7D32).copy(alpha = 0.12f) else MaterialTheme.colorScheme.surfaceVariant
            ),
            shape = RoundedCornerShape(12.dp),
            modifier = Modifier.fillMaxWidth()
        ) {
            Column(modifier = Modifier.padding(14.dp)) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Surface(
                            shape = CircleShape,
                            color = if (isRunning) Color(0xFF2E7D32) else MaterialTheme.colorScheme.outline,
                            modifier = Modifier.size(10.dp)
                        ) {}
                        Spacer(modifier = Modifier.width(8.dp))
                        Text(
                            text = if (isRunning) "ENGINE RUNNING" else "ENGINE STOPPED",
                            style = MaterialTheme.typography.titleSmall,
                            fontWeight = FontWeight.Bold,
                            color = if (isRunning) Color(0xFF2E7D32) else MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }

                    Button(
                        onClick = onToggleProxy,
                        modifier = Modifier.height(34.dp).testTag("diag_toggle_btn")
                    ) {
                        Icon(
                            imageVector = if (isRunning) Icons.Default.Stop else Icons.Default.PlayArrow,
                            contentDescription = null,
                            modifier = Modifier.size(14.dp)
                        )
                        Spacer(modifier = Modifier.width(4.dp))
                        Text(if (isRunning) "Stop" else "Start", fontSize = 11.sp)
                    }
                }

                Spacer(modifier = Modifier.height(8.dp))

                Text(
                    text = "Bound Interface: 0.0.0.0:$port (All IPv4 interfaces)",
                    style = MaterialTheme.typography.bodySmall.copy(fontFamily = FontFamily.Monospace)
                )
                Text(
                    text = "Device Wi-Fi / Local IP: $localIp",
                    style = MaterialTheme.typography.bodySmall.copy(fontFamily = FontFamily.Monospace, fontWeight = FontWeight.SemiBold),
                    color = MaterialTheme.colorScheme.primary
                )
                Text(
                    text = "Active Live Sockets: $activeConnections",
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.outline
                )

                if (lastError != null) {
                    Spacer(modifier = Modifier.height(6.dp))
                    Text(text = "Error: $lastError", style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.error)
                }
            }
        }

        Spacer(modifier = Modifier.height(16.dp))

        // Metrics Grid
        Text(
            text = "Telemetry & Packet Counters",
            style = MaterialTheme.typography.labelLarge,
            fontWeight = FontWeight.Bold
        )
        Spacer(modifier = Modifier.height(8.dp))

        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            MetricCard(
                title = "Total Observed",
                value = "$totalObserved",
                icon = Icons.Default.Sensors,
                color = MaterialTheme.colorScheme.primary,
                modifier = Modifier.weight(1f)
            )
            MetricCard(
                title = "Parsed HTTP",
                value = "$parsedHttpCount",
                icon = Icons.Default.Lan,
                color = Color(0xFF2E7D32),
                modifier = Modifier.weight(1f)
            )
        }

        Spacer(modifier = Modifier.height(8.dp))

        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            MetricCard(
                title = "TLS Tunnels",
                value = "$httpsCount",
                icon = Icons.Default.Lock,
                color = Color(0xFF8E24AA),
                modifier = Modifier.weight(1f)
            )
            MetricCard(
                title = "Errors / Failed",
                value = "$failuresCount",
                icon = Icons.Default.Warning,
                color = MaterialTheme.colorScheme.error,
                modifier = Modifier.weight(1f)
            )
        }

        Spacer(modifier = Modifier.height(16.dp))

        // Quick Verification Test Action
        OutlinedButton(
            onClick = onRunDiagnosticTest,
            modifier = Modifier.fillMaxWidth().testTag("run_diag_test_btn")
        ) {
            Icon(imageVector = Icons.Default.CheckCircle, contentDescription = null, modifier = Modifier.size(16.dp))
            Spacer(modifier = Modifier.width(6.dp))
            Text("Send Diagnostic Verification Packet (Loopback)")
        }

        Spacer(modifier = Modifier.height(16.dp))

        // Technical Guide for Chrome & Android Wi-Fi
        Text(
            text = "Android 15 & Chrome Routing Guidance",
            style = MaterialTheme.typography.labelLarge,
            fontWeight = FontWeight.Bold
        )
        Spacer(modifier = Modifier.height(6.dp))

        Card(
            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.4f)),
            shape = RoundedCornerShape(10.dp),
            modifier = Modifier.fillMaxWidth()
        ) {
            Column(modifier = Modifier.padding(12.dp)) {
                GuideItem(
                    title = "1. Chrome on the Same Android Phone",
                    body = "Android OS prohibits apps from proxying into 127.0.0.1 loopback via Wi-Fi settings. In your Wi-Fi settings, configure Proxy Hostname to your phone's actual LAN IP ($localIp) instead of localhost. Port: $port."
                )
                Divider(modifier = Modifier.padding(vertical = 8.dp))
                GuideItem(
                    title = "2. HTTP vs HTTPS Decryption",
                    body = "Standard HTTP traffic, redirects, and forms are parsed in full (headers, URL parameters, payloads). HTTPS connections use CONNECT tunneling; content is encrypted end-to-end unless the target test website/app opts into user certificates in network_security_config.xml."
                )
                Divider(modifier = Modifier.padding(vertical = 8.dp))
                GuideItem(
                    title = "3. Form Submission Interception",
                    body = "When submitting HTML forms on test sites, DavProx identifies Content-Type (application/x-www-form-urlencoded or JSON) and extracts individual field keys and values into the Parameters tab."
                )
            }
        }

        Spacer(modifier = Modifier.height(40.dp))
    }
}

@Composable
fun MetricCard(
    title: String,
    value: String,
    icon: ImageVector,
    color: Color,
    modifier: Modifier = Modifier
) {
    Card(
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f)),
        shape = RoundedCornerShape(10.dp),
        modifier = modifier
    ) {
        Column(modifier = Modifier.padding(12.dp)) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween,
                modifier = Modifier.fillMaxWidth()
            ) {
                Text(text = title, style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                Icon(imageVector = icon, contentDescription = null, tint = color, modifier = Modifier.size(16.dp))
            }
            Spacer(modifier = Modifier.height(6.dp))
            Text(text = value, style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold, fontFamily = FontFamily.Monospace), color = color)
        }
    }
}

@Composable
fun GuideItem(title: String, body: String) {
    Column {
        Text(text = title, style = MaterialTheme.typography.labelMedium, fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.primary)
        Spacer(modifier = Modifier.height(2.dp))
        Text(text = body, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
    }
}
