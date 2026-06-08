package com.example.securecall.data.mapper

import com.example.securecall.data.remote.dto.IceCandidateDocument
import org.webrtc.IceCandidate

fun IceCandidateDocument.toIceCandidate() =
    IceCandidate(sdpMid, sdpMLineIndex, sdp)

fun IceCandidate.toDocument(senderId: String) =
    IceCandidateDocument(
        sdpMid = sdpMid,
        sdpMLineIndex = sdpMLineIndex,
        sdp = sdp,
        serverUrl = serverUrl ?: "",
        senderId = senderId
    )
