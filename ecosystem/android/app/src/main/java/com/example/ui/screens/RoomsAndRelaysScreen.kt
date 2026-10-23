package com.example.ui.screens

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
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.AccessTime
import androidx.compose.material.icons.filled.Bedtime
import androidx.compose.material.icons.filled.CloudUpload
import androidx.compose.material.icons.filled.DirectionsWalk
import androidx.compose.material.icons.filled.Mic
import androidx.compose.material.icons.filled.PowerSettingsNew
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material.icons.filled.Send
import androidx.compose.material.icons.filled.Terminal
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Surface
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
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.data.model.ActuatorAction
import com.example.data.model.AuditEventEntity
import com.example.data.model.ClawGridTab
import com.example.data.model.OccupancyState
import com.example.data.model.RoomState
import com.example.data.model.TelemetryMode
import com.example.data.remote.HardwareState
import com.example.ui.ClawGridUiState
import com.example.ui.components.PillTone
import com.example.ui.components.SegmentedActuatorToggle
import com.example.ui.components.StatusPillBadge
import com.example.ui.theme.ClawAmberLight
import com.example.ui.theme.ClawBgCard
import com.example.ui.theme.ClawBgControl
import com.example.ui.theme.ClawBlue
import com.example.ui.theme.ClawBlueDark
import com.example.ui.theme.ClawBorder
import com.example.ui.theme.ClawCyan
import com.example.ui.theme.ClawEmeraldLight
import com.example.ui.theme.ClawPurple
import com.example.ui.theme.ClawRed
import com.example.ui.theme.ClawSky
import com.example.ui.theme.ClawTextDim
import com.example.ui.theme.ClawTextMain
import com.example.ui.theme.ClawTextMuted
import com.example.ui.theme.JetBrainsMonoFontFamily

@OptIn(ExperimentalLayoutApi::class)
@Composable
fun RoomsAndRelaysScreen(
    uiState: ClawGridUiState,
    recentEvents: List<AuditEventEntity>,
    showOverviewExtras: Boolean,
    onActuate: (String, String, ActuatorAction) -> Unit,
    onApplyScene: (String) -> Unit,
    onScheduleTimer: (String, String, Int) -> Unit,
    onClearTimers: () -> Unit,
    onVoiceInputChanged: (String) -> Unit,
    onRunVoiceCommand: (String?) -> Unit,
    onNavigateTab: (ClawGridTab) -> Unit,
    onSyncSupabase: () -> Unit
) {
    Column(
        modifier = Modifier.fillMaxWidth(),
        verticalArrangement = Arrangement.spacedBy(14.dp)
    ) {
        // Main Room Relays, Scenes & Countdown Timers Container Card matching screenshot
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
                verticalArrangement = Arrangement.spacedBy(14.dp)
            ) {
                // Header & Scene Quick Buttons
                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.Top
                    ) {
                        Column(modifier = Modifier.weight(1f)) {
                            Text(
                                text = "Room Relays, Scenes & Countdown Timers",
                                style = MaterialTheme.typography.titleMedium,
                                color = ClawTextMain,
                                fontWeight = FontWeight.Bold
                            )
                            Spacer(modifier = Modifier.height(2.dp))
                            Text(
                                text = if (uiState.mode == TelemetryMode.HARDWARE_NODE_A) "Bedroom light manual ON/OFF: proposal → confirmation → ACK + board readback. Firmware hold: 5 minutes. AUTO/fan/scenes/timers pending integration." else "SIMULATED · Manual ON/OFF locks a ${uiState.policy.manualHoldMinutes.toInt()}-minute hold; AUTO restores simulated policy control.",
                                style = MaterialTheme.typography.bodySmall,
                                color = ClawTextMuted
                            )
                        }
                    }

                    // Scene Action Buttons Row matching screenshot ("Resume All AUTO", "Emergency All Off", plus Sleep & Away)
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .horizontalScroll(rememberScrollState()),
                        horizontalArrangement = Arrangement.spacedBy(8.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        OutlinedButton(
                            onClick = { onApplyScene("HOME") },
                            enabled = uiState.mode == TelemetryMode.SIMULATION_4_ROOM,
                            shape = RoundedCornerShape(8.dp),
                            border = BorderStroke(1.dp, ClawBorder),
                            colors = ButtonDefaults.outlinedButtonColors(
                                containerColor = ClawBgControl,
                                contentColor = ClawTextMain
                            ),
                            modifier = Modifier.testTag("scene_resume_auto_button")
                        ) {
                            Icon(
                                imageVector = Icons.Default.Refresh,
                                contentDescription = null,
                                tint = ClawSky,
                                modifier = Modifier.size(15.dp)
                            )
                            Spacer(modifier = Modifier.width(6.dp))
                            Text("Resume All AUTO", style = MaterialTheme.typography.labelLarge)
                        }

                        Button(
                            onClick = { onApplyScene("ALL_OFF") },
                            enabled = uiState.mode == TelemetryMode.SIMULATION_4_ROOM,
                            shape = RoundedCornerShape(8.dp),
                            colors = ButtonDefaults.buttonColors(
                                containerColor = ClawRed,
                                contentColor = Color.White
                            ),
                            modifier = Modifier.testTag("scene_emergency_off_button")
                        ) {
                            Icon(
                                imageVector = Icons.Default.PowerSettingsNew,
                                contentDescription = null,
                                modifier = Modifier.size(15.dp)
                            )
                            Spacer(modifier = Modifier.width(6.dp))
                            Text("All Off (subject to dwell)", style = MaterialTheme.typography.labelLarge)
                        }

                        OutlinedButton(
                            onClick = { onApplyScene("SLEEP") },
                            enabled = uiState.mode == TelemetryMode.SIMULATION_4_ROOM,
                            shape = RoundedCornerShape(8.dp),
                            border = BorderStroke(1.dp, ClawBorder),
                            colors = ButtonDefaults.outlinedButtonColors(
                                containerColor = ClawBgControl,
                                contentColor = ClawTextMuted
                            ),
                            modifier = Modifier.testTag("scene_sleep_button")
                        ) {
                            Icon(
                                imageVector = Icons.Default.Bedtime,
                                contentDescription = null,
                                tint = ClawPurple,
                                modifier = Modifier.size(15.dp)
                            )
                            Spacer(modifier = Modifier.width(6.dp))
                            Text("Sleep Mode", style = MaterialTheme.typography.labelLarge)
                        }

                        OutlinedButton(
                            onClick = { onApplyScene("AWAY") },
                            enabled = uiState.mode == TelemetryMode.SIMULATION_4_ROOM,
                            shape = RoundedCornerShape(8.dp),
                            border = BorderStroke(1.dp, ClawBorder),
                            colors = ButtonDefaults.outlinedButtonColors(
                                containerColor = ClawBgControl,
                                contentColor = ClawTextMuted
                            ),
                            modifier = Modifier.testTag("scene_away_button")
                        ) {
                            Icon(
                                imageVector = Icons.Default.DirectionsWalk,
                                contentDescription = null,
                                tint = ClawAmberLight,
                                modifier = Modifier.size(15.dp)
                            )
                            Spacer(modifier = Modifier.width(6.dp))
                            Text("Away Mode", style = MaterialTheme.typography.labelLarge)
                        }
                    }
                }

                // 4 Room Cards (Bedroom, Living Room, Kitchen, Study)
                uiState.rooms.forEachIndexed { index, room ->
                    RoomRelayControlCard(
                        room = room,
                        roomIndex = index,
                        nowMs = uiState.nowMs,
                        luxThreshold = uiState.policy.luxThreshold,
                        tempThreshold = uiState.policy.tempThreshold,
                        hardwareMode = uiState.mode == TelemetryMode.HARDWARE_NODE_A,
                        hardwareState = uiState.hardwareState,
                        onActuate = onActuate
                    )
                }

                // Auto-Off Countdown Timer Scheduler Card matching screenshot
                if (uiState.mode == TelemetryMode.SIMULATION_4_ROOM) CountdownTimerSchedulerCard(
                    uiState = uiState,
                    onScheduleTimer = onScheduleTimer,
                    onClearTimers = onClearTimers
                )
            }
        }

        // Offline Edge Voice & Natural Language Command Parser Card
        VoiceCommandAssistantCard(
            uiState = uiState,
            onVoiceInputChanged = onVoiceInputChanged,
            onRunVoiceCommand = onRunVoiceCommand
        )

        // Embedded RTOS, Watchdog, Pi Backup & Live Event Stream (especially prominent on Overview)
        if (showOverviewExtras || recentEvents.isNotEmpty()) {
            EmbeddedRtosOverviewCard(
                uiState = uiState,
                onOpenRtosLab = { onNavigateTab(ClawGridTab.SAFETY_CLOUD) },
                onSyncSupabase = onSyncSupabase
            )

            RecentAuditStreamCard(
                events = recentEvents.take(6),
                onViewAllLogs = { onNavigateTab(ClawGridTab.SAFETY_CLOUD) }
            )
        }
    }
}

@Composable
private fun RoomRelayControlCard(
    room: RoomState,
    roomIndex: Int,
    nowMs: Long,
    luxThreshold: Int,
    tempThreshold: Double,
    hardwareMode: Boolean,
    hardwareState: HardwareState,
    onActuate: (String, String, ActuatorAction) -> Unit
) {
    val roomEmoji = when (room.id) {
        "bedroom" -> "🛏️"
        "living_room" -> "🛋️"
        "kitchen" -> "🍳"
        else -> "📚"
    }

    val badgeTone = when (room.state) {
        OccupancyState.OCCUPIED -> PillTone.EMERALD
        OccupancyState.LIKELY_OCCUPIED -> PillTone.AMBER
        OccupancyState.UNKNOWN -> PillTone.RED
        OccupancyState.VACANT -> PillTone.NEUTRAL
    }

    Surface(
        color = Color(0xFF0B111E),
        shape = RoundedCornerShape(12.dp),
        border = BorderStroke(
            1.dp,
            if (room.state == OccupancyState.OCCUPIED) ClawBlue.copy(alpha = 0.35f) else ClawBorder
        ),
        modifier = Modifier
            .fillMaxWidth()
            .testTag("room_card_${room.id}")
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(14.dp),
            verticalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            // Room Header Row: Emoji + Blue Title + Occupancy Pill Badge
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    Text(text = roomEmoji, fontSize = 15.sp)
                    Text(
                        text = room.name,
                        style = MaterialTheme.typography.titleMedium,
                        color = Color(0xFF60A5FA),
                        fontWeight = FontWeight.Bold
                    )
                }

                StatusPillBadge(
                    text = room.state.label,
                    tone = badgeTone
                )
            }

            // Sensor & Environment Metadata Line matching screenshot: "PIR_1 (Zone 1) · 25.0°C · 120 lx"
            Text(
                text = if (hardwareMode) "${room.zoneLabel} · Temperature unavailable · Lux unavailable" else "${room.zoneLabel} · ${"%.1f".format(room.tempC)}°C · ${room.lux} lx",
                style = MaterialTheme.typography.bodySmall,
                color = ClawTextDim,
                fontSize = 11.5.sp
            )

            if (hardwareMode) Text(
                text = if (room.id == "bedroom") "${hardwareState.message}\nRequested: ${hardwareState.request?.let { "Light ${if (it.on) "ON" else "OFF"} · ${it.phase} · ${if (it.id > 0) "request #${it.id}" else "request ID not assigned"}" } ?: "none"}\nOutputs: ${hardwareState.sample?.takeIf { hardwareState.connected }?.outputsEnabled?.let { if (it) "enabled" else "inhibited" } ?: "unknown"}. Physical load not measured."
                    else "Unprovisioned in this milestone — board state UNKNOWN",
                color = ClawAmberLight, style = MaterialTheme.typography.bodySmall
            )

            // Actuator 1: Ceiling Light
            ActuatorControlRow(
                iconEmoji = "💡",
                title = "Ceiling Light",
                metaStatus = if (hardwareMode) "Board-reported light: ${if (room.reportedKnown) { if (room.lightOn) "ON" else "OFF" } else "UNKNOWN"}" else room.lightModeStatus(nowMs, luxThreshold),
                known = !hardwareMode || room.reportedKnown,
                enabled = !hardwareMode || (room.id == "bedroom" && room.reportedKnown && hardwareState.request?.active != true),
                autoEnabled = !hardwareMode,
                isOn = room.lightOn,
                isAuto = if (hardwareMode) room.reportedKnown && ((hardwareState.sample?.autoFlags ?: 0) and 1) != 0 && ((hardwareState.sample?.manualFlags ?: 0) and 1) == 0 else room.autoLight && nowMs >= room.manualLightHoldUntilMs,
                onSelect = { action -> onActuate(room.id, "light", action) },
                testTagPrefix = "b${roomIndex}_light"
            )

            // Actuator 2: Ceiling Fan
            ActuatorControlRow(
                iconEmoji = "🌀",
                title = "Ceiling Fan",
                metaStatus = if (hardwareMode) "Board-reported fan: ${if (room.reportedKnown) { if (room.fanOn) "ON" else "OFF" } else "UNKNOWN"} · control pending" else room.fanModeStatus(nowMs, tempThreshold),
                known = !hardwareMode || room.reportedKnown,
                enabled = !hardwareMode,
                autoEnabled = !hardwareMode,
                isOn = room.fanOn,
                isAuto = if (hardwareMode) room.reportedKnown && ((hardwareState.sample?.autoFlags ?: 0) and 2) != 0 && ((hardwareState.sample?.manualFlags ?: 0) and 2) == 0 else room.autoFan && nowMs >= room.manualFanHoldUntilMs,
                onSelect = { action -> onActuate(room.id, "fan", action) },
                testTagPrefix = "b${roomIndex}_fan"
            )
        }
    }
}

@Composable
private fun ActuatorControlRow(
    iconEmoji: String,
    title: String,
    metaStatus: String,
    isOn: Boolean,
    isAuto: Boolean,
    onSelect: (ActuatorAction) -> Unit,
    testTagPrefix: String,
    known: Boolean = true,
    enabled: Boolean = true,
    autoEnabled: Boolean = true
) {
    Surface(
        color = ClawBgControl,
        shape = RoundedCornerShape(10.dp),
        border = BorderStroke(1.dp, ClawBorder),
        modifier = Modifier.fillMaxWidth()
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 12.dp, vertical = 10.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Column(
                modifier = Modifier.weight(1f),
                verticalArrangement = Arrangement.spacedBy(2.dp)
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    Text(text = iconEmoji, fontSize = 13.sp)
                    Text(
                        text = title,
                        style = MaterialTheme.typography.titleSmall,
                        color = ClawTextMain,
                        fontWeight = FontWeight.SemiBold
                    )
                }
                Text(
                    text = metaStatus,
                    style = MaterialTheme.typography.bodySmall,
                    color = if (metaStatus.contains("Hold:")) ClawTextMuted else ClawTextDim,
                    fontSize = 11.sp
                )
            }

            Spacer(modifier = Modifier.width(8.dp))

            SegmentedActuatorToggle(
                isOn = isOn,
                isAuto = isAuto,
                onSelect = onSelect,
                testTagPrefix = testTagPrefix,
                known = known, enabled = enabled, autoEnabled = autoEnabled
            )
        }
    }
}

@OptIn(ExperimentalLayoutApi::class)
@Composable
private fun CountdownTimerSchedulerCard(
    uiState: ClawGridUiState,
    onScheduleTimer: (String, String, Int) -> Unit,
    onClearTimers: () -> Unit
) {
    var selectedRoomId by remember { mutableStateOf("bedroom") }
    var selectedDevice by remember { mutableStateOf("light") }
    var minutesText by remember { mutableStateOf("10") }
    var roomDropdownExpanded by remember { mutableStateOf(false) }
    var devDropdownExpanded by remember { mutableStateOf(false) }

    val selectedRoomName = uiState.rooms.find { it.id == selectedRoomId }?.name ?: "Bedroom"
    val selectedDevLabel = if (selectedDevice == "light") "Ceiling Light" else "Ceiling Fan"

    Surface(
        color = Color(0xFF0B111E),
        shape = RoundedCornerShape(12.dp),
        border = BorderStroke(1.dp, ClawBorder),
        modifier = Modifier.fillMaxWidth()
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(14.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(6.dp)
            ) {
                Icon(
                    imageVector = Icons.Default.AccessTime,
                    contentDescription = null,
                    tint = ClawSky,
                    modifier = Modifier.size(16.dp)
                )
                Text(
                    text = "Auto-Off Countdown Timer Scheduler",
                    style = MaterialTheme.typography.titleSmall,
                    color = ClawTextMain,
                    fontWeight = FontWeight.Bold
                )
            }

            FlowRow(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                // Room Selector Dropdown
                Box {
                    Surface(
                        color = ClawBgControl,
                        shape = RoundedCornerShape(8.dp),
                        border = BorderStroke(1.dp, ClawBorder),
                        modifier = Modifier
                            .clickable { roomDropdownExpanded = true }
                            .padding(horizontal = 12.dp, vertical = 10.dp)
                            .testTag("timer_room_selector")
                    ) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            Text(text = selectedRoomName, style = MaterialTheme.typography.bodyMedium, color = ClawTextMain)
                            Text(text = "▾", color = ClawTextMuted)
                        }
                    }
                    DropdownMenu(
                        expanded = roomDropdownExpanded,
                        onDismissRequest = { roomDropdownExpanded = false },
                        modifier = Modifier.background(ClawBgCard)
                    ) {
                        uiState.rooms.forEach { r ->
                            DropdownMenuItem(
                                text = { Text(r.name, color = ClawTextMain) },
                                onClick = {
                                    selectedRoomId = r.id
                                    roomDropdownExpanded = false
                                }
                            )
                        }
                    }
                }

                // Device Selector Dropdown
                Box {
                    Surface(
                        color = ClawBgControl,
                        shape = RoundedCornerShape(8.dp),
                        border = BorderStroke(1.dp, ClawBorder),
                        modifier = Modifier
                            .clickable { devDropdownExpanded = true }
                            .padding(horizontal = 12.dp, vertical = 10.dp)
                            .testTag("timer_device_selector")
                    ) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            Text(text = selectedDevLabel, style = MaterialTheme.typography.bodyMedium, color = ClawTextMain)
                            Text(text = "▾", color = ClawTextMuted)
                        }
                    }
                    DropdownMenu(
                        expanded = devDropdownExpanded,
                        onDismissRequest = { devDropdownExpanded = false },
                        modifier = Modifier.background(ClawBgCard)
                    ) {
                        DropdownMenuItem(
                            text = { Text("Ceiling Light", color = ClawTextMain) },
                            onClick = {
                                selectedDevice = "light"
                                devDropdownExpanded = false
                            }
                        )
                        DropdownMenuItem(
                            text = { Text("Ceiling Fan", color = ClawTextMain) },
                            onClick = {
                                selectedDevice = "fan"
                                devDropdownExpanded = false
                            }
                        )
                    }
                }

                // Minutes Input
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    OutlinedTextField(
                        value = minutesText,
                        onValueChange = { minutesText = it.filter { ch -> ch.isDigit() }.take(4) },
                        singleLine = true,
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                        colors = OutlinedTextFieldDefaults.colors(
                            focusedContainerColor = ClawBgControl,
                            unfocusedContainerColor = ClawBgControl,
                            focusedBorderColor = ClawBlue,
                            unfocusedBorderColor = ClawBorder,
                            focusedTextColor = ClawTextMain,
                            unfocusedTextColor = ClawTextMain
                        ),
                        modifier = Modifier
                            .width(78.dp)
                            .testTag("timer_minutes_input")
                    )
                    Text(
                        text = "minutes",
                        style = MaterialTheme.typography.bodySmall,
                        color = ClawTextDim
                    )
                }
            }

            Row(
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Button(
                    onClick = {
                        val mins = minutesText.toIntOrNull() ?: 10
                        onScheduleTimer(selectedRoomId, selectedDevice, mins)
                    },
                    shape = RoundedCornerShape(8.dp),
                    colors = ButtonDefaults.buttonColors(
                        containerColor = ClawBlueDark,
                        contentColor = Color.White
                    ),
                    modifier = Modifier.testTag("start_countdown_timer_button")
                ) {
                    Text("Start Countdown Timer", style = MaterialTheme.typography.labelLarge)
                }

                OutlinedButton(
                    onClick = onClearTimers,
                    shape = RoundedCornerShape(8.dp),
                    border = BorderStroke(1.dp, ClawBorder),
                    colors = ButtonDefaults.outlinedButtonColors(
                        containerColor = ClawBgControl,
                        contentColor = ClawTextMain
                    ),
                    modifier = Modifier.testTag("clear_all_timers_button")
                ) {
                    Text("Clear All Timers", style = MaterialTheme.typography.labelLarge)
                }
            }

            if (uiState.timers.isEmpty()) {
                Text(
                    text = "No active countdown timers.",
                    style = MaterialTheme.typography.bodySmall,
                    color = ClawTextMuted
                )
            } else {
                Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                    uiState.timers.forEach { tm ->
                        val remSec = ((tm.targetTimeMs - uiState.nowMs + 999L) / 1000L).coerceAtLeast(0L)
                        val m = remSec / 60L
                        val s = remSec % 60L
                        Surface(
                            color = ClawBgControl,
                            shape = RoundedCornerShape(8.dp),
                            border = BorderStroke(1.dp, ClawCyan.copy(alpha = 0.4f))
                        ) {
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(horizontal = 12.dp, vertical = 8.dp),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Text(
                                    text = "⏳ ${tm.roomName} · ${if (tm.device == "light") "Ceiling Light" else "Ceiling Fan"}",
                                    style = MaterialTheme.typography.labelLarge,
                                    color = ClawTextMain
                                )
                                Text(
                                    text = "Auto-OFF in %02d:%02d".format(m, s),
                                    fontFamily = JetBrainsMonoFontFamily,
                                    fontSize = 11.5.sp,
                                    color = ClawSky
                                )
                            }
                        }
                    }
                }
            }
        }
    }
}

@OptIn(ExperimentalLayoutApi::class)
@Composable
private fun VoiceCommandAssistantCard(
    uiState: ClawGridUiState,
    onVoiceInputChanged: (String) -> Unit,
    onRunVoiceCommand: (String?) -> Unit
) {
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
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    Icon(
                        imageVector = Icons.Default.Mic,
                        contentDescription = null,
                        tint = ClawSky,
                        modifier = Modifier.size(18.dp)
                    )
                    Column {
                        Text(
                            text = "Local Typed Command Parser",
                            style = MaterialTheme.typography.titleSmall,
                            color = ClawTextMain,
                            fontWeight = FontWeight.Bold
                        )
                        Text(
                            text = "Text preview only; phone microphone uses the separate push-to-talk control above",
                            style = MaterialTheme.typography.bodySmall,
                            color = ClawTextDim,
                            fontSize = 11.sp
                        )
                    }
                }
            }

            StatusPillBadge(
                text = uiState.voiceStatusBadge,
                tone = PillTone.CYAN
            )

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                OutlinedTextField(
                    value = uiState.voiceInputText,
                    onValueChange = onVoiceInputChanged,
                    placeholder = {
                        Text(
                            "e.g. \"hey claw, turn on bedroom light\"",
                            color = ClawTextDim,
                            fontSize = 12.sp
                        )
                    },
                    singleLine = true,
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedContainerColor = ClawBgControl,
                        unfocusedContainerColor = ClawBgControl,
                        focusedBorderColor = ClawBlue,
                        unfocusedBorderColor = ClawBorder,
                        focusedTextColor = ClawTextMain,
                        unfocusedTextColor = ClawTextMain
                    ),
                    modifier = Modifier
                        .weight(1f)
                        .testTag("voice_command_input")
                )

                Button(
                    onClick = { onRunVoiceCommand(null) },
                    shape = RoundedCornerShape(10.dp),
                    colors = ButtonDefaults.buttonColors(
                        containerColor = ClawBlueDark,
                        contentColor = Color.White
                    ),
                    modifier = Modifier.testTag("voice_execute_button")
                ) {
                    Icon(
                        imageVector = Icons.Default.Send,
                        contentDescription = "Execute Voice Command",
                        modifier = Modifier.size(16.dp)
                    )
                }
            }

            // Quick Voice Chips matching HTML dashboard
            FlowRow(
                horizontalArrangement = Arrangement.spacedBy(6.dp),
                verticalArrangement = Arrangement.spacedBy(6.dp)
            ) {
                val sampleCommands = listOf(
                    "turn on bedroom light",
                    "turn on bedroom fan",
                    "turn on study light",
                    "sleep mode",
                    "resume auto",
                    "all off"
                )
                sampleCommands.forEach { cmd ->
                    Box(
                        modifier = Modifier
                            .clip(RoundedCornerShape(16.dp))
                            .background(ClawBgControl)
                            .border(1.dp, ClawBorder, RoundedCornerShape(16.dp))
                            .clickable { onRunVoiceCommand(cmd) }
                            .padding(horizontal = 10.dp, vertical = 5.dp)
                    ) {
                        Text(
                            text = "\"$cmd\"",
                            fontFamily = JetBrainsMonoFontFamily,
                            fontSize = 10.5.sp,
                            color = ClawSky
                        )
                    }
                }
            }

            // SafetyKernel Terminal Output Block
            Surface(
                color = ClawBgControl,
                shape = RoundedCornerShape(8.dp),
                border = BorderStroke(1.dp, ClawBorder),
                modifier = Modifier.fillMaxWidth()
            ) {
                Row(
                    modifier = Modifier.padding(10.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    Icon(
                        imageVector = Icons.Default.Terminal,
                        contentDescription = null,
                        tint = ClawEmeraldLight,
                        modifier = Modifier.size(15.dp)
                    )
                    Text(
                        text = uiState.terminalOutput,
                        fontFamily = JetBrainsMonoFontFamily,
                        fontSize = 11.sp,
                        color = ClawEmeraldLight
                    )
                }
            }
        }
    }
}

@Composable
private fun EmbeddedRtosOverviewCard(
    uiState: ClawGridUiState,
    onOpenRtosLab: () -> Unit,
    onSyncSupabase: () -> Unit
) {
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
                        text = "🛡️ 3-Tier RTOS, Watchdog & Supabase Cloud",
                        style = MaterialTheme.typography.titleSmall,
                        color = ClawTextMain,
                        fontWeight = FontWeight.Bold
                    )
                    Text(
                        text = "Dual ESP32-S3 Nodes + Raspberry Pi Hub + Supabase Cloud Sync",
                        style = MaterialTheme.typography.bodySmall,
                        color = ClawTextMuted
                    )
                }
                OutlinedButton(
                    onClick = onOpenRtosLab,
                    shape = RoundedCornerShape(8.dp),
                    border = BorderStroke(1.dp, ClawBorder)
                ) {
                    Text("RTOS & Cloud →", style = MaterialTheme.typography.labelLarge, color = ClawSky)
                }
            }

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                MiniTierBox(
                    title = "NODE A (BEDROOM)",
                    value = if (uiState.mode == TelemetryMode.HARDWARE_NODE_A) "Runtime unavailable" else "Simulated 200 Hz",
                    meta = if (uiState.mode == TelemetryMode.HARDWARE_NODE_A) "A task/watchdog health unavailable" else "Simulated boot #${uiState.rtosHealth.nodeABootCount} · 5.0s WDT",
                    valueColor = ClawEmeraldLight,
                    modifier = Modifier.weight(1f)
                )
                MiniTierBox(
                    title = "NODE B (SPEECH)",
                    value = if (uiState.mode == TelemetryMode.HARDWARE_NODE_A) "Runtime unavailable" else "Simulated 200 Hz",
                    meta = if (uiState.mode == TelemetryMode.HARDWARE_NODE_A) "Memory unavailable" else "Simulated SRAM: ${uiState.rtosHealth.sramFreeKib} KiB",
                    valueColor = ClawSky,
                    modifier = Modifier.weight(1f)
                )
                MiniTierBox(
                    title = "SUPABASE CLOUD",
                    value = if (uiState.supabaseStatus.isConnected) "Connected" else "Integration pending",
                    meta = "Ref: ${uiState.supabaseStatus.projectRef.take(8)}…",
                    valueColor = ClawPurple,
                    modifier = Modifier
                        .weight(1f)
                        .clickable(onClick = onSyncSupabase)
                )
            }
        }
    }
}

@Composable
private fun MiniTierBox(
    title: String,
    value: String,
    meta: String,
    valueColor: Color,
    modifier: Modifier = Modifier
) {
    Surface(
        color = ClawBgControl,
        shape = RoundedCornerShape(8.dp),
        border = BorderStroke(1.dp, ClawBorder),
        modifier = modifier
    ) {
        Column(
            modifier = Modifier.padding(10.dp),
            verticalArrangement = Arrangement.spacedBy(3.dp)
        ) {
            Text(
                text = title,
                style = MaterialTheme.typography.labelSmall,
                color = ClawTextDim,
                fontSize = 9.5.sp
            )
            Text(
                text = value,
                style = MaterialTheme.typography.titleSmall,
                color = valueColor,
                fontWeight = FontWeight.Bold,
                fontSize = 12.sp
            )
            Text(
                text = meta,
                fontFamily = JetBrainsMonoFontFamily,
                fontSize = 10.sp,
                color = ClawTextMuted
            )
        }
    }
}

@Composable
private fun RecentAuditStreamCard(
    events: List<AuditEventEntity>,
    onViewAllLogs: () -> Unit
) {
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
                Column {
                    Text(
                        text = "Live Event Audit Stream",
                        style = MaterialTheme.typography.titleSmall,
                        color = ClawTextMain,
                        fontWeight = FontWeight.Bold
                    )
                    Text(
                        text = "Real-time Occupancy, Policy, and SafetyKernel Transitions",
                        style = MaterialTheme.typography.bodySmall,
                        color = ClawTextMuted
                    )
                }
                Text(
                    text = "Full Audit Log →",
                    style = MaterialTheme.typography.labelLarge,
                    color = ClawSky,
                    modifier = Modifier.clickable(onClick = onViewAllLogs)
                )
            }

            events.forEach { evt ->
                Surface(
                    color = ClawBgControl,
                    shape = RoundedCornerShape(8.dp),
                    border = BorderStroke(1.dp, ClawBorder),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 10.dp, vertical = 8.dp),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Column(modifier = Modifier.weight(1f)) {
                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(6.dp)
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
                            Text(
                                text = evt.description,
                                style = MaterialTheme.typography.bodySmall,
                                color = ClawTextMuted,
                                maxLines = 2
                            )
                        }
                        Spacer(modifier = Modifier.width(6.dp))
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
            }
        }
    }
}
