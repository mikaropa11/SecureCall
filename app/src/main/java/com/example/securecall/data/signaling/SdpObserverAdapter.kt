package com.example.securecall.data.signaling

import org.webrtc.SdpObserver
import org.webrtc.SessionDescription

open class SdpObserverAdapter : SdpObserver {

    override fun onCreateSuccess(sdp: SessionDescription) {}

    override fun onSetSuccess() {}

    override fun onCreateFailure(error: String?) {}

    override fun onSetFailure(error: String?) {}
}