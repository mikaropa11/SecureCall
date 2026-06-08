package com.example.securecall.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.VolumeOff
import androidx.compose.material.icons.automirrored.filled.VolumeUp
import androidx.compose.material.icons.filled.CallEnd
import androidx.compose.material.icons.filled.Cameraswitch
import androidx.compose.material.icons.filled.Mic
import androidx.compose.material.icons.filled.MicOff
import androidx.compose.material.icons.filled.Videocam
import androidx.compose.material.icons.filled.VideocamOff
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.LocalContentColor
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.ui.Alignment
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
    modifier: Modifier = Modifier,
    showVideoControls: Boolean = true
) {
    Row(
        modifier = modifier
            .widthIn(max = 460.dp)
            .background(Color.Black.copy(alpha = 0.52f), RoundedCornerShape(32.dp))
            .padding(horizontal = 16.dp, vertical = 12.dp),
        horizontalArrangement = Arrangement.SpaceEvenly,
        verticalAlignment = Alignment.CenterVertically
    ) {
        ControlButton(
            active = uiState.isMicEnabled,
            onClick = onToggleMic
        ) {
            Icon(
                imageVector = if (uiState.isMicEnabled) Icons.Default.Mic else Icons.Default.MicOff,
                contentDescription = "Microphone"
            )
        }

        if (showVideoControls) {
            ControlButton(
                active = uiState.isCameraEnabled,
                onClick = onToggleCamera
            ) {
                Icon(
                    imageVector = if (uiState.isCameraEnabled) Icons.Default.Videocam else Icons.Default.VideocamOff,
                    contentDescription = "Camera"
                )
            }

            ControlButton(onClick = onSwitchCamera) {
                Icon(Icons.Default.Cameraswitch, contentDescription = "Switch camera")
            }
        }

        ControlButton(
            active = uiState.isSpeakerEnabled,
            onClick = onToggleSpeaker
        ) {
            Icon(
                imageVector = if (uiState.isSpeakerEnabled) {
                    Icons.AutoMirrored.Filled.VolumeUp
                } else {
                    Icons.AutoMirrored.Filled.VolumeOff
                },
                contentDescription = "Speaker"
            )
        }

        IconButton(
            onClick = onEndCall,
            modifier = Modifier
                .size(64.dp)
                .background(Color(0xFFE53935), CircleShape)
        ) {
            Icon(
                Icons.Default.CallEnd,
                contentDescription = "End call",
                tint = Color.White
            )
        }
    }
}

@Composable
private fun ControlButton(
    onClick: () -> Unit,
    active: Boolean = true,
    content: @Composable () -> Unit
) {
    IconButton(
        onClick = onClick,
        modifier = Modifier
            .size(52.dp)
            .background(
                if (active) Color.White.copy(alpha = 0.14f) else Color(0xFFB0BEC5).copy(alpha = 0.24f),
                CircleShape
            )
    ) {
        CompositionLocalProvider(LocalContentColor provides Color.White) {
            content()
        }
    }
}
