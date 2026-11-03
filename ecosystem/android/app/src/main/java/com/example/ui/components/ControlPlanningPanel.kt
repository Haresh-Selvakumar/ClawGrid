package com.example.ui.components

import androidx.compose.foundation.layout.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import com.example.data.remote.HardwareState
import com.example.domain.PlanningMath
import com.example.domain.PlanningRule
import java.util.Locale

private data class RuleReview(val rule: PlanningRule, val action: Int, val value: Long,
                              val room: String, val device: String)

@Composable
fun ControlPlanningPanel(state: HardwareState, voiceBusy: Boolean, room: String = "bedroom",
    configure: (String,String,Int,Long,Boolean,Long)->Unit,
    countdown: (String,String,Long,Long)->Unit,
    sensorConfig: (String,String,Long,Long)->Unit,
    deviceMode: (String,String,Boolean,Long)->Unit) {
    val roomLabel=if(room == "bedroom") "Room 1 / Node A" else "Room 2 / Node C"
    val prefs=LocalContext.current.getSharedPreferences("planning_display_$room",0)
    var expanded by remember(room) { mutableStateOf(false) }
    var speechReport by remember(room) { mutableStateOf(false) }
    var device by remember(room) { mutableStateOf("light") }
    var clockText by remember(room) { mutableStateOf("18:00") }
    var turnOn by remember(room) { mutableStateOf(false) }
    var capMinutes by remember(room) { mutableStateOf("1") }
    var countdownMinutes by remember(room) { mutableStateOf("1") }
    var lightWatts by remember(room) { mutableStateOf(prefs.getString("light_watts","").orEmpty()) }
    var fanWatts by remember(room) { mutableStateOf(prefs.getString("fan_watts","").orEmpty()) }
    var error by remember(room) { mutableStateOf("") }
    var confirmation by remember(room) { mutableStateOf<RuleReview?>(null) }
    var reviewBoot by remember(room) { mutableStateOf(0L) }
    var reviewAt by remember(room) { mutableStateOf(0L) }
    var dueAt by remember(room) { mutableStateOf(0L) }
    val sample=state.sample
    val now=android.os.SystemClock.elapsedRealtime()
    val fresh=state.connected && sample?.fresh(now) == true
    val enabled=fresh && sample?.featuresVersion in 1..3 && sample.voiceOutputModeAllowed() && state.request?.active != true && !voiceBusy
    val plan=if(device == "light") sample?.lightPlan else sample?.fanPlan
    val on=if(device == "light") sample?.light else sample?.fan
    val observedRun=if(fresh) sample?.observedRunAt(device,now) else null
    val countdownLeft=if(fresh) sample?.countdownAt(device,now) else null
    val bit=if(device == "light") 1 else 2
    val scheduleReady=fresh && sample!!.mode!="AWAY" && (sample.featuresVersion !in 2..3 ||
        ((sample.autoFlags and bit)!=0 && (sample.manualFlags and bit)==0))
    val sources=listOf("Unknown / awaiting command","Manual or confirmed voice","Schedule","OFF rule (countdown or ON cap)","Safety cutoff","Sensor","Sensor fault OFF","Schedule-only idle")
    val results=listOf("None since boot","Armed","Executed by policy","Blocked by policy / dwell","Cancelled","Overridden by sensor priority")
    fun review(rule: PlanningRule, action: Int, value: Long) {
        confirmation=RuleReview(rule,action,value,room,device)
        reviewBoot=sample!!.boot; reviewAt=android.os.SystemClock.elapsedRealtime(); error=""
    }
    Card(Modifier.fillMaxWidth()) {
        Column(Modifier.padding(14.dp),verticalArrangement=Arrangement.spacedBy(8.dp)) {
            SensorAutoPanel(state,voiceBusy,room,device,sensorConfig,deviceMode)
            Text("$roomLabel: timing & energy",style=MaterialTheme.typography.titleMedium)
            Row { listOf("light","fan").forEach { d -> TextButton(onClick={device=d; confirmation=null}) { Text(if(device==d) "Selected: $d" else d) } } }
            Text("Observed continuous ON: " + when {
                !fresh -> "Unavailable"
                on == false -> "0:00 (OFF)"
                observedRun == null -> "Unknown start; waiting for a fresh OFF/ON observation"
                else -> PlanningMath.duration(observedRun)+" (software observation)"
            })
            Text("Continuous ON cap: " + if(!fresh) "Unavailable" else if((plan?.capMs ?: 0)==0L) "Disabled" else
                PlanningMath.duration(plan!!.capMs)+" from ON" + when {
                    on == false -> " (applies to the next ON)"
                    observedRun == null -> "; remaining time unknown after connection"
                    else -> "; estimated remaining "+PlanningMath.duration((plan.capMs-observedRun).coerceAtLeast(0))
                })
            Text("OFF countdown: " + when {
                countdownLeft == null -> "Unavailable"
                countdownLeft == 0L -> "Not armed"
                plan!!.countdownMs <= sample!!.ageAt(now) -> "Due; awaiting controller readback"
                else -> PlanningMath.duration(countdownLeft)+" remaining"
            })
            Text("Both rules only turn OFF. If both are enabled, the earlier OFF wins. Authorizing a countdown does not reset continuous ON time or its cap.")
            TextButton(onClick={expanded=!expanded}) { Text(if(expanded) "Hide timing controls" else "Open timing controls") }
            if(expanded) {
                Text("OFF countdown from Authorize",style=MaterialTheme.typography.titleSmall)
                OutlinedTextField(countdownMinutes,{countdownMinutes=it},label={Text("OFF countdown minutes (1-240)")},singleLine=true)
                Button(enabled=enabled,onClick={runCatching {
                    review(PlanningRule.OFF_COUNTDOWN,0,PlanningMath.minutesToMillis(countdownMinutes.toLong()))
                }.onFailure{error="Countdown must be 1-240 whole minutes."}}) { Text("Review OFF countdown") }
                TextButton(enabled=enabled,onClick={countdown(room,device,0,sample!!.boot)}) { Text("Cancel OFF countdown") }
                Text("A fresh duration starts when the controller accepts Authorize, even if already ON. It never turns ON. Manual commands do not cancel this OFF deadline; cancel it explicitly. Dwell can briefly delay OFF. Reboot clears it.")
                HorizontalDivider()
                Text("Maximum continuous ON time",style=MaterialTheme.typography.titleSmall)
                OutlinedTextField(capMinutes,{capMinutes=it},label={Text("Continuous ON cap minutes (1-240)")},singleLine=true)
                Button(enabled=enabled,onClick={runCatching {
                    review(PlanningRule.CONTINUOUS_CAP,0,PlanningMath.minutesToMillis(capMinutes.toLong()))
                }.onFailure{error="ON cap must be 1-240 whole minutes."}}) { Text("Review continuous ON cap") }
                TextButton(enabled=enabled,onClick={configure(room,device,0,0,true,sample!!.boot)}) { Text("Disable continuous ON cap") }
                Text("Counts from the latest ON transition. Authorize does not restart it. If already overdue, OFF can be immediate. A later ON starts a new run. The four-hour safety cutoff remains.")
                HorizontalDivider()
                Text("Clock schedule (one-shot HH:mm)",style=MaterialTheme.typography.titleSmall)
                Text(state.message)
                if(!scheduleReady) Text("Before arming: return to HOME and explicitly enable AUTO for this device. A schedule does not release MANUAL holds.")
                val sensorPolicy=if(device=="light") sample?.lightAuto else sample?.fanAuto
                if(sensorPolicy?.settings?.sensorsFirst==true) Text("Sensors first is accepted: a conflicting sensor demand overrides this clock action. Choose Schedules first in this device's AUTO settings if the schedule should take priority.")
                if(sensorPolicy!=null) Text("A successful schedule controls the device for ${sensorPolicy.settings.overrideSeconds}s, then sensor control resumes. An OFF countdown instead holds OFF until explicitly released.")
                Text(if(fresh) "Manual hold flag: ${if((sample!!.manualFlags and bit)!=0) "MANUAL HOLD" else "NO MANUAL HOLD; check rules below"}" else "Manual hold flag: unavailable")
                Text("Last applied output source: ${if(fresh) sources.getOrElse(plan?.source ?: 0){"Unknown"} else "Unavailable"}")
                Text("Last schedule: ${if(fresh) results.getOrElse(plan?.result ?: 0){"Unknown"} else "Unavailable"}")
                Text("Next action: ${if(fresh && (plan?.remainingMs ?: 0)>0) "${if(plan!!.action==1) "ON" else "OFF"} in ${plan.remainingMs/1000}s" else "None / unavailable"}")
                OutlinedTextField(clockText,{clockText=it},label={Text("Next local time HH:mm (one-shot)")},singleLine=true)
                Row {
                    TextButton(onClick={turnOn=!turnOn}) { Text("Action: ${if(turnOn) "ON" else "OFF"}") }
                    Button(enabled=enabled,onClick={runCatching {
                        require(scheduleReady) { "MODE" }
                        require(Regex("[0-9]{2}:[0-9]{2}").matches(clockText))
                        val parts=clockText.split(":").map{it.toInt()}
                        val delay=PlanningMath.delayUntil(parts[0],parts[1],System.currentTimeMillis())
                        dueAt=System.currentTimeMillis()+delay
                        review(PlanningRule.SCHEDULE,if(turnOn) 1 else 0,delay)
                    }.onFailure{error=if(!scheduleReady) "Schedule not armed: enable this device's AUTO in HOME first." else "Enter valid HH:mm, at least one second ahead and within 24 hours."}}) { Text("Review schedule") }
                }
                Text("A passed clock time means its next occurrence tomorrow. On updated controllers, enable AUTO explicitly first; arming does not change MANUAL mode. Successful manual commands cancel its clock schedule. Blocked or over-one-second-late actions are consumed without replay. Reboot clears schedules and caps.")
                TextButton(enabled=enabled,onClick={configure(room,device,2,0,false,sample!!.boot)}) { Text("Cancel device schedule") }
                HorizontalDivider()
                Text("Runtime since boot: ${if(fresh) "light ${sample!!.lightOnMs/1000}s / fan ${sample.fanOnMs/1000}s, boot ${sample.boot}" else "Unavailable"}. Cumulative software ON time; no electrical feedback.")
                OutlinedTextField(lightWatts,{lightWatts=it;prefs.edit().putString("light_watts",it).apply()},label={Text("Assumed light watts (optional)")},singleLine=true)
                OutlinedTextField(fanWatts,{fanWatts=it;prefs.edit().putString("fan_watts",it).apply()},label={Text("Assumed fan watts (optional)")},singleLine=true)
                val lw=lightWatts.toDoubleOrNull(); val fw=fanWatts.toDoubleOrNull()
                val estimate=if(fresh && lw!=null && fw!=null) runCatching { PlanningMath.estimateWh(sample!!.lightOnMs,lw)+PlanningMath.estimateWh(sample.fanOnMs,fw) }.getOrNull() else null
                Text(if(estimate==null) "Estimated appliance energy: unavailable; enter valid assumed watts for both devices." else String.format(Locale.US,"Estimated appliance energy: %.3f Wh since boot; assumed wattage x software runtime.",estimate))
                Text("LED-only demonstration: appliance energy and savings are not measured. No measured savings percentage is available.")
                Text("Sensor AUTO uses the separately reviewed settings above. PIR/occupancy automation remains unavailable. Room 1 light reports bright/dark, never lux.")
            }
            if(error.isNotEmpty()) Text(error,color=MaterialTheme.colorScheme.error)
            TextButton(onClick={speechReport=!speechReport}) { Text(if(speechReport) "Hide recorded speech comparison" else "Recorded speech comparison") }
            if(speechReport) SpeechComparisonReport()
        }
    }
    val pending=confirmation
    if(pending!=null) AlertDialog(onDismissRequest={confirmation=null},
        title={Text("Authorize $roomLabel / ${pending.device} / "+when(pending.rule) {
            PlanningRule.OFF_COUNTDOWN -> "OFF countdown"
            PlanningRule.CONTINUOUS_CAP -> "continuous ON cap"
            PlanningRule.SCHEDULE -> "clock schedule"
        })},
        text={Text(when(pending.rule) {
            PlanningRule.OFF_COUNTDOWN -> "Start a fresh ${pending.value/60000}-minute OFF countdown when this authorization reaches the controller. It does not turn ON or reset the continuous ON cap."+
                if((plan?.capMs ?: 0)>0) " A continuous ON cap is also enabled and may turn OFF earlier." else ""
            PlanningRule.CONTINUOUS_CAP -> "Limit continuous ON time to ${pending.value/60000} minutes from the latest ON. Time already ON counts; this does not restart the clock. OFF may be immediate if already overdue."
            PlanningRule.SCHEDULE -> "Arm ${if(pending.action==1) "ON" else "OFF"} at ${java.text.SimpleDateFormat("EEE dd MMM yyyy HH:mm z",Locale.getDefault()).format(java.util.Date(dueAt))}. Requires AUTO at execution; replaces this device's previous clock schedule. Manual commands cancel it. Source priority and finite override duration still apply."
        })},
        confirmButton={TextButton(enabled=enabled,onClick={
            if(sample?.boot != reviewBoot || pending.room!=room || pending.device!=device || android.os.SystemClock.elapsedRealtime()-reviewAt>=30000) error="Review expired or controller/context changed; review again."
            else runCatching {
                if(pending.rule==PlanningRule.SCHEDULE) require(scheduleReady) { "MODE" }
                val value=PlanningMath.authorizationValue(pending.rule,pending.value,dueAt,System.currentTimeMillis())
                when(pending.rule) {
                    PlanningRule.OFF_COUNTDOWN -> countdown(pending.room,pending.device,value,reviewBoot)
                    PlanningRule.CONTINUOUS_CAP -> configure(pending.room,pending.device,0,value,true,reviewBoot)
                    PlanningRule.SCHEDULE -> configure(pending.room,pending.device,pending.action,value,false,reviewBoot)
                }
            }.onFailure{error=if(pending.rule==PlanningRule.SCHEDULE && !scheduleReady) "Schedule not armed: device left AUTO/HOME; enable AUTO and review again." else "Clock schedule is now past or invalid; review again."}
            confirmation=null
        }){Text("Authorize")}},
        dismissButton={TextButton(onClick={confirmation=null}){Text("Cancel")}})
}

@Composable
private fun SpeechComparisonReport() {
    Text("Offline speech comparison · recorded trials",style=MaterialTheme.typography.titleMedium)
    Text("MN6 / MN7: embedded inference on gateway ESP32-S3. Vosk: offline inference on phone; authenticated deterministic controller still owns GPIO. No cloud recognition is needed.")
    Text("MN6: an older 16-command synthetic corpus produced 0 detections. Two real phone-to-ESP trials on 23 Oct produced no command despite completed processing. Transport success did not prove recognition.")
    Text("MN6 short-command preflight (25 Oct): LIGHT ENABLE reference accepted, score 0.73673; LIGHT DISABLE timed out without an accepted candidate. No subsequent phone collection was performed for this experiment.")
    Text("MN7 preflight (25 Oct): official TELL ME A JOKE reference accepted at 0.75179; LIGHT ENABLE accepted at 0.85891; LIGHT DISABLE timed out. The frozen first-failure rule stopped collection and the baseline was restored. Scores are engine-specific, not accuracy rates or comparable to Vosk word scores.")
    Text("MN6/MN7 mandatory OFF-reference failures blocked adoption of those tested configurations. Neither became the accepted final path; this does not establish universal engine failure. Sources: docs/VOICE_SHORT_V1_PREFLIGHT_20261025.md and docs/MN7_EVALUATION_20261025.md.")
    Text("Vosk: eight recorded positive commands on 28 Oct correlated with applied ACK and fresh controller state: light ON/OFF #6/#7 and #11/#12; fan ON/OFF #9/#10 and #13/#14, boot 61. Visible LED behavior was user-reported.")
    Text("Vosk small English 0.15, Vosk Android 0.3.75, fixed 0.7 per-word gate. Finalized utterances only; strict target/action parsing, negative and ambiguous commands rejected, explicit preview and confirmation retained.")
    Text("Four negative phrases and cancellation were user-reported working. Their capture contains extra ON #15/OFF #16; it does not isolate zero dispatch for every negative. Extra OFF #8 has unspecified provenance.")
    Text("Evidence: 23 Oct phone-trial records; docs/VOICE_LED_DEMONSTRATION_20261028.md; benchmarks/review/voice-led-no-pir-20261028/FINAL_CHECKPOINT.json. Historical records, not rerun here. No common held-out corpus, accuracy percentage, fair latency ranking or measured model-power comparison is established.")
}
