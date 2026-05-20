package com.example.securecall.data.webrtc

import android.content.Context
import android.media.AudioManager
import android.util.Log
import com.example.securecall.data.signaling.SdpObserverAdapter
import kotlinx.coroutines.suspendCancellableCoroutine
import org.webrtc.AudioTrack
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

    private var videoCapturer: CameraVideoCapturer? = null
    private var surfaceTextureHelper: SurfaceTextureHelper? = null

    private var isCameraEnabled = true
    private var isFrontCamera = true
    private var isMicEnabled = true
    private var isCapturing = false

    private val iceServers = listOf(
        PeerConnection.IceServer.builder(
            "stun:stun.l.google.com:19302"
        ).createIceServer(),

        PeerConnection.IceServer.builder(
            "stun:stun1.l.google.com:19302"
        ).createIceServer()
    )

    private val mediaConstraints = MediaConstraints().apply {
        mandatory.add(MediaConstraints.KeyValuePair("OfferToReceiveAudio", "true"))
        mandatory.add(MediaConstraints.KeyValuePair("OfferToReceiveVideo", "true"))
    }

    fun initializePeerConnection(observer: PeerConnection.Observer): Boolean {
        val config = PeerConnection.RTCConfiguration(iceServers).apply {
            sdpSemantics = PeerConnection.SdpSemantics.UNIFIED_PLAN
            continualGatheringPolicy = PeerConnection.ContinualGatheringPolicy.GATHER_CONTINUALLY
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
            })
        return peerConnection != null
    }

    fun setupLocalTracks(localRenderer: SurfaceViewRenderer) {

        val factory = webRTCManager.peerConnectionFactory

        surfaceTextureHelper = SurfaceTextureHelper.create(
            "CaptureThread",
            webRTCManager.eglContext
        )

        initializeAudio()
        videoCapturer = createCameraCapturer()

        val videoSource = factory.createVideoSource(videoCapturer?.isScreencast ?: false)

        videoCapturer?.initialize(
            surfaceTextureHelper,
            webRTCManager.context,
            videoSource.capturerObserver
        )

        videoCapturer?.startCapture(1280, 720, 30)

        localVideoTrack = factory
            .createVideoTrack(TRACK_ID_VIDEO_LOCAL, videoSource)
            .also { track ->
                track.addSink(localRenderer)
                peerConnection?.addTrack(track, listOf(STREAM_ID))
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
        val enumerator = Camera2Enumerator(webRTCManager.context)

        val deviceName = enumerator.deviceNames.firstOrNull {
            enumerator.isFrontFacing(it)
        } ?: enumerator.deviceNames.firstOrNull {
            enumerator.isBackFacing(it)
        }

        return deviceName?.let {
            enumerator.createCapturer(it, null)
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
        }, mediaConstraints)
    }

    fun handleOffer(
        sdp: SessionDescription,
        onAnswerReady: (SessionDescription) -> Unit,
        onError: (String?) -> Unit
    ) {
        peerConnection?.setRemoteDescription(object : SdpObserverAdapter() {
            override fun onSetSuccess() = createAnswer(onAnswerReady, onError)
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
        }, mediaConstraints)
    }

    suspend fun handleAnswer(sdp: SessionDescription) = suspendCancellableCoroutine { cont ->
        peerConnection?.setRemoteDescription(object : SdpObserverAdapter() {
            override fun onSetSuccess() = cont.resume(Unit) {}
            override fun onSetFailure(error: String?) =
                cont.resumeWithException(Exception(error))
        }, sdp)
    }

    fun addIceCandidate(candidate: IceCandidate) {
        peerConnection?.addIceCandidate(candidate)
    }

    fun setMicEnabled(enabled: Boolean) {
        isMicEnabled = enabled
        localAudioTrack?.setEnabled(enabled)
    }

    fun setCameraEnabled(enabled: Boolean) {
        isCameraEnabled = enabled
        localVideoTrack?.setEnabled(enabled)

        try {
            if (enabled && !isCapturing) {
                videoCapturer?.startCapture(1280, 720, 30)
                isCapturing = true
            } else if (!enabled && isCapturing) {
                videoCapturer?.stopCapture()
                isCapturing = false
            }
        } catch (e: Exception) {
            Log.e("WebRTC", "Camera toggle error", e)
        }
    }

    fun switchCamera() {
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

        peerConnection?.close()
        peerConnection?.dispose()
        peerConnection = null
    }


    companion object {
        private const val TRACK_ID_VIDEO_LOCAL = "local_video_track"
        private const val TRACK_ID_AUDIO_LOCAL = "local_audio_track"
        private const val STREAM_ID = "local_stream"
    }


}