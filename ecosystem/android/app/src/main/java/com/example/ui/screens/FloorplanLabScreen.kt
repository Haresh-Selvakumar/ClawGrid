package com.example.ui.screens

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.detectDragGestures
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.ExitToApp
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material.icons.filled.Warning
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Checkbox
import androidx.compose.material3.CheckboxDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Slider
import androidx.compose.material3.SliderDefaults
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.nativeCanvas
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.data.model.OccupancyState
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
import com.example.ui.theme.ClawRed
import com.example.ui.theme.ClawRedLight
import com.example.ui.theme.ClawSky
import com.example.ui.theme.ClawTextDim
import com.example.ui.theme.ClawTextMain
import com.example.ui.theme.ClawTextMuted
import com.example.ui.theme.JetBrainsMonoFontFamily

@OptIn(ExperimentalLayoutApi::class)
@Composable
fun FloorplanLabScreen(
    uiState: ClawGridUiState,
    onMoveAvatarToPoint: (Float, Float) -> Unit,
    onNudgeAvatar: (Float, Float) -> Unit,
    onJumpToRoom: (Int) -> Unit,
    onLeaveHouse: () -> Unit,
    onToggleTour: () -> Unit,
    onResetHouse: () -> Unit,
    onSelectRoomForEdit: (Int) -> Unit,
    onUpdateRoomEnvironment: (Int, Double, Int, Boolean, Boolean, Boolean) -> Unit,
    onSetRoomSensorsEnabled: (Int, Boolean) -> Unit
) {
    if (uiState.mode != com.example.data.model.TelemetryMode.SIMULATION_4_ROOM) {
        Text("4-Room Lab is simulation only. Select 4-Room Simulation to use its controls.", color = ClawTextMuted)
        return
    }
    val selectedRoomIdx = uiState.selectedSimRoomIndex.coerceIn(0, 3)
    val selectedRoom = uiState.rooms.getOrNull(selectedRoomIdx) ?: return

    Column(
        modifier = Modifier.fillMaxWidth(),
        verticalArrangement = Arrangement.spacedBy(14.dp)
    ) {
        // 4-Room Interactive Floorplan Card
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
                            text = "ClawGrid 4-Room Interactive Simulation Lab",
                            style = MaterialTheme.typography.titleMedium,
                            color = ClawTextMain,
                            fontWeight = FontWeight.Bold
                        )
                        Text(
                            text = "Tap or drag inside any room to move the occupant and trigger real-time PIR/Presence policies.",
                            style = MaterialTheme.typography.bodySmall,
                            color = ClawTextMuted
                        )
                    }
                    StatusPillBadge(
                        text = if (uiState.isRoomTourRunning) "TOUR ACTIVE" else "SIMULATED · Active",
                        tone = PillTone.EMERALD
                    )
                }

                // Top Simulation Controls Row
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .horizontalScroll(rememberScrollState()),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    Button(
                        onClick = onToggleTour,
                        shape = RoundedCornerShape(8.dp),
                        colors = ButtonDefaults.buttonColors(
                            containerColor = if (uiState.isRoomTourRunning) ClawAmber else ClawBlueDark,
                            contentColor = Color.White
                        ),
                        modifier = Modifier.testTag("sim_run_tour_button")
                    ) {
                        Icon(Icons.Default.PlayArrow, contentDescription = null, modifier = Modifier.size(16.dp))
                        Spacer(modifier = Modifier.width(4.dp))
                        Text(
                            if (uiState.isRoomTourRunning) "Pause Room Tour" else "Run Room Tour",
                            style = MaterialTheme.typography.labelLarge
                        )
                    }

                    OutlinedButton(
                        onClick = onLeaveHouse,
                        shape = RoundedCornerShape(8.dp),
                        border = BorderStroke(1.dp, ClawBorder),
                        colors = ButtonDefaults.outlinedButtonColors(
                            containerColor = ClawBgControl,
                            contentColor = ClawTextMain
                        ),
                        modifier = Modifier.testTag("sim_leave_house_button")
                    ) {
                        Icon(Icons.Default.ExitToApp, contentDescription = null, modifier = Modifier.size(15.dp))
                        Spacer(modifier = Modifier.width(4.dp))
                        Text("Leave House", style = MaterialTheme.typography.labelLarge)
                    }

                    OutlinedButton(
                        onClick = onResetHouse,
                        shape = RoundedCornerShape(8.dp),
                        border = BorderStroke(1.dp, ClawBorder),
                        colors = ButtonDefaults.outlinedButtonColors(
                            containerColor = ClawBgControl,
                            contentColor = ClawTextMain
                        ),
                        modifier = Modifier.testTag("sim_reset_defaults_button")
                    ) {
                        Icon(Icons.Default.Refresh, contentDescription = null, modifier = Modifier.size(15.dp))
                        Spacer(modifier = Modifier.width(4.dp))
                        Text("Reset Defaults", style = MaterialTheme.typography.labelLarge)
                    }
                }

                // Interactive 4-Room Floorplan Canvas
                InteractiveHouseFloorplanCanvas(
                    uiState = uiState,
                    onMoveAvatarToPoint = onMoveAvatarToPoint
                )

                // D-Pad Movement & Avatar Coordinates Bar
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Row(
                        horizontalArrangement = Arrangement.spacedBy(6.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text("Move:", style = MaterialTheme.typography.bodySmall, color = ClawTextDim)
                        listOf(
                            "←" to (-36f to 0f),
                            "↑" to (0f to -36f),
                            "↓" to (0f to 36f),
                            "→" to (36f to 0f)
                        ).forEach { (arrow, delta) ->
                            Surface(
                                color = ClawBgControl,
                                shape = RoundedCornerShape(6.dp),
                                border = BorderStroke(1.dp, ClawBorder),
                                modifier = Modifier
                                    .size(34.dp)
                                    .clickable { onNudgeAvatar(delta.first, delta.second) }
                            ) {
                                Box(contentAlignment = Alignment.Center) {
                                    Text(text = arrow, color = ClawTextMain, fontWeight = FontWeight.Bold)
                                }
                            }
                        }
                    }

                    val currentRoomName = uiState.rooms.firstOrNull { it.state == OccupancyState.OCCUPIED }?.name ?: "Outside"
                    Text(
                        text = if (uiState.avatarX < 0f) "Avatar: Outside House"
                        else "Pos: (${uiState.avatarX.toInt()}, ${uiState.avatarY.toInt()}) · $currentRoomName",
                        fontFamily = JetBrainsMonoFontFamily,
                        fontSize = 11.sp,
                        color = ClawSky
                    )
                }

                // Quick Teleport Bar
                FlowRow(
                    horizontalArrangement = Arrangement.spacedBy(6.dp),
                    verticalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    val labels = listOf("🛏️ Bedroom", "🛋️ Living Room", "🍳 Kitchen", "📚 Study")
                    labels.forEachIndexed { idx, label ->
                        val isSelected = idx == selectedRoomIdx
                        Surface(
                            color = if (isSelected) Color(0xFF172554) else ClawBgControl,
                            shape = RoundedCornerShape(8.dp),
                            border = BorderStroke(1.dp, if (isSelected) ClawBlue else ClawBorder),
                            modifier = Modifier
                                .clickable { onJumpToRoom(idx) }
                                .testTag("teleport_room_$idx")
                        ) {
                            Text(
                                text = label,
                                style = MaterialTheme.typography.labelLarge,
                                color = if (isSelected) Color.White else ClawTextMuted,
                                modifier = Modifier.padding(horizontal = 10.dp, vertical = 6.dp)
                            )
                        }
                    }
                    Surface(
                        color = ClawBgControl,
                        shape = RoundedCornerShape(8.dp),
                        border = BorderStroke(1.dp, ClawBorder),
                        modifier = Modifier.clickable(onClick = onLeaveHouse)
                    ) {
                        Text(
                            text = "🚪 Outside",
                            style = MaterialTheme.typography.labelLarge,
                            color = ClawTextMuted,
                            modifier = Modifier.padding(horizontal = 10.dp, vertical = 6.dp)
                        )
                    }
                }
            }
        }

        // Room Environmental Control & Weather Presets Card
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
                    Text(
                        text = "Room Environmental Control",
                        style = MaterialTheme.typography.titleSmall,
                        color = ClawTextMain,
                        fontWeight = FontWeight.Bold
                    )
                    StatusPillBadge(text = selectedRoom.name, tone = PillTone.CYAN)
                }

                // Room selector tabs
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    uiState.rooms.forEachIndexed { idx, r ->
                        val active = idx == selectedRoomIdx
                        Box(
                            modifier = Modifier
                                .weight(1f)
                                .clip(RoundedCornerShape(8.dp))
                                .background(if (active) ClawBlueDark else ClawBgControl)
                                .border(1.dp, if (active) ClawBlue else ClawBorder, RoundedCornerShape(8.dp))
                                .clickable { onSelectRoomForEdit(idx) }
                                .padding(vertical = 8.dp),
                            contentAlignment = Alignment.Center
                        ) {
                            Text(
                                text = r.name,
                                style = MaterialTheme.typography.labelLarge,
                                color = if (active) Color.White else ClawTextMuted,
                                fontSize = 11.sp
                            )
                        }
                    }
                }

                // 1-Click Weather / Lighting Presets
                Text(
                    text = "1-Click Weather & Lighting Presets",
                    style = MaterialTheme.typography.labelSmall,
                    color = ClawTextDim
                )
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    PresetPillButton(
                        text = "☀️ Daylight (24°C, 550 lx)",
                        onClick = {
                            onUpdateRoomEnvironment(
                                selectedRoomIdx,
                                24.0,
                                550,
                                selectedRoom.pirEnabled,
                                selectedRoom.presenceEnabled,
                                selectedRoom.doorOpen
                            )
                        },
                        modifier = Modifier.weight(1f)
                    )
                    PresetPillButton(
                        text = "🌙 Night (23.5°C, 45 lx)",
                        onClick = {
                            onUpdateRoomEnvironment(
                                selectedRoomIdx,
                                23.5,
                                45,
                                selectedRoom.pirEnabled,
                                selectedRoom.presenceEnabled,
                                selectedRoom.doorOpen
                            )
                        },
                        modifier = Modifier.weight(1f)
                    )
                }
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    PresetPillButton(
                        text = "🔥 Hot & Dim (31°C, 110 lx)",
                        onClick = {
                            onUpdateRoomEnvironment(
                                selectedRoomIdx,
                                31.0,
                                110,
                                selectedRoom.pirEnabled,
                                selectedRoom.presenceEnabled,
                                selectedRoom.doorOpen
                            )
                        },
                        modifier = Modifier.weight(1f)
                    )
                    PresetPillButton(
                        text = "❄️ Cool & Bright (21°C, 420 lx)",
                        onClick = {
                            onUpdateRoomEnvironment(
                                selectedRoomIdx,
                                21.0,
                                420,
                                selectedRoom.pirEnabled,
                                selectedRoom.presenceEnabled,
                                selectedRoom.doorOpen
                            )
                        },
                        modifier = Modifier.weight(1f)
                    )
                }

                // Temperature Slider
                Column {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Text("Temperature (°C)", style = MaterialTheme.typography.bodySmall, color = ClawTextMuted)
                        Text(
                            "${"%.1f".format(selectedRoom.tempC)} °C (Auto Fan ≥ ${uiState.policy.tempThreshold}°C)",
                            fontFamily = JetBrainsMonoFontFamily,
                            fontSize = 11.sp,
                            color = ClawSky
                        )
                    }
                    Slider(
                        value = selectedRoom.tempC.toFloat(),
                        onValueChange = { newT ->
                            onUpdateRoomEnvironment(
                                selectedRoomIdx,
                                (newT * 2).toInt() / 2.0,
                                selectedRoom.lux,
                                selectedRoom.pirEnabled,
                                selectedRoom.presenceEnabled,
                                selectedRoom.doorOpen
                            )
                        },
                        valueRange = 16f..42f,
                        colors = SliderDefaults.colors(
                            thumbColor = ClawCyan,
                            activeTrackColor = ClawCyan
                        )
                    )
                }

                // Illuminance Slider
                Column {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Text("Illuminance (Lux)", style = MaterialTheme.typography.bodySmall, color = ClawTextMuted)
                        Text(
                            "${selectedRoom.lux} lx (Auto Light < ${uiState.policy.luxThreshold} lx)",
                            fontFamily = JetBrainsMonoFontFamily,
                            fontSize = 11.sp,
                            color = ClawAmberLight
                        )
                    }
                    Slider(
                        value = selectedRoom.lux.toFloat(),
                        onValueChange = { newL ->
                            onUpdateRoomEnvironment(
                                selectedRoomIdx,
                                selectedRoom.tempC,
                                newL.toInt(),
                                selectedRoom.pirEnabled,
                                selectedRoom.presenceEnabled,
                                selectedRoom.doorOpen
                            )
                        },
                        valueRange = 10f..1000f,
                        colors = SliderDefaults.colors(
                            thumbColor = ClawAmber,
                            activeTrackColor = ClawAmber
                        )
                    )
                }

                // Virtual Sensor Checkboxes
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    SensorCheckItem(
                        label = "Virtual PIR",
                        checked = selectedRoom.pirEnabled,
                        onCheckedChange = {
                            onUpdateRoomEnvironment(
                                selectedRoomIdx,
                                selectedRoom.tempC,
                                selectedRoom.lux,
                                it,
                                selectedRoom.presenceEnabled,
                                selectedRoom.doorOpen
                            )
                        }
                    )
                    SensorCheckItem(
                        label = "mmWave Presence",
                        checked = selectedRoom.presenceEnabled,
                        onCheckedChange = {
                            onUpdateRoomEnvironment(
                                selectedRoomIdx,
                                selectedRoom.tempC,
                                selectedRoom.lux,
                                selectedRoom.pirEnabled,
                                it,
                                selectedRoom.doorOpen
                            )
                        }
                    )
                    SensorCheckItem(
                        label = "Door Open",
                        checked = selectedRoom.doorOpen,
                        onCheckedChange = {
                            onUpdateRoomEnvironment(
                                selectedRoomIdx,
                                selectedRoom.tempC,
                                selectedRoom.lux,
                                selectedRoom.pirEnabled,
                                selectedRoom.presenceEnabled,
                                it
                            )
                        }
                    )
                }

                // Sensor Fault & Safety Experiments
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
                        Text(
                            text = "Sensor Fault & Safety Experiments (${selectedRoom.name})",
                            style = MaterialTheme.typography.titleSmall,
                            color = ClawTextMain,
                            fontWeight = FontWeight.Bold
                        )
                        Text(
                            text = "Disabling both PIR and Presence sensors in a room forces occupancy evidence to UNKNOWN (never falsely claiming VACANT).",
                            style = MaterialTheme.typography.bodySmall,
                            color = ClawTextMuted
                        )
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            OutlinedButton(
                                onClick = { onSetRoomSensorsEnabled(selectedRoomIdx, false) },
                                shape = RoundedCornerShape(8.dp),
                                border = BorderStroke(1.dp, ClawAmber),
                                modifier = Modifier.weight(1f)
                            ) {
                                Icon(Icons.Default.Warning, contentDescription = null, tint = ClawAmberLight, modifier = Modifier.size(15.dp))
                                Spacer(modifier = Modifier.width(6.dp))
                                Text("Disable Sensors", color = ClawAmberLight, fontSize = 11.5.sp)
                            }
                            OutlinedButton(
                                onClick = { onSetRoomSensorsEnabled(selectedRoomIdx, true) },
                                shape = RoundedCornerShape(8.dp),
                                border = BorderStroke(1.dp, ClawEmerald),
                                modifier = Modifier.weight(1f)
                            ) {
                                Icon(Icons.Default.CheckCircle, contentDescription = null, tint = ClawEmeraldLight, modifier = Modifier.size(15.dp))
                                Spacer(modifier = Modifier.width(6.dp))
                                Text("Restore Sensors", color = ClawEmeraldLight, fontSize = 11.5.sp)
                            }
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun InteractiveHouseFloorplanCanvas(
    uiState: ClawGridUiState,
    onMoveAvatarToPoint: (Float, Float) -> Unit
) {
    val rooms = uiState.rooms
    val now = uiState.nowMs

    Box(
        modifier = Modifier
            .fillMaxWidth()
            .aspectRatio(800f / 560f)
            .clip(RoundedCornerShape(12.dp))
            .background(Color(0xFF070B14))
            .border(1.dp, ClawBorder, RoundedCornerShape(12.dp))
            .pointerInput(Unit) {
                detectTapGestures { offset ->
                    val normX = (offset.x / size.width.toFloat()) * 800f
                    val normY = (offset.y / size.height.toFloat()) * 560f
                    onMoveAvatarToPoint(normX, normY)
                }
            }
            .pointerInput(Unit) {
                detectDragGestures { change, _ ->
                    change.consume()
                    val normX = (change.position.x / size.width.toFloat()) * 800f
                    val normY = (change.position.y / size.height.toFloat()) * 560f
                    onMoveAvatarToPoint(normX, normY)
                }
            }
            .testTag("interactive_floorplan_canvas")
    ) {
        Canvas(modifier = Modifier.matchParentSize()) {
            val scaleX = size.width / 800f
            val scaleY = size.height / 560f

            for (i in 0 until 4) {
                val r = rooms.getOrNull(i) ?: continue
                val rx = (i % 2) * 400f * scaleX
                val ry = (i / 2) * 280f * scaleY
                val rw = 388f * scaleX
                val rh = 268f * scaleY
                val pad = 6f * scaleX

                val bgCol = when {
                    r.lightOn -> Color(0xFF1C2B27)
                    r.state == OccupancyState.OCCUPIED -> Color(0xFF131D31)
                    else -> Color(0xFF0B111E)
                }

                drawRoundRect(
                    color = bgCol,
                    topLeft = Offset(rx + pad, ry + pad),
                    size = Size(rw, rh),
                    cornerRadius = CornerRadius(12f, 12f)
                )

                val isSelected = i == uiState.selectedSimRoomIndex
                val strokeCol = when {
                    isSelected -> ClawSky
                    r.state == OccupancyState.OCCUPIED -> ClawEmerald
                    r.state == OccupancyState.LIKELY_OCCUPIED -> ClawAmber
                    r.state == OccupancyState.UNKNOWN -> ClawRed
                    else -> ClawBorder
                }
                drawRoundRect(
                    color = strokeCol,
                    topLeft = Offset(rx + pad, ry + pad),
                    size = Size(rw, rh),
                    cornerRadius = CornerRadius(12f, 12f),
                    style = Stroke(width = if (isSelected) 3.dp.toPx() else 1.5.dp.toPx())
                )

                // Draw native text labels inside each room quadrant
                drawContext.canvas.nativeCanvas.apply {
                    val titlePaint = android.graphics.Paint().apply {
                        color = android.graphics.Color.WHITE
                        textSize = 14.sp.toPx()
                        isFakeBoldText = true
                        isAntiAlias = true
                    }
                    drawText(r.name, rx + 20f * scaleX, ry + 34f * scaleY, titlePaint)

                    val statusPaint = android.graphics.Paint().apply {
                        color = when (r.state) {
                            OccupancyState.OCCUPIED -> android.graphics.Color.parseColor("#34D399")
                            OccupancyState.LIKELY_OCCUPIED -> android.graphics.Color.parseColor("#FBBF24")
                            OccupancyState.UNKNOWN -> android.graphics.Color.parseColor("#F87171")
                            OccupancyState.VACANT -> android.graphics.Color.parseColor("#64748B")
                        }
                        textSize = 11.sp.toPx()
                        isFakeBoldText = true
                        isAntiAlias = true
                    }
                    val statusLabel = when (r.state) {
                        OccupancyState.OCCUPIED -> "● OCCUPIED (Active)"
                        OccupancyState.LIKELY_OCCUPIED -> {
                            val rem = ((uiState.policy.graceSeconds * 1000.0 - (now - r.dwellStartMs)) / 1000.0).coerceAtLeast(0.0)
                            "◐ GRACE (${"%.1f".format(rem)}s)"
                        }
                        OccupancyState.UNKNOWN -> "⚠ UNKNOWN (Sensors Off)"
                        OccupancyState.VACANT -> "○ VACANT"
                    }
                    drawText(statusLabel, rx + 20f * scaleX, ry + 58f * scaleY, statusPaint)

                    val envPaint = android.graphics.Paint().apply {
                        color = android.graphics.Color.parseColor("#94A3B8")
                        textSize = 10.5.sp.toPx()
                        isAntiAlias = true
                    }
                    drawText(
                        "${"%.1f".format(r.tempC)}°C · ${r.lux} lx · ${if (r.pirEnabled) "PIR:ON" else "PIR:OFF"}",
                        rx + 20f * scaleX,
                        ry + 80f * scaleY,
                        envPaint
                    )

                    val loadPaint = android.graphics.Paint().apply {
                        color = if (r.lightOn) android.graphics.Color.parseColor("#FDE047") else android.graphics.Color.parseColor("#64748B")
                        textSize = 11.sp.toPx()
                        isFakeBoldText = true
                        isAntiAlias = true
                    }
                    drawText(
                        "💡 Light: ${if (r.lightOn) "ON" else "OFF"}",
                        rx + 20f * scaleX,
                        ry + 215f * scaleY,
                        loadPaint
                    )

                    val fanPaint = android.graphics.Paint().apply {
                        color = if (r.fanOn) android.graphics.Color.parseColor("#38BDF8") else android.graphics.Color.parseColor("#64748B")
                        textSize = 11.sp.toPx()
                        isFakeBoldText = true
                        isAntiAlias = true
                    }
                    drawText(
                        "🌀 Fan: ${if (r.fanOn) "ON" else "OFF"}",
                        rx + 20f * scaleX,
                        ry + 240f * scaleY,
                        fanPaint
                    )
                }
            }

            // Draw Occupant Avatar if inside house
            if (uiState.avatarX >= 0f && uiState.avatarY >= 0f) {
                val ax = uiState.avatarX * scaleX
                val ay = uiState.avatarY * scaleY
                drawCircle(
                    color = ClawSky.copy(alpha = 0.25f),
                    radius = 22.dp.toPx(),
                    center = Offset(ax, ay)
                )
                drawCircle(
                    color = ClawBlue,
                    radius = 12.dp.toPx(),
                    center = Offset(ax, ay)
                )
                drawCircle(
                    color = Color.White,
                    radius = 12.dp.toPx(),
                    center = Offset(ax, ay),
                    style = Stroke(width = 2.dp.toPx())
                )
            }
        }
    }
}

@Composable
private fun PresetPillButton(
    text: String,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    Surface(
        color = ClawBgControl,
        shape = RoundedCornerShape(8.dp),
        border = BorderStroke(1.dp, ClawBorder),
        modifier = modifier.clickable(onClick = onClick)
    ) {
        Box(
            modifier = Modifier.padding(horizontal = 8.dp, vertical = 9.dp),
            contentAlignment = Alignment.Center
        ) {
            Text(
                text = text,
                style = MaterialTheme.typography.labelLarge,
                color = ClawTextMain,
                fontSize = 11.sp,
                maxLines = 1
            )
        }
    }
}

@Composable
private fun SensorCheckItem(
    label: String,
    checked: Boolean,
    onCheckedChange: (Boolean) -> Unit
) {
    Row(
        verticalAlignment = Alignment.CenterVertically,
        modifier = Modifier.clickable { onCheckedChange(!checked) }
    ) {
        Checkbox(
            checked = checked,
            onCheckedChange = onCheckedChange,
            colors = CheckboxDefaults.colors(
                checkedColor = ClawBlue,
                uncheckedColor = ClawTextDim
            )
        )
        Text(
            text = label,
            style = MaterialTheme.typography.bodySmall,
            color = ClawTextMain
        )
    }
}
