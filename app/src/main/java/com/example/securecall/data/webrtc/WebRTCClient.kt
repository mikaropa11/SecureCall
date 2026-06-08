package com.example.securecall.data.webrtc

import android.content.Context
import android.media.AudioManager
import android.os.Build
import android.util.Log
import com.example.securecall.BuildConfig
import com.example.securecall.data.signaling.SdpObserverAdapter
import kotlinx.coroutines.suspendCancellableCoroutine
import org.webrtc.AudioTrack
import org.webrtc.Camera1Enumerator
import org.webrtc.Camera2Enumerator
import org.webrtc.CameraVideoCapturer
import org.webrtc.EglBase
import org.webrtc.IceCandidate
import org.webrtc.MediaConstraints
import org.webrtc.PeerConnection
import org.webrtc.RtpTransceiver
import org.webrtc.SessionDescription
import org.webrtc.SurfaceTextureHelper
import org.webrtc.SurfaceViewRenderer
import org.webrtc.VideoTrack
import kotlin.coroutines.resumeWithException

class WebRTCClient(
    private val webRTCManager: WebRTCManager
) {

    private var peerConnection: PeerConnection? = null
    private var localAudioTrack: AudioTrack? = null
    private var localVideoTrack: VideoTrack? = null
    private var remoteVideoTrack: VideoTrack? = null
    private var pendingRemoteRenderer: SurfaceViewRenderer? = null
    private val pendingRemoteCandidates = mutableListOf<IceCandidate>()
    private var remoteDescriptionSet = false

    private var videoCapturer: CameraVideoCapturer? = null
    private var surfaceTextureHelper: SurfaceTextureHelper? = null

    private var isCameraEnabled = true
    private var isVideoCall = true
    private var isFrontCamera = true
    private var isMicEnabled = true
    private var isCapturing = false

    private val iceServers: List<PeerConnection.IceServer> = buildList {
        add(PeerConnection.IceServer.builder("stun:stun.l.google.com:19302").createIceServer())
        add(PeerConnection.IceServer.builder("stun:stun1.l.google.com:19302").createIceServer())

        if (BuildConfig.TURN_USERNAME.isNotBlank() && BuildConfig.TURN_CREDENTIAL.isNotBlank()) {
            add(
                PeerConnection.IceServer.builder(
                    listOf(
                        "turns:fr-turn2.xirsys.com:443?transport=udp",
                        "turns:fr-turn2.xirsys.com:443?transport=tcp",

                        "turn:fr-turn2.xirsys.com:80?transport=udp",
                        "turn:fr-turn2.xirsys.com:3478?transport=udp",
                        "turn:fr-turn2.xirsys.com:80?transport=tcp",
                        "turn:fr-turn2.xirsys.com:3478?transport=tcp",

                        "turns:fr-turn2.xirsys.com:5349?transport=tcp",
                        "turns:fr-turn2.xirsys.com:5349?transport=udp"
                    )
                )
                    .setUsername(BuildConfig.TURN_USERNAME)
                    .setPassword(BuildConfig.TURN_CREDENTIAL)
                    .createIceServer()
            )
        } else {
            Log.w("WEBRTC_ICE", "TURN credentials are empty; relay candidates will not be available")
        }
    }

    private fun mediaConstraints() = MediaConstraints().apply {
        mandatory.add(MediaConstraints.KeyValuePair("OfferToReceiveAudio", "true"))
        mandatory.add(MediaConstraints.KeyValuePair("OfferToReceiveVideo", isVideoCall.toString()))
    }

    fun initializePeerConnection(observer: PeerConnection.Observer, isVideoCall: Boolean): Boolean {
        this.isVideoCall = isVideoCall
        remoteDescriptionSet = false
        pendingRemoteCandidates.clear()
        val config = PeerConnection.RTCConfiguration(iceServers).apply {
            sdpSemantics = PeerConnection.SdpSemantics.UNIFIED_PLAN
            continualGatheringPolicy = PeerConnection.ContinualGatheringPolicy.GATHER_CONTINUALLY
            iceCandidatePoolSize = 1
            iceTransportsType = PeerConnection.IceTransportsType.ALL
        }
        peerConnection = webRTCManager.peerConnectionFactory
            .createPeerConnection(config, object : PeerConnection.Observer by observer {
                override fun onTrack(transceiver: RtpTransceiver?) {
                    val track = transceiver?.receiver?.track()
                    Log.d("WEBRTC", "onTrack called, track: $track, pendingRemoteRenderer: $pendingRemoteRenderer")
                    if(track is VideoTrack) {
                        remoteVideoTrack = track
                        Log.d("WEBRTC", "Adding sink to remote track")
                        pendingRemoteRenderer?.let { track.addSink(it) }
                    }
                }

                override fun onIceCandidate(candidate: IceCandidate?) {
                    candidate?.let {
                        logCandidate("LOCAL", it)
                    }
                    observer.onIceCandidate(candidate)
                }

                override fun onIceConnectionChange(state: PeerConnection.IceConnectionState?) {
                    Log.d("WEBRTC_ICE", "onIceConnectionChange=$state")
                    observer.onIceConnectionChange(state)
                }

                override fun onIceGatheringChange(state: PeerConnection.IceGatheringState?) {
                    Log.d("WEBRTC_ICE", "onIceGatheringChange=$state")
                    observer.onIceGatheringChange(state)
                }

                override fun onConnectionChange(state: PeerConnection.PeerConnectionState?) {
                    Log.d("WEBRTC_ICE", "onConnectionChange=$state")
                    observer.onConnectionChange(state)
                }
            })
        return peerConnection != null
    }

    fun setupLocalTracks(localRenderer: SurfaceViewRenderer, isVideoCall: Boolean) {

        val factory = webRTCManager.peerConnectionFactory

        initializeAudio()
        if (isVideoCall) {
            surfaceTextureHelper = SurfaceTextureHelper.create(
                "CaptureThread",
                webRTCManager.eglContext
            )

            videoCapturer = createCameraCapturer()

            val videoSource = factory.createVideoSource(videoCapturer?.isScreencast ?: false)

            videoCapturer?.initialize(
                surfaceTextureHelper,
                webRTCManager.context,
                videoSource.capturerObserver
            )

            startCaptureWithFallback()

            localVideoTrack = factory
                .createVideoTrack(TRACK_ID_VIDEO_LOCAL, videoSource)
                .also { track ->
                    track.addSink(localRenderer)
                    peerConnection?.addTrack(track, listOf(STREAM_ID))
                }
        }

        val audioConstraints = MediaConstraints().apply {
            mandatory.add(
                MediaConstraints.KeyValuePair("googEchoCancellation", "true")
            )
            mandatory.add(
                MediaConstraints.KeyValuePair("googNoiseSuppression", "true")
            )
            mandatory.add(
                MediaConstraints.KeyValuePair("googAutoGainControl", "true")
            )
        }

        val audioSource = factory.createAudioSource(audioConstraints)

        localAudioTrack = factory
            .createAudioTrack(TRACK_ID_AUDIO_LOCAL, audioSource)
            .also { track ->
                peerConnection?.addTrack(track, listOf(STREAM_ID))
            }
    }

    private fun createCameraCapturer(): CameraVideoCapturer? {
        return createCameraCapturer(Camera2Enumerator(webRTCManager.context))
            ?: createCameraCapturer(Camera1Enumerator(false))
    }

    private fun createCameraCapturer(enumerator: org.webrtc.CameraEnumerator): CameraVideoCapturer? {
        val deviceName = enumerator.deviceNames.firstOrNull {
            enumerator.isFrontFacing(it)
        } ?: enumerator.deviceNames.firstOrNull {
            enumerator.isBackFacing(it)
        }

        return deviceName?.let {
            runCatching {
                enumerator.createCapturer(it, cameraEventsHandler)
            }.onFailure { error ->
                Log.e("WEBRTC_CAMERA", "createCapturer failed for $it", error)
            }.getOrNull()
        }
    }

    private val cameraEventsHandler = object : CameraVideoCapturer.CameraEventsHandler {
        override fun onCameraError(errorDescription: String?) {
            Log.e("WEBRTC_CAMERA", "onCameraError=$errorDescription")
        }

        override fun onCameraDisconnected() {
            Log.w("WEBRTC_CAMERA", "onCameraDisconnected")
        }

        override fun onCameraFreezed(errorDescription: String?) {
            Log.e("WEBRTC_CAMERA", "onCameraFreezed=$errorDescription")
        }

        override fun onCameraOpening(cameraName: String?) {
            Log.d("WEBRTC_CAMERA", "onCameraOpening=$cameraName")
        }

        override fun onFirstFrameAvailable() {
            Log.d("WEBRTC_CAMERA", "onFirstFrameAvailable")
        }

        override fun onCameraClosed() {
            Log.d("WEBRTC_CAMERA", "onCameraClosed")
        }
    }

    private fun startCaptureWithFallback() {
        val resolutions = if (Build.PRODUCT == "klimt_eea") {
            listOf(
                Triple(640, 480, 30),
                Triple(640, 360, 24),
                Triple(320, 240, 15)
            )
        } else {
            listOf(
                Triple(1280, 720, 30),
                Triple(640, 480, 30),
                Triple(640, 360, 24),
                Triple(320, 240, 15)
            )
        }

        for ((width, height, fps) in resolutions) {
            try {
                videoCapturer?.startCapture(width, height, fps)
                isCapturing = true
                Log.d("WEBRTC_CAMERA", "startCapture ${width}x$height@$fps")
                return
            } catch (e: Exception) {
                Log.e("WEBRTC_CAMERA", "startCapture failed ${width}x$height@$fps", e)
            }
        }
    }

    private fun initializeAudio() {
        val audioManager =
            webRTCManager.context.getSystemService(Context.AUDIO_SERVICE) as AudioManager
        audioManager.mode = AudioManager.MODE_IN_COMMUNICATION
        audioManager.isSpeakerphoneOn = false
        audioManager.requestAudioFocus(
            null,
            AudioManager.STREAM_VOICE_CALL,
            AudioManager.AUDIOFOCUS_GAIN
        )
    }


    //SDP offers
    fun createOffer(onSuccess: (SessionDescription) -> Unit, onError: (String?) -> Unit) {
        peerConnection?.createOffer(object : SdpObserverAdapter() {
            override fun onCreateSuccess(sdp: SessionDescription) {
                peerConnection?.setLocalDescription(object : SdpObserverAdapter() {
                    override fun onSetSuccess() = onSuccess(sdp)
                    override fun onSetFailure(error: String?) = onError(error)
                }, sdp)
            }
            override fun onCreateFailure(error: String?) = onError(error)
        }, mediaConstraints())
    }

    fun handleOffer(
        sdp: SessionDescription,
        onAnswerReady: (SessionDescription) -> Unit,
        onError: (String?) -> Unit
    ) {
        peerConnection?.setRemoteDescription(object : SdpObserverAdapter() {
            override fun onSetSuccess() {
                remoteDescriptionSet = true
                drainPendingRemoteCandidates()
                createAnswer(onAnswerReady, onError)
            }
            override fun onSetFailure(error: String?) = onError(error)
        }, sdp)
    }

    private fun createAnswer(onSuccess: (SessionDescription) -> Unit, onError: (String?) -> Unit) {
        peerConnection?.createAnswer(object : SdpObserverAdapter() {
            override fun onCreateSuccess(sdp: SessionDescription) {
                peerConnection?.setLocalDescription(object : SdpObserverAdapter() {
                    override fun onSetSuccess() = onSuccess(sdp)
                    override fun onSetFailure(error: String?) = onError(error)
                }, sdp)
            }
            override fun onCreateFailure(error: String?) = onError(error)
        }, mediaConstraints())
    }

    suspend fun handleAnswer(sdp: SessionDescription) = suspendCancellableCoroutine { cont ->
        peerConnection?.setRemoteDescription(object : SdpObserverAdapter() {
            override fun onSetSuccess() {
                remoteDescriptionSet = true
                drainPendingRemoteCandidates()
                cont.resume(Unit) {}
            }
            override fun onSetFailure(error: String?) =
                cont.resumeWithException(Exception(error))
        }, sdp)
    }

    fun addIceCandidate(candidate: IceCandidate) {
        logCandidate("REMOTE", candidate)
        if (!remoteDescriptionSet) {
            pendingRemoteCandidates.add(candidate)
            Log.d("WEBRTC_ICE", "queued remote candidate count=${pendingRemoteCandidates.size}")
            return
        }

        val added = peerConnection?.addIceCandidate(candidate) ?: false
        Log.d("WEBRTC_ICE", "addIceCandidate result=$added")
    }

    private fun drainPendingRemoteCandidates() {
        if (pendingRemoteCandidates.isEmpty()) return
        Log.d("WEBRTC_ICE", "draining ${pendingRemoteCandidates.size} queued remote candidates")
        pendingRemoteCandidates.toList().forEach { candidate ->
            val added = peerConnection?.addIceCandidate(candidate) ?: false
            Log.d("WEBRTC_ICE", "addQueuedIceCandidate result=$added")
        }
        pendingRemoteCandidates.clear()
    }

    fun setMicEnabled(enabled: Boolean) {
        isMicEnabled = enabled
        localAudioTrack?.setEnabled(enabled)
    }

    fun setCameraEnabled(enabled: Boolean) {
        if (!isVideoCall) return
        isCameraEnabled = enabled
        localVideoTrack?.setEnabled(enabled)

        try {
            if (enabled && !isCapturing) {
                startCaptureWithFallback()
            } else if (!enabled && isCapturing) {
                videoCapturer?.stopCapture()
                isCapturing = false
            }
        } catch (e: Exception) {
            Log.e("WebRTC", "Camera toggle error", e)
        }
    }

    fun switchCamera() {
        if (!isVideoCall) return
        val capturer = videoCapturer as? CameraVideoCapturer ?: return

        try {
            capturer.switchCamera(null)
            isFrontCamera = !isFrontCamera
        } catch (e: Exception) {
            Log.e("WebRTC", "switchCamera error", e)
        }
    }

    fun setSpeakerEnabled(enabled: Boolean) {

        val audioManager =
            webRTCManager.context.getSystemService(Context.AUDIO_SERVICE) as AudioManager

        audioManager.mode = AudioManager.MODE_IN_COMMUNICATION
        audioManager.isSpeakerphoneOn = enabled
    }

    fun attachRemoteRenderer(renderer: SurfaceViewRenderer) {
        pendingRemoteRenderer = renderer
        Log.d("WEBRTC", "attachRemoteRenderer called, remoteTrack: $remoteVideoTrack")
        remoteVideoTrack?.addSink(renderer)
    }

    fun getEglContext(): EglBase.Context = webRTCManager.eglContext



    fun close() {
        try {
            videoCapturer?.stopCapture()
        } catch (_: Exception) {}
        videoCapturer = null

        surfaceTextureHelper?.dispose()
        surfaceTextureHelper = null

        localVideoTrack?.dispose()
        localVideoTrack = null

        localAudioTrack?.dispose()
        localAudioTrack = null

        remoteVideoTrack?.dispose()
        remoteVideoTrack = null

        pendingRemoteRenderer = null
        pendingRemoteCandidates.clear()
        remoteDescriptionSet = false

        peerConnection?.close()
        peerConnection?.dispose()
        peerConnection = null
    }


    companion object {
        private const val TRACK_ID_VIDEO_LOCAL = "local_video_track"
        private const val TRACK_ID_AUDIO_LOCAL = "local_audio_track"
        private const val STREAM_ID = "local_stream"
    }

    private fun logCandidate(direction: String, candidate: IceCandidate) {
        val sdp = candidate.sdp
        val type = Regex(" typ ([a-zA-Z0-9]+)").find(sdp)?.groupValues?.getOrNull(1) ?: "unknown"
        val protocol = Regex(" UDP | TCP ", RegexOption.IGNORE_CASE)
            .find(sdp)
            ?.value
            ?.trim()
            ?: "unknown"
        val parts = sdp.split(" ")
        val address = parts.getOrNull(4).orEmpty()
        val port = parts.getOrNull(5).orEmpty()
        Log.d(
            "WEBRTC_ICE",
            "$direction candidate type=$type protocol=$protocol address=$address port=$port mid=${candidate.sdpMid}"
        )
    }


}
