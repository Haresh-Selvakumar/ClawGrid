package com.example.ui.screens

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.Canvas
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
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.data.model.SensorDiagnosticRow
import com.example.data.model.TelemetryMode
import com.example.ui.ClawGridUiState
import com.example.ui.components.PillTone
import com.example.ui.components.StatusPillBadge
import com.example.ui.theme.ClawAmberLight
import com.example.ui.theme.ClawBgCard
import com.example.ui.theme.ClawBgControl
import com.example.ui.theme.ClawBlue
import com.example.ui.theme.ClawBlueDark
import com.example.ui.theme.ClawBorder
import com.example.ui.theme.ClawCyan
import com.example.ui.theme.ClawEmeraldLight
import com.example.ui.theme.ClawIndigo
import com.example.ui.theme.ClawSky
import com.example.ui.theme.ClawTextDim
import com.example.ui.theme.ClawTextMain
import com.example.ui.theme.ClawTextMuted
import com.example.ui.theme.JetBrainsMonoFontFamily
import kotlin.math.cos
import kotlin.math.sin

@OptIn(ExperimentalLayoutApi::class)
@Composable
fun TelemetryAndDiagnosticsScreen(
    uiState: ClawGridUiState,
    diagnosticRows: List<SensorDiagnosticRow>,
    onInjectPulse: (String) -> Unit,
    onSetChartWindow: (Int) -> Unit,
    onSetRoomFilter: (Int) -> Unit,
    onToggleSensorFault: (String) -> Unit
) {
    if (uiState.mode == TelemetryMode.HARDWARE_NODE_A) {
        Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
            Text("Hardware telemetry · ${uiState.hardwareState.message}", color = ClawTextMain)
            diagnosticRows.forEach { row -> Text("${row.roomName} ${row.type}: ${row.latestValue} · ${row.freshness}", color = ClawTextMuted) }
            Text("Temperature and lux charts unavailable: no measured sensor fields in schema 3. A task counters, heap and watchdog health unavailable.", color = ClawAmberLight)
        }
        return
    }
    Column(
        modifier = Modifier.fillMaxWidth(),
        verticalArrangement = Arrangement.spacedBy(14.dp)
    ) {
        // High-Resolution Sensor Telemetry & Signal Generator Card
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
                            text = "High-Resolution Sensor Telemetry & Signal Generator",
                            style = MaterialTheme.typography.titleMedium,
                            color = ClawTextMain,
                            fontWeight = FontWeight.Bold
                        )
                        Text(
                            text = "Live dual-axis waveforms react immediately to Light/Fan actuation, occupancy, and environmental pulses.",
                            style = MaterialTheme.typography.bodySmall,
                            color = ClawTextMuted
                        )
                    }
                }

                // Room Filter + Window Selector Row
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .horizontalScroll(rememberScrollState()),
                    horizontalArrangement = Arrangement.spacedBy(6.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    val roomOptions = listOf(
                        -1 to "All Rooms (Avg)",
                        0 to "Bedroom",
                        1 to "Living Room",
                        2 to "Kitchen",
                        3 to "Study"
                    )
                    roomOptions.forEach { (idx, label) ->
                        val active = uiState.telemetryRoomFilterIndex == idx
                        Box(
                            modifier = Modifier
                                .clip(RoundedCornerShape(8.dp))
                                .background(if (active) ClawBlueDark else ClawBgControl)
                                .border(1.dp, if (active) ClawBlue else ClawBorder, RoundedCornerShape(8.dp))
                                .clickable { onSetRoomFilter(idx) }
                                .padding(horizontal = 10.dp, vertical = 6.dp)
                        ) {
                            Text(
                                text = label,
                                style = MaterialTheme.typography.labelLarge,
                                color = if (active) Color.White else ClawTextMuted,
                                fontSize = 11.sp
                            )
                        }
                    }

                    Spacer(modifier = Modifier.width(6.dp))

                    listOf(60 to "60s", 300 to "5m", 900 to "15m").forEach { (sec, label) ->
                        val active = uiState.chartWindowSeconds == sec
                        Box(
                            modifier = Modifier
                                .clip(RoundedCornerShape(8.dp))
                                .background(if (active) Color(0xFF1E293B) else ClawBgControl)
                                .border(1.dp, if (active) ClawSky else ClawBorder, RoundedCornerShape(8.dp))
                                .clickable { onSetChartWindow(sec) }
                                .padding(horizontal = 8.dp, vertical = 6.dp)
                        ) {
                            Text(
                                text = label,
                                fontFamily = JetBrainsMonoFontFamily,
                                fontSize = 10.5.sp,
                                color = if (active) ClawSky else ClawTextDim
                            )
                        }
                    }
                }

                // Interactive Signal Pulse Buttons
                FlowRow(
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                    verticalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    PulseActionChip("🔥 Heat Pulse (+2.5°C)", "pulse_heat") { onInjectPulse("heat") }
                    PulseActionChip("❄️ Cool Pulse (-2.5°C)", "pulse_cool") { onInjectPulse("cool") }
                    PulseActionChip("☀️ Daylight (+200 lx)", "pulse_lux") { onInjectPulse("lux") }
                    PulseActionChip("🌙 Dusk (-180 lx)", "pulse_dark") { onInjectPulse("dark") }
                }

                // Dual-Axis Telemetry Waveform Canvas
                DualAxisTelemetryWaveformChart(uiState = uiState)

                // Legend Row
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Row(
                        horizontalArrangement = Arrangement.spacedBy(14.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        LegendDot(color = ClawCyan, label = "Temperature (15°C – 40°C)")
                        LegendDot(color = ClawIndigo, label = "Illuminance (0 – 1000 lx)")
                    }
                    StatusPillBadge(
                        text = if (uiState.mode == TelemetryMode.HARDWARE_NODE_A) "HARDWARE_MEASURED" else "SIMULATED",
                        tone = if (uiState.mode == TelemetryMode.HARDWARE_NODE_A) PillTone.EMERALD else PillTone.CYAN
                    )
                }
            }
        }

        // 6-Axis Environmental & Safety Radar Chart Card
        MultiZoneRadarChartCard(uiState = uiState)

        // 16-Channel Sensor Health & Diagnostics Matrix Card
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
                            text = "Sensor Health & Diagnostics Matrix (16 Channels)",
                            style = MaterialTheme.typography.titleSmall,
                            color = ClawTextMain,
                            fontWeight = FontWeight.Bold
                        )
                        Text(
                            text = "Per-sensor availability, warm-up readiness, and fault injection controls",
                            style = MaterialTheme.typography.bodySmall,
                            color = ClawTextMuted
                        )
                    }
                    StatusPillBadge(
                        text = "${diagnosticRows.count { it.isOnline }} / ${diagnosticRows.size} Online",
                        tone = if (diagnosticRows.all { it.isOnline }) PillTone.EMERALD else PillTone.AMBER
                    )
                }

                diagnosticRows.forEach { row ->
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
                            Column(modifier = Modifier.weight(1f)) {
                                Row(
                                    verticalAlignment = Alignment.CenterVertically,
                                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                                ) {
                                    Text(
                                        text = row.sensorId,
                                        fontFamily = JetBrainsMonoFontFamily,
                                        fontWeight = FontWeight.Bold,
                                        fontSize = 11.5.sp,
                                        color = ClawTextMain
                                    )
                                    StatusPillBadge(
                                        text = if (row.isOnline) "Online" else "Disabled",
                                        tone = if (row.isOnline) PillTone.EMERALD else PillTone.RED
                                    )
                                }
                                Text(
                                    text = "${row.roomName} · ${row.type} · ${row.latestValue} (${row.freshness})",
                                    style = MaterialTheme.typography.bodySmall,
                                    color = ClawTextMuted,
                                    fontSize = 11.sp
                                )
                            }

                            if (row.canToggleFault) {
                                OutlinedButton(
                                    onClick = { onToggleSensorFault(row.sensorId) },
                                    shape = RoundedCornerShape(8.dp),
                                    border = BorderStroke(1.dp, ClawBorder),
                                    modifier = Modifier.testTag("diag_toggle_${row.sensorId}")
                                ) {
                                    Text(
                                        text = if (row.isOnline) "Inject Fault" else "Restore",
                                        style = MaterialTheme.typography.labelLarge,
                                        color = if (row.isOnline) ClawAmberLight else ClawEmeraldLight,
                                        fontSize = 11.sp
                                    )
                                }
                            } else {
                                StatusPillBadge(text = row.healthStatus, tone = PillTone.BLUE_MUTED)
                            }
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun DualAxisTelemetryWaveformChart(uiState: ClawGridUiState) {
    val points = uiState.telemetryHistory.takeLast(uiState.chartWindowSeconds)
    val roomFilter = uiState.telemetryRoomFilterIndex

    Box(
        modifier = Modifier
            .fillMaxWidth()
            .height(210.dp)
            .clip(RoundedCornerShape(10.dp))
            .background(ClawBgControl)
            .border(1.dp, ClawBorder, RoundedCornerShape(10.dp))
            .padding(12.dp)
    ) {
        Canvas(modifier = Modifier.matchParentSize()) {
            val w = size.width
            val h = size.height

            // Horizontal reference grid lines
            for (i in 0..4) {
                val y = (h * i) / 4f
                drawLine(
                    color = Color(0xFF151D2F),
                    start = Offset(0f, y),
                    end = Offset(w, y),
                    strokeWidth = 1.dp.toPx()
                )
            }

            if (points.size >= 2) {
                val tempPath = Path()
                val luxPath = Path()

                points.forEachIndexed { idx, pt ->
                    val x = (idx.toFloat() / (points.size - 1).toFloat()) * w
                    val tempVal = if (roomFilter in 0..3 && roomFilter < pt.roomTemps.size) {
                        pt.roomTemps[roomFilter]
                    } else {
                        pt.avgTempC
                    }
                    val luxVal = if (roomFilter in 0..3 && roomFilter < pt.roomLuxes.size) {
                        pt.roomLuxes[roomFilter].toDouble()
                    } else {
                        pt.avgLux
                    }

                    val normTemp = ((tempVal - 15.0) / 25.0).coerceIn(0.05, 0.95).toFloat()
                    val normLux = (luxVal / 1000.0).coerceIn(0.05, 0.95).toFloat()

                    val ty = h - (normTemp * h)
                    val ly = h - (normLux * h)

                    if (idx == 0) {
                        tempPath.moveTo(x, ty)
                        luxPath.moveTo(x, ly)
                    } else {
                        tempPath.lineTo(x, ty)
                        luxPath.lineTo(x, ly)
                    }
                }

                drawPath(
                    path = tempPath,
                    color = ClawCyan,
                    style = Stroke(width = 2.5.dp.toPx(), cap = StrokeCap.Round)
                )
                drawPath(
                    path = luxPath,
                    color = ClawIndigo,
                    style = Stroke(width = 2.5.dp.toPx(), cap = StrokeCap.Round)
                )
            }
        }

        // Overlay Live Readout Pill
        val latest = points.lastOrNull()
        if (latest != null) {
            Text(
                text = "Avg Temp: ${"%.1f".format(latest.avgTempC)}°C · Avg Lux: ${latest.avgLux.toInt()} lx",
                fontFamily = JetBrainsMonoFontFamily,
                fontSize = 10.5.sp,
                color = ClawSky,
                modifier = Modifier
                    .align(Alignment.TopEnd)
                    .clip(RoundedCornerShape(6.dp))
                    .background( Color(0xCC0B111E))
                    .padding(horizontal = 8.dp, vertical = 4.dp)
            )
        }
    }
}

@Composable
private fun MultiZoneRadarChartCard(uiState: ClawGridUiState) {
    val rooms = uiState.rooms
    if (rooms.size < 4) return

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
            Text(
                text = "6-Axis Multi-Zone Environmental & Safety Radar",
                style = MaterialTheme.typography.titleSmall,
                color = ClawTextMain,
                fontWeight = FontWeight.Bold
            )

            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(190.dp)
                    .clip(RoundedCornerShape(10.dp))
                    .background(ClawBgControl)
                    .border(1.dp, ClawBorder, RoundedCornerShape(10.dp)),
                contentAlignment = Alignment.Center
            ) {
                Canvas(modifier = Modifier.size(170.dp)) {
                    val cx = size.width / 2f
                    val cy = size.height / 2f
                    val maxR = size.minDimension * 0.42f
                    val angles = listOf(
                        -Math.PI / 2,
                        -Math.PI / 6,
                        Math.PI / 6,
                        Math.PI / 2,
                        5 * Math.PI / 6,
                        -5 * Math.PI / 6
                    )

                    // Concentric radar rings
                    listOf(0.35f, 0.68f, 1.0f).forEach { frac ->
                        drawCircle(
                            color = Color(0xFF1E293B),
                            radius = maxR * frac,
                            center = Offset(cx, cy),
                            style = Stroke(width = 1.dp.toPx())
                        )
                    }

                    val tempScores = listOf(
                        (rooms[0].tempC / 40.0).coerceIn(0.2, 1.0),
                        (rooms[1].tempC / 40.0).coerceIn(0.2, 1.0),
                        (rooms[2].tempC / 40.0).coerceIn(0.2, 1.0),
                        (rooms[3].tempC / 40.0).coerceIn(0.2, 1.0),
                        0.95,
                        0.88
                    )
                    val luxScores = listOf(
                        (rooms[0].lux / 600.0).coerceIn(0.2, 1.0),
                        (rooms[1].lux / 600.0).coerceIn(0.2, 1.0),
                        (rooms[2].lux / 600.0).coerceIn(0.2, 1.0),
                        (rooms[3].lux / 600.0).coerceIn(0.2, 1.0),
                        0.90,
                        0.82
                    )

                    fun buildPoly(scores: List<Double>): Path {
                        val p = Path()
                        scores.forEachIndexed { i, s ->
                            val r = (s * maxR).toFloat()
                            val x = cx + r * cos(angles[i]).toFloat()
                            val y = cy + r * sin(angles[i]).toFloat()
                            if (i == 0) p.moveTo(x, y) else p.lineTo(x, y)
                        }
                        p.close()
                        return p
                    }

                    val p1 = buildPoly(tempScores)
                    val p2 = buildPoly(luxScores)

                    drawPath(p1, color = ClawCyan.copy(alpha = 0.22f))
                    drawPath(p1, color = ClawCyan, style = Stroke(width = 2.dp.toPx()))

                    drawPath(p2, color = ClawIndigo.copy(alpha = 0.22f))
                    drawPath(p2, color = ClawIndigo, style = Stroke(width = 2.dp.toPx()))
                }
            }
        }
    }
}

@Composable
private fun PulseActionChip(
    text: String,
    testTag: String,
    onClick: () -> Unit
) {
    Surface(
        color = ClawBgControl,
        shape = RoundedCornerShape(8.dp),
        border = BorderStroke(1.dp, ClawBorder),
        modifier = Modifier
            .clickable(onClick = onClick)
            .testTag(testTag)
    ) {
        Text(
            text = text,
            style = MaterialTheme.typography.labelLarge,
            color = ClawTextMain,
            fontSize = 11.sp,
            modifier = Modifier.padding(horizontal = 10.dp, vertical = 7.dp)
        )
    }
}

@Composable
private fun LegendDot(color: Color, label: String) {
    Row(
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(5.dp)
    ) {
        Box(
            modifier = Modifier
                .size(8.dp)
                .clip(CircleShape)
                .background(color)
        )
        Text(text = label, style = MaterialTheme.typography.bodySmall, color = ClawTextMuted, fontSize = 11.sp)
    }
}
