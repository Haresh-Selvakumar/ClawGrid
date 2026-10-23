package com.example.ui.screens

import android.content.ClipData
import android.content.ClipboardManager
import android.content.Context
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.CloudDownload
import androidx.compose.material.icons.filled.CloudUpload
import androidx.compose.material.icons.filled.ContentCopy
import androidx.compose.material.icons.filled.DeleteOutline
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material.icons.filled.Save
import androidx.compose.material.icons.filled.WifiTethering
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Surface
import androidx.compose.material3.Switch
import androidx.compose.material3.SwitchDefaults
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.data.model.AuditEventEntity
import com.example.data.model.PolicyConfig
import com.example.data.remote.SupabaseClient
import com.example.ui.ClawGridUiState
import com.example.ui.components.PillTone
import com.example.ui.components.StatusPillBadge
import com.example.ui.theme.ClawAmber
import com.example.ui.theme.ClawAmberLight
import com.example.ui.theme.ClawBgCard
import com.example.ui.theme.ClawBgControl
import com.example.ui.theme.ClawBlue
import com.example.ui.theme.ClawBlueDark
import com.example.ui.theme.ClawBorder
import com.example.ui.theme.ClawCyan
import com.example.ui.theme.ClawEmerald
import com.example.ui.theme.ClawEmeraldLight
import com.example.ui.theme.ClawPurple
import com.example.ui.theme.ClawSky
import com.example.ui.theme.ClawTextDim
import com.example.ui.theme.ClawTextMain
import com.example.ui.theme.ClawTextMuted
import com.example.ui.theme.JetBrainsMonoFontFamily

@OptIn(ExperimentalLayoutApi::class)
@Composable
fun SafetyRtosAndCloudScreen(
    uiState: ClawGridUiState,
    allEvents: List<AuditEventEntity>,
    onSyncSupabasePush: () -> Unit,
    onSyncSupabasePull: () -> Unit,
    onTriggerLiveBroadcast: () -> Unit,
    onReconnectWss: () -> Unit,
    onToggleAutoSync: () -> Unit,
    onSimulateWdtStall: () -> Unit,
    onSimulateRadioLeaseExpiry: () -> Unit,
    onSimulatePiFailover: () -> Unit,
    onRestoreNominalRtos: () -> Unit,
    onSavePolicy: (PolicyConfig) -> Unit,
    onUpdateLogFilters: (String, String, String, String) -> Unit,
    onClearLogs: () -> Unit,
    onShowToast: (String, String, String) -> Unit
) {
    val context = LocalContext.current
    val sb = uiState.supabaseStatus
    val rtos = uiState.rtosHealth
    val isLive = sb.wsConnected || sb.isConnected

    Column(
        modifier = Modifier.fillMaxWidth(),
        verticalArrangement = Arrangement.spacedBy(14.dp)
    ) {
        // 1. SUPABASE REALTIME WEBSOCKET & CLOUD SYNC CONSOLE
        Surface(
            color = ClawBgCard,
            shape = RoundedCornerShape(14.dp),
            border = BorderStroke(
                1.dp,
                if (isLive) ClawEmerald.copy(alpha = 0.55f) else ClawBorder
            ),
            modifier = Modifier.fillMaxWidth()
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(16.dp),
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.Top
                ) {
                    Column(modifier = Modifier.weight(1f)) {
                        Text(
                            text = "☁️ Supabase Realtime WebSocket & Cloud Console",
                            style = MaterialTheme.typography.titleMedium,
                            color = ClawTextMain,
                            fontWeight = FontWeight.Bold
                        )
                        Text(
                            text = "WSS: wss://${sb.projectRef}.supabase.co/realtime/v1/websocket",
                            fontFamily = JetBrainsMonoFontFamily,
                            fontSize = 10.5.sp,
                            color = ClawSky
                        )
                    }
                    StatusPillBadge(
                        text = when {
                            sb.wsChannelJoined -> "WSS LIVE (${sb.wsRttMs}ms)"
                            sb.isConnected -> "CLOUD ONLINE"
                            else -> "CONNECTING…"
                        },
                        tone = if (isLive) PillTone.EMERALD else PillTone.CYAN
                    )
                }

                // Live Network Telemetry & Frame Inspector
                Surface(
                    color = ClawBgControl,
                    shape = RoundedCornerShape(10.dp),
                    border = BorderStroke(1.dp, ClawBorder),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Column(
                        modifier = Modifier.padding(12.dp),
                        verticalArrangement = Arrangement.spacedBy(6.dp)
                    ) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text(
                                text = "Channel: realtime:clawgrid_live",
                                fontFamily = JetBrainsMonoFontFamily,
                                fontWeight = FontWeight.Bold,
                                fontSize = 11.sp,
                                color = ClawEmeraldLight
                            )
                            Text(
                                text = "TX: ${sb.txPackets} pkts · RX: ${sb.rxPackets} pkts · RTT: ${sb.wsRttMs}ms",
                                fontFamily = JetBrainsMonoFontFamily,
                                fontSize = 10.5.sp,
                                color = ClawSky
                            )
                        }
                        Text(
                            text = sb.statusMessage,
                            style = MaterialTheme.typography.bodySmall,
                            color = ClawTextMain
                        )
                        Text(
                            text = "Latest Frame: ${sb.lastNetworkEvent}",
                            fontFamily = JetBrainsMonoFontFamily,
                            fontSize = 10.5.sp,
                            color = ClawTextMuted
                        )
                    }
                }

                // Auto-Stream Toggle Row
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Column(modifier = Modifier.weight(1f)) {
                        Text(
                            text = "Real-Time Bidirectional Cloud Streaming",
                            style = MaterialTheme.typography.titleSmall,
                            color = ClawTextMain,
                            fontWeight = FontWeight.SemiBold
                        )
                        Text(
                            text = "Automatically stream every relay toggle, scene, and sensor reading to Supabase",
                            style = MaterialTheme.typography.bodySmall,
                            color = ClawTextDim,
                            fontSize = 11.sp
                        )
                    }
                    Switch(
                        checked = sb.autoRealtimeSync,
                        onCheckedChange = { onToggleAutoSync() },
                        colors = SwitchDefaults.colors(
                            checkedThumbColor = Color.White,
                            checkedTrackColor = ClawEmerald
                        )
                    )
                }

                // Cloud & Realtime Action Buttons
                FlowRow(
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                    verticalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    Button(
                        onClick = onTriggerLiveBroadcast,
                        shape = RoundedCornerShape(8.dp),
                        colors = ButtonDefaults.buttonColors(
                            containerColor = ClawEmerald,
                            contentColor = Color.White
                        ),
                        modifier = Modifier.testTag("supabase_broadcast_test_button")
                    ) {
                        Icon(Icons.Default.WifiTethering, contentDescription = null, modifier = Modifier.size(15.dp))
                        Spacer(modifier = Modifier.width(6.dp))
                        Text("Test Live WSS Broadcast", style = MaterialTheme.typography.labelLarge)
                    }

                    Button(
                        onClick = onSyncSupabasePush,
                        shape = RoundedCornerShape(8.dp),
                        colors = ButtonDefaults.buttonColors(
                            containerColor = ClawBlueDark,
                            contentColor = Color.White
                        ),
                        modifier = Modifier.testTag("supabase_push_button")
                    ) {
                        Icon(Icons.Default.CloudUpload, contentDescription = null, modifier = Modifier.size(15.dp))
                        Spacer(modifier = Modifier.width(6.dp))
                        Text("Push Snapshot Now", style = MaterialTheme.typography.labelLarge)
                    }

                    OutlinedButton(
                        onClick = onSyncSupabasePull,
                        shape = RoundedCornerShape(8.dp),
                        border = BorderStroke(1.dp, ClawBorder),
                        colors = ButtonDefaults.outlinedButtonColors(
                            containerColor = ClawBgControl,
                            contentColor = ClawTextMain
                        ),
                        modifier = Modifier.testTag("supabase_pull_button")
                    ) {
                        Icon(Icons.Default.CloudDownload, contentDescription = null, tint = ClawSky, modifier = Modifier.size(15.dp))
                        Spacer(modifier = Modifier.width(6.dp))
                        Text("Pull Cloud State", style = MaterialTheme.typography.labelLarge)
                    }

                    OutlinedButton(
                        onClick = onReconnectWss,
                        shape = RoundedCornerShape(8.dp),
                        border = BorderStroke(1.dp, ClawBorder),
                        colors = ButtonDefaults.outlinedButtonColors(
                            containerColor = ClawBgControl,
                            contentColor = ClawTextMuted
                        )
                    ) {
                        Icon(Icons.Default.Refresh, contentDescription = null, tint = ClawCyan, modifier = Modifier.size(15.dp))
                        Spacer(modifier = Modifier.width(6.dp))
                        Text("Reconnect WSS", style = MaterialTheme.typography.labelLarge)
                    }

                    OutlinedButton(
                        onClick = {
                            val cm = context.getSystemService(Context.CLIPBOARD_SERVICE) as? ClipboardManager
                            cm?.setPrimaryClip(ClipData.newPlainText("ClawGrid Supabase SQL", SupabaseClient.RECOMMENDED_SUPABASE_SQL))
                            onShowToast("SQL Schema Copied", "Copied clawgrid_rooms & clawgrid_audit_logs DDL to clipboard.", "SUCCESS")
                        },
                        shape = RoundedCornerShape(8.dp),
                        border = BorderStroke(1.dp, ClawBorder),
                        colors = ButtonDefaults.outlinedButtonColors(
                            containerColor = ClawBgControl,
                            contentColor = ClawTextMuted
                        )
                    ) {
                        Icon(Icons.Default.ContentCopy, contentDescription = null, modifier = Modifier.size(14.dp))
                        Spacer(modifier = Modifier.width(6.dp))
                        Text("Copy Supabase SQL Schema", style = MaterialTheme.typography.labelLarge)
                    }
                }
            }
        }

        // 2. FREERTOS TASK & WATCHDOG TELEMETRY + INTERACTIVE FAULT LAB CARD
        Surface(
            color = ClawBgCard,
            shape = RoundedCornerShape(14.dp),
            border = BorderStroke(1.dp, ClawBorder),
            modifier = Modifier.fillMaxWidth()
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(16.dp),
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.Top
                ) {
                    Column(modifier = Modifier.weight(1f)) {
                        Text(
                            text = "FreeRTOS Task & Watchdog Telemetry",
                            style = MaterialTheme.typography.titleMedium,
                            color = ClawTextMain,
                            fontWeight = FontWeight.Bold
                        )
                        Text(
                            text = "Dual ESP32-S3 Task Monitoring (esp_task_wdt_init(5, true)) & Job Counters",
                            style = MaterialTheme.typography.bodySmall,
                            color = ClawTextMuted
                        )
                    }
                    StatusPillBadge(
                        text = rtos.wdtBadgeText,
                        tone = if (rtos.wdtNominal) PillTone.EMERALD else PillTone.RED
                    )
                }

                rtos.tasks.forEach { task ->
                    Surface(
                        color = ClawBgControl,
                        shape = RoundedCornerShape(10.dp),
                        border = BorderStroke(1.dp, ClawBorder),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(horizontal = 12.dp, vertical = 9.dp),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Column(modifier = Modifier.weight(1f)) {
                                Text(
                                    text = task.taskName,
                                    style = MaterialTheme.typography.titleSmall,
                                    color = ClawTextMain,
                                    fontWeight = FontWeight.Bold
                                )
                                Text(
                                    text = "${task.coreAndPrio} · Period: ${task.period} · Stack Free: ${task.minStackFree}",
                                    style = MaterialTheme.typography.bodySmall,
                                    color = ClawTextDim,
                                    fontSize = 11.sp
                                )
                            }
                            Column(horizontalAlignment = Alignment.End) {
                                Text(
                                    text = "%,d jobs".format(task.runtimeJobs),
                                    fontFamily = JetBrainsMonoFontFamily,
                                    fontSize = 11.5.sp,
                                    color = ClawSky
                                )
                                StatusPillBadge(
                                    text = task.wdtStatus,
                                    tone = if (task.wdtHealthy) PillTone.EMERALD else PillTone.RED
                                )
                            }
                        }
                    }
                }

                // Interactive Watchdog, Radio Lease & Raspberry Pi Failover Lab
                Surface(
                    color = ClawBgControl,
                    shape = RoundedCornerShape(10.dp),
                    border = BorderStroke(1.dp, ClawBorder),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Column(
                        modifier = Modifier.padding(12.dp),
                        verticalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text(
                                text = "⚡ Interactive Watchdog, Radio Lease & Pi Failover Lab",
                                style = MaterialTheme.typography.titleSmall,
                                color = ClawAmberLight,
                                fontWeight = FontWeight.Bold
                            )
                            Text(
                                text = if (uiState.mode == com.example.data.model.TelemetryMode.HARDWARE_NODE_A) uiState.hardwareState.sample?.let { "Board boot #${it.boot} · Sample uptime: ${it.uptime / 1000}s (last report)" } ?: "Board boot / uptime unavailable" else "Simulated boot #${rtos.nodeABootCount} · Uptime: ${rtos.nodeAUptimeSec}s",
                                fontFamily = JetBrainsMonoFontFamily,
                                fontSize = 11.sp,
                                color = ClawTextMuted
                            )
                        }
                        Text(
                            text = "Test embedded faults: stalling ControlTask triggers an ESP_RST_TASK_WDT panic/reboot after 5.0s (incrementing Boot ID and restoring NVS policy with relays SAFE OFF).",
                            style = MaterialTheme.typography.bodySmall,
                            color = ClawTextMuted
                        )
                        FlowRow(
                            horizontalArrangement = Arrangement.spacedBy(8.dp),
                            verticalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            OutlinedButton(
                                onClick = onSimulateWdtStall,
                                shape = RoundedCornerShape(8.dp),
                                border = BorderStroke(1.dp, ClawAmber),
                                modifier = Modifier.testTag("wdt_stall_button")
                            ) {
                                Text("💥 Simulate ControlTask Stall", color = ClawAmberLight, fontSize = 11.5.sp)
                            }
                            OutlinedButton(
                                onClick = onSimulateRadioLeaseExpiry,
                                shape = RoundedCornerShape(8.dp),
                                border = BorderStroke(1.dp, ClawBorder)
                            ) {
                                Text("📡 Simulate 3.0s Lease Expiry", color = ClawTextMain, fontSize = 11.5.sp)
                            }
                            OutlinedButton(
                                onClick = onSimulatePiFailover,
                                shape = RoundedCornerShape(8.dp),
                                border = BorderStroke(1.dp, ClawPurple)
                            ) {
                                Text("🍓 Trigger Pi Backup Takeover", color = ClawPurple, fontSize = 11.5.sp)
                            }
                            OutlinedButton(
                                onClick = onRestoreNominalRtos,
                                shape = RoundedCornerShape(8.dp),
                                border = BorderStroke(1.dp, ClawEmerald),
                                modifier = Modifier.testTag("wdt_restore_button")
                            ) {
                                Text("✅ Restore Nominal State", color = ClawEmeraldLight, fontSize = 11.5.sp)
                            }
                        }
                    }
                }
            }
        }

        // 3. POLICY & THRESHOLD CONFIGURATION CARD
        PolicyConfigurationCard(
            policy = uiState.policy,
            hardwareMode = uiState.mode == com.example.data.model.TelemetryMode.HARDWARE_NODE_A,
            onSavePolicy = onSavePolicy
        )

        // 4. FULL AUDIT & EVENT LOGS CARD
        FullAuditLogsCard(
            uiState = uiState,
            allEvents = allEvents,
            onUpdateLogFilters = onUpdateLogFilters,
            onClearLogs = onClearLogs
        )
    }
}

@Composable
private fun PolicyConfigurationCard(
    policy: PolicyConfig,
    hardwareMode: Boolean,
    onSavePolicy: (PolicyConfig) -> Unit
) {
    var graceText by remember(policy) { mutableStateOf(policy.graceSeconds.toInt().toString()) }
    var dwellText by remember(policy) { mutableStateOf(policy.dwellSeconds.toInt().toString()) }
    var holdText by remember(policy) { mutableStateOf(policy.manualHoldMinutes.toInt().toString()) }
    var luxText by remember(policy) { mutableStateOf(policy.luxThreshold.toString()) }
    var tempText by remember(policy) { mutableStateOf(policy.tempThreshold.toString()) }
    var hwUrlText by remember(policy) { mutableStateOf(policy.hardwareUrl) }
    var hwTokenText by remember(policy) { mutableStateOf(policy.hardwareToken) }

    Surface(
        color = ClawBgCard,
        shape = RoundedCornerShape(14.dp),
        border = BorderStroke(1.dp, ClawBorder),
        modifier = Modifier.fillMaxWidth()
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = "ClawGrid Policy & Hardware Network Configuration",
                        style = MaterialTheme.typography.titleSmall,
                        color = ClawTextMain,
                        fontWeight = FontWeight.Bold
                    )
                    Text(
                        text = if (hardwareMode) "Only gateway URL/token apply here. Board policy unchanged: 5-minute manual hold; AUTO/resume and per-device automation require further integration. Temperature monitoring only." else "Simulation settings only; environmental thresholds do not change board policy.",
                        style = MaterialTheme.typography.bodySmall,
                        color = ClawTextMuted
                    )
                }
            }

            if (!hardwareMode) Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                ConfigInputBox(
                    label = "Vacancy Grace (s)",
                    value = graceText,
                    onValueChange = { graceText = it },
                    modifier = Modifier.weight(1f)
                )
                ConfigInputBox(
                    label = "Min Dwell (s)",
                    value = dwellText,
                    onValueChange = { dwellText = it },
                    modifier = Modifier.weight(1f)
                )
                ConfigInputBox(
                    label = "Manual Hold (m)",
                    value = holdText,
                    onValueChange = { holdText = it },
                    modifier = Modifier.weight(1f)
                )
            }

            if (!hardwareMode) Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                ConfigInputBox(
                    label = "Auto-Light Lux",
                    value = luxText,
                    onValueChange = { luxText = it },
                    modifier = Modifier.weight(1f)
                )
                ConfigInputBox(
                    label = "Auto-Fan Temp (°C)",
                    value = tempText,
                    onValueChange = { tempText = it },
                    modifier = Modifier.weight(1f)
                )
            }

            OutlinedTextField(
                value = hwTokenText, onValueChange = { hwTokenText = it },
                label = { Text("Existing gateway token (memory only)") },
                visualTransformation = androidx.compose.ui.text.input.PasswordVisualTransformation(),
                singleLine = true, modifier = Modifier.fillMaxWidth()
            )
            ConfigInputBox(
                label = "Gateway Wi-Fi IPv4 URL (ESP AP: http://192.168.4.1)",
                value = hwUrlText,
                onValueChange = { hwUrlText = it },
                isNumeric = false,
                modifier = Modifier.fillMaxWidth()
            )

            Row(
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Button(
                    onClick = {
                        onSavePolicy(
                            policy.copy(
                                graceSeconds = graceText.toDoubleOrNull() ?: 5.0,
                                dwellSeconds = dwellText.toDoubleOrNull() ?: 3.0,
                                manualHoldMinutes = holdText.toDoubleOrNull() ?: 5.0,
                                luxThreshold = luxText.toIntOrNull() ?: 300,
                                tempThreshold = tempText.toDoubleOrNull() ?: 28.0,
                                hardwareUrl = hwUrlText.ifBlank { "http://192.168.4.1" },
                                hardwareToken = hwTokenText
                            )
                        )
                    },
                    shape = RoundedCornerShape(8.dp),
                    colors = ButtonDefaults.buttonColors(
                        containerColor = ClawBlueDark,
                        contentColor = Color.White
                    ),
                    modifier = Modifier.testTag("save_policy_config_button")
                ) {
                    Icon(Icons.Default.Save, contentDescription = null, modifier = Modifier.size(15.dp))
                    Spacer(modifier = Modifier.width(6.dp))
                    Text(if (hardwareMode) "Apply Gateway Connection" else "Save Simulation Settings", style = MaterialTheme.typography.labelLarge)
                }

                OutlinedButton(
                    onClick = {
                        onSavePolicy(PolicyConfig())
                    },
                    shape = RoundedCornerShape(8.dp),
                    border = BorderStroke(1.dp, ClawBorder)
                ) {
                    Text("Factory Defaults", style = MaterialTheme.typography.labelLarge, color = ClawTextMuted)
                }
            }
        }
    }
}

@Composable
private fun ConfigInputBox(
    label: String,
    value: String,
    onValueChange: (String) -> Unit,
    isNumeric: Boolean = true,
    modifier: Modifier = Modifier
) {
    Column(modifier = modifier, verticalArrangement = Arrangement.spacedBy(4.dp)) {
        Text(text = label, style = MaterialTheme.typography.labelSmall, color = ClawTextDim)
        OutlinedTextField(
            value = value,
            onValueChange = onValueChange,
            singleLine = true,
            keyboardOptions = if (isNumeric) KeyboardOptions(keyboardType = KeyboardType.Decimal) else KeyboardOptions.Default,
            colors = OutlinedTextFieldDefaults.colors(
                focusedContainerColor = ClawBgControl,
                unfocusedContainerColor = ClawBgControl,
                focusedBorderColor = ClawBlue,
                unfocusedBorderColor = ClawBorder,
                focusedTextColor = ClawTextMain,
                unfocusedTextColor = ClawTextMain
            ),
            modifier = Modifier.fillMaxWidth()
        )
    }
}

@Composable
private fun FullAuditLogsCard(
    uiState: ClawGridUiState,
    allEvents: List<AuditEventEntity>,
    onUpdateLogFilters: (String, String, String, String) -> Unit,
    onClearLogs: () -> Unit
) {
    val filtered = allEvents.filter { evt ->
        val matchesSearch = uiState.logSearchQuery.isBlank() ||
            evt.description.contains(uiState.logSearchQuery, ignoreCase = true) ||
            evt.eventType.contains(uiState.logSearchQuery, ignoreCase = true) ||
            evt.room.contains(uiState.logSearchQuery, ignoreCase = true)
        val matchesSev = uiState.logSeverityFilter == "ALL" || evt.severity == uiState.logSeverityFilter
        matchesSearch && matchesSev
    }

    Surface(
        color = ClawBgCard,
        shape = RoundedCornerShape(14.dp),
        border = BorderStroke(1.dp, ClawBorder),
        modifier = Modifier.fillMaxWidth()
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
                verticalArrangement = Arrangement.spacedBy(10.dp)
            ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = "ClawGrid Event Audit Logs (${filtered.size})",
                        style = MaterialTheme.typography.titleSmall,
                        color = ClawTextMain,
                        fontWeight = FontWeight.Bold
                    )
                    Text(
                        text = "Persistent Room SQLite + Supabase Realtime Cloud audit trail",
                        style = MaterialTheme.typography.bodySmall,
                        color = ClawTextMuted
                    )
                }

                OutlinedButton(
                    onClick = onClearLogs,
                    shape = RoundedCornerShape(8.dp),
                    border = BorderStroke(1.dp, ClawBorder)
                ) {
                    Icon(Icons.Default.DeleteOutline, contentDescription = null, tint = ClawTextMuted, modifier = Modifier.size(15.dp))
                    Spacer(modifier = Modifier.width(4.dp))
                    Text("Clear", style = MaterialTheme.typography.labelLarge, color = ClawTextMuted)
                }
            }

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                OutlinedTextField(
                    value = uiState.logSearchQuery,
                    onValueChange = {
                        onUpdateLogFilters(it, uiState.logRoomFilter, uiState.logSeverityFilter, uiState.logSourceFilter)
                    },
                    placeholder = { Text("Filter audit events…", color = ClawTextDim, fontSize = 12.sp) },
                    singleLine = true,
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedContainerColor = ClawBgControl,
                        unfocusedContainerColor = ClawBgControl,
                        focusedBorderColor = ClawBlue,
                        unfocusedBorderColor = ClawBorder,
                        focusedTextColor = ClawTextMain,
                        unfocusedTextColor = ClawTextMain
                    ),
                    modifier = Modifier.weight(1f)
                )
            }

            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .horizontalScroll(rememberScrollState()),
                horizontalArrangement = Arrangement.spacedBy(6.dp)
            ) {
                listOf("ALL", "INFO", "WARN", "ALARM").forEach { sev ->
                    val active = uiState.logSeverityFilter == sev
                    Box(
                        modifier = Modifier
                            .clip(RoundedCornerShape(8.dp))
                            .background(if (active) ClawBlueDark else ClawBgControl)
                            .border(1.dp, if (active) ClawBlue else ClawBorder, RoundedCornerShape(8.dp))
                            .clickable {
                                onUpdateLogFilters(uiState.logSearchQuery, uiState.logRoomFilter, sev, uiState.logSourceFilter)
                            }
                            .padding(horizontal = 10.dp, vertical = 6.dp)
                    ) {
                        Text(
                            text = sev,
                            fontFamily = JetBrainsMonoFontFamily,
                            fontSize = 10.5.sp,
                            color = if (active) Color.White else ClawTextMuted
                        )
                    }
                }
            }

            filtered.take(25).forEach { evt ->
                Surface(
                    color = ClawBgControl,
                    shape = RoundedCornerShape(8.dp),
                    border = BorderStroke(1.dp, ClawBorder),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Column(
                        modifier = Modifier.padding(10.dp),
                        verticalArrangement = Arrangement.spacedBy(4.dp)
                    ) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Row(
                                horizontalArrangement = Arrangement.spacedBy(6.dp),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Text(
                                    text = evt.timestamp,
                                    fontFamily = JetBrainsMonoFontFamily,
                                    fontSize = 10.5.sp,
                                    color = ClawTextDim
                                )
                                Text(
                                    text = evt.room,
                                    style = MaterialTheme.typography.labelLarge,
                                    color = ClawSky
                                )
                                Text(
                                    text = evt.eventType,
                                    fontFamily = JetBrainsMonoFontFamily,
                                    fontSize = 10.5.sp,
                                    color = ClawTextMain
                                )
                            }
                            Row(horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                                StatusPillBadge(
                                    text = evt.source,
                                    tone = if (evt.source == "SUPABASE") PillTone.CYAN else PillTone.NEUTRAL
                                )
                                StatusPillBadge(
                                    text = evt.severity,
                                    tone = when (evt.severity) {
                                        "ALARM" -> PillTone.RED
                                        "WARN" -> PillTone.AMBER
                                        else -> PillTone.EMERALD
                                    }
                                )
                            }
                        }
                        Text(
                            text = evt.description,
                            style = MaterialTheme.typography.bodySmall,
                            color = ClawTextMuted
                        )
                    }
                }
            }
        }
    }
}
