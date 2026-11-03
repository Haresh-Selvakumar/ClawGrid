package com.example.ui.components

import android.Manifest
import android.content.pm.PackageManager
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.background
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.material3.Button
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.onClick
import androidx.compose.ui.semantics.role
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.unit.dp
import androidx.core.content.ContextCompat
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleEventObserver
import androidx.lifecycle.compose.LocalLifecycleOwner
import com.example.ui.ClawGridUiState
import com.example.BuildConfig
import com.example.ui.theme.*
import com.example.data.remote.PhoneVoicePolicy

@Composable
fun PhonePushToTalk(uiState: ClawGridUiState, onStart: () -> Unit, onStop: () -> Unit, onCancel: () -> Unit, onRoom: (String?) -> Unit, onDismissPreview: () -> Unit) {
    val context = LocalContext.current
    val lifecycle = LocalLifecycleOwner.current
    var permission by remember { mutableStateOf(ContextCompat.checkSelfPermission(context, Manifest.permission.RECORD_AUDIO) == PackageManager.PERMISSION_GRANTED) }
    val launcher = rememberLauncherForActivityResult(ActivityResultContracts.RequestPermission()) { permission = it }
    val cancel by rememberUpdatedState(onCancel)
    DisposableEffect(lifecycle) {
        val observer = LifecycleEventObserver { _, event ->
            if (event == Lifecycle.Event.ON_RESUME) permission = ContextCompat.checkSelfPermission(context, Manifest.permission.RECORD_AUDIO) == PackageManager.PERMISSION_GRANTED
            if (event == Lifecycle.Event.ON_PAUSE || event == Lifecycle.Event.ON_STOP) cancel()
        }
        lifecycle.lifecycle.addObserver(observer)
        onDispose { lifecycle.lifecycle.removeObserver(observer); cancel() }
    }
    val enabled by rememberUpdatedState(permission && uiState.voiceReady && !uiState.voiceBusy)
    val recording by rememberUpdatedState(uiState.voiceRecording || uiState.voiceAwaitingRelease)
    Column(verticalArrangement = Arrangement.spacedBy(8.dp), modifier = Modifier.fillMaxWidth()) {
        Text("Offline on-phone recognition · embedded control", color = ClawTextMain, style = MaterialTheme.typography.titleMedium)
        Text("App ${BuildConfig.VERSION_NAME} (${BuildConfig.VERSION_CODE})", color = ClawTextMuted, style = MaterialTheme.typography.bodySmall)
        Text("Bundled Vosk English model · audio stays on phone · no transcript recording or cloud fallback", color = ClawTextMuted, style = MaterialTheme.typography.bodySmall)
        for((room,label) in listOf("bedroom" to "Room 1 · Node A", "living_room" to "Room 2 · Node C")) {
            Button(onClick={ onRoom(if(uiState.voiceRoom == room) null else room) },
                enabled=uiState.hardwareState.request?.phase !in listOf(com.example.data.remote.RequestPhase.SENDING,com.example.data.remote.RequestPhase.PENDING)) {
                Text(if(uiState.voiceRoom == room) "$label selected" else "Select $label")
            }
        }
        Text(when {
            !uiState.voiceModelReady -> "Offline model preparing or unavailable; manual controls remain available."
            uiState.voiceRoom == null -> "Choose a room explicitly."
            !uiState.voiceReady -> "Waiting for fresh supported controller state for the selected room."
            else -> "Hold, wait for Speak now, say LIGHT ON/OFF or FAN ON/OFF, then release (4 seconds maximum). Review the complete selected room / device / action and explicitly Confirm."
        }, color = ClawTextMuted)
        if (!permission) Button(onClick = { launcher.launch(Manifest.permission.RECORD_AUDIO) }) { Text("Allow microphone for push-to-talk") }
        Surface(modifier = Modifier.fillMaxWidth().semantics {
            role = Role.Button
            onClick(label = "Start or stop push-to-talk") { if (recording) onStop() else if (enabled) onStart(); true }
        }.pointerInput(Unit) {
            detectTapGestures(onPress = {
                if (enabled) {
                    onStart()
                    if (tryAwaitRelease()) onStop() else onCancel()
                }
            })
        }, color = if (uiState.voiceRecording) ClawRed else ClawBgControl) {
            Text(if (uiState.voiceAwaitingRelease) "Microphone stopped at cap; release to recognize" else if (uiState.voiceRecording && !uiState.voiceMicReady) "Starting microphone… keep holding" else if (uiState.voiceRecording) "Speak now… release to recognize" else if (uiState.voiceBusy) "Recognizing on phone…" else "Hold to talk",
                modifier = Modifier.padding(16.dp), color = if (permission && uiState.voiceReady) ClawTextMain else ClawTextDim)
        }
        Text("Canonical commands: LIGHT ON · LIGHT OFF · FAN ON · FAN OFF", color = ClawTextMain)
        Text("Supported complete phrases: " + (PhoneVoicePolicy.allowedPhrases(uiState.voiceRoom) + PhoneVoicePolicy.allowedPhrases(uiState.voiceRoom, "fan")).joinToString("; "), color = ClawTextMuted, style = MaterialTheme.typography.bodySmall)
        Text("Unknown speech, negation and mixed commands are rejected. Review the exact words and target before confirmation.", color = ClawTextMuted, style = MaterialTheme.typography.bodySmall)
        Text(uiState.voiceMessage, color = ClawAmberLight, style = MaterialTheme.typography.bodySmall)
        uiState.voicePreview?.let { preview ->
            Surface(color = ClawBgControl, modifier = Modifier.fillMaxWidth()) {
                Column(modifier = Modifier.padding(12.dp), verticalArrangement = Arrangement.spacedBy(6.dp)) {
                    Text("Memory-only transcript preview", color = ClawTextMain)
                    preview.exactFinalSegments.forEachIndexed { index, text ->
                        Text("Exact final words [${index + 1}]: «$text»", color = ClawTextMain)
                    }
                    if (preview.exactFinalSegments.isEmpty()) Text("Exact final words: unavailable (invalid final output)", color = ClawTextMuted)
                    Text("Normalized parser text: «${preview.normalizedText}»", color = ClawTextMain)
                    Text("Parser result: ${preview.reason}", color = ClawAmberLight)
                    Text("Interpreted target: ${preview.intent?.label ?: "none"}", color = ClawTextMain)
                    Text("Reason: ${preview.detail}", color = ClawTextMuted)
                    Text("Not saved or shared. Cleared on dismiss, new recording, leaving this screen/context, or after 60 seconds.", color = ClawTextMuted, style = MaterialTheme.typography.bodySmall)
                    Button(onClick = onDismissPreview) { Text("Dismiss transcript preview") }
                }
            }
        }
        if (uiState.voiceLastResult.isNotEmpty()) Text("Last recognition outcome: ${uiState.voiceLastResult}", color = ClawTextMuted, style = MaterialTheme.typography.bodySmall)
        if (uiState.voiceCaptureInfo.isNotEmpty()) Text(uiState.voiceCaptureInfo, color = ClawTextMuted, style = MaterialTheme.typography.bodySmall)
        if (uiState.voiceDiagnostics.isNotEmpty()) Text(uiState.voiceDiagnostics, color = ClawTextMuted, style = MaterialTheme.typography.bodySmall)
        if (uiState.voiceBusy) Button(onClick = onCancel) { Text("Cancel voice") }
    }
}
