package com.example.data.remote

import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.withContext
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.RequestBody.Companion.toRequestBody
import org.json.JSONObject
import java.util.concurrent.TimeUnit
import com.example.domain.ContinuousRunObservation
import com.example.domain.SensorAutoSettings

/** Only this adapter speaks the maintained B firmware API. POSTs are never retried. */
fun interface BoardApi {
    suspend fun request(path: String, body: String?): JSONObject
}

class BoardRejected(val status: Int) : Exception("Gateway rejected request (HTTP $status)")

class HttpBoardApi(baseUrl: String, private val token: String, wifiSockets: javax.net.SocketFactory? = null, wifiDns: okhttp3.Dns? = null) : BoardApi {
    private val base = baseUrl.trim().trimEnd('/')
    private val http = OkHttpClient.Builder().apply { if (wifiSockets != null) socketFactory(wifiSockets); if (wifiDns != null) dns(wifiDns) }.retryOnConnectionFailure(false)
        .followRedirects(false).followSslRedirects(false)
        .connectTimeout(1500, TimeUnit.MILLISECONDS).readTimeout(1500, TimeUnit.MILLISECONDS)
        .callTimeout(2, TimeUnit.SECONDS).build()

    override suspend fun request(path: String, body: String?): JSONObject = withContext(Dispatchers.IO) {
        require(token.isNotBlank()) { "Configure the existing gateway token" }
        val request = Request.Builder().url(base + path).header("X-ClawGrid-Token", token)
        if (body != null) request.post(body.toRequestBody("text/plain; charset=utf-8".toMediaType()))
        http.newCall(request.build()).execute().use {
            if (!it.isSuccessful) throw BoardRejected(it.code)
            require(it.code == if (path.substringBefore('?') == "/api/confirm" || path.substringBefore('?') == "/api/control") 202 else 200) { "Unexpected firmware response status" }
            val text = it.body?.string().orEmpty()
            require(text.length <= 16384) { "Oversized gateway response" }
            JSONObject(text)
        }
    }
}

data class BoardSample(
    val boot: Long, val uptime: Long, val lastCommand: Long, val age: Long,
    val received: Long, val light: Boolean, val fan: Boolean,
    val occupancy: String, val motion: Boolean?, val outputsEnabled: Boolean?,
    val autoFlags: Int, val manualFlags: Int,
    val temperatureDeciC: Int? = null, val temperatureAge: Long? = null,
    val temperatureStatus: String = "UNAVAILABLE",
    val controlPolicyVersion: Int = 1, val mode: String = "UNKNOWN", val outputProfile: String = "UNKNOWN",
    val graceMs: Long? = null, val pirAvailable: Boolean? = null, val automationAvailable: Boolean? = null,
    val featuresVersion: Int = 0, val lightPlan: DevicePlan = DevicePlan(), val fanPlan: DevicePlan = DevicePlan(),
    val lightOnMs: Long = 0, val fanOnMs: Long = 0,
    val lightInputAvailable: Boolean = false, val lightInputRaw: Boolean? = null,
    val lightObservedRunMs: Long? = null, val fanObservedRunMs: Long? = null,
    val lightAuto: DeviceAutoState? = null, val fanAuto: DeviceAutoState? = null,
    val sensorEvaluation: SensorEvaluation? = null
) {
    fun ageAt(now: Long) = age + (now - received).coerceAtLeast(0)
    fun fresh(now: Long) = ageAt(now) < 3000
    fun observedRunAt(device: String, now: Long): Long? {
        if(!fresh(now) || featuresVersion !in 1..3) return null
        val on=if(device=="light") light else fan
        if(!on) return 0L
        return (if(device=="light") lightObservedRunMs else fanObservedRunMs)?.plus(ageAt(now))
    }
    fun countdownAt(device: String, now: Long): Long? {
        if(!fresh(now) || featuresVersion !in 1..3) return null
        val remaining=(if(device=="light") lightPlan else fanPlan).countdownMs
        // Keep a due timer distinct from a board-confirmed cancelled/expired timer.
        return if(remaining==0L) 0L else (remaining-ageAt(now)).coerceAtLeast(1)
    }
    fun voiceOutputModeAllowed() = outputsEnabled == false ||
        (outputsEnabled == true && controlPolicyVersion == 2 && outputProfile in listOf("LED_INDICATORS_10K_ACTIVE_LOW", "LED_INDICATORS_10K_ACTIVE_HIGH"))
    fun temperatureAt(now: Long): Double? = temperatureDeciC?.takeIf {
        fresh(now) && temperatureStatus == "OK" && temperatureAge != null &&
            temperatureAge + (now - received).coerceAtLeast(0) < 10000
    }?.div(10.0)
}

data class DeviceAutoState(val settings: SensorAutoSettings, val overrideMs: Long, val demand: Int,
    val winner: Int, val blocking: Int, val overrideAction: Int)

data class DevicePlan(val remainingMs: Long = 0, val action: Int = 0, val result: Int = 0,
    val source: Int = 0, val capMs: Long = 0, val countdownMs: Long = 0)

enum class RequestPhase { IDLE, PREPARING, PROPOSED, SENDING, PENDING, REPORTED, REJECTED, EXPIRED, OUTCOME_UNKNOWN }

data class LightRequest(
    val on: Boolean, val phase: RequestPhase, val boot: Long,
    val proposal: Long = 0, val expires: Long = 0, val id: Long = 0,
    val sampleBefore: Long = 0, val deadline: Long = 0, val ack: Boolean = false,
    val source: String = "manual", val speechJob: Long = 0,
    val device: String = "light", val action: String = if (on) "ON" else "OFF",
    val configValue: Long = 0, val configAction: Int = 0
) {
    val active get() = phase in listOf(RequestPhase.PREPARING, RequestPhase.PROPOSED, RequestPhase.SENDING, RequestPhase.PENDING)
}

data class HardwareState(
    val sample: BoardSample? = null, val connected: Boolean = false,
    val request: LightRequest? = null, val message: String = "Disconnected; board state unknown",
    val manualVerifiedBoot: Long? = null,
    val manualFanVerifiedBoot: Long? = null,
    val connectionIssue: String? = null
) {
    fun temperaturePresentation(now: Long): TemperaturePresentation {
        val board = sample?.takeIf { connected && it.fresh(now) }
        val c = board?.temperatureAt(now)
        return if (c != null) TemperaturePresentation(String.format(java.util.Locale.US, "%.1f °C", c), "DHT11 · physical sensor", true)
        else TemperaturePresentation("Unavailable", if (board == null) "Board stale or disconnected" else
            if (board.temperatureStatus == "OK") "DHT11 reading invalid or stale" else "DHT11 · ${board.temperatureStatus}", false)
    }
}
data class TemperaturePresentation(val valueText: String, val detail: String, val available: Boolean)

/** In-memory requests only. Reconnect/reset cannot dispatch a command. All methods run on one owner coroutine dispatcher. */
class HardwareRepository(private val api: BoardApi, private val clock: () -> Long) {
    var room: String = "bedroom"
        private set
    constructor(api: BoardApi, clock: () -> Long, room: String): this(api,clock) {
        require(room in listOf("bedroom","living_room")); this.room=room
    }
    val nodeId get() = if(room == "bedroom") 1 else 3

    private val mutable = MutableStateFlow(HardwareState())
    val state = mutable.asStateFlow()
    private var generation = 0L
    private val observedLight=ContinuousRunObservation()
    private val observedFan=ContinuousRunObservation()

    private suspend fun request(path: String, body: String?): JSONObject = api.request(if(room == "bedroom") path else "$path?room=2", body).also { j ->
        require(if(room == "living_room") j.optInt("node_id",-1)==nodeId else !j.has("node_id") || j.optInt("node_id",-1)==nodeId) { "Wrong controller node" }
        // Production schema 3 omits a source on state/proposals. Explicit host/simulation markers are never accepted.
        for (key in listOf("source", "processing_source"))
            if (j.has(key)) require(j.optString(key) == "HARDWARE_MEASURED") { "Non-hardware source" }
        require(j.optString("input_source") != "SIMULATED" && j.optString("execution_mode") != "HOST_ONLY") { "Host simulation is not hardware" }
    }

    fun reset() {
        generation++
        observedLight.clear(); observedFan.clear()
        mutable.value = HardwareState(message = "Connection changed; old requests discarded without replay")
    }

    private fun finish(phase: RequestPhase, message: String) {
        mutable.value = mutable.value.copy(request = mutable.value.request?.copy(phase = phase), message = message)
    }

    fun age() {
        val s = mutable.value
        val r = s.request
        if (r?.phase == RequestPhase.PROPOSED && clock() >= r.expires) {
            finish(RequestPhase.EXPIRED, "Proposal expired; prepare a new request")
        } else if (r?.phase == RequestPhase.PENDING && clock() >= r.deadline) {
            finish(RequestPhase.OUTCOME_UNKNOWN, "ACK/readback timeout; outcome unknown. No automatic retry")
        }
        if (s.sample?.fresh(clock()) == false) {
            observedLight.clear(); observedFan.clear()
            if (mutable.value.request?.active == true)
                finish(RequestPhase.OUTCOME_UNKNOWN, "Telemetry stale; request discarded, outcome unknown. No automatic retry")
            mutable.value = mutable.value.copy(connected = false, connectionIssue = "STALE_REPORT", message = "STALE — board state unknown; no automatic retry")
        }
    }

    suspend fun poll() {
        val epoch = generation
        val started = clock()
        try {
            val j = request("/api/state", null)
            if (epoch != generation) return
            val now = clock()
            require(j.optString("room") == room && j.opt("known") == true && j.opt("authenticated") == true)
            val boot = uint(j, "boot_id"); val uptime = uint(j, "sample_uptime_ms")
            require(boot != 0L)
            val sourceAge = uint(j, "age_ms") + (now - started).coerceAtLeast(0)
            require(sourceAge < 3000 && j.opt("light") is Boolean && j.opt("fan") is Boolean)
            val lightAvailable=j.opt("light_input_available") == true
            require(room == "bedroom" || !lightAvailable) { "Room2 has no light sensor" }
            require(if(lightAvailable) j.opt("light_input_raw") is Boolean else !j.has("light_input_raw") || j.isNull("light_input_raw"))
            val old = mutable.value.sample
            if (old != null) {
                require(boot >= old.boot) { "Regressing boot" }
                if (boot == old.boot && uptime != old.uptime) require(newer(uptime, old.uptime)) { "Regressing sample" }
                if (boot != old.boot && mutable.value.request?.active == true)
                    finish(RequestPhase.OUTCOME_UNKNOWN, "Controller reboot; old request invalidated. No automatic retry")
                if (boot != old.boot) mutable.value = mutable.value.copy(manualVerifiedBoot = null, manualFanVerifiedBoot = null)
                if (boot == old.boot && uptime == old.uptime) require(
                    old.light == j.getBoolean("light") && old.fan == j.getBoolean("fan") && old.lastCommand == uint(j, "last_command")
                ) { "Same sample changed contents" }
            }
            val lightOnMs=optionalUint(j,"light_on_ms"); val fanOnMs=optionalUint(j,"fan_on_ms")
            val contiguous=old!=null && boot==old.boot && mutable.value.connected && old.fresh(now) &&
                ((uptime-old.uptime) and 0xffffffffL)<3000
            val sample = BoardSample(boot, uptime, uint(j, "last_command"),
                if (old != null && boot == old.boot && uptime == old.uptime) maxOf(sourceAge, old.ageAt(now)) else sourceAge,
                now, j.getBoolean("light"), j.getBoolean("fan"), j.optString("occupancy", "UNKNOWN"),
                j.opt("motion") as? Boolean, j.opt("physical_outputs_enabled") as? Boolean,
                j.optInt("auto_flags", 0), j.optInt("manual_flags", 0),
                temperatureValue(j), temperatureAge(j, now - started), j.optString("temperature_status", "UNAVAILABLE"),
                j.optInt("control_policy_version", 1), j.optString("mode", "UNKNOWN"), j.optString("output_profile", "UNKNOWN"),
                if (j.has("grace_ms")) uint(j, "grace_ms").also { require(it in 30000..1800000) } else null,
                j.opt("pir_available") as? Boolean, j.opt("automation_available") as? Boolean,
                j.optInt("features_version", 0), plan(j,"light"), plan(j,"fan"),
                lightOnMs, fanOnMs,
                j.opt("light_input_available") == true, j.opt("light_input_raw") as? Boolean,
                observedLight.observe(boot,j.getBoolean("light"),lightOnMs,contiguous),
                observedFan.observe(boot,j.getBoolean("fan"),fanOnMs,contiguous),
                autoState(j,"light"),autoState(j,"fan"),evaluation(j,now-started))
            val recovered = !mutable.value.connected || mutable.value.connectionIssue != null
            mutable.value = mutable.value.copy(sample = sample, connected = sample.fresh(now), connectionIssue = null,
                message = if (recovered && mutable.value.request?.active != true)
                    "Fresh board report; physical load not measured" else mutable.value.message)
            age()
            val r = mutable.value.request
            if (r?.phase == RequestPhase.PENDING && mutable.value.connected) {
                val tx = request("/api/transaction", null)
                if (epoch != generation || mutable.value.request != r) return
                if (uint(tx, "id") == r.id) {
                    when (tx.getInt("phase")) {
                        2 -> {
                            require(tx.opt("outcome_known") == true)
                            if (tx.getInt("status") != 0) finish(RequestPhase.REJECTED, "SafetyKernel rejected command #${r.id} (status ${tx.getInt("status")})")
                            else if (r.action in listOf("AUTO", "SCENE", "SCHEDULE", "CAP", "COUNTDOWN", "SENSOR_CONFIG", "DEVICE_MODE", "SENSOR_ORDER") || tx.opt(r.device) == r.on) mutable.value = mutable.value.copy(request = r.copy(ack = true))
                        }
                        3 -> finish(RequestPhase.OUTCOME_UNKNOWN, "Gateway command timed out; outcome unknown. No automatic retry")
                    }
                } else if (tx.optInt("phase", -1) == 0) {
                    finish(RequestPhase.OUTCOME_UNKNOWN, "Gateway transaction lost/restarted; outcome unknown. No automatic retry")
                }
                val pending = mutable.value.request
                if (pending?.phase == RequestPhase.PENDING && pending.ack && sample.boot == pending.boot &&
                    newer(sample.uptime, pending.sampleBefore) && sample.lastCommand == pending.id &&
                    reportedTarget(sample, pending) && sample.fresh(clock()))
                {
                    finish(RequestPhase.REPORTED, "Matching ACK + fresh board report #${pending.id}. Physical load not measured")
                    if (pending.source == "manual" && pending.action in listOf("ON", "OFF")) {
                        mutable.value = if (pending.device == "light") mutable.value.copy(manualVerifiedBoot = pending.boot)
                            else mutable.value.copy(manualFanVerifiedBoot = pending.boot)
                    }
                }
                age()
            } else if (r == null && mutable.value.connected) mutable.value = mutable.value.copy(message = "Fresh board report; physical load not measured")
        } catch (e: CancellationException) { throw e
        } catch (failure: Exception) {
            if (epoch != generation) return
            observedLight.clear(); observedFan.clear()
            if (mutable.value.request?.active == true)
                finish(RequestPhase.OUTCOME_UNKNOWN, "Connection/readback lost; outcome unknown. No automatic retry")
            val category = when (failure) {
                is BoardRejected -> "HTTP_${failure.status}"
                is java.net.SocketTimeoutException, is java.io.InterruptedIOException -> "HTTP_TIMEOUT"
                is java.io.IOException -> "LOCAL_CONNECTION_IO"
                is org.json.JSONException -> "INVALID_GATEWAY_JSON"
                is IllegalArgumentException -> "INVALID_OR_STALE_REPORT"
                is IllegalStateException -> "WIFI_OR_CONFIGURATION_UNAVAILABLE"
                else -> "LOCAL_READ_FAILURE"
            }
            mutable.value = mutable.value.copy(connected = false, connectionIssue = category,
                message = "Disconnected or invalid/stale telemetry [$category]; board state unknown. No automatic retry")
        }
    }

    suspend fun prepare(on: Boolean, device: String = "light") {
        require(device == "light" || device == "fan") { "Unsupported physical device" }
        prepareLight(on, "manual", 0, device = device)
    }
    suspend fun prepareAuto(device: String) {
        require(device == "light" || device == "fan")
        age()
        val sample = mutable.value.sample ?: return
        if (sample.automationAvailable == false && sample.featuresVersion !in 2..3) {
            mutable.value = mutable.value.copy(message = "AUTO unavailable: physical PIR removed for this milestone")
            return
        }
        if (sample.controlPolicyVersion != 2 || sample.mode != "HOME") {
            mutable.value = mutable.value.copy(message = if (sample.mode == "AWAY") "AUTO blocked by AWAY; global policy unchanged" else "Controller does not advertise manual-until-AUTO support")
            return
        }
        prepareLight(false, "manual", 0, device = device, action = "AUTO")
    }

    /** Explicit user configuration action. No background dispatch or retry. Controller owns timing. */
    suspend fun configurePlan(device: String, action: Int, value: Long, cap: Boolean = false, expectedBoot: Long? = null) {
        require(device in listOf("light", "fan"))
        require(if(cap) action == 0 && (value == 0L || value in 3000L..14400000L)
                else (action == 2 && value == 0L) || (action in 0..1 && value in 1000L..86400000L))
        if(!cap && action in 0..1) {
            val sample=mutable.value.sample ?: return
            val bit=if(device=="light") 1 else 2
            if(sample.mode=="AWAY" || (sample.featuresVersion in 2..3 &&
                ((sample.autoFlags and bit)==0 || (sample.manualFlags and bit)!=0))) {
                mutable.value=mutable.value.copy(message="Schedule not armed: return to HOME and explicitly enable this device's AUTO first. MANUAL/AWAY would block execution.")
                return
            }
        }
        configureRule(device,if(cap) 8 else 7,if(cap) "CAP" else "SCHEDULE",action,value,expectedBoot)
    }

    suspend fun configureScene(action: Int, expectedBoot: Long) {
        require(action in 0..3)
        configureRule("light",2,"SCENE",action,0,expectedBoot)
    }

    suspend fun configureSensorOrder(temperatureFirst: Boolean, expectedBoot: Long) {
        require(room=="bedroom") { "Sensor evaluation order is Room1 only" }
        configureRule("light",11,"SENSOR_ORDER",0,if(temperatureFirst) 1 else 0,expectedBoot)
    }

    suspend fun configureSensorAuto(device: String, settings: SensorAutoSettings, expectedBoot: Long) {
        require(device in listOf("light","fan"))
        require(!(room=="living_room" && device=="light" && settings.sensorsFirst)) { "Room 2 light has no sensor" }
        configureRule(device,9,"SENSOR_CONFIG",0,settings.packed(),expectedBoot)
    }

    suspend fun configureDeviceMode(device: String, auto: Boolean, expectedBoot: Long) {
        require(device in listOf("light","fan"))
        configureRule(device,10,"DEVICE_MODE",if(auto) 1 else 0,0,expectedBoot)
    }

    suspend fun configureCountdown(device: String, value: Long, expectedBoot: Long? = null) {
        require(device in listOf("light","fan"))
        require(value==0L || value in 60000L..14400000L)
        configureRule(device,3,"COUNTDOWN",0,value,expectedBoot)
    }

    private suspend fun configureRule(device: String, op: Int, kind: String, action: Int, value: Long, expectedBoot: Long?) {
        age()
        val state=mutable.value
        val sample=state.sample ?: return
        if(!state.connected || !sample.fresh(clock()) || state.request?.active == true) return
        if(expectedBoot != null && sample.boot != expectedBoot) return
        if(sample.featuresVersion !in 1..3 || (op>=9 && sample.featuresVersion !in 2..3) || (op==11 && sample.featuresVersion!=3) || !sample.voiceOutputModeAllowed()) {
            mutable.value=state.copy(message="Updated controller and gateway firmware required for schedules/energy caps")
            return
        }
        val epoch=++generation
        val r=LightRequest(false,RequestPhase.SENDING,sample.boot,source="configuration",device=device,
            action=kind,configValue=value,configAction=action)
        mutable.value=state.copy(request=r,message="Submitting $device $kind configuration; awaiting controller")
        try {
            val index=if(device == "light") 0 else 1
            val j=request("/api/control","boot ${sample.boot} $op $index $action $value")
            if(epoch != generation || mutable.value.request != r) return
            val id=uint(j,"id")
            require(j.opt("queued") == true && id > 0)
            require(mutable.value.connected && mutable.value.sample?.boot == r.boot && mutable.value.sample?.fresh(clock()) == true)
            mutable.value=mutable.value.copy(request=r.copy(phase=RequestPhase.PENDING,id=id,
                sampleBefore=sample.uptime,deadline=clock()+5000),message="Configuration #$id queued; waiting for ACK and fresh configuration")
        } catch(e: CancellationException) { throw e
        } catch(e: Exception) {
            if(epoch == generation) finish(if(e is BoardRejected) RequestPhase.REJECTED else RequestPhase.OUTCOME_UNKNOWN,
                "Configuration rejected or outcome unknown; inspect fresh controller state. No automatic retry")
        }
    }

    fun voiceReady(device: String = "light"): Boolean = mutable.value.let {
        device in listOf("light", "fan") && it.connected && it.sample?.fresh(clock()) == true && it.sample.voiceOutputModeAllowed() && it.request?.active != true
    }

    suspend fun prepareSpeech(on: Boolean, job: Long) {
        require(job in 1..0xffffffffL && voiceReady()) { "Fresh supported controller required" }
        prepareLight(on, "speech", job)
    }

    suspend fun preparePhone(on: Boolean, boot: Long, capture: Long, expires: Long, device: String = "light") {
        if (!voiceReady(device) || mutable.value.sample?.boot != boot || mutable.value.sample?.voiceOutputModeAllowed() != true ||
            capture !in 1..0xffffffffL || clock() >= expires) return
        prepareLight(on, "phone", capture, boot, expires, device = device)
    }

    private suspend fun prepareLight(on: Boolean, source: String, job: Long, capturedBoot: Long? = null, contextExpires: Long = Long.MAX_VALUE, device: String = "light", action: String = if (on) "ON" else "OFF") {
        age()
        val s = mutable.value
        if (!s.connected || s.sample?.fresh(clock()) != true || s.request?.active == true) return
        val sample = s.sample
        val epoch = ++generation
        val start = clock()
        val fields = "physical_bedroom $room $device $action"
        mutable.value = s.copy(request = LightRequest(on, RequestPhase.PREPARING, sample.boot, source = source, speechJob = job, device = device, action = action), message = "Preparing $device $action request; reported state unchanged")
        try {
            val p = request("/api/proposal", "$source $fields $job" + if (source == "phone") " $capturedBoot" else "")
            if (epoch != generation || mutable.value.request?.phase != RequestPhase.PREPARING) return
            require(p.optString("target") == "physical_bedroom" && p.optString("room") == room &&
                p.optString("device") == device && p.optString("action") == action)
            val id = uint(p, "proposal_id"); require(id > 0 && p.opt("authorized") == false)
            if (source == "phone") require(p.optString("origin") == "phone" && uint(p, "controller_boot") == capturedBoot && uint(p, "capture_id") == job)
            val lease = uint(p, "expires_in_ms").coerceAtMost(15000)
            val expires = minOf(start + lease, contextExpires)
            require(clock() < expires && mutable.value.sample?.boot == sample.boot && mutable.value.sample?.fresh(clock()) == true && mutable.value.connected)
            mutable.value = mutable.value.copy(request = LightRequest(on, RequestPhase.PROPOSED, sample.boot, id, expires, source = source, speechJob = job, device = device, action = action),
                message = "Requested $device $action; confirm proposal #$id")
        } catch (e: CancellationException) { throw e
        } catch (_: Exception) {
            if (epoch == generation) finish(RequestPhase.REJECTED, "Proposal rejected/unavailable; nothing confirmed")
        }
    }

    fun cancelProposal() {
        if (mutable.value.request?.phase !in listOf(RequestPhase.PREPARING, RequestPhase.PROPOSED)) return
        generation++
        finish(RequestPhase.EXPIRED, "Proposal cancelled; nothing confirmed")
    }

    suspend fun confirm() {
        age()
        val r = mutable.value.request ?: return
        val sample = mutable.value.sample ?: return
        if (r.phase != RequestPhase.PROPOSED || !mutable.value.connected || !sample.fresh(clock()) || sample.boot != r.boot || clock() >= r.expires) return
        if (r.action == "AUTO" && (sample.mode != "HOME" || sample.controlPolicyVersion != 2)) {
            finish(RequestPhase.REJECTED, "AUTO unavailable after policy/context change; nothing confirmed"); return
        }
        val epoch = ++generation
        mutable.value = mutable.value.copy(request = r.copy(phase = RequestPhase.SENDING), message = "Confirming; reported state unchanged")
        try {
            val j = request("/api/confirm", "${r.proposal} physical_bedroom $room ${r.device} ${r.action}")
            if (epoch != generation || mutable.value.request?.phase != RequestPhase.SENDING) return
            val id = uint(j, "id"); require(j.opt("queued") == true && id > 0)
            require(mutable.value.connected && mutable.value.sample?.boot == r.boot && mutable.value.sample?.fresh(clock()) == true)
            mutable.value = mutable.value.copy(request = r.copy(phase = RequestPhase.PENDING, id = id, sampleBefore = sample.uptime, deadline = clock() + 5000),
                message = "Requested ${r.device} ${r.action} queued (#$id); waiting for ACK + board report")
        } catch (e: CancellationException) { throw e
        } catch (e: Exception) {
            if (epoch == generation) finish(if (e is BoardRejected) RequestPhase.REJECTED else RequestPhase.OUTCOME_UNKNOWN,
                if (e is BoardRejected) "Confirmation rejected (HTTP ${e.status})" else "Confirmation outcome unknown. No automatic retry")
        }
    }

    companion object {
        private fun optionalUint(j: JSONObject, key: String): Long = if(j.has(key)) uint(j,key) else 0
        private fun plan(j: JSONObject, device: String): DevicePlan {
            if(j.optInt("features_version",0) !in 1..3) return DevicePlan()
            val remaining=uint(j,"${device}_schedule_ms")
            val action=uint(j,"${device}_schedule_action").toInt()
            val result=uint(j,"${device}_schedule_result").toInt()
            val source=uint(j,"${device}_control_source").toInt()
            val cap=uint(j,"${device}_energy_cap_ms")
            val countdown=optionalUint(j,"${device}_timer_ms")
            require(remaining <= 86400000 && action in 0..1 && result in 0..(if(j.optInt("features_version")>=2) 5 else 4) && source in 0..(if(j.optInt("features_version")>=2) 7 else 4))
            require(cap == 0L || cap in 3000L..14400000L)
            require(countdown<=86400000L)
            return DevicePlan(remaining,action,result,source,cap,countdown)
        }
        private fun evaluation(j: JSONObject, transit: Long): SensorEvaluation? {
            if(j.optInt("features_version")!=3) return null
            require(j.optString("room")=="bedroom" && j.optInt("node_id")==1)
            val order=uint(j,"sensor_order").toInt();require(order in 0..1)
            fun rule(d: String): SensorRuleEvaluation {
                val step=uint(j,"${d}_eval_step").toInt();val requested=uint(j,"${d}_sensor_requested").toInt()
                val result=uint(j,"${d}_sensor_result").toInt()
                require(step in 1..2 && requested in 0..1 && result in 0..7)
                return SensorRuleEvaluation(step,requested==1,result,uint(j,"${d}_eval_start_us"),uint(j,"${d}_eval_elapsed_us"))
            }
            val light=rule("light");val fan=rule("fan")
            require(light.step==if(order==0) 1 else 2)
            require(fan.step==if(order==0) 2 else 1)
            fun age(key: String): Long?=uint(j,key).let { if(it==0xffffffffL) null else it+transit.coerceAtLeast(0) }
            return SensorEvaluation(order,uint(j,"evaluation_cycle"),uint(j,"sensor_snapshot_ms"),uint(j,"evaluation_started_us"),
                j.opt("evaluation_clock_valid")==true,age("light_sample_age_ms"),age("temperature_sample_age_ms"),light,fan)
        }
        private fun autoState(j: JSONObject, device: String): DeviceAutoState? {
            if(j.optInt("features_version",0) !in 2..3) return null
            val config=SensorAutoSettings.unpack(uint(j,"${device}_sensor_config"))
            require(!(j.optString("room")=="living_room" && device=="light" && config.sensorsFirst))
            val lease=uint(j,"${device}_override_ms"); val demand=uint(j,"${device}_sensor_demand").toInt()
            val winner=uint(j,"${device}_winner").toInt(); val blocking=uint(j,"${device}_blocking").toInt()
            val action=uint(j,"${device}_override_action").toInt()
            require(lease<=3600000 && demand in 0..2 && winner in 0..7 && blocking in 0..6 && action in 0..1)
            return DeviceAutoState(config,lease,demand,winner,blocking,action)
        }
        private fun reportedTarget(sample: BoardSample, request: LightRequest): Boolean {
            if(request.action == "SCENE") return when(request.configAction) {
                0 -> sample.mode == "HOME" && sample.autoFlags == 3 && sample.manualFlags == 0
                1 -> sample.mode == "AWAY" && !sample.light && !sample.fan
                2 -> sample.mode == "HOME" && !sample.light && sample.fan
                3 -> sample.mode == "HOME" && !sample.light && !sample.fan
                else -> false
            }
            val plan=if(request.device == "light") sample.lightPlan else sample.fanPlan
            if(request.action == "SENSOR_ORDER") return sample.featuresVersion==3 && sample.sensorEvaluation?.order?.toLong()==request.configValue
            if(request.action == "SENSOR_CONFIG") return sample.featuresVersion in 2..3 &&
                (if(request.device=="light") sample.lightAuto else sample.fanAuto)?.settings?.packed()==request.configValue
            if(request.action == "DEVICE_MODE") {
                val bit=if(request.device=="light") 1 else 2
                return sample.featuresVersion in 2..3 && ((sample.autoFlags and bit)!=0)==(request.configAction==1) &&
                    ((sample.manualFlags and bit)==0)==(request.configAction==1)
            }
            if(request.action == "CAP") return sample.featuresVersion in 1..3 && plan.capMs == request.configValue
            if(request.action == "COUNTDOWN") {
                if(sample.featuresVersion !in 1..3) return false
                if(request.configValue==0L) return plan.countdownMs==0L
                val elapsed=(sample.uptime-request.sampleBefore) and 0xffffffffL
                return plan.countdownMs in (request.configValue-elapsed-1000).coerceAtLeast(1)..request.configValue
            }
            if(request.action == "SCHEDULE") return sample.featuresVersion in 1..3 &&
                (if(request.configAction == 2) plan.remainingMs == 0L && plan.result == 4
                 else plan.action == request.configAction && (plan.result == 1 || plan.result == 2 || plan.result == 3))
            if (request.action != "AUTO") return (if (request.device == "light") sample.light else sample.fan) == request.on
            val bit = if (request.device == "light") 1 else 2
            return sample.controlPolicyVersion == 2 && sample.mode == "HOME" &&
                (sample.autoFlags and bit) != 0 && (sample.manualFlags and bit) == 0
        }
        private fun temperatureValue(j: JSONObject): Int? {
            if (j.optInt("telemetry_schema", 0) != 4 || j.optString("temperature_status") != "OK") return null
            val value = j.opt("temperature_deci_c")
            return (value as? Number)?.takeIf { (it is Int || it is Long) && it.toLong() in 0..500 }?.toInt()
        }
        private fun temperatureAge(j: JSONObject, transit: Long): Long? {
            if (temperatureValue(j) == null) return null
            val age = j.opt("temperature_age_ms")
            return (age as? Number)?.takeIf { (it is Int || it is Long) && it.toLong() in 0..9999 }
                ?.toLong()?.plus(transit.coerceAtLeast(0))
        }
        private fun uint(j: JSONObject, key: String): Long {
            val value = j.get(key)
            require(value is Int || value is Long)
            return (value as Number).toLong().also { require(it in 0..0xffffffffL) }
        }
        fun newer(next: Long, previous: Long) = ((next - previous) and 0xffffffffL) in 1..0x7fffffffL
    }
}
