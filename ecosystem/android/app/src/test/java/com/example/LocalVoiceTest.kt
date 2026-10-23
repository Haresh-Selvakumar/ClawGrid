package com.example

import com.example.data.remote.*
import kotlinx.coroutines.CompletableDeferred
import kotlinx.coroutines.launch
import kotlinx.coroutines.runBlocking
import org.json.JSONObject
import org.junit.Assert.*
import org.junit.Test

class LocalVoiceTest {
    private var now = 100L
    private val pcm = ByteArray(3200)
    private var sample = JSONObject("""{"room":"bedroom","known":true,"authenticated":true,"boot_id":9,"sample_uptime_ms":100,"last_command":0,"age_ms":0,"light":false,"fan":false}""")
    private var id = 41L
    private var ackOn = true
    private val calls = mutableListOf<Pair<String, String?>>()
    private val hardware = HardwareRepository(BoardApi { path, body ->
        calls += path to body
        when (path) {
            "/api/state" -> sample
            "/api/proposal" -> JSONObject("""{"proposal_id":7,"target":"physical_bedroom","room":"bedroom","device":"light","expires_in_ms":15000,"authorized":false}""").put("action", body!!.split(' ')[4])
            "/api/confirm" -> JSONObject().put("queued", true).put("id", ++id)
            "/api/transaction" -> JSONObject().put("id", id).put("phase", 2).put("status", 0).put("outcome_known", true).put("light", ackOn)
            else -> error("Unexpected endpoint")
        }
    }) { now }
    private var uploads = 0
    private var uploaded = false
    private var supported = true
    private var result = JSONObject("""{"supported":true,"processing_source":"HARDWARE_MEASURED","authorized":false,"phase":4,"job":71,"error_code":0,"engine_errors":0,"command_id":2,"confidence":0.9,"samples":1600}""")
        .put("pcm_crc32", java.util.zip.CRC32().apply { update(pcm) }.value)
    private val api = object : SpeechApi {
        override suspend fun status(): JSONObject = if (uploaded) result else JSONObject().put("supported", supported).put("phase", 1).put("authorized", false).put("processing_source", "HARDWARE_MEASURED")
        override suspend fun upload(pcm: ByteArray): JSONObject {
            uploads++; uploaded = true
            return JSONObject("""{"queued":true,"job":71,"authorized":false}""")
        }
    }
    private suspend fun manual() {
        hardware.poll(); hardware.prepare(true); hardware.confirm()
        sample.put("last_command", 42).put("light", true).put("sample_uptime_ms", 200)
        hardware.poll(); assertTrue(hardware.voiceReady())
    }
    private fun voice() = LocalVoiceRepository(api, hardware) { }

    @Test fun audioNeedsManualAckAndReadbackFirst() = runBlocking {
        hardware.poll()
        assertTrue(runCatching { voice().propose(pcm) }.isFailure)
        assertEquals(0, uploads)
        hardware.prepare(true); hardware.confirm()
        assertFalse(hardware.voiceReady())
    }

    @Test fun recognizedSpeechUsesJobBoundProposalAndExplicitConfirmation() = runBlocking {
        manual(); val message = voice().propose(pcm)
        assertTrue(message.contains("Review and confirm"))
        assertTrue(calls.contains("/api/proposal" to "speech physical_bedroom bedroom light OFF 71"))
        assertEquals(1, calls.count { it.first == "/api/confirm" })
        assertEquals("speech", hardware.state.value.request!!.source)
        assertTrue(hardware.state.value.sample!!.light)
        hardware.confirm(); assertEquals(RequestPhase.PENDING, hardware.state.value.request!!.phase)
        sample.put("light", false).put("last_command", 43).put("sample_uptime_ms", 205); ackOn = false
        hardware.poll(); assertEquals(RequestPhase.REPORTED, hardware.state.value.request!!.phase)
    }

    @Test fun unsupportedModelDoesNotUpload() = runBlocking {
        manual(); supported = false
        assertTrue(runCatching { voice().propose(pcm) }.isFailure); assertEquals(0, uploads)
    }

    @Test fun noCommandWrongRoomFanAndLowConfidenceNeverPrepare() = runBlocking {
        manual()
        val before = calls.count { it.first == "/api/proposal" }
        for (command in listOf(0, 3, 5, 16)) {
            result.put("command_id", command)
            assertTrue(runCatching { voice().propose(pcm) }.isFailure)
        }
        result.put("command_id", 1).put("confidence", 0.699)
        assertTrue(runCatching { voice().propose(pcm) }.isFailure)
        assertEquals(before, calls.count { it.first == "/api/proposal" })
    }

    @Test fun wrongJobCrcSampleCountOrEngineErrorNeverPrepare() = runBlocking {
        manual()
        for ((field, value) in listOf("job" to 72, "pcm_crc32" to 1, "samples" to 1599, "error_code" to 7, "engine_errors" to 1)) {
            val saved = result.get(field); result.put(field, value)
            assertTrue(field, runCatching { voice().propose(pcm) }.isFailure)
            result.put(field, saved)
        }
        assertEquals(1, calls.count { it.first == "/api/proposal" })
    }

    @Test fun simulatedRecognitionNeverPreparesHardware() = runBlocking {
        manual(); result.put("processing_source", "HOST_MEASURED")
        assertTrue(runCatching { voice().propose(pcm) }.isFailure)
        assertEquals(1, calls.count { it.first == "/api/proposal" })
    }

    @Test fun controllerRebootRequiresNewManualVerification() = runBlocking {
        manual(); sample.put("boot_id", 10).put("sample_uptime_ms", 1); hardware.poll()
        assertFalse(hardware.voiceReady()); assertNull(hardware.state.value.manualVerifiedBoot)
        assertTrue(runCatching { voice().propose(pcm) }.isFailure); assertEquals(0, uploads)
    }

    @Test fun staleControllerDuringRecognitionCannotCreateProposal() = runBlocking {
        manual()
        val voice = LocalVoiceRepository(api, hardware) { now += 3000; hardware.age() }
        assertTrue(runCatching { voice.propose(pcm) }.isFailure)
        assertEquals(1, calls.count { it.first == "/api/proposal" })
    }

    @Test fun cancellingInFlightUploadDoesNotReplayOrPrepare() = runBlocking {
        manual(); val start = CompletableDeferred<Unit>(); val release = CompletableDeferred<Unit>()
        val voice = LocalVoiceRepository(object : SpeechApi {
            override suspend fun status() = api.status()
            override suspend fun upload(pcm: ByteArray): JSONObject { start.complete(Unit); release.await(); return api.upload(pcm) }
        }, hardware) { }
        val job = launch { assertTrue(runCatching { voice.propose(pcm) }.isFailure) }
        start.await(); voice.cancel(); release.complete(Unit); job.join()
        assertEquals(1, uploads); assertEquals(1, calls.count { it.first == "/api/proposal" })
    }

    @Test fun pcmBoundsAndLittleEndianAreExact() {
        for (size in listOf(0, 3199, 3201, 128002)) assertTrue(runCatching { GatewayPcm.validate(ByteArray(size)) }.isFailure)
        GatewayPcm.validate(ByteArray(3200)); GatewayPcm.validate(ByteArray(128000))
        val shorts = ShortArray(1600); shorts[0] = (-32768).toShort(); shorts[1] = 32767
        val raw = GatewayPcm.encode(shorts, shorts.size, 16000)
        assertArrayEquals(byteArrayOf(0, 0x80.toByte(), 0xff.toByte(), 0x7f), raw.copyOf(4))
    }

    @Test fun resamplingPreservesDcAndCapsFourSeconds() {
        for (rate in listOf(16000, 44100, 48000)) {
            val raw = GatewayPcm.encode(ShortArray(rate * 5) { 1000 }, rate * 5, rate)
            assertEquals(128000, raw.size)
            for (i in raw.indices step 2) assertEquals(1000, (raw[i].toInt() and 255) or ((raw[i + 1].toInt() and 255) shl 8))
        }
    }

    @Test fun physicalPhoneAddressRejectsLoopbackAndEmulatorAliases() {
        assertEquals("http://192.168.4.1", GatewayAddress.validate("http://192.168.4.1/"))
        assertEquals("http://192.168.1.25:80", GatewayAddress.validate("http://192.168.1.25:80"))
        for (url in listOf("http://localhost", "http://127.0.0.1", "http://10.0.2.2:8770", "https://example.com", "http://192.168.4.1/api", "http://user@192.168.4.1", "http://192.168.4.1?token=x"))
            assertTrue(url, runCatching { GatewayAddress.validate(url) }.isFailure)
    }
}
