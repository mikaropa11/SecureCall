package com.example.securecall.ui.components

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.VolumeUp
import androidx.compose.material.icons.filled.CallEnd
import androidx.compose.material.icons.filled.Cameraswitch
import androidx.compose.material.icons.filled.Mic
import androidx.compose.material.icons.filled.MicOff
import androidx.compose.material.icons.filled.Videocam
import androidx.compose.material.icons.filled.VideocamOff
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import com.example.securecall.ui.viewmodel.CallUiState

@Composable
fun CallControls(
    uiState: CallUiState,
    onToggleMic: () -> Unit,
    onToggleCamera: () -> Unit,
    onSwitchCamera: () -> Unit,
    onToggleSpeaker: () -> Unit,
    onEndCall: () -> Unit,
    modifier: Modifier = Modifier
) {

    Row(
        modifier = modifier
            .fillMaxWidth()
            .padding(24.dp),
        horizontalArrangement = Arrangement.SpaceEvenly
    ) {

        IconButton(onClick = onToggleMic) {
            Icon(
                imageVector = if (uiState.isMicEnabled)
                    Icons.Default.Mic else Icons.Default.MicOff,
                contentDescription = "Mic"
            )
        }

        IconButton(onClick = onToggleCamera) {
            Icon(
                imageVector = if (uiState.isCameraEnabled)
                    Icons.Default.Videocam else Icons.Default.VideocamOff,
                contentDescription = "Camera"
            )
        }

        IconButton(onClick = onSwitchCamera) {
            Icon(Icons.Default.Cameraswitch, "Switch")
        }

        IconButton(onClick = onToggleSpeaker) {
            Icon(Icons.AutoMirrored.Filled.VolumeUp, "Speaker")
        }

        IconButton(onClick = onEndCall) {
            Icon(
                Icons.Default.CallEnd,
                contentDescription = "End",
                tint = Color.Red
            )
        }
    }
}