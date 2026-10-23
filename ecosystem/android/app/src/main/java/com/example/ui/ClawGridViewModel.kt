package com.example.ui

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.example.data.local.ClawGridDatabase
import com.example.data.model.ActuatorAction
import com.example.data.model.AuditEventEntity
import com.example.data.model.ClawGridTab
import com.example.data.model.CountdownTimerItem
import com.example.data.model.HardwareRealtimeTelemetry
import com.example.data.model.OccupancyState
import com.example.data.model.PolicyConfig
import com.example.data.model.RoomSnapshotEntity
import com.example.data.model.RoomState
import com.example.data.model.RtosHealthState
import com.example.data.model.SensorDiagnosticRow
import com.example.data.model.SupabaseSyncStatus
import com.example.data.model.TelemetryMode
import com.example.data.model.TelemetryPoint
import com.example.data.model.ToastMessage
import com.example.data.remote.HardwareRepository
import com.example.data.remote.HttpBoardApi
import com.example.data.remote.HardwareState
import com.example.data.remote.RequestPhase
import com.example.data.remote.WifiGatewayApis
import com.example.data.remote.GatewayAddress
import com.example.data.remote.LocalVoiceRepository
import com.example.data.remote.PhonePcmRecorder
import com.example.domain.SafetyKernelEngine
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import org.json.JSONObject
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale
import kotlin.math.cos
import kotlin.math.sin

data class ClawGridUiState(
    val activeTab: ClawGridTab = ClawGridTab.RELAYS,
    val mode: TelemetryMode = TelemetryMode.HARDWARE_NODE_A,
    val nowMs: Long = System.currentTimeMillis(),
    val rooms: List<RoomState> = emptyList(),
    val avatarX: Float = 180f, // Normalized 0..800 canvas X
    val avatarY: Float = 140f, // Normalized 0..560 canvas Y
    val selectedSimRoomIndex: Int = 0,
    val isRoomTourRunning: Boolean = false,
    val timers: List<CountdownTimerItem> = emptyList(),
    val policy: PolicyConfig = PolicyConfig(),
    val rtosHealth: RtosHealthState = RtosHealthState(),
    val hardwareTelemetry: HardwareRealtimeTelemetry = HardwareRealtimeTelemetry(),
    val telemetryHistory: List<TelemetryPoint> = emptyList(),
    val chartWindowSeconds: Int = 60,
    val telemetryRoomFilterIndex: Int = -1, // -1 = All Rooms (Avg), 0..3 = specific room
    val voiceInputText: String = "",
    val voiceStatusBadge: String = "Local text parser; microphone recognition unvalidated",
    val terminalOutput: String = "ClawGrid V2.0 initialized. SafetyKernel enforcing 3.0s minimum dwell, 5m manual hold, and zero mains AC.",
    val supabaseStatus: SupabaseSyncStatus = SupabaseSyncStatus(),
    val customSupabaseUrl: String = "https://ozoizpxxnqtxywpgsgov.supabase.co",
    val logSearchQuery: String = "",
    val logRoomFilter: String = "ALL",
    val logSeverityFilter: String = "ALL",
    val logSourceFilter: String = "ALL",
    val unreadAlertsCount: Int = 0,
    val hardwareState: HardwareState = HardwareState(),
    val voiceReady: Boolean = false,
    val voiceBusy: Boolean = false,
    val voiceRecording: Boolean = false,
    val voiceAwaitingRelease: Boolean = false,
    val voiceMessage: String = "First complete manual light ACK + board readback for this boot",
    val activeToast: ToastMessage? = null
)

class ClawGridViewModel(application: Application) : AndroidViewModel(application) {

    private val db = ClawGridDatabase.getInstance(application)
    private val auditDao = db.auditEventDao()
    private val roomDao = db.roomSnapshotDao()
    private var gatewayApis = WifiGatewayApis(application, GatewayAddress.DEFAULT, "")
    private var hardware = HardwareRepository(gatewayApis, android.os.SystemClock::elapsedRealtime)
    private var voice = LocalVoiceRepository(gatewayApis, hardware)
    private var recorder: PhonePcmRecorder? = null
    private var voiceJob: kotlinx.coroutines.Job? = null
    private var voiceRelease: kotlinx.coroutines.CompletableDeferred<Unit>? = null
    private var hardwareCollection: kotlinx.coroutines.Job? = null
    private var simulationState: ClawGridUiState? = null
    private var gatewaySettings = PolicyConfig()

    val auditEvents: StateFlow<List<AuditEventEntity>> = auditDao.observeEvents()
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    private val _uiState = MutableStateFlow(createInitialUiState())
    val uiState: StateFlow<ClawGridUiState> = _uiState.asStateFlow()

    private var tourStepIndex = 0
    private var tickCounter = 0L

    init {
        bindHardware()
        startDeterministicTickLoop()
        viewModelScope.launch {
            while (true) {
                if (_uiState.value.mode == TelemetryMode.HARDWARE_NODE_A) hardware.poll()
                delay(500)
            }
        }
    }

    private fun bindHardware() {
        hardwareCollection?.cancel()
        hardwareCollection = viewModelScope.launch {
            hardware.state.collect { hw ->
                if (_uiState.value.mode == TelemetryMode.HARDWARE_NODE_A) renderHardware(hw)
            }
        }
    }

    private fun renderHardware(hw: HardwareState) {
        val fresh = hw.connected && hw.sample?.fresh(android.os.SystemClock.elapsedRealtime()) == true
        val sample = hw.sample
        _uiState.update { state -> state.copy(
            hardwareState = hw,
            voiceReady = hardware.voiceReady(),
            rooms = state.rooms.map { room -> room.copy(
                state = if (fresh && room.id == "bedroom") runCatching { OccupancyState.valueOf(sample!!.occupancy) }.getOrDefault(OccupancyState.UNKNOWN) else OccupancyState.UNKNOWN,
                tempC = Double.NaN, lux = -1, reportedKnown = fresh && room.id == "bedroom",
                lightOn = fresh && room.id == "bedroom" && sample!!.light,
                fanOn = fresh && room.id == "bedroom" && sample!!.fan,
                autoLight = false, autoFan = false, manualLightHoldUntilMs = 0, manualFanHoldUntilMs = 0
            ) },
            telemetryHistory = emptyList(), timers = emptyList(), isRoomTourRunning = false,
            hardwareTelemetry = HardwareRealtimeTelemetry(
                connected = fresh, stale = !fresh,
                freshnessMs = sample?.ageAt(android.os.SystemClock.elapsedRealtime())?.coerceAtMost(Int.MAX_VALUE.toLong())?.toInt() ?: Int.MAX_VALUE,
                bedroomOccupied = fresh && sample!!.occupancy == "OCCUPIED",
                lightOn = fresh && sample!!.light, fanOn = fresh && sample!!.fan,
                outputsInhibited = sample?.outputsEnabled != true
            ),
            rtosHealth = unknownRtos(), terminalOutput = hw.message
        ) }
    }

    private fun unknownRtos() = RtosHealthState(nodeABootCount = 0, nodeAUptimeSec = 0,
        wdtNominal = false, wdtBadgeText = "UNAVAILABLE - no A task diagnostics",
        tier1Status = "UNKNOWN", tier2Status = "UNKNOWN", tier3Status = "UNAVAILABLE",
        sramFreeKib = Double.NaN, minFreeSramKib = Double.NaN, psramFreeKib = 0, tasks = emptyList())

    fun confirmHardwareLight() { viewModelScope.launch { hardware.confirm() } }
    fun cancelHardwareProposal() { hardware.cancelProposal() }

    fun startPhoneVoice() {
        if (_uiState.value.mode != TelemetryMode.HARDWARE_NODE_A || !hardware.voiceReady() || voiceJob?.isActive == true) return
        val capture = PhonePcmRecorder(); recorder = capture
        val released = kotlinx.coroutines.CompletableDeferred<Unit>(); voiceRelease = released
        _uiState.update { it.copy(voiceRecording = true, voiceBusy = true, voiceMessage = "Recording phone microphone; release to send, maximum four seconds") }
        voiceJob = viewModelScope.launch {
            var pcm: ByteArray? = null
            try {
                pcm = capture.record()
                if (!released.isCompleted) {
                    _uiState.update { it.copy(voiceRecording = false, voiceAwaitingRelease = true, voiceMessage = "Four-second capture cap reached; microphone stopped. Release to send or Cancel to discard") }
                    released.await()
                }
                _uiState.update { it.copy(voiceRecording = false, voiceAwaitingRelease = false, voiceMessage = "Sending PCM to ESP32 over local Wi-Fi; waiting for on-device recognition") }
                val message = voice.propose(pcm)
                _uiState.update { it.copy(voiceMessage = message) }
            } catch (e: kotlinx.coroutines.CancellationException) { throw e
            } catch (e: Exception) {
                _uiState.update { it.copy(voiceMessage = "Voice stopped: ${e.message ?: "capture/connection failed"}. No automatic retry or confirmation") }
            } finally {
                pcm?.fill(0)
                if (recorder === capture) {
                    recorder = null; voiceRelease = null
                    _uiState.update { it.copy(voiceRecording = false, voiceAwaitingRelease = false, voiceBusy = false) }
                }
            }
        }
    }

    fun stopPhoneVoice() { recorder?.stop(); voiceRelease?.complete(Unit) }
    fun cancelPhoneVoice() {
        voice.cancel(); recorder?.cancel(); voiceJob?.cancel()
        if (hardware.state.value.request?.source == "speech") hardware.cancelProposal()
        _uiState.update { it.copy(voiceRecording = false, voiceAwaitingRelease = false, voiceBusy = false, voiceMessage = "Voice cancelled; no automatic retry") }
    }

    private fun createInitialUiState(simulation: Boolean = false): ClawGridUiState {
        val now = System.currentTimeMillis()
        val initialRooms = listOf(
            RoomState(
                id = "bedroom",
                name = "Bedroom",
                zoneLabel = "PIR_1 (Zone 1)",
                sensorId = "PIR_1",
                state = OccupancyState.OCCUPIED,
                tempC = 25.0,
                lux = 120,
                pirEnabled = true,
                presenceEnabled = true,
                doorOpen = false,
                lightOn = false,
                fanOn = false,
                autoLight = false,
                autoFan = false,
                manualLightHoldUntilMs = now + 180_000L,
                manualFanHoldUntilMs = now + 245_000L,
                lastLightTransitionMs = now - 10_000L,
                lastFanTransitionMs = now - 10_000L
            ),
            RoomState(
                id = "living_room",
                name = "Living Room",
                zoneLabel = "Virtual Zone 2",
                sensorId = "PIR_2",
                state = OccupancyState.VACANT,
                tempC = 24.0,
                lux = 280,
                pirEnabled = true,
                presenceEnabled = true,
                doorOpen = false,
                lightOn = false,
                fanOn = false,
                autoLight = false,
                autoFan = false
            ),
            RoomState(
                id = "kitchen",
                name = "Kitchen",
                zoneLabel = "Virtual Zone 3",
                sensorId = "PIR_3",
                state = OccupancyState.VACANT,
                tempC = 29.5,
                lux = 450,
                pirEnabled = true,
                presenceEnabled = true,
                doorOpen = false,
                lightOn = false,
                fanOn = false,
                autoLight = false,
                autoFan = false
            ),
            RoomState(
                id = "study",
                name = "Study",
                zoneLabel = "Virtual Zone 4",
                sensorId = "PIR_4",
                state = OccupancyState.VACANT,
                tempC = 22.8,
                lux = 95,
                pirEnabled = true,
                presenceEnabled = true,
                doorOpen = false,
                lightOn = false,
                fanOn = false,
                autoLight = false,
                autoFan = false,
                manualLightHoldUntilMs = now + 178_000L
            )
        )

        val seedHistory = (0 until 45).map { idx ->
            val t = now - (45 - idx) * 1000L
            val wave = sin(idx * 0.25)
            TelemetryPoint(
                timestampMs = t,
                avgTempC = 25.3 + wave * 0.4,
                avgLux = 236.0 + cos(idx * 0.2) * 12.0,
                roomTemps = listOf(25.0 + wave * 0.3, 24.0, 29.5 - wave * 0.2, 22.8),
                roomLuxes = listOf(120, 280, 450, 95),
                occupiedCount = 1,
                energizedRelays = 0
            )
        }

        return ClawGridUiState(
            nowMs = now,
            mode = if (simulation) TelemetryMode.SIMULATION_4_ROOM else TelemetryMode.HARDWARE_NODE_A,
            rooms = if (simulation) initialRooms else initialRooms.map { it.copy(state = OccupancyState.UNKNOWN,
                tempC = Double.NaN, lux = -1, lightOn = false, fanOn = false, autoLight = false, autoFan = false,
                manualLightHoldUntilMs = 0, manualFanHoldUntilMs = 0, reportedKnown = false) },
            telemetryHistory = if (simulation) seedHistory else emptyList(),
            rtosHealth = if (simulation) RtosHealthState() else unknownRtos(),
            customSupabaseUrl = "",
            supabaseStatus = SupabaseSyncStatus(
                projectUrl = "",
                isConfigured = false,
                statusMessage = "Connecting to Supabase Realtime WebSocket & Storage…"
            )
        )
    }





    private fun startDeterministicTickLoop() {
        viewModelScope.launch {
            while (true) {
                delay(500L)
                tickCounter++
                val now = System.currentTimeMillis()
                val state = _uiState.value

                if (state.mode == TelemetryMode.HARDWARE_NODE_A) {
                    hardware.age()
                    renderHardware(hardware.state.value)
                    _uiState.update { it.copy(nowMs = now) }
                    continue
                }

                // Simulation-only state source. No network writes or hardware policy evaluation.
                // 1. Check expired countdown timers
                val expiredTimers = state.timers.filter { now >= it.targetTimeMs }
                val remainingTimers = state.timers.filter { now < it.targetTimeMs }
                if (expiredTimers.isNotEmpty()) {
                    expiredTimers.forEach { tm ->
                        setActuator(tm.roomId, tm.device, ActuatorAction.OFF, fromTimer = true)
                        recordAuditEvent(
                            room = tm.roomName,
                            eventType = "TIMER_EXPIRED",
                            severity = "INFO",
                            description = "Auto-off countdown timer (${tm.durationMinutes}m) expired for ${tm.device}. Commanded logical OFF."
                        )
                        showToast(
                            "Timer Expired",
                            "${tm.roomName} ${tm.device.uppercase()} turned OFF by scheduler.",
                            "INFO"
                        )
                    }
                }

                // 2. Room tour step every 3.0 seconds (6 ticks) when active
                if (state.isRoomTourRunning && tickCounter % 6L == 0L) {
                    tourStepIndex = (tourStepIndex + 1) % 4
                    jumpAvatarToRoom(tourStepIndex, silentToast = true)
                }

                // 3. Evaluate Occupancy & SafetyKernel policies across all 4 rooms
                val avatarRoomIdx = roomIndexAtPosition(_uiState.value.avatarX, _uiState.value.avatarY)
                val updatedRooms = _uiState.value.rooms.mapIndexed { idx, room ->
                    SafetyKernelEngine.evaluateRoomPolicy(
                        room = room,
                        isAvatarHere = idx == avatarRoomIdx,
                        policy = state.policy,
                        nowMs = now
                    )
                }

                // 4. Update telemetry waveform point every 1 second (2 ticks)
                val updatedHistory = if (tickCounter % 2L == 0L) {
                    val jitter = sin(tickCounter * 0.3) * 0.15
                    val avgT = updatedRooms.map { it.tempC }.average() + jitter
                    val avgL = updatedRooms.map { it.lux }.average() + (cos(tickCounter * 0.25) * 3.0)
                    val occCount = updatedRooms.count { it.state == OccupancyState.OCCUPIED }
                    val relays = updatedRooms.sumOf { (if (it.lightOn) 1 else 0) + (if (it.fanOn) 1 else 0) }
                    val pt = TelemetryPoint(
                        timestampMs = now,
                        avgTempC = avgT,
                        avgLux = avgL,
                        roomTemps = updatedRooms.map { it.tempC + jitter },
                        roomLuxes = updatedRooms.map { it.lux },
                        occupiedCount = occCount,
                        energizedRelays = relays
                    )
                    (state.telemetryHistory + pt).takeLast(900)
                } else {
                    state.telemetryHistory
                }

                // 5. Update RTOS counters & Hardware freshness
                val rtos = state.rtosHealth
                val updatedTasks = rtos.tasks.mapIndexed { idx, t ->
                    when (idx) {
                        0 -> if (rtos.wdtNominal) t.copy(runtimeJobs = t.runtimeJobs + 100) else t
                        1 -> t.copy(runtimeJobs = t.runtimeJobs + 100)
                        3 -> t.copy(runtimeJobs = t.runtimeJobs + 2)
                        else -> t
                    }
                }
                val updatedRtos = rtos.copy(
                    nodeAUptimeSec = if (tickCounter % 2L == 0L) rtos.nodeAUptimeSec + 1 else rtos.nodeAUptimeSec,
                    tasks = updatedTasks
                )

                val bed = updatedRooms.first()
                val jitterMs = (18 + (tickCounter % 14).toInt())
                val updatedHw = state.hardwareTelemetry.copy(
                    freshnessMs = jitterMs,
                    bedroomOccupied = bed.state == OccupancyState.OCCUPIED,
                    lightOn = bed.lightOn,
                    fanOn = bed.fanOn
                )

                _uiState.update {
                    it.copy(
                        nowMs = now,
                        rooms = updatedRooms,
                        timers = remainingTimers,
                        telemetryHistory = updatedHistory,
                        rtosHealth = updatedRtos,
                        hardwareTelemetry = updatedHw
                    )
                }


            }
        }
    }

    private suspend fun persistRoomsLocally(rooms: List<RoomState>) {
        roomDao.upsertRooms(
            rooms.map { r ->
                RoomSnapshotEntity(
                    id = r.id,
                    name = r.name,
                    zoneLabel = r.zoneLabel,
                    state = r.state.name,
                    tempC = r.tempC,
                    lux = r.lux,
                    pirEnabled = r.pirEnabled,
                    presenceEnabled = r.presenceEnabled,
                    doorOpen = r.doorOpen,
                    lightOn = r.lightOn,
                    fanOn = r.fanOn,
                    autoLight = r.autoLight,
                    autoFan = r.autoFan,
                    manualLightHoldUntilMs = r.manualLightHoldUntilMs,
                    manualFanHoldUntilMs = r.manualFanHoldUntilMs
                )
            }
        )
    }

    fun selectTab(tab: ClawGridTab) {
        if (_uiState.value.activeTab != tab) cancelPhoneVoice()
        _uiState.update { it.copy(activeTab = tab) }
    }

    fun switchTelemetryMode(mode: TelemetryMode) {
        val previous = _uiState.value
        if (previous.mode == mode) return
        cancelPhoneVoice()
        hardware.reset()
        if (previous.mode == TelemetryMode.SIMULATION_4_ROOM) simulationState = previous
        val next = if (mode == TelemetryMode.SIMULATION_4_ROOM) simulationState ?: createInitialUiState(true)
            else createInitialUiState().copy(policy = gatewaySettings)
        _uiState.value = next.copy(activeTab = previous.activeTab, mode = mode)
        if (mode == TelemetryMode.HARDWARE_NODE_A) renderHardware(hardware.state.value)
    }

    fun toggleAutoRealtimeSync() { cloudUnavailable() }

    fun reconnectRealtimeWebSocket() { cloudUnavailable() }

    fun triggerLiveCloudBroadcastDemo() { cloudUnavailable() }

    private fun broadcastRealtimeDelta(eventName: String, extra: JSONObject = JSONObject()) { /* Simulation stays local. */ }

    fun setActuator(
        roomId: String,
        device: String,
        action: ActuatorAction,
        fromTimer: Boolean = false
    ) {
        if (_uiState.value.mode == TelemetryMode.HARDWARE_NODE_A) {
            if (roomId != "bedroom" || device != "light" || action == ActuatorAction.AUTO || fromTimer) {
                showToast("Not supported in this milestone", "Hardware slice: Bedroom light manual ON/OFF. AUTO/policy semantics remain unchanged.", "WARN")
                return
            }
            cancelPhoneVoice()
            viewModelScope.launch { hardware.prepare(action == ActuatorAction.ON) }
            return
        }
        val now = System.currentTimeMillis()
        val state = _uiState.value
        val targetRoom = state.rooms.find { it.id == roomId } ?: return
        val outcome = SafetyKernelEngine.evaluateActuation(
            room = targetRoom,
            device = device,
            action = action,
            policy = state.policy,
            nowMs = now
        )

        val newRooms = state.rooms.map { if (it.id == roomId) outcome.updatedRoom else it }
        _uiState.update {
            it.copy(
                nowMs = now,
                rooms = newRooms,
                terminalOutput = outcome.terminalMessage
            )
        }

        if (!fromTimer) {
            showToast(outcome.toastTitle, outcome.toastMessage, outcome.severity)
            recordAuditEvent(
                room = targetRoom.name,
                eventType = if (outcome.accepted) "RELAY_${action.name}" else "SAFETY_DWELL_LOCK",
                severity = if (outcome.accepted) "INFO" else "WARN",
                description = outcome.terminalMessage
            )
            broadcastRealtimeDelta(
                "relay_actuation",
                JSONObject()
                    .put("room", roomId)
                    .put("device", device)
                    .put("action", action.name)
                    .put("accepted", outcome.accepted)
            )
        }

        viewModelScope.launch {
            persistRoomsLocally(newRooms)

        }
    }

    fun applyScene(sceneName: String) {
        if (_uiState.value.mode != TelemetryMode.SIMULATION_4_ROOM) return
        val now = System.currentTimeMillis()
        val state = _uiState.value
        val avatarRoomIdx = roomIndexAtPosition(state.avatarX, state.avatarY)

        val updatedRooms = state.rooms.mapIndexed { idx, r ->
            val modified = when (sceneName) {
                "HOME" -> r.copy(
                    autoLight = true,
                    autoFan = true,
                    manualLightHoldUntilMs = 0L,
                    manualFanHoldUntilMs = 0L
                )
                "AWAY" -> r.copy(
                    lightOn = false,
                    fanOn = false,
                    autoLight = false,
                    autoFan = false,
                    manualLightHoldUntilMs = 0L,
                    manualFanHoldUntilMs = 0L
                )
                "SLEEP" -> r.copy(
                    lightOn = false,
                    autoLight = false,
                    manualLightHoldUntilMs = 0L,
                    fanOn = if (idx == 0) r.fanOn else false,
                    autoFan = idx == 0,
                    manualFanHoldUntilMs = 0L
                )
                "ALL_OFF" -> r.copy(
                    lightOn = false,
                    fanOn = false,
                    autoLight = false,
                    autoFan = false,
                    manualLightHoldUntilMs = 0L,
                    manualFanHoldUntilMs = 0L
                )
                else -> r
            }
            SafetyKernelEngine.evaluateRoomPolicy(modified, idx == avatarRoomIdx, state.policy, now)
        }

        val (terminalMsg, toastTitle, toastBody, sev) = when (sceneName) {
            "HOME" -> Quad(
                "[SCENE HOME] Automatic occupancy policies enabled across all 4 rooms.",
                "Scene: Resume All AUTO",
                "All rooms restored to sensor-driven automatic policies.",
                "SUCCESS"
            )
            "AWAY" -> Quad(
                "[SCENE AWAY] All loads turned OFF. Auto policies paused.",
                "Scene: Away Mode",
                "All lights and fans turned OFF.",
                "INFO"
            )
            "SLEEP" -> Quad(
                "[SCENE SLEEP] All lights turned OFF. Bedroom fan primed in AUTO.",
                "Scene: Sleep Mode",
                "All house lights OFF; Bedroom fan in AUTO.",
                "INFO"
            )
            else -> Quad(
                "[SCENE ALL_OFF] EMERGENCY SHUTOFF: All 8 relays commanded to logical OFF.",
                "Emergency All Off",
                "Every relay in all 4 rooms commanded to logical OFF.",
                "DANGER"
            )
        }

        _uiState.update {
            it.copy(
                nowMs = now,
                rooms = updatedRooms,
                terminalOutput = terminalMsg
            )
        }
        showToast(toastTitle, toastBody, sev)
        recordAuditEvent(
            room = "House",
            eventType = "SCENE_$sceneName",
            severity = if (sceneName == "ALL_OFF") "ALARM" else "INFO",
            description = terminalMsg
        )
        broadcastRealtimeDelta("scene_apply", JSONObject().put("scene", sceneName))
        viewModelScope.launch {
            persistRoomsLocally(updatedRooms)
            if (_uiState.value.supabaseStatus.autoRealtimeSync) {
                syncWithSupabaseCloud(silent = true)
            }
        }
    }

    private data class Quad(val a: String, val b: String, val c: String, val d: String)

    fun scheduleCountdownTimer(roomId: String, device: String, minutes: Int) {
        if (_uiState.value.mode != TelemetryMode.SIMULATION_4_ROOM) return
        val validMins = minutes.coerceIn(1, 1440)
        val room = _uiState.value.rooms.find { it.id == roomId } ?: return
        val now = System.currentTimeMillis()
        val item = CountdownTimerItem(
            id = now,
            roomId = roomId,
            roomName = room.name,
            device = device,
            durationMinutes = validMins,
            targetTimeMs = now + validMins * 60_000L
        )
        _uiState.update { it.copy(timers = it.timers + item) }
        recordAuditEvent(
            room = room.name,
            eventType = "TIMER_SCHEDULED",
            severity = "INFO",
            description = "Scheduled ${validMins}m auto-off countdown timer for ${room.name} ${if (device == "light") "Ceiling Light" else "Ceiling Fan"}."
        )
        showToast(
            "Timer Scheduled",
            "${room.name} ${device.uppercase()} will turn OFF in $validMins min.",
            "SUCCESS"
        )
    }

    fun clearAllTimers() {
        _uiState.update { it.copy(timers = emptyList()) }
        recordAuditEvent(
            room = "System",
            eventType = "TIMERS_CLEARED",
            severity = "INFO",
            description = "Cleared all active auto-off countdown timers."
        )
        showToast("Timers Cleared", "All countdown timers cancelled.", "INFO")
    }

    fun roomIndexAtPosition(x: Float, y: Float): Int {
        if (x < 0f || x > 800f || y < 0f || y > 560f) return -1
        return (if (y >= 280f) 2 else 0) + (if (x >= 400f) 1 else 0)
    }

    fun moveAvatarToCanvasPoint(x: Float, y: Float) {
        if (_uiState.value.mode != TelemetryMode.SIMULATION_4_ROOM) return
        val clampedX = x.coerceIn(20f, 780f)
        val clampedY = y.coerceIn(20f, 540f)
        val prevIdx = roomIndexAtPosition(_uiState.value.avatarX, _uiState.value.avatarY)
        val rIdx = roomIndexAtPosition(clampedX, clampedY)
        val now = System.currentTimeMillis()
        val updatedRooms = _uiState.value.rooms.mapIndexed { idx, room ->
            SafetyKernelEngine.evaluateRoomPolicy(room, idx == rIdx, _uiState.value.policy, now)
        }
        _uiState.update {
            it.copy(
                avatarX = clampedX,
                avatarY = clampedY,
                selectedSimRoomIndex = if (rIdx in 0..3) rIdx else it.selectedSimRoomIndex,
                rooms = updatedRooms
            )
        }
        if (rIdx != prevIdx && rIdx in 0..3) {
            val rName = updatedRooms[rIdx].name
            broadcastRealtimeDelta("occupancy_change", JSONObject().put("room", rName).put("state", "OCCUPIED"))
            if (_uiState.value.supabaseStatus.autoRealtimeSync) {
                syncWithSupabaseCloud(silent = true)
            }
        }
    }

    fun nudgeAvatar(dx: Float, dy: Float) {
        if (_uiState.value.mode != TelemetryMode.SIMULATION_4_ROOM) return
        val curX = if (_uiState.value.avatarX < 0f) 180f else _uiState.value.avatarX
        val curY = if (_uiState.value.avatarY < 0f) 140f else _uiState.value.avatarY
        moveAvatarToCanvasPoint(curX + dx, curY + dy)
    }

    fun jumpAvatarToRoom(roomIdx: Int, silentToast: Boolean = false) {
        if (_uiState.value.mode != TelemetryMode.SIMULATION_4_ROOM) return
        val centers = listOf(
            180f to 140f,
            580f to 140f,
            180f to 420f,
            580f to 420f
        )
        if (roomIdx !in 0..3) return
        val (cx, cy) = centers[roomIdx]
        moveAvatarToCanvasPoint(cx, cy)
        val roomName = _uiState.value.rooms[roomIdx].name
        if (!silentToast) {
            recordAuditEvent(
                room = roomName,
                eventType = "AVATAR_MOVE",
                severity = "INFO",
                description = "Avatar localized in $roomName. Occupancy set to OCCUPIED."
            )
            showToast("Moved to $roomName", "Avatar localized in $roomName. Occupancy set to OCCUPIED.", "SUCCESS")
        }
    }

    fun leaveHouse() {
        if (_uiState.value.mode != TelemetryMode.SIMULATION_4_ROOM) return
        val now = System.currentTimeMillis()
        val updatedRooms = _uiState.value.rooms.map { room ->
            SafetyKernelEngine.evaluateRoomPolicy(room, isAvatarHere = false, _uiState.value.policy, now)
        }
        _uiState.update {
            it.copy(
                avatarX = -100f,
                avatarY = -100f,
                isRoomTourRunning = false,
                rooms = updatedRooms
            )
        }
        recordAuditEvent(
            room = "House",
            eventType = "LEAVE_HOUSE",
            severity = "INFO",
            description = "Virtual person left the house. Vacancy grace countdown timers started (${_uiState.value.policy.graceSeconds}s)."
        )
        broadcastRealtimeDelta("leave_house")
        showToast("Left House", "Avatar is Outside. Rooms entering vacancy grace countdown.", "INFO")
    }

    fun toggleRoomTour() {
        if (_uiState.value.mode != TelemetryMode.SIMULATION_4_ROOM) return
        val running = !_uiState.value.isRoomTourRunning
        _uiState.update { it.copy(isRoomTourRunning = running) }
        if (running) {
            tourStepIndex = 0
            jumpAvatarToRoom(0, silentToast = true)
            showToast("Room Tour Started", "Cycling through all 4 rooms every 3 seconds.", "SUCCESS")
        } else {
            showToast("Room Tour Paused", "Manual floorplan control active.", "INFO")
        }
    }

    fun selectSimRoomForEdit(index: Int) {
        if (_uiState.value.mode != TelemetryMode.SIMULATION_4_ROOM) return
        if (index in 0..3) {
            _uiState.update { it.copy(selectedSimRoomIndex = index) }
        }
    }

    fun updateRoomEnvironment(
        roomIndex: Int,
        tempC: Double,
        lux: Int,
        pirEnabled: Boolean,
        presenceEnabled: Boolean,
        doorOpen: Boolean
    ) {
        if (_uiState.value.mode != TelemetryMode.SIMULATION_4_ROOM) return
        if (roomIndex !in 0..3) return
        val now = System.currentTimeMillis()
        val state = _uiState.value
        val avatarIdx = roomIndexAtPosition(state.avatarX, state.avatarY)
        val updatedRooms = state.rooms.mapIndexed { idx, r ->
            if (idx == roomIndex) {
                val mod = r.copy(
                    tempC = tempC.coerceIn(0.0, 60.0),
                    lux = lux.coerceIn(0, 2000),
                    pirEnabled = pirEnabled,
                    presenceEnabled = presenceEnabled,
                    doorOpen = doorOpen
                )
                SafetyKernelEngine.evaluateRoomPolicy(mod, idx == avatarIdx, state.policy, now)
            } else r
        }
        _uiState.update { it.copy(rooms = updatedRooms) }
        val rName = updatedRooms[roomIndex].name
        recordAuditEvent(
            room = rName,
            eventType = "ENV_UPDATE",
            severity = if (!pirEnabled && !presenceEnabled) "WARN" else "INFO",
            description = "Updated $rName environment: ${"%.1f".format(tempC)}°C, $lux lx, PIR=${if (pirEnabled) "ON" else "OFF"}, Presence=${if (presenceEnabled) "ON" else "OFF"}."
        )
        broadcastRealtimeDelta(
            "environment_update",
            JSONObject().put("room", rName).put("tempC", tempC).put("lux", lux)
        )
        if (_uiState.value.supabaseStatus.autoRealtimeSync) {
            syncWithSupabaseCloud(silent = true)
        }
    }

    fun setRoomSensorsEnabled(roomIndex: Int, enabled: Boolean) {
        if (_uiState.value.mode != TelemetryMode.SIMULATION_4_ROOM) return
        if (roomIndex !in 0..3) return
        val r = _uiState.value.rooms[roomIndex]
        updateRoomEnvironment(roomIndex, r.tempC, r.lux, enabled, enabled, r.doorOpen)
        showToast(
            if (enabled) "Sensors Restored" else "Sensors Disabled (UNKNOWN)",
            if (enabled) "${r.name} PIR & Presence sensors online." else "${r.name} occupancy forced to UNKNOWN (fail-safe).",
            if (enabled) "SUCCESS" else "WARN"
        )
    }

    fun resetSimulationDefaults() {
        if (_uiState.value.mode != TelemetryMode.SIMULATION_4_ROOM) return
        val now = System.currentTimeMillis()
        val defaults = listOf(
            Triple(25.0, 120, true),
            Triple(24.0, 280, false),
            Triple(29.5, 450, false),
            Triple(22.8, 95, false)
        )
        val updated = _uiState.value.rooms.mapIndexed { idx, r ->
            val (t, l, occ) = defaults[idx]
            SafetyKernelEngine.evaluateRoomPolicy(
                r.copy(
                    tempC = t,
                    lux = l,
                    pirEnabled = true,
                    presenceEnabled = true,
                    doorOpen = false,
                    state = if (occ) OccupancyState.OCCUPIED else OccupancyState.VACANT,
                    autoLight = true,
                    autoFan = idx == 0,
                    manualLightHoldUntilMs = 0L,
                    manualFanHoldUntilMs = 0L
                ),
                isAvatarHere = occ,
                policy = _uiState.value.policy,
                nowMs = now
            )
        }
        _uiState.update {
            it.copy(
                avatarX = 180f,
                avatarY = 140f,
                selectedSimRoomIndex = 0,
                isRoomTourRunning = false,
                rooms = updated
            )
        }
        recordAuditEvent("House", "SIM_RESET", "INFO", "Simulation state reset to ClawGrid factory defaults.")
        showToast("Simulation Reset", "All 4 rooms restored to default temperatures, lux, and policies.", "SUCCESS")
        if (_uiState.value.supabaseStatus.autoRealtimeSync) {
            syncWithSupabaseCloud(silent = true)
        }
    }

    fun injectTelemetryPulse(type: String) {
        if (_uiState.value.mode != TelemetryMode.SIMULATION_4_ROOM) return
        val now = System.currentTimeMillis()
        val state = _uiState.value
        val avatarIdx = roomIndexAtPosition(state.avatarX, state.avatarY)
        val updatedRooms = state.rooms.mapIndexed { idx, r ->
            val mod = when (type) {
                "heat" -> r.copy(tempC = (r.tempC + 2.5).coerceAtMost(45.0))
                "cool" -> r.copy(tempC = (r.tempC - 2.5).coerceAtLeast(15.0))
                "lux" -> r.copy(lux = (r.lux + 200).coerceAtMost(1800))
                "dark" -> r.copy(lux = (r.lux - 180).coerceAtLeast(15))
                else -> r
            }
            SafetyKernelEngine.evaluateRoomPolicy(mod, idx == avatarIdx, state.policy, now)
        }
        _uiState.update { it.copy(rooms = updatedRooms) }
        val label = when (type) {
            "heat" -> "Heat Pulse (+2.5°C)"
            "cool" -> "Cool Pulse (-2.5°C)"
            "lux" -> "Daylight Pulse (+200 lx)"
            else -> "Dusk Pulse (-180 lx)"
        }
        recordAuditEvent("Telemetry", "SIGNAL_PULSE", "INFO", "Injected $label across 4 zones.")
        broadcastRealtimeDelta("telemetry_pulse", JSONObject().put("pulse", label))
        showToast("Signal Injected", label, "INFO")
    }

    fun setChartWindowSeconds(seconds: Int) {
        _uiState.update { it.copy(chartWindowSeconds = seconds) }
    }

    fun setTelemetryRoomFilter(index: Int) {
        _uiState.update { it.copy(telemetryRoomFilterIndex = index) }
    }

    fun updateVoiceInputText(text: String) {
        _uiState.update { it.copy(voiceInputText = text) }
    }

    fun runVoiceCommand(overrideText: String? = null) {
        val raw = (overrideText ?: _uiState.value.voiceInputText).trim()
        if (overrideText != null) {
            _uiState.update { it.copy(voiceInputText = overrideText) }
        }
        val parsed = SafetyKernelEngine.parseVoiceCommand(raw)
        _uiState.update {
            it.copy(
                voiceStatusBadge = if (it.mode == TelemetryMode.HARDWARE_NODE_A) "Local text intent only; actuation requires confirmation" else parsed.spokenResponse,
                terminalOutput = if (it.mode == TelemetryMode.HARDWARE_NODE_A) "[LOCAL TEXT PARSER] Proposed intent only; no physical switching verified" else "[VOICE PARSER] \"${parsed.rawText}\" -> ${parsed.spokenResponse} (conf=${(parsed.confidence * 100).toInt()}%)"
            )
        }
        when (parsed.intent) {
            "SCENE" -> parsed.sceneName?.let { applyScene(it) }
            "ACTUATE" -> {
                if (parsed.roomId != null && parsed.device != null && parsed.action != null) {
                    recordAuditEvent(
                        room = parsed.roomLabel ?: "House",
                        eventType = "VOICE_INTENT",
                        severity = "INFO",
                        description = "Parsed offline command \"$raw\" -> ${parsed.roomLabel} ${parsed.device.uppercase()} ${parsed.action.name}"
                    )
                    setActuator(parsed.roomId, parsed.device, parsed.action)
                }
            }
            "HELP" -> showToast("Voice Assistant Help", parsed.spokenResponse, "INFO")
            else -> showToast("Clarification Needed", parsed.spokenResponse, "WARN")
        }
    }

    fun simulateWdtStall() {
        if (_uiState.value.mode != TelemetryMode.SIMULATION_4_ROOM) return
        val current = _uiState.value.rtosHealth
        val stalledTasks = current.tasks.mapIndexed { idx, row ->
            if (idx == 0) row.copy(wdtStatus = "STALLED (ESP_RST_TASK_WDT)", wdtHealthy = false, missedDeadlines = row.missedDeadlines + 3)
            else row
        }
        val nextBoot = current.nodeABootCount + 1
        val safeRooms = _uiState.value.rooms.map { it.copy(lightOn = false, fanOn = false) }
        _uiState.update {
            it.copy(
                rooms = safeRooms,
                rtosHealth = current.copy(
                    nodeABootCount = nextBoot,
                    nodeAUptimeSec = 0L,
                    wdtNominal = false,
                    wdtBadgeText = "WDT PANIC REBOOT -> Boot #$nextBoot",
                    tier1Status = "REBOOTED (SAFE OFF)",
                    tasks = stalledTasks
                ),
                terminalOutput = "[ESP_RST_TASK_WDT] ControlTask missed 5.0s watchdog feed! Node A rebooted to Boot #$nextBoot with relays SAFE OFF and NVS policy intact."
            )
        }
        recordAuditEvent(
            room = "Node A",
            eventType = "WDT_PANIC_REBOOT",
            severity = "ALARM",
            description = "ControlTask stall triggered 5.0s hardware watchdog reset. Boot incremented to #$nextBoot; all relays forced to safe OFF."
        )
        broadcastRealtimeDelta("wdt_panic_reboot", JSONObject().put("bootId", nextBoot))
        showToast("Watchdog Panic Reboot", "Node A rebooted (Boot #$nextBoot) with relays SAFE OFF.", "DANGER")
    }

    fun simulateRadioLeaseExpiry() {
        if (_uiState.value.mode != TelemetryMode.SIMULATION_4_ROOM) return
        val current = _uiState.value.rtosHealth
        _uiState.update {
            it.copy(
                rtosHealth = current.copy(
                    wdtBadgeText = "Radio Lease Expired (>3.0s)",
                    tier1Status = "AUTONOMOUS PIR",
                    tier2Status = "LEASE EXPIRED (>3.0s)"
                ),
                terminalOutput = "[SECURITY / ESP-NOW] 3.0s command freshness lease expired. Node A transitioned to autonomous local PIR control."
            )
        }
        recordAuditEvent(
            room = "Node B",
            eventType = "RADIO_LEASE_EXPIRED",
            severity = "WARN",
            description = "ESP-NOW 3.0s freshness gate expired. Node A running autonomous local SafetyKernel."
        )
        showToast("Radio Lease Expired", "Node A switched to autonomous local PIR control.", "WARN")
    }

    fun simulatePiFailover() {
        if (_uiState.value.mode != TelemetryMode.SIMULATION_4_ROOM) return
        val current = _uiState.value.rtosHealth
        _uiState.update {
            it.copy(
                rtosHealth = current.copy(
                    wdtBadgeText = "Pi Backup Coordinator Active",
                    tier3Status = "PRIMARY TAKEOVER"
                ),
                terminalOutput = "[TIER 3 FAILOVER] Raspberry Pi Backup Coordinator assumed active SafetyKernel and multi-room policy authority."
            )
        }
        recordAuditEvent(
            room = "Raspberry Pi",
            eventType = "PI_FAILOVER_ACTIVE",
            severity = "WARN",
            description = "Tier 3 Raspberry Pi Coordinator assumed active SafetyKernel mirror authority."
        )
        showToast("Pi Failover Active", "Raspberry Pi Backup Coordinator is now primary.", "INFO")
    }

    fun restoreNominalRtos() {
        if (_uiState.value.mode != TelemetryMode.SIMULATION_4_ROOM) return
        val current = _uiState.value.rtosHealth
        val nominalTasks = current.tasks.mapIndexed { idx, row ->
            when (idx) {
                0 -> row.copy(wdtStatus = "Subscribed (5.0s)", wdtHealthy = true, missedDeadlines = 0)
                1 -> row.copy(wdtStatus = "Subscribed (5.0s)", wdtHealthy = true, missedDeadlines = 0)
                else -> row.copy(wdtHealthy = true, missedDeadlines = 0)
            }
        }
        _uiState.update {
            it.copy(
                rtosHealth = current.copy(
                    wdtNominal = true,
                    wdtBadgeText = "Task WDT Nominal (5.0s)",
                    tier1Status = "ARMED",
                    tier2Status = "ONLINE",
                    tier3Status = "ACTIVE BACKUP",
                    tasks = nominalTasks
                ),
                terminalOutput = "[RTOS NOMINAL] Dual ESP32-S3 task watchdogs (5.0s) and ESP-NOW authenticated link nominal."
            )
        }
        recordAuditEvent("System", "RTOS_RESTORED", "INFO", "All 3 hardware tiers and FreeRTOS task watchdogs restored to nominal state.")
        showToast("RTOS Nominal", "All 3 hardware tiers and watchdogs restored.", "SUCCESS")
    }

    fun getDiagnosticRows(): List<SensorDiagnosticRow> {
        if (_uiState.value.mode == TelemetryMode.HARDWARE_NODE_A) {
            val hw = _uiState.value.hardwareState
            return listOf(SensorDiagnosticRow("PIR_BEDROOM", "bedroom", "Bedroom", "Digital PIR",
                if (hw.connected) hw.sample?.motion?.let { if (it) "HIGH" else "LOW" } ?: "UNKNOWN" else "UNKNOWN",
                if (hw.connected) "${_uiState.value.hardwareTelemetry.freshnessMs} ms" else "STALE / DISCONNECTED",
                hw.connected, "Reported logic; wiring not verified", false),
                SensorDiagnosticRow("TEMP_BEDROOM", "bedroom", "Bedroom", "Temperature", "UNAVAILABLE", "No wire field", false, "Driver/transport pending", false))
        }
        val rows = mutableListOf<SensorDiagnosticRow>()
        _uiState.value.rooms.forEach { r ->
            val tag = r.id.uppercase()
            rows.add(
                SensorDiagnosticRow(
                    sensorId = "PIR_$tag",
                    roomId = r.id,
                    roomName = r.name,
                    type = "Digital PIR Motion",
                    latestValue = if (r.pirEnabled) (if (r.state == OccupancyState.OCCUPIED) "MOTION (HIGH)" else "QUIET (LOW)") else "DISABLED",
                    freshness = "< 50ms",
                    isOnline = r.pirEnabled,
                    healthStatus = if (r.pirEnabled) "Healthy" else "Fault / Offline",
                    canToggleFault = true
                )
            )
            rows.add(
                SensorDiagnosticRow(
                    sensorId = "PRES_$tag",
                    roomId = r.id,
                    roomName = r.name,
                    type = "mmWave Presence",
                    latestValue = if (r.presenceEnabled) (if (r.state == OccupancyState.OCCUPIED) "PRESENT" else "ABSENT") else "DISABLED",
                    freshness = "< 50ms",
                    isOnline = r.presenceEnabled,
                    healthStatus = if (r.presenceEnabled) "Healthy" else "Fault / Offline",
                    canToggleFault = true
                )
            )
            rows.add(
                SensorDiagnosticRow(
                    sensorId = "TEMP_$tag",
                    roomId = r.id,
                    roomName = r.name,
                    type = "Thermistor Temp",
                    latestValue = "${"%.1f".format(r.tempC)} °C",
                    freshness = "< 50ms",
                    isOnline = true,
                    healthStatus = "Calibrated",
                    canToggleFault = false
                )
            )
            rows.add(
                SensorDiagnosticRow(
                    sensorId = "LUX_$tag",
                    roomId = r.id,
                    roomName = r.name,
                    type = "Photodiode Lux",
                    latestValue = "${r.lux} lx",
                    freshness = "< 50ms",
                    isOnline = true,
                    healthStatus = "Calibrated",
                    canToggleFault = false
                )
            )
        }
        return rows
    }

    fun toggleSensorFault(sensorId: String) {
        if (_uiState.value.mode != TelemetryMode.SIMULATION_4_ROOM) return
        val idx = _uiState.value.rooms.indexOfFirst { sensorId.endsWith(it.id.uppercase()) }
        if (idx !in 0..3) return
        val r = _uiState.value.rooms[idx]
        val newPir = if (sensorId.startsWith("PIR_")) !r.pirEnabled else r.pirEnabled
        val newPres = if (sensorId.startsWith("PRES_")) !r.presenceEnabled else r.presenceEnabled
        updateRoomEnvironment(idx, r.tempC, r.lux, newPir, newPres, r.doorOpen)
        showToast(
            "Sensor Diagnostic Updated",
            "$sensorId in ${r.name} is now ${if ((sensorId.startsWith("PIR_") && newPir) || (sensorId.startsWith("PRES_") && newPres)) "Online" else "Faulted"}.",
            "WARN"
        )
    }

    fun savePolicyConfig(newPolicy: PolicyConfig) {
        if (_uiState.value.mode == TelemetryMode.HARDWARE_NODE_A) {
            val url = runCatching { GatewayAddress.validate(newPolicy.hardwareUrl) }.getOrNull()
            if (url == null) {
                showToast("Invalid gateway URL", "Use the actual private Wi-Fi IPv4 origin, for example http://192.168.4.1. No localhost or emulator alias.", "WARN")
                return
            }
            cancelPhoneVoice()
            hardware.reset()
            gatewaySettings = gatewaySettings.copy(hardwareUrl = url, hardwareToken = newPolicy.hardwareToken)
            _uiState.update { it.copy(policy = gatewaySettings) }
            gatewayApis = WifiGatewayApis(getApplication(), url, newPolicy.hardwareToken)
            hardware = HardwareRepository(gatewayApis, android.os.SystemClock::elapsedRealtime)
            voice = LocalVoiceRepository(gatewayApis, hardware)
            bindHardware()
            renderHardware(hardware.state.value)
            showToast("Gateway settings applied", "Token stays in memory. Persistent board policy unchanged.")
        } else _uiState.update { it.copy(policy = newPolicy) }
    }

    fun updateLogFilters(
        search: String = _uiState.value.logSearchQuery,
        room: String = _uiState.value.logRoomFilter,
        severity: String = _uiState.value.logSeverityFilter,
        source: String = _uiState.value.logSourceFilter
    ) {
        _uiState.update {
            it.copy(
                logSearchQuery = search,
                logRoomFilter = room,
                logSeverityFilter = severity,
                logSourceFilter = source
            )
        }
    }

    fun clearAuditLogs() {
        viewModelScope.launch {
            auditDao.clearAll()
            showToast("Audit Logs Cleared", "Local event log table cleared.", "INFO")
        }
    }

    fun clearUnreadAlerts() {
        _uiState.update { it.copy(unreadAlertsCount = 0) }
    }

    fun syncWithSupabaseCloud(silent: Boolean = false) { if (!silent) cloudUnavailable() }

    private fun cloudUnavailable() {
        showToast("Cloud integration blocked", "Requires signed-in user, home membership and verified RLS; no app-authored measured telemetry is uploaded.", "WARN")
    }

    fun pullFromSupabaseCloud() { cloudUnavailable() }

    private fun recordAuditEvent(
        room: String,
        eventType: String,
        severity: String,
        description: String,
        sourceOverride: String? = null,
        pushToRemoteTable: Boolean = true
    ) {
        val now = System.currentTimeMillis()
        val timeStr = SimpleDateFormat("HH:mm:ss", Locale.US).format(Date(now))
        val src = sourceOverride ?: if (_uiState.value.mode == TelemetryMode.HARDWARE_NODE_A) "APP_REQUEST" else "SYNTHETIC"
        if (severity == "WARN" || severity == "ALARM") {
            _uiState.update { it.copy(unreadAlertsCount = it.unreadAlertsCount + 1) }
        }
        viewModelScope.launch {
            auditDao.insertEvent(
                AuditEventEntity(
                    timestamp = timeStr,
                    epochMs = now,
                    room = room,
                    eventType = eventType,
                    severity = severity,
                    source = src,
                    description = description
                )
            )

        }
    }

    fun showToast(title: String, message: String, severity: String = "INFO") {
        val toast = ToastMessage(title = title, message = message, severity = severity)
        _uiState.update { it.copy(activeToast = toast) }
        viewModelScope.launch {
            delay(3200L)
            _uiState.update { if (it.activeToast?.id == toast.id) it.copy(activeToast = null) else it }
        }
    }

    fun dismissToast() {
        _uiState.update { it.copy(activeToast = null) }
    }
}
