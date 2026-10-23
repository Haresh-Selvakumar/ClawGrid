package com.example.data.remote

import com.example.BuildConfig
import kotlinx.coroutines.*
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import okhttp3.*
import org.json.JSONArray
import org.json.JSONObject
import java.util.UUID
import java.util.concurrent.TimeUnit

data class EcosystemState(
    val status: String = "Disconnected — no sample readings are shown as hardware",
    val snapshot: JSONObject? = null,
    val history: List<JSONObject> = emptyList(),
    val ack: String = "No command submitted",
    val pending: Boolean = false
)

class EcosystemClient {
    private val http = OkHttpClient.Builder().connectTimeout(8, TimeUnit.SECONDS)
        .readTimeout(0, TimeUnit.MILLISECONDS).pingInterval(20, TimeUnit.SECONDS).build()
    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.IO)
    private val mutable = MutableStateFlow(EcosystemState())
    val state = mutable.asStateFlow()
    private var socket: WebSocket? = null
    private var wire: RealtimeWire? = null
    private var heartbeat: Job? = null
    private var timeout: Job? = null
    private var staleCheck: Job? = null
    @Volatile private var stopped = true
    @Volatile private var lastReceived = 0L
    @Volatile private var pendingId: String? = null
    private var session = ""
    private var sequence = -1L
    private var lastSent = 0L

    fun connect(token: String) {
        disconnect()
        require(token.length >= 16) { "Enter the existing ClawGrid control token" }
        wire = RealtimeWire(token, BuildConfig.CLAWGRID_HOME_ID, BuildConfig.CLAWGRID_GATEWAY_ID)
        stopped = false
        open()
        staleCheck = scope.launch {
            while (!stopped) {
                delay(500)
                if (lastReceived != 0L && android.os.SystemClock.elapsedRealtime() - lastReceived > 4000) {
                    lastReceived = 0L
                    mutable.update { it.copy(snapshot = null, status = "STALE — no fresh ESP gateway sample") }
                }
            }
        }
    }

    private fun open() {
        if (stopped) return
        mutable.update { it.copy(status = "Connecting to encrypted Supabase channel…", snapshot = null) }
        val url = BuildConfig.SUPABASE_URL.trimEnd('/').replace("https://", "wss://") +
            "/realtime/v1/websocket?apikey=${BuildConfig.SUPABASE_ANON_KEY}&vsn=1.0.0"
        socket = http.newWebSocket(Request.Builder().url(url).build(), object : WebSocketListener() {
            override fun onOpen(webSocket: WebSocket, response: Response) {
                if (stopped || webSocket !== socket) { webSocket.close(1000, "Superseded connection"); return }
                val config = JSONObject().put("broadcast", JSONObject().put("ack", false).put("self", false))
                    .put("presence", JSONObject().put("enabled", false)).put("postgres_changes", JSONArray()).put("private", false)
                webSocket.send(frame("phx_join", JSONObject().put("config", config)
                    .put("access_token", BuildConfig.SUPABASE_ANON_KEY), "join"))
                heartbeat?.cancel()
                heartbeat = scope.launch {
                    while (!stopped) {
                        delay(20000)
                        webSocket.send(JSONObject().put("topic", "phoenix").put("event", "heartbeat")
                            .put("payload", JSONObject()).put("ref", UUID.randomUUID().toString()).toString())
                    }
                }
            }

            override fun onMessage(webSocket: WebSocket, text: String) {
                if (stopped || webSocket !== socket) return
                if (text.length > 65536) return
                runCatching {
                    val outer = JSONObject(text)
                    if (outer.optString("event") == "phx_reply" && outer.optString("ref") == "join") {
                        mutable.update { it.copy(status = if (outer.optJSONObject("payload")?.optString("status") == "ok")
                            "Supabase connected; waiting for authenticated ESP data" else "Supabase channel rejected") }
                        return
                    }
                    if (outer.optString("event") != "broadcast") return
                    val event = outer.optJSONObject("payload") ?: return
                    if (event.optString("event") != "clawgrid_secure_v1") return
                    val value = wire?.decrypt(event.getJSONObject("payload")) ?: return
                    val sent = value.optLong("sent_at_ms", 0)
                    val age = System.currentTimeMillis() - sent
                    if (sent == 0L || age > 5000 || age < -2000) return
                    when (value.optString("kind")) {
                        "snapshot" -> {
                            if (value.optString("source") != "HARDWARE_MEASURED" ||
                                value.optString("home_id") != BuildConfig.CLAWGRID_HOME_ID ||
                                value.optString("gateway_id") != BuildConfig.CLAWGRID_GATEWAY_ID) return
                            val incomingSession = value.optString("session_id")
                            val incomingSequence = value.optLong("sequence", -1)
                            if (sent <= lastSent) return
                            if (incomingSession == session && incomingSequence <= sequence) return
                            session = incomingSession; sequence = incomingSequence
                            lastSent = sent
                            lastReceived = android.os.SystemClock.elapsedRealtime()
                            mutable.update { it.copy(snapshot = value, history = (it.history + value).takeLast(60),
                                status = if (fresh(value)) "LIVE · authenticated ESP state through Supabase" else "Supabase live; A unavailable/stale") }
                        }
                        "command_ack" -> if (value.optString("cloud_id") == pendingId) {
                            timeout?.cancel(); pendingId = null
                            val result = value.optJSONObject("result") ?: JSONObject()
                            val accepted = result.optInt("phase") == 2 && result.optInt("status", -1) == 0
                            mutable.update { it.copy(pending = false, ack = if (accepted)
                                "ACK: A accepted command #${result.optLong("id")}. Physical load not measured."
                                else "Command rejected / outcome unknown. No automatic retry.") }
                        }
                    }
                } // Wrong-token, malformed and tampered frames are discarded.
            }

            private fun lost(webSocket: WebSocket) {
                if (stopped || webSocket !== socket) return
                heartbeat?.cancel(); timeout?.cancel(); pendingId = null
                mutable.update { it.copy(snapshot = null, pending = false, status = "Disconnected; reconnecting without replay",
                    ack = if (it.pending) "Connection lost; command outcome unknown" else it.ack) }
                if (!stopped) scope.launch { delay(2000); open() }
            }
            override fun onFailure(webSocket: WebSocket, t: Throwable, response: Response?) { lost(webSocket) }
            override fun onClosed(webSocket: WebSocket, code: Int, reason: String) { lost(webSocket) }
        })
    }

    fun command(device: String, on: Boolean, physical: Boolean) {
        require(device in listOf("light", "fan"))
        require(fresh(mutable.value.snapshot) && !mutable.value.pending) { "Fresh authenticated A data is required" }
        val id = UUID.randomUUID().toString()
        val issued = System.currentTimeMillis() / 1000.0
        val value = JSONObject().put("kind", "command").put("id", id)
            .put("target", if (physical) "physical_bedroom" else "virtual_rooms")
            .put("room", "bedroom").put("device", device).put("action", if (on) "ON" else "OFF")
            .put("issued_at", issued).put("expires_at", issued + 10)
        val envelope = wire?.encrypt(value) ?: error("Not connected")
        pendingId = id
        mutable.update { it.copy(pending = true, ack = "Sent through Supabase; waiting for A ACK…") }
        if (socket?.send(frame("broadcast", JSONObject().put("type", "broadcast")
                .put("event", "clawgrid_secure_v1").put("payload", envelope))) != true) {
            pendingId = null
            mutable.update { it.copy(pending = false, ack = "Not sent; disconnected") }
            return
        }
        timeout = scope.launch {
            delay(16000); pendingId = null
            mutable.update { it.copy(pending = false, ack = "ACK timeout; outcome unknown. No automatic retry.") }
        }
    }

    private fun frame(event: String, payload: JSONObject, ref: String = UUID.randomUUID().toString()): String =
        JSONObject().put("topic", "realtime:clawgrid_live").put("event", event).put("payload", payload).put("ref", ref).toString()

    fun disconnect() {
        stopped = true; staleCheck?.cancel(); heartbeat?.cancel(); timeout?.cancel()
        socket?.close(1000, "User disconnected"); socket = null; wire = null; pendingId = null; lastReceived = 0
        mutable.update { it.copy(snapshot = null, pending = false, status = "Disconnected") }
    }

    companion object {
        fun fresh(snapshot: JSONObject?): Boolean {
            val a = snapshot?.optJSONObject("node_a") ?: return false
            val age = a.optLong("age_ms", Long.MAX_VALUE)
            return a.optBoolean("known", false) && a.optBoolean("authenticated", false) && age in 0..2999
        }
    }
}
