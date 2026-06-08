package com.example.securecall.ui.view.call

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.systemBarsPadding
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.KeyboardArrowLeft
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableLongStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.viewinterop.AndroidView
import com.example.securecall.ui.components.CallControls
import com.example.securecall.ui.viewmodel.CallUiState
import kotlinx.coroutines.delay
import org.webrtc.SurfaceViewRenderer

@Composable
fun VideoCallScreen(
    uiState: CallUiState,
    contactName: String,
    localRenderer: SurfaceViewRenderer,
    remoteRenderer: SurfaceViewRenderer,
    onToggleMic: () -> Unit,
    onToggleCamera: () -> Unit,
    onSwitchCamera: () -> Unit,
    onToggleSpeaker: () -> Unit,
    onEndCall: () -> Unit
) {
    var controlsVisible by remember { mutableStateOf(true) }
    var elapsedSeconds by remember { mutableLongStateOf(0L) }

    LaunchedEffect(Unit) {
        while (true) {
            delay(1_000)
            elapsedSeconds += 1
        }
    }

    LaunchedEffect(controlsVisible) {
        if (controlsVisible) {
            delay(3_000)
            controlsVisible = false
        }
    }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(Color.Black)
            .clickable { controlsVisible = true }
    ) {
        AndroidView(
            factory = { remoteRenderer.apply { setZOrderMediaOverlay(false) } },
            update = { remoteRenderer.requestLayout() },
            modifier = Modifier.fillMaxSize()
        )

        Box(
            modifier = Modifier
                .fillMaxSize()
                .background(
                    Brush.verticalGradient(
                        colors = listOf(
                            Color.Black.copy(alpha = 0.38f),
                            Color.Transparent,
                            Color.Black.copy(alpha = 0.55f)
                        )
                    )
                )
        )

        AnimatedVisibility(
            visible = controlsVisible,
            enter = fadeIn(),
            exit = fadeOut(),
            modifier = Modifier.align(Alignment.TopStart)
        ) {
            Row(
                modifier = Modifier
                    .systemBarsPadding()
                    .padding(horizontal = 12.dp, vertical = 8.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                IconButton(onClick = { controlsVisible = false }) {
                    Icon(
                        Icons.AutoMirrored.Filled.KeyboardArrowLeft,
                        contentDescription = "Back",
                        tint = Color.White
                    )
                }
                Column {
                    Text(
                        text = contactName,
                        style = MaterialTheme.typography.titleMedium,
                        color = Color.White,
                        fontWeight = FontWeight.SemiBold
                    )
                    Text(
                        text = "${formatDuration(elapsedSeconds)} - ${connectionLabel(uiState.iceConnectionState)}",
                        style = MaterialTheme.typography.bodySmall,
                        color = Color.White.copy(alpha = 0.78f)
                    )
                }
            }
        }

        Box(
            modifier = Modifier
                .systemBarsPadding()
                .padding(top = 16.dp, end = 16.dp)
                .size(width = 112.dp, height = 156.dp)
                .align(Alignment.TopEnd)
                .clip(RoundedCornerShape(18.dp))
                .border(1.dp, Color.White.copy(alpha = 0.28f), RoundedCornerShape(18.dp))
                .background(Color.Black)
        ) {
            AndroidView(
                factory = { localRenderer.apply { setZOrderMediaOverlay(true) } },
                modifier = Modifier.fillMaxSize()
            )
            if (!uiState.isCameraEnabled) {
                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .background(Color(0xFF202124)),
                    contentAlignment = Alignment.Center
                ) {
                    Box(
                        modifier = Modifier
                            .size(42.dp)
                            .background(Color.White.copy(alpha = 0.18f), CircleShape)
                    )
                }
            }
        }

        AnimatedVisibility(
            visible = controlsVisible,
            enter = fadeIn(),
            exit = fadeOut(),
            modifier = Modifier.align(Alignment.BottomCenter)
        ) {
            CallControls(
                uiState = uiState,
                onToggleMic = onToggleMic,
                onToggleCamera = onToggleCamera,
                onSwitchCamera = onSwitchCamera,
                onToggleSpeaker = onToggleSpeaker,
                onEndCall = onEndCall,
                showVideoControls = true,
                modifier = Modifier
                    .systemBarsPadding()
                    .padding(horizontal = 16.dp, vertical = 18.dp)
            )
        }
    }
}

internal fun formatDuration(totalSeconds: Long): String {
    val minutes = totalSeconds / 60
    val seconds = totalSeconds % 60
    return "%02d:%02d".format(minutes, seconds)
}

private fun connectionLabel(state: String?): String = when (state) {
    "CONNECTED", "COMPLETED" -> "Buena"
    "CHECKING", "NEW" -> "Conectando"
    "DISCONNECTED" -> "Regular"
    "FAILED" -> "Mala"
    else -> "Estable"
}
