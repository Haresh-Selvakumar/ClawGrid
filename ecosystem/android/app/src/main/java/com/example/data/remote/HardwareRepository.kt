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
            require(it.code == if (path == "/api/confirm") 202 else 200) { "Unexpected firmware response status" }
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
    val autoFlags: Int, val manualFlags: Int
) {
    fun ageAt(now: Long) = age + (now - received).coerceAtLeast(0)
    fun fresh(now: Long) = ageAt(now) < 3000
}

enum class RequestPhase { IDLE, PREPARING, PROPOSED, SENDING, PENDING, REPORTED, REJECTED, EXPIRED, OUTCOME_UNKNOWN }

data class LightRequest(
    val on: Boolean, val phase: RequestPhase, val boot: Long,
    val proposal: Long = 0, val expires: Long = 0, val id: Long = 0,
    val sampleBefore: Long = 0, val deadline: Long = 0, val ack: Boolean = false,
    val source: String = "manual", val speechJob: Long = 0
) {
    val active get() = phase in listOf(RequestPhase.PREPARING, RequestPhase.PROPOSED, RequestPhase.SENDING, RequestPhase.PENDING)
}

data class HardwareState(
    val sample: BoardSample? = null, val connected: Boolean = false,
    val request: LightRequest? = null, val message: String = "Disconnected; board state unknown",
    val manualVerifiedBoot: Long? = null
)

/** In-memory requests only. Reconnect/reset cannot dispatch a command. All methods run on one owner coroutine dispatcher. */
class HardwareRepository(private val api: BoardApi, private val clock: () -> Long) {
    private val mutable = MutableStateFlow(HardwareState())
    val state = mutable.asStateFlow()
    private var generation = 0L

    private suspend fun request(path: String, body: String?): JSONObject = api.request(path, body).also { j ->
        // Production schema 3 omits a source on state/proposals. Explicit host/simulation markers are never accepted.
        for (key in listOf("source", "processing_source"))
            if (j.has(key)) require(j.optString(key) == "HARDWARE_MEASURED") { "Non-hardware source" }
        require(j.optString("input_source") != "SIMULATED" && j.optString("execution_mode") != "HOST_ONLY") { "Host simulation is not hardware" }
    }

    fun reset() {
        generation++
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
            if (mutable.value.request?.active == true)
                finish(RequestPhase.OUTCOME_UNKNOWN, "Telemetry stale; request discarded, outcome unknown. No automatic retry")
            mutable.value = mutable.value.copy(connected = false, message = "STALE — board state unknown; no automatic retry")
        }
    }

    suspend fun poll() {
        val epoch = generation
        val started = clock()
        try {
            val j = request("/api/state", null)
            if (epoch != generation) return
            val now = clock()
            require(j.optString("room") == "bedroom" && j.opt("known") == true && j.opt("authenticated") == true)
            val boot = uint(j, "boot_id"); val uptime = uint(j, "sample_uptime_ms")
            require(boot != 0L)
            val sourceAge = uint(j, "age_ms") + (now - started).coerceAtLeast(0)
            require(sourceAge < 3000 && j.opt("light") is Boolean && j.opt("fan") is Boolean)
            val old = mutable.value.sample
            if (old != null) {
                require(boot >= old.boot) { "Regressing boot" }
                if (boot == old.boot && uptime != old.uptime) require(newer(uptime, old.uptime)) { "Regressing sample" }
                if (boot != old.boot && mutable.value.request?.active == true)
                    finish(RequestPhase.OUTCOME_UNKNOWN, "Controller reboot; old request invalidated. No automatic retry")
                if (boot != old.boot) mutable.value = mutable.value.copy(manualVerifiedBoot = null)
                if (boot == old.boot && uptime == old.uptime) require(
                    old.light == j.getBoolean("light") && old.fan == j.getBoolean("fan") && old.lastCommand == uint(j, "last_command")
                ) { "Same sample changed contents" }
            }
            val sample = BoardSample(boot, uptime, uint(j, "last_command"),
                if (old != null && boot == old.boot && uptime == old.uptime) maxOf(sourceAge, old.ageAt(now)) else sourceAge,
                now, j.getBoolean("light"), j.getBoolean("fan"), j.optString("occupancy", "UNKNOWN"),
                j.opt("motion") as? Boolean, j.opt("physical_outputs_enabled") as? Boolean,
                j.optInt("auto_flags", 0), j.optInt("manual_flags", 0))
            mutable.value = mutable.value.copy(sample = sample, connected = sample.fresh(now))
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
                            else if (tx.opt("light") == r.on) mutable.value = mutable.value.copy(request = r.copy(ack = true))
                        }
                        3 -> finish(RequestPhase.OUTCOME_UNKNOWN, "Gateway command timed out; outcome unknown. No automatic retry")
                    }
                } else if (tx.optInt("phase", -1) == 0) {
                    finish(RequestPhase.OUTCOME_UNKNOWN, "Gateway transaction lost/restarted; outcome unknown. No automatic retry")
                }
                val pending = mutable.value.request
                if (pending?.phase == RequestPhase.PENDING && pending.ack && sample.boot == pending.boot &&
                    newer(sample.uptime, pending.sampleBefore) && sample.lastCommand == pending.id && sample.light == pending.on && sample.fresh(clock()))
                {
                    finish(RequestPhase.REPORTED, "Matching ACK + fresh board report #${pending.id}. Physical load not measured")
                    if (pending.source == "manual") mutable.value = mutable.value.copy(manualVerifiedBoot = pending.boot)
                }
                age()
            } else if (r == null && mutable.value.connected) mutable.value = mutable.value.copy(message = "Fresh board report; physical load not measured")
        } catch (e: CancellationException) { throw e
        } catch (_: Exception) {
            if (epoch != generation) return
            if (mutable.value.request?.active == true)
                finish(RequestPhase.OUTCOME_UNKNOWN, "Connection/readback lost; outcome unknown. No automatic retry")
            mutable.value = mutable.value.copy(connected = false, message = "Disconnected or invalid/stale telemetry; board state unknown. No automatic retry")
        }
    }

    suspend fun prepare(on: Boolean) = prepareLight(on, "manual", 0)

    fun voiceReady(): Boolean = mutable.value.let {
        it.connected && it.sample?.fresh(clock()) == true && it.manualVerifiedBoot == it.sample.boot && it.request?.active != true
    }

    suspend fun prepareSpeech(on: Boolean, job: Long) {
        require(job in 1..0xffffffffL && voiceReady()) { "Manual ACK + readback required for this boot" }
        prepareLight(on, "speech", job)
    }

    private suspend fun prepareLight(on: Boolean, source: String, job: Long) {
        age()
        val s = mutable.value
        if (!s.connected || s.sample?.fresh(clock()) != true || s.request?.active == true) return
        val sample = s.sample
        val epoch = ++generation
        val start = clock()
        val fields = "physical_bedroom bedroom light ${if (on) "ON" else "OFF"}"
        mutable.value = s.copy(request = LightRequest(on, RequestPhase.PREPARING, sample.boot, source = source, speechJob = job), message = "Preparing light request; reported state unchanged")
        try {
            val p = request("/api/proposal", "$source $fields $job")
            if (epoch != generation || mutable.value.request?.phase != RequestPhase.PREPARING) return
            require(p.optString("target") == "physical_bedroom" && p.optString("room") == "bedroom" &&
                p.optString("device") == "light" && p.optString("action") == if (on) "ON" else "OFF")
            val id = uint(p, "proposal_id"); require(id > 0 && p.opt("authorized") == false)
            val lease = uint(p, "expires_in_ms").coerceAtMost(15000)
            require(clock() < start + lease && mutable.value.sample?.boot == sample.boot && mutable.value.sample?.fresh(clock()) == true && mutable.value.connected)
            mutable.value = mutable.value.copy(request = LightRequest(on, RequestPhase.PROPOSED, sample.boot, id, start + lease, source = source, speechJob = job),
                message = "Requested light ${if (on) "ON" else "OFF"}; confirm proposal #$id")
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
        val epoch = ++generation
        mutable.value = mutable.value.copy(request = r.copy(phase = RequestPhase.SENDING), message = "Confirming; reported state unchanged")
        try {
            val j = request("/api/confirm", "${r.proposal} physical_bedroom bedroom light ${if (r.on) "ON" else "OFF"}")
            if (epoch != generation || mutable.value.request?.phase != RequestPhase.SENDING) return
            val id = uint(j, "id"); require(j.opt("queued") == true && id > 0)
            require(mutable.value.connected && mutable.value.sample?.boot == r.boot && mutable.value.sample?.fresh(clock()) == true)
            mutable.value = mutable.value.copy(request = r.copy(phase = RequestPhase.PENDING, id = id, sampleBefore = sample.uptime, deadline = clock() + 5000),
                message = "Requested ${if (r.on) "ON" else "OFF"} queued (#$id); waiting for ACK + board report")
        } catch (e: CancellationException) { throw e
        } catch (e: Exception) {
            if (epoch == generation) finish(if (e is BoardRejected) RequestPhase.REJECTED else RequestPhase.OUTCOME_UNKNOWN,
                if (e is BoardRejected) "Confirmation rejected (HTTP ${e.status})" else "Confirmation outcome unknown. No automatic retry")
        }
    }

    companion object {
        private fun uint(j: JSONObject, key: String): Long {
            val value = j.get(key)
            require(value is Int || value is Long)
            return (value as Number).toLong().also { require(it in 0..0xffffffffL) }
        }
        fun newer(next: Long, previous: Long) = ((next - previous) and 0xffffffffL) in 1..0x7fffffffL
    }
}
