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
import com.example.ui.theme.*

@Composable
fun PhonePushToTalk(uiState: ClawGridUiState, onStart: () -> Unit, onStop: () -> Unit, onCancel: () -> Unit) {
    val context = LocalContext.current
    val lifecycle = LocalLifecycleOwner.current
    var permission by remember { mutableStateOf(ContextCompat.checkSelfPermission(context, Manifest.permission.RECORD_AUDIO) == PackageManager.PERMISSION_GRANTED) }
    val launcher = rememberLauncherForActivityResult(ActivityResultContracts.RequestPermission()) { permission = it }
    DisposableEffect(lifecycle) {
        val observer = LifecycleEventObserver { _, event ->
            if (event == Lifecycle.Event.ON_RESUME) permission = ContextCompat.checkSelfPermission(context, Manifest.permission.RECORD_AUDIO) == PackageManager.PERMISSION_GRANTED
        }
        lifecycle.lifecycle.addObserver(observer)
        onDispose { lifecycle.lifecycle.removeObserver(observer) }
    }
    val enabled by rememberUpdatedState(permission && uiState.voiceReady && !uiState.voiceBusy)
    val recording by rememberUpdatedState(uiState.voiceRecording || uiState.voiceAwaitingRelease)
    Column(verticalArrangement = Arrangement.spacedBy(8.dp), modifier = Modifier.fillMaxWidth()) {
        Text("Phone microphone → ESP32 local recognition", color = ClawTextMain, style = MaterialTheme.typography.titleMedium)
        Text(if (!uiState.voiceReady) "First verify one manual light command with matching board ACK + fresh readback for this boot."
            else "Hold to talk, release to send (maximum 4 seconds). Say: TURN ON BEDROOM LIGHT or TURN OFF BEDROOM LIGHT. Recognition never confirms a command.", color = ClawTextMuted)
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
            Text(if (uiState.voiceAwaitingRelease) "Microphone stopped at cap; release to send" else if (uiState.voiceRecording) "Recording… release to send" else if (uiState.voiceBusy) "Processing local audio…" else "Hold to talk",
                modifier = Modifier.padding(16.dp), color = if (permission && uiState.voiceReady) ClawTextMain else ClawTextDim)
        }
        Text(uiState.voiceMessage, color = ClawAmberLight, style = MaterialTheme.typography.bodySmall)
        if (uiState.voiceBusy) Button(onClick = onCancel) { Text("Cancel voice") }
    }
}
