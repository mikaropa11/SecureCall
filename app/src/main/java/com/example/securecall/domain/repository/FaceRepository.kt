package com.example.securecall.domain.repository

import androidx.camera.core.ImageProxy
import com.example.securecall.data.repository.FaceResult
import com.example.securecall.ui.viewmodel.FaceState

interface FaceRepository {

    suspend fun getEmbedding(imageProxy: ImageProxy): FaceResult
    suspend fun compareEmbedding(embedding: FloatArray): Boolean
    fun addEmbedding(embedding: FloatArray)
    fun hasEnoughFrames(): Boolean
    fun getAveragedEmbedding(): FloatArray?
    fun resetEmbeddings()
}