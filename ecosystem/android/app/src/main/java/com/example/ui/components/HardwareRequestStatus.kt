package com.example.ui.components

import android.os.SystemClock
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.example.data.remote.HardwareState
import com.example.ui.theme.ClawAmberLight
import com.example.ui.theme.ClawBgCard
import com.example.ui.theme.ClawBorder
import com.example.ui.theme.ClawTextMain
import com.example.ui.theme.ClawTextMuted

/** Request identity is explicit; a proposal ID and board-reported ID are different evidence. */
@Composable
fun HardwareRequestStatus(state: HardwareState) {
    val request = state.request
    val sample = state.sample?.takeIf { state.connected && it.fresh(SystemClock.elapsedRealtime()) }
    Surface(color = ClawBgCard, shape = RoundedCornerShape(12.dp), border = BorderStroke(1.dp, ClawBorder), modifier = Modifier.fillMaxWidth()) {
        Column(modifier = Modifier.padding(14.dp), verticalArrangement = Arrangement.spacedBy(7.dp)) {
            Text(
                text = if (request != null && request.id > 0) "REQUEST ID #${request.id}" else "REQUEST ID: not assigned",
                style = MaterialTheme.typography.headlineSmall, fontWeight = FontWeight.Bold,
                fontFamily = FontFamily.Monospace, color = ClawTextMain,
                modifier = Modifier.testTag("hardware_request_id")
            )
            Text(
                text = request?.let { "Phase: ${it.phase} · Requested light ${if (it.on) "ON" else "OFF"}" } ?: "No request sent by this app session",
                style = MaterialTheme.typography.titleMedium, color = ClawAmberLight,
                modifier = Modifier.testTag("hardware_request_phase")
            )
            if (request != null && request.proposal > 0 && request.id == 0L) Text(
                "Proposal ID #${request.proposal} · request ID is assigned after confirmation",
                style = MaterialTheme.typography.bodySmall, color = ClawTextMuted
            )
            Text(
                text = sample?.let { "Board-reported last command #${it.lastCommand} · light ${if (it.light) "ON" else "OFF"} · boot #${it.boot}" }
                    ?: "Board-reported state: UNKNOWN",
                style = MaterialTheme.typography.bodyMedium, color = ClawTextMain,
                modifier = Modifier.testTag("hardware_reported_command_id")
            )
            Text(
                text = "Outputs: ${sample?.outputsEnabled?.let { if (it) "ENABLED" else "INHIBITED" } ?: "UNKNOWN"} · physical load not measured",
                style = MaterialTheme.typography.bodyMedium, color = ClawAmberLight
            )
            Text(state.message, style = MaterialTheme.typography.bodySmall, color = ClawTextMuted)
        }
    }
}
