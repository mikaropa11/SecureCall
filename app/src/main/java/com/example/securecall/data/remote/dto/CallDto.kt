package com.example.securecall.data.remote.dto

import com.example.securecall.domain.model.CallStatus
import com.example.securecall.domain.model.CallType

data class CallDto(
    val callId: String = "",
    val callerId: String = "",
    val receiverId: String = "",
    val type: String = CallType.VIDEO.name,
    val status: String = CallStatus.IDLE.name,
    val timestamp: Long = 0L,
    val duration: Long = 0L
) {
    fun callType() = CallType.valueOf(type)
    fun callStatus() = CallStatus.valueOf(status)
}
