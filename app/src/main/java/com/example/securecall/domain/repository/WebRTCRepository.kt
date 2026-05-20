package com.example.securecall.domain.repository

import com.example.securecall.data.remote.dto.CallDto
import com.example.securecall.domain.model.CallStatus
import kotlinx.coroutines.flow.Flow
import org.webrtc.EglBase
import org.webrtc.IceCandidate
import org.webrtc.PeerConnection
import org.webrtc.SessionDescription
import org.webrtc.SurfaceViewRenderer

interface WebRTCRepository {

    suspend fun createCall(call: CallDto): Result<Unit>

    suspend fun endCall(callId: String)

    suspend fun rejectCall(callId: String)

    fun listenForCallStatus(callId: String): Flow<CallStatus>

    fun listenForIncomingCall(userId: String): Flow<CallDto?>

    suspend fun createAndSendOffer(callId: String): Result<Unit>

    fun listenForOffer(callId: String): Flow<SessionDescription>

    suspend fun handleOfferAndSendAnswer(
        callId: String,
        offer: SessionDescription
    ): Result<Unit>

    fun listenForAnswer(callId: String): Flow<SessionDescription>

    suspend fun handleAnswer(callId: String): Result<Unit>


    suspend fun sendIceCandidate(
        callId: String,
        candidate: IceCandidate
    ): Result<Unit>

    fun listenForIceCandidates(callId: String): Flow<IceCandidate>

    fun addIceCandidate(candidate: IceCandidate)

    fun initializeSession(
        observer: PeerConnection.Observer,
        localRenderer: SurfaceViewRenderer
    ): Result<Unit>
    fun getEglContext(): EglBase.Context
    fun setMicEnabled(enabled: Boolean)
    fun setCameraEnabled(enabled: Boolean)
    fun switchCamera()
    fun setSpeakerEnabled(enabled: Boolean)
    fun attachRemoteRenderer(renderer: SurfaceViewRenderer)


    fun cleanup()
}
