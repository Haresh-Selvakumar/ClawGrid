package com.example

import com.example.data.remote.*
import kotlinx.coroutines.CompletableDeferred
import kotlinx.coroutines.launch
import kotlinx.coroutines.runBlocking
import org.json.JSONObject
import org.junit.Assert.*
import org.junit.Test
import java.io.IOException

class HardwareRepositoryTest {
    private var now = 1000L
    private var sample = report()
    private var transaction = JSONObject("""{"id":42,"phase":1,"status":0,"light":true,"outcome_known":false}""")
    private var proposal = JSONObject("""{"proposal_id":7,"target":"physical_bedroom","room":"bedroom","device":"light","action":"ON","expires_in_ms":15000,"authorized":false}""")
    private val calls = mutableListOf<Pair<String, String?>>()
    private var fail: String? = null
    private var reject: String? = null
    private val api = BoardApi { path, body ->
        calls += path to body
        if (fail == path) throw IOException("fixture disconnect")
        if (reject == path) throw BoardRejected(409)
        when (path) {
            "/api/state" -> sample
            "/api/proposal" -> proposal
            "/api/confirm" -> JSONObject("""{"queued":true,"id":42}""")
            "/api/transaction" -> transaction
            else -> error("Unexpected route")
        }
    }
    private val repo = HardwareRepository(api) { now }

    private fun report(boot: Long = 9, uptime: Long = 100, on: Boolean = false, command: Long = 1) = JSONObject()
        .put("room", "bedroom").put("known", true).put("authenticated", true).put("boot_id", boot)
        .put("sample_uptime_ms", uptime).put("age_ms", 0).put("light", on).put("fan", false)
        .put("last_command", command).put("occupancy", "UNKNOWN").put("physical_outputs_enabled", false)

    private suspend fun submit(on: Boolean = true) {
        proposal.put("action", if (on) "ON" else "OFF")
        repo.poll(); repo.prepare(on); repo.confirm()
    }

    private fun ack(on: Boolean = true, status: Int = 0) {
        transaction.put("phase", 2).put("outcome_known", true).put("status", status).put("light", on)
    }

    @Test fun exactContractAndHttpAcceptanceNeverChangesReport() = runBlocking {
        submit()
        assertEquals(RequestPhase.PENDING, repo.state.value.request!!.phase)
        assertFalse(repo.state.value.sample!!.light)
        assertTrue(calls.contains("/api/proposal" to "manual physical_bedroom bedroom light ON 0"))
        assertTrue(calls.contains("/api/confirm" to "7 physical_bedroom bedroom light ON"))
        assertEquals(1, calls.count { it.first == "/api/confirm" })
    }

    @Test fun ackRequiresAdvancingMatchingReadback() = runBlocking {
        submit(); ack(); repo.poll()
        assertEquals(RequestPhase.PENDING, repo.state.value.request!!.phase)
        sample = report(uptime = 105, on = true, command = 42); repo.poll()
        assertEquals(RequestPhase.REPORTED, repo.state.value.request!!.phase)
        assertTrue(repo.state.value.message.contains("Physical load not measured"))
    }

    @Test fun reportWithoutAckDoesNotComplete() = runBlocking {
        submit(); sample = report(uptime = 105, on = true, command = 42); repo.poll()
        assertEquals(RequestPhase.PENDING, repo.state.value.request!!.phase)
    }

    @Test fun wrongAckIdDoesNotComplete() = runBlocking {
        submit(); ack(); transaction.put("id", 41)
        sample = report(uptime = 105, on = true, command = 42); repo.poll()
        assertEquals(RequestPhase.PENDING, repo.state.value.request!!.phase)
    }

    @Test fun wrongReportCommandIdDoesNotComplete() = runBlocking {
        submit(); ack(); sample = report(uptime = 105, on = true, command = 41); repo.poll()
        assertEquals(RequestPhase.PENDING, repo.state.value.request!!.phase)
    }

    @Test fun offContractAlsoRequiresReport() = runBlocking {
        sample = report(on = true); submit(false); ack(false)
        assertTrue(repo.state.value.sample!!.light)
        sample = report(uptime = 105, on = false, command = 42); repo.poll()
        assertEquals(RequestPhase.REPORTED, repo.state.value.request!!.phase)
        assertTrue(calls.contains("/api/confirm" to "7 physical_bedroom bedroom light OFF"))
    }

    @Test fun safetyRejectionIsNotSuccess() = runBlocking {
        submit(); ack(status = 3); sample = report(uptime = 105); repo.poll()
        assertEquals(RequestPhase.REJECTED, repo.state.value.request!!.phase)
    }

    @Test fun rebootInvalidatesPendingEvenIfNewStateMatches() = runBlocking {
        submit(); ack(); sample = report(boot = 10, uptime = 1, on = true, command = 42); repo.poll()
        assertEquals(RequestPhase.OUTCOME_UNKNOWN, repo.state.value.request!!.phase)
        repeat(3) { sample = report(boot = 10, uptime = 5L + it); repo.poll() }
        assertEquals(1, calls.count { it.first == "/api/confirm" })
    }

    @Test fun rebootInvalidatesUnconfirmedProposal() = runBlocking {
        repo.poll(); repo.prepare(true); sample = report(boot = 10, uptime = 1); repo.poll(); repo.confirm()
        assertFalse(calls.any { it.first == "/api/confirm" })
    }

    @Test fun frozenSampleCannotRefreshAge() = runBlocking {
        repo.poll(); now += 2999; repo.poll()
        assertTrue(repo.state.value.connected)
        now++; repo.poll()
        assertFalse(repo.state.value.connected)
        repo.prepare(true)
        assertFalse(calls.any { it.first == "/api/proposal" })
    }

    @Test fun duplicateSampleChangingContentsRejected() = runBlocking {
        repo.poll(); sample.put("light", true); repo.poll()
        assertFalse(repo.state.value.connected)
    }

    @Test fun regressingSamplesAndBootRejected() = runBlocking {
        repo.poll(); sample = report(uptime = 99); repo.poll(); assertFalse(repo.state.value.connected)
        sample = report(boot = 8); repo.poll(); assertFalse(repo.state.value.connected)
    }

    @Test fun uptimeWrapIsAccepted() = runBlocking {
        sample = report(uptime = 0xfffffff0L); repo.poll()
        sample = report(uptime = 5); repo.poll(); assertTrue(repo.state.value.connected)
    }

    @Test fun missingNullOrUnauthenticatedTelemetryNeverFresh() = runBlocking {
        for (field in listOf("light", "fan", "boot_id", "age_ms", "last_command", "sample_uptime_ms", "authenticated", "known")) {
            val test = HardwareRepository(api) { now }
            sample = report().put(field, JSONObject.NULL); test.poll()
            assertFalse(field, test.state.value.connected)
        }
        sample = report().put("age_ms", -1); repo.poll(); assertFalse(repo.state.value.connected)
        sample = report().put("age_ms", 3000); repo.poll(); assertFalse(repo.state.value.connected)
    }

    @Test fun mismatchedOrExpiredProposalNeverConfirmed() = runBlocking {
        repo.poll(); proposal.put("room", "kitchen"); repo.prepare(true); repo.confirm()
        assertEquals(RequestPhase.REJECTED, repo.state.value.request!!.phase)
        proposal.put("room", "bedroom"); repo.prepare(true)
        now += 15000; repo.confirm()
        assertFalse(calls.any { it.first == "/api/confirm" })
    }

    @Test fun cancelNeverDispatchesAndResetDiscardsWithoutReplay() = runBlocking {
        repo.poll(); repo.prepare(true); repo.cancelProposal(); repo.confirm()
        repo.reset(); repo.poll(); repo.confirm()
        assertFalse(calls.any { it.first == "/api/confirm" })
        assertNull(repo.state.value.request)
    }

    @Test fun confirmationTransportLossIsUnknownAndNeverRetried() = runBlocking {
        fail = "/api/confirm"; submit()
        assertEquals(RequestPhase.OUTCOME_UNKNOWN, repo.state.value.request!!.phase)
        fail = null; repeat(3) { repo.poll(); repo.confirm() }
        assertEquals(1, calls.count { it.first == "/api/confirm" })
    }

    @Test fun confirmationHttpRejectionIsRejected() = runBlocking {
        reject = "/api/confirm"; submit()
        assertEquals(RequestPhase.REJECTED, repo.state.value.request!!.phase)
    }

    @Test fun disconnectClearsPendingAndReconnectDoesNotReplay() = runBlocking {
        submit(); fail = "/api/state"; repo.poll()
        assertEquals(RequestPhase.OUTCOME_UNKNOWN, repo.state.value.request!!.phase)
        assertFalse(repo.state.value.connected)
        fail = null; sample = report(uptime = 105); repo.poll(); repo.confirm()
        assertEquals(1, calls.count { it.first == "/api/confirm" })
    }

    @Test fun ackTimeoutAndGatewayRebootAreUnknown() = runBlocking {
        submit(); now += 5000; repo.age()
        assertEquals(RequestPhase.OUTCOME_UNKNOWN, repo.state.value.request!!.phase)
        sample = report(uptime = 105); submit(); transaction.put("phase", 0).put("id", 0); repo.poll()
        assertEquals(RequestPhase.OUTCOME_UNKNOWN, repo.state.value.request!!.phase)
    }

    @Test fun resetWhileConfirmInFlightCannotResurrectPending() = runBlocking {
        val response = CompletableDeferred<Unit>()
        val waiting = CompletableDeferred<Unit>()
        val delayed = HardwareRepository(BoardApi { path, body ->
            if (path == "/api/confirm") { waiting.complete(Unit); response.await() }
            api.request(path, body)
        }) { now }
        delayed.poll(); delayed.prepare(true)
        val job = launch { delayed.confirm() }; waiting.await(); delayed.reset(); response.complete(Unit); job.join()
        assertNull(delayed.state.value.request)
        delayed.poll(); delayed.confirm()
        assertEquals(1, calls.count { it.first == "/api/confirm" })
    }

    @Test fun repeatedTapsCannotSendAnotherProposalOrConfirmation() = runBlocking {
        submit(); repeat(4) { repo.prepare(true); repo.confirm() }
        assertEquals(1, calls.count { it.first == "/api/proposal" })
        assertEquals(1, calls.count { it.first == "/api/confirm" })
    }

    @Test fun sourceAgeIncludesTimeSpentInFlight() = runBlocking {
        val slow = HardwareRepository(BoardApi { path, body ->
            val result = api.request(path, body)
            now += 2000
            result
        }) { now }
        sample.put("age_ms", 1000)
        slow.poll()
        assertFalse(slow.state.value.connected)
        slow.prepare(true)
        assertFalse(calls.any { it.first == "/api/proposal" })
    }

    @Test fun lateAckAfterTimeoutCannotTurnUnknownIntoSuccess() = runBlocking {
        submit(); now += 5000; repo.age(); ack()
        sample = report(uptime = 200, on = true, command = 42); repo.poll()
        assertTrue(repo.state.value.sample!!.light)
        assertEquals(RequestPhase.OUTCOME_UNKNOWN, repo.state.value.request!!.phase)
        assertEquals(1, calls.count { it.first == "/api/confirm" })
    }

    @Test fun explicitlySimulatedOrHostMeasuredStateNeverEntersHardware() = runBlocking {
        for ((key, value) in listOf("processing_source" to "HOST_MEASURED", "source" to "SIMULATED", "input_source" to "SIMULATED", "execution_mode" to "HOST_ONLY")) {
            val isolated = HardwareRepository(api) { now }
            sample = report().put(key, value); isolated.poll(); isolated.prepare(true)
            assertFalse(key, isolated.state.value.connected)
        }
        assertFalse(calls.any { it.first == "/api/proposal" })
    }

    @Test fun hostProposalOrAckCannotBeMistakenForHardware() = runBlocking {
        repo.poll(); proposal.put("input_source", "SIMULATED"); repo.prepare(true); repo.confirm()
        assertFalse(calls.any { it.first == "/api/confirm" })
        proposal.remove("input_source"); submit(); ack(); transaction.put("source", "HOST_MEASURED")
        sample = report(uptime = 105, on = true, command = 42); repo.poll()
        assertEquals(RequestPhase.OUTCOME_UNKNOWN, repo.state.value.request!!.phase)
    }

    @Test fun fanUsesImmutableTargetAndRequiresFanAckAndNewFanReport() = runBlocking {
        repo.poll(); proposal.put("device", "fan"); repo.prepare(true, "fan"); repo.confirm()
        assertTrue(calls.contains("/api/proposal" to "manual physical_bedroom bedroom fan ON 0"))
        assertTrue(calls.contains("/api/confirm" to "7 physical_bedroom bedroom fan ON"))
        assertFalse(repo.state.value.sample!!.fan)
        // Light ACK/state must never complete a fan command.
        ack(); transaction.put("fan", false)
        sample = report(uptime = 105, on = true, command = 42); repo.poll()
        assertEquals(RequestPhase.PENDING, repo.state.value.request!!.phase)
        transaction.put("fan", true); repo.poll()
        assertEquals(RequestPhase.PENDING, repo.state.value.request!!.phase)
        sample = report(uptime = 110, on = true, command = 42).put("fan", true); repo.poll()
        assertEquals(RequestPhase.REPORTED, repo.state.value.request!!.phase)
        assertEquals("fan", repo.state.value.request!!.device)
        assertFalse(repo.voiceReady()) // A fan result does not replace the manual-light proof.
    }

    @Test fun fanOffNeedsItsOwnReadback() = runBlocking {
        sample.put("fan", true); repo.poll()
        proposal.put("device", "fan").put("action", "OFF")
        repo.prepare(false, "fan"); repo.confirm(); ack(false); transaction.put("fan", false)
        repo.poll(); assertEquals(RequestPhase.PENDING, repo.state.value.request!!.phase)
        sample = report(uptime = 105, command = 42); repo.poll()
        assertEquals(RequestPhase.REPORTED, repo.state.value.request!!.phase)
        assertTrue(calls.contains("/api/confirm" to "7 physical_bedroom bedroom fan OFF"))
    }

    @Test fun lightProposalCannotAuthorizeFanAndInvalidDeviceNeverPosts() = runBlocking {
        repo.poll(); repo.prepare(true, "fan"); repo.confirm()
        assertEquals(RequestPhase.REJECTED, repo.state.value.request!!.phase)
        assertFalse(calls.any { it.first == "/api/confirm" })
        val postsBefore = calls.count { it.second != null }
        try { repo.prepare(true, "heater"); fail("Unsupported target accepted") } catch (_: IllegalArgumentException) { }
        assertEquals(postsBefore, calls.count { it.second != null })
    }

    @Test fun fanRebootOrConnectionLossNeverReplaysOn() = runBlocking {
        repo.poll(); proposal.put("device", "fan"); repo.prepare(true, "fan"); repo.confirm()
        fail = "/api/state"; repo.poll()
        assertEquals(RequestPhase.OUTCOME_UNKNOWN, repo.state.value.request!!.phase)
        fail = null; sample = report(boot = 10, uptime = 1)
        repeat(3) { repo.poll(); repo.confirm() }
        assertEquals(1, calls.count { it.first == "/api/confirm" })
        assertFalse(repo.state.value.sample!!.fan)
    }

    @Test fun realTemperatureRequiresVersionValueStatusAndAge() = runBlocking {
        sample.put("telemetry_schema", 4).put("temperature_status", "OK")
            .put("temperature_deci_c", 253).put("temperature_age_ms", 2500)
        repo.poll(); assertEquals(25.3, repo.state.value.sample!!.temperatureAt(now)!!, 0.001)
        now += 3000; assertNull(repo.state.value.sample!!.temperatureAt(now)) // Board stale independently.
        for ((field, value) in listOf("telemetry_schema" to 3, "temperature_status" to "CHECKSUM",
            "temperature_deci_c" to 999, "temperature_deci_c" to 25.3, "temperature_age_ms" to 10000)) {
            sample = report(uptime = now).put("telemetry_schema", 4).put("temperature_status", "OK")
                .put("temperature_deci_c", 253).put("temperature_age_ms", 2500).put(field, value)
            repo.poll(); assertTrue(repo.state.value.connected)
            assertNull(field, repo.state.value.sample!!.temperatureAt(now))
            now++
        }
    }

    @Test fun missingOrInvalidTemperatureNeverGeneratesValueOrDisconnectsManualControl() = runBlocking {
        repo.poll(); assertNull(repo.state.value.sample!!.temperatureAt(now))
        sample = report(uptime = 105).put("telemetry_schema", 4).put("temperature_status", "TIMEOUT")
            .put("temperature_deci_c", JSONObject.NULL).put("temperature_age_ms", JSONObject.NULL)
        repo.poll(); assertTrue(repo.state.value.connected); assertNull(repo.state.value.sample!!.temperatureAt(now))
        repo.prepare(false); assertEquals(RequestPhase.REJECTED, repo.state.value.request!!.phase) // Fixture ON proposal mismatches OFF.
        assertTrue(calls.any { it.first == "/api/proposal" })
    }

    @Test fun fanAutoNeedsItsOwnModeAckAndFreshReport() = runBlocking {
        sample.put("control_policy_version", 2).put("mode", "HOME").put("manual_flags", 3).put("auto_flags", 1)
        proposal.put("device", "fan").put("action", "AUTO")
        repo.poll(); repo.prepareAuto("fan"); repo.confirm()
        assertTrue(calls.contains("/api/proposal" to "manual physical_bedroom bedroom fan AUTO 0"))
        assertTrue(calls.contains("/api/confirm" to "7 physical_bedroom bedroom fan AUTO"))
        assertEquals(RequestPhase.PENDING, repo.state.value.request!!.phase)
        ack(); transaction.put("fan", true)
        sample = report(uptime = 105, command = 42).put("control_policy_version", 2).put("mode", "HOME").put("auto_flags", 1).put("manual_flags", 2)
        repo.poll(); assertEquals(RequestPhase.PENDING, repo.state.value.request!!.phase) // Light AUTO is insufficient.
        sample = report(uptime = 110, command = 42).put("control_policy_version", 2).put("mode", "HOME").put("auto_flags", 3).put("manual_flags", 1)
        repo.poll(); assertEquals(RequestPhase.REPORTED, repo.state.value.request!!.phase)
        assertFalse(repo.state.value.sample!!.fan) // Mode confirmation does not invent output state.
        assertFalse(repo.voiceReady())
    }

    @Test fun legacyOrAwayControllerCannotPrepareAuto() = runBlocking {
        repo.poll(); repo.prepareAuto("light")
        sample = report(uptime = 105).put("control_policy_version", 2).put("mode", "AWAY"); repo.poll(); repo.prepareAuto("fan")
        assertFalse(calls.any { it.first == "/api/proposal" })
        assertTrue(repo.state.value.message.contains("AWAY"))
    }

    @Test fun autoContextChangeBeforeConfirmationNeverDispatches() = runBlocking {
        sample.put("control_policy_version", 2).put("mode", "HOME"); proposal.put("action", "AUTO")
        repo.poll(); repo.prepareAuto("light")
        sample = report(uptime = 105).put("control_policy_version", 2).put("mode", "AWAY"); repo.poll(); repo.confirm()
        assertEquals(RequestPhase.REJECTED, repo.state.value.request!!.phase)
        assertFalse(calls.any { it.first == "/api/confirm" })
    }

    @Test fun voiceOutputGateRequiresInhibitionOrExactLedPolicyProfile() = runBlocking {
        for ((version, profile, expected) in listOf(Triple(1, "LED_INDICATORS_10K_ACTIVE_LOW", false),
            Triple(2, "GENERIC_ENABLED", false), Triple(2, "UNKNOWN", false),
            Triple(2, "LED_INDICATORS_10K_ACTIVE_LOW", true), Triple(2, "LED_INDICATORS_10K_ACTIVE_HIGH", true))) {
            sample = report(uptime = now).put("physical_outputs_enabled", true).put("control_policy_version", version).put("output_profile", profile)
            repo.poll(); assertEquals(expected, repo.state.value.sample!!.voiceOutputModeAllowed()); now++
        }
    }

    @Test fun hardwareTemperatureTileUsesFreshMeasuredRoomRatherThanUnavailablePlaceholder() = runBlocking {
        sample.put("telemetry_schema", 4).put("temperature_status", "OK").put("temperature_deci_c", 289).put("temperature_age_ms", 500)
        repo.poll(); val ui = repo.state.value.temperaturePresentation(now)
        assertTrue(ui.available); assertEquals("28.9 °C", ui.valueText); assertTrue(ui.detail.contains("physical sensor"))
        now += 3000; assertFalse(repo.state.value.temperaturePresentation(now).available)
        assertEquals("Unavailable", repo.state.value.temperaturePresentation(now).valueText)
        sample = report(uptime = 105).put("temperature_status", "TIMEOUT"); repo.poll()
        assertFalse(repo.state.value.temperaturePresentation(now).available)
    }

    @Test fun connectionFailureCategoryDoesNotLogPayloadAndClearsOnlyOnValidReport() = runBlocking {
        repo.poll(); fail = "/api/state"; repo.poll()
        assertEquals("LOCAL_CONNECTION_IO", repo.state.value.connectionIssue)
        assertFalse(repo.state.value.connected)
        fail = null; sample = report(uptime = 105); repo.poll()
        assertTrue(repo.state.value.connected); assertNull(repo.state.value.connectionIssue)
        reject = "/api/state"; repo.poll(); assertEquals("HTTP_409", repo.state.value.connectionIssue)
        reject = null; sample = report(uptime = 110).put("known", false); repo.poll()
        assertEquals("INVALID_OR_STALE_REPORT", repo.state.value.connectionIssue)
    }
    @Test fun removedPirIsUnavailableAndCannotDispatchAuto() = runBlocking {
        sample.put("control_policy_version", 2).put("mode", "HOME").put("pir_available", false)
            .put("automation_available", false).put("occupancy", "UNAVAILABLE").put("motion", JSONObject.NULL)
        repo.poll(); calls.clear(); repo.prepareAuto("light")
        assertEquals(false, repo.state.value.sample!!.pirAvailable)
        assertEquals(false, repo.state.value.sample!!.automationAvailable)
        assertNull(repo.state.value.sample!!.motion)
        assertEquals("UNAVAILABLE", repo.state.value.sample!!.occupancy)
        assertTrue(calls.isEmpty()); assertTrue(repo.state.value.message.contains("AUTO unavailable"))
    }
    @Test fun proposalTargetIsImmutableAndDoubleConfirmDispatchesOnce() = runBlocking {
        repo.poll(); repo.prepare(true, "light")
        repo.prepare(false, "fan")
        assertEquals("light", repo.state.value.request!!.device)
        assertTrue(repo.state.value.request!!.on)
        repo.confirm(); repo.confirm()
        assertEquals(1, calls.count { it.first == "/api/confirm" })
        assertTrue(calls.contains("/api/confirm" to "7 physical_bedroom bedroom light ON"))
    }
    @Test fun reconnectClearsDisconnectedMessageWithHistoricalCompletedRequest() = runBlocking {
        submit(); ack(); sample = report(uptime = 105, on = true, command = 42); repo.poll()
        assertEquals(RequestPhase.REPORTED, repo.state.value.request!!.phase)
        fail = "/api/state"; repo.poll()
        assertFalse(repo.state.value.connected); assertTrue(repo.state.value.message.contains("Disconnected"))
        fail = null; sample = report(uptime = 110, on = true, command = 42); repo.poll()
        assertTrue(repo.state.value.connected); assertNull(repo.state.value.connectionIssue)
        assertTrue(repo.state.value.message.startsWith("Fresh board report"))
        assertEquals(RequestPhase.REPORTED, repo.state.value.request!!.phase)
        assertEquals(1, calls.count { it.first == "/api/confirm" })
    }
}
