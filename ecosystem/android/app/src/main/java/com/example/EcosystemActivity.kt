package com.example

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.unit.dp
import com.example.data.remote.EcosystemClient
import com.example.ui.theme.MyApplicationTheme
import org.json.JSONObject

/** Production hardware view. The older four-room local demo is not the launcher. */
class EcosystemActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContent { MyApplicationTheme { LiveEcosystemScreen() } }
    }
}

private fun field(obj: JSONObject?, name: String, suffix: String = ""): String =
    if (obj == null || !obj.has(name) || obj.isNull(name)) "UNKNOWN" else obj.opt(name).toString() + suffix

private fun commanded(obj: JSONObject?, name: String): String =
    if (obj == null || obj.isNull(name) || !obj.has(name)) "UNKNOWN" else if (obj.optBoolean(name)) "ON" else "OFF"

@Composable
private fun LiveCard(title: String, content: @Composable ColumnScope.() -> Unit) {
    Card(Modifier.fillMaxWidth()) {
        Column(Modifier.padding(18.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
            Text(title, style = MaterialTheme.typography.titleLarge)
            content()
        }
    }
}

@Composable
private fun LiveEcosystemScreen() {
    val client = remember { EcosystemClient() }
    val state by client.state.collectAsState()
    var token by remember { mutableStateOf("") }
    var physical by remember { mutableStateOf(false) }
    var error by remember { mutableStateOf("") }
    var tab by remember { mutableIntStateOf(0) }
    DisposableEffect(client) { onDispose { client.disconnect() } }
    val snapshot = state.snapshot
    val a = snapshot?.optJSONObject("node_a")
    val b = snapshot?.optJSONObject("node_b")
    val fresh = EcosystemClient.fresh(snapshot)
    Surface(Modifier.fillMaxSize(), color = MaterialTheme.colorScheme.background) {
        Column(Modifier.fillMaxSize().systemBarsPadding().padding(18.dp).verticalScroll(rememberScrollState()),
            verticalArrangement = Arrangement.spacedBy(16.dp)) {
            Text("ClawGrid · Live ESP ecosystem", style = MaterialTheme.typography.headlineMedium)
            Text("A: PIR/relays → ESP-NOW → USB B → PC → encrypted Supabase → app + webpage")
            LiveCard("Connect") {
                OutlinedTextField(value = token, onValueChange = { token = it }, label = { Text("ClawGrid control token") },
                    visualTransformation = PasswordVisualTransformation(), singleLine = true, modifier = Modifier.fillMaxWidth())
                Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                    Button(onClick = { runCatching { client.connect(token) }.onFailure { error = it.message.orEmpty() } }) { Text("Connect live") }
                    OutlinedButton(onClick = { client.disconnect() }) { Text("Disconnect") }
                }
                Text(state.status)
                if (error.isNotEmpty()) Text(error, color = MaterialTheme.colorScheme.error)
                Text("No service-role key is used by this app. The token is kept in memory.", style = MaterialTheme.typography.bodySmall)
            }
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                listOf("Live", "RTOS", "History").forEachIndexed { index, title ->
                    if (tab == index) Button(onClick = { tab = index }) { Text(title) }
                    else OutlinedButton(onClick = { tab = index }) { Text(title) }
                }
            }
            if (tab == 0) {
                LiveCard("Node A · Bedroom controller") {
                    if (!fresh) Text("UNKNOWN / STALE — waiting for fresh authenticated A telemetry")
                    else {
                        Text("Expected MAC: ${field(a, "expected_mac")}")
                        Text("Boot ${field(a, "boot_id")} · uptime ${field(a, "sample_uptime_ms", " ms")}")
                        Text("PIR occupancy: ${field(a, "occupancy")} · motion ${field(a, "motion")}")
                        Text("Light commanded: ${commanded(a, "light")} · fan commanded: ${commanded(a, "fan")}")
                        Text("GPIO outputs: ${if (a?.optBoolean("physical_outputs_enabled") == true) "ENABLED" else "INHIBITED"}")
                        Text("Radio report age: ${field(a, "age_ms", " ms")} · reports ${field(a, "reports")}")
                        Text("Invalid ${field(a, "invalid")} · replayed ${field(a, "replayed")}")
                    }
                    Text("Relay contacts and real load state are not independently measured.")
                }
                LiveCard("Physical sensor map") {
                    Text("Bedroom: one PIR on A GPIO6. Light GPIO4; fan GPIO5.")
                    Text("Temperature: not installed. Illuminance: not installed.")
                    Text("Living room, kitchen and study: no physical sensors assigned.")
                }
                LiveCard("Command through Supabase") {
                    Row { Checkbox(checked = physical, onCheckedChange = { physical = it }); Text("Physical bedroom policy (unchecked = virtual)") }
                    for (device in listOf("light", "fan")) {
                        Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                            for (on in listOf(true, false)) Button(enabled = fresh && !state.pending, onClick = {
                                runCatching { client.command(device, on, physical) }.onFailure { error = it.message.orEmpty() }
                            }) { Text("${device.replaceFirstChar { it.uppercase() }} ${if (on) "ON" else "OFF"}") }
                        }
                    }
                    Text(state.ack)
                    Text("Commands expire in 10 seconds. Only A's ACK confirms software acceptance. Keep three seconds between transitions.")
                }
            } else if (tab == 1) {
                LiveCard("Node B · Measured diagnostics") {
                    val diagnosticsFresh = b != null && b.optLong("diagnostics_at_ms", 0) > 0 &&
                        System.currentTimeMillis() - b.optLong("diagnostics_at_ms") < 10000
                    val m = if (diagnosticsFresh) b?.optJSONObject("memory") else null
                    val r = if (diagnosticsFresh) b?.optJSONObject("rtos") else null
                    Text("USB: ${field(b, "usb_port")} · expected MAC ${field(b, "expected_mac")}")
                    Text("Internal heap free: ${field(m, "internal_free", " B")}")
                    Text("Minimum heap: ${field(m, "internal_min", " B")}")
                    Text("PSRAM free / total: ${field(m, "psram_free")} / ${field(m, "psram_total")} B")
                    Text("Task ${field(r, "task")} · core ${field(r, "core")} · priority ${field(r, "priority")}")
                    Text("Jobs ${field(r, "jobs")} · period ${field(r, "period_ms", " ms")}")
                    Text("Max execution ${field(r, "max_exec_us", " us")} · max late ${field(r, "max_late_us", " us")}")
                    Text("Deadline misses ${field(r, "deadline_misses")} · stack free ${field(r, "stack_free_bytes", " B")}")
                    Text("Watchdog subscribed: ${field(r, "watchdog")}")
                    Text("Node A heap/task diagnostics are unavailable through the current radio protocol. PC speech is offline Vosk; no on-board speech task is claimed.")
                }
            } else {
                LiveCard("Received real sample history") {
                    Text("${state.history.size} received samples (latest 60). No generated waveforms or seed readings.")
                    state.history.takeLast(20).reversed().forEach { sample ->
                        val row = sample.optJSONObject("node_a")
                        Text("${sample.optLong("sent_at_ms")} UTC ms · age ${field(row, "age_ms", " ms")} · ${field(row, "occupancy")} · light ${commanded(row, "light")}",
                            style = MaterialTheme.typography.bodySmall)
                    }
                }
            }
            Text("Live snapshots target ~1 second; B diagnostics ~5 seconds. Actual latency depends on USB, radio and internet. Stale data loses live status.",
                style = MaterialTheme.typography.bodySmall)
        }
    }
}
