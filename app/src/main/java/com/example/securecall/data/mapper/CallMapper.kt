package com.example.securecall.data.mapper

import com.example.securecall.data.local.entity.CallEntity
import com.example.securecall.data.remote.dto.CallDto
import com.example.securecall.domain.model.Call
import com.example.securecall.domain.model.CallStatus
import com.example.securecall.domain.model.CallType

fun CallEntity.toDomain(): Call {
    return Call(
        callId = callId,
        callerId = callerId,
        receiverId = receiverId,
        timestamp = timestamp,
        duration = duration,
        type = type,
        status = status
    )
}

fun Call.toEntity(): CallEntity {
    return CallEntity(
        callId = callId,
        callerId = callerId,
        receiverId = receiverId,
        timestamp = timestamp,
        duration = duration,
        type = type,
        status = status
    )
}
fun CallDto.toDomain(): Call {
    return Call(
        callId = callId,
        callerId = callerId,
        receiverId = receiverId,
        timestamp = timestamp,
        duration = duration,
        type = callType(),
        status = callStatus()
    )
}

fun Call.toDto(): CallDto {
    return CallDto(
        callId = callId,
        callerId = callerId,
        receiverId = receiverId,
        timestamp = timestamp,
        duration = duration,
        type = type.name,
        status = status.name
    )
}


