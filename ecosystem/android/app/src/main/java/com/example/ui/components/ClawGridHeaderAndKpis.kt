package com.example.ui.components

import androidx.compose.animation.animateColorAsState
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
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
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Bolt
import androidx.compose.material.icons.filled.CloudDone
import androidx.compose.material.icons.filled.CloudSync
import androidx.compose.material.icons.filled.Menu
import androidx.compose.material.icons.filled.Notifications
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material.icons.filled.WifiTethering
import androidx.compose.material.icons.outlined.Lightbulb
import androidx.compose.material.icons.outlined.Map
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.data.model.ActuatorAction
import com.example.data.model.ClawGridTab
import com.example.data.model.OccupancyState
import com.example.data.model.TelemetryMode
import com.example.ui.ClawGridUiState
import com.example.ui.theme.ClawAmber
import com.example.ui.theme.ClawAmberBg
import com.example.ui.theme.ClawAmberLight
import com.example.ui.theme.ClawBgCard
import com.example.ui.theme.ClawBgControl
import com.example.ui.theme.ClawBgSidebar
import com.example.ui.theme.ClawBlue
import com.example.ui.theme.ClawBlueDark
import com.example.ui.theme.ClawBlueLight
import com.example.ui.theme.ClawBorder
import com.example.ui.theme.ClawCyan
import com.example.ui.theme.ClawEmerald
import com.example.ui.theme.ClawEmeraldBg
import com.example.ui.theme.ClawEmeraldLight
import com.example.ui.theme.ClawRed
import com.example.ui.theme.ClawRedBg
import com.example.ui.theme.ClawRedLight
import com.example.ui.theme.ClawSky
import com.example.ui.theme.ClawTextDim
import com.example.ui.theme.ClawTextMain
import com.example.ui.theme.ClawTextMuted
import com.example.ui.theme.JetBrainsMonoFontFamily
import com.example.ui.theme.SpaceGroteskFontFamily
import kotlin.math.sin

@Composable
fun ClawGridTopHeader(
    uiState: ClawGridUiState,
    onOpenDrawer: () -> Unit,
    onSwitchMode: (TelemetryMode) -> Unit,
    onSyncSupabase: () -> Unit,
    onOpenNotifications: () -> Unit,
    onOpenSettings: () -> Unit
) {
    val isHardware = uiState.mode == TelemetryMode.HARDWARE_NODE_A
    val sb = uiState.supabaseStatus
    val isLive = sb.wsConnected || sb.isConnected

    Surface(
        color = ClawBgSidebar,
        border = BorderStroke(1.dp, ClawBorder),
        modifier = Modifier.fillMaxWidth()
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 14.dp, vertical = 10.dp),
            verticalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            // Top Row: Menu button, Title + Mode Badge, Right Action Icons
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(10.dp),
                    modifier = Modifier.weight(1f)
                ) {
                    Surface(
                        color = ClawBgControl,
                        shape = RoundedCornerShape(8.dp),
                        border = BorderStroke(1.dp, ClawBorder),
                        modifier = Modifier
                            .size(40.dp)
                            .clickable(onClick = onOpenDrawer)
                            .testTag("drawer_menu_button")
                    ) {
                        Box(contentAlignment = Alignment.Center) {
                            Icon(
                                imageVector = Icons.Default.Menu,
                                contentDescription = "Open Navigation Drawer",
                                tint = ClawTextMuted,
                                modifier = Modifier.size(20.dp)
                            )
                        }
                    }

                    Column {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            Text(
                                text = uiState.activeTab.title,
                                style = MaterialTheme.typography.titleMedium,
                                color = ClawTextMain,
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis
                            )
                            StatusPillBadge(
                                text = uiState.mode.badgeText,
                                tone = if (isHardware) PillTone.EMERALD else PillTone.CYAN
                            )
                        }
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(4.dp)
                        ) {
                            Icon(
                                imageVector = Icons.Default.Bolt,
                                contentDescription = null,
                                tint = ClawAmberLight,
                                modifier = Modifier.size(12.dp)
                            )
                            Text(
                                text = "ClawGrid › ${uiState.activeTab.title}",
                                style = MaterialTheme.typography.bodySmall,
                                color = ClawTextDim,
                                fontSize = 11.sp
                            )
                        }
                    }
                }

                // Right Actions: Live Supabase Realtime Status, Notifications, Settings
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    Surface(
                        color = if (isLive) ClawEmeraldBg else ClawBgControl,
                        shape = RoundedCornerShape(8.dp),
                        border = BorderStroke(
                            1.dp,
                            if (isLive) ClawEmerald.copy(alpha = 0.6f) else ClawBorder
                        ),
                        modifier = Modifier
                            .height(36.dp)
                            .clickable(onClick = onSyncSupabase)
                            .testTag("header_supabase_sync_button")
                    ) {
                        Row(
                            modifier = Modifier.padding(horizontal = 8.dp),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(5.dp)
                        ) {
                            Box(
                                modifier = Modifier
                                    .size(7.dp)
                                    .clip(CircleShape)
                                    .background(if (isLive) ClawEmeraldLight else ClawSky)
                            )
                            Icon(
                                imageVector = if (isLive) Icons.Default.CloudDone else Icons.Default.CloudSync,
                                contentDescription = "Supabase Realtime Cloud",
                                tint = if (isLive) ClawEmeraldLight else ClawSky,
                                modifier = Modifier.size(15.dp)
                            )
                            Text(
                                text = if (sb.wsChannelJoined) "WSS LIVE · ${sb.wsRttMs}ms"
                                else if (isLive) "LIVE · ${sb.wsRttMs}ms"
                                else "CONNECTING",
                                fontFamily = JetBrainsMonoFontFamily,
                                fontWeight = FontWeight.Bold,
                                fontSize = 10.sp,
                                color = if (isLive) ClawEmeraldLight else ClawSky
                            )
                        }
                    }

                    // Notification Bell with Red Unread Dot
                    Box {
                        Surface(
                            color = ClawBgControl,
                            shape = RoundedCornerShape(8.dp),
                            border = BorderStroke(1.dp, ClawBorder),
                            modifier = Modifier
                                .size(36.dp)
                                .clickable(onClick = onOpenNotifications)
                                .testTag("header_notifications_button")
                        ) {
                            Box(contentAlignment = Alignment.Center) {
                                Icon(
                                    imageVector = Icons.Default.Notifications,
                                    contentDescription = "Alerts and Notifications",
                                    tint = ClawTextMuted,
                                    modifier = Modifier.size(18.dp)
                                )
                            }
                        }
                        if (uiState.unreadAlertsCount > 0) {
                            Box(
                                modifier = Modifier
                                    .align(Alignment.TopEnd)
                                    .padding(4.dp)
                                    .size(8.dp)
                                    .clip(CircleShape)
                                    .background(ClawRed)
                            )
                        }
                    }

                    // Settings Button
                    Surface(
                        color = ClawBgControl,
                        shape = RoundedCornerShape(8.dp),
                        border = BorderStroke(1.dp, ClawBorder),
                        modifier = Modifier
                            .size(36.dp)
                            .clickable(onClick = onOpenSettings)
                            .testTag("header_settings_button")
                    ) {
                        Box(contentAlignment = Alignment.Center) {
                            Icon(
                                imageVector = Icons.Default.Settings,
                                contentDescription = "Policy Settings",
                                tint = ClawTextMuted,
                                modifier = Modifier.size(18.dp)
                            )
                        }
                    }
                }
            }

            // Second Row: Segmented Mode Switcher + Live Network Packet Counter + Token Pill
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .horizontalScroll(rememberScrollState()),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Row(
                    modifier = Modifier
                        .clip(RoundedCornerShape(24.dp))
                        .background(ClawBgControl)
                        .border(1.dp, ClawBorder, RoundedCornerShape(24.dp))
                        .padding(3.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    ModeSegmentButton(
                        text = "Hardware (Node A)",
                        selected = isHardware,
                        dotColor = ClawEmerald,
                        onClick = { onSwitchMode(TelemetryMode.HARDWARE_NODE_A) },
                        testTag = "mode_hardware_button"
                    )
                    ModeSegmentButton(
                        text = "4-Room Simulation",
                        selected = !isHardware,
                        dotColor = ClawCyan,
                        onClick = { onSwitchMode(TelemetryMode.SIMULATION_4_ROOM) },
                        testTag = "mode_simulation_button"
                    )
                }

                Spacer(modifier = Modifier.width(8.dp))

                // Live TX / RX Packet Counter Pill
                Surface(
                    color = ClawBgControl,
                    shape = RoundedCornerShape(8.dp),
                    border = BorderStroke(1.dp, ClawBorder)
                ) {
                    Row(
                        modifier = Modifier.padding(horizontal = 10.dp, vertical = 6.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(6.dp)
                    ) {
                        Text(
                            text = "NET TX:${sb.txPackets} RX:${sb.rxPackets}",
                            fontFamily = JetBrainsMonoFontFamily,
                            fontSize = 10.5.sp,
                            color = ClawSky
                        )
                        Text(
                            text = "· Token ••••••••",
                            fontFamily = JetBrainsMonoFontFamily,
                            fontSize = 10.5.sp,
                            color = ClawTextDim
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun ModeSegmentButton(
    text: String,
    selected: Boolean,
    dotColor: Color,
    onClick: () -> Unit,
    testTag: String
) {
    val brush = if (selected) {
        Brush.linearGradient(listOf(ClawBlueDark, ClawCyan))
    } else {
        Brush.linearGradient(listOf(Color.Transparent, Color.Transparent))
    }

    Box(
        modifier = Modifier
            .clip(RoundedCornerShape(20.dp))
            .background(brush)
            .clickable(onClick = onClick)
            .padding(horizontal = 12.dp, vertical = 6.dp)
            .testTag(testTag)
    ) {
        Row(
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(6.dp)
        ) {
            Box(
                modifier = Modifier
                    .size(7.dp)
                    .clip(CircleShape)
                    .background(dotColor)
            )
            Text(
                text = text,
                style = MaterialTheme.typography.labelLarge,
                color = if (selected) Color.White else ClawTextMuted,
                fontSize = 11.5.sp
            )
        }
    }
}

@Composable
fun ContextModeBanner(
    uiState: ClawGridUiState,
    onOpenFloorplan: () -> Unit,
    onToggleRoomTour: () -> Unit,
    onOpenRelays: () -> Unit,
    onTriggerLiveBroadcast: () -> Unit = {}
) {
    val isHardware = uiState.mode == TelemetryMode.HARDWARE_NODE_A
    val accentColor = if (isHardware) ClawEmerald else ClawCyan

    Surface(
        color = ClawBgCard,
        shape = RoundedCornerShape(12.dp),
        border = BorderStroke(1.dp, ClawBorder),
        modifier = Modifier.fillMaxWidth()
    ) {
        Row(
            modifier = Modifier.fillMaxWidth()
        ) {
            Box(
                modifier = Modifier
                    .width(4.dp)
                    .height(108.dp)
                    .background(accentColor)
            )
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 14.dp, vertical = 12.dp),
                verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Text(
                        text = if (isHardware) {
                            "📡 Real-Time Hardware Mode (Bedroom Node A)"
                        } else {
                            "🏠 4-Room Interactive Simulation Mode"
                        },
                        style = MaterialTheme.typography.titleSmall,
                        color = ClawTextMain,
                        fontWeight = FontWeight.Bold,
                        modifier = Modifier.weight(1f, fill = false)
                    )
                    StatusPillBadge(
                        text = if (isHardware) "Source: HARDWARE / HOST" else "Source: SIMULATED",
                        tone = if (isHardware) PillTone.EMERALD else PillTone.CYAN
                    )
                }

                Text(
                    text = if (isHardware) {
                        "${uiState.hardwareState.message}. Local gateway readback; cloud integration pending."
                    } else {
                        "Auto policy: Occupied & Lux < ${uiState.policy.luxThreshold} lx turns Light ON · Occupied & Temp ≥ ${uiState.policy.tempThreshold}°C turns Fan ON · Live synced via Supabase WSS."
                    },
                    style = MaterialTheme.typography.bodySmall,
                    color = ClawTextMuted
                )

                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .horizontalScroll(rememberScrollState()),
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    BannerActionButton(
                        text = "Open 4-Room Floorplan",
                        icon = Icons.Outlined.Map,
                        isPrimary = true,
                        onClick = onOpenFloorplan,
                        testTag = "banner_open_floorplan"
                    )
                    BannerActionButton(
                        text = if (uiState.isRoomTourRunning) "⏸ Pause Room Tour" else "▶ Run Room Tour",
                        icon = Icons.Default.PlayArrow,
                        isPrimary = false,
                        onClick = onToggleRoomTour,
                        testTag = "banner_run_tour"
                    )
                    BannerActionButton(
                        text = "⚡ Live WSS Broadcast",
                        icon = Icons.Default.WifiTethering,
                        isPrimary = false,
                        onClick = onTriggerLiveBroadcast,
                        testTag = "banner_live_broadcast"
                    )
                    BannerActionButton(
                        text = "💡 All Room Controls",
                        icon = Icons.Outlined.Lightbulb,
                        isPrimary = false,
                        onClick = onOpenRelays,
                        testTag = "banner_all_controls"
                    )
                }
            }
        }
    }
}

@Composable
private fun BannerActionButton(
    text: String,
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    isPrimary: Boolean,
    onClick: () -> Unit,
    testTag: String
) {
    val bgBrush = if (isPrimary) {
        Brush.linearGradient(listOf(ClawBlueDark, ClawBlue))
    } else {
        Brush.linearGradient(listOf(ClawBgControl, ClawBgControl))
    }
    Box(
        modifier = Modifier
            .clip(RoundedCornerShape(8.dp))
            .background(bgBrush)
            .border(1.dp, if (isPrimary) ClawBlue else ClawBorder, RoundedCornerShape(8.dp))
            .clickable(onClick = onClick)
            .padding(horizontal = 11.dp, vertical = 7.dp)
            .testTag(testTag)
    ) {
        Row(
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(5.dp)
        ) {
            Icon(
                imageVector = icon,
                contentDescription = null,
                tint = if (isPrimary) Color.White else ClawAmberLight,
                modifier = Modifier.size(14.dp)
            )
            Text(
                text = text,
                style = MaterialTheme.typography.labelLarge,
                color = Color.White,
                fontSize = 11.5.sp
            )
        }
    }
}

@Composable
fun ClawGridKpiGrid(
    uiState: ClawGridUiState,
    onKpiClick: (ClawGridTab) -> Unit
) {
    val isHardware = uiState.mode == TelemetryMode.HARDWARE_NODE_A
    val rooms = uiState.rooms
    val occupiedCount = rooms.count { it.state == OccupancyState.OCCUPIED }
    val energizedRelays = rooms.sumOf { (if (it.lightOn) 1 else 0) + (if (it.fanOn) 1 else 0) }
    val avgTemp = if (rooms.isNotEmpty()) rooms.map { it.tempC }.average() else 25.0
    val avgLux = if (rooms.isNotEmpty()) rooms.map { it.lux }.average().toInt() else 236

    Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            // KPI 1: ROOM OCCUPANCY
            KpiCardContainer(
                title = "ROOM OCCUPANCY",
                mainValue = if (isHardware) {
                    if (uiState.hardwareTelemetry.connected) { if (uiState.hardwareTelemetry.bedroomOccupied) "1 / 1" else "0 / 1" } else "UNKNOWN"
                } else {
                    "$occupiedCount / 4"
                },
                badgeText = if (isHardware) {
                    uiState.rooms.firstOrNull()?.state?.label ?: "UNKNOWN"
                } else {
                    "${(occupiedCount * 100) / 4}% Active"
                },
                badgeTone = if ((isHardware && uiState.hardwareTelemetry.bedroomOccupied) || (!isHardware && occupiedCount > 0)) {
                    PillTone.BLUE
                } else {
                    PillTone.BLUE_MUTED
                },
                subtitle = if (isHardware) {
                    "Zone: Physical Bedroom (PIR_1)"
                } else if (occupiedCount > 0) {
                    "Active: ${rooms.filter { it.state == OccupancyState.OCCUPIED }.joinToString { it.name }}"
                } else {
                    "All 4 rooms vacant"
                },
                onClick = { onKpiClick(ClawGridTab.FLOORPLAN) },
                modifier = Modifier.weight(1f),
                bottomGraphic = {
                    Row(
                        horizontalArrangement = Arrangement.spacedBy(4.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        val colors = listOf(
                            ClawBlue,
                            Color(0xFF1E293B),
                            Color(0xFF1E293B),
                            ClawAmber
                        )
                        for (i in 0 until 4) {
                            val c = if (!isHardware && i < rooms.size) {
                                when (rooms[i].state) {
                                    OccupancyState.OCCUPIED -> ClawBlue
                                    OccupancyState.LIKELY_OCCUPIED -> ClawAmber
                                    else -> Color(0xFF1E293B)
                                }
                            } else {
                                Color(0xFF1E293B)
                            }
                            Box(
                                modifier = Modifier
                                    .size(width = 14.dp, height = 10.dp)
                                    .clip(RoundedCornerShape(2.dp))
                                    .background(c)
                            )
                        }
                    }
                }
            )

            // KPI 2: ACTIVE LOADS & RELAYS
            KpiCardContainer(
                title = "ACTIVE LOADS & RELAYS",
                mainValue = if (isHardware) { if (uiState.hardwareTelemetry.connected) "$energizedRelays / 2" else "UNKNOWN" } else "$energizedRelays / 8",
                badgeText = "Relays Guarded",
                badgeTone = PillTone.EMERALD,
                subtitle = if (isHardware) {
                    "Board-reported only · Outputs ${uiState.hardwareState.sample?.takeIf { uiState.hardwareTelemetry.connected }?.outputsEnabled?.let { if (it) "enabled" else "inhibited" } ?: "unknown"}"
                } else {
                    "$energizedRelays Energized · Low-Voltage DC"
                },
                onClick = { onKpiClick(ClawGridTab.RELAYS) },
                modifier = Modifier.weight(1f),
                bottomGraphic = {
                    if (!isHardware) Canvas(modifier = Modifier.size(width = 68.dp, height = 12.dp)) {
                        val w = size.width
                        val h = size.height
                        val path = Path().apply {
                            moveTo(0f, h * 0.85f)
                            lineTo(w * 0.18f, h * 0.85f)
                            lineTo(w * 0.28f, h * 0.15f)
                            lineTo(w * 0.48f, h * 0.15f)
                            lineTo(w * 0.58f, h * 0.85f)
                            lineTo(w * 0.72f, h * 0.85f)
                            lineTo(w * 0.82f, h * 0.15f)
                            lineTo(w, h * 0.15f)
                        }
                        drawPath(
                            path = path,
                            color = ClawEmerald,
                            style = Stroke(width = 2.dp.toPx(), cap = StrokeCap.Round)
                        )
                    }
                }
            )
        }

        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            // KPI 3: AVG ROOM TEMP & LUX
            KpiCardContainer(
                title = "AVG ROOM TEMP & LUX",
                mainValue = if (isHardware) "Unavailable" else "${"%.1f".format(avgTemp)} °C",
                badgeText = if (isHardware) "No sensor transport" else "$avgLux lx Avg",
                badgeTone = PillTone.AMBER,
                subtitle = if (isHardware) {
                    "Node A Digital PIR_1 Telemetry"
                } else {
                    "4-Zone Ambient Fusion"
                },
                onClick = { onKpiClick(ClawGridTab.TELEMETRY) },
                modifier = Modifier.weight(1f),
                bottomGraphic = {
                    if (!isHardware) Canvas(modifier = Modifier.size(width = 68.dp, height = 12.dp)) {
                        val w = size.width
                        val h = size.height
                        val path = Path()
                        for (x in 0..24) {
                            val ratio = x / 24f
                            val px = ratio * w
                            val py = (h * 0.5f) - sin(ratio * Math.PI * 2).toFloat() * (h * 0.35f)
                            if (x == 0) path.moveTo(px, py) else path.lineTo(px, py)
                        }
                        drawPath(
                            path = path,
                            color = ClawAmber,
                            style = Stroke(width = 2.dp.toPx(), cap = StrokeCap.Round)
                        )
                    }
                }
            )

            // KPI 4: SAFETYKERNEL & HEALTH
            KpiCardContainer(
                title = "SAFETYKERNEL & HEALTH",
                mainValue = if (isHardware) { if (uiState.hardwareTelemetry.connected) "Fresh report" else "UNKNOWN" } else "Simulated",
                badgeText = if (isHardware) { if (uiState.hardwareTelemetry.connected) "Age ${uiState.hardwareTelemetry.freshnessMs}ms" else "STALE / DISCONNECTED" } else "WSS (${uiState.supabaseStatus.wsRttMs}ms)",
                badgeTone = PillTone.EMERALD,
                subtitle = if (isHardware) {
                    "A task/watchdog health unavailable"
                } else {
                    "${uiState.policy.dwellSeconds}s Dwell · 0 Faults"
                },
                onClick = { onKpiClick(ClawGridTab.SAFETY_CLOUD) },
                modifier = Modifier.weight(1f),
                bottomGraphic = {
                    Row(
                        horizontalArrangement = Arrangement.spacedBy(6.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        repeat(5) {
                            Box(
                                modifier = Modifier
                                    .size(7.dp)
                                    .clip(CircleShape)
                                    .background(if (uiState.rtosHealth.wdtNominal) ClawEmerald else ClawAmber)
                            )
                        }
                    }
                }
            )
        }
    }
}

@Composable
private fun KpiCardContainer(
    title: String,
    mainValue: String,
    badgeText: String,
    badgeTone: PillTone,
    subtitle: String,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    bottomGraphic: @Composable () -> Unit
) {
    Surface(
        color = ClawBgCard,
        shape = RoundedCornerShape(14.dp),
        border = BorderStroke(1.dp, ClawBorder),
        modifier = modifier
            .clickable(onClick = onClick)
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 14.dp, vertical = 12.dp),
            verticalArrangement = Arrangement.SpaceBetween
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = title,
                    style = MaterialTheme.typography.labelSmall,
                    color = ClawTextMuted,
                    fontWeight = FontWeight.Bold,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
                Text(
                    text = "↗",
                    style = MaterialTheme.typography.labelSmall,
                    color = ClawTextDim
                )
            }

            Spacer(modifier = Modifier.height(6.dp))

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = mainValue,
                    fontFamily = SpaceGroteskFontFamily,
                    fontWeight = FontWeight.Bold,
                    fontSize = 21.sp,
                    color = Color.White,
                    maxLines = 1
                )
                StatusPillBadge(text = badgeText, tone = badgeTone)
            }

            Spacer(modifier = Modifier.height(4.dp))

            Text(
                text = subtitle,
                style = MaterialTheme.typography.bodySmall,
                color = ClawTextDim,
                fontSize = 11.sp,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )

            Spacer(modifier = Modifier.height(8.dp))

            Box(
                modifier = Modifier.fillMaxWidth(),
                contentAlignment = Alignment.Center
            ) {
                bottomGraphic()
            }
        }
    }
}

enum class PillTone {
    EMERALD, CYAN, BLUE, BLUE_MUTED, AMBER, RED, NEUTRAL
}

@Composable
fun StatusPillBadge(
    text: String,
    tone: PillTone,
    modifier: Modifier = Modifier
) {
    val (bg, fg, borderCol) = when (tone) {
        PillTone.EMERALD -> Triple(ClawEmeraldBg, ClawEmeraldLight, ClawEmerald.copy(alpha = 0.35f))
        PillTone.CYAN -> Triple(Color(0xFF0A2736), ClawSky, ClawCyan.copy(alpha = 0.35f))
        PillTone.BLUE -> Triple(Color(0xFF13254C), ClawBlueLight, ClawBlue.copy(alpha = 0.4f))
        PillTone.BLUE_MUTED -> Triple(Color(0xFF111C35), Color(0xFF60A5FA), ClawBlue.copy(alpha = 0.25f))
        PillTone.AMBER -> Triple(ClawAmberBg, ClawAmberLight, ClawAmber.copy(alpha = 0.4f))
        PillTone.RED -> Triple(ClawRedBg, ClawRedLight, ClawRed.copy(alpha = 0.4f))
        PillTone.NEUTRAL -> Triple(Color(0xFF172033), ClawTextMuted, ClawBorder)
    }

    Surface(
        color = bg,
        shape = RoundedCornerShape(6.dp),
        border = BorderStroke(1.dp, borderCol),
        modifier = modifier
    ) {
        Text(
            text = text,
            fontFamily = JetBrainsMonoFontFamily,
            fontWeight = FontWeight.Bold,
            fontSize = 9.5.sp,
            color = fg,
            modifier = Modifier.padding(horizontal = 7.dp, vertical = 3.dp),
            maxLines = 1
        )
    }
}

@Composable
fun SegmentedActuatorToggle(
    isOn: Boolean,
    isAuto: Boolean,
    onSelect: (ActuatorAction) -> Unit,
    testTagPrefix: String,
    known: Boolean = true,
    enabled: Boolean = true,
    autoEnabled: Boolean = true
) {
    Row(
        modifier = Modifier
            .clip(RoundedCornerShape(8.dp))
            .background(ClawBgControl)
            .border(1.dp, ClawBorder, RoundedCornerShape(8.dp))
            .padding(2.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        ToggleSegmentItem(
            label = "ON",
            active = known && isOn && !isAuto,
            activeBg = ClawEmerald,
            activeFg = Color.White,
            onClick = { onSelect(ActuatorAction.ON) },
            enabled = enabled,
            testTag = "${testTagPrefix}_on"
        )
        ToggleSegmentItem(
            label = "OFF",
            active = known && !isOn && !isAuto,
            activeBg = Color(0xFF334155),
            activeFg = Color.White,
            onClick = { onSelect(ActuatorAction.OFF) },
            enabled = enabled,
            testTag = "${testTagPrefix}_off"
        )
        ToggleSegmentItem(
            label = "AUTO",
            active = known && isAuto,
            activeBg = ClawBlueDark,
            activeFg = Color.White,
            onClick = { onSelect(ActuatorAction.AUTO) },
            enabled = enabled && autoEnabled,
            testTag = "${testTagPrefix}_auto"
        )
    }
}

@Composable
private fun ToggleSegmentItem(
    label: String,
    active: Boolean,
    activeBg: Color,
    activeFg: Color,
    onClick: () -> Unit,
    testTag: String,
    enabled: Boolean = true
) {
    val bgColor by animateColorAsState(
        targetValue = if (active) activeBg else Color.Transparent,
        label = "toggleBg"
    )
    Box(
        modifier = Modifier
            .clip(RoundedCornerShape(6.dp))
            .background(bgColor)
            .clickable(enabled = enabled, onClick = onClick)
            .padding(horizontal = 10.dp, vertical = 6.dp)
            .testTag(testTag),
        contentAlignment = Alignment.Center
    ) {
        Text(
            text = label,
            fontFamily = JetBrainsMonoFontFamily,
            fontWeight = FontWeight.Bold,
            fontSize = 10.5.sp,
            color = if (active) activeFg else ClawTextMuted
        )
    }
}
