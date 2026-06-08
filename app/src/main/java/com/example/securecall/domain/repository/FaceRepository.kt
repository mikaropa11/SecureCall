package com.example.securecall.domain.repository

import androidx.camera.core.ImageProxy
import com.example.securecall.data.repository.AuthResult
import com.example.securecall.data.repository.RegisterResult

interface FaceRepository {

    suspend fun register(imageProxy: ImageProxy): RegisterResult
    suspend fun authenticate(imageProxy: ImageProxy): AuthResult
    fun getRegistrationProgressPublic(): RegistrationProgress
}

enum class RegistrationStep {
    FRONTAL,
    LEFT,
    RIGHT
}

enum class DetectedOrientation {
    FRONTAL,
    LEFT,
    RIGHT,
    UNKNOWN
}

data class HeadPose(
    val yaw: Float
)

data class FaceFrameInfo(
    val headPose: HeadPose,
    val orientation: DetectedOrientation,
    val detectionScore: Float,
    val eyeDistance: Float,
    val faceCenterOffsetX: Float,
    val faceCenterOffsetY: Float
)

data class RegistrationProgress(
    val currentStep: RegistrationStep,
    val framesInStep: Int,
    val framesNeeded: Int,
    val frontalCompleted: Boolean = false,
    val leftCompleted: Boolean = false,
    val rightCompleted: Boolean = false,
    val isComplete: Boolean = false
)
