package com.example.securecall.ui.view.call

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import com.example.securecall.ui.components.CallControls
import com.example.securecall.ui.viewmodel.CallUiState

@Composable
fun AudioCallScreen(
    uiState: CallUiState,
    onToggleMic: () -> Unit,
    onToggleSpeaker: () -> Unit,
    onEndCall: () -> Unit
) {

    Box(
        modifier = Modifier.fillMaxSize(),
        contentAlignment = Alignment.Center
    ) {

        Column(horizontalAlignment = Alignment.CenterHorizontally) {

            Box(
                modifier = Modifier
                    .size(140.dp)
                    .background(Color.Gray, CircleShape)
            )

            Spacer(Modifier.height(16.dp))

            Text("In call", style = MaterialTheme.typography.titleMedium)

            Spacer(Modifier.height(32.dp))

            CallControls(
                uiState = uiState,
                onToggleMic = {},
                onToggleCamera = {}, // hidden or disabled
                onSwitchCamera = {},
                onToggleSpeaker = onToggleSpeaker,
                onEndCall = onEndCall
            )
        }
    }
}