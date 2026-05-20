package com.example.securecall.data.webrtc

import android.content.Context
import org.webrtc.DefaultVideoDecoderFactory
import org.webrtc.DefaultVideoEncoderFactory
import org.webrtc.EglBase
import org.webrtc.PeerConnectionFactory

class WebRTCManager(
    val context: Context
){

    private val eglBase: EglBase by lazy {
        EglBase.create()
    }

    val eglContext
        get() = eglBase.eglBaseContext

    val peerConnectionFactory: PeerConnectionFactory by lazy {
        initializePeerConnectionFactory()

        val options = PeerConnectionFactory.Options()

        val encoderFactory = DefaultVideoEncoderFactory(
            eglBase.eglBaseContext,
            true,
            true)

        val decoderFactory = DefaultVideoDecoderFactory(
            eglBase.eglBaseContext)

        PeerConnectionFactory.builder()
            .setOptions(options)
            .setVideoEncoderFactory(encoderFactory)
            .setVideoDecoderFactory(decoderFactory)
            .createPeerConnectionFactory()
    }


    private fun initializePeerConnectionFactory() {

        val options =
            PeerConnectionFactory.InitializationOptions
                .builder(context)
                .setEnableInternalTracer(true)
                .createInitializationOptions()

        PeerConnectionFactory.initialize(options)
    }



}