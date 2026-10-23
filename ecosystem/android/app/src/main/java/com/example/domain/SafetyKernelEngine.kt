package com.example.domain

import com.example.data.model.ActuatorAction
import com.example.data.model.OccupancyState
import com.example.data.model.PolicyConfig
import com.example.data.model.RoomState
import kotlin.math.max
import kotlin.math.min

/**
 * Deterministic SafetyKernel & Offline Voice Intent Parser ported from ClawGrid V2.0 firmware/simulator.
 * Enforces:
 * 1. Minimum Dwell Time (default 3.0s anti-cycling hysteresis lock)
 * 2. Manual Override Hold Duration (default 5.0m = 300s countdown timer)
 * 3. Occupancy Grace Window (OCCUPIED -> LIKELY_OCCUPIED -> VACANT, or UNKNOWN if sensors disabled)
 * 4. Offline Grammar-Slot Voice Intent Parser with Levenshtein fuzzy matching
 */
object SafetyKernelEngine {

    data class ActuationOutcome(
        val accepted: Boolean,
        val updatedRoom: RoomState,
        val terminalMessage: String,
        val toastTitle: String,
        val toastMessage: String,
        val severity: String // "INFO", "SUCCESS", "WARN", "DANGER"
    )

    fun evaluateActuation(
        room: RoomState,
        device: String, // "light" or "fan"
        action: ActuatorAction,
        policy: PolicyConfig,
        nowMs: Long
    ): ActuationOutcome {
        val isLight = device == "light"
        val devLabel = if (isLight) "Ceiling Light" else "Ceiling Fan"
        val lastTransition = if (isLight) room.lastLightTransitionMs else room.lastFanTransitionMs
        val currentOn = if (isLight) room.lightOn else room.fanOn
        val minDwellMs = (policy.dwellSeconds * 1000.0).toLong()
        val holdMs = (policy.manualHoldMinutes * 60.0 * 1000.0).toLong()

        return when (action) {
            ActuatorAction.AUTO -> {
                val updated = if (isLight) {
                    room.copy(autoLight = true, manualLightHoldUntilMs = 0L)
                } else {
                    room.copy(autoFan = true, manualFanHoldUntilMs = 0L)
                }
                val reEvaluated = evaluateRoomPolicy(updated, isAvatarHere = updated.state == OccupancyState.OCCUPIED, policy = policy, nowMs = nowMs)
                ActuationOutcome(
                    accepted = true,
                    updatedRoom = reEvaluated,
                    terminalMessage = "[SAFETYKERNEL] ${room.name} $devLabel -> AUTO policy resumed (Lux < ${policy.luxThreshold} / Temp ≥ ${policy.tempThreshold}°C).",
                    toastTitle = "${room.name} ${device.uppercase()} → AUTO",
                    toastMessage = "Sensor-driven policy control restored.",
                    severity = "SUCCESS"
                )
            }

            ActuatorAction.ON, ActuatorAction.OFF -> {
                val targetOn = action == ActuatorAction.ON
                val elapsedMs = nowMs - lastTransition
                // Check Minimum Dwell Time anti-cycling rule when state is flipping
                if (lastTransition > 0L && targetOn != currentOn && elapsedMs < minDwellMs) {
                    val remSec = (minDwellMs - elapsedMs) / 1000.0
                    return ActuationOutcome(
                        accepted = false,
                        updatedRoom = room,
                        terminalMessage = "[SAFETYKERNEL] REJECTED: ${room.name} $devLabel anti-cycling dwell lock active (${"%.1f".format(remSec)}s remaining of ${policy.dwellSeconds}s).",
                        toastTitle = "SafetyKernel Dwell Lock (${"%.1f".format(remSec)}s)",
                        toastMessage = "Anti-cycling guard blocked rapid relay toggle on ${room.name} $devLabel.",
                        severity = "WARN"
                    )
                }

                val holdUntil = nowMs + holdMs
                val holdSec = holdMs / 1000L
                val updated = if (isLight) {
                    room.copy(
                        lightOn = targetOn,
                        autoLight = false,
                        manualLightHoldUntilMs = holdUntil,
                        lastLightTransitionMs = if (targetOn != currentOn) nowMs else room.lastLightTransitionMs
                    )
                } else {
                    room.copy(
                        fanOn = targetOn,
                        autoFan = false,
                        manualFanHoldUntilMs = holdUntil,
                        lastFanTransitionMs = if (targetOn != currentOn) nowMs else room.lastFanTransitionMs
                    )
                }

                ActuationOutcome(
                    accepted = true,
                    updatedRoom = updated,
                    terminalMessage = "[SAFETYKERNEL] ${room.name} $devLabel -> Manual ${action.name} (Hold locked for ${holdSec}s).",
                    toastTitle = "${room.name} ${device.uppercase()} → ${action.name}",
                    toastMessage = "Manual ${action.name} hold active for ${policy.manualHoldMinutes.toInt()} min (${holdSec}s).",
                    severity = "INFO"
                )
            }
        }
    }

    fun evaluateRoomPolicy(
        room: RoomState,
        isAvatarHere: Boolean,
        policy: PolicyConfig,
        nowMs: Long
    ): RoomState {
        val graceMs = (policy.graceSeconds * 1000.0).toLong()

        // 1. Occupancy State Machine
        val newState: OccupancyState
        val newDwellStart: Long

        if (!room.pirEnabled && !room.presenceEnabled) {
            // Disabling both sensors forces UNKNOWN (never fabricates VACANT)
            newState = OccupancyState.UNKNOWN
            newDwellStart = 0L
        } else if (isAvatarHere && (room.pirEnabled || room.presenceEnabled)) {
            newState = OccupancyState.OCCUPIED
            newDwellStart = 0L
        } else {
            when (room.state) {
                OccupancyState.OCCUPIED -> {
                    newState = OccupancyState.LIKELY_OCCUPIED
                    newDwellStart = nowMs
                }
                OccupancyState.LIKELY_OCCUPIED -> {
                    val start = if (room.dwellStartMs > 0L) room.dwellStartMs else nowMs
                    if (nowMs - start >= graceMs) {
                        newState = OccupancyState.VACANT
                        newDwellStart = 0L
                    } else {
                        newState = OccupancyState.LIKELY_OCCUPIED
                        newDwellStart = start
                    }
                }
                OccupancyState.UNKNOWN -> {
                    newState = OccupancyState.VACANT
                    newDwellStart = 0L
                }
                OccupancyState.VACANT -> {
                    newState = OccupancyState.VACANT
                    newDwellStart = 0L
                }
            }
        }

        // 2. Check Manual Override Hold Expiration
        var autoLight = room.autoLight
        var lightHold = room.manualLightHoldUntilMs
        if (lightHold > 0L && nowMs >= lightHold) {
            lightHold = 0L
            autoLight = true
        }

        var autoFan = room.autoFan
        var fanHold = room.manualFanHoldUntilMs
        if (fanHold > 0L && nowMs >= fanHold) {
            fanHold = 0L
            autoFan = true
        }

        // 3. Apply Automatic Sensor-Driven Policies when not in Manual Hold
        var lightOn = room.lightOn
        if (lightHold == 0L && autoLight) {
            lightOn = when (newState) {
                OccupancyState.OCCUPIED, OccupancyState.LIKELY_OCCUPIED -> room.lux < policy.luxThreshold
                OccupancyState.VACANT -> false
                OccupancyState.UNKNOWN -> room.lightOn // fail-safe hold current state when sensors offline
            }
        }

        var fanOn = room.fanOn
        if (fanHold == 0L && autoFan) {
            fanOn = when (newState) {
                OccupancyState.OCCUPIED, OccupancyState.LIKELY_OCCUPIED -> room.tempC >= policy.tempThreshold
                OccupancyState.VACANT -> false
                OccupancyState.UNKNOWN -> room.fanOn
            }
        }

        return room.copy(
            state = newState,
            dwellStartMs = newDwellStart,
            lightOn = lightOn,
            fanOn = fanOn,
            autoLight = autoLight,
            autoFan = autoFan,
            manualLightHoldUntilMs = lightHold,
            manualFanHoldUntilMs = fanHold
        )
    }

    data class VoiceParseResult(
        val rawText: String,
        val intent: String, // "SCENE", "ACTUATE", "HELP", "STATUS", "CLARIFY"
        val sceneName: String? = null,
        val roomId: String? = null,
        val roomLabel: String? = null,
        val device: String? = null,
        val action: ActuatorAction? = null,
        val confidence: Double = 0.0,
        val spokenResponse: String
    )

    private val wakeWords = listOf("hey claw", "hi claw", "claw", "computer", "hey grid")

    fun parseVoiceCommand(rawSpeech: String): VoiceParseResult {
        var text = rawSpeech.trim().lowercase()
        for (ww in wakeWords) {
            if (text.startsWith(ww)) {
                text = text.removePrefix(ww).trim().trimStart(',', ' ')
                break
            }
        }

        if (text.isBlank()) {
            return VoiceParseResult(
                rawText = rawSpeech,
                intent = "CLARIFY",
                confidence = 0.0,
                spokenResponse = "Please enter or tap a voice command such as 'turn on bedroom light'."
            )
        }

        // Scene shortcuts
        if (text.contains("all off") || text.contains("emergency off") || text.contains("shut down")) {
            return VoiceParseResult(
                rawText = rawSpeech,
                intent = "SCENE",
                sceneName = "ALL_OFF",
                confidence = 0.99,
                spokenResponse = "Emergency All Off executed: all 8 relays commanded to logical OFF."
            )
        }
        if (text.contains("sleep")) {
            return VoiceParseResult(
                rawText = rawSpeech,
                intent = "SCENE",
                sceneName = "SLEEP",
                confidence = 0.98,
                spokenResponse = "Sleep scene applied: house lights OFF, Bedroom fan primed in AUTO."
            )
        }
        if (text.contains("away") || text.contains("leave house")) {
            return VoiceParseResult(
                rawText = rawSpeech,
                intent = "SCENE",
                sceneName = "AWAY",
                confidence = 0.98,
                spokenResponse = "Away scene applied: all loads OFF and auto policies paused."
            )
        }
        if (text.contains("home mode") || text.contains("resume auto") || text.contains("resume all auto")) {
            return VoiceParseResult(
                rawText = rawSpeech,
                intent = "SCENE",
                sceneName = "HOME",
                confidence = 0.98,
                spokenResponse = "Home scene applied: all 4 rooms restored to automatic occupancy policy."
            )
        }

        if (text.contains("help") || text.contains("what can you do")) {
            return VoiceParseResult(
                rawText = rawSpeech,
                intent = "HELP",
                confidence = 0.99,
                spokenResponse = "Try 'turn on bedroom light', 'turn off kitchen fan', 'set study light auto', or 'sleep mode'."
            )
        }

        val roomCandidates = listOf(
            Triple("bedroom", "Bedroom", listOf("bedroom", "bed room", "bed")),
            Triple("living_room", "Living Room", listOf("living room", "living", "hall", "lounge")),
            Triple("kitchen", "Kitchen", listOf("kitchen", "cooking")),
            Triple("study", "Study", listOf("study", "office", "work room", "desk"))
        )

        var matchedRoom = roomCandidates.firstOrNull { (_, _, aliases) ->
            aliases.any { text.contains(it) }
        }
        if (matchedRoom == null) {
            // Fuzzy token match
            val words = text.split(Regex("\\s+"))
            var bestScore = 0.0
            for (cand in roomCandidates) {
                for (alias in cand.third) {
                    for (w in words) {
                        val score = similarity(w, alias)
                        if (score > bestScore && score >= 0.72) {
                            bestScore = score
                            matchedRoom = cand
                        }
                    }
                }
            }
        }
        val finalRoom = matchedRoom ?: roomCandidates.first()

        val device = when {
            listOf("fan", "cooler", "ventilator", "air").any { text.contains(it) } -> "fan"
            listOf("light", "lights", "lamp", "bulb", "led").any { text.contains(it) } -> "light"
            else -> null
        }

        val action = when {
            text.contains("turn on") || text.contains("switch on") || text.endsWith(" on") -> ActuatorAction.ON
            text.contains("turn off") || text.contains("switch off") || text.endsWith(" off") -> ActuatorAction.OFF
            text.contains("auto") || text.contains("automatic") -> ActuatorAction.AUTO
            else -> null
        }

        if (device == null || action == null) {
            return VoiceParseResult(
                rawText = rawSpeech,
                intent = "CLARIFY",
                confidence = 0.45,
                spokenResponse = "Clarification needed for \"$rawSpeech\". Specify room, device (light/fan), and action (on/off/auto)."
            )
        }

        return VoiceParseResult(
            rawText = rawSpeech,
            intent = "ACTUATE",
            roomId = finalRoom.first,
            roomLabel = finalRoom.second,
            device = device,
            action = action,
            confidence = 0.96,
            spokenResponse = "Intent: ${finalRoom.second} ${device.uppercase()} → ${action.name}"
        )
    }

    private fun similarity(s1: String, s2: String): Double {
        val maxLen = max(s1.length, s2.length)
        if (maxLen == 0) return 1.0
        return 1.0 - (levenshtein(s1, s2).toDouble() / maxLen.toDouble())
    }

    private fun levenshtein(a: String, b: String): Int {
        if (a.isEmpty()) return b.length
        if (b.isEmpty()) return a.length
        val prev = IntArray(b.length + 1) { it }
        val curr = IntArray(b.length + 1)
        for (i in a.indices) {
            curr[0] = i + 1
            for (j in b.indices) {
                val cost = if (a[i] == b[j]) 0 else 1
                curr[j + 1] = min(min(curr[j] + 1, prev[j + 1] + 1), prev[j] + cost)
            }
            System.arraycopy(curr, 0, prev, 0, prev.size)
        }
        return prev[b.length]
    }
}
