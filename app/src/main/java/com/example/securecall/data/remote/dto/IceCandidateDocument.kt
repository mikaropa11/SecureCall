package com.example.securecall.data.remote.dto

data class IceCandidateDocument(
    val sdpMid: String = "",
    val sdpMLineIndex: Int = 0,
    val sdp: String = "",
    val serverUrl: String = "",
)
