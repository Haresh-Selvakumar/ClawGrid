package com.example.data.model

import androidx.room.Entity
import androidx.room.PrimaryKey

enum class TelemetryMode(val label: String, val badgeText: String) {
    HARDWARE_NODE_A("Hardware (Node A)", "HARDWARE (NODE A)"),
    SIMULATION_4_ROOM("4-Room Simulation", "SYNTHETIC SIMULATION")
}

enum class OccupancyState(val label: String) {
    OCCUPIED("OCCUPIED"),
    LIKELY_OCCUPIED("LIKELY_OCCUPIED"),
    VACANT("VACANT"),
    UNKNOWN("UNKNOWN")
}

enum class ActuatorAction {
    ON, OFF, AUTO
}

enum class ClawGridTab(val route: String, val title: String, val section: String) {
    RELAYS("relays", "Rooms & Relays", "HOME AUTOMATION"),
    FLOORPLAN("floorplan", "4-Room Lab", "HOME AUTOMATION"),
    OVERVIEW("overview", "Home Overview", "HOME AUTOMATION"),
    TELEMETRY("telemetry", "Live Telemetry", "HOME AUTOMATION"),
    SAFETY_CLOUD("safety_cloud", "Safety, RTOS & Cloud", "SAFETY & DIAGNOSTICS")
}

data class RoomState(
    val id: String,
    val name: String,
    val zoneLabel: String,
    val sensorId: String,
    val state: OccupancyState,
    val tempC: Double,
    val lux: Int,
    val pirEnabled: Boolean = true,
    val presenceEnabled: Boolean = true,
    val doorOpen: Boolean = false,
    val lightOn: Boolean = false,
    val fanOn: Boolean = false,
    val autoLight: Boolean = true,
    val autoFan: Boolean = false,
    val manualLightHoldUntilMs: Long = 0L,
    val manualFanHoldUntilMs: Long = 0L,
    val lastLightTransitionMs: Long = 0L,
    val lastFanTransitionMs: Long = 0L,
    val reportedKnown: Boolean = true,
    val dwellStartMs: Long = 0L
) {
    fun lightModeStatus(nowMs: Long, luxThreshold: Int): String {
        return if (nowMs < manualLightHoldUntilMs) {
            val remSec = ((manualLightHoldUntilMs - nowMs + 999L) / 1000L).coerceAtLeast(0L)
            "Manual ${if (lightOn) "ON" else "OFF"} (Hold: ${remSec}s)"
        } else if (autoLight) {
            "${if (lightOn) "ON" else "OFF"} · Auto (Lux < $luxThreshold)"
        } else {
            "${if (lightOn) "ON" else "OFF"} · Manual"
        }
    }

    fun fanModeStatus(nowMs: Long, tempThreshold: Double): String {
        return if (nowMs < manualFanHoldUntilMs) {
            val remSec = ((manualFanHoldUntilMs - nowMs + 999L) / 1000L).coerceAtLeast(0L)
            "Manual ${if (fanOn) "ON" else "OFF"} (Hold: ${remSec}s)"
        } else if (autoFan) {
            "${if (fanOn) "ON" else "OFF"} · Auto (Temp ≥ ${"%.1f".format(tempThreshold)}°C)"
        } else {
            "${if (fanOn) "ON" else "OFF"} · Manual ${if (fanOn) "ON" else "OFF"}"
        }
    }
}

data class CountdownTimerItem(
    val id: Long,
    val roomId: String,
    val roomName: String,
    val device: String, // "light" or "fan"
    val durationMinutes: Int,
    val targetTimeMs: Long
)

@Entity(tableName = "audit_events")
data class AuditEventEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val timestamp: String,
    val epochMs: Long,
    val room: String,
    val eventType: String,
    val severity: String, // "INFO", "WARN", "ALARM"
    val source: String,   // "REAL_TIME", "SYNTHETIC", "SUPABASE"
    val description: String,
    val syncedToSupabase: Boolean = false
)

@Entity(tableName = "room_snapshots")
data class RoomSnapshotEntity(
    @PrimaryKey val id: String,
    val name: String,
    val zoneLabel: String,
    val state: String,
    val tempC: Double,
    val lux: Int,
    val pirEnabled: Boolean,
    val presenceEnabled: Boolean,
    val doorOpen: Boolean,
    val lightOn: Boolean,
    val fanOn: Boolean,
    val autoLight: Boolean,
    val autoFan: Boolean,
    val manualLightHoldUntilMs: Long,
    val manualFanHoldUntilMs: Long
)

data class PolicyConfig(
    val graceSeconds: Double = 5.0,
    val dwellSeconds: Double = 3.0,
    val manualHoldMinutes: Double = 5.0,
    val luxThreshold: Int = 300,
    val tempThreshold: Double = 28.0,
    val hardwareUrl: String = "http://192.168.4.1",
    val hardwareToken: String = ""
)

data class RtosTaskRow(
    val taskName: String,
    val coreAndPrio: String,
    val period: String,
    val minStackFree: String,
    val runtimeJobs: Long,
    val wdtStatus: String,
    val wdtHealthy: Boolean,
    val missedDeadlines: Int
)

data class RtosHealthState(
    val nodeABootCount: Int = 1,
    val nodeAUptimeSec: Long = 42L,
    val wdtNominal: Boolean = true,
    val wdtBadgeText: String = "Task WDT Nominal (5.0s)",
    val tier1Status: String = "ARMED",
    val tier2Status: String = "ONLINE",
    val tier3Status: String = "ACTIVE BACKUP",
    val dwellBadgeText: String = "ENFORCED (3.0s)",
    val sramFreeKib: Double = 184.2,
    val minFreeSramKib: Double = 142.0,
    val psramFreeKib: Int = 7812,
    val tasks: List<RtosTaskRow> = listOf(
        RtosTaskRow("ControlTask (Node A)", "Core 1 · P5", "5 ms", "3,480 B", 128492L, "Subscribed (5.0s)", true, 0),
        RtosTaskRow("LinkTask (Node A/B)", "Core 0 · P3", "5 ms", "2,912 B", 64120L, "Subscribed (5.0s)", true, 0),
        RtosTaskRow("SpeechTask (Node B)", "Core 1 · P4", "Event", "8,192 B", 1420L, "Bounded Job Timeout", true, 0),
        RtosTaskRow("Pi Coordinator Bridge", "Host · Async", "250 ms", "N/A (Linux/Host)", 4210L, "Standby / Active Hub", true, 0)
    )
)

data class HardwareRealtimeTelemetry(
    val connected: Boolean = false,
    val freshnessMs: Int = Int.MAX_VALUE,
    val stale: Boolean = true,
    val bedroomOccupied: Boolean = false,
    val lightOn: Boolean = false,
    val fanOn: Boolean = false,
    val outputsInhibited: Boolean = true,
    val faultCount: Int = 0
)

data class TelemetryPoint(
    val timestampMs: Long,
    val avgTempC: Double,
    val avgLux: Double,
    val roomTemps: List<Double>,
    val roomLuxes: List<Int>,
    val occupiedCount: Int,
    val energizedRelays: Int
)

data class SensorDiagnosticRow(
    val sensorId: String,
    val roomId: String,
    val roomName: String,
    val type: String,
    val latestValue: String,
    val freshness: String,
    val isOnline: Boolean,
    val healthStatus: String,
    val canToggleFault: Boolean
)

data class SupabaseSyncStatus(
    val projectUrl: String = "https://ozoizpxxnqtxywpgsgov.supabase.co",
    val projectRef: String = "ozoizpxxnqtxywpgsgov",
    val isConfigured: Boolean = true,
    val isConnected: Boolean = false,
    val isSyncing: Boolean = false,
    val wsConnected: Boolean = false,
    val wsChannelJoined: Boolean = false,
    val wsRttMs: Int = 0,
    val txPackets: Int = 0,
    val rxPackets: Int = 0,
    val autoRealtimeSync: Boolean = false,
    val storageBucketReady: Boolean = false,
    val postgrestTablesFound: List<String> = emptyList(),
    val lastSyncTime: String = "Connecting…",
    val lastRevisionIso: String = "",
    val lastNetworkEvent: String = "Opening wss://ozoizpxxnqtxywpgsgov.supabase.co/realtime/v1/websocket…",
    val syncedRoomsCount: Int = 0,
    val syncedEventsCount: Int = 0,
    val statusMessage: String = "Connecting to Supabase Realtime & Storage (ozoizpxxnqtxywpgsgov)…"
)

data class ToastMessage(
    val id: Long = System.currentTimeMillis(),
    val title: String,
    val message: String,
    val severity: String = "INFO" // "INFO", "SUCCESS", "WARN", "DANGER"
)
