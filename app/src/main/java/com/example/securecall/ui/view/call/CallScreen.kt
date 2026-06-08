package com.example.securecall.ui.view.call

import android.Manifest
import android.content.pm.PackageManager
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.core.content.ContextCompat
import androidx.hilt.navigation.compose.hiltViewModel
import com.example.securecall.domain.model.CallStatus
import com.example.securecall.domain.model.CallType
import com.example.securecall.ui.components.ErrorBanner
import com.example.securecall.ui.viewmodel.CallUiState
import com.example.securecall.ui.viewmodel.CallViewModel
import org.webrtc.DataChannel
import org.webrtc.IceCandidate
import org.webrtc.MediaStream
import org.webrtc.PeerConnection
import org.webrtc.RendererCommon
import org.webrtc.RtpTransceiver
import org.webrtc.SurfaceViewRenderer

@Composable
fun CallScreen(
    callId: String,
    receiverId: String,
    isCaller: Boolean,
    callType: CallType,
    contactName: String,
    uiState: CallUiState,
    onAccept: () -> Unit = {},
    onReject: () -> Unit = {},
    onEndCall: () -> Unit = {},
    onToggleMic: () -> Unit = {},
    onToggleCamera: () -> Unit = {},
    onSwitchCamera: () -> Unit = {},
    onToggleSpeaker: () -> Unit = {},
    viewModel: CallViewModel = hiltViewModel()
) {
    val context = LocalContext.current
    val requiredPermissions = remember(callType) {
        if (callType == CallType.VIDEO) {
            arrayOf(Manifest.permission.RECORD_AUDIO, Manifest.permission.CAMERA)
        } else {
            arrayOf(Manifest.permission.RECORD_AUDIO)
        }
    }

    fun hasRequiredPermissions(): Boolean = requiredPermissions.all { permission ->
        ContextCompat.checkSelfPermission(context, permission) == PackageManager.PERMISSION_GRANTED
    }

    var permissionsGranted by remember(callType) { mutableStateOf(hasRequiredPermissions()) }
    var callInitialized by remember(callId, callType) { mutableStateOf(false) }
    val permissionLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.RequestMultiplePermissions()
    ) { results ->
        val granted = requiredPermissions.all { results[it] == true }
        permissionsGranted = granted
        if (!granted) {
            viewModel.permissionsDenied()
            onReject()
        }
    }

    val eglContext = remember { viewModel.getEglContext() }

    val localRenderer = remember {
        SurfaceViewRenderer(context).apply {
            init(eglContext, null)
            setMirror(true)
        }
    }

    val remoteRenderer = remember {
        SurfaceViewRenderer(context).apply {
            init(eglContext, null)
            setScalingType(RendererCommon.ScalingType.SCALE_ASPECT_FILL)
            setEnableHardwareScaler(true)
        }
    }

    val peerConnectionObserver = remember {
        object : PeerConnection.Observer {
            override fun onIceCandidate(candidate: IceCandidate?) {
                candidate ?: return
                viewModel.onLocalIceCandidate(candidate)
            }

            override fun onTrack(transceiver: RtpTransceiver?) = Unit
            override fun onConnectionChange(state: PeerConnection.PeerConnectionState?) = Unit
            override fun onSignalingChange(state: PeerConnection.SignalingState?) = Unit
            override fun onIceConnectionChange(state: PeerConnection.IceConnectionState?) {
                viewModel.onIceConnectionStateChanged(state)
            }

            override fun onIceGatheringChange(state: PeerConnection.IceGatheringState?) {
                viewModel.onIceGatheringStateChanged(state)
            }

            override fun onIceCandidatesRemoved(candidates: Array<out IceCandidate>?) = Unit
            override fun onIceConnectionReceivingChange(receiving: Boolean) = Unit
            override fun onAddStream(stream: MediaStream?) = Unit
            override fun onRemoveStream(stream: MediaStream?) = Unit
            override fun onDataChannel(channel: DataChannel?) = Unit
            override fun onRenegotiationNeeded() = Unit
        }
    }

    LaunchedEffect(callType, permissionsGranted) {
        if (!permissionsGranted) {
            viewModel.waitingForPermissions(callType)
            permissionLauncher.launch(requiredPermissions)
        }
    }

    LaunchedEffect(callId, permissionsGranted, callType, callInitialized) {
        if (!permissionsGranted || callInitialized) return@LaunchedEffect

        viewModel.initializeCall(
            callId = callId,
            observer = peerConnectionObserver,
            localRenderer = localRenderer,
            receiverId = receiverId,
            isCaller = isCaller,
            callType = callType
        )

        if (callType == CallType.VIDEO) {
            viewModel.setRemoteRenderer(remoteRenderer)
        }
        callInitialized = true
    }

    DisposableEffect(Unit) {
        onDispose {
            localRenderer.release()
            remoteRenderer.release()
        }
    }

    Box(modifier = Modifier.fillMaxSize()) {
        when {
            uiState.callStatus == CallStatus.CALLING && !uiState.isInCall -> {
                CallingScreen(
                    isConnecting = uiState.isConnecting,
                    onReject = onReject
                )
            }

            uiState.isInCall && uiState.isVideoCall -> {
                VideoCallScreen(
                    uiState = uiState,
                    contactName = contactName,
                    localRenderer = localRenderer,
                    remoteRenderer = remoteRenderer,
                    onToggleMic = onToggleMic,
                    onToggleCamera = onToggleCamera,
                    onSwitchCamera = onSwitchCamera,
                    onToggleSpeaker = onToggleSpeaker,
                    onEndCall = onEndCall
                )
            }

            uiState.isInCall -> {
                AudioCallScreen(
                    uiState = uiState,
                    contactName = contactName,
                    onToggleMic = onToggleMic,
                    onToggleSpeaker = onToggleSpeaker,
                    onEndCall = onEndCall
                )
            }

            uiState.isWaitingForPermissions -> {
                Box(
                    modifier = Modifier.fillMaxSize(),
                    contentAlignment = Alignment.Center
                ) {
                    CircularProgressIndicator()
                }
            }
        }

        uiState.error?.let { error ->
            if (error == "Error de conexion") {
                AlertDialog(
                    onDismissRequest = viewModel::clearError,
                    title = { Text("Error de conexion") },
                    text = { Text("No se pudo establecer la conexion WebRTC. Reintenta la llamada.") },
                    confirmButton = {
                        TextButton(
                            onClick = {
                                viewModel.clearError()
                                callInitialized = false
                            }
                        ) {
                            Text("Reintentar")
                        }
                    },
                    dismissButton = {
                        TextButton(onClick = onEndCall) {
                            Text("Colgar")
                        }
                    }
                )
            } else {
                ErrorBanner(error)
            }
        }
    }
}
