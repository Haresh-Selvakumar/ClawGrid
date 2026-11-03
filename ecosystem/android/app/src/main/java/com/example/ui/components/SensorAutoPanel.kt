package com.example.ui.components

import androidx.compose.foundation.layout.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.unit.dp
import com.example.data.remote.HardwareState
import com.example.domain.SensorAutoSettings
import java.util.Locale

private data class AutoReview(val room: String, val device: String, val boot: Long,
    val at: Long, val config: Long? = null, val mode: Boolean? = null)

@Composable
fun SensorAutoPanel(state: HardwareState, voiceBusy: Boolean, room: String, device: String,
    configure: (String,String,Long,Long)->Unit, mode: (String,String,Boolean,Long)->Unit) {
    val sample=state.sample
    val now=android.os.SystemClock.elapsedRealtime()
    val fresh=state.connected && sample?.fresh(now)==true
    val accepted=if(device=="fan") sample?.fanAuto else sample?.lightAuto
    val supported=fresh && sample?.featuresVersion in 2..3 && accepted!=null
    val enabled=supported && sample!!.voiceOutputModeAllowed() && state.request?.active!=true && !voiceBusy
    val label=if(room=="bedroom") "Room 1 / Node A" else "Room 2 / Node C"
    val noSensor=room=="living_room" && device=="light"
    val bit=if(device=="light") 1 else 2
    val actualAuto=supported && (sample!!.autoFlags and bit)!=0 && (sample.manualFlags and bit)==0
    // Drafts are separate from authoritative readback and remain editable.
    var on by remember(room,device) { mutableStateOf("28.0") }
    var off by remember(room,device) { mutableStateOf("26.0") }
    var seconds by remember(room,device) { mutableStateOf("60") }
    var sensorsFirst by remember(room,device) { mutableStateOf(false) }
    var expanded by remember(room,device) { mutableStateOf(device=="fan") }
    var error by remember(room,device) { mutableStateOf("") }
    var review by remember(room,device) { mutableStateOf<AutoReview?>(null) }
    fun loadAccepted() {
        accepted?.settings?.let {
            on=String.format(Locale.US,"%.1f",it.onDeciC/10.0)
            off=String.format(Locale.US,"%.1f",it.offDeciC/10.0)
            seconds=it.overrideSeconds.toString(); sensorsFirst=it.sensorsFirst
        }
    }
    LaunchedEffect(room,device,sample?.boot) { loadAccepted() }
    Column(verticalArrangement=Arrangement.spacedBy(8.dp)) {
        Text("$label / $device: sensor AUTO",style=MaterialTheme.typography.titleMedium)
        Text(if(!supported) "Controller settings unavailable. Updated gateway and room firmware required; draft values are demo defaults."
            else "Accepted mode: ${if(actualAuto) "AUTO" else "MANUAL"} · Source priority: ${if(accepted!!.settings.sensorsFirst) "Sensors first (vs schedules)" else "Schedules first"}")
        if(supported) {
            val current=accepted!!
            if(device=="fan") Text(String.format(Locale.US,"Accepted fan ON ≥ %.1f °C / OFF ≤ %.1f °C",current.settings.onDeciC/10.0,current.settings.offDeciC/10.0))
            Text("Accepted schedule override: ${current.settings.overrideSeconds}s. Sensor demand: "+
                if(noSensor) "Unavailable (no sensor)" else listOf("Unavailable","OFF","ON")[current.demand])
            val remaining=if(current.overrideMs==0L) 0L else (current.overrideMs-sample!!.ageAt(now)).coerceAtLeast(0)
            Text(if(current.overrideMs==0L) "Active override: none" else "Active override: ${if(current.overrideAction==1) "ON" else "OFF"}, ${remaining/1000}s remaining (controller lease)")
            Text("Winning source: "+listOf("Unknown","Manual / voice","Schedule","OFF protection","Safety cutoff","Sensor","Sensor fault OFF","Schedule-only idle")[current.winner])
            Text("Blocking reason: "+listOf("None","Manual hold","Invalid / stale required sensor","No sensor; schedule-only idle","SafetyKernel dwell","Forced OFF; explicit release required","AWAY")[current.blocking])
            if(device=="light") Text(if(noSensor) "No light sensor in Room 2. Schedule-only AUTO; sensor priority unavailable."
                else "LM393: "+when(sample!!.lightInputRaw) { true->"dark (HIGH)"; false->"bright (LOW)"; else->"unavailable" }+"; filtered digital input, not lux.")
        }
        TextButton(onClick={expanded=!expanded}) { Text(if(expanded) "Hide AUTO settings" else "Edit AUTO settings / fan thresholds") }
        if(expanded) {
            if(device=="fan") {
                OutlinedTextField(on,{on=it},label={Text("Fan ON temperature (°C, 0.1–50)")},singleLine=true)
                OutlinedTextField(off,{off=it},label={Text("Fan OFF temperature (°C, 0–49.9; below ON)")},singleLine=true)
                Text("Inside the band, retain previous sensor demand. Entry inside the band starts OFF. Invalid/stale temperature requests OFF; it never uses the last good value.")
            }
            OutlinedTextField(seconds,{seconds=it},label={Text("Schedule override seconds (3–3600)")},singleLine=true)
            Row {
                TextButton(onClick={sensorsFirst=false}) { Text(if(!sensorsFirst) "Selected: Schedules first" else "Schedules first") }
                TextButton(enabled=!noSensor,onClick={sensorsFirst=true}) { Text(if(sensorsFirst) "Selected: Sensors first (vs schedules)" else "Sensors first (vs schedules)") }
            }
            TextButton(enabled=supported,onClick={loadAccepted();error=""}) { Text("Load accepted settings into draft") }
            Button(enabled=enabled,onClick={runCatching {
                val config=SensorAutoSettings.parse(on,off,seconds,sensorsFirst)
                require(!noSensor || !config.sensorsFirst)
                review=AutoReview(room,device,sample!!.boot,now,config=config.packed());error=""
            }.onFailure { error="Use finite temperatures in 0–50 °C, at most one decimal, OFF below ON, and 3–3600 whole seconds." }}) { Text("Review settings") }
            Text("Editing or applying settings does not enable AUTO. Accepted configuration appears only after controller ACK and fresh readback. An existing override is never renewed by polling or settings edits.")
            Row {
                Button(enabled=enabled,onClick={review=AutoReview(room,device,sample!!.boot,now,mode=true)}) { Text("Review enable AUTO") }
                TextButton(enabled=enabled,onClick={review=AutoReview(room,device,sample!!.boot,now,mode=false)}) { Text("Review MANUAL hold") }
            }
            Text("Required sensor fault cancels an active override and requests OFF, subject to dwell. No schedule fallback. Countdown/cap/safety OFF holds until explicit manual action or AUTO release. Reboot starts MANUAL/OFF with no active override.")
            if(error.isNotEmpty()) Text(error,color=MaterialTheme.colorScheme.error)
        }
    }
    review?.let { pending ->
        AlertDialog(onDismissRequest={review=null},title={Text("Authorize $label / $device / "+if(pending.config!=null) "settings" else if(pending.mode==true) "AUTO" else "MANUAL")},
            text={Text(if(pending.config!=null) {
                val c=SensorAutoSettings.unpack(pending.config)
                (if(device=="fan") "Fan ON ${c.onDeciC/10.0} °C, OFF ${c.offDeciC/10.0} °C. " else "")+
                    "${if(c.sensorsFirst) "Sensors first (vs schedules)" else "Schedules first"}; schedule override ${c.overrideSeconds}s. Current mode remains unchanged."
            } else if(pending.mode==true) "Explicitly release manual/forced-OFF hold and enable this device's AUTO using accepted controller settings. This can turn its LED ON immediately after safety checks. Pending schedules and overrides are cleared."
            else "Hold this device's current output in MANUAL and cancel its schedule/override. This does not turn an ON output OFF; use manual OFF for that.")},
            confirmButton={TextButton(enabled=enabled,onClick={
                if(sample?.boot!=pending.boot || room!=pending.room || device!=pending.device || android.os.SystemClock.elapsedRealtime()-pending.at>=30000)
                    error="Review expired or room/controller changed; review again."
                else if(pending.config!=null) configure(pending.room,pending.device,pending.config,pending.boot)
                else mode(pending.room,pending.device,pending.mode==true,pending.boot)
                review=null
            }){Text("Authorize")}},dismissButton={TextButton(onClick={review=null}){Text("Cancel")}})
    }
}
