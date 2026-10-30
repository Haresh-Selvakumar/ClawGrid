package com.example.data.remote

import org.json.JSONObject
import java.util.Locale

data class PhoneVoiceCommand(val on: Boolean, val device: String = "light", val room: String = "bedroom") {
    val action get() = if (on) "ON" else "OFF"
    val label get() = "Bedroom / $device / ${if (on) "ON" else "OFF"}"
}
enum class PhoneVoiceReason { ACCEPTED, SILENCE, UNSUPPORTED_WORDS, LOW_CONFIDENCE, INVALID_RESULT, ROOM_REQUIRED }
data class PhoneVoiceDecision(val reason: PhoneVoiceReason, val command: PhoneVoiceCommand? = null,
    val wordCount: Int = 0, val minimumConfidence: Double? = null,
    val preview: PhoneTranscriptPreview? = null) {
    val message get() = when (reason) {
        PhoneVoiceReason.ACCEPTED -> "Recognized on phone: ${command!!.label}"
        PhoneVoiceReason.SILENCE -> "No speech recognized; nothing proposed"
        PhoneVoiceReason.UNSUPPORTED_WORDS -> "Speech was not one complete allowed command; nothing proposed"
        PhoneVoiceReason.LOW_CONFIDENCE -> "Word confidence below the fixed 0.7 gate; nothing proposed"
        PhoneVoiceReason.INVALID_RESULT -> "Local engine returned an invalid/incomplete result; nothing proposed"
        PhoneVoiceReason.ROOM_REQUIRED -> "Select Bedroom explicitly before recording"
    }
    // Metadata only. Never include raw or normalized transcript words in retained UI diagnostics.
    val metadata get() = "$reason · $wordCount words" + (minimumConfidence?.let { " · minimum word confidence %.3f".format(Locale.ROOT, it) } ?: "")
}

/** Foreground memory only. Redacted toString prevents accidental state/exception logging. */
class PhoneTranscriptPreview(val exactFinalSegments: List<String>, val normalizedText: String,
    val reason: PhoneVoiceReason, val detail: String, val intent: PhoneVoiceCommand? = null) {
    override fun toString() = "PhoneTranscriptPreview(REDACTED; memory only)"
}

/** Full utterances only. No fuzzy matching, forced grammar, partial results or inferred room. */
object PhoneVoicePolicy {
    const val MIN_WORD_CONFIDENCE = 0.7
    val lightPhrases = buildMap {
        for ((action, on) in listOf("on" to true, "off" to false)) {
            put("light $action", on)
            put("turn $action the light", on)
            put("turn the light $action", on)
            put("switch $action the light", on)
            put("switch the light $action", on)
            put("turn $action bedroom light", on)
        }
    }
    val phrases = lightPhrases + lightPhrases.mapKeys { (phrase, _) -> phrase.replace("light", "fan") }
    fun allowedPhrases(room: String?, device: String = "light"): List<String> =
        if (room == "bedroom" && device in listOf("light", "fan")) phrases.keys.filter { device in it.split(' ') } else emptyList()
    // Explicit rejection paths prevent the small decoder graph from containing
    // only actuator commands. These phrases never become actionable intents.
    val rejectionPhrases = listOf("what time is it", "don't turn the light on", "don't turn the fan on",
        "do not turn on the light", "do not turn on the fan", "light on off", "fan on off",
        "light and fan on", "turn the television on")
    val decoderPhrases get() = phrases.keys.toList() + rejectionPhrases + "[unk]"
    val decoderGrammar get() = org.json.JSONArray(decoderPhrases).toString()
    val requiredVocabulary get() = (phrases.keys + rejectionPhrases).flatMap { it.split(' ') }.toSortedSet()
    fun normalize(text: String) = text.lowercase(Locale.ROOT).replace('\u2019', '\'')
        .replace(Regex("[.,!?;:\"\u201c\u201d]"), " ").trim().replace(Regex("[\\s\u00a0]+"), " ")

    fun parse(finalSegments: List<String>, selectedRoom: String?) = decide(finalSegments, selectedRoom).command
    fun decide(finalSegments: List<String>, selectedRoom: String?): PhoneVoiceDecision {
        val exact = mutableListOf<String>()
        var normalized = ""
        var detail = "Final result could not be parsed as JSON."
        fun outcome(reason: PhoneVoiceReason, command: PhoneVoiceCommand? = null,
            count: Int = 0, minimum: Double? = null, why: String): PhoneVoiceDecision =
            PhoneVoiceDecision(reason, command, count, minimum,
                PhoneTranscriptPreview(exact.toList(), normalized, reason, why, command))
        if (selectedRoom != "bedroom") return outcome(PhoneVoiceReason.ROOM_REQUIRED, why = "No supported room/device selected; no command is eligible.")
        if (finalSegments.size !in 1..16) return outcome(PhoneVoiceReason.INVALID_RESULT, why = "Invalid number of final segments.")
        return try {
            val words = mutableListOf<String>()
            val texts = mutableListOf<String>()
            val confidences = mutableListOf<Double>()
            val parsed = finalSegments.map { raw ->
                detail = "Final JSON exceeds its size bound or is malformed."
                require(raw.length <= 8192)
                val json = JSONObject(raw)
                detail = "Engine returned partial/alternative output instead of a complete final text."
                require(!json.has("partial") && !json.has("alternatives"))
                val text = json.get("text") as? String ?: error("Final text type")
                detail = "Final text exceeds the memory-only preview limit."
                require(text.length <= 512)
                exact += text
                json
            }
            normalized = exact.map(::normalize).filter { it.isNotEmpty() }.joinToString(" ")
            for (json in parsed) {
                val text = normalize(json.getString("text"))
                detail = "Final text contains unsupported characters or exceeds the command length bound."
                require(text.length <= 128 && text.all { it in 'a'..'z' || it in " '[]" })
                if (text.isEmpty()) {
                    detail = "Empty final text unexpectedly contains word results."
                    require(!json.has("result") || json.getJSONArray("result").length() == 0)
                    continue
                }
                texts += text
                detail = "Final text has missing or invalid word/confidence entries."
                val results = json.getJSONArray("result")
                require(results.length() in 1..12)
                for (i in 0 until results.length()) {
                    val word = results.getJSONObject(i)
                    val confidence = word.getDouble("conf")
                    require(confidence.isFinite() && confidence in 0.0..1.0)
                    confidences += confidence
                    val token = normalize(word.getString("word"))
                    require(token.isNotEmpty() && ' ' !in token)
                    words += token
                }
            }
            if (words.isEmpty() && texts.isEmpty()) return outcome(PhoneVoiceReason.SILENCE, why = "All final text segments are empty; nothing proposed.")
            detail = "Combined final output exceeds the word count bound."
            require(words.size in 1..12)
            val text = texts.joinToString(" ")
            detail = "Final word entries disagree with the normalized final text; nothing proposed."
            require(words.joinToString(" ") == text)
            val minimum = confidences.min()
            if (words.any { it in setOf("not", "no", "never", "don't", "dont", "can't", "cannot") })
                return outcome(PhoneVoiceReason.UNSUPPORTED_WORDS, count = words.size, minimum = minimum,
                    why = "Negation is not an actuator request; the COMPLETE utterance is rejected.")
            if ("[unk]" in words) return outcome(PhoneVoiceReason.UNSUPPORTED_WORDS, count = words.size, minimum = minimum,
                why = "Decoder marked unknown speech; the COMPLETE utterance is rejected.")
            if (words.filter { it in listOf("light", "fan") }.distinct().size > 1)
                return outcome(PhoneVoiceReason.UNSUPPORTED_WORDS, count = words.size, minimum = minimum,
                    why = "Multiple devices in the COMPLETE utterance; select exactly one device.")
            if ("on" in words && "off" in words) return outcome(PhoneVoiceReason.UNSUPPORTED_WORDS, count = words.size, minimum = minimum,
                why = "Conflicting ON and OFF actions in the COMPLETE utterance.")
            if (words.none { it in listOf("light", "fan") }) return outcome(PhoneVoiceReason.UNSUPPORTED_WORDS, count = words.size, minimum = minimum,
                why = "Missing or unsupported device in the COMPLETE utterance.")
            if (words.none { it in listOf("on", "off") }) return outcome(PhoneVoiceReason.UNSUPPORTED_WORDS, count = words.size, minimum = minimum,
                why = "Missing ON/OFF action in the COMPLETE utterance.")
            val on = phrases[text] ?: return outcome(PhoneVoiceReason.UNSUPPORTED_WORDS, count = words.size, minimum = minimum,
                why = "The COMPLETE normalized final text is not in the Bedroom / light or fan phrase list. No substring, synonym or fuzzy matching is used.")
            if (minimum < MIN_WORD_CONFIDENCE) return outcome(PhoneVoiceReason.LOW_CONFIDENCE, count = words.size, minimum = minimum,
                why = "Phrase matches, but at least one word is below the unchanged 0.7 confidence gate.")
            outcome(PhoneVoiceReason.ACCEPTED, PhoneVoiceCommand(on, if (text in lightPhrases) "light" else "fan", selectedRoom), words.size, minimum,
                "Exact allowed phrase and confidence gate passed. A separate explicit confirmation is required; recognition does not switch anything.")
        } catch (_: Exception) { outcome(PhoneVoiceReason.INVALID_RESULT, why = detail) }
    }
}

/** A recording belongs to one explicit room, controller boot and short-lived context. */
data class PhoneVoiceContext(val room: String, val boot: Long, val capture: Long, val expires: Long, val revision: Long)
class PhoneVoiceSession(private val clock: () -> Long) {
    private var revision = 0L
    fun cancel() { revision++ }
    fun begin(room: String?, hardware: HardwareRepository, capture: Long): PhoneVoiceContext {
        require(room == "bedroom" && capture in 1..0xffffffffL && hardware.voiceReady())
        val sample = hardware.state.value.sample!!
        require(sample.voiceOutputModeAllowed())
        return PhoneVoiceContext(room, sample.boot, capture, clock() + 30000, ++revision)
    }
    fun current(context: PhoneVoiceContext, room: String?, hardware: HardwareRepository): Boolean {
        val state = hardware.state.value
        return context.revision == revision && context.room == room && clock() < context.expires &&
            state.connected && state.sample?.fresh(clock()) == true && state.sample.boot == context.boot &&
            state.sample.voiceOutputModeAllowed() && state.manualVerifiedBoot == context.boot
    }
}
