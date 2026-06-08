package com.example.securecall.data.repository

import android.util.Log
import com.example.securecall.data.mapper.toDocument
import com.example.securecall.data.mapper.toIceCandidate
import com.example.securecall.data.mapper.toSdpDocument
import com.example.securecall.data.mapper.toSessionDescription
import com.example.securecall.data.remote.dto.CallDto
import com.example.securecall.data.remote.dto.IceCandidateDocument
import com.example.securecall.data.remote.dto.SdpDocument
import com.example.securecall.data.webrtc.WebRTCClient
import com.example.securecall.domain.model.CallStatus
import com.example.securecall.domain.repository.WebRTCRepository
import com.google.firebase.firestore.DocumentChange
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.ListenerRegistration
import kotlinx.coroutines.channels.awaitClose
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.callbackFlow
import kotlinx.coroutines.flow.take
import kotlinx.coroutines.suspendCancellableCoroutine
import kotlinx.coroutines.tasks.await
import org.webrtc.EglBase
import org.webrtc.IceCandidate
import org.webrtc.PeerConnection
import org.webrtc.SessionDescription
import org.webrtc.SurfaceViewRenderer
import kotlin.coroutines.resumeWithException

class WebRTCRepositoryImpl(
    private val firestore: FirebaseFirestore,
    private val webRTCClient: WebRTCClient
): WebRTCRepository {

    companion object {
        private const val CALLS_COLLECTION = "calls"
        private const val OFFER_DOCUMENT = "offer"
        private const val ANSWER_DOCUMENT = "answer"
        private const val ICE_CANDIDATES_COLLECTION = "iceCandidates"
    }

    private var offerListener: ListenerRegistration? = null
    private var answerListener: ListenerRegistration? = null
    private var iceCandidatesListener: ListenerRegistration? = null

    //Collections
    private fun callDoc(callId: String) =
        firestore.collection(CALLS_COLLECTION).document(callId)

    private fun offerDoc(callId: String) =
        callDoc(callId).collection(OFFER_DOCUMENT).document(OFFER_DOCUMENT)

    private fun answerDoc(callId: String) =
        callDoc(callId).collection(ANSWER_DOCUMENT).document(ANSWER_DOCUMENT)

    private fun iceCandidatesCol(callId: String) =
        callDoc(callId).collection(ICE_CANDIDATES_COLLECTION)



    override suspend fun createCall(call: CallDto): Result<Unit> = runCatching {
        Log.d("WEBRTC", call.toString())
        callDoc(call.callId).set(call).await()
    }

    override suspend fun endCall(callId: String) {
        updateCallStatus(callId, CallStatus.ENDED)
        cleanup()
    }

    override suspend fun rejectCall(callId: String) {
        updateCallStatus(callId, CallStatus.REJECTED)
        cleanup()
    }

    override fun listenForCallStatus(callId: String): Flow<CallStatus> =
        callbackFlow {

            val listener = callDoc(callId)
                .addSnapshotListener { snapshot, error ->

                    if (error != null) {
                        close(error)
                        return@addSnapshotListener
                    }

                    snapshot
                        ?.getString("status")
                        ?.let { runCatching { CallStatus.valueOf(it) }.getOrNull() }
                        ?.let { trySend(it) }
                }

            awaitClose { listener.remove() }
        }

    override fun listenForIncomingCall(userId: String): Flow<CallDto?> = callbackFlow {
        val listener = firestore.collection(CALLS_COLLECTION)
            .whereEqualTo("receiverId", userId)
            .whereEqualTo("status", CallStatus.CALLING.name)
            .addSnapshotListener { snapshot, error ->
                if (error != null) { close(error); return@addSnapshotListener }
                val call = snapshot?.documents
                    ?.firstOrNull()
                    ?.toObject(CallDto::class.java)
                trySend(call)
            }
        awaitClose { listener.remove() }
    }


    override suspend fun createAndSendOffer(callId: String): Result<Unit> =
        runCatching {
            val sdp = awaitOffer()
            offerDoc(callId)
                .set(sdp.toSdpDocument())
                .await()
            updateCallStatus(callId, CallStatus.CALLING)
        }

    override fun listenForOffer(callId: String): Flow<SessionDescription> =
        callbackFlow {
            val listener = offerDoc(callId)
                .addSnapshotListener { snapshot, error ->

                    if (error != null) {
                        close(error)
                        return@addSnapshotListener
                    }

                    snapshot
                        ?.takeIf { it.exists() }
                        ?.toObject(SdpDocument::class.java)
                        ?.toSessionDescription()
                        ?.let { trySend(it) }
                }

            awaitClose { listener.remove() }
    }

    override suspend fun handleOfferAndSendAnswer(
        callId: String,
        offer: SessionDescription
    ): Result<Unit> =
        runCatching {

            val answer = awaitAnswer(offer)

            answerDoc(callId)
                .set(answer.toSdpDocument())
                .await()

            updateCallStatus(callId, CallStatus.ONGOING)
        }

    override fun listenForAnswer(callId: String): Flow<SessionDescription> =
        callbackFlow {

            val listener = answerDoc(callId)
                .addSnapshotListener { snapshot, error ->

                    if (error != null) {
                        close(error)
                        return@addSnapshotListener
                    }

                    snapshot
                        ?.takeIf { it.exists() }
                        ?.toObject(SdpDocument::class.java)
                        ?.toSessionDescription()
                        ?.let { trySend(it) }
                }

            awaitClose { listener.remove() }
        }

    override suspend fun handleAnswer(callId: String): Result<Unit> =
        runCatching {
            listenForAnswer(callId)
                .take(1)
                .collect { answer ->
                    webRTCClient.handleAnswer(answer)
                }
        }


    override suspend fun sendIceCandidate(
        callId: String,
        candidate: IceCandidate,
        senderId: String
    ) : Result<Unit> =
        runCatching {
            iceCandidatesCol(callId)
                .add(candidate.toDocument(senderId))
                .await()
        }

    override fun listenForIceCandidates(callId: String, localUserId: String): Flow<IceCandidate> =
        callbackFlow {

            val listener = iceCandidatesCol(callId)
                .addSnapshotListener { snapshot, error ->

                    if (error != null) {
                        close(error)
                        return@addSnapshotListener
                    }

                    snapshot?.documentChanges
                        ?.filter { it.type == DocumentChange.Type.ADDED }
                        ?.mapNotNull {
                            val document = it.document.toObject(IceCandidateDocument::class.java)
                            if (document.senderId == localUserId) {
                                null
                            } else {
                                document.toIceCandidate()
                            }
                        }
                        ?.forEach { trySend(it) }
                }

            awaitClose { listener.remove() }
        }

    override fun addIceCandidate(candidate: IceCandidate) {
        webRTCClient.addIceCandidate(candidate)
    }

    override fun initializeSession(
        observer: PeerConnection.Observer,
        localRenderer: SurfaceViewRenderer,
        isVideoCall: Boolean
    ): Result<Unit> = runCatching {

        val ok = webRTCClient.initializePeerConnection(observer, isVideoCall)
        if (!ok) throw IllegalStateException("PeerConnection init failed")

        webRTCClient.setupLocalTracks(localRenderer, isVideoCall)
    }

    override fun getEglContext(): EglBase.Context = webRTCClient.getEglContext()

    override fun setMicEnabled(enabled: Boolean) {
        webRTCClient.setMicEnabled(enabled)
    }

    override fun setCameraEnabled(enabled: Boolean) {
        webRTCClient.setCameraEnabled(enabled)
    }

    override fun switchCamera() {
        webRTCClient.switchCamera()
    }

    override fun setSpeakerEnabled(enabled: Boolean) {
        webRTCClient.setSpeakerEnabled(enabled)
    }

    override fun attachRemoteRenderer(renderer: SurfaceViewRenderer) {
        webRTCClient.attachRemoteRenderer(renderer)
    }



    override fun cleanup() {
        offerListener?.remove()
        answerListener?.remove()
        iceCandidatesListener?.remove()
        offerListener = null
        answerListener = null
        iceCandidatesListener = null
        webRTCClient.close()
    }

    private suspend fun updateCallStatus(callId: String, status: CallStatus) {
            runCatching {
                callDoc(callId).update("status", status.name).await()
            }
    }

    private suspend fun awaitOffer(): SessionDescription =
        suspendCancellableCoroutine { cont ->
            webRTCClient.createOffer(
                onSuccess = { cont.resume(it) {} },
                onError = { cont.resumeWithException(Exception(it)) }
            )
        }

    private suspend fun awaitAnswer(
        offer: SessionDescription
    ): SessionDescription =
        suspendCancellableCoroutine { cont ->
            webRTCClient.handleOffer(
                sdp = offer,
                onAnswerReady = { cont.resume(it) {} },
                onError = { cont.resumeWithException(Exception(it)) }
            )
        }

}



