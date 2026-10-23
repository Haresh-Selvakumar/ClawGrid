package com.example.data.remote

import kotlinx.coroutines.delay
import kotlinx.coroutines.withTimeout
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.RequestBody.Companion.toRequestBody
import org.json.JSONObject
import java.util.concurrent.TimeUnit
import kotlin.math.ceil
import kotlin.math.floor
import kotlin.math.roundToInt

/** Raw samples only: no WAV header, codec, Android recognizer or cloud service. */
object GatewayPcm {
    const val RATE = 16000
    const val MAX_SAMPLES = 64000
    fun validate(bytes: ByteArray) { require(bytes.size in 3200..128000 && bytes.size % 2 == 0) { "Record 0.1–4 seconds of mono PCM16LE" } }
    fun encode(samples: ShortArray, count: Int, sourceRate: Int): ByteArray {
        require(sourceRate in RATE..192000 && count in 0..samples.size)
        val length = minOf(MAX_SAMPLES, count.toLong().times(RATE).div(sourceRate).toInt())
        val output = ByteArray(length * 2)
        val ratio = sourceRate.toDouble() / RATE
        for (i in 0 until length) {
            val start = i * ratio; val end = (i + 1) * ratio
            var sum = 0.0
            for (j in floor(start).toInt() until minOf(count, ceil(end).toInt())) {
                val weight = (minOf(end, j + 1.0) - maxOf(start, j.toDouble())).coerceAtLeast(0.0)
                sum += samples[j] * weight
            }
            val value = (sum / ratio).roundToInt().coerceIn(-32768, 32767)
            output[i * 2] = value.toByte(); output[i * 2 + 1] = (value shr 8).toByte()
        }
        validate(output)
        return output
    }
}

interface SpeechApi {
    suspend fun status(): JSONObject
    suspend fun upload(pcm: ByteArray): JSONObject
}

class HttpSpeechApi(private val base: String, private val token: String, wifiSockets: javax.net.SocketFactory? = null, wifiDns: okhttp3.Dns? = null) : SpeechApi {
    private val http = OkHttpClient.Builder().apply { if (wifiSockets != null) socketFactory(wifiSockets); if (wifiDns != null) dns(wifiDns) }.retryOnConnectionFailure(false).followRedirects(false).followSslRedirects(false)
        .connectTimeout(1500, TimeUnit.MILLISECONDS).readTimeout(2, TimeUnit.SECONDS)
        .writeTimeout(4, TimeUnit.SECONDS).callTimeout(5, TimeUnit.SECONDS).build()
    private suspend fun call(pcm: ByteArray?): JSONObject = withContext(Dispatchers.IO) {
        require(token.isNotBlank()) { "Existing gateway token required" }
        val request = Request.Builder().url(base.trimEnd('/') + if (pcm == null) "/api/speech" else "/api/speech/audio")
            .header("X-ClawGrid-Token", token)
        if (pcm != null) { GatewayPcm.validate(pcm); request.post(pcm.toRequestBody("application/octet-stream".toMediaType())) }
        http.newCall(request.build()).execute().use {
            if (!it.isSuccessful) throw BoardRejected(it.code)
            require(it.code == if (pcm == null) 200 else 202)
            val text = it.body?.string().orEmpty(); require(text.length <= 16384)
            JSONObject(text)
        }
    }
    override suspend fun status() = call(null)
    override suspend fun upload(pcm: ByteArray) = call(pcm)
}

/** This creates a speech-bound proposal only. Existing confirmation + ACK/readback is still mandatory. */
class LocalVoiceRepository(
    private val speech: SpeechApi, private val hardware: HardwareRepository,
    private val pause: suspend () -> Unit = { delay(200) }
) {
    private var revision = 0L
    fun cancel() { revision++ }
    suspend fun propose(pcm: ByteArray): String {
        GatewayPcm.validate(pcm)
        require(hardware.voiceReady()) { "Complete manual light ACK + readback on this boot first" }
        val boot = hardware.state.value.sample!!.boot
        val mine = ++revision
        fun current() {
            require(mine == revision && hardware.voiceReady() && hardware.state.value.sample?.boot == boot) { "Voice cancelled or controller unavailable/rebooted; record again" }
        }
        return withTimeout(12000) {
            current()
            val ready = speech.status(); measured(ready)
            require(ready.opt("supported") == true && ready.optInt("phase") in listOf(1, 4)) { "ESP speech model unavailable or busy" }
            current()
            val accepted = speech.upload(pcm); measured(accepted)
            require(accepted.opt("queued") == true && accepted.opt("authorized") == false)
            val job = accepted.getLong("job"); require(job in 1..0xffffffffL)
            while (true) {
                current(); pause(); current()
                val result = speech.status(); measured(result); current()
                require(result.opt("supported") == true && result.getLong("job") == job) { "Speech job replaced or gateway restarted; record again" }
                require(result.optInt("phase") != 5) { "ESP speech engine failed" }
                if (result.optInt("phase") != 4) continue
                val confidence = result.getDouble("confidence")
                require(result.getInt("error_code") == 0 && result.getInt("engine_errors") == 0 && confidence.isFinite() && confidence in 0.7..1.0) { "No confident ESP recognition; nothing proposed" }
                val crc = java.util.zip.CRC32().apply { update(pcm) }.value
                require(result.getLong("pcm_crc32") == crc && result.getInt("samples") == pcm.size / 2) { "Recognized audio does not match uploaded PCM" }
                val command = result.getInt("command_id")
                require(command in 1..2) { "Only Bedroom light ON/OFF is integrated; recognized target was not submitted" }
                current()
                hardware.prepareSpeech(command == 1, job)
                require(hardware.state.value.request?.let { it.phase == RequestPhase.PROPOSED && it.source == "speech" && it.speechJob == job } == true) { "Speech proposal rejected; nothing confirmed" }
                return@withTimeout "ESP recognized Bedroom light ${if (command == 1) "ON" else "OFF"} (job $job). Review and confirm; reported state unchanged"
            }
            @Suppress("UNREACHABLE_CODE") error("No result")
        }
    }
    private fun measured(j: JSONObject) {
        for (key in listOf("source", "processing_source")) if (j.has(key)) require(j.optString(key) == "HARDWARE_MEASURED")
        require(j.optString("input_source") != "SIMULATED" && j.optString("execution_mode") != "HOST_ONLY")
        require(j.opt("authorized") == false) { "Speech cannot authorize actuation" }
    }
}
