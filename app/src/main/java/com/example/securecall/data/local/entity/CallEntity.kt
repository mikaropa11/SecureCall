package com.example.securecall.data.local.entity

import androidx.room.Entity
import androidx.room.PrimaryKey
import com.example.securecall.domain.model.CallStatus
import com.example.securecall.domain.model.CallType

@Entity(tableName = "calls")
data class CallEntity(
    @PrimaryKey val callId: String,
    val callerId: String,
    val receiverId: String,
    val timestamp: Long,
    val duration: Long,
    val type: CallType,
    val status: CallStatus
)