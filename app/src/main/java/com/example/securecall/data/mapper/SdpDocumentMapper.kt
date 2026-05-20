package com.example.securecall.data.mapper

import com.example.securecall.data.remote.dto.SdpDocument
import org.webrtc.SessionDescription

fun SdpDocument.toSessionDescription(): SessionDescription =
    SessionDescription(
        SessionDescription.Type.fromCanonicalForm(type),
        sdp)

fun SessionDescription.toSdpDocument(): SdpDocument =
    SdpDocument(
        type = type.canonicalForm(),
        sdp = description
    )