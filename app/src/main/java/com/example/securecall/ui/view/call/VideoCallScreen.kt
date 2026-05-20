package com.example.securecall.ui.view.call

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.systemBarsPadding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import androidx.compose.ui.viewinterop.AndroidView
import com.example.securecall.ui.components.CallControls
import com.example.securecall.ui.viewmodel.CallUiState
import org.webrtc.SurfaceViewRenderer

@Composable
fun VideoCallScreen(
    uiState: CallUiState,
    localRenderer: SurfaceViewRenderer,
    remoteRenderer: SurfaceViewRenderer,
    onToggleMic: () -> Unit,
    onToggleCamera: () -> Unit,
    onSwitchCamera: () -> Unit,
    onToggleSpeaker: () -> Unit,
    onEndCall: () -> Unit
) {

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(Color.Black)
            .systemBarsPadding()  // ← respeta status bar y navigation bar
    ) {

        // ── Remote video — fondo completo ────────────────────────────────────
        AndroidView(
            factory = { remoteRenderer.apply { setZOrderMediaOverlay(false) } },
            update = { remoteRenderer.requestLayout() },
            modifier = Modifier.fillMaxSize()
        )

        // ── Local PiP — esquina superior derecha con bordes redondeados ──────
        Box(
            modifier = Modifier
                .padding(top = 16.dp, end = 16.dp)
                .size(width = 100.dp, height = 140.dp)
                .align(Alignment.TopEnd)
                .clip(RoundedCornerShape(16.dp))
                .border(
                    width = 2.dp,
                    color = Color.White.copy(alpha = 0.3f),
                    shape = RoundedCornerShape(16.dp)
                )
                .shadow(
                    elevation = 8.dp,
                    shape = RoundedCornerShape(16.dp)
                )
        ) {
            AndroidView(
                factory = { localRenderer.apply { setZOrderMediaOverlay(true) } },
                modifier = Modifier.fillMaxSize()
            )
        }

        Box(
            modifier = Modifier
                .fillMaxWidth()
                .height(180.dp)
                .align(Alignment.BottomCenter)
                .background(
                    brush = Brush.verticalGradient(
                        colors = listOf(
                            Color.Transparent,
                            Color.Black.copy(alpha = 0.7f)
                        )
                    )
                )
        )
        // 🎛 Controls
        CallControls(
            uiState = uiState,
            onToggleMic = onToggleMic,
            onToggleCamera = onToggleCamera,
            onSwitchCamera = onSwitchCamera,
            onToggleSpeaker = onToggleSpeaker,
            onEndCall = onEndCall,
            modifier = Modifier.align(Alignment.BottomCenter)
        )
    }
}