package com.example.securecall.di

import android.content.Context
import com.example.securecall.data.repository.WebRTCRepositoryImpl
import com.example.securecall.data.webrtc.WebRTCClient
import com.example.securecall.data.webrtc.WebRTCManager
import com.example.securecall.domain.repository.WebRTCRepository
import com.google.firebase.firestore.FirebaseFirestore
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.android.qualifiers.ApplicationContext
import dagger.hilt.components.SingletonComponent
import javax.inject.Singleton

@Module
@InstallIn(SingletonComponent::class)
object WebRTCModule {

    @Provides
    @Singleton
    fun provideWebRTCManager(@ApplicationContext context: Context): WebRTCManager = WebRTCManager(context)

    @Provides
    @Singleton
    fun provideWebRTCClient(webRTCManager: WebRTCManager): WebRTCClient = WebRTCClient(webRTCManager)

    @Provides
    @Singleton
    fun provideWebRTCRepository(firestore: FirebaseFirestore, webRTCClient: WebRTCClient): WebRTCRepository =
        WebRTCRepositoryImpl(firestore, webRTCClient)


}