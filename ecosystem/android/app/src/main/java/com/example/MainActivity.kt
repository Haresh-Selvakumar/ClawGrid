package com.example

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.BackHandler
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBars
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.safeDrawing
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Bolt
import androidx.compose.material.icons.filled.Dashboard
import androidx.compose.material.icons.filled.Home
import androidx.compose.material.icons.filled.Lightbulb
import androidx.compose.material.icons.filled.Security
import androidx.compose.material.icons.filled.ShowChart
import androidx.compose.material.icons.outlined.Dashboard
import androidx.compose.material.icons.outlined.Home
import androidx.compose.material.icons.outlined.Lightbulb
import androidx.compose.material.icons.outlined.Security
import androidx.compose.material.icons.outlined.ShowChart
import androidx.compose.material3.DrawerValue
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.TextButton
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.ModalDrawerSheet
import androidx.compose.material3.ModalNavigationDrawer
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.NavigationBarItemDefaults
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.rememberDrawerState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleEventObserver
import androidx.lifecycle.compose.LocalLifecycleOwner
import com.example.ui.components.PhonePushToTalk
import com.example.ui.components.HardwareRequestStatus
import com.example.data.model.AuditEventEntity
import com.example.data.model.ClawGridTab
import com.example.data.model.TelemetryMode
import com.example.data.remote.RequestPhase
import com.example.ui.ClawGridUiState
import com.example.ui.ClawGridViewModel
import com.example.ui.components.ClawGridKpiGrid
import com.example.ui.components.ClawGridTopHeader
import com.example.ui.components.ContextModeBanner
import com.example.ui.components.PillTone
import com.example.ui.components.StatusPillBadge
import com.example.ui.screens.FloorplanLabScreen
import com.example.ui.screens.RoomsAndRelaysScreen
import com.example.ui.screens.SafetyRtosAndCloudScreen
import com.example.ui.screens.TelemetryAndDiagnosticsScreen
import com.example.ui.theme.ClawAmber
import com.example.ui.theme.ClawAmberLight
import com.example.ui.theme.ClawBgCard
import com.example.ui.theme.ClawBgControl
import com.example.ui.theme.ClawBgDark
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
import com.example.ui.theme.ClawRedLight
import com.example.ui.theme.ClawSky
import com.example.ui.theme.ClawTextDim
import com.example.ui.theme.ClawTextMain
import com.example.ui.theme.ClawTextMuted
import com.example.ui.theme.JetBrainsMonoFontFamily
import com.example.ui.theme.MyApplicationTheme
import com.example.ui.theme.SpaceGroteskFontFamily
import kotlinx.coroutines.launch

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContent {
            MyApplicationTheme {
                ClawGridAppRoot()
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ClawGridAppRoot(
    viewModel: ClawGridViewModel = viewModel()
) {
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()
    val auditEvents by viewModel.auditEvents.collectAsStateWithLifecycle()
    val drawerState = rememberDrawerState(initialValue = DrawerValue.Closed)
    val scope = rememberCoroutineScope()
    var showNotificationsSheet by remember { mutableStateOf(false) }
    val owner = LocalLifecycleOwner.current
    DisposableEffect(owner, viewModel) {
        val observer = LifecycleEventObserver { _, event -> if (event == Lifecycle.Event.ON_STOP) viewModel.cancelPhoneVoice() }
        owner.lifecycle.addObserver(observer)
        onDispose { owner.lifecycle.removeObserver(observer); viewModel.cancelPhoneVoice() }
    }

    if (uiState.mode == TelemetryMode.HARDWARE_NODE_A && uiState.hardwareState.request?.phase == RequestPhase.PROPOSED) {
        AlertDialog(
            onDismissRequest = viewModel::cancelHardwareProposal,
            title = { Text("Confirm Bedroom light ${if (uiState.hardwareState.request?.on == true) "ON" else "OFF"}") },
            text = { Text((if (uiState.hardwareState.request?.source == "speech") "ESP speech job ${uiState.hardwareState.request?.speechJob} recognized this intent. " else "Manual request. ") + "This requests the physical_bedroom target through SafetyKernel. Current firmware applies a five-minute manual hold and three-second dwell. Board report and ACK are required; the physical load is not measured.") },
            confirmButton = { TextButton(onClick = viewModel::confirmHardwareLight) { Text("Confirm") } },
            dismissButton = { TextButton(onClick = viewModel::cancelHardwareProposal) { Text("Cancel") } }
        )
    }

    // Handle system back button when on non-default tabs
    BackHandler(enabled = uiState.activeTab != ClawGridTab.RELAYS) {
        viewModel.selectTab(ClawGridTab.RELAYS)
    }

    ModalNavigationDrawer(
        drawerState = drawerState,
        drawerContent = {
            ModalDrawerSheet(
                drawerContainerColor = ClawBgSidebar,
                drawerContentColor = ClawTextMain,
                modifier = Modifier.width(280.dp)
            ) {
                ClawGridSidebarContent(
                    uiState = uiState,
                    eventCount = auditEvents.size,
                    onSelectTab = { tab ->
                        viewModel.selectTab(tab)
                        scope.launch { drawerState.close() }
                    }
                )
            }
        }
    ) {
        Box(
            modifier = Modifier
                .fillMaxSize()
                .background(ClawBgDark)
        ) {
            Scaffold(
                containerColor = ClawBgDark,
                contentWindowInsets = WindowInsets.safeDrawing,
                topBar = {
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .background(ClawBgSidebar)
                            .padding(top = 28.dp)
                    ) {
                        ClawGridTopHeader(
                            uiState = uiState,
                            onOpenDrawer = { scope.launch { drawerState.open() } },
                            onSwitchMode = viewModel::switchTelemetryMode,
                            onSyncSupabase = { viewModel.syncWithSupabaseCloud(silent = false) },
                            onOpenNotifications = {
                                viewModel.clearUnreadAlerts()
                                showNotificationsSheet = true
                            },
                            onOpenSettings = { viewModel.selectTab(ClawGridTab.SAFETY_CLOUD) }
                        )
                    }
                },
                bottomBar = {
                    ClawGridBottomNavBar(
                        activeTab = uiState.activeTab,
                        eventCount = auditEvents.size,
                        onSelectTab = viewModel::selectTab
                    )
                }
            ) { innerPadding ->
                Column(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(innerPadding)
                        .verticalScroll(rememberScrollState())
                        .padding(horizontal = 14.dp, vertical = 12.dp),
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .widthIn(max = 960.dp),
                        verticalArrangement = Arrangement.spacedBy(14.dp)
                    ) {
                        // 1. Intuitive Mode & Quick-Action Strip Banner matching screenshot
                        ContextModeBanner(
                            uiState = uiState,
                            onOpenFloorplan = { viewModel.selectTab(ClawGridTab.FLOORPLAN) },
                            onToggleRoomTour = {
                                viewModel.toggleRoomTour()
                                viewModel.selectTab(ClawGridTab.FLOORPLAN)
                            },
                            onOpenRelays = { viewModel.selectTab(ClawGridTab.RELAYS) },
                            onTriggerLiveBroadcast = viewModel::triggerLiveCloudBroadcastDemo
                        )

                        if (uiState.mode == TelemetryMode.HARDWARE_NODE_A) {
                            HardwareRequestStatus(uiState.hardwareState)
                        }

                        // 2. 4 KPI Cards Row/Grid matching screenshot
                        ClawGridKpiGrid(
                            uiState = uiState,
                            onKpiClick = viewModel::selectTab
                        )

                        if (uiState.mode == TelemetryMode.HARDWARE_NODE_A && uiState.activeTab in listOf(ClawGridTab.RELAYS, ClawGridTab.OVERVIEW)) {
                            PhonePushToTalk(uiState, viewModel::startPhoneVoice, viewModel::stopPhoneVoice, viewModel::cancelPhoneVoice)
                        }

                        // 3. Active Screen Content
                        when (uiState.activeTab) {
                            ClawGridTab.RELAYS -> {
                                RoomsAndRelaysScreen(
                                    uiState = uiState,
                                    recentEvents = auditEvents,
                                    showOverviewExtras = false,
                                    onActuate = { room, dev, action ->
                                        viewModel.setActuator(room, dev, action)
                                    },
                                    onApplyScene = viewModel::applyScene,
                                    onScheduleTimer = viewModel::scheduleCountdownTimer,
                                    onClearTimers = viewModel::clearAllTimers,
                                    onVoiceInputChanged = viewModel::updateVoiceInputText,
                                    onRunVoiceCommand = viewModel::runVoiceCommand,
                                    onNavigateTab = viewModel::selectTab,
                                    onSyncSupabase = { viewModel.syncWithSupabaseCloud(silent = false) }
                                )
                            }

                            ClawGridTab.OVERVIEW -> {
                                RoomsAndRelaysScreen(
                                    uiState = uiState,
                                    recentEvents = auditEvents,
                                    showOverviewExtras = true,
                                    onActuate = { room, dev, action ->
                                        viewModel.setActuator(room, dev, action)
                                    },
                                    onApplyScene = viewModel::applyScene,
                                    onScheduleTimer = viewModel::scheduleCountdownTimer,
                                    onClearTimers = viewModel::clearAllTimers,
                                    onVoiceInputChanged = viewModel::updateVoiceInputText,
                                    onRunVoiceCommand = viewModel::runVoiceCommand,
                                    onNavigateTab = viewModel::selectTab,
                                    onSyncSupabase = { viewModel.syncWithSupabaseCloud(silent = false) }
                                )
                            }

                            ClawGridTab.FLOORPLAN -> {
                                FloorplanLabScreen(
                                    uiState = uiState,
                                    onMoveAvatarToPoint = viewModel::moveAvatarToCanvasPoint,
                                    onNudgeAvatar = viewModel::nudgeAvatar,
                                    onJumpToRoom = { idx -> viewModel.jumpAvatarToRoom(idx) },
                                    onLeaveHouse = viewModel::leaveHouse,
                                    onToggleTour = viewModel::toggleRoomTour,
                                    onResetHouse = viewModel::resetSimulationDefaults,
                                    onSelectRoomForEdit = viewModel::selectSimRoomForEdit,
                                    onUpdateRoomEnvironment = viewModel::updateRoomEnvironment,
                                    onSetRoomSensorsEnabled = viewModel::setRoomSensorsEnabled
                                )
                            }

                            ClawGridTab.TELEMETRY -> {
                                TelemetryAndDiagnosticsScreen(
                                    uiState = uiState,
                                    diagnosticRows = viewModel.getDiagnosticRows(),
                                    onInjectPulse = viewModel::injectTelemetryPulse,
                                    onSetChartWindow = viewModel::setChartWindowSeconds,
                                    onSetRoomFilter = viewModel::setTelemetryRoomFilter,
                                    onToggleSensorFault = viewModel::toggleSensorFault
                                )
                            }

                            ClawGridTab.SAFETY_CLOUD -> {
                                SafetyRtosAndCloudScreen(
                                    uiState = uiState,
                                    allEvents = auditEvents,
                                    onSyncSupabasePush = { viewModel.syncWithSupabaseCloud(silent = false) },
                                    onSyncSupabasePull = viewModel::pullFromSupabaseCloud,
                                    onTriggerLiveBroadcast = viewModel::triggerLiveCloudBroadcastDemo,
                                    onReconnectWss = viewModel::reconnectRealtimeWebSocket,
                                    onToggleAutoSync = viewModel::toggleAutoRealtimeSync,
                                    onSimulateWdtStall = viewModel::simulateWdtStall,
                                    onSimulateRadioLeaseExpiry = viewModel::simulateRadioLeaseExpiry,
                                    onSimulatePiFailover = viewModel::simulatePiFailover,
                                    onRestoreNominalRtos = viewModel::restoreNominalRtos,
                                    onSavePolicy = viewModel::savePolicyConfig,
                                    onUpdateLogFilters = viewModel::updateLogFilters,
                                    onClearLogs = viewModel::clearAuditLogs,
                                    onShowToast = viewModel::showToast
                                )
                            }
                        }

                        Spacer(modifier = Modifier.height(16.dp))
                    }
                }
            }

            // Non-blocking Toast Banner Overlay
            AnimatedVisibility(
                visible = uiState.activeToast != null,
                enter = fadeIn(),
                exit = fadeOut(),
                modifier = Modifier
                    .align(Alignment.BottomCenter)
                    .padding(bottom = 86.dp, start = 16.dp, end = 16.dp)
            ) {
                uiState.activeToast?.let { toast ->
                    val borderCol = when (toast.severity) {
                        "DANGER" -> ClawRed
                        "WARN" -> ClawAmber
                        "SUCCESS" -> ClawEmerald
                        else -> ClawBlue
                    }
                    Surface(
                        color = Color(0xF20B111E),
                        shape = RoundedCornerShape(12.dp),
                        border = BorderStroke(1.5.dp, borderCol),
                        shadowElevation = 10.dp,
                        modifier = Modifier
                            .fillMaxWidth()
                            .widthIn(max = 520.dp)
                            .clickable { viewModel.dismissToast() }
                    ) {
                        Column(
                            modifier = Modifier.padding(horizontal = 14.dp, vertical = 10.dp),
                            verticalArrangement = Arrangement.spacedBy(2.dp)
                        ) {
                            Text(
                                text = toast.title,
                                style = MaterialTheme.typography.titleSmall,
                                color = Color.White,
                                fontWeight = FontWeight.Bold
                            )
                            Text(
                                text = toast.message,
                                style = MaterialTheme.typography.bodySmall,
                                color = ClawTextMuted
                            )
                        }
                    }
                }
            }

            // Notifications & Alerts Modal Bottom Sheet
            if (showNotificationsSheet) {
                ModalBottomSheet(
                    onDismissRequest = { showNotificationsSheet = false },
                    containerColor = ClawBgSidebar,
                    contentColor = ClawTextMain
                ) {
                    NotificationsBottomSheetContent(
                        events = auditEvents.take(15),
                        supabaseStatus = uiState.supabaseStatus.statusMessage
                    )
                }
            }
        }
    }
}

@Composable
private fun ClawGridSidebarContent(
    uiState: ClawGridUiState,
    eventCount: Int,
    onSelectTab: (ClawGridTab) -> Unit
) {
    Column(
        modifier = Modifier
            .fillMaxHeight()
            .padding(vertical = 12.dp),
        verticalArrangement = Arrangement.SpaceBetween
    ) {
        Column {
            // Brand Header matching screenshot: Blue glowing bolt icon + CLAWGRID + SMART HOME & EDGE AI
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 18.dp, vertical = 12.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                Box(
                    modifier = Modifier
                        .size(38.dp)
                        .clip(RoundedCornerShape(10.dp))
                        .background(Brush.linearGradient(listOf(ClawBlue, ClawCyan))),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        imageVector = Icons.Default.Bolt,
                        contentDescription = null,
                        tint = Color.White,
                        modifier = Modifier.size(22.dp)
                    )
                }
                Column {
                    Text(
                        text = "CLAWGRID",
                        fontFamily = SpaceGroteskFontFamily,
                        fontWeight = FontWeight.Bold,
                        fontSize = 18.sp,
                        color = Color.White,
                        letterSpacing = 0.8.sp
                    )
                    Text(
                        text = "SMART HOME & EDGE AI",
                        style = MaterialTheme.typography.labelSmall,
                        color = ClawSky,
                        fontSize = 9.5.sp
                    )
                }
            }

            HorizontalDivider(color = ClawBorder)

            // User / Node Widget matching screenshot ("CG" avatar + "ClawGrid V2.0" + "Hardware Mode · Bedroom Node A")
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .background(Color(0x06FFFFFF))
                    .padding(horizontal = 18.dp, vertical = 14.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                Box(
                    modifier = Modifier
                        .size(40.dp)
                        .clip(RoundedCornerShape(10.dp))
                        .background(ClawBgCard)
                        .border(1.5.dp, ClawCyan, RoundedCornerShape(10.dp)),
                    contentAlignment = Alignment.Center
                ) {
                    Text(
                        text = "CG",
                        fontFamily = JetBrainsMonoFontFamily,
                        fontWeight = FontWeight.Bold,
                        fontSize = 13.sp,
                        color = ClawSky
                    )
                }
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = "ClawGrid V2.0",
                        style = MaterialTheme.typography.titleSmall,
                        color = ClawTextMain,
                        fontWeight = FontWeight.Bold
                    )
                    Text(
                        text = if (uiState.mode == TelemetryMode.HARDWARE_NODE_A) {
                            "Hardware Mode · Bedroom Node A"
                        } else {
                            "4-Room Simulation · SafetyKernel"
                        },
                        style = MaterialTheme.typography.bodySmall,
                        color = ClawCyan,
                        fontSize = 11.sp,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                }
            }

            HorizontalDivider(color = ClawBorder)

            // Navigation Section: HOME AUTOMATION
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 10.dp, vertical = 10.dp),
                verticalArrangement = Arrangement.spacedBy(4.dp)
            ) {
                Text(
                    text = "HOME AUTOMATION",
                    style = MaterialTheme.typography.labelSmall,
                    color = ClawTextDim,
                    modifier = Modifier.padding(horizontal = 12.dp, vertical = 6.dp)
                )

                SidebarNavItem(
                    emoji = "🏠",
                    title = "Home Overview",
                    selected = uiState.activeTab == ClawGridTab.OVERVIEW,
                    badge = null,
                    onClick = { onSelectTab(ClawGridTab.OVERVIEW) }
                )
                SidebarNavItem(
                    emoji = "🏘️",
                    title = "4-Room Interactive Lab",
                    selected = uiState.activeTab == ClawGridTab.FLOORPLAN,
                    badge = "4 Zones",
                    onClick = { onSelectTab(ClawGridTab.FLOORPLAN) }
                )
                SidebarNavItem(
                    emoji = "💡",
                    title = "Rooms & Relays",
                    selected = uiState.activeTab == ClawGridTab.RELAYS,
                    badge = null,
                    onClick = { onSelectTab(ClawGridTab.RELAYS) }
                )
                SidebarNavItem(
                    emoji = "📈",
                    title = "Live Telemetry",
                    selected = uiState.activeTab == ClawGridTab.TELEMETRY,
                    badge = null,
                    onClick = { onSelectTab(ClawGridTab.TELEMETRY) }
                )

                Spacer(modifier = Modifier.height(8.dp))

                Text(
                    text = "SAFETY, CLOUD & DIAGNOSTICS",
                    style = MaterialTheme.typography.labelSmall,
                    color = ClawTextDim,
                    modifier = Modifier.padding(horizontal = 12.dp, vertical = 6.dp)
                )

                SidebarNavItem(
                    emoji = "🛡️",
                    title = "SafetyKernel, RTOS & Cloud",
                    selected = uiState.activeTab == ClawGridTab.SAFETY_CLOUD,
                    badge = eventCount.toString(),
                    onClick = { onSelectTab(ClawGridTab.SAFETY_CLOUD) }
                )
            }
        }

        // Sidebar Footer Card matching screenshot: "🛡️ SafetyKernel Active"
        Surface(
            color = ClawEmeraldBg,
            shape = RoundedCornerShape(10.dp),
            border = BorderStroke(1.dp, ClawEmerald.copy(alpha = 0.35f)),
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 12.dp, vertical = 8.dp)
        ) {
            Column(
                modifier = Modifier.padding(12.dp),
                verticalArrangement = Arrangement.spacedBy(4.dp)
            ) {
                Text(
                    text = "🛡️ SafetyKernel Active",
                    style = MaterialTheme.typography.titleSmall,
                    color = ClawEmeraldLight,
                    fontWeight = FontWeight.Bold,
                    fontSize = 12.sp
                )
                Text(
                    text = "Low-voltage DC only · ${uiState.policy.dwellSeconds.toInt()}s dwell guard · 4h max run cutoff enforced.",
                    style = MaterialTheme.typography.bodySmall,
                    color = ClawTextMuted,
                    fontSize = 11.sp
                )
            }
        }
    }
}

@Composable
private fun SidebarNavItem(
    emoji: String,
    title: String,
    selected: Boolean,
    badge: String?,
    onClick: () -> Unit
) {
    val bgBrush = if (selected) {
        Brush.linearGradient(
            listOf(
                ClawBlue.copy(alpha = 0.18f),
                ClawCyan.copy(alpha = 0.14f)
            )
        )
    } else {
        Brush.linearGradient(listOf(Color.Transparent, Color.Transparent))
    }

    Box(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(8.dp))
            .background(bgBrush)
            .border(
                1.dp,
                if (selected) ClawBlue.copy(alpha = 0.45f) else Color.Transparent,
                RoundedCornerShape(8.dp)
            )
            .clickable(onClick = onClick)
            .padding(horizontal = 12.dp, vertical = 10.dp)
    ) {
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
                Text(text = emoji, fontSize = 15.sp)
                Text(
                    text = title,
                    style = MaterialTheme.typography.titleSmall,
                    color = if (selected) ClawBlueLight else ClawTextMuted,
                    fontWeight = if (selected) FontWeight.Bold else FontWeight.Medium,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
            }
            if (badge != null) {
                Surface(
                    color = ClawBlue.copy(alpha = 0.22f),
                    shape = RoundedCornerShape(10.dp)
                ) {
                    Text(
                        text = badge,
                        fontFamily = JetBrainsMonoFontFamily,
                        fontWeight = FontWeight.Bold,
                        fontSize = 10.sp,
                        color = ClawBlueLight,
                        modifier = Modifier.padding(horizontal = 7.dp, vertical = 2.dp)
                    )
                }
            }
        }
    }
}

@Composable
private fun ClawGridBottomNavBar(
    activeTab: ClawGridTab,
    eventCount: Int,
    onSelectTab: (ClawGridTab) -> Unit
) {
    val items = listOf(
        Triple(ClawGridTab.RELAYS, Icons.Filled.Lightbulb to Icons.Outlined.Lightbulb, "Relays"),
        Triple(ClawGridTab.FLOORPLAN, Icons.Filled.Dashboard to Icons.Outlined.Dashboard, "4-Room Lab"),
        Triple(ClawGridTab.OVERVIEW, Icons.Filled.Home to Icons.Outlined.Home, "Overview"),
        Triple(ClawGridTab.TELEMETRY, Icons.Filled.ShowChart to Icons.Outlined.ShowChart, "Telemetry"),
        Triple(ClawGridTab.SAFETY_CLOUD, Icons.Filled.Security to Icons.Outlined.Security, "Safety & Cloud")
    )

    NavigationBar(
        containerColor = ClawBgSidebar,
        contentColor = ClawTextMain,
        windowInsets = WindowInsets.navigationBars,
        modifier = Modifier.border(1.dp, ClawBorder)
    ) {
        items.forEach { (tab, icons, label) ->
            val selected = activeTab == tab
            NavigationBarItem(
                selected = selected,
                onClick = { onSelectTab(tab) },
                icon = {
                    Icon(
                        imageVector = if (selected) icons.first else icons.second,
                        contentDescription = label
                    )
                },
                label = {
                    Text(
                        text = label,
                        fontSize = 10.5.sp,
                        fontWeight = if (selected) FontWeight.Bold else FontWeight.Medium,
                        maxLines = 1
                    )
                },
                colors = NavigationBarItemDefaults.colors(
                    selectedIconColor = ClawSky,
                    selectedTextColor = ClawSky,
                    indicatorColor = Color(0xFF172554),
                    unselectedIconColor = ClawTextMuted,
                    unselectedTextColor = ClawTextDim
                ),
                modifier = Modifier.testTag("bottom_nav_${tab.route}")
            )
        }
    }
}

@Composable
private fun NotificationsBottomSheetContent(
    events: List<AuditEventEntity>,
    supabaseStatus: String
) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 18.dp, vertical = 12.dp),
        verticalArrangement = Arrangement.spacedBy(10.dp)
    ) {
        Text(
            text = "SafetyKernel & Cloud Alerts",
            style = MaterialTheme.typography.titleMedium,
            color = ClawTextMain,
            fontWeight = FontWeight.Bold
        )
        Text(
            text = supabaseStatus,
            style = MaterialTheme.typography.bodySmall,
            color = ClawSky
        )
        HorizontalDivider(color = ClawBorder)
        events.take(8).forEach { evt ->
            Surface(
                color = ClawBgControl,
                shape = RoundedCornerShape(8.dp),
                border = BorderStroke(1.dp, ClawBorder),
                modifier = Modifier.fillMaxWidth()
            ) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(10.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Column(modifier = Modifier.weight(1f)) {
                        Text(
                            text = "${evt.timestamp} · ${evt.room} · ${evt.eventType}",
                            fontFamily = JetBrainsMonoFontFamily,
                            fontSize = 10.5.sp,
                            color = ClawSky
                        )
                        Text(
                            text = evt.description,
                            style = MaterialTheme.typography.bodySmall,
                            color = ClawTextMuted
                        )
                    }
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
        Spacer(modifier = Modifier.height(16.dp))
    }
}
