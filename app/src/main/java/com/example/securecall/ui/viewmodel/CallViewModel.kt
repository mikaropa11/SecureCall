package com.example.securecall.ui.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.securecall.data.remote.dto.CallDto
import com.example.securecall.domain.model.CallStatus
import com.example.securecall.domain.model.CallType
import com.example.securecall.domain.model.UserStatus
import com.example.securecall.domain.repository.UserRepository
import com.example.securecall.domain.repository.WebRTCRepository
import com.google.firebase.auth.FirebaseAuth
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.Job
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.take
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import kotlinx.coroutines.withTimeoutOrNull
import org.webrtc.IceCandidate
import org.webrtc.PeerConnection
import org.webrtc.SurfaceViewRenderer
import javax.inject.Inject

@HiltViewModel
class CallViewModel @Inject constructor(
    private val repository: WebRTCRepository,
    private val userRepository: UserRepository
) : ViewModel() {

    // ─────────────────────────────────────────────────────────────────────────
    // UI STATE
    // ─────────────────────────────────────────────────────────────────────────

    private val _uiState = MutableStateFlow(CallUiState())
    val uiState: StateFlow<CallUiState> = _uiState.asStateFlow()

    private val _incomingCall = MutableStateFlow<CallDto?>(null)
    val incomingCall: StateFlow<CallDto?> = _incomingCall.asStateFlow()

    private var currentCallId: String? = null

    // ─────────────────────────────────────────────────────────────────────────
    // JOBS
    // ─────────────────────────────────────────────────────────────────────────

    private var answerJob: Job? = null
    private var offerJob: Job? = null
    private var iceCandidatesJob: Job? = null
    private var callStatusJob: Job? = null
    private var disconnectedWarningJob: Job? = null

    // ─────────────────────────────────────────────────────────────────────────
    // INITIALIZATION
    // ─────────────────────────────────────────────────────────────────────────

    init {
        FirebaseAuth.getInstance().currentUser?.uid?.let { userId ->
            viewModelScope.launch {
                repository.listenForIncomingCall(userId).collect { call ->
                    _incomingCall.value = call
                }
            }
        }
    }

    fun initializeCall(
        callId: String,
        observer: PeerConnection.Observer,
        localRenderer: SurfaceViewRenderer,
        isCaller: Boolean,
        receiverId: String,
        callType: CallType
    ) {

        currentCallId = callId

        repository.initializeSession(
            observer = observer,
            localRenderer = localRenderer,
            isVideoCall = callType == CallType.VIDEO
        ).onFailure {
            emitError(it.message ?: "Failed to initialize call")
            return
        }

        _uiState.update {
            it.copy(
                isVideoCall = callType == CallType.VIDEO,
                isCameraEnabled = callType == CallType.VIDEO,
                isWaitingForPermissions = false
            )
        }

        observeIceCandidates(callId)
        observeCallStatus(callId)

        if (isCaller) {
            val callerId = FirebaseAuth.getInstance().currentUser?.uid ?: run {
                emitError("User not authenticated")
                return
            }
            val call = CallDto(
                callId = callId,
                callerId = callerId,
                receiverId = receiverId,
                type = callType.name,
                status = CallStatus.CALLING.name,
                timestamp = System.currentTimeMillis()
            )
            startCallerFlow(callId, call)
        } else {
            startCalleeFlow(callId)
        }
    }


    // ─────────────────────────────────────────────────────────────────────────
    // CALLER FLOW
    // ─────────────────────────────────────────────────────────────────────────

    private fun startCallerFlow(callId: String, call: CallDto) {

        answerJob?.cancel()

        answerJob = viewModelScope.launch {

            _uiState.update {
                it.copy(
                    callStatus = CallStatus.CALLING,
                    isConnecting = true
                )
            }

            repository.createCall(call)
                .onFailure {
                    emitError(it.message ?: "Failed to create call")
                    return@launch
                }

            repository.createAndSendOffer(callId)
                .onFailure {
                    emitError(it.message ?: "Failed to create offer")
                    return@launch
                }

            val answerResult = withTimeoutOrNull(CALL_TIMEOUT_MS) {
                repository.handleAnswer(callId)
            }

            if (answerResult == null) {

                repository.endCall(callId)

                _uiState.update {
                    it.copy(
                        callStatus = CallStatus.MISSED,
                        isConnecting = false
                    )
                }

                return@launch
            }

            answerResult
                .onFailure {
                    emitError(it.message ?: "Failed to handle answer")
                }
        }
    }

    // ─────────────────────────────────────────────────────────────────────────
    // CALLEE FLOW
    // ─────────────────────────────────────────────────────────────────────────

    private fun startCalleeFlow(callId: String) {

        offerJob?.cancel()

        offerJob = viewModelScope.launch {

            repository.listenForOffer(callId)
                .take(1)
                .collect { offer ->

                    repository.handleOfferAndSendAnswer(
                        callId = callId,
                        offer = offer
                    ).onFailure {
                        emitError(it.message ?: "Failed to send answer")
                    }
                }
        }
    }

    // ─────────────────────────────────────────────────────────────────────────
    // ICE CANDIDATES
    // ─────────────────────────────────────────────────────────────────────────

    private fun observeIceCandidates(callId: String) {

        iceCandidatesJob?.cancel()

        iceCandidatesJob = viewModelScope.launch {
            val userId = FirebaseAuth.getInstance().currentUser?.uid
            if (userId == null) {
                emitError("User not authenticated")
                return@launch
            }

            repository.listenForIceCandidates(callId, userId)
                .collect { candidate ->

                    repository.addIceCandidate(candidate)
                }
        }
    }

    fun onLocalIceCandidate(
        candidate: IceCandidate
    ) {

        val callId = currentCallId ?: return
        val senderId = FirebaseAuth.getInstance().currentUser?.uid ?: run {
            emitError("User not authenticated")
            return
        }

        viewModelScope.launch {

            repository.sendIceCandidate(
                callId = callId,
                candidate = candidate,
                senderId = senderId
            ).onFailure {
                emitError(it.message ?: "Failed to send ICE candidate")
            }
        }
    }

    fun onIceConnectionStateChanged(state: PeerConnection.IceConnectionState?) {
        _uiState.update { it.copy(iceConnectionState = state?.name) }

        when (state) {
            PeerConnection.IceConnectionState.FAILED -> {
                disconnectedWarningJob?.cancel()
                emitError("Error de conexion")
            }
            PeerConnection.IceConnectionState.DISCONNECTED -> {
                disconnectedWarningJob?.cancel()
                disconnectedWarningJob = viewModelScope.launch {
                    kotlinx.coroutines.delay(5_000)
                    if (_uiState.value.iceConnectionState == PeerConnection.IceConnectionState.DISCONNECTED.name) {
                        emitError("Conexion inestable")
                    }
                }
            }
            PeerConnection.IceConnectionState.CONNECTED,
            PeerConnection.IceConnectionState.COMPLETED -> {
                disconnectedWarningJob?.cancel()
                _uiState.update { it.copy(error = null) }
            }
            else -> Unit
        }
    }

    fun onIceGatheringStateChanged(state: PeerConnection.IceGatheringState?) {
        _uiState.update { it.copy(iceGatheringState = state?.name) }
    }

    // ─────────────────────────────────────────────────────────────────────────
    // CALL STATUS
    // ─────────────────────────────────────────────────────────────────────────

    private fun observeCallStatus(callId: String) {

        callStatusJob?.cancel()
        disconnectedWarningJob?.cancel()

        callStatusJob = viewModelScope.launch {

            repository.listenForCallStatus(callId)
                .collect { status ->

                    _uiState.update {
                        it.copy(
                            callStatus = status
                        )
                    }

                    when (status) {

                        CallStatus.ONGOING -> {
                            updateCurrentUserStatus(UserStatus.in_call)
                            _uiState.update {
                                it.copy(
                                    isConnecting = false,
                                    isInCall = true
                                )
                            }
                        }

                        CallStatus.ENDED,
                        CallStatus.REJECTED,
                        CallStatus.MISSED -> {
                            handleCallTermination()
                        }

                        else -> Unit
                    }
                }
        }
    }

    // ─────────────────────────────────────────────────────────────────────────
    // CONTROLS
    // ─────────────────────────────────────────────────────────────────────────
    fun setRemoteRenderer(renderer: SurfaceViewRenderer) {
        repository.attachRemoteRenderer(renderer)
    }


    fun toggleMic() {

        val enabled = !_uiState.value.isMicEnabled

        repository.setMicEnabled(enabled)

        _uiState.update {
            it.copy(isMicEnabled = enabled)
        }
    }

    fun toggleCamera() {
        if (!_uiState.value.isVideoCall) return

        val enabled = !_uiState.value.isCameraEnabled

        repository.setCameraEnabled(enabled)

        _uiState.update {
            it.copy(isCameraEnabled = enabled)
        }
    }

    fun switchCamera() {
        repository.switchCamera()
    }

    fun toggleSpeaker() {

        val enabled = !_uiState.value.isSpeakerEnabled

        repository.setSpeakerEnabled(enabled)

        _uiState.update {
            it.copy(isSpeakerEnabled = enabled)
        }
    }

    fun getEglContext() = repository.getEglContext()

    // ─────────────────────────────────────────────────────────────────────────
    // END CALL
    // ─────────────────────────────────────────────────────────────────────────

    fun endCall() {

        val callId = currentCallId ?: return

        viewModelScope.launch {

            repository.endCall(callId)

            handleCallTermination()
        }
    }

    fun rejectCall(callId: String) {

        viewModelScope.launch {

            repository.rejectCall(callId)

            handleCallTermination()
        }
    }

    // ─────────────────────────────────────────────────────────────────────────
    // CLEANUP
    // ─────────────────────────────────────────────────────────────────────────

    private fun handleCallTermination() {

        cancelAllJobs()

        repository.cleanup()
        updateCurrentUserStatus(UserStatus.online)

        _uiState.update {
            it.copy(
                isConnecting = false,
                isInCall = false
            )
        }
    }

    private fun cancelAllJobs() {

        answerJob?.cancel()
        offerJob?.cancel()
        iceCandidatesJob?.cancel()
        callStatusJob?.cancel()

        answerJob = null
        offerJob = null
        iceCandidatesJob = null
        callStatusJob = null
        disconnectedWarningJob?.cancel()
        disconnectedWarningJob = null
    }

    private fun emitError(message: String) {

        _uiState.update {
            it.copy(error = message)
        }
    }

    fun clearError() {

        _uiState.update {
            it.copy(error = null)
        }
    }

    override fun onCleared() {
        super.onCleared()
        handleCallTermination()
    }

    fun clearIncomingCall() {
        _incomingCall.value = null
    }

    private fun updateCurrentUserStatus(status: UserStatus) {
        val userId = FirebaseAuth.getInstance().currentUser?.uid ?: return
        viewModelScope.launch {
            userRepository.updateUserStatus(userId, status)
        }
    }

    fun waitingForPermissions(callType: CallType) {
        _uiState.update {
            it.copy(
                isWaitingForPermissions = true,
                isVideoCall = callType == CallType.VIDEO,
                error = null
            )
        }
    }

    fun permissionsDenied() {
        _uiState.update {
            it.copy(
                isWaitingForPermissions = false,
                isConnecting = false,
                error = "Permisos requeridos denegados"
            )
        }
    }

    companion object {
        private const val CALL_TIMEOUT_MS = 30_000L
    }
}

// ─────────────────────────────────────────────────────────────────────────────
// UI STATE
// ─────────────────────────────────────────────────────────────────────────────

data class CallUiState(

    val callStatus: CallStatus = CallStatus.IDLE,

    val isConnecting: Boolean = false,

    val isInCall: Boolean = false,

    val isMicEnabled: Boolean = true,

    val isCameraEnabled: Boolean = true,

    val isSpeakerEnabled: Boolean = true,

    val isVideoCall: Boolean = true,

    val isWaitingForPermissions: Boolean = false,

    val error: String? = null,

    val iceConnectionState: String? = null,

    val iceGatheringState: String? = null
)
