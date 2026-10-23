package com.example

import android.content.Context
import androidx.test.core.app.ApplicationProvider
import com.example.data.model.ActuatorAction
import com.example.data.model.OccupancyState
import com.example.data.model.PolicyConfig
import com.example.data.model.RoomState
import com.example.domain.SafetyKernelEngine
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [36])
class ExampleRobolectricTest {

    @Test
    fun `read string from context`() {
        val context = ApplicationProvider.getApplicationContext<Context>()
        val appName = context.getString(R.string.app_name)
        assertEquals("ClawGrid", appName)
    }

    @Test
    fun `safety kernel enforces minimum dwell lock and manual hold`() {
        val policy = PolicyConfig(dwellSeconds = 3.0, manualHoldMinutes = 5.0)
        val initial = RoomState(
            id = "bedroom",
            name = "Bedroom",
            zoneLabel = "PIR_1 (Zone 1)",
            sensorId = "PIR_1",
            state = OccupancyState.OCCUPIED,
            tempC = 25.0,
            lux = 120
        )

        val t0 = 100_000L
        val onOutcome = SafetyKernelEngine.evaluateActuation(initial, "light", ActuatorAction.ON, policy, t0)
        assertTrue(onOutcome.accepted)
        assertTrue(onOutcome.updatedRoom.lightOn)

        // Rapid toggle 1 second later should be rejected by 3.0s dwell lock
        val rapidOff = SafetyKernelEngine.evaluateActuation(onOutcome.updatedRoom, "light", ActuatorAction.OFF, policy, t0 + 1000L)
        assertFalse(rapidOff.accepted)
        assertTrue(rapidOff.updatedRoom.lightOn)

        // Toggle after 3.5 seconds succeeds
        val delayedOff = SafetyKernelEngine.evaluateActuation(onOutcome.updatedRoom, "light", ActuatorAction.OFF, policy, t0 + 3500L)
        assertTrue(delayedOff.accepted)
        assertFalse(delayedOff.updatedRoom.lightOn)
    }

    @Test
    fun `offline voice parser extracts room device and action`() {
        val parsed = SafetyKernelEngine.parseVoiceCommand("hey claw, turn on bedroom fan")
        assertEquals("ACTUATE", parsed.intent)
        assertEquals("bedroom", parsed.roomId)
        assertEquals("fan", parsed.device)
        assertEquals(ActuatorAction.ON, parsed.action)
    }
}
