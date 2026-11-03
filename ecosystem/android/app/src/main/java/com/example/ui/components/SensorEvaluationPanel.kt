package com.example.ui.components

import androidx.compose.foundation.layout.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.example.data.remote.HardwareState

@Composable
fun SensorEvaluationPanel(state: HardwareState, busy: Boolean, configure: (Boolean,Long)->Unit) {
    val sample=state.sample
    val now=android.os.SystemClock.elapsedRealtime()
    val fresh=state.connected && sample?.fresh(now)==true
    val evidence=sample?.sensorEvaluation
    val supported=fresh && sample?.featuresVersion==3 && evidence!=null
    val enabled=supported && sample!!.voiceOutputModeAllowed() && state.request?.active!=true && !busy
    var draft by remember(sample?.boot) { mutableStateOf(false) }
    var review by remember { mutableStateOf<Triple<Boolean,Long,Long>?>(null) }
    var error by remember { mutableStateOf("") }
    LaunchedEffect(sample?.boot,evidence?.order) { if(supported) draft=evidence!!.order==1 }
    fun age(value: Long?)=value?.let { "${it+(now-(sample?.received?:now)).coerceAtLeast(0)} ms" }?:"unavailable"
    val lightFresh=supported && sample!!.lightInputAvailable && evidence!!.lightAge!=null &&
        evidence.lightAge+(now-sample.received).coerceAtLeast(0)<1000
    Card(Modifier.fillMaxWidth()) {
        Column(Modifier.padding(14.dp),verticalArrangement=Arrangement.spacedBy(8.dp)) {
            Text("Room 1 sensor inputs & evaluation priority",style=MaterialTheme.typography.titleMedium)
            Text("DHT11: "+(if(fresh) sample?.temperatureAt(now)?.let { "%.1f °C".format(java.util.Locale.US,it) }?:"Unavailable (${sample?.temperatureStatus})" else "Unavailable — board stale/disconnected")+
                "; valid reading age: "+(if(fresh) age(sample?.temperatureAge) else "unavailable")+
                "; latest acquisition attempt age: "+(if(supported) age(evidence!!.temperatureAttemptAge) else "unavailable"))
            Text("LM393: "+(if(lightFresh) if(sample!!.lightInputRaw==true) "dark (HIGH) — available" else "bright (LOW) — available" else "Unavailable / stale")+
                "; filtered sample age: "+(if(supported) age(evidence!!.lightAge) else "unavailable"))
            Text("One controller snapshot of the latest inputs, with independent sample ages. Physical acquisition is not simultaneous. Invalid inputs stay unavailable; no last-good sample is substituted.")
            Text("Accepted evaluation order: "+if(supported) if(evidence!!.order==0) "Light sensor first" else "Temperature sensor first" else "Unavailable — updated Room 1 controller and gateway required")
            Text("This selects which sensor rule is evaluated first within the same bounded controller cycle. Both receive service. Per-device schedule-versus-sensor source priority is a separate setting below.")
            Row {
                TextButton(onClick={draft=false}) { Text(if(!draft) "Selected: Light sensor first" else "Light sensor first") }
                TextButton(onClick={draft=true}) { Text(if(draft) "Selected: Temperature sensor first" else "Temperature sensor first") }
            }
            Button(enabled=enabled,onClick={review=Triple(draft,sample!!.boot,now)}) { Text("Review sensor evaluation order") }
            Text("${state.message}",style=MaterialTheme.typography.bodySmall)
            if(supported) {
                Text("Controller evidence: cycle ${evidence!!.cycle}, input snapshot uptime ${evidence.snapshotMs} ms.")
                val rules=listOf("Light" to evidence.light,"Temperature / fan" to evidence.fan).sortedBy{it.second.step}
                val labels=listOf("not evaluated","request accepted / satisfied","blocked: MANUAL hold","blocked: safety / OFF protection hold","blocked: SafetyKernel dwell","blocked: AWAY","invalid input: fault OFF accepted","deferred: schedule source has control")
                for((label,rule) in rules) Text("Step ${rule.step} — $label requests ${if(rule.requestedOn) "ON" else "OFF"}: ${labels[rule.result]}. "+
                    if(evidence.clockValid) "Actual controller start ${rule.startUs} µs, elapsed ${rule.elapsedUs} µs." else "Controller timing unavailable.")
                Text("Bounded latest-cycle diagnostic snapshot, not a record of every cycle. Ordering evidence comes from controller sequence/timing, not LED appearance.")
            }
            if(error.isNotEmpty()) Text(error,color=MaterialTheme.colorScheme.error)
        }
    }
    review?.let { pending -> AlertDialog(onDismissRequest={review=null},
        title={Text("Authorize Room 1 sensor evaluation order")},
        text={Text("${if(pending.first) "Temperature sensor first" else "Light sensor first"}. Both rules still run each cycle. This does not enable AUTO, release manual holds, reset timers, change source priority or restore ON outputs.")},
        confirmButton={TextButton(enabled=enabled,onClick={
            if(sample?.boot!=pending.second || android.os.SystemClock.elapsedRealtime()-pending.third>=30000) error="Review expired or controller changed; review again."
            else configure(pending.first,pending.second)
            review=null
        }){Text("Authorize")}},dismissButton={TextButton(onClick={review=null}){Text("Cancel")}}) }
}
