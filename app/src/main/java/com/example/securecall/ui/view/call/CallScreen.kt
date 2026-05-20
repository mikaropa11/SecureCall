package com.example.securecall.ui.view.call

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.hilt.navigation.compose.hiltViewModel
import com.example.securecall.domain.model.CallStatus
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

    val eglContext = remember { viewModel.getEglContext() }

    // Renderers
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

    // Observer WebRTC
    val peerConnectionObserver = remember {

        object : PeerConnection.Observer {

            override fun onIceCandidate(candidate: IceCandidate?) {
                candidate ?: return
                viewModel.onLocalIceCandidate(candidate)
            }

            override fun onTrack(transceiver: RtpTransceiver?) {
                // El remote track se gestiona dentro del client
            }

            override fun onConnectionChange(
                state: PeerConnection.PeerConnectionState?
            ) {}

            override fun onSignalingChange(
                state: PeerConnection.SignalingState?
            ) {}

            override fun onIceConnectionChange(
                state: PeerConnection.IceConnectionState?
            ) {}

            override fun onIceGatheringChange(
                state: PeerConnection.IceGatheringState?
            ) {}

            override fun onIceCandidatesRemoved(
                candidates: Array<out IceCandidate>?
            ) {}
            override fun onIceConnectionReceivingChange(receiving: Boolean) {}

            override fun onAddStream(stream: MediaStream?) {}

            override fun onRemoveStream(stream: MediaStream?) {}

            override fun onDataChannel(channel: DataChannel?) {}

            override fun onRenegotiationNeeded() {}
        }
    }

    // Inicialización
    LaunchedEffect(Unit) {

        viewModel.initializeCall(
            callId = callId,
            observer = peerConnectionObserver,
            localRenderer = localRenderer,
            receiverId = receiverId,
            isCaller = isCaller
        )

        viewModel.setRemoteRenderer(remoteRenderer)
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

            uiState.isInCall && uiState.isCameraEnabled -> {
                VideoCallScreen(
                    uiState = uiState,
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
                    onToggleMic = onToggleMic,
                    onToggleSpeaker = onToggleSpeaker,
                    onEndCall = onEndCall
                )
            }
        }

        uiState.error?.let {
            ErrorBanner(it)
        }
    }
}