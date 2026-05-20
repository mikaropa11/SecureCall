package com.example.securecall.domain.model

data class Call(
    val callId: String,
    val callerId: String,
    val receiverId: String,
    val timestamp: Long,
    val duration: Long,
    val type: CallType,
    val status: CallStatus
)

enum class CallType { AUDIO, VIDEO }

enum class CallStatus {
    IDLE,
    CALLING,
    ONGOING,
    ENDED,
    MISSED,
    REJECTED
}
