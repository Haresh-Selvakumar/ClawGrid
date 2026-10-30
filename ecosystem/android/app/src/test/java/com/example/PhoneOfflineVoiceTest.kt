package com.example

import com.example.data.remote.*
import kotlinx.coroutines.runBlocking
import org.json.JSONArray
import org.json.JSONObject
import org.junit.Assert.*
import org.junit.Test

class PhoneOfflineVoiceTest {
    private fun final(text: String, confidence: Double = 0.95): String = JSONObject().put("text", text)
        .put("result", JSONArray(text.split(' ').filter { it.isNotEmpty() }.map {
            JSONObject().put("word", it).put("conf", confidence)
        })).toString()

    @Test fun everyFrozenPhraseHasOneExactTarget() {
        val expected = mapOf("light on" to true, "light off" to false,
            "turn on the light" to true, "turn off the light" to false,
            "turn on bedroom light" to true, "turn off bedroom light" to false)
        assertTrue(PhoneVoicePolicy.lightPhrases.entries.containsAll(expected.entries))
        assertEquals(12, PhoneVoicePolicy.lightPhrases.size)
        assertEquals(24, PhoneVoicePolicy.phrases.size)
        assertEquals(PhoneVoicePolicy.lightPhrases.keys.toList(), PhoneVoicePolicy.allowedPhrases("bedroom"))
        assertTrue(PhoneVoicePolicy.allowedPhrases("kitchen").isEmpty())
        assertEquals(PhoneVoicePolicy.lightPhrases.keys.map { it.replace("light", "fan") }, PhoneVoicePolicy.allowedPhrases("bedroom", "fan"))
        assertTrue(PhoneVoicePolicy.allowedPhrases("bedroom", "heater").isEmpty())
        for ((phrase, on) in expected) {
            val command = PhoneVoicePolicy.parse(listOf(final(phrase)), "bedroom")!!
            assertEquals(on, command.on)
            assertEquals("Bedroom / light / ${if (on) "ON" else "OFF"}", command.label)
        }
    }
    @Test fun exactVariantsPunctuationAndStructuredTarget() {
        for (phrase in listOf("light on", "light off", "fan on", "fan off", "turn on the light", "turn the light off", "switch on the fan", "switch the fan off")) {
            val text = "  " + phrase.uppercase().replace(" ", "  ") + "! "
            val json = JSONObject(final(phrase)).put("text", text)
            val decision = PhoneVoicePolicy.decide(listOf(json.toString()), "bedroom")
            assertEquals(PhoneVoiceReason.ACCEPTED, decision.reason)
            assertEquals(phrase, decision.preview!!.normalizedText)
            assertEquals("bedroom", decision.command!!.room)
            assertEquals(decision.command, decision.preview.intent)
        }
        val punctuation = JSONObject(final("light on")).put("text", "LIGHT, ON!")
        assertNotNull(PhoneVoicePolicy.parse(listOf(punctuation.toString()), "bedroom"))
    }
    @Test fun explicitGrammarIncludesUnknownAndNegativesWithoutAliases() {
        val grammar = JSONArray(PhoneVoicePolicy.decoderGrammar)
        val entries = (0 until grammar.length()).map { grammar.getString(it) }
        assertTrue(entries.contains("[unk]")); assertTrue(entries.contains("don't turn the light on"))
        assertTrue(entries.containsAll(PhoneVoicePolicy.phrases.keys))
        assertFalse(entries.contains("dawn on fine"))
        for ((phrase, detail) in listOf("light on off" to "Conflicting", "light and fan on" to "Multiple",
            "don't turn the light on" to "Negation", "turn the television on" to "unsupported device")) {
            val result = PhoneVoicePolicy.decide(listOf(final(phrase)), "bedroom")
            assertNull(result.command); assertTrue(result.preview!!.detail.contains(detail))
        }
        assertNull(PhoneVoicePolicy.parse(listOf(final("[unk]")), "bedroom"))
        assertNull(PhoneVoicePolicy.parse(listOf(final("dawn on fine")), "bedroom"))
        assertNull(PhoneVoicePolicy.parse(listOf(final("light on")), "virtual_rooms"))
    }
    @Test fun fanPhrasesKeepWholeTextConfidenceAndRoomChecks() {
        for ((phrase, on) in PhoneVoicePolicy.lightPhrases) {
            val fanPhrase = phrase.replace("light", "fan")
            val command = PhoneVoicePolicy.parse(listOf(final(fanPhrase)), "bedroom")!!
            assertEquals("fan", command.device); assertEquals(on, command.on)
            assertEquals("Bedroom / fan / ${if (on) "ON" else "OFF"}", command.label)
            assertNull(PhoneVoicePolicy.parse(listOf(final(fanPhrase, 0.69999)), "bedroom"))
            assertNull(PhoneVoicePolicy.parse(listOf(final(fanPhrase)), "kitchen"))
        }
        assertNull(PhoneVoicePolicy.parse(listOf(final("fan on"), final("light off")), "bedroom"))
        assertNull(PhoneVoicePolicy.parse(listOf(final("do not turn on the fan")), "bedroom"))
    }
    @Test fun fanRequiresOwnManualProofAndExactProposalBeforeConfirm() = runBlocking {
        val f = Fixture(); f.ready()
        f.repo.preparePhone(true, 9, 77, 31000, "fan")
        assertTrue(f.calls.isEmpty())
        f.repo.prepare(false, "fan"); f.repo.confirm(); f.uptime++; f.repo.poll()
        assertTrue(f.repo.voiceReady("fan")); f.calls.clear()
        f.repo.preparePhone(true, 9, 77, 31000, "fan")
        assertEquals(listOf("/api/proposal" to "phone physical_bedroom bedroom fan ON 77 9"), f.calls)
        assertEquals("fan", f.repo.state.value.request!!.device)
        assertEquals(RequestPhase.PROPOSED, f.repo.state.value.request!!.phase)
        f.repo.cancelProposal(); f.repo.confirm()
        assertFalse(f.calls.any { it.first == "/api/confirm" })
        f.boot++; f.uptime=1; f.repo.poll()
        assertFalse(f.repo.voiceReady("fan")); assertFalse(f.repo.voiceReady())
    }
    @Test fun previewPreservesExactFinalTextAndShowsActualNormalization() {
        val raw = "  LIGHT\t OFF\n"
        val json = JSONObject(final("light off")).put("text", raw).toString()
        val decision = PhoneVoicePolicy.decide(listOf(json), "bedroom")
        assertEquals(PhoneVoiceReason.ACCEPTED, decision.reason)
        assertEquals(listOf(raw), decision.preview!!.exactFinalSegments)
        assertEquals("light off", decision.preview!!.normalizedText)
        assertFalse(decision.command!!.on)
    }
    @Test fun previewDoesNotInventAnAliasAndDoesNotLeakThroughToString() {
        // Synthetic parser fixture only, not a claim about any phone transcript.
        val decision = PhoneVoicePolicy.decide(listOf(final("lights off")), "bedroom")
        assertEquals(PhoneVoiceReason.UNSUPPORTED_WORDS, decision.reason)
        assertNull(decision.command)
        assertEquals("lights off", decision.preview!!.normalizedText)
        assertTrue(decision.preview!!.detail.contains("COMPLETE"))
        assertFalse(decision.toString().contains("lights off"))
        assertFalse(decision.metadata.contains("lights off"))
        assertTrue(decision.preview.toString().contains("REDACTED"))
    }
    @Test fun previewIncludesAllFinalSegmentsAndSpecificSchemaRejection() {
        val decision = PhoneVoicePolicy.decide(listOf(final("light on"), final("light off")), "bedroom")
        assertEquals(listOf("light on", "light off"), decision.preview!!.exactFinalSegments)
        assertEquals("light on light off", decision.preview!!.normalizedText)
        assertNull(decision.command)
        val invalid = JSONObject(final("light on")); invalid.getJSONArray("result").getJSONObject(1).put("word", "off")
        val mismatch = PhoneVoicePolicy.decide(listOf(invalid.toString()), "bedroom")
        assertEquals(PhoneVoiceReason.INVALID_RESULT, mismatch.reason)
        assertTrue(mismatch.preview!!.detail.contains("disagree"))
        assertEquals("light on", mismatch.preview!!.normalizedText)
    }
    @Test fun previewKeepsConfidenceThresholdAndSilenceReason() {
        val low = PhoneVoicePolicy.decide(listOf(final("light off", 0.69999)), "bedroom")
        assertEquals(PhoneVoiceReason.LOW_CONFIDENCE, low.reason)
        assertEquals("light off", low.preview!!.normalizedText)
        assertTrue(low.preview!!.detail.contains("0.7"))
        assertEquals(PhoneVoiceReason.SILENCE, PhoneVoicePolicy.decide(listOf(final("")), "bedroom").preview!!.reason)
    }
    @Test fun rejectWholeUnknownConflictingNegatedOrUnsupportedUtterance() {
        for (text in listOf("", "do not turn on the light", "light on and light off", "light on please",
            "fan toggle", "light on and fan off", "kitchen fan on", "kitchen light on", "light toggle", "turn on", "bedroom", "what time is it",
            "light on in kitchen", "light off or on", "light on light on")) {
            assertNull(text, PhoneVoicePolicy.parse(listOf(final(text)), "bedroom"))
        }
    }
    @Test fun rejectPartialMissingWordsLowNonFiniteConfidenceAndRoom() {
        assertNull(PhoneVoicePolicy.parse(listOf("{\"partial\":\"light on\"}"), "bedroom"))
        assertNull(PhoneVoicePolicy.parse(listOf("{\"text\":\"light on\"}"), "bedroom"))
        assertNull(PhoneVoicePolicy.parse(listOf(final("light on", 0.69999)), "bedroom"))
        assertNotNull(PhoneVoicePolicy.parse(listOf(final("light on", 0.7)), "bedroom"))
        assertNull(PhoneVoicePolicy.parse(listOf(final("light on", 1.01)), "bedroom"))
        for (room in listOf(null, "kitchen", "Bedroom")) assertNull(PhoneVoicePolicy.parse(listOf(final("light on")), room))
    }
    @Test fun neverTakeLastSegmentOfConflictingOrUnknownSpeech() {
        assertNull(PhoneVoicePolicy.parse(listOf(final("light on"), final("light off")), "bedroom"))
        assertNull(PhoneVoicePolicy.parse(listOf(final("do not"), final("light on")), "bedroom"))
        assertNotNull(PhoneVoicePolicy.parse(listOf(final(""), final("light off"), final("")), "bedroom"))
    }
    @Test fun rejectMismatchedWordTextOrOversizedResult() {
        val json = JSONObject(final("light on")); json.getJSONArray("result").getJSONObject(1).put("word", "off")
        assertNull(PhoneVoicePolicy.parse(listOf(json.toString()), "bedroom"))
        assertNull(PhoneVoicePolicy.parse(List(17) { final("") }, "bedroom"))
    }
    @Test fun rejectionCategoriesAreDistinctWithoutRetainingTranscripts() {
        for ((segments, reason) in listOf(
            listOf(final("")) to PhoneVoiceReason.SILENCE,
            listOf(final("what time is it")) to PhoneVoiceReason.UNSUPPORTED_WORDS,
            listOf(final("light off", 0.6999)) to PhoneVoiceReason.LOW_CONFIDENCE,
            listOf("{\"text\":\"light off\"}") to PhoneVoiceReason.INVALID_RESULT)) {
            val decision = PhoneVoicePolicy.decide(segments, "bedroom")
            assertEquals(reason, decision.reason); assertNull(decision.command)
            assertFalse(decision.metadata.contains("what time"))
            assertFalse(decision.metadata.contains("light off"))
        }
        assertEquals(PhoneVoiceReason.ROOM_REQUIRED, PhoneVoicePolicy.decide(listOf(final("light off")), null).reason)
    }
    @Test fun acceptedDecisionCarriesOnlyTypedTargetAndConfidenceMetadata() {
        val decision = PhoneVoicePolicy.decide(listOf(final("turn off bedroom light", 0.7)), "bedroom")
        assertEquals(PhoneVoiceReason.ACCEPTED, decision.reason)
        assertEquals("Bedroom / light / OFF", decision.command!!.label)
        assertEquals(4, decision.wordCount); assertEquals(0.7, decision.minimumConfidence!!, 0.0)
        assertFalse(decision.metadata.contains("turn off"))
    }

    private class Fixture {
        var now = 1000L
        var boot = 9L
        var uptime = 1L
        var on = false
        var last = 0L
        var outputs = false
        var phoneEchoBoot = 9L
        var phoneEchoCapture = 77L
        var echoOrigin = "phone"
        var ack = false
        val calls = mutableListOf<Pair<String, String?>>()
        val repo = HardwareRepository(BoardApi { path, body ->
            calls += path to body
            when (path) {
                "/api/state" -> JSONObject().put("room", "bedroom").put("known", true).put("authenticated", true)
                    .put("boot_id", boot).put("sample_uptime_ms", uptime).put("age_ms", 0).put("light", on)
                    .put("fan", false).put("last_command", last).put("occupancy", "UNKNOWN").put("physical_outputs_enabled", outputs)
                "/api/proposal" -> JSONObject().put("proposal_id", 8).put("target", "physical_bedroom").put("room", "bedroom")
                    .put("device", if (body!!.contains(" bedroom fan ")) "fan" else "light").put("action", if (body.contains(" ON ")) "ON" else "OFF")
                    .put("expires_in_ms", 15000).put("authorized", false).put("origin", echoOrigin)
                    .put("controller_boot", phoneEchoBoot).put("capture_id", phoneEchoCapture)
                "/api/confirm" -> JSONObject().put("queued", true).put("id", 42)
                "/api/transaction" -> JSONObject().put("id", 42).put("phase", if (ack) 2 else 1)
                    .put("status", 0).put("light", on).put("fan", false).put("outcome_known", ack)
                else -> error("No speech/audio or other network endpoint allowed")
            }
        }) { now }
        suspend fun ready() {
            repo.poll(); repo.prepare(false); repo.confirm()
            ack = true; last = 42; uptime++; repo.poll()
            assertTrue(repo.voiceReady()); calls.clear()
        }
    }
    @Test fun phoneProposalCarriesCapturedBootAndNonceAndCannotActuateUntilConfirm() = runBlocking {
        val f = Fixture(); f.ready()
        f.repo.preparePhone(true, 9, 77, 31000)
        assertEquals(listOf("/api/proposal" to "phone physical_bedroom bedroom light ON 77 9"), f.calls)
        assertEquals(RequestPhase.PROPOSED, f.repo.state.value.request!!.phase)
        assertFalse(f.repo.state.value.sample!!.light)
        f.ack = false; f.repo.confirm()
        assertEquals(RequestPhase.PENDING, f.repo.state.value.request!!.phase)
        f.on = true; f.last = 42; f.uptime++; f.repo.poll()
        assertEquals(RequestPhase.PENDING, f.repo.state.value.request!!.phase)
        f.ack = true; f.uptime++; f.repo.poll()
        assertEquals(RequestPhase.REPORTED, f.repo.state.value.request!!.phase)
        assertEquals(1, f.calls.count { it.first == "/api/confirm" })
    }
    @Test fun wrongBootNonceOrOriginEchoCannotProduceProposal() = runBlocking {
        for (variant in 0..2) {
            val f = Fixture(); f.ready()
            when (variant) { 0 -> f.phoneEchoBoot++; 1 -> f.phoneEchoCapture++; 2 -> f.echoOrigin = "speech" }
            f.repo.preparePhone(true, 9, 77, 31000); f.repo.confirm()
            assertEquals(RequestPhase.REJECTED, f.repo.state.value.request!!.phase)
            assertFalse(f.calls.any { it.first == "/api/confirm" })
        }
    }
    @Test fun voiceContextCancelledByRoomRevisionExpiryStaleOrReboot() = runBlocking {
        val f = Fixture(); f.ready(); val session = PhoneVoiceSession { f.now }
        val c = session.begin("bedroom", f.repo, 77)
        assertTrue(session.current(c, "bedroom", f.repo))
        assertFalse(session.current(c, null, f.repo))
        session.cancel(); assertFalse(session.current(c, "bedroom", f.repo))
        val next = session.begin("bedroom", f.repo, 78)
        f.now += 3000; assertFalse(session.current(next, "bedroom", f.repo))
        f.uptime++; f.repo.poll(); val reboot = session.begin("bedroom", f.repo, 79)
        f.boot++; f.uptime = 1; f.repo.poll(); assertFalse(session.current(reboot, "bedroom", f.repo))
        assertFalse(f.calls.any { it.first == "/api/confirm" })
    }
    @Test fun contextDeadlineAlsoLimitsProposalConfirmation() = runBlocking {
        val f = Fixture(); f.ready()
        f.repo.preparePhone(true, 9, 77, 1100)
        f.now = 1100; f.repo.confirm()
        assertFalse(f.calls.any { it.first == "/api/confirm" })
        assertEquals(RequestPhase.EXPIRED, f.repo.state.value.request!!.phase)
    }
    @Test fun noReplayOnContextCancelOrControllerReboot() = runBlocking {
        val f = Fixture(); f.ready(); f.repo.preparePhone(true, 9, 77, 31000)
        f.repo.cancelProposal(); f.repo.confirm(); f.repo.poll()
        assertFalse(f.calls.any { it.first == "/api/confirm" })
        f.repo.preparePhone(true, 9, 77, 31000); f.boot++; f.uptime = 1; f.repo.poll(); f.repo.confirm()
        assertFalse(f.calls.any { it.first == "/api/confirm" })
    }
    @Test fun outputsEnabledOrWrongCapturedBootCannotPropose() = runBlocking {
        val f = Fixture(); f.ready(); f.repo.preparePhone(true, 8, 77, 31000)
        assertTrue(f.calls.isEmpty())
        f.outputs = true; f.uptime++; f.repo.poll(); f.calls.clear()
        f.repo.preparePhone(true, 9, 77, 31000); assertTrue(f.calls.isEmpty())
    }
}
